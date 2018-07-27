package dulinglai.android.alode.callbacks;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import dulinglai.android.alode.ResultWriter;
import org.pmw.tinylog.Logger;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

/**
 * A callback analyzer that favors performance over precision.
 * 
 * @author Steven Arzt
 *
 */
public class FastCallbackAnalyzer extends AbstractCallbackAnalyzer {

	public FastCallbackAnalyzer(Set<SootClass> entryPointClasses, ResultWriter resultWriter)
			throws IOException {
		super(entryPointClasses, 0, resultWriter);
	}

	public FastCallbackAnalyzer(Set<SootClass> entryPointClasses,
			String callbackFile, ResultWriter resultWriter) throws IOException {
		super(entryPointClasses, callbackFile, 0, resultWriter);
	}

	public FastCallbackAnalyzer(Set<SootClass> entryPointClasses,
			Set<String> androidCallbacks, ResultWriter resultWriter) throws IOException {
		super(entryPointClasses, androidCallbacks, 0, resultWriter);
	}

	@Override
	public void collectCallbackMethods() {
		super.collectCallbackMethods();
		Logger.info("Collecting callbacks in FAST mode...");

		// Find the mappings between classes and layouts
		findClassLayoutMappings();

		for (SootClass sc : Scene.v().getApplicationClasses()) {
			if (sc.isConcrete()) {
				for (SootMethod sm : sc.getMethods()) {
					if (sm.isConcrete()) {
						analyzeMethodForCallbackRegistrations(null, sm);
						analyzeMethodForDynamicBroadcastReceiver(sm);
						analyzeMethodForServiceConnection(sm);
					}
				}

				// Check for method overrides
				analyzeMethodOverrideCallbacks(sc);
			}
		}
	}

	/**
	 * Finds the mappings between classes and their respective layout files
	 */
	private void findClassLayoutMappings() {
        // Here we log the instance
        StringBuilder resultStringBuilder = new StringBuilder();
		for (SootClass sc : Scene.v().getApplicationClasses()) {
		    if (sc.getName().contains("InitialLaunchActivity"))
		        Logger.debug("here");

		    if (sc.isConcrete()) {
				for (SootMethod sm : sc.getMethods()) {
					if (!sm.isConcrete())
						continue;

					// Here we try to prevent the multi-catch bug from happening
					try{
					    Chain<Unit> units = sm.retrieveActiveBody().getUnits();
                        for (Unit u : units) {
                            // Check for class layout mappings
                            if (u instanceof Stmt) {
                                Stmt stmt = (Stmt) u;
                                if (stmt.containsInvokeExpr()) {
                                    InvokeExpr inv = stmt.getInvokeExpr();
                                    if (invokesSetContentView(inv) || invokesInflate(inv)) {
                                        for (Value val : inv.getArgs()) {
                                            Integer intValue = valueProvider.getValue(sm, stmt, val, Integer.class);
                                            if (intValue != null)
                                                this.layoutClasses.put(sm.getDeclaringClass(), intValue);
                                        }
                                    }
                                } else if (stmt instanceof AssignStmt){
                                    Value rightOp = ((AssignStmt) stmt).getRightOp();
                                    if (rightOp instanceof NewExpr){
                                        if (assignsNewWidget(((NewExpr) rightOp).getBaseType())){
                                            // TODO replace the logger with something else to capture the dynamic widgets
                                            resultStringBuilder.append(sc.getName());
                                            resultStringBuilder.append("    ");
                                            resultStringBuilder.append(((NewExpr) rightOp).getBaseType().getSootClass().getName());
                                            resultStringBuilder.append(" , ");
                                        }
                                    }
                                }
                            }
                        }
                    } catch (RuntimeException e){
                        Logger.warn(e.getMessage());
                    }
				}
			}
		}
        try {
            resultWriter.appendStringToResultFile("DynamicWidgets.csv", resultStringBuilder.toString());
        } catch (IOException e){
            e.printStackTrace();
        }
	}

    @Override
	public void excludeEntryPoint(SootClass entryPoint) {
		// not supported
	}

}
