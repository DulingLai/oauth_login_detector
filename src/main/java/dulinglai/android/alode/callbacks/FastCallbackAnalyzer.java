package dulinglai.android.alode.callbacks;

import java.io.IOException;
import java.util.Set;

import dulinglai.android.alode.utils.sootUtils.SystemClassHandler;
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

	public FastCallbackAnalyzer(Set<SootClass> entryPointClasses, Set<String> activityList)
			throws IOException {
		super(entryPointClasses, 0, activityList);
	}

	public FastCallbackAnalyzer(Set<SootClass> entryPointClasses,
			String callbackFile, Set<String> activityList) throws IOException {
		super(entryPointClasses, callbackFile, 0, activityList);
	}

	public FastCallbackAnalyzer(Set<SootClass> entryPointClasses,
			Set<String> androidCallbacks, Set<String> activityList) throws IOException {
		super(entryPointClasses, androidCallbacks, 0, activityList);
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
        Boolean isActivityClass = false;
		for (SootClass sc : Scene.v().getApplicationClasses()) {

		    if (sc.isConcrete()) {
                // Do not start the search in system classes
                if (SystemClassHandler.isClassInSystemPackage(sc.getName()))
                    continue;

                isActivityClass = checkActivityClass(sc.getName());

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
                                    } else if (isActivityClass && invokesFindViewById(inv)){
                                        for (Value val : inv.getArgs()){
                                            Integer intValue = valueProvider.getValue(sm, stmt, val, Integer.class);
                                            if (intValue != null)

                                        }
									}
                                } else if (stmt instanceof AssignStmt){
                                    Value rightOp = ((AssignStmt) stmt).getRightOp();
                                    if (rightOp instanceof NewExpr){
                                        if (assignsNewWidget(((NewExpr) rightOp).getBaseType())){
                                            // TODO replace the logger with something else to capture the dynamic widgets

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
//        try {
//            resultWriter.appendStringToResultFile("DynamicWidgets.csv", resultStringBuilder.toString());
//        } catch (IOException e){
//            e.printStackTrace();
//        }
	}

    @Override
	public void excludeEntryPoint(SootClass entryPoint) {
		// not supported
	}

}
