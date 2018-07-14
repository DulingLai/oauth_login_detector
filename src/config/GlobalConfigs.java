package config;

/**
 * The Singleton class used to store configuration variables
 */
public class GlobalConfigs {

    // root directory of the project
    private String project_path;
    private String input_apk_path;
    private String output_path;

    // Android OS related configs
    private String android_sdk_path;
    private String android_jar_path;
    private int android_api_level = 23;

    // boolean config flags
    private boolean debugConfig = true;
    private boolean verboseConfig = true;


    // Getters and setters for configuration variables
    /**
     * Gets the current project working directory
     * @return The current project working directory
     */
    public String getProjectPath(){
        return project_path;
    }
    /**
     * Sets the current project working directory
     * @param projectPath
     *          The path to current project working directory
     */
    public void setProjectPath(String projectPath){
        this.project_path = projectPath;
    }

    /**
     * Gets the path to the input apk file
     * @return The path to the input apk file
     */
    public String getInputApkPath(){
        return input_apk_path;
    }
    /**
     * Sets the path to the input apk file
     * @param inputApkPath
     *          The path to the input apk file
     */
    public void setInputApkPath(String inputApkPath){
        this.input_apk_path = inputApkPath;
    }

    /**
     * Gets the directory for output files
     * @return The output path
     */
    public String getOutputPath(){
        return output_path;
    }
    /**
     * Sets the directory for output files
     * @param outputPath
     *          The path to the output directory
     */
    public void setOutputPath(String outputPath){
        this.output_path = outputPath;
    }

    /**
     * Gets the Android SDK directory
     * @return The directory in which Android SDK is located
     */
    public String getAndroidSdkPath(){
        return android_sdk_path;
    }
    /**
     * Sets the Android SDK directory
     * @param androidSdkPath
     *          The directory in which Android SDK is located
     */
    public void setAndroidSdkPath(String androidSdkPath){
        this.android_sdk_path = androidSdkPath;
    }

    /**
     * Gets the Android JAR Path
     * @return The Android JAR Path
     */
    public String getAndroidJarPath(){
        return android_jar_path;
    }
    /**
     * Sets the path to Android JAR file
     * @param androidJarPath
     *          The path to Android JAR file
     */
    public void setAndroidJarPath(String androidJarPath){
        this.android_jar_path = androidJarPath;
    }

    /**
     * Gets the current Android API Level setting
     * @return The current Android API level setting
     */
    public int getAndroidApiLevel(){
        return android_api_level;
    }
    /**
     * Sets the path to Android JAR file
     * @param androidApiLevel
     *          The target Android API level
     */
    public void setAndroidApiLevel(int androidApiLevel){
        this.android_api_level = androidApiLevel;
    }

    /**
     * Gets the current debug setting
     * @return The current debug setting
     */
    public boolean getDebugConfig(){
        return debugConfig;
    }
    /**
     * Sets the debug setting
     * @param debugConfig
     *          The target debug setting (boolean)
     */
    public void setDebugConfig(boolean debugConfig){
        this.debugConfig = debugConfig;
    }

    /**
     * Gets the current verbose setting
     * @return The current verbose setting
     */
    public boolean getVerboseConfig(){
        return verboseConfig;
    }
    /**
     * Sets the debug setting
     * @param verboseConfig
     *          The target verbose setting (boolean)
     */
    public void setVerboseConfig(boolean verboseConfig){
        this.verboseConfig = verboseConfig;
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
