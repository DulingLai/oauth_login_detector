package dulinglai.android.alode.iccparser;

import dulinglai.android.alode.entryPointCreators.components.ComponentEntryPointCollection;
import org.pmw.tinylog.Logger;
import soot.*;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.util.Chain;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

import java.util.*;

public class IccInstrumenter {

	protected final String iccModel;
	protected final SootClass dummyMainClass;
	protected final ComponentEntryPointCollection componentToEntryPoint;

	protected IccRedirectionCreator redirectionCreator = null;

	protected final SootMethod smMessengerSend;
	protected final Set<SootMethod> processedMethods = new HashSet<>();
	protected final MultiMap<Body, Unit> instrumentedUnits = new HashMultiMap<>();

	public IccInstrumenter(String iccModel, SootClass dummyMainClass,
			ComponentEntryPointCollection componentToEntryPoint) {
		this.iccModel = iccModel;
		this.dummyMainClass = dummyMainClass;
		this.componentToEntryPoint = componentToEntryPoint;

		// Fetch some Soot methods
		smMessengerSend = Scene.v().grabMethod("<android.os.Messenger: void send(android.os.Message)>");
	}

	public void onBeforeCallgraphConstruction() {
		Logger.info("[IccTA] Launching IccTA Transformer...");

		Logger.info("[IccTA] Loading the ICC Model...");
		Ic3Provider provider = new Ic3Provider(iccModel);
		List<IccLink> iccLinks = provider.getIccLinks();
		Logger.info("[IccTA] ...End Loading the ICC Model");

		// Create the redirection creator
		if (redirectionCreator == null)
			redirectionCreator = new IccRedirectionCreator(dummyMainClass, componentToEntryPoint);
		else
			redirectionCreator.undoInstrumentation();

		Logger.info("[IccTA] Lauching ICC Redirection Creation...");
		for (IccLink link : iccLinks) {
			if (link.getFromU() == null) {
				continue;
			}
			redirectionCreator.redirectToDestination(link);
		}

		// Remove any potential leftovers from the last last instrumentation
		for (Body body : instrumentedUnits.keySet()) {
			for (Unit u : instrumentedUnits.get(body)) {
				body.getUnits().remove(u);
			}
		}
		instrumentedUnits.clear();

		// Instrument the messenger class
		instrumentMessenger();

		// Remove data that is no longer needed
		processedMethods.clear();

		Logger.info("[IccTA] ...End ICC Redirection Creation");
	}

	protected void instrumentMessenger() {
		Logger.info("Launching Messenger Transformer...");

		Chain<SootClass> applicationClasses = Scene.v().getApplicationClasses();
		for (Iterator<SootClass> iter = applicationClasses.snapshotIterator(); iter.hasNext();) {
			SootClass sootClass = iter.next();

			// We copy the list of methods to emulate a snapshot iterator which
			// doesn't exist for methods in Soot
			List<SootMethod> methodCopyList = new ArrayList<>(sootClass.getMethods());
			for (SootMethod sootMethod : methodCopyList) {
				if (sootMethod.isConcrete()) {
					final Body body = sootMethod.retrieveActiveBody();
					final LocalGenerator lg = new LocalGenerator(body);

					// Mark the method as processed
					if (!processedMethods.add(sootMethod))
						continue;

					for (Iterator<Unit> unitIter = body.getUnits().snapshotIterator(); unitIter.hasNext();) {
						Stmt stmt = (Stmt) unitIter.next();

						if (stmt.containsInvokeExpr()) {
							SootMethod callee = stmt.getInvokeExpr().getMethod();

							// For Messenger.send(), we directly call the respective handler
							if (callee == smMessengerSend) {
								Set<SootClass> handlers = MessageHandler.v().getAllHandlers();
								for (SootClass handler : handlers) {
									Local handlerLocal = lg.generateLocal(handler.getType());

									Unit newU = Jimple.v().newAssignStmt(handlerLocal,
											Jimple.v().newNewExpr(handler.getType()));
									body.getUnits().insertAfter(newU, stmt);
									instrumentedUnits.put(body, newU);

									SootMethod initMethod = handler.getMethod("void <init>()");
									Unit initU = Jimple.v().newInvokeStmt(
											Jimple.v().newSpecialInvokeExpr(handlerLocal, initMethod.makeRef()));
									body.getUnits().insertAfter(initU, newU);
									instrumentedUnits.put(body, initU);

									SootMethod hmMethod = handler.getMethod("void handleMessage(android.os.Message)");
									Unit callHMU = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(
											handlerLocal, hmMethod.makeRef(), stmt.getInvokeExpr().getArg(0)));
									body.getUnits().insertAfter(callHMU, initU);
									instrumentedUnits.put(body, callHMU);
								}
							}
						}
					}

					body.validate();
				}

			}
		}

	}
}
