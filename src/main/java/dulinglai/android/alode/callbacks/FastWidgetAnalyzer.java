package dulinglai.android.alode.callbacks;

import dulinglai.android.alode.graphBuilder.AbstractWidgetNode;
import dulinglai.android.alode.graphBuilder.ClickWidgetNode;
import dulinglai.android.alode.graphBuilder.EditWidgetNode;
import dulinglai.android.alode.resources.resources.LayoutFileParser;
import dulinglai.android.alode.resources.resources.controls.AndroidLayoutControl;
import dulinglai.android.alode.resources.resources.controls.EditTextControl;
import dulinglai.android.alode.utils.sootUtils.SystemClassHandler;
import org.pmw.tinylog.Logger;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;
import soot.util.Chain;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A widget analyzer that creates widgets by analyzing the jimple file
 */
public class FastWidgetAnalyzer extends AbstractWidgetAnalyzer {

    public FastWidgetAnalyzer(Set<SootClass> entryPointClasses, Set<String> activityList, LayoutFileParser layoutFileParser)
            throws IOException {
        super(entryPointClasses, 0, activityList, layoutFileParser);
        this.editTextWidgetSet = new HashSet<>();
        this.clickWidgetNodeSet = new HashSet<>();
        this.ownershipEdges = new HashMap<>();
    }

    @Override
    public void collectWidgets() {
        super.collectWidgets();
        Logger.info("Collecting widgets in FAST mode...");

        // Find the mappings between classes and layouts
        findClassLayoutMappings();

        // Find the widgets and mapping
        findWidgetsMappings();

        // Assign callbacks to widgets
        for (SootClass sc : Scene.v().getApplicationClasses()) {
            if (!sc.isConcrete())
                continue;

            if (!isExportedActivityClass(sc.getName()))
                continue;

            for (SootMethod sm : sc.getMethods()) {
                if (sm.isConcrete()) {
                    analyzeMethodForCallbackRegistrations(sc, sm);
                    analyzeMethodForDynamicBroadcastReceiver(sm);
                    analyzeMethodForServiceConnection(sm);
                }
            }
            // Check for method overrides
            analyzeMethodOverrideCallbacks(sc);
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
                if (!isExportedActivityClass(sc.getName()))
                    continue;

                // TODO Remove this method, used for debug only
                if (sc.getName().equalsIgnoreCase("com.dominos.bot.BotSignInDialogFragment"))
                    Logger.debug("Here");

                // Map the activity class with the base activity that it implements
                SootClass curClass = sc;
                while (curClass.hasSuperclass()){
                    SootClass superClass = curClass.getSuperclass();
                    if (superClass.getName().equals("android.app.Activity")
                            || superClass.getName().equals("android.support.v7.app.ActionBarActivity")
                            || superClass.getName().equals("android.support.v7.app.AppCompatActivity")
                            || superClass.getName().equals("android.support.v4.app.SupportActivity")) {
                        baseActivityMapping.put(sc, curClass);
                        nestedActivitySet.add(curClass);
                    } else
                        break;
                    curClass = superClass;
                }
                findWidgetsMappingsOnSootClass(sc);
            }
        }

        // Analyze the nested activities
        for (SootClass sc : nestedActivitySet) {
            if (sc.isConcrete()) {
                // Do not start the search in system classes
                if (SystemClassHandler.isClassInSystemPackage(sc.getName()))
                    continue;

                findWidgetsMappingsOnSootClass(sc);
            }
        }
    }

    /**
     * Finds the widgets and ownership mappings on given SootClass
     * @param sc The soot class to analyze
     */
    private void findWidgetsMappingsOnSootClass(SootClass sc) {
        Set<AbstractWidgetNode> widgetNodeSet = new HashSet<>();

        for (SootMethod sm : sc.getMethods()) {
            if (!sm.isConcrete())
                continue;

            // TODO Remove this debug log
            if (sm.getName().equalsIgnoreCase("onAfterViews"))
                Logger.debug("Here");

            try {
                Body b = sm.retrieveActiveBody();
                Chain<Unit> units = b.getUnits();
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
                                    // Check the usage of this local variable
                                    BriefUnitGraph unitGraph = new BriefUnitGraph(sm.retrieveActiveBody());
                                    SimpleLocalDefs localDefs = new SimpleLocalDefs(unitGraph);
                                    SimpleLocalUses localUses = new SimpleLocalUses(unitGraph, localDefs);

                                    if (leftOp instanceof Local) {
                                        // Editable widgets
                                        List<UnitValueBoxPair> usePair = localUses.getUsesOf(u);
                                        // Check for cast expr
                                        Unit u2 = reassignsLocal(usePair);
                                        if (u2!=null)
                                            usePair = localUses.getUsesOf(u2);

                                        if (isEditTextWidget(newSootClass)) {
                                            EditWidgetNode newEditWidgetNode = createNewEditTextWidget(usePair, leftOp);
                                            if (newEditWidgetNode != null) {
                                                editTextWidgetSet.add(newEditWidgetNode);
                                                widgetNodeSet.add(newEditWidgetNode);
                                            }
                                        }
                                        // Clickable widgets
                                        else {
                                            ClickWidgetNode newClickWidgetNode = createNewClickWidget(usePair, leftOp);
                                            if (newClickWidgetNode != null) {
                                                clickWidgetNodeSet.add(newClickWidgetNode);
                                                widgetNodeSet.add(newClickWidgetNode);
                                            }
                                        }
                                    }
                                }
                            }

                            // Here we collect widgets that are mapped to the XML files
                            if (rightOp instanceof InvokeExpr) {
                                // collect resource Id
                                if (invokesFindViewById((InvokeExpr) rightOp)) {
                                    int resourceId = -1;
                                    AndroidLayoutControl userControl = null;

                                    for (int i=0; i<((InvokeExpr) rightOp).getArgCount(); i++){
                                        String arg = ((InvokeExpr)rightOp).getArg(0).toString();
                                        if (arg.matches("-?\\d+")) {
                                            resourceId = Integer.parseInt(arg);
                                            userControl = this.layoutFileParser.findUserControlById(resourceId);
                                            break;
                                        }
                                    }

                                    if (userControl != null) {
                                        // Def-use analysis to find the attr
                                        if (leftOp instanceof Local) {
                                            BriefUnitGraph unitGraph = new BriefUnitGraph(sm.retrieveActiveBody());
                                            SimpleLocalDefs localDefs = new SimpleLocalDefs(unitGraph);
                                            SimpleLocalUses localUses = new SimpleLocalUses(unitGraph, localDefs);

                                            List<UnitValueBoxPair> usePair = localUses.getUsesOf(u);
                                            // Check for cast expr
                                            Unit u2 = reassignsLocal(usePair);
                                            if (u2!=null)
                                                usePair = localUses.getUsesOf(u2);

                                            if (userControl instanceof EditTextControl) {
                                                EditWidgetNode newEditWidgetNode = createNewEditTextWidget(usePair, leftOp, resourceId);
                                                if (newEditWidgetNode != null) {
                                                    editTextWidgetSet.add(newEditWidgetNode);
                                                    widgetNodeSet.add(newEditWidgetNode);
                                                }
                                            }
                                            // Clickable widgets
                                            else {
                                                ClickWidgetNode newClickWidgetNode = createNewClickWidget(usePair, leftOp, resourceId);
                                                if (newClickWidgetNode != null) {
                                                    clickWidgetNodeSet.add(newClickWidgetNode);
                                                    widgetNodeSet.add(newClickWidgetNode);
                                                }
                                            }
                                        } else
                                            Logger.warn("[LayoutParser] Cannot find the widgets in findViewById...");
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (RuntimeException e) {
                Logger.warn(e.toString());
            }
        }
        // add ownership edges
        if (!widgetNodeSet.isEmpty())
            ownershipEdges.put(sc, widgetNodeSet);
    }

    /**
     * Finds the mappings between classes and their respective layout files
     */
    private void findClassLayoutMappings() {
//        Chain<SootClass> scs = Scene.v().getClasses();
//        for (SootClass newsc : scs) {
//            if (newsc.getName().equalsIgnoreCase("com.dominos.commons.BaseActivity"))
//        }
        for (SootClass sc : Scene.v().getApplicationClasses()) {

            // TODO Remove this method, used for debug only
            if (sc.getName().equalsIgnoreCase("com.dominos.dialogs.ProductDetailsPopUp"))
                Logger.debug("Here");

            // Do not start the search in system classes
            if (SystemClassHandler.isClassInSystemPackage(sc.getName()))
                continue;

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
                                break;
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
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void excludeEntryPoint(SootClass entryPoint) {
        // not supported
    }

}
