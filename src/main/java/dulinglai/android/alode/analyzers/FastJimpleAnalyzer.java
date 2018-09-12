package dulinglai.android.alode.analyzers;

import dulinglai.android.alode.graphBuilder.ClickWidgetNode;
import dulinglai.android.alode.graphBuilder.EditWidgetNode;
import dulinglai.android.alode.resources.resources.LayoutFileParser;
import dulinglai.android.alode.resources.resources.controls.AndroidLayoutControl;
import dulinglai.android.alode.resources.resources.controls.EditTextControl;
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
import soot.util.HashMultiMap;

import java.io.IOException;
import java.util.*;

/**
 * A widget analyzer that creates widgets by analyzing the jimple file
 */
public class FastJimpleAnalyzer extends AbstractJimpleAnalyzer {

    private static final String ANALYZER = "FastJimpleAnalyzer";

    public FastJimpleAnalyzer(Set<SootClass> entryPointClasses, Set<String> activityList,
                              LayoutFileParser layoutFileParser, Map<SootClass, SootClass> classesForFurtherAnalysis)
            throws IOException {
        super(entryPointClasses, 0, activityList, layoutFileParser, classesForFurtherAnalysis);
        this.editTextWidgetList = new ArrayList<>();
        this.clickWidgetNodeList = new ArrayList<>();
        this.ownershipEdges = new HashMultiMap<>();
        this.classMapForActivityHierarchy = new HashMap<>();
    }

    @Override
    public void analyzeJimpleClasses() {
        super.analyzeJimpleClasses();
        Logger.info("[{}] Analyzing Jimple classes in FAST mode...", ANALYZER);

        // Check and add nested activities
        checkAndAddNestedActivities();

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
                // We only care about activity classes
                if (!isExportedActivityClass(sc.getName()) && !baseActivitySet.contains(sc))
                    continue;

                // TODO Remove this debug logging
                if (sc.getName().contains("draw.dialog.LoginDialog"))
                    Logger.debug("Here");

                findWidgetsMappingsOnSootClass(sc);
            }
        }
    }

    /**
     * Finds the widgets and ownership mappings on given SootClass
     * @param sc The soot class to analyze
     */
    private void findWidgetsMappingsOnSootClass(SootClass sc) {
        // Check for Facebook Login Button
        for (SootField sootField : sc.getFields()) {
            if (sootField.getType().toString().equalsIgnoreCase("com.facebook.login.widget.LoginButton")) {
                potentialLoginMap.put(sc,"Facebook");
            }
        }

        for (SootMethod sm : sc.getMethods()) {
            if (!sm.isConcrete())
                continue;

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

                            if (rightOp instanceof NewExpr) {
                                // Here we capture dynamically created widgets
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
                                        Local widgetLocal;
                                        Local newLocal = analyzerUtils.reassignsLocal(usePair);
                                        if (newLocal==null) {
                                            widgetLocal = (Local)leftOp;
                                        } else {
                                            widgetLocal = newLocal;
                                        }

                                        List<InvokeExpr> invokeExprList = analyzerUtils.getInvokeExprOnLocal(units,widgetLocal);

                                        if (isEditTextWidget(newSootClass)) {
                                            EditWidgetNode newEditWidgetNode = createNewEditTextWidget(invokeExprList);
                                            if (newEditWidgetNode != null) {
                                                editTextWidgetList.add(newEditWidgetNode);
                                                ownershipEdges.put(sc, newEditWidgetNode);
                                            }
                                        }
                                        // Clickable widgets
                                        else {
                                            ClickWidgetNode newClickWidgetNode = createNewClickWidget(invokeExprList);
                                            if (newClickWidgetNode != null) {
                                                clickWidgetNodeList.add(newClickWidgetNode);
                                                ownershipEdges.put(sc, newClickWidgetNode);
                                            }
                                        }
                                    }
                                }
                                // Google sign-in options
                                else if (newSootClass.getName().contains("GoogleSignInOptions")) {
                                    potentialLoginMap.put(sc, "Google");
                                }
                                // Activity hierarchy
                                else {
                                    for (SootClass classForFurtherAnalysis : sourceClassesForFurtherAnalysis.keySet()) {
                                        if (newSootClass.getName().equalsIgnoreCase(classForFurtherAnalysis.getName())) {
                                            classMapForActivityHierarchy.put(classForFurtherAnalysis, sc);
                                        }
                                    }
                                }
                            }

                            // Here we collect widgets that are mapped to the XML files
                            else if (rightOp instanceof InvokeExpr) {
                                // collect resource Id
                                if (invokesFindViewById((InvokeExpr) rightOp)) {
                                    int resourceId = -1;
                                    AndroidLayoutControl userControl = null;

                                    for (int i=0; i<((InvokeExpr) rightOp).getArgCount(); i++){
                                        String arg = ((InvokeExpr)rightOp).getArg(i).toString();
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
                                            SimpleLocalDefs simoleLocalDefs = new SimpleLocalDefs(unitGraph);

                                            // Here we have a timeout for combinedDUAnalysis as it may not converge in some cases
                                            List<UnitValueBoxPair> usePair;
                                            try {
                                                usePair = runCombinedDUAnalysis(u, unitGraph, 5);
                                            } catch (Exception e) {
                                                SimpleLocalUses simpleLocalUses = new SimpleLocalUses(unitGraph, simoleLocalDefs);
                                                usePair = simpleLocalUses.getUsesOf(u);
                                            }

                                            // Check for cast expr
                                            Local widgetLocal;
                                            assert usePair != null;
                                            Local newLocal = analyzerUtils.reassignsLocal(usePair);
                                            if (newLocal==null) {
                                                widgetLocal = (Local)leftOp;
                                            } else {
                                                widgetLocal = newLocal;
                                            }
                                            List<InvokeExpr> invokeExprList = analyzerUtils.getInvokeExprOnLocal(units,widgetLocal);

                                            if (userControl instanceof EditTextControl) {
                                                EditWidgetNode newEditWidgetNode = createNewEditTextWidget(invokeExprList, resourceId);
                                                if (newEditWidgetNode != null) {
                                                    editTextWidgetList.add(newEditWidgetNode);
                                                    ownershipEdges.put(sc, newEditWidgetNode);
                                                }
                                            }
                                            // Clickable widgets
                                            else {
                                                ClickWidgetNode newClickWidgetNode = createNewClickWidget(invokeExprList, resourceId);
                                                if (newClickWidgetNode != null) {
                                                    clickWidgetNodeList.add(newClickWidgetNode);
                                                    ownershipEdges.put(sc, newClickWidgetNode);
                                                }
                                            }
                                        } else
                                            Logger.warn("[LayoutParser] Cannot find the widgets in findViewById...");
                                    }
                                }
                            }
                        }

                        if (stmt.containsInvokeExpr()) {
                            InvokeExpr inv = stmt.getInvokeExpr();
                            if (invokesFacebookLogin(inv)){
                                potentialLoginMap.put(sc,"Facebook");
                            } else if (invokesGoogleLogin(inv)) {
                                potentialLoginMap.put(sc,"Google");
                            }
                        }
                    }
                }
            } catch (RuntimeException e) {
                Logger.warn(e.toString());
            }
        }
    }

    /**
     * Finds the mappings between classes and their respective layout files
     */
    private void findClassLayoutMappings() {
        // Check base activities first, as their sentContentView methods might be overridden
        for (SootClass sc : baseActivitySet) {
            for (SootMethod sm : sc.getMethods()) {
                if (!sm.isConcrete())
                    continue;

                try {
                    checkAndAddClassLayoutMappings(sm, sc,true);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }

        for (SootClass sc : Scene.v().getApplicationClasses()) {

            // TODO Remove this method, used for debug only
            if (sc.getName().equalsIgnoreCase("com.ebay.app.common.activities.c"))
                Logger.debug("Here");

            // We only care about the exported activities
            if (!isExportedActivityClass(sc.getName()))
                continue;

            for (SootMethod sm : sc.getMethods()) {
                if (!sm.isConcrete())
                    continue;

                try{
                    // Here we add the method wrappers (for findViewById and setContentView)
                    addMethodWrappers(sm);
                    // Check for class layout mappings
                    checkAndAddClassLayoutMappings(sm, sc,false);
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
