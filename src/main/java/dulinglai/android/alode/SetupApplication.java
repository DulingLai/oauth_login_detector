package dulinglai.android.alode;

import dulinglai.android.alode.analyzers.*;
import dulinglai.android.alode.analyzers.filters.AlienFragmentFilter;
import dulinglai.android.alode.analyzers.filters.AlienHostComponentFilter;
import dulinglai.android.alode.analyzers.filters.ApplicationCallbackFilter;
import dulinglai.android.alode.analyzers.filters.UnreachableConstructorFilter;
import dulinglai.android.alode.config.GlobalConfigs;
import dulinglai.android.alode.config.soot.SootSettings;
import dulinglai.android.alode.entryPointCreators.AndroidEntryPointCreator;
import dulinglai.android.alode.graphBuilder.*;
import dulinglai.android.alode.iccparser.Ic3Provider;
import dulinglai.android.alode.iccparser.IccLink;
import dulinglai.android.alode.memory.IMemoryBoundedSolver;
import dulinglai.android.alode.memory.MemoryWatcher;
import dulinglai.android.alode.memory.TimeoutWatcher;
import dulinglai.android.alode.resources.manifest.ProcessManifest;
import dulinglai.android.alode.resources.resources.ARSCFileParser;
import dulinglai.android.alode.resources.resources.LayoutFileParser;
import dulinglai.android.alode.resources.resources.controls.AndroidLayoutControl;
import dulinglai.android.alode.resources.resources.controls.EditTextControl;
import dulinglai.android.alode.resources.resources.controls.GenericLayoutControl;
import dulinglai.android.alode.utils.androidUtils.ClassUtils;
import dulinglai.android.alode.utils.androidUtils.SystemClassHandler;
import heros.solver.Pair;
import org.pmw.tinylog.Logger;
import org.xmlpull.v1.XmlPullParserException;
import soot.*;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

import java.io.IOException;
import java.util.*;

import static dulinglai.android.alode.graphBuilder.graphUtils.resolveExecutionPath;

public class SetupApplication {
    // Logging tags
    private static final String RESOURCE_PARSER = "ResourceParser";
    private static final String CLASS_ANALYZER = "JimpleAnalyzer";
    private static final String LOGIN_DETECTOR = "LoginDetector";
    private static final String ICC_PARSER = "IccParser";

    private String apkPath;
    private String outputDir;

    // Resources
    private ProcessManifest manifest;
    private ARSCFileParser resources;
    private Map<Integer, String> stringResource = new HashMap<>();
    private Map<Integer, String> resourceId = new HashMap<>();
    private Map<String, Integer> layoutResource = new HashMap<>();

    // Entry points related
    private AndroidEntryPointCreator entryPointCreator = null;
    private Set<SootClass> entrypoints;
    private MultiMap<SootClass, SootClass> fragmentClasses = new HashMultiMap<>();

    // Callbacks
    private MultiMap<SootClass, CallbackDefinition> callbackMethods = new HashMultiMap<>();
    private GlobalConfigs.CallbackAnalyzer callbackAnalyzerType;
    private int maxCallbacksPerComponent;
    private long maxTimeout;
    private int maxCallbackAnalysisDepth;

    // Activity Nodes
    private List<ActivityNode> activityNodeList;
    private List<ServiceNode> serviceNodeList;
    private MultiMap<SootClass, Integer> layoutClasses = new HashMultiMap<>();

    // Widget Nodes
    private List<EditWidgetNode> editWidgetNodeList;
    private List<ClickWidgetNode> clickWidgetNodeList;
    private MultiMap<SootClass, CallbackDefinition> uicallbacks = new HashMultiMap<>();

    // Edges
    private MultiMap<SootClass, AbstractWidgetNode> ownershipEdgesClasses = new HashMultiMap<>();
    private MultiMap<ActivityNode, AbstractWidgetNode> ownershipEdges = new HashMultiMap<>();

    // Login detection
    private MultiMap<SootClass, String> potentialLoginMap = new HashMultiMap<>();
    private Set<ActivityNode> potentialLoginActivity = new HashSet<>();
    private MultiMap<ActivityNode, AbstractWidgetNode> potentialPasswordWidget = new HashMultiMap<>();
    private MultiMap<ActivityNode, AbstractWidgetNode> potentialUsernameWidget = new HashMultiMap<>();

    // Icc Model
    private String iccModel = null;
    private Map<SootClass, SootClass> classesForFurtherAnalysis = new HashMap<>();
    private ActivityHierarchy activityHierarchy;


    SetupApplication(GlobalConfigs config) {
        // Setup analysis config
        this.apkPath = config.getInputApkPath();
        this.outputDir = config.getOutputPath();
        this.callbackAnalyzerType = config.getCallbackAnalyzer();
        this.maxCallbacksPerComponent = config.getMaxCallbacksPerComponent();
        this.maxTimeout = config.getMaxTimeout();
        this.maxCallbackAnalysisDepth = config.getMaxCallbackAnalysisDepth();

        // Setup Soot for analysis
        SootSettings sootSettings = new SootSettings(config);
        sootSettings.initializeSoot();

        // Setup resources
        this.resources = new ARSCFileParser();

        // Setup ICC
        this.iccModel = config.getIccModelPath();
    }

    void runAnalysis(){
        // Parse Resources - collect resource ids
        Logger.info("[{}] Parsing ARSC files for resources mapping ...", RESOURCE_PARSER);
        long beforeARSC = System.nanoTime();
        parseResources();
        Logger.info("[{}] DONE: Resource parsing took " + (System.nanoTime() - beforeARSC) / 1E9 + " seconds.", RESOURCE_PARSER);

        /*
         Step 1. Collect activities
          */
        Logger.info("[{}] Collecting activity nodes ...", RESOURCE_PARSER);
        try {
            collectActivityNodes(apkPath);
        } catch (IOException|XmlPullParserException e) {
            Logger.error("[ERROR] Failed to parse the manifest!");
        }

        /*
        Step 2. Build activity hierarchy
         */
        Logger.info("[{}] Loading the ICC Model...", ICC_PARSER);
        Ic3Provider provider = new Ic3Provider(iccModel);
        List<IccLink> iccLinks = provider.getIccLinks();
        Logger.info("[{}] ...End Loading the ICC Model", ICC_PARSER);
        buildActivityHierarchy(iccLinks,activityNodeList);

        /*
        Step 3. Parse XML-based widgets
         */
        Logger.info("[{}] Collecting XML-based widgets ...", RESOURCE_PARSER);
        LayoutFileParser layoutFileParser = new LayoutFileParser(manifest.getPackageName(), resources);
        layoutFileParser.parseLayoutFileDirect(apkPath);

        /*
        Step 4. Analyze the class files
         */
        Logger.info("[{}] Analyzing the class files ...", CLASS_ANALYZER);
        for (SootClass component : entrypoints) {
            Set<SootClass> entryPointClasses = prepareJimpleAnalysis(component);
            processJimpleClasses(entryPointClasses, layoutFileParser);
        }

        /*
        Step 5. Combine the XML-based widgets with the programmatically set widgets and properties
         */
        Logger.info("[{}] Combining the data from XML and class files ...", CLASS_ANALYZER);
        combineClassAndXMLData(layoutFileParser.getUserControls());

        /*
        Step 6. Detect potential login activity and login widgets
         */
        Logger.info("[{}] Detecting potential login widgets ...", LOGIN_DETECTOR);
        LoginDetector loginDetector = new LoginDetector(potentialLoginMap, ownershipEdges, activityNodeList);
        loginDetector.detectPotentialLogin();
        potentialLoginActivity = loginDetector.getPotentialLoginActivity();
        potentialUsernameWidget = loginDetector.getPotentialUsernameWidget();
        potentialPasswordWidget = loginDetector.getPotentialPasswordWidget();

        /*
        Step 7. Build execution path
         */
        MultiMap<SootClass, Set<SootClass>> loginExecutionPath = buildExecutionPath(activityHierarchy,potentialLoginActivity);

        /*
        Results
         */

        // Debug logging
        String packageName = manifest.getPackageName();
        ResultWriter writer = new ResultWriter(packageName,outputDir);
        for (ActivityNode activity : potentialLoginActivity) {
            try {
                writer.appendStringToResultFile("loginDetectionTest.txt", activity.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private MultiMap<SootClass, Set<SootClass>> buildExecutionPath(ActivityHierarchy activityHierarchy, Set<ActivityNode> potentialLoginActivity) {
        MultiMap<SootClass, Set<SootClass>> loginExecutionPath = new HashMultiMap<>();
        for (ActivityNode activityNode : potentialLoginActivity) {
            SootClass activityClass = Scene.v().getSootClassUnsafe(activityNode.getName());
            if (activityHierarchy.isActivityInHierarchy(activityClass)){
                Set<SootClass> launchableActivities = new HashSet<>();
                for (String launchableActString : manifest.getLaunchableActivities()) {
                    launchableActivities.add(Scene.v().getSootClassUnsafe(launchableActString));
                }
                loginExecutionPath.putAll(resolveExecutionPath(activityHierarchy, activityClass, launchableActivities));
            } else {
                Logger.warn("[{}] Failed to find the login activity");
                //TODO log the error in debug file
            }
        }
        return loginExecutionPath;
    }

    private void buildActivityHierarchy(List<IccLink> iccLinks, List<ActivityNode> activityNodeList) {
        Logger.info("Building activity hierarchy ...");
        ClassUtils classUtils = new ClassUtils();

        // Setup activity hierarchy
        activityHierarchy = new ActivityHierarchy(activityNodeList);

        // Resolve the activity links
        for (IccLink iccLink : iccLinks) {
            SootClass destClass = iccLink.getDestinationC();
            SootClass sourceClass = iccLink.getFromSM().getDeclaringClass();
            ClassUtils.ComponentType destType = classUtils.getComponentType(destClass);
            ClassUtils.ComponentType sourceType = classUtils.getComponentType(sourceClass);

            Logger.debug("ICC Link: {} -> {} -> {}", sourceClass, iccLink.getFromSM(), destClass);
            Logger.debug("Type: {} -> {}", sourceType, destType);

            if (destType.equals(ClassUtils.ComponentType.Activity)) {
                if (sourceType.equals(ClassUtils.ComponentType.Activity))
                    activityHierarchy.addActivityToSubactivities(sourceClass, destClass);
                else {
                    if (sourceType.equals(ClassUtils.ComponentType.ClickListener)) {
                        classesForFurtherAnalysis.put(sourceClass, destClass);
                    } else {
                        Logger.debug("[Debug] Source class {} has type {}", sourceClass, sourceType);
                    }
                }
            } else if (sourceType.equals(ClassUtils.ComponentType.Activity)) {
                Logger.debug("[Debug] Destination class {} has type {}", destClass, destType);
            }
        }
        activityHierarchy.removeNullValues();
    }

    /**
     * Prepares the graphs for Jimple class analysis
     * @param component The component to start analysis
     */
    private Set<SootClass> prepareJimpleAnalysis(SootClass component) {
        // Construct new call-graph
        resetCallgraph();
        createMainMethod(component);
        constructCallgraphInternal();
        return getComponentsToAnalyze(component);
    }

    /**
     * Parse the string resources ("en" only) and other resource ids of the resource packages
     */
    private void parseResources() {
        try {
            resources.parse(apkPath);
        } catch (IOException e) {
            Logger.error("[ERROR] Failed to parse the resource files");
        }
        List<ARSCFileParser.ResPackage> resPackages = resources.getPackages();

//        List<String> excludeLang = new ArrayList<>(Arrays.asList("ar","ru","de","es","fr","it","in",
//                "ja","ko","pt","zh","th","vi","tr","ca","da","fa","ka","pa","ta","nb","be","he","is",
//                "ne","te","af","bg","fi","hi","si","kk","mk","sk","uk","el","gl","ml","nl","pl","ms",
//                "sl","tl","am","km","bn","kn","mn","lo","ro","ro","sq","hr","mr","sr","ur","bs","cs",
//                "et","lt","eu","gu","hu","zu","lv","sv","iw","sw","hy","ky","my","az","uz"));

        // Collect the resource mappings
        for (ARSCFileParser.ResPackage resPackage : resPackages) {
            for (ARSCFileParser.ResType resType : resPackage.getDeclaredTypes()) {
                switch (resType.getTypeName()) {
                    case "string":
                        // only keep English Strings
                        for (ARSCFileParser.ResConfig string : resType.getConfigurations()) {
                            if (string.getConfig().getLanguage().equals("\u0000\u0000")) {
                                for (ARSCFileParser.AbstractResource resource : string.getResources()) {
                                    if (resource instanceof ARSCFileParser.StringResource) {
                                        stringResource.put(resource.getResourceID(), ((ARSCFileParser.StringResource) resource).getValue());
                                    }
                                }
                            }
                        }
                        break;
                    case "id":
                        for (ARSCFileParser.ResConfig resIdConfig : resType.getConfigurations()) {
                            for (ARSCFileParser.AbstractResource resource : resIdConfig.getResources()) {
                                resourceId.put(resource.getResourceID(), resource.getResourceName());
                            }
                        }
                        break;
                    case "layout":
                        for (ARSCFileParser.ResConfig resLayoutConfig : resType.getConfigurations()) {
                            for (ARSCFileParser.AbstractResource resource : resLayoutConfig.getResources()) {
                                layoutResource.put(resource.getResourceName(), resource.getResourceID());
                            }
                        }
                        break;
                }
            }
        }
    }

    /**
     * Parse the apk manifest file for entry point classes and exported activities and services
     * @param targetApk The target apk file to parse
     * @throws IOException When the apk is not found
     * @throws XmlPullParserException When we failed to parse the apk manifest
     */
    private void collectActivityNodes(String targetApk) throws IOException, XmlPullParserException {
        this.manifest = new ProcessManifest(targetApk);
        Set<String> entryPointString = manifest.getLaunchableActivities();

        entrypoints = new HashSet<>(entryPointString.size());
        for (String className : entryPointString){
            SootClass sc = Scene.v().getSootClassUnsafe(className);
            if (sc != null)
                entrypoints.add(sc);
        }

        // Gets the activities and services nodes from manifest
        this.activityNodeList = manifest.getActivityNodeList();
        this.serviceNodeList = manifest.getServiceNodeList();
    }

    /**
     * Collects the widgets from Jimple files
     * @param entryPointClasses The entry activity to start analysis
     * @param layoutFileParser The layout file parser
     */
    private void processJimpleClasses(Set<SootClass> entryPointClasses, LayoutFileParser layoutFileParser) {
        try {
            switch (callbackAnalyzerType) {
                case Fast:
                    processJimpleClassesFast(layoutFileParser, entryPointClasses);
                    break;
                case Default:
                    processJimpleClassesDefault(layoutFileParser, entryPointClasses);
                    break;
                default:
                    throw new RuntimeException("Unknown callback analyzer");
            }
        } catch (IOException ex) {
            ex.getMessage();
        }

        // Find all UI callbacks
        for (SootClass callback : callbackMethods.keySet()){
            for (CallbackDefinition callbackDefinition : callbackMethods.get(callback)){
                if (callbackDefinition.getCallbackType() == CallbackDefinition.CallbackType.Widget){
                    uicallbacks.put(callback, callbackDefinition);
                }
            }
        }
    }

    private void combineClassAndXMLData(MultiMap<String, AndroidLayoutControl> userControls) {
        // Add resource IDs for activity nodes
        for (SootClass layoutClass : layoutClasses.keySet()) {
            for (ActivityNode activityNode : activityNodeList) {
                if (activityNode.getName()==null)
                    Logger.warn("[Activity Node] Missing activity name...");
                else if (activityNode.getName().equals(layoutClass.getName())) {
                    activityNode.setResourceId(layoutClasses.get(layoutClass).iterator().next());

                    Set<AbstractWidgetNode> widgetNodeSet = ownershipEdgesClasses.get(layoutClass);
                    if (widgetNodeSet!=null && !widgetNodeSet.isEmpty()) {
                        ownershipEdges.putAll(activityNode, widgetNodeSet);
                    }
                }
            }
        }
        // collectXmlWidgets
        collectXmlWidgets(userControls);
    }

    /**
     * Collect the widgets from xml files
     * @param userControls The user controls found from xml files.
     *                     We are only interested in EditText widgets and Clickable widgets
     */
    private void collectXmlWidgets(MultiMap<String, AndroidLayoutControl> userControls) {
        for (String layoutKeyFull : userControls.keySet()) {
            String layoutKey = layoutKeyFull.substring(layoutKeyFull.lastIndexOf('/') + 1, layoutKeyFull.lastIndexOf('.'));
            Integer resId = layoutResource.get(layoutKey);
            if (resId != null) {
                for (ActivityNode activityNode : activityNodeList) {
                    if (activityNode.getResourceId() == resId) {
                        Set<AbstractWidgetNode> newWidgetNodeSet = createWidgetNodeForUserControls(userControls.get(layoutKeyFull));
                        Set<AbstractWidgetNode> oldWidgetNodeSet = ownershipEdges.get(activityNode);
                        if (oldWidgetNodeSet!=null) {
                            if (!oldWidgetNodeSet.isEmpty()) {
                                for (AbstractWidgetNode widget : newWidgetNodeSet) {
                                    oldWidgetNodeSet.removeIf(oldWidget -> oldWidget.getResourceId() == widget.getResourceId());
                                }
                            }
                            newWidgetNodeSet.addAll(oldWidgetNodeSet);
                        }
                        ownershipEdges.putAll(activityNode, newWidgetNodeSet);
                    }
                }
            } else
                Logger.warn("[GraphBuilder] Cannot find the resource id for {}", layoutKeyFull);
        }
    }

    private Set<AbstractWidgetNode> createWidgetNodeForUserControls(Set<AndroidLayoutControl> androidLayoutControls) {
        Set<AbstractWidgetNode> newWidgetSet = new HashSet<>();
        for (AndroidLayoutControl usercontrol : androidLayoutControls) {
            if (usercontrol instanceof EditTextControl) {
                newWidgetSet.add(createEditTextForUserControl((EditTextControl)usercontrol));
            } else {
                if (usercontrol.getClickListener() != null)
                    newWidgetSet.add(createClickWidgetForUserControl((GenericLayoutControl)usercontrol, ClickWidgetNode.EventType.Click));
                else {
                    for (ClickWidgetNode clickWidgetNode : clickWidgetNodeList) {
                        if (clickWidgetNode.getResourceId() == usercontrol.getID()) {
                            newWidgetSet.add(createClickWidgetForUserControl((GenericLayoutControl) usercontrol,
                                    clickWidgetNode.getInputType()));
                        }
                    }
                }
            }
        }
        return newWidgetSet;
    }

    /**
     * Creates edit text widget out of given user control
     * @param usercontrol The user control
     * @return The edit text widget created
     */
    private EditWidgetNode createEditTextForUserControl(EditTextControl usercontrol) {
        int resId = usercontrol.getID();
        int inputType = usercontrol.getInputType();

        String text = checkStringResource(usercontrol.getText());
        String contentDescription = checkStringResource(usercontrol.getContentDescription());
        String hint = checkStringResource(usercontrol.getHint());

        if (resId!=-1) {
            String resString = resources.findResource(resId).getResourceName();
            return new EditWidgetNode(resId,resString,text,contentDescription,hint,inputType);
        } else {
            return new EditWidgetNode(resId, text, contentDescription, hint, inputType);
        }
    }

    /**
     * Creates click widget out of given user control
     * @param usercontrol The user control
     * @return The click widget created
     */
    private ClickWidgetNode createClickWidgetForUserControl(GenericLayoutControl usercontrol, ClickWidgetNode.EventType eventType) {
        int resId = usercontrol.getID();
        String text = checkStringResource(usercontrol.getText());

        if (resId!=-1) {
            String resString = resources.findResource(resId).getResourceName();
            return new ClickWidgetNode(resId, resString, text, eventType);
        } else {
            return new ClickWidgetNode(resId, text, eventType);
        }
    }

    /**
     * Checks the string resources if the given string is a resource id
     * @param text The string to check
     * @return The string found in string resource if the given string is a resource id,
     * else returns the original string
     */
    private String checkStringResource(String text) {
        if (text != null) {
            if (text.matches("-?\\d+")) {
                int resId = Integer.parseInt(text);
                String newText = stringResource.get(resId);
                if (newText != null)
                    return newText;
                else
                    return text;
            } else
                return text;
        } else
            return null;
    }

    /**
     * Collect the widget nodes in a fast mode.
     * This method prefers performance over precision and scans the code including unreachable methods.
     *
     * @param layoutFileParser
     *            The layout file parser to be used for analyzing UI controls
     * @throws IOException
     *             Thrown if a required configuration cannot be read
     */
    private void processJimpleClassesFast(LayoutFileParser layoutFileParser,
                                          Set<SootClass> entryPointClasses) throws IOException {
        // Collect the callback interfaces implemented in the app's source code
        AbstractJimpleAnalyzer jimpleAnalyzer = new FastJimpleAnalyzer(entryPointClasses,
                manifest.getAllActivityClasses(), layoutFileParser, classesForFurtherAnalysis);
        jimpleAnalyzer.analyzeJimpleClasses();

        // Get the layout class maps
        layoutClasses = jimpleAnalyzer.getLayoutClasses();

        // Get the nodes set collected from Jimple files
        this.editWidgetNodeList = jimpleAnalyzer.getEditTextWidgetList();
        this.clickWidgetNodeList = jimpleAnalyzer.getClickWidgetNodeList();
        this.ownershipEdgesClasses = jimpleAnalyzer.getOwnershipEdges();
        this.potentialLoginMap = jimpleAnalyzer.getPotentialLoginMap();

        Map<SootClass, SootClass> classMapForActivityHierarchy = jimpleAnalyzer.getClassMapForActivityHierarchy();
        for (SootClass sc : classMapForActivityHierarchy.keySet()) {
            activityHierarchy.addActivityToSubactivities(classMapForActivityHierarchy.get(sc), classesForFurtherAnalysis.get(sc));
        }

        // Collect the results
        this.callbackMethods.putAll(jimpleAnalyzer.getCallbackMethods());
//        this.entrypoints.addAll(jimpleAnalyzer.getDynamicManifestComponents());
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
        SootMethod dummyMainMethod = entryPointCreator.createDummyMain();
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

        // If we we already have an entry point creator, clean up the leftovers from previous runs
        if (entryPointCreator == null)
            entryPointCreator = new AndroidEntryPointCreator(manifest, components);
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
    private boolean collectXmlBasedCallbackMethods(LayoutFileParser lfp, AbstractJimpleAnalyzer jimpleClass) {
        SootMethod smViewOnClick = Scene.v()
                .grabMethod("<android.view.View$OnClickListener: void onClick(android.view.View)>");

        // Collect the XML-based callback methods
        boolean hasNewCallback = false;
        for (final SootClass callbackClass : jimpleClass.getLayoutClasses().keySet()) {
            if (jimpleClass.isExcludedEntryPoint(callbackClass))
                continue;

            Set<Integer> classIds = jimpleClass.getLayoutClasses().get(callbackClass);
            for (Integer classId : classIds) {
                ARSCFileParser.AbstractResource resource = resources.findResource(classId);
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
            // component as there might be interactions between the two
            Set<SootClass> components = new HashSet<>(2);
            components.add(component);

            String applicationName = this.manifest.getApplicationName();
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


    /**
     * Calculates the set of callback methods declared in the XML resource files or
     * the app's source code
     *
     * @param layoutFileParser
     *            The layout file parser to be used for analyzing UI controls
     * @throws IOException
     *             Thrown if a required configuration cannot be read
     */
    private void processJimpleClassesDefault(LayoutFileParser layoutFileParser, Set<SootClass> entryPointClasses) throws IOException {
        // cleanup the callgraph
        resetCallgraph();

        // Make sure that we don't have any leftovers from previous runs
        PackManager.v().getPack("wjtp").remove("wjtp.lfp");
        PackManager.v().getPack("wjtp").remove("wjtp.ajc");

        // Collect the callback interfaces implemented in the app's
        // source code. Note that the filters should know all components to
        // filter out callbacks even if the respective component is only
        // analyzed later.
        AbstractJimpleAnalyzer callbackAnalyzer = new DefaultJimpleAnalyzer(entryPointClasses,
                maxCallbacksPerComponent, manifest.getAllActivityClasses(), layoutFileParser, classesForFurtherAnalysis);

        callbackAnalyzer.addCallbackFilter(new AlienHostComponentFilter(entrypoints));
        callbackAnalyzer.addCallbackFilter(new ApplicationCallbackFilter(entrypoints));
        callbackAnalyzer.addCallbackFilter(new UnreachableConstructorFilter());
        callbackAnalyzer.analyzeJimpleClasses();

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
}
