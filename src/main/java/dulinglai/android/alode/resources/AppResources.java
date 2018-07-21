package dulinglai.android.alode.resources;

import dulinglai.android.alode.resources.manifest.ProcessManifest;
import dulinglai.android.alode.resources.resources.ARSCFileParser;
import org.pmw.tinylog.Logger;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.*;

public class AppResources {

    // App info that we care about (activities, widgets, stringList)
    private String appName = null;
    private ARSCFileParser resources;
    private ProcessManifest manifest;
    private Set<String> activityClasses;
    private List<ARSCFileParser.ResConfig> stringList;
    private List<ARSCFileParser.ResConfig> resourceIdList;
    private Set<String> entryPoints;

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
        this.manifest = new ProcessManifest(targetApk);
        this.appName = manifest.getPackageName();
        this.activityClasses = manifest.getAllActivityClasses();
        this.entryPoints = manifest.getEntryPointClasses();

        // debug logging
        Logger.info("[{}] Parsing manifest: {}", TAG, appName);
        Logger.info("[{}] DONE: Found {} activity classes.", TAG, activityClasses.size());
        for (String activity:activityClasses) {
            Logger.debug("[{}] Found activity class: {}", TAG, activity);
        }

        // parse the resources files
        Logger.info("[{}] Parsing resources.arsc ...", TAG);
        long beforeARSC = System.nanoTime();
        resources = new ARSCFileParser();
        resources.parse(targetApk);
        List<ARSCFileParser.ResPackage> resPackages = resources.getPackages();
        // parse string resources
        parseResources(resPackages);
        Logger.info("[{}] DONE: ARSC file parsing took " + (System.nanoTime() - beforeARSC) / 1E9 + " seconds.", TAG);
    }

    /**
     * Parse the string resources ("en" only) of the resource packages
     * @param resPackages The resource packages that need to be parsed
     */
    private void parseResources(List<ARSCFileParser.ResPackage> resPackages) {
        for (ARSCFileParser.ResPackage resPackage : resPackages) {
            for (ARSCFileParser.ResType resType : resPackage.getDeclaredTypes()) {
                if (resType.getTypeName().equals("string")){
                    stringList = resType.getConfigurations();
                    // remove String files of other languages (this is because the getLanguage does not return the current english version but its variant instead
                    for (Iterator<ARSCFileParser.ResConfig> iter = stringList.listIterator(); iter.hasNext();){
                        ARSCFileParser.ResConfig string = iter.next();
                        List<String> excludeLang = new ArrayList<>(Arrays.asList("ar","ru","de","es","fr","it","in","ja","ko","pt","zh","th","vi","tr"));
                        if (excludeLang.contains(string.getConfig().getLanguage())){
                            iter.remove();
                        }
                    }

                    for (ARSCFileParser.ResConfig string : stringList) {
                        Logger.debug("Remaining languages: {} with {} entries",string.getConfig().getLanguage(),string.getResources().size());
//                        for (ARSCFileParser.AbstractResource resource : resConfig.getResources()){
//                            if (resource instanceof ARSCFileParser.StringResource){
//                                Logger.debug("{} : {}", resource.getResourceID(), ((ARSCFileParser.StringResource) resource).getValue());
//                            }
//                        }
                    }
                }else if (resType.getTypeName().equals("id")){
                    resourceIdList = resType.getConfigurations();
//                    for (ARSCFileParser.ResConfig resourceId : resourceIdList) {
//                        Logger.debug("Remaining resource id: {} with {} entries",resourceId.getConfig().getLanguage(),resourceId.getResources().size());
//                        for (ARSCFileParser.AbstractResource resource : resourceId.getResources()){
//                            Logger.debug("{} : {}", resource.getResourceID(), resource.getResourceName());
//                        }
//                    }
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
     * Gets the string resources of this ARSC file instance
     * @return The string resources of this ARSC file instance
     */
    public List<ARSCFileParser.ResConfig> getStringList(){ return stringList; }

    /**
     * Gets the resources id list of this ARSC file instance
     * @return The resources id list of this ARSC file instance
     */
    public List<ARSCFileParser.ResConfig> getResourceIdList(){ return resourceIdList; }
}
