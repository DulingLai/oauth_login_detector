package dulinglai.android.alode.analyzers;

import dulinglai.android.alode.graphBuilder.widgetNodes.ClickWidgetNode;
import dulinglai.android.alode.graphBuilder.widgetNodes.EditWidgetNode;
import dulinglai.android.alode.resources.androidConstants.AndroidSootClassConstants;
import dulinglai.android.alode.resources.resources.LayoutFileParser;
import dulinglai.android.alode.resources.resources.controls.AndroidLayoutControl;
import dulinglai.android.alode.resources.resources.controls.EditTextControl;
import dulinglai.android.alode.resources.resources.controls.LayoutControl;
import dulinglai.android.alode.sootData.values.ResourceValueProvider;
import heros.solver.Pair;
import org.pmw.tinylog.Logger;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;
import soot.util.Chain;
import soot.util.HashMultiMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A widget analyzer that creates widgets by analyzing the jimple file
 */
public class FastJimpleAnalyzer extends AbstractJimpleAnalyzer {

    private static final String ANALYZER = "FastJimpleAnalyzer";

    public FastJimpleAnalyzer(Set<SootClass> entryPointClasses, Set<String> activityList,
                              LayoutFileParser layoutFileParser, ResourceValueProvider resourceValueProvider)
            throws IOException {
        super(entryPointClasses, 0, activityList, layoutFileParser, resourceValueProvider);
        this.editTextWidgetList = new ArrayList<>();
        this.clickWidgetNodeList = new ArrayList<>();
        this.ownershipEdges = new HashMultiMap<>();
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

                findWidgetsMappingsOnSootClass(sc);
            }
        }
    }

    /**
     * Finds the widgets and ownership mappings on given SootClass
     * @param sc The soot class to analyze
     */
    private void findWidgetsMappingsOnSootClass(SootClass sc) {
        // TODO remove debug log
        if (sc.getName().equalsIgnoreCase("com.mcdonalds.account.activity.ForgotPasswordActivity"))
            Logger.debug("Here");

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
                    if (u instanceof Stmt) {
                        Stmt stmt = (Stmt) u;

                        if (stmt instanceof AssignStmt) {
                            Value rightOp = ((AssignStmt) stmt).getRightOp();
                            Value leftOp = ((AssignStmt) stmt).getLeftOp();

                            if (rightOp instanceof NewExpr) {
                                // Here we capture dynamically created widgets
                                SootClass newSootClass = ((NewExpr) rightOp).getBaseType().getSootClass();
                                if (assignsNewWidget(newSootClass)) {
                                    collectDynamicWidget(leftOp, u, b, newSootClass, sc);
                                }
                                // Google sign-in options
                                else if (newSootClass.getName().contains("GoogleSignInOptions")) {
                                    potentialLoginMap.put(sc, "Google");
                                }
                            } // Here we collect widgets that are mapped to the XML files
                            else if (rightOp instanceof InvokeExpr) {
                                collectXMLWidget(leftOp, rightOp, b, u, sc);
                            } else if (rightOp instanceof FieldRef) {
                                // check if the field ref of the widget is assigned to a variable
                                SootField sootField = ((FieldRef) rightOp).getField();
                                if (fieldWidgetMap.containsKey(sootField)) {
                                    int resourceId = this.fieldWidgetMap.get(sootField);
                                    LayoutControl userControl = this.layoutFileParser.findUserControlById(resourceId);
                                    collectXMLWidgetProperties(leftOp, u, resourceId, userControl, b, sc);
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
     * Collects all XML-based widgets
     * @param leftOp The left side of the assign expr
     * @param rightOp The right side of the assign expr
     * @param b The body of the method
     * @param u The unit
     * @param sc The soot class
     */
    private void collectXMLWidget(Value leftOp, Value rightOp, Body b, Unit u, SootClass sc) {
        // collect resource Id
        if (invokesFindViewById((InvokeExpr) rightOp)) {
            int resourceId = -1;
            AndroidLayoutControl userControl = null;

            List<Value> args = ((InvokeExpr) rightOp).getArgs();

            // Find the widget and its resource id
            for (Value arg : args) {
                if (arg.getType().toString().equals("int")) {
                    if (arg.toString().matches("-?\\d+")) {
                        resourceId = Integer.parseInt(arg.toString());
                        userControl = this.layoutFileParser.findUserControlById(resourceId);
                        break;
                    } else if (arg instanceof Local) {
                        BriefUnitGraph unitGraph = new BriefUnitGraph(b);
                        SimpleLocalDefs simpleLocalDefs = new SimpleLocalDefs(unitGraph);

                        List<Unit> defPair = simpleLocalDefs.getDefsOfAt((Local) arg, u);
                        for (Unit def : defPair) {
                            if (def instanceof AssignStmt) {
                                AssignStmt assignStmt = (AssignStmt) def;
                                if (assignStmt.getRightOp() instanceof FieldRef) {
                                    SootField fieldRef = ((FieldRef) assignStmt.getRightOp()).getField();
                                    if (fieldRef.getDeclaringClass().getName().contains("R$id")) {
                                        resourceId = resourceValueProvider.getResouceIdByString(fieldRef.getName());
                                        userControl = this.layoutFileParser.findUserControlById(resourceId);
                                        break;
                                    }
                                }
                            }
                        }
//                                                IFDSTabulationProblem<Unit, Pair<Value, Set<DefinitionStmt>>, SootMethod,
//                                                        InterproceduralCFG<Unit,SootMethod>> problem =
//                                                        (IFDSTabulationProblem) new IFDSReachingDefinitions(icfg);
//                                                JimpleIFDSSolver<Pair<Value, Set<DefinitionStmt>>,
//                                                        InterproceduralCFG<Unit, SootMethod>> solver = new JimpleIFDSSolver<>(problem, true);
//                                                solver.solve();
//                                                Set<Pair<Value, Set<DefinitionStmt>>> results = new HashSet<>();
                    }
                }
            }

            // An XML-based widget is found
            if (userControl != null) {
                collectXMLWidgetProperties(leftOp, u, resourceId, userControl, b, sc);
            }
        }
    }

    /**
     * Collects the XML widgets properties set in the Java class
     * @param leftOp The left side of the assign expr
     * @param u The unit
     * @param resourceId The resource id of the widget
     * @param userControl The widget found from XML file
     * @param b The body of the Soot method
     * @param sc The Soot class
     */
    private void collectXMLWidgetProperties(Value leftOp, Unit u, int resourceId, LayoutControl userControl, Body b, SootClass sc) {
        // Def-use analysis to find the attr
        if (leftOp instanceof Local) {
            BriefUnitGraph unitGraph = new BriefUnitGraph(b);
            SimpleLocalDefs simpleLocalDefs = new SimpleLocalDefs(unitGraph);

            // Here we have a timeout for combinedDUAnalysis as it may not converge in some cases
            List<UnitValueBoxPair> usePair;
            try {
                usePair = runCombinedDUAnalysis(u, unitGraph, 5);
            } catch (Exception e) {
                SimpleLocalUses simpleLocalUses = new SimpleLocalUses(unitGraph, simpleLocalDefs);
                usePair = simpleLocalUses.getUsesOf(u);
            }

            // Check for cast expr
            Local widgetLocal;
            Unit widgetUnit;
            Pair<Local, Unit> newLocalPair = analyzerUtils.reassignsLocal(usePair);
            if (newLocalPair != null) {
                widgetLocal = newLocalPair.getO1();
                widgetUnit = newLocalPair.getO2();
            } else {
                widgetLocal = (Local)leftOp;
                widgetUnit = u;
            }

            // Check for field assignment
            SootField widgetField = assignWidgetField(widgetLocal, widgetUnit, simpleLocalDefs, unitGraph);
            if (widgetField!=null) {
                // maintain a map
                fieldWidgetMap.put(widgetField, resourceId);
            }

            // Gets a list of invoke expr on the Local variable
            List<InvokeExpr> invokeExprList = getInvokeExprOnLocal(widgetLocal, widgetUnit, simpleLocalDefs, unitGraph);

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

    /**
     * Collects the dynamic registered widgets
     * @param leftOp The left side of the assign statement
     * @param u The unit
     * @param b The method body
     * @param newSootClass The new soot class
     * @param sc The soot class
     */
    private void collectDynamicWidget(Value leftOp, Unit u, Body b, SootClass newSootClass, SootClass sc) {
        // Check the usage of this local variable
        BriefUnitGraph unitGraph = new BriefUnitGraph(b);
        SimpleLocalDefs localDefs = new SimpleLocalDefs(unitGraph);
        SimpleLocalUses localUses = new SimpleLocalUses(unitGraph, localDefs);

        if (leftOp instanceof Local) {
            // Editable widgets
            List<UnitValueBoxPair> usePair = localUses.getUsesOf(u);
            // Check for cast expr
            Local widgetLocal;
            Unit widgetUnit;
            assert usePair != null;
            Pair<Local, Unit> newLocalPair = analyzerUtils.reassignsLocal(usePair);
            if (newLocalPair != null) {
                widgetLocal = newLocalPair.getO1();
                widgetUnit = newLocalPair.getO2();
            } else {
                widgetLocal = (Local)leftOp;
                widgetUnit = u;
            }

            List<InvokeExpr> invokeExprList = getInvokeExprOnLocal(widgetLocal, widgetUnit, localDefs, unitGraph);

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
            // We only care about the exported activities
            if (!isExportedActivityClass(sc.getName()))
                continue;

            if (sc.getName().equalsIgnoreCase("com.pizzapizza.activity.LoginActivity"))
                Logger.debug("Here");

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

    /**
     * Gets the invoke expr on a given local
     * @param val The local variable
     * @param u The jimple unit
     * @param defs The local defs
     * @param unitGraph The unit graph
     * @return A list of invoke expr if found, otherwise null
     */
    List<InvokeExpr> getInvokeExprOnLocal(Local val, Unit u, SimpleLocalDefs defs, UnitGraph unitGraph) {
        List<InvokeExpr> invokeExprList = new ArrayList<>();

        SimpleLocalUses simpleLocalUses = new SimpleLocalUses(unitGraph, defs);
        List<UnitValueBoxPair> usePairs = simpleLocalUses.getUsesOf(u);
        for (UnitValueBoxPair usePair : usePairs) {
            Unit useUnit = usePair.getUnit();
            if (useUnit instanceof Stmt) {
                Stmt newStmt = (Stmt) useUnit;
                if (newStmt.containsInvokeExpr()) {
                    InvokeExpr inv = newStmt.getInvokeExpr();
                    if (inv instanceof VirtualInvokeExpr) {
                        Value baseValue = ((VirtualInvokeExpr) inv).getBase();
                        if (baseValue instanceof Local) {
                            if (baseValue.equals(val)) {
                                int i = 0;
                                for (Value arg : inv.getArgs()) {
                                    if (arg instanceof IntConstant || arg instanceof StringConstant) {
                                        invokeExprList.add(inv);
                                    } else if (arg instanceof FloatConstant) {
                                        StringConstant floatValue = StringConstant.v(String.valueOf(((FloatConstant) arg).value));
                                        inv.setArg(i, floatValue);
                                        invokeExprList.add(inv);
                                    } else if (arg instanceof NullConstant) {
                                        continue;
                                    } else {
                                        Set<String> stringConst = Scene.v().getPointsToAnalysis().reachingObjects((Local) arg)
                                                .possibleStringConstants();
                                        if (stringConst != null && !stringConst.isEmpty()) {
                                            Value argValue = Jimple.v().newLocal(stringConst.iterator().next(),
                                                    RefType.v("java.lang.String"));
                                            inv.setArg(i, argValue);
                                            invokeExprList.add(inv);
                                        } else if (arg instanceof Local) {
                                            if (arg.getType().toString().equals("int")) {
                                                List<Unit> defUnits = defs.getDefsOfAt((Local) arg, useUnit);
                                                for (Unit defUnit : defUnits) {
                                                    Integer resId = stringResourceValueAnalysis(defUnit, defs);
                                                    if (resId != null) {
                                                        StringConstant argValue = StringConstant.v(resId.toString());
                                                        inv.setArg(i, argValue);
                                                        invokeExprList.add(inv);
                                                    }
                                                }
                                            } else if (androidCallbacks.contains(inv.getMethod().getParameterType(0).toString())) {
                                                if (AndroidSootClassConstants.SET_LISTENER_METHODS.contains(inv.getMethod().getName())) {
                                                    StringConstant argValue = StringConstant.v(arg.getType().toString());
                                                    inv.setArg(0, argValue);
                                                    invokeExprList.add(inv);
                                                }
                                            }
                                        } else {
                                            invokeExprList.add(inv);
                                        }
                                    }
                                    i++;
                                }
                            }
                        }
                    }
                }
            }
        }
        return invokeExprList;
    }

    /**
     * Simple string resource analysis to retrieve the string resource
     * @param u The Jimple unit
     * @param defs The simple local defs
     */
    public Integer stringResourceValueAnalysis(Unit u, SimpleLocalDefs defs) {
        if (u instanceof Stmt) {
            Stmt stmt = (Stmt) u;
            if (stmt.containsInvokeExpr()) {
                InvokeExpr inv = stmt.getInvokeExpr();
                if ((inv instanceof VirtualInvokeExpr)
                        && inv.getMethod().getReturnType().toString().equalsIgnoreCase("java.lang.String")) {
                    for (Value arg : inv.getArgs()) {
                        if (arg.getType().toString().equals("int")) {
                            List<Unit> defUnits = defs.getDefsOfAt((Local) arg, u);
                            for (Unit defUnit : defUnits) {
                                Integer resId = assignStringResource(defUnit);
                                if (resId!=null)
                                    return resId;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets the resource id of the string resource if the string resource is assigned in a given unit
     * @param u The unit
     * @return The string resource id, null if not found
     */
    public Integer assignStringResource(Unit u) {
        if (u instanceof Stmt) {
            Stmt stmt = (Stmt) u;
            if (stmt instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) stmt;
                if (assignStmt.getRightOp() instanceof FieldRef) {
                    SootField fieldRef = ((FieldRef) assignStmt.getRightOp()).getField();
                    if (fieldRef.getDeclaringClass().getName().contains("R$string")) {
                        return resourceValueProvider.getResouceIdByString(fieldRef.getName());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks if the widget variable is assigned to a field
     * @param l The local variable of the widget
     * @param u The def unit of the widget variable
     * @param defs The local defs
     * @return The assigned field if found, otherwise null
     */
    public SootField assignWidgetField(Local l, Unit u, SimpleLocalDefs defs, UnitGraph unitGraph) {
        SimpleLocalUses simpleLocalUses = new SimpleLocalUses(unitGraph, defs);
        List<UnitValueBoxPair> usePair = simpleLocalUses.getUsesOf(u);
        for (UnitValueBoxPair useUnitPair : usePair) {
            Unit useUnit = useUnitPair.getUnit();
            if (useUnit instanceof Stmt) {
                Stmt stmt = (Stmt) useUnit;
                if (stmt instanceof AssignStmt) {
                    Value leftOp = ((AssignStmt) stmt).getLeftOp();
                    if (leftOp instanceof FieldRef) {
                        return ((FieldRef) leftOp).getField();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void excludeEntryPoint(SootClass entryPoint) {
        // not supported
    }

}
