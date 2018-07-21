package dulinglai.android.alode.config;

import org.pmw.tinylog.Logger;

/**
 * The Singleton class used to store configuration variables
 */
public class GlobalConfigs {

    /**
     * Enumeration containing the callgraph algorithms supported for the use
     * with the data flow tracker
     */
    public enum CallgraphAlgorithm {
        AutomaticSelection, CHA, VTA, RTA, SPARK, GEOM
    }
    /**
     * Enumeration containing the supported callback analyzers
     */
    public enum CallbackAnalyzer {
        /**
         * The highly-precise default analyzer
         */
        Default,
        /**
         * An analyzer that favors performance over precision
         */
        Fast
    }

    // analysis related configs
    private CallgraphAlgorithm callgraphAlgorithm = CallgraphAlgorithm.AutomaticSelection;
    private int maxCallbacksPerComponent = 100;
    private long maxTimeout = 0;
    private CallbackAnalyzer callbackAnalyzer = CallbackAnalyzer.Fast;
    private int maxCallbackAnalysisDepth = 0;

    // root directory of the project
    private String projectPath;
    private String inputApkPath;
    private String outputPath = "sootOutput";

    // Android OS related configs
    private String androidSdkPath;
    private String androidJarPath;
    private int androidApiLevel = 23;
    private boolean forceAndroidJar = false;


    // Getters and setters for configuration variables
    /*
    Project related
     */
    /**
     * Gets the current project working directory
     * @return The current project working directory
     */
    public String getProjectPath(){
        return projectPath;
    }
    /**
     * Sets the current project working directory
     * @param projectPath
     *          The path to current project working directory
     */
    public void setProjectPath(String projectPath){
        this.projectPath = projectPath;
    }

    /**
     * Gets the path to the input apk file
     * @return The path to the input apk file
     */
    public String getInputApkPath(){
        return inputApkPath;
    }
    /**
     * Sets the path to the input apk file
     * @param inputApkPath
     *          The path to the input apk file
     */
    public void setInputApkPath(String inputApkPath){
        this.inputApkPath = inputApkPath;
    }

    /**
     * Gets the directory for output files
     * @return The output path
     */
    public String getOutputPath(){
        return outputPath;
    }
    /**
     * Sets the directory for output files
     * @param outputPath
     *          The path to the output directory
     */
    public void setOutputPath(String outputPath){
        this.outputPath = outputPath;
    }

    /**
     * Gets the Android SDK directory
     * @return The directory in which Android SDK is located
     */
    public String getAndroidSdkPath(){
        return androidSdkPath;
    }
    /**
     * Sets the Android SDK directory
     * @param androidSdkPath
     *          The directory in which Android SDK is located
     */
    public void setAndroidSdkPath(String androidSdkPath){
        this.androidSdkPath = androidSdkPath;
    }

    /**
     * Gets the Android JAR Path
     * @return The Android JAR Path
     */
    public String getAndroidJarPath(){
        return androidJarPath;
    }
    /**
     * Sets the path to Android JAR file
     * @param androidJarPath
     *          The path to Android JAR file
     */
    public void setAndroidJarPath(String androidJarPath){
        this.androidJarPath = androidJarPath;
    }

    /**
     * Gets the current Android API Level setting
     * @return The current Android API level setting
     */
    public int getAndroidApiLevel(){
        return androidApiLevel;
    }
    /**
     * Sets the path to Android JAR file
     * @param androidApiLevel
     *          The target Android API level
     */
    public void setAndroidApiLevel(int androidApiLevel){
        this.androidApiLevel = androidApiLevel;
    }

    /**
     * Gets force android jar setting
     * @return The current force android jar setting
     */
    public boolean getForceAndroidJar() {
        return forceAndroidJar;
    }
    /**
     * Sets force android jar setting
     * @param force_android_jar
     *          The target setting for forcing android jar
     */
    public void setForceAndroidJar(boolean force_android_jar) {
        this.forceAndroidJar = force_android_jar;
    }


    /*
    Analysis related
    */
    /**
     * Gets the callgraph algorithm to be used by the data flow tracker
     * @return The callgraph algorithm to be used by the data flow tracker
     */
    public CallgraphAlgorithm getCallgraphAlgorithm(){ return callgraphAlgorithm; }
    /**
     * Sets the callgraph algorithm to be used by the data flow tracker
     * @param algorithm
     *          The callgraph algorithm to be used by the data flow tracker
     */
    public void setCallgraphAlgorithm(String algorithm){ this.callgraphAlgorithm = parseCallgraphAlgorithm(algorithm); }

    /**
     * Gets the maximum number of callbacks modeled for each component
     * @return The maximum number of callbacks modeled for each component
     */
    public int getMaxCallbacksPerComponent() {
        return maxCallbacksPerComponent;
    }

    /**
     * Sets the maximum number of callbacks modeled for each component
     * @param maxCallbacksPerComponent
     *          The maximum number of callbacks modeled for each component
     */
    public void setMaxCallbacksPerComponent(int maxCallbacksPerComponent) {
        this.maxCallbacksPerComponent = maxCallbacksPerComponent;
    }

    /**
     * Gets the maximum timeout during callback analysis
     * @return The maximum timeout during callback analysis
     */
    public long getMaxTimeout(){
        return maxTimeout;
    }
    /**
     * Sets the maximum timeout during callback analysis
     * @param maxTimeout
     *          The maximum timeout during callback analysis
     */
    public void setMaxTimeout(long maxTimeout){
        this.maxTimeout = maxTimeout;
    }

    /**
     * Gets the maximum callback resolve depth during callback analysis
     * @return The maximum callback resolve depth during callback analysis
     */
    public int getMaxCallbackAnalysisDepth(){
        return maxCallbackAnalysisDepth;
    }
    /**
     * Sets the maximum callback resolve depth during callback analysis
     * @param maxCallbackAnalysisDepth
     *          The maximum callback resolve depth during callback analysis
     */
    public void setMaxCallbackAnalysisDepth(int maxCallbackAnalysisDepth){
        this.maxCallbackAnalysisDepth = maxCallbackAnalysisDepth;
    }

    /**
     * Gets the type of analyzer used during callback analysis
     * @return The type of analyzer used during callback analysis
     */
    public CallbackAnalyzer getCallbackAnalyzer() {
        return callbackAnalyzer;
    }

    /**
     * Sets the type of analyzer used during callback analysis
     * @param callbackAnalyzer
     *          The type of analyzer used during callback analysis
     */
    public void setCallbackAnalyzer(CallbackAnalyzer callbackAnalyzer) {
        this.callbackAnalyzer = callbackAnalyzer;
    }


    // enum parsers
    private static CallgraphAlgorithm parseCallgraphAlgorithm(String algo) {
        if (algo.equalsIgnoreCase("AUTO"))
            return CallgraphAlgorithm.AutomaticSelection;
        else if (algo.equalsIgnoreCase("CHA"))
            return CallgraphAlgorithm.CHA;
        else if (algo.equalsIgnoreCase("VTA"))
            return CallgraphAlgorithm.VTA;
        else if (algo.equalsIgnoreCase("RTA"))
            return CallgraphAlgorithm.RTA;
        else if (algo.equalsIgnoreCase("SPARK"))
            return CallgraphAlgorithm.SPARK;
        else if (algo.equalsIgnoreCase("GEOM"))
            return CallgraphAlgorithm.GEOM;
        else {
            Logger.error(String.format("Invalid callgraph algorithm: %s", algo));
            throw new RuntimeException();
        }
    }

    // TODO remove this section if not in use
    // Variables for paths to Android and APKs
    public final static String ANDROID_JAR = "/Users/dulinglai/Library/Android/sdk/platforms/";
    public final static String APKTOOL_JAR = "/Users/dulinglai/Documents/Study/ResearchProjects/login/scripts/libs/apktool.jar";

    // Results file for debugging
    public final static String RESULT_FILE = "/Users/dulinglai/Documents/Study/ResearchProjects/login/scripts/oauth_login_detector/tmp/oauth_result.txt";
    public final static String TEMP_FOLDER = "./tmp/";

    // Bag of words used for naive classifier
//    public final static String[] userNameWords = {"(?i)(user|account|client|phone|card)[\\s\\_\\-]*(name|id|number|#)",
//            "(?i)(log|sign)[\\s\\_\\-]*in"};
//
//    public final static String[] signUpAliases = {"(?i)(create|need)[\\s\\_\\-]*(a|an|)[\\s\\_\\-]*(new)?[\\s\\_\\-]*(account|user|id)",
//            "(?i)(sign)[\\s\\_\\-]*up"};
//
//    public final static String[] forgotPasswordAliases = {"(?i)forgot[\\s\\_\\-]*(your)?[\\s\\_\\-]*(password|(user|account|card|client)[\\s\\_\\-]*(name|id|number|#))",
//            "(?i)(trouble|help)[\\s\\_\\-]*(signing|logging)[\\s\\_\\-]*in", "(?i)(old|original|new|confirm)[\\s\\_\\-]*(pass|pin)"};
//
//    public final static String[] oauth_providers = {"google", "facebook", "fb", "twitter", "instagram", "kakao"};
//
//    public final static String passwordWords = "(?i)(pass|pin)[\\s\\_\\-]*(word|code)";
}
