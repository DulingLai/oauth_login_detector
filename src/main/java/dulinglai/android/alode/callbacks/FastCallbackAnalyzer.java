package dulinglai.android.alode.callbacks;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import dulinglai.android.alode.utils.sootUtils.SystemClassHandler;
import org.pmw.tinylog.Logger;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;
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

    @Override
    public void collectCallbackMethods() {
        super.collectCallbackMethods();
        Logger.info("Collecting callbacks in FAST mode...");

        // Find the mappings between classes and layouts
        findClassLayoutMappings();

        // Find the widgets and mapping
        findWidgetsMappings();

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
     * Creates widget nodes and ownership edges
     */
    private void findWidgetsMappings() {
        for (SootClass sc : Scene.v().getApplicationClasses()){
            if (sc.isConcrete()){
                // Do not start the search in system classes
                if (SystemClassHandler.isClassInSystemPackage(sc.getName()))
                    continue;

                // We only care about activity classes
                if (!checkActivityClass(sc.getName()))
                    continue;

                // Map the activity class with the base activity that it implements
                if (sc.hasSuperclass()){
                    SootClass superClass = sc.getSuperclass();
                    if (!superClass.getName().equals("android.app.Activity")
                            && !superClass.getName().equals("android.support.v7.app.ActionBarActivity")
                            && !superClass.getName().equals("android.support.v7.app.AppCompatActivity"))
                        baseActivityMapping.put(sc, superClass);
                }

                for (SootMethod sm : sc.getMethods()) {
                    if (!sm.isConcrete())
                        continue;

                    try {
                        Chain<Unit> units = sm.retrieveActiveBody().getUnits();
                        for (Unit u : units) {
                            // Check for class layout mappings
                            if (u instanceof Stmt) {
                                Stmt stmt = (Stmt) u;

                                if (stmt instanceof AssignStmt) {
                                    Value rightOp = ((AssignStmt) stmt).getRightOp();
                                    Value leftOp = ((AssignStmt) stmt).getLeftOp();

                                    // Here we capture dynamically created widgets
                                    if (rightOp instanceof NewExpr) {
                                        SootClass newSootClass = ((NewExpr) rightOp).getBaseType().getSootClass();
                                        if (assignsNewWidget(newSootClass)) {
                                            // TODO replace the logger with something else to capture the dynamic widgets
                                            // Editable widgets
                                            if (isEditTextWidget(newSootClass)){
                                                collectEditTextAttr(units, leftOp);
                                            }

                                            // Clickable widgets

                                        }
                                    }
                                    // Here we map widgets to activities
                                    if (rightOp instanceof InvokeExpr) {
                                        if (invokesFindViewById((InvokeExpr) rightOp)) {
                                            int resourceId = Integer.parseInt(((InvokeExpr)rightOp).getArg(0).toString());
                                            Logger.debug("{}", ((AssignStmt) stmt).getLeftOp());
                                        }
                                    }
                                }
                            }
                        }
                    } catch (RuntimeException e) {
                        Logger.warn(e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Finds the mappings between classes and their respective layout files
     */
    private void findClassLayoutMappings() {
        for (SootClass sc : Scene.v().getApplicationClasses()) {

            if (sc.isConcrete()) {
                // Do not start the search in system classes
                if (SystemClassHandler.isClassInSystemPackage(sc.getName()))
                    continue;

                if (sc.getName().equalsIgnoreCase("com.dominos.activities.InitialLaunchActivity"))
                    Logger.debug("Here");

                for (SootMethod sm : sc.getMethods()) {
                    if (!sm.isConcrete())
                        continue;

                    try{
                        // Here we add the methods that wrap findViewById
                        if (sm.getReturnType().toString().equalsIgnoreCase("android.view.View")) {
                            int i = 0;
                            for (Type paramType : sm.getParameterTypes()) {
                                if (paramType.toString().equals("int")) {
                                    Local paramLocal = sm.retrieveActiveBody().getParameterLocal(i);
                                    // Check the usage of this local variable
                                    BriefUnitGraph unitGraph = new BriefUnitGraph(sm.retrieveActiveBody());
                                    SimpleLocalDefs localDefs = new SimpleLocalDefs(unitGraph);
                                    SimpleLocalUses localUses = new SimpleLocalUses(unitGraph, localDefs);

                                    List<Unit> intDefs = localDefs.getDefsOf(paramLocal);
                                    for (Unit intDef : intDefs) {
                                        List<UnitValueBoxPair> usePair = localUses.getUsesOf(intDef);
                                        Logger.debug("{}", usePair);
                                        Unit u = usePair.get(0).getUnit();
                                        if (u instanceof Stmt) {
                                            Stmt stmt = (Stmt)u;
                                            if (stmt.containsInvokeExpr()) {
                                                InvokeExpr inv = stmt.getInvokeExpr();
                                                if (invokesFindViewById(inv)) {
                                                    findViewMethod.add(sm);
                                                }
                                            }
                                        }
                                    }
                                }
                                i++;
                            }
                        }

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
                                }
                            }
                        }
                    } catch (RuntimeException e){
                        Logger.warn(e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void excludeEntryPoint(SootClass entryPoint) {
        // not supported
    }

}
