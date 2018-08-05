package dulinglai.android.alode;

import dulinglai.android.alode.callbacks.AbstractCallbackAnalyzer;
import dulinglai.android.alode.callbacks.CallbackDefinition;
import dulinglai.android.alode.callbacks.DefaultCallbackAnalyzer;
import dulinglai.android.alode.callbacks.FastCallbackAnalyzer;
import dulinglai.android.alode.callbacks.filters.AlienFragmentFilter;
import dulinglai.android.alode.callbacks.filters.AlienHostComponentFilter;
import dulinglai.android.alode.callbacks.filters.ApplicationCallbackFilter;
import dulinglai.android.alode.callbacks.filters.UnreachableConstructorFilter;
import dulinglai.android.alode.config.GlobalConfigs;
import dulinglai.android.alode.config.soot.SootSettings;
import dulinglai.android.alode.entryPointCreators.AndroidEntryPointCreator;
import dulinglai.android.alode.graph.ActivityWidgetTransitionGraph;
import dulinglai.android.alode.ic3.Ic3Analysis;
import dulinglai.android.alode.ic3.Ic3Config;
import dulinglai.android.alode.iccta.IccInstrumenter;
import dulinglai.android.alode.memory.IMemoryBoundedSolver;
import dulinglai.android.alode.memory.MemoryWatcher;
import dulinglai.android.alode.memory.TimeoutWatcher;
import dulinglai.android.alode.resources.AppResources;
import dulinglai.android.alode.resources.resources.ARSCFileParser;
import dulinglai.android.alode.resources.resources.LayoutFileParser;
import dulinglai.android.alode.resources.resources.controls.AndroidLayoutControl;
import dulinglai.android.alode.utils.sootUtils.SystemClassHandler;
import heros.solver.Pair;
import org.pmw.tinylog.Logger;
import soot.*;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

import java.io.IOException;
import java.util.*;

public class SetupApplication {

    // An instance that holds the app's resources references
    private AppResources appResources;
    private String packageName;
    private String apkPath;
    private String androidJar;
    private String outputDir;

    // Entry points related
    private AndroidEntryPointCreator entryPointCreator = null;
    private Set<SootClass> entrypoints = null;
    private Set<String> entryPointString = null;
    private MultiMap<SootClass, SootClass> fragmentClasses = new HashMultiMap<>();

    private SootMethod dummyMainMethod;

    // Callbacks
    private MultiMap<SootClass, CallbackDefinition> callbackMethods = new HashMultiMap<>();
    private GlobalConfigs.CallbackAnalyzer callbackAnalyzerType;
    private int maxCallbacksPerComponent;
    private long maxTimeout;
    private int maxCallbackAnalysisDepth;

    // widgets
    private MultiMap<SootClass, Integer> layoutClasses = new HashMultiMap<>();
    private MultiMap<SootClass, CallbackDefinition> uicallbacks = new HashMultiMap<>();

    // Activity and widgets map
    private Set<String> activityList;
    private Map<SootClass, Set<Integer>> ownershipEdges = new HashMap<>();

    // Activity Widget Graph
    private ActivityWidgetTransitionGraph awtg;

    // IccTA
    private IccInstrumenter iccInstrumenter = null;
    private String iccModel = null;

    // results
    private ResultWriter resultWriter;


    public SetupApplication(GlobalConfigs config) {
        // Setup Soot for analysis
        SootSettings sootSettings = new SootSettings(config);
        sootSettings.initializeSoot();

        // Setup app resources
        this.appResources = new AppResources(config.getInputApkPath());
        this.packageName = appResources.getAppName();
        this.apkPath = config.getInputApkPath();
        this.androidJar = config.getAndroidJarPath();
        this.outputDir = config.getOutputPath();
        this.activityList = appResources.getActivityClasses();

        // Setup result writer
        this.resultWriter = new ResultWriter(this.packageName, this.outputDir);

        // Setup analysis config
        this.callbackAnalyzerType = config.getCallbackAnalyzer();
        this.maxCallbacksPerComponent = config.getMaxCallbacksPerComponent();
        this.maxTimeout = config.getMaxTimeout();
        this.maxCallbackAnalysisDepth = config.getMaxCallbackAnalysisDepth();

        // Create the initial entry point - launchable activities
        entryPointString = appResources.getLaunchableActivities();
        entrypoints = new HashSet<>(entryPointString.size());
        for (String className : entryPointString){
            SootClass sc = Scene.v().getSootClassUnsafe(className);
            if (sc != null)
                entrypoints.add(sc);
        }
        // Check if the entry points have been successfully created
        if (entrypoints == null || entrypoints.isEmpty()) {
            Logger.error("No entry points");
            entryPointString = appResources.getManifest().getEntryPointClasses();
            entrypoints = new HashSet<>(entryPointString.size());
            for (String className : entryPointString){
                SootClass sc = Scene.v().getSootClassUnsafe(className);
                if (sc != null)
                    entrypoints.add(sc);
            }
        }

        // AWTG
        this.awtg = appResources.getManifest().getAwtg();
    }

    public void runAnalysis(){
        // We process one launchable activity as entry point at once
        for (SootClass component : entrypoints) {
            // Calculate the callback methods (we also get the class <-> layout mapping here
            try {
                calculateCallbacks(component);
            } catch (IOException ex) {
                Logger.error(ex.getMessage());
                ex.printStackTrace();
            }
        }

//        runIC3();
    }

    public void runIC3(){
        // read configuration
        Ic3Config ic3Config = new Ic3Config(apkPath, packageName, androidJar, outputDir, entryPointString);

        // run analysis
        Ic3Analysis ic3Analysis = new Ic3Analysis(ic3Config,callbackMethods,entrypoints,dummyMainMethod,androidJar);
        ic3Analysis.performAnalysis(ic3Config);
    }


    // TODO: current method values performance over precision, check if we need more precise approach
    private void calculateCallbacks(SootClass component) throws IOException {
        LayoutFileParser layoutFileParser = new LayoutFileParser(packageName, appResources.getResources());

        // Choose between fast callback analyzer or default callback analyzer
        switch (callbackAnalyzerType){
            case Fast:
                calculateCallbackMethodsFast(layoutFileParser, component);
                break;
            case Default:
                calculateCallbackMethods(layoutFileParser, component);
                break;
            default:
                throw new RuntimeException("Unknown callback analyzer");
        }

        Logger.info("Entry point calculation done.");

        // Find all UI callbacks
        for (SootClass callback : callbackMethods.keySet()){
            for (CallbackDefinition callbackDefinition : callbackMethods.get(callback)){
                if (callbackDefinition.getCallbackType() == CallbackDefinition.CallbackType.Widget){
                    uicallbacks.put(callback, callbackDefinition);
                }
            }
        }
    }

    /**
     * Calculates the set of callback methods declared in the XML resource files or
     * the app's source code
     *
     * @param layoutFileParser
     *            The layout file parser to be used for analyzing UI controls
     * @throws IOException
     *             Thrown if a required configuration cannot be read
     */
    private void calculateCallbackMethods(LayoutFileParser layoutFileParser, SootClass component) throws IOException {
        // cleanup the callgraph
        resetCallgraph();

        // Make sure that we don't have any leftovers from previous runs
        PackManager.v().getPack("wjtp").remove("wjtp.lfp");
        PackManager.v().getPack("wjtp").remove("wjtp.ajc");

        // Get the classes for which to find callbacks
        Set<SootClass> entryPointClasses = getComponentsToAnalyze(component);

        // Collect the callback interfaces implemented in the app's
        // source code. Note that the filters should know all components to
        // filter out callbacks even if the respective component is only
        // analyzed later.
        AbstractCallbackAnalyzer callbackAnalyzer = new DefaultCallbackAnalyzer(entryPointClasses, maxCallbacksPerComponent, resultWriter);

        callbackAnalyzer.addCallbackFilter(new AlienHostComponentFilter(entrypoints));
        callbackAnalyzer.addCallbackFilter(new ApplicationCallbackFilter(entrypoints));
        callbackAnalyzer.addCallbackFilter(new UnreachableConstructorFilter());
        callbackAnalyzer.collectCallbackMethods();

        // Find the user-defined sources in the layout XML files.
        layoutFileParser.parseLayoutFile(apkPath);

        // Watch the callback collection algorithm's memory consumption
        MemoryWatcher memoryWatcher = null;
        TimeoutWatcher timeoutWatcher = null;
        if (callbackAnalyzer instanceof IMemoryBoundedSolver) {
            memoryWatcher = new MemoryWatcher();
            memoryWatcher.addSolver((IMemoryBoundedSolver) callbackAnalyzer);

            // Make sure that we don't spend too much time in the callback
            // analysis
            if (maxTimeout > 0) {
                timeoutWatcher = new TimeoutWatcher(maxTimeout);
                timeoutWatcher.addSolver((IMemoryBoundedSolver) callbackAnalyzer);
                timeoutWatcher.start();
            }
        }

        try {
            int depthIdx = 0;
            boolean hasChanged = true;
            boolean isInitial = true;
            while (hasChanged) {
                hasChanged = false;

                // Check whether the solver has been aborted in the meantime
                if (callbackAnalyzer instanceof IMemoryBoundedSolver) {
                    if (((IMemoryBoundedSolver) callbackAnalyzer).isKilled())
                        break;
                }

                // Create the new iteration of the main method
                createMainMethod(component);

                // Since the gerenation of the main method can take some time,
                // we check again whether we need to stop.
                if (callbackAnalyzer instanceof IMemoryBoundedSolver) {
                    if (((IMemoryBoundedSolver) callbackAnalyzer).isKilled())
                        break;
                }

                if (!isInitial) {
                    // Reset the callgraph
                    resetCallgraph();
                    // We only want to parse the layout files once
                    PackManager.v().getPack("wjtp").remove("wjtp.lfp");
                }
                isInitial = false;

                // Run the soot-based operations
                constructCallgraphInternal();
                if (!Scene.v().hasCallGraph())
                    throw new RuntimeException("No callgraph in Scene even after creating one. That's very sad "
                            + "and should never happen.");
                PackManager.v().getPack("wjtp").apply();

                // Creating all callgraph takes time and memory. Check whether
                // the solver has been aborted in the meantime
                if (callbackAnalyzer instanceof IMemoryBoundedSolver) {
                    if (((IMemoryBoundedSolver) callbackAnalyzer).isKilled()) {
                        Logger.warn("Aborted callback collection because of low memory");
                        break;
                    }
                }

                // Collect the results of the soot-based phases
                if (this.callbackMethods.putAll(callbackAnalyzer.getCallbackMethods()))
                    hasChanged = true;

                if (entrypoints.addAll(callbackAnalyzer.getDynamicManifestComponents()))
                    hasChanged = true;

                // Collect the XML-based callback methods
                if (collectXmlBasedCallbackMethods(layoutFileParser, callbackAnalyzer))
                    hasChanged = true;

                // Avoid callback overruns. If we are beyond the callback limit
                // for one entry point, we may not collect any further callbacks
                // for that entry point.
                if (maxCallbacksPerComponent > 0) {
                    for (Iterator<SootClass> componentIt = this.callbackMethods.keySet().iterator(); componentIt
                            .hasNext();) {
                        SootClass callbackComponent = componentIt.next();
                        if (this.callbackMethods.get(callbackComponent).size() > maxCallbacksPerComponent) {
                            componentIt.remove();
                            callbackAnalyzer.excludeEntryPoint(callbackComponent);
                        }
                    }
                }

                // Check depth limiting
                depthIdx++;
                if (maxCallbackAnalysisDepth > 0 && depthIdx >= maxCallbackAnalysisDepth)
                    break;
            }
        } catch (Exception ex) {
            Logger.error("Could not calculate callback methods", ex);
            throw ex;
        } finally {
            // Shut down the watchers
            if (timeoutWatcher != null)
                timeoutWatcher.stop();
            if (memoryWatcher != null)
                memoryWatcher.close();
        }

        // Filter out callbacks that belong to fragments that are not used by
        // the host activity
        AlienFragmentFilter fragmentFilter = new AlienFragmentFilter(invertMap(fragmentClasses));
        fragmentFilter.reset();
        for (Iterator<Pair<SootClass, CallbackDefinition>> cbIt = this.callbackMethods.iterator(); cbIt.hasNext();) {
            Pair<SootClass, CallbackDefinition> pair = cbIt.next();

            // Check whether the filter accepts the given mapping
            if (!fragmentFilter.accepts(pair.getO1(), pair.getO2().getTargetMethod()))
                cbIt.remove();
            else if (!fragmentFilter.accepts(pair.getO1(), pair.getO2().getTargetMethod().getDeclaringClass())) {
                cbIt.remove();
            }
        }

        // Avoid callback overruns
        if (maxCallbacksPerComponent > 0) {
            for (Iterator<SootClass> componentIt = this.callbackMethods.keySet().iterator(); componentIt.hasNext();) {
                SootClass callbackComponent = componentIt.next();
                if (this.callbackMethods.get(callbackComponent).size() > maxCallbacksPerComponent)
                    componentIt.remove();
            }
        }

        // Make sure that we don't retain any weird Soot phases
        PackManager.v().getPack("wjtp").remove("wjtp.lfp");
        PackManager.v().getPack("wjtp").remove("wjtp.ajc");

        // get the layout class maps
        layoutClasses = callbackAnalyzer.getLayoutClasses();
        // Debug logging
        Logger.debug("[LayoutClass] print lay out classes: ");
        for (SootClass layoutClass : layoutClasses.keySet()){
            Logger.debug("[LayoutClass] {} -> {}", layoutClass, layoutClasses.get(layoutClass));
        }
        Logger.debug("[UserControl] print user controls: ");
        for (String layoutKey : layoutFileParser.getUserControls().keySet()){
            Logger.debug("[UserControl] {} -> {}", layoutKey, layoutFileParser.getUserControls().get(layoutKey));
        }


        // Warn the user if we had to abort the callback analysis early
        boolean abortedEarly = false;
        if (callbackAnalyzer instanceof IMemoryBoundedSolver) {
            if (((IMemoryBoundedSolver) callbackAnalyzer).isKilled()) {
                Logger.warn("Callback analysis aborted early due to time or memory exhaustion");
                abortedEarly = true;
            }
        }
        if (!abortedEarly)
            Logger.info("Callback analysis completed...");
    }

    /**
     * Calculates the set of callback methods declared in the XML resource files or
     * the app's source code. This method prefers performance over precision and
     * scans the code including unreachable methods.
     *
     * @param layoutFileParser
     *            The layout file parser to be used for analyzing UI controls
     * @throws IOException
     *             Thrown if a required configuration cannot be read
     */
    private void calculateCallbackMethodsFast(LayoutFileParser layoutFileParser, SootClass component) throws IOException {
        // Construct new callgraph
        resetCallgraph();
        createMainMethod(component);
        constructCallgraphInternal();

        // Get the entry-point classes
        Set<SootClass> entryPointClasses = getComponentsToAnalyze(component);

        // Collect the callback interfaces implemented in the app's source code
        AbstractCallbackAnalyzer callbackAnalyzer = new FastCallbackAnalyzer(entryPointClasses, activityList);
        callbackAnalyzer.collectCallbackMethods();

        // Collect the results
        this.callbackMethods.putAll(callbackAnalyzer.getCallbackMethods());
        this.entrypoints.addAll(callbackAnalyzer.getDynamicManifestComponents());

        // Find the user-defined sources in the layout XML files.
        layoutFileParser.parseLayoutFileDirect(apkPath);

        // Collect the XML-based callback methods
        collectXmlBasedCallbackMethods(layoutFileParser, callbackAnalyzer);

        // Construct the final callgraph
        resetCallgraph();
        createMainMethod(component);
        constructCallgraphInternal();

        // get the layout class maps
        layoutClasses = callbackAnalyzer.getLayoutClasses();
        // Debug logging
        for (SootClass layoutClass : layoutClasses.keySet()){
            Logger.debug("[LayoutClass] {} -> {}", layoutClass, layoutClasses.get(layoutClass));
            if (awtg.getActivityByName(layoutClass.getName())!=null) {
                awtg.getActivityByName(layoutClass.getName()).setResourceId(layoutClasses.get(layoutClass).iterator().next());
            }
        }
        for (String layoutKey : layoutFileParser.getUserControls().keySet()){
            Logger.debug("[UserControl] {} -> {}", layoutKey, layoutFileParser.getUserControls().get(layoutKey));
            // TODO add widgets and ownership edges here


        }
    }

    private void createEntrypoints() {
        //TODO Check if we need all those entry points (the original code models all activity, serviceNodes, providerNodes and receiverNodes)
        // Create entry points
        entryPointString = appResources.getEntryPoints();
        entrypoints = new HashSet<>(entryPointString.size());
        for (String className : entryPointString){
            SootClass sc = Scene.v().getSootClassUnsafe(className);
            if (sc != null)
                entrypoints.add(sc);
        }
        // Check if the entry points have been successfully created
        if (entrypoints == null || entrypoints.isEmpty()) {
            Logger.error("No entry points");
            System.exit(1);
        }
    }

    /**
     * Releases the callgraph and all intermediate objects associated with it
     */
    private void resetCallgraph() {
        Scene.v().releaseCallGraph();
        Scene.v().releasePointsToAnalysis();
        Scene.v().releaseReachableMethods();
        G.v().resetSpark();
    }


    private void createMainMethod(SootClass component){
        entryPointCreator = createEntryPointCreator(component);
        dummyMainMethod = entryPointCreator.createDummyMain();
        Scene.v().setEntryPoints(Collections.singletonList(dummyMainMethod));
        if (!dummyMainMethod.getDeclaringClass().isInScene())
            Scene.v().addClass(dummyMainMethod.getDeclaringClass());

        // addClass() declares the given class as a library class. We need to fix it.
        dummyMainMethod.getDeclaringClass().setApplicationClass();
    }

    /**
     * Creates the {@link AndroidEntryPointCreator} instance which will later create
     * the dummy main method for the analysis
     *
     * @return The {@link AndroidEntryPointCreator} responsible for generating the
     *         dummy main method
     */
    private AndroidEntryPointCreator createEntryPointCreator(SootClass component) {
        Set<SootClass> components = getComponentsToAnalyze(component);

        // If we we already have an entry point creator, we make sure to clean up our
        // leftovers from previous runs
        if (entryPointCreator == null)
            entryPointCreator = new AndroidEntryPointCreator(appResources.getManifest(), components);
        else {
            entryPointCreator.removeGeneratedMethods(false);
            entryPointCreator.reset();
        }

        MultiMap<SootClass, SootMethod> callbackMethodSigs = new HashMultiMap<>();
        if (component == null) {
            // Get all callbacks for all components
            for (SootClass sc : this.callbackMethods.keySet()) {
                Set<CallbackDefinition> callbackDefs = this.callbackMethods.get(sc);
                if (callbackDefs != null)
                    for (CallbackDefinition cd : callbackDefs)
                        callbackMethodSigs.put(sc, cd.getTargetMethod());
            }
        } else {
            // Get the callbacks for the current component only
            for (SootClass sc : components) {
                Set<CallbackDefinition> callbackDefs = this.callbackMethods.get(sc);
                if (callbackDefs != null)
                    for (CallbackDefinition cd : callbackDefs)
                        callbackMethodSigs.put(sc, cd.getTargetMethod());
            }
        }

        entryPointCreator.setCallbackFunctions(callbackMethodSigs);
        entryPointCreator.setFragments(fragmentClasses);
        entryPointCreator.setComponents(components);
        return entryPointCreator;
    }

    /**
     * Triggers the callgraph construction in Soot
     */
    private void constructCallgraphInternal() {
        // enable ICC instrumenter
        if (iccModel!=null) {
            iccInstrumenter = new IccInstrumenter("icc.cmodel",
                    entryPointCreator.getGeneratedMainMethod().getDeclaringClass(),
                    entryPointCreator.getComponentToEntryPointInfo());
            iccInstrumenter.onBeforeCallgraphConstruction();
        }

        // Construct the actual callgraph
        Logger.info("Constructing the callgraph...");
        PackManager.v().getPack("cg").apply();

        // Make sure that we have a hierarchy
        Scene.v().getOrMakeFastHierarchy();
    }

    /**
     * Collects the XML-based callback methods, e.g., Button.onClick() declared in
     * layout XML files
     *
     * @param lfp
     *            The layout file parser
     * @param jimpleClass
     *            The analysis class that gives us a mapping between layout IDs and
     *            components
     * @return True if at least one new callback method has been added, otherwise
     *         false
     */
    private boolean collectXmlBasedCallbackMethods(LayoutFileParser lfp, AbstractCallbackAnalyzer jimpleClass) {
        SootMethod smViewOnClick = Scene.v()
                .grabMethod("<android.view.View$OnClickListener: void onClick(android.view.View)>");

        // Collect the XML-based callback methods
        boolean hasNewCallback = false;
        for (final SootClass callbackClass : jimpleClass.getLayoutClasses().keySet()) {
            if (jimpleClass.isExcludedEntryPoint(callbackClass))
                continue;

            Set<Integer> classIds = jimpleClass.getLayoutClasses().get(callbackClass);
            for (Integer classId : classIds) {
                ARSCFileParser.AbstractResource resource = appResources.getResources().findResource(classId);
                if (resource instanceof ARSCFileParser.StringResource) {
                    final String layoutFileName = ((ARSCFileParser.StringResource) resource).getValue();

                    // Add the callback methods for the given class
                    Set<String> callbackMethods = lfp.getCallbackMethods().get(layoutFileName);
                    if (callbackMethods != null) {
                        for (String methodName : callbackMethods) {
                            final String subSig = "void " + methodName + "(android.view.View)";

                            // The callback may be declared directly in the
                            // class or in one of the superclasses
                            SootClass currentClass = callbackClass;
                            while (true) {
                                SootMethod callbackMethod = currentClass.getMethodUnsafe(subSig);
                                if (callbackMethod != null) {
                                    if (this.callbackMethods.put(callbackClass,
                                            new CallbackDefinition(callbackMethod, smViewOnClick, CallbackDefinition.CallbackType.Widget)))
                                        hasNewCallback = true;
                                    break;
                                }
                                SootClass sclass = currentClass.getSuperclassUnsafe();
                                if (sclass == null) {
                                    Logger.error(String.format("Callback method %s not found in class %s", methodName,
                                            callbackClass.getName()));
                                    break;
                                }
                                currentClass = sclass;
                            }
                        }
                    }

                    // Add the fragments for this class
                    Set<SootClass> fragments = lfp.getFragments().get(layoutFileName);
                    if (fragments != null)
                        for (SootClass fragment : fragments)
                            if (fragmentClasses.put(callbackClass, fragment))
                                hasNewCallback = true;

                    // For user-defined views, we need to emulate their
                    // callbacks
                    Set<AndroidLayoutControl> controls = lfp.getUserControls().get(layoutFileName);
                    if (controls != null) {
                        for (AndroidLayoutControl lc : controls)
                            if (!SystemClassHandler.isClassInSystemPackage(lc.getViewClass().getName()))
                                registerCallbackMethodsForView(callbackClass, lc);
                    }
                } else
                    Logger.error("Unexpected resource type for layout class");
            }
        }

        // Collect the fragments, merge the fragments created in the code with
        // those declared in Xml files
        if (fragmentClasses.putAll(jimpleClass.getFragmentClasses())) // Fragments
            // declared
            // in
            // code
            hasNewCallback = true;

        return hasNewCallback;
    }

    /**
     * Registers the callback methods in the given layout control so that they are
     * included in the dummy main method
     *
     * @param callbackClass
     *            The class with which to associate the layout callbacks
     * @param lc
     *            The layout control whose callbacks are to be associated with the
     *            given class
     */
    private void registerCallbackMethodsForView(SootClass callbackClass, AndroidLayoutControl lc) {
        // Ignore system classes
        if (SystemClassHandler.isClassInSystemPackage(callbackClass.getName()))
            return;

        // Get common Android classes
        SootClass scView = Scene.v().getSootClass("android.view.View");

        // Check whether the current class is actually a view
        if (!Scene.v().getOrMakeFastHierarchy().canStoreType(lc.getViewClass().getType(), scView.getType()))
            return;

        // There are also some classes that implement interesting callback
        // methods.
        // We model this as follows: Whenever the user overwrites a method in an
        // Android OS class, we treat it as a potential callback.
        SootClass sc = lc.getViewClass();
        Map<String, SootMethod> systemMethods = new HashMap<>(10000);
        for (SootClass parentClass : Scene.v().getActiveHierarchy().getSuperclassesOf(sc)) {
            if (parentClass.getName().startsWith("android."))
                for (SootMethod sm : parentClass.getMethods())
                    if (!sm.isConstructor())
                        systemMethods.put(sm.getSubSignature(), sm);
        }

        // Scan for methods that overwrite parent class methods
        for (SootMethod sm : sc.getMethods()) {
            if (!sm.isConstructor()) {
                SootMethod parentMethod = systemMethods.get(sm.getSubSignature());
                if (parentMethod != null)
                    // This is a real callback method
                    this.callbackMethods.put(callbackClass,
                            new CallbackDefinition(sm, parentMethod, CallbackDefinition.CallbackType.Widget));
            }
        }
    }

    /**
     * Gets the components to analyze. If the given component is not null, we assume
     * that only this component and the application class (if any) shall be
     * analyzed. Otherwise, all components are to be analyzed.
     *
     * @param component
     *            A component class name to only analyze this class and the
     *            application class (if any), or null to analyze all classes.
     * @return The set of classes to analyze
     */
    private Set<SootClass> getComponentsToAnalyze(SootClass component) {
        if (component == null)
            return this.entrypoints;
        else {
            // We always analyze the application class together with each
            // component
            // as there might be interactions between the two
            Set<SootClass> components = new HashSet<>(2);
            components.add(component);

            String applicationName = this.appResources.getManifest().getApplicationName();
            if (applicationName != null && !applicationName.isEmpty())
                components.add(Scene.v().getSootClassUnsafe(applicationName));
            return components;
        }
    }

    /**
     * Inverts the given {@link MultiMap}. The keys become values and vice versa
     *
     * @param original
     *            The map to invert
     * @return An inverted copy of the given map
     */
    private <K, V> MultiMap<K, V> invertMap(MultiMap<V, K> original) {
        MultiMap<K, V> newTag = new HashMultiMap<>();
        for (V key : original.keySet())
            for (K value : original.get(key))
                newTag.put(value, key);
        return newTag;
    }
}
