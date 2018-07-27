package dulinglai.android.alode.resources;

import dulinglai.android.alode.resources.manifest.ProcessManifest;
import dulinglai.android.alode.resources.resources.ARSCFileParser;
import org.pmw.tinylog.Logger;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.*;

public class AppResources {

    // App info that we care about (activityNodes, widgets, stringList)
    private String appName = null;
    private ARSCFileParser resources;
    private ProcessManifest manifest;

    private Set<String> activityClasses;
    private Map<Integer, String> stringResource = new HashMap<>();
    private Map<Integer, String> resourceId = new HashMap<>();
    private Map<Integer, String> layoutResource = new HashMap<>();

    private Set<String> entryPoints;

    // App launchable activities (i.e. the start activity where we construct our graph)
    private Set<String> launchableActivities;

    private final static String TAG = "AppParser";

    public AppResources(String targetApk){
        try {
            parseAppResources(targetApk);
        } catch (IOException | XmlPullParserException e) {
            Logger.error("Failed to parse the app resources: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Parsing the app resources failed: ", e);
        }
    }

    private void parseAppResources(String targetApk) throws IOException, XmlPullParserException{
        Logger.info("[{}] Parsing app resources (manifest and resource.arsc) ...", TAG);
        long beforeARSC = System.nanoTime();
        this.manifest = new ProcessManifest(targetApk);
        this.appName = manifest.getPackageName();
        this.activityClasses = manifest.getAllActivityClasses();
        this.entryPoints = manifest.getEntryPointClasses();

        // activities and services
//        this.activities = manifest.getActivities();
//        this.services = manifest.getServices();
        this.launchableActivities = manifest.getLaunchableActivities();

        // parse the resources files
        resources = new ARSCFileParser();
        resources.parse(targetApk);
        List<ARSCFileParser.ResPackage> resPackages = resources.getPackages();
        // parse string resources
        parseResources(resPackages);
        Logger.info("[{}] DONE: Resource parsing took " + (System.nanoTime() - beforeARSC) / 1E9 + " seconds.", TAG);
    }

    /**
     * Parse the string resources ("en" only) of the resource packages
     * @param resPackages The resource packages that need to be parsed
     */
    private void parseResources(List<ARSCFileParser.ResPackage> resPackages) {
//        List<String> excludeLang = new ArrayList<>(Arrays.asList("ar","ru","de","es","fr","it","in",
//                "ja","ko","pt","zh","th","vi","tr","ca","da","fa","ka","pa","ta","nb","be","he","is",
//                "ne","te","af","bg","fi","hi","si","kk","mk","sk","uk","el","gl","ml","nl","pl","ms",
//                "sl","tl","am","km","bn","kn","mn","lo","ro","ro","sq","hr","mr","sr","ur","bs","cs",
//                "et","lt","eu","gu","hu","zu","lv","sv","iw","sw","hy","ky","my","az","uz"));

        for (ARSCFileParser.ResPackage resPackage : resPackages) {
            for (ARSCFileParser.ResType resType : resPackage.getDeclaredTypes()) {
                if (resType.getTypeName().equals("string")){
                    // only keep English Strings
                    for (ARSCFileParser.ResConfig string : resType.getConfigurations()){
                        if(string.getConfig().getLanguage().equals("\u0000\u0000")){
                            for (ARSCFileParser.AbstractResource resource : string.getResources()){
                                if (resource instanceof ARSCFileParser.StringResource){
                                    stringResource.put(resource.getResourceID(), ((ARSCFileParser.StringResource) resource).getValue());
                                }
                            }
                        }
                    }
                } else if (resType.getTypeName().equals("id")){
                    for (ARSCFileParser.ResConfig resIdConfig : resType.getConfigurations()){
                        for (ARSCFileParser.AbstractResource resource : resIdConfig.getResources()){
                            resourceId.put(resource.getResourceID(), resource.getResourceName());
                        }
                    }
                } else if (resType.getTypeName().equals("layout")){
                    for (ARSCFileParser.ResConfig resLayoutConfig : resType.getConfigurations()){
                        for (ARSCFileParser.AbstractResource resource : resLayoutConfig.getResources()){
                            layoutResource.put(resource.getResourceID(), resource.getResourceName());
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets all activity classes of the current manifest instance
     * @return The list of all activity classes of the current manifest instance
     */
    public Set<String> getActivityClasses() { return activityClasses; }

    /**
     * Gets the app name of the current manifest instance
     * @return The app name of the current manifest instance
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Gets the resources of the app (ARSCFileParser)
     * @return The resources of the app (ARSCFileParser)
     */
    public ARSCFileParser getResources() { return resources; }

    /**
     * Gets the manifest of the app (ProcessManifest)
     * @return The manifest of the app (ProcessManifest)
     */
    public ProcessManifest getManifest() { return manifest; }

    /**
     * Gets the list of entry points to model the lifecycle
     * @return The list of entry points (SootClass)
     */
    public Set<String> getEntryPoints() {
        return entryPoints;
    }

    /**
     * Gets the string resources by resource ID
     * @return The string resources of given resource ID
     */
    public String getStringResourceById(int id){ return stringResource.get(id); }

    /**
     * Gets the resources id list of this ARSC file instance
     * @return The resources id list of this ARSC file instance
     */
    public String getResourceNameById(int id){ return resourceId.get(id); }

    public Set<String> getLaunchableActivities() {
        return launchableActivities;
    }

    public void setLaunchableActivities(Set<String> launchableActivities) {
        this.launchableActivities = launchableActivities;
    }
}
