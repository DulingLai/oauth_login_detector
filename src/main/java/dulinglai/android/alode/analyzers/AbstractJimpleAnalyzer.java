package dulinglai.android.alode.analyzers;

import dulinglai.android.alode.analyzers.CallbackDefinition.CallbackType;
import dulinglai.android.alode.analyzers.filters.ICallbackFilter;
import dulinglai.android.alode.entryPointCreators.SimulatedCodeElementTag;
import dulinglai.android.alode.graphBuilder.ICFG;
import dulinglai.android.alode.graphBuilder.widgetNodes.AbstractWidgetNode;
import dulinglai.android.alode.graphBuilder.widgetNodes.ClickWidgetNode;
import dulinglai.android.alode.graphBuilder.widgetNodes.EditWidgetNode;
import dulinglai.android.alode.resources.androidConstants.AndroidSootClassConstants;
import dulinglai.android.alode.resources.androidConstants.ComponentConstants;
import dulinglai.android.alode.resources.resources.LayoutFileParser;
import dulinglai.android.alode.resources.resources.controls.AndroidLayoutControl;
import dulinglai.android.alode.resources.resources.controls.EditTextControl;
import dulinglai.android.alode.resources.resources.controls.LayoutControl;
import dulinglai.android.alode.sootData.values.IValueProvider;
import dulinglai.android.alode.sootData.values.ResourceValueProvider;
import dulinglai.android.alode.sootData.values.SimpleConstantValueProvider;
import dulinglai.android.alode.utils.androidUtils.SystemClassHandler;
import dulinglai.android.alode.utils.sootUtils.ResourceUtils;
import dulinglai.android.alode.utils.sootUtils.SootMethodRepresentationParser;
import heros.solver.Pair;
import org.pmw.tinylog.Logger;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.CombinedDUAnalysis;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;
import soot.util.Chain;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public abstract class AbstractJimpleAnalyzer {

    private static final String SIG_CAR_CREATE = "<android.car.Car: android.car.Car createCar(android.content.Context,android.content.ServiceConnection)>";

    protected final SootClass scContext = Scene.v().getSootClassUnsafe("android.content.Context");

    protected final SootClass scBroadcastReceiver = Scene.v()
            .getSootClassUnsafe(ComponentConstants.BROADCASTRECEIVERCLASS);
    protected final SootClass scServiceConnection = Scene.v()
            .getSootClassUnsafe(ComponentConstants.SERVICECONNECTIONINTERFACE);

    protected final SootClass scFragmentTransaction = Scene.v().getSootClassUnsafe("android.app.FragmentTransaction");
    protected final SootClass scFragment = Scene.v().getSootClassUnsafe(ComponentConstants.FRAGMENTCLASS);

    protected final SootClass scSupportFragmentTransaction = Scene.v()
            .getSootClassUnsafe("android.support.v4.app.FragmentTransaction");
    protected final SootClass scSupportFragment = Scene.v().getSootClassUnsafe("android.support.v4.app.Fragment");

    protected final Set<SootClass> entryPointClasses;
    protected final Set<String> androidCallbacks;
    protected final int maxCallbacksPerComponent;

    protected final MultiMap<SootClass, CallbackDefinition> callbackMethods = new HashMultiMap<>();
    protected MultiMap<SootClass, CallbackDefinition> uicallbacks = new HashMultiMap<>();
    protected final Set<SootClass> dynamicManifestComponents = new HashSet<>();
    protected final MultiMap<SootClass, SootClass> fragmentClasses = new HashMultiMap<>();
    protected final Map<SootClass, Integer> fragmentIDs = new HashMap<>();

    protected final List<ICallbackFilter> callbackFilters = new ArrayList<>();
    protected final Set<SootClass> excludedEntryPoints = new HashSet<>();

    protected IValueProvider valueProvider = new SimpleConstantValueProvider();

    protected Set<String> activityList;
    protected MultiMap<SootClass, Pair<Unit, SootMethod>> iccUnitsForWidgetAnalysis;

    LayoutFileParser layoutFileParser;
    final ResourceValueProvider resourceValueProvider;
    protected final MultiMap<SootClass, Integer> layoutClasses = new HashMultiMap<>();

    protected ICFG icfg;

    protected final MultiMap<SootClass, String> potentialLoginMap = new HashMultiMap<>();
    protected Set<SootMethod> findViewMethod = new HashSet<>();
    protected MultiMap<SootClass, SootClass> baseActivityMapping = new HashMultiMap<>();
    protected Set<SootClass> baseActivitySet = new HashSet<>();

    Map<SootClass, String> setContentViewWrapperMap = new HashMap<>();
    Map<SootField, Integer> fieldWidgetMap = new HashMap<>();

    protected List<EditWidgetNode> editTextWidgetList;
    protected List<ClickWidgetNode> clickWidgetNodeList;
    protected MultiMap<SootClass, AbstractWidgetNode> ownershipEdges;

    public AbstractJimpleAnalyzer(Set<SootClass> entryPointClasses, int maxCallbacksPerComponent,
                                  Set<String> activityList, LayoutFileParser layoutFileParser,
                                  ResourceValueProvider resourceValueProvider,
                                  MultiMap<SootClass, Pair<Unit, SootMethod>> iccUnitsForWidgetAnalysis) throws IOException {
        this(entryPointClasses, "AndroidCallbacks.txt", maxCallbacksPerComponent, activityList,
                layoutFileParser, resourceValueProvider, iccUnitsForWidgetAnalysis);
    }

    public AbstractJimpleAnalyzer(Set<SootClass> entryPointClasses,
                                  String callbackFile, int maxCallbacksPerComponent,
                                  Set<String> activityList, LayoutFileParser layoutFileParser,
                                  ResourceValueProvider resourceValueProvider,
                                  MultiMap<SootClass, Pair<Unit, SootMethod>> iccUnitsForWidgetAnalysis) throws IOException {
        this(entryPointClasses, loadAndroidCallbacks(callbackFile), maxCallbacksPerComponent, activityList,
                layoutFileParser, resourceValueProvider, iccUnitsForWidgetAnalysis);
    }

    public AbstractJimpleAnalyzer(Set<SootClass> entryPointClasses,
                                  Set<String> androidCallbacks, int maxCallbacksPerComponent,
                                  Set<String> activityList, LayoutFileParser layoutFileParser,
                                  ResourceValueProvider resourceValueProvider,
                                  MultiMap<SootClass, Pair<Unit, SootMethod>> iccUnitsForWidgetAnalysis) {
        this.entryPointClasses = entryPointClasses;
        this.androidCallbacks = androidCallbacks;
        this.maxCallbacksPerComponent = maxCallbacksPerComponent;
        this.activityList = activityList;
        this.layoutFileParser = layoutFileParser;
        this.resourceValueProvider = resourceValueProvider;
        this.iccUnitsForWidgetAnalysis = iccUnitsForWidgetAnalysis;
    }

    public void constructIcfg() {
        this.icfg = new ICFG();
    }

    /**
     * Loads the set of interfaces that are used to implement Android callback
     * handlers from a file on disk
     *
     * @param androidCallbackFile
     *            The file from which to load the callback definitions
     * @return A set containing the names of the interfaces that are used to
     *         implement Android callback handlers
     */
    private static Set<String> loadAndroidCallbacks(String androidCallbackFile) throws IOException {
        String fileName = androidCallbackFile;
        if (!new File(fileName).exists()) {
            fileName = "../soot-infoflow-android/AndroidCallbacks.txt";
            if (!new File(fileName).exists()) {
                return loadAndroidCallbacks(
                        new InputStreamReader(ResourceUtils.getResourceStream("/AndroidCallbacks.txt")));
            }
        }
        return loadAndroidCallbacks(new FileReader(fileName));
    }

    /**
     * Loads the set of interfaces that are used to implement Android callback
     * handlers from a file on disk
     *
     * @param reader
     *            A file reader
     * @return A set containing the names of the interfaces that are used to
     *         implement Android callback handlers
     */
    public static Set<String> loadAndroidCallbacks(Reader reader) throws IOException {
        Set<String> androidCallbacks = new HashSet<String>();
        BufferedReader bufReader = new BufferedReader(reader);
        try {
            String line;
            while ((line = bufReader.readLine()) != null)
                if (!line.isEmpty())
                    androidCallbacks.add(line);

        } finally {
            bufReader.close();
        }
        return androidCallbacks;
    }

    /**
     * Collects the callback methods for all Android default handlers implemented in
     * the source code.
     */
    public void analyzeJimpleClasses() {
        // Initialize the filters
        for (ICallbackFilter filter : callbackFilters)
            filter.reset();
    }

    /**
     * Analyzes the given method and looks for callback registrations
     *
     * @param lifecycleElement
     *            The lifecycle element (activity, etc.) with which to associate the
     *            found callbacks
     * @param method
     *            The method in which to look for callbacks
     */
    protected void analyzeMethodForCallbackRegistrations(SootClass lifecycleElement, SootMethod method) {
        // Do not analyze system classes
        if (SystemClassHandler.isClassInSystemPackage(method.getDeclaringClass().getName()))
            return;
        if (!method.isConcrete())
            return;

        // Iterate over all statement and find callback registration methods
        Set<SootClass> callbackClasses = new HashSet<>();
        for (Unit u : method.retrieveActiveBody().getUnits()) {
            Stmt stmt = (Stmt) u;
            // Callback registrations are always instance invoke expressions
            if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
                InstanceInvokeExpr iinv = (InstanceInvokeExpr) stmt.getInvokeExpr();

                final SootMethodRef mref = iinv.getMethodRef();
                for (int i = 0; i < iinv.getArgCount(); i++) {
                    final Type type = mref.parameterType(i);
                    if (!(type instanceof RefType))
                        continue;
                    String param = type.toString();
                    if (androidCallbacks.contains(param)) {
                        Value arg = iinv.getArg(i);

                        // This call must be to a system API in order to
                        // register an OS-level callback
                        if (!SystemClassHandler.isClassInSystemPackage(iinv.getMethod().getDeclaringClass().getName()))
                            continue;

                        // We have a formal parameter type that corresponds to one of the Android
                        // callback interfaces. Look for definitions of the parameter to estimate the
                        // actual type.
                        if (arg instanceof Local) {
                            Set<Type> possibleTypes = Scene.v().getPointsToAnalysis().reachingObjects((Local) arg)
                                    .possibleTypes();
                            for (Type possibleType : possibleTypes) {
                                RefType baseType;
                                if (possibleType instanceof RefType)
                                    baseType = (RefType) possibleType;
                                else if (possibleType instanceof AnySubType)
                                    baseType = ((AnySubType) possibleType).getBase();
                                else {
                                    Logger.warn("Unsupported type detected in callback analysis");
                                    continue;
                                }

                                SootClass targetClass = baseType.getSootClass();
                                if (!SystemClassHandler.isClassInSystemPackage(targetClass.getName()))
                                    callbackClasses.add(targetClass);
                            }

                            // If we don't have pointsTo information, we take
                            // the type of the local
                            if (possibleTypes.isEmpty()) {
                                Type argType = arg.getType();
                                RefType baseType;
                                if (argType instanceof RefType)
                                    baseType = (RefType) argType;
                                else if (argType instanceof AnySubType)
                                    baseType = ((AnySubType) argType).getBase();
                                else {
                                    Logger.warn("Unsupported type detected in callback analysis");
                                    continue;
                                }

                                SootClass targetClass = baseType.getSootClass();
                                if (!SystemClassHandler.isClassInSystemPackage(targetClass.getName()))
                                    callbackClasses.add(targetClass);
                            }
                        }
                    }
                }
            }
        }

        // Analyze all found callback classes
        for (SootClass callbackClass : callbackClasses)
            analyzeClassInterfaceCallbacks(callbackClass, callbackClass, lifecycleElement);
    }

    /**
     * Checks whether all filters accept the association between the callback class
     * and its parent component
     *
     * @param lifecycleElement
     *            The hosting component's class
     * @param targetClass
     *            The class implementing the callbacks
     * @return True if all filters accept the given component-callback mapping,
     *         otherwise false
     */
    private boolean filterAccepts(SootClass lifecycleElement, SootClass targetClass) {
        for (ICallbackFilter filter : callbackFilters)
            if (!filter.accepts(lifecycleElement, targetClass))
                return false;
        return true;
    }

    /**
     * Checks whether all filters accept the association between the callback method
     * and its parent component
     *
     * @param lifecycleElement
     *            The hosting component's class
     * @param targetMethod
     *            The method implementing the callback
     * @return True if all filters accept the given component-callback mapping,
     *         otherwise false
     */
    private boolean filterAccepts(SootClass lifecycleElement, SootMethod targetMethod) {
        for (ICallbackFilter filter : callbackFilters)
            if (!filter.accepts(lifecycleElement, targetMethod))
                return false;
        return true;
    }

    /**
     * Checks whether the given method dynamically registers a new broadcast
     * receiver
     *
     * @param method
     *            The method to check
     */
    protected void analyzeMethodForDynamicBroadcastReceiver(SootMethod method) {
        // Do not analyze system classes
        if (SystemClassHandler.isClassInSystemPackage(method.getDeclaringClass().getName()))
            return;
        if (!method.isConcrete() || !method.hasActiveBody())
            return;

        final FastHierarchy fastHierarchy = Scene.v().getFastHierarchy();
        final RefType contextType = scContext.getType();
        for (Unit u : method.getActiveBody().getUnits()) {
            Stmt stmt = (Stmt) u;
            if (stmt.containsInvokeExpr()) {
                final InvokeExpr iexpr = stmt.getInvokeExpr();
                final SootMethodRef methodRef = iexpr.getMethodRef();
                if (methodRef.name().equals("registerReceiver") && iexpr.getArgCount() > 0
                        && fastHierarchy.canStoreType(methodRef.declaringClass().getType(), contextType)) {
                    Value br = iexpr.getArg(0);
                    if (br.getType() instanceof RefType) {
                        RefType rt = (RefType) br.getType();
                        if (!SystemClassHandler.isClassInSystemPackage(rt.getSootClass().getName()))
                            dynamicManifestComponents.add(rt.getSootClass());
                    }
                }
            }
        }
    }

    /**
     * Checks whether the given method dynamically registers a new service
     * connection
     *
     * @param method
     *            The method to check
     */
    protected void analyzeMethodForServiceConnection(SootMethod method) {
        // Do not analyze system classes
        if (SystemClassHandler.isClassInSystemPackage(method.getDeclaringClass().getName()))
            return;
        if (!method.isConcrete() || !method.hasActiveBody())
            return;

        for (Unit u : method.getActiveBody().getUnits()) {
            Stmt stmt = (Stmt) u;
            if (stmt.containsInvokeExpr()) {
                final InvokeExpr iexpr = stmt.getInvokeExpr();
                final SootMethodRef methodRef = iexpr.getMethodRef();
                if (methodRef.getSignature().equals(SIG_CAR_CREATE)) {
                    Value br = iexpr.getArg(1);

                    // We need all possible types for the parameter
                    if (br instanceof Local && Scene.v().hasPointsToAnalysis()) {
                        PointsToSet pts = Scene.v().getPointsToAnalysis().reachingObjects((Local) br);
                        for (Type tp : pts.possibleTypes()) {
                            if (tp instanceof RefType) {
                                RefType rt = (RefType) tp;
                                if (!SystemClassHandler.isClassInSystemPackage(rt.getSootClass().getName()))
                                    dynamicManifestComponents.add(rt.getSootClass());
                            }
                        }
                    }

                    // Just to be sure, also add the declared type
                    if (br.getType() instanceof RefType) {
                        RefType rt = (RefType) br.getType();
                        if (!SystemClassHandler.isClassInSystemPackage(rt.getSootClass().getName()))
                            dynamicManifestComponents.add(rt.getSootClass());
                    }
                }
            }
        }
    }

    /**
     * Checks whether the given method executes a fragment transaction that creates
     * new fragment
     *
     * @author Goran Piskachev
     * @param method
     *            The method to check
     */
    protected void analyzeMethodForFragmentTransaction(SootClass lifecycleElement, SootMethod method) {
        if (scFragment == null || scFragmentTransaction == null)
            if (scSupportFragment == null || scSupportFragmentTransaction == null)
                return;
        if (!method.isConcrete() || !method.hasActiveBody())
            return;

        // first check if there is a Fragment manager, a fragment transaction
        // and a call to the add method which adds the fragment to the transaction
        boolean isFragmentManager = false;
        boolean isFragmentTransaction = false;
        boolean isAddTransaction = false;
        for (Unit u : method.getActiveBody().getUnits()) {
            Stmt stmt = (Stmt) u;
            if (stmt.containsInvokeExpr()) {
                final String methodName = stmt.getInvokeExpr().getMethod().getName();
                if (methodName.equals("getFragmentManager"))
                    isFragmentManager = true;
                else if (methodName.equals("beginTransaction"))
                    isFragmentTransaction = true;
                else if (methodName.equals("add") || methodName.equals("replace"))
                    isAddTransaction = true;
                else if (methodName.equals("inflate") && stmt.getInvokeExpr().getArgCount() > 1) {
                    Value arg = stmt.getInvokeExpr().getArg(0);
                    Integer fragmentID = valueProvider.getValue(method, stmt, arg, Integer.class);
                    if (fragmentID != null)
                        fragmentIDs.put(lifecycleElement, fragmentID);
                }
            }
        }

        // now get the fragment class from the second argument of the add method
        // from the transaction
        if (isFragmentManager && isFragmentTransaction && isAddTransaction)
            for (Unit u : method.getActiveBody().getUnits()) {
                Stmt stmt = (Stmt) u;
                if (stmt.containsInvokeExpr()) {
                    InvokeExpr invExpr = stmt.getInvokeExpr();
                    if (invExpr instanceof InstanceInvokeExpr) {
                        InstanceInvokeExpr iinvExpr = (InstanceInvokeExpr) invExpr;

                        // Make sure that we referring to the correct class and
                        // method
                        isFragmentTransaction = scFragmentTransaction != null && Scene.v().getFastHierarchy()
                                .canStoreType(iinvExpr.getBase().getType(), scFragmentTransaction.getType());
                        isFragmentTransaction |= scSupportFragmentTransaction != null && Scene.v().getFastHierarchy()
                                .canStoreType(iinvExpr.getBase().getType(), scSupportFragmentTransaction.getType());
                        isAddTransaction = stmt.getInvokeExpr().getMethod().getName().equals("add")
                                || stmt.getInvokeExpr().getMethod().getName().equals("replace");

                        if (isFragmentTransaction && isAddTransaction) {
                            // We take all fragments passed to the method
                            for (int i = 0; i < stmt.getInvokeExpr().getArgCount(); i++) {
                                Value br = stmt.getInvokeExpr().getArg(i);

                                // Is this a fragment?
                                if (br.getType() instanceof RefType) {
                                    RefType rt = (RefType) br.getType();

                                    boolean addFragment = scFragment != null
                                            && Scene.v().getFastHierarchy().canStoreType(rt, scFragment.getType());
                                    addFragment |= scSupportFragment != null && Scene.v().getFastHierarchy()
                                            .canStoreType(rt, scSupportFragment.getType());
                                    if (addFragment)
                                        fragmentClasses.put(method.getDeclaringClass(), rt.getSootClass());
                                }
                            }
                        }
                    }
                }
            }
    }

    /**
     * Gets whether the call in the given statement can end up in the respective
     * method inherited from one of the given classes.
     *
     * @param stmt
     *            The statement containing the call sites
     * @param classNames
     *            The base classes in which the call can potentially end up
     * @return True if the given call can end up in a method inherited from one of
     *         the given classes, otherwise falae
     */
    protected boolean isInheritedMethod(Stmt stmt, String... classNames) {
        if (!stmt.containsInvokeExpr())
            return false;

        // Look at the direct callee
        SootMethod tgt = stmt.getInvokeExpr().getMethod();
        for (String className : classNames)
            if (className.equals(tgt.getDeclaringClass().getName()))
                return true;

        // If we have a callgraph, we can use that.
        if (Scene.v().hasCallGraph()) {
            Iterator<Edge> edgeIt = Scene.v().getCallGraph().edgesOutOf(stmt);
            while (edgeIt.hasNext()) {
                Edge edge = edgeIt.next();
                String targetClass = edge.getTgt().method().getDeclaringClass().getName();
                for (String className : classNames)
                    if (className.equals(targetClass))
                        return true;
            }
        }
        return false;
    }

    /**
     * Checks whether this invocation calls Android's Activity.setContentView method
     *
     * @param inv
     *            The invocaton to check
     * @return True if this invocation calls setContentView, otherwise false
     */
    protected boolean invokesSetContentView(InvokeExpr inv) {
        String methodName = SootMethodRepresentationParser.v()
                .getMethodNameFromSubSignature(inv.getMethodRef().getSubSignature().getString());
        if (!methodName.equals("setContentView"))
            return false;

        // In some cases, the bytecode points the invocation to the current
        // class even though it does not implement setContentView, instead
        // of using the superclass signature
        SootClass curClass = inv.getMethod().getDeclaringClass();
        while (curClass != null) {
            if (curClass.getName().equals("android.app.Activity")
                    || curClass.getName().equals("android.support.v7.app.ActionBarActivity")
                    || curClass.getName().equals("android.support.v7.app.AppCompatActivity"))
                return true;
            curClass = curClass.hasSuperclass() ? curClass.getSuperclass() : null;
        }
        return false;
    }

    /**
     * Checks whether this invocation calls Android's LayoutInflater.inflate method
     *
     * @param inv
     *            The invocaton to check
     * @return True if this invocation calls inflate, otherwise false
     */
    boolean invokesInflate(InvokeExpr inv) {
        String methodName = SootMethodRepresentationParser.v()
                .getMethodNameFromSubSignature(inv.getMethodRef().getSubSignature().getString());
        if (!methodName.equals("inflate"))
            return false;

        // In some cases, the bytecode points the invocation to the current
        // class even though it does not implement setContentView, instead
        // of using the superclass signature
        SootClass curClass = inv.getMethod().getDeclaringClass();
        while (curClass != null) {
            if (curClass.declaresMethod("android.view.View inflate(int,android.view.ViewGroup,boolean)"))
                return true;
            curClass = curClass.hasSuperclass() ? curClass.getSuperclass() : null;
        }
        return false;
    }

    /**
     * Checks whether this invocation calls Android's findViewById method
     * @param inv The invocaton to check
     * @return True if this invocation calls findViewById, otherwise false
     */
    boolean invokesFindViewById(InvokeExpr inv) {
        String methodName = SootMethodRepresentationParser.v()
                .getMethodNameFromSubSignature(inv.getMethodRef().getSubSignature().getString());
        String returnType = inv.getMethod().getReturnType().toString();

        if (returnType.equalsIgnoreCase("android.view.View") &&
                methodName.equalsIgnoreCase("findViewById")) return true;

        if (!findViewMethod.isEmpty()) {
            for (SootMethod fvMethod : findViewMethod) {
                if (returnType.equalsIgnoreCase("android.view.View") &&
                        inv.getMethod().getName().equals(fvMethod.getName())) return true;
            }
        }

        if (returnType.equalsIgnoreCase("android.view.View")) {
            for (Value arg : inv.getArgs()) {
                if (arg.getType().toString().equals("int")) {
                    if (arg.toString().matches("-?\\d+"))
                        return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks whether this invocation calls the Facebook login widgets
     * @param inv The invocaton to check
     * @return True if this invocation calls Facebook login
     */
    boolean invokesFacebookLogin(InvokeExpr inv) {
        return inv.getMethod().getDeclaringClass().getName().contains("com.facebook.login.widget.LoginButton");
    }

    /**
     * Checks whether this invocation calls the Google login widgets
     * @param inv The invocaton to check
     * @return True if this invocation calls Google login
     */
    boolean invokesGoogleLogin(InvokeExpr inv) {
        return inv.getMethod().getDeclaringClass().getName().contains("com.google.android.gms.auth.api.signin");
    }

    boolean isExportedActivityClass(String className) {
        return activityList.contains(className);
    }

    /**
     *  Find if the NewExpr is used to assign a new button
     * @param newClass The Soot RefType of the NewExpr
     * @return True if this NewExpr assigns a new button
     */
    protected boolean assignsNewWidget(SootClass newClass) {
        try {
            while (newClass != null) {
                if (newClass.getName().length()>=14) {
                    if (newClass.getName().substring(0, 14).equals("android.widget"))
                        return true;
                }
                newClass = newClass.hasSuperclass() ? newClass.getSuperclass() : null;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
        return false;
    }

    /**
     * Checks if the SootClass is an EditText class
     * @param newClass The soot class to check
     * @return True if it is an EditText class
     */
    protected boolean isEditTextWidget(SootClass newClass) {
        while (newClass != null) {
            if (newClass.getName().equals("android.widget.EditText"))
                return true;
            newClass = newClass.hasSuperclass() ? newClass.getSuperclass() : null;
        }
        return false;
    }

    /**
     * Creates edit text widgets
     * @param invokeExprList The invoke expr list that invokes on the widget local variable
     * @return An edit text widget
     */
    EditWidgetNode createNewEditTextWidget(List<InvokeExpr> invokeExprList) {
        return createNewEditTextWidget(invokeExprList, -1);
    }

    /**
     * Creates edit text widgets
     * @param invokeExprList The invoke expr list that invokes on the widget local variable
     * @param resourceId The resource id of the widget
     * @return An edit text widget
     */
    EditWidgetNode createNewEditTextWidget(List<InvokeExpr> invokeExprList, int resourceId) {
        String text = "";
        String contentDescription = "";
        String hint = "";
        int inputType = -1;

        for (InvokeExpr inv : invokeExprList) {
            SootMethod sootMethod = inv.getMethod();
            Value arg = inv.getArg(0);
            // Collect setInputType
            if (sootMethod.getName().equals("setInputType"))
                inputType = Integer.parseInt(arg.toString());

            // Collect setHint
            if (sootMethod.getName().equals("setHint") || sootMethod.getName().equals("setHintText")) {
                hint = arg.toString();
            }

            // Collect text
            if (sootMethod.getName().equals("setText"))
                text = arg.toString();

            // Collect Content Description
            if (sootMethod.getName().equals("setContentDescription"))
                contentDescription = arg.toString();
        }
        return new EditWidgetNode(resourceId,text,contentDescription,hint,inputType);
    }

    /**
     * Creates clickable widgets
     * @param invokeExprList The invoke expr list that invokes on the widget local variable
     * @return The clickable widget. If no click listener is attached, return null
     */
    ClickWidgetNode createNewClickWidget(List<InvokeExpr> invokeExprList) {
        return createNewClickWidget(invokeExprList,-1);
    }

    /**
     * Creates clickable widgets
     * @param invokeExprList The invoke expr list that invokes on the widget local variable
     * @param resourceId The resource id of the widget
     * @return The clickable widget. If no click listener is attached, return null
     */
    ClickWidgetNode createNewClickWidget(List<InvokeExpr> invokeExprList, int resourceId) {
        String text = "";
        ClickWidgetNode.EventType eventType = ClickWidgetNode.EventType.None;
        String clickListener = null;

        for (InvokeExpr inv : invokeExprList) {
            SootMethod sootMethod = inv.getMethod();
            // Collect clickable methods
            switch (sootMethod.getName()) {
                case "setClickable":
                case "setOnClickListener":
                case "setOnItemClickListener":
                    eventType = ClickWidgetNode.EventType.Click;
                    clickListener = inv.getArg(0).toString().replace("\"","");
                    break;
                case "setContextClickable":
                case "setOnContextClickListener":
                case "setOnCreateContextMenuListener":
                    eventType = ClickWidgetNode.EventType.ContextClick;
                    clickListener = inv.getArg(0).toString().replace("\"","");
                    break;
                case "setLongClickable":
                case "setOnLongClickListener":
                    eventType = ClickWidgetNode.EventType.LongClick;
                    clickListener = inv.getArg(0).toString().replace("\"","");
                    break;
                case "setOnTouchListener":
                    eventType = ClickWidgetNode.EventType.Touch;
                    clickListener = inv.getArg(0).toString().replace("\"","");
                    break;
                case "setOnKeyListener":
                    eventType = ClickWidgetNode.EventType.Key;
                    clickListener = inv.getArg(0).toString().replace("\"","");
                    break;
            }
        }
        if (eventType == ClickWidgetNode.EventType.None)
            return null;
        else
            return new ClickWidgetNode(resourceId,text,eventType, clickListener);
    }

    /**
     * Add the method wrappers for findViewById, and we also check for method wrappers
     * @param sm The method to analyze
     */
    void addMethodWrappers(SootMethod sm){
        // skip the original methods
        final List<String> methodNameList = Arrays.asList("findViewById", "setContentView");
        for (String methodName : methodNameList) {
            if (sm.getName().contains(methodName))
                return;
        }

        // check wrapper for findViewById
        if (analyzerUtils.isWrapperForFindViewById(sm)) {
            findViewMethod.add(sm);
        }
    }

    /**
     * Checks and adds the mappings between class file and layout file
     * @param sm The SootMethod to check for class layout mappings
     * @param isBaseActivity If we are checking a base activity
     */
    void checkAndAddClassLayoutMappings(SootMethod sm, SootClass sc, boolean isBaseActivity) {
        if (isBaseActivity) {
            Chain<Unit> units = sm.retrieveActiveBody().getUnits();
            for (Unit u : units) {
                if (u instanceof Stmt) {
                    Stmt stmt = (Stmt) u;
                    if (stmt.containsInvokeExpr()) {
                        InvokeExpr inv = stmt.getInvokeExpr();
                        // if it invokes setContentView or inflate
                        if (invokesSetContentView(inv) || invokesInflate(inv)) {
                            Value val = inv.getArg(0);
                            if (val.getType().toString().equals("int")) {
                                Integer intValue = valueProvider.getValue(sm, stmt, val, Integer.class);
                                if (intValue != null)
                                    this.layoutClasses.put(sc, intValue);
                                else
                                    intValueAnalysis(sc, sm, val, u);
                            }else if (val.getType().toString().equalsIgnoreCase("android.view.View")){
                                // if the parameter to setContentView is not a constant int, it could be a view

                            }
                        }
                    }
                }
            }
        } else {
            Chain<Unit> units = sm.retrieveActiveBody().getUnits();
            for (Unit u : units) {
                if (u instanceof Stmt) {
                    Stmt stmt = (Stmt) u;
                    if (stmt.containsInvokeExpr()) {
                        InvokeExpr inv = stmt.getInvokeExpr();
                        if (invokesSetContentView(inv) || invokesInflate(inv)) {
                            Value val = inv.getArg(0);
                            if (val.getType().toString().equals("int")) {
                                Integer intValue = valueProvider.getValue(sm, stmt, val, Integer.class);
                                if (intValue != null)
                                    this.layoutClasses.put(sc, intValue);
                                else {
                                    intValueAnalysis(sc,sm,val,u);
                                }
                            }
                        }
                    }
                }
            }

            for (SootClass baseActivity : baseActivityMapping.get(sc)) {
                if (sm.getSubSignature().equals(setContentViewWrapperMap.get(baseActivity))) {
                    Value returnValue = retrieveReturnValue(sm);
                    if (returnValue != null && returnValue.toString().matches("-?\\d+")) {
                        int resourceId = Integer.parseInt(returnValue.toString());
                        if (resourceId != 0 && resourceId != -1) {
                            this.layoutClasses.put(sc, resourceId);
                        }
                    } else if (returnValue instanceof Local) {
                        for (Unit u : units) {
                            if (u instanceof Stmt) {
                                Stmt stmt = (Stmt) u;
                                if (stmt instanceof ReturnStmt) {
                                    int resId = returnValueAnalysis(sm, u, (Local)returnValue);
                                    if (resId!=-1) {
                                        this.layoutClasses.put(sc, resId);
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    private Integer returnValueAnalysis(SootMethod sm, Unit u, Local local) {
        BriefUnitGraph unitGraph = new BriefUnitGraph(sm.retrieveActiveBody());
        SimpleLocalDefs simpleLocalDefs = new SimpleLocalDefs(unitGraph);

        List<Unit> defUnits = simpleLocalDefs.getDefsOfAt(local, u);

        for (Unit defUnit : defUnits) {
            if (defUnit instanceof Stmt) {
                Stmt defStmt = (Stmt) defUnit;
                if (defStmt instanceof AssignStmt) {
                    Value rightOp = ((AssignStmt)defStmt).getRightOp();
                    if (rightOp instanceof FieldRef) {
                        SootField fieldRef = ((FieldRef)rightOp).getField();
                        if (fieldRef.getDeclaringClass().getName().contains("R$layout")) {
                            return resourceValueProvider.getLayoutResourceId(fieldRef.getName());
                        }
                    }
                }
            }
        }
        return -1;
    }

    List<UnitValueBoxPair> runCombinedDUAnalysis(Unit u, UnitGraph unitGraph, int timeout) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<CombinedDUAnalysis> future = executor.submit(() -> new CombinedDUAnalysis(unitGraph));
        executor.shutdown();

        try {
            return future.get(timeout, TimeUnit.SECONDS).getUsesOf(u);
        } catch (TimeoutException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    /**
     * Analyze the return value of a method if the method is called on setContentView
     * @param sc The SootClass
     * @param sm The SootMethod
     * @param val The value which is supposed to be a Local variable
     * @param u The unit
     */
    void intValueAnalysis(SootClass sc, SootMethod sm, Value val, Unit u) {
        if (val instanceof Local) {
            // Inter-procedural analysis to reveal the value
            BriefUnitGraph unitGraph = new BriefUnitGraph(sm.retrieveActiveBody());
            SimpleLocalDefs localDefs = new SimpleLocalDefs(unitGraph);

            for (Unit defUnit : localDefs.getDefsOfAt((Local) val, u)) {
                Stmt defStmt = (Stmt) defUnit;
                if (defStmt.containsInvokeExpr()) {
                    InvokeExpr defInv = defStmt.getInvokeExpr();
                    if (defInv.getMethod().getReturnType().toString().equals("int")) {
                        SootMethod newMethod = defInv.getMethodRef().tryResolve();
                        Value returnValue = retrieveReturnValue(newMethod);
                        setContentViewWrapperMap.put(sc, newMethod.getSubSignature());
                        if (returnValue != null && returnValue.toString().matches("-?\\d+")) {
                            int resourceId = Integer.parseInt(returnValue.toString());
                            if (resourceId != 0 && resourceId != -1) {
                                this.layoutClasses.put(sc, resourceId);
                            }
                        }
                    }
                } else {
                    // If the definition is assigning a constant int value
                    Value defValue = defStmt.getUseBoxes().get(0).getValue();
                    if (defValue instanceof FieldRef) {
                        SootField field = ((FieldRef) defValue).getField();
                        if (field.getDeclaringClass().getName().contains("R$layout")) {
                            this.layoutClasses.put(sm.getDeclaringClass(), resourceValueProvider.getLayoutResourceId(field.getName()));
                        }
                    } else if (defValue.getType().toString().equals("int")) {
                        Integer defIntValue = valueProvider.getValue(sm, defStmt, defValue, Integer.class);
                        if (defIntValue != null) {
                            this.layoutClasses.put(sm.getDeclaringClass(), defIntValue);
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets the return value of this Soot Method
     * @param sm The soot method to check for return value
     * @return The return value
     */
    Value retrieveReturnValue(SootMethod sm) {
        if (!sm.isConcrete())
            return null;

        Chain<Unit> units = sm.retrieveActiveBody().getUnits();
        for (Unit u : units) {
            if (u instanceof Stmt) {
                Stmt stmt = (Stmt)u;
                if (stmt instanceof ReturnStmt) {
                    return ((ReturnStmt)stmt).getOp();
                }
            }
        }
        return null;
    }

    protected void analyzeMethodOverrideCallbacks(SootClass sootClass) {
        if (sootClass.getName().equalsIgnoreCase("com.pizzapizza.activity.OrderConfirmationActivity$1"))
            Logger.debug("here");

        if (!sootClass.isConcrete())
            return;
        if (sootClass.isInterface())
            return;

        // Do not start the search in system classes
        if (SystemClassHandler.isClassInSystemPackage(sootClass.getName()))
            return;

        // There are also some classes that implement interesting callback
        // methods.
        // We model this as follows: Whenever the user overwrites a method in an
        // Android OS class, we treat it as a potential callback.
        Map<String, SootMethod> systemMethods = new HashMap<>(10000);
        for (SootClass parentClass : Scene.v().getActiveHierarchy().getSuperclassesOf(sootClass)) {
            if (SystemClassHandler.isClassInSystemPackage(parentClass.getName()))
                for (SootMethod sm : parentClass.getMethods())
                    if (!sm.isConstructor())
                        systemMethods.put(sm.getSubSignature(), sm);
        }

        // Iterate over all user-implemented methods. If they are inherited
        // from a system class, they are callback candidates.
        for (SootClass parentClass : Scene.v().getActiveHierarchy().getSubclassesOfIncluding(sootClass)) {
            if (SystemClassHandler.isClassInSystemPackage(parentClass.getName()))
                continue;
            for (SootMethod method : parentClass.getMethods()) {
                if (!method.hasTag(SimulatedCodeElementTag.TAG_NAME)) {
                    // Check whether this is a real callback method
                    SootMethod parentMethod = systemMethods.get(method.getSubSignature());
                    if (parentMethod != null)
                        checkAndAddMethod(method, parentMethod, sootClass, CallbackType.Default);
                }
            }
        }
    }

    private SootMethod getMethodFromHierarchyEx(SootClass c, String methodSignature) {
        SootMethod m = c.getMethodUnsafe(methodSignature);
        if (m != null)
            return m;
        SootClass superClass = c.getSuperclassUnsafe();
        if (superClass != null)
            return getMethodFromHierarchyEx(superClass, methodSignature);
        return null;
    }

    private void analyzeClassInterfaceCallbacks(SootClass baseClass, SootClass sootClass, SootClass lifecycleElement) {
        // We cannot create instances of abstract classes anyway, so there is no
        // reason to look for interface implementations
        if (!baseClass.isConcrete())
            return;

        // Do not analyze system classes
        if (SystemClassHandler.isClassInSystemPackage(baseClass.getName()))
            return;
        if (SystemClassHandler.isClassInSystemPackage(sootClass.getName()))
            return;

        // Check the filters
        if (!filterAccepts(lifecycleElement, baseClass))
            return;
        if (!filterAccepts(lifecycleElement, sootClass))
            return;

        // If we are a class, one of our superclasses might implement an Android
        // interface
        SootClass superClass = sootClass.getSuperclassUnsafe();
        if (superClass != null)
            analyzeClassInterfaceCallbacks(baseClass, superClass, lifecycleElement);

        // Do we implement one of the well-known interfaces?
        for (SootClass i : collectAllInterfaces(sootClass)) {
            if (androidCallbacks.contains(i.getName())) {
                CallbackType callbackType = isUICallback(i) ? CallbackType.Widget : CallbackType.Default;

                for (SootMethod sm : i.getMethods()) {
                    SootMethod callbackImplementation = getMethodFromHierarchyEx(baseClass, sm.getSubSignature());
                    if (callbackImplementation != null)
                        checkAndAddMethod(callbackImplementation, sm, lifecycleElement, callbackType);
                }
            }
        }
    }

    /**
     * Gets whether the given callback interface or class represents a UI callback
     *
     * @param i
     *            The callback interface or class to check
     * @return True if the given callback interface or class represents a UI
     *         callback, otherwise false
     */
    private boolean isUICallback(SootClass i) {
        return i.getName().startsWith("android.widget") || i.getName().startsWith("android.view")
                || i.getName().startsWith("android.content.DialogInterface$");
    }


    /**
     * Checks and adds the base activities
     */
    void checkAndAddNestedActivities() {
        // Map the activity class with the base activity that it implements
        for (SootClass sc : Scene.v().getApplicationClasses()) {
            if (!isExportedActivityClass(sc.getName()))
                continue;

            Set<SootClass> tmpBaseActivitySet = new HashSet<>();
            MultiMap<SootClass,SootClass> tmpBaseActivityMapping = new HashMultiMap<>();

            SootClass curClass = sc;
            while (curClass.hasSuperclass()) {
                SootClass superClass = curClass.getSuperclass();
                if (superClass.getName().equals("android.app.Activity")
                        || superClass.getName().contains("android.support.v7.app")
                        || superClass.getName().contains("android.support.v4.app")) {
                    if (!curClass.equals(sc)) {
                        baseActivityMapping.put(sc, curClass);
                        baseActivitySet.add(curClass);
                        if (!tmpBaseActivityMapping.isEmpty())
                            baseActivityMapping.putAll(tmpBaseActivityMapping);
                        if (!tmpBaseActivitySet.isEmpty())
                            baseActivitySet.addAll(tmpBaseActivitySet);
                    }
                    break;
                } else {
                    if (!curClass.equals(sc)) {
                        tmpBaseActivityMapping.put(sc, curClass);
                        tmpBaseActivitySet.add(curClass);
                    }
                    curClass = superClass;
                }
            }
        }

        // Remove the exported activities
        Set<SootClass> toRemove = new HashSet<>();
        for (SootClass sc : baseActivitySet) {
            if (isExportedActivityClass(sc.getName()))
                toRemove.add(sc);
        }
        baseActivitySet.removeAll(toRemove);
    }

    public void resolveIccMethodToWidget() {
        // Resolving the ICC methods to callback methods to click listeners
        for (SootClass sc : Scene.v().getApplicationClasses()) {
            if (iccUnitsForWidgetAnalysis.containsKey(sc)) {
                // load the callback methods
                Set<SootMethod> callbackMethodSet = new HashSet<>();
                for (CallbackDefinition callbackDefinition : uicallbacks.get(sc)){
                    callbackMethodSet.add(callbackDefinition.getTargetMethod());
                }

                for (Pair<Unit, SootMethod> iccPair : iccUnitsForWidgetAnalysis.get(sc)) {
                    Unit iccUnit = iccPair.getO1();
                    SootMethod iccMethod = iccPair.getO2();
                    for (SootMethod sm : sc.getMethods()) {
                        if (sm.equals(iccMethod)) {
                            // We iterate the call graph (DFS) from ICC method until we have reached one of the callback methods
                            Queue<SootMethod> unvisitedNodes = new LinkedList<>();
                            unvisitedNodes.add(sm);
                            while (!unvisitedNodes.isEmpty()) {
                                SootMethod callee = unvisitedNodes.poll();
                                if (callbackMethodSet.contains(callee)) {
                                    Logger.debug("Here");
                                } else {
                                    Collection<Unit> callers = icfg.getCallersOf(callee);
                                    for (Unit caller : callers) {
                                        SootMethod callerMethod = icfg.getMethodOf(caller);
                                        if (callbackMethodSet.contains(callerMethod)) {
                                            Logger.debug("here");
                                        } else unvisitedNodes.add(callerMethod);
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates widget nodes and ownership edges
     */
    public void findWidgetsMappings() {
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
        if (sc.getName().equalsIgnoreCase("com.pizzapizza.activity.OrderHistoryActivity"))
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
                                            StringConstant argValue = StringConstant.v(stringConst.iterator().next());
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

    /**
     * Checks whether the given Soot method comes from a system class. If not, it is
     * added to the list of callback methods.
     *
     * @param method
     *            The method to check and add
     * @param parentMethod
     *            The original method in the Android framework that declared the
     *            callback. This can, for example, be the method in the interface.
     * @param lifecycleClass
     *            The base class (activity, service, etc.) to which this callback
     *            method belongs
     * @param callbackType
     *            The type of callback to be registered
     * @return True if the method is new, i.e., has not been seen before, otherwise
     *         false
     */
    protected boolean checkAndAddMethod(SootMethod method, SootMethod parentMethod, SootClass lifecycleClass,
                                        CallbackType callbackType) {
        // Do not call system methods
        if (SystemClassHandler.isClassInSystemPackage(method.getDeclaringClass().getName()))
            return false;

        // Skip empty methods
        if (method.isConcrete() && isEmpty(method.retrieveActiveBody()))
            return false;

        // Skip constructors
        if (method.isConstructor() || method.isStaticInitializer())
            return false;

        // Check the filters
        if (!filterAccepts(lifecycleClass, method.getDeclaringClass()))
            return false;
        if (!filterAccepts(lifecycleClass, method))
            return false;

        return this.callbackMethods.put(lifecycleClass, new CallbackDefinition(method, parentMethod, callbackType));
    }

    private boolean isEmpty(Body activeBody) {
        for (Unit u : activeBody.getUnits())
            if (!(u instanceof IdentityStmt || u instanceof ReturnVoidStmt))
                return false;
        return true;
    }



    private Set<SootClass> collectAllInterfaces(SootClass sootClass) {
        Set<SootClass> interfaces = new HashSet<SootClass>(sootClass.getInterfaces());
        for (SootClass i : sootClass.getInterfaces())
            interfaces.addAll(collectAllInterfaces(i));
        return interfaces;
    }

    public MultiMap<SootClass, CallbackDefinition> getCallbackMethods() {
        return this.callbackMethods;
    }

    public MultiMap<SootClass, Integer> getLayoutClasses() {
        return this.layoutClasses;
    }

    public MultiMap<SootClass, SootClass> getFragmentClasses() {
        return this.fragmentClasses;
    }

    public Set<SootClass> getDynamicManifestComponents() {
        return this.dynamicManifestComponents;
    }

    /**
     * Adds a new filter that checks every callback before it is associated with the
     * respective host component
     *
     * @param filter
     *            The filter to add
     */
    public void addCallbackFilter(ICallbackFilter filter) {
        this.callbackFilters.add(filter);
    }

    /**
     * Excludes an entry point from all further processing. No more callbacks will
     * be collected for the given entry point
     *
     * @param entryPoint
     *            The entry point to exclude
     */
    public void excludeEntryPoint(SootClass entryPoint) {
        this.excludedEntryPoints.add(entryPoint);
    }

    /**
     * Checks whether the given class is an excluded entry point
     *
     * @param entryPoint
     *            The entry point to check
     * @return True if the given class is an excluded entry point, otherwise false
     */
    public boolean isExcludedEntryPoint(SootClass entryPoint) {
        return this.excludedEntryPoints.contains(entryPoint);
    }

    /**
     * Sets the provider that shall be used for obtaining constant values during the
     * callback analysis
     *
     * @param valueProvider
     *            The value provider to use
     */
    public void setValueProvider(IValueProvider valueProvider) {
        this.valueProvider = valueProvider;
    }

    /**
     * Gets the EditText widgets set collected from the Jimple files
     * @return The EditText widgets set collected from the Jimple files
     */
    public List<EditWidgetNode> getEditTextWidgetList() {
        return editTextWidgetList;
    }

    /**
     * Gets the clickable widgets set collected from the Jimple files
     * @return The clickable widgets set collected from the Jimple files
     */
    public List<ClickWidgetNode> getClickWidgetNodeList() {
        return clickWidgetNodeList;
    }

    /**
     * Gets the ownership edges set collected from the Jimple files
     * @return The ownership edges set collected from the Jimple files
     */
    public MultiMap<SootClass, AbstractWidgetNode> getOwnershipEdges() {
        return ownershipEdges;
    }

    /**
     * Gets the potential login mapped to the SootClass
     * @return The ownership edges set collected from the Jimple files
     */
    public MultiMap<SootClass, String> getPotentialLoginMap() {
        return potentialLoginMap;
    }

    /**
     * Gets the base activity mapping
     * @return The base activity mapping
     */
    public MultiMap<SootClass, SootClass> getBaseActivityMapping() {
        return baseActivityMapping;
    }
}
