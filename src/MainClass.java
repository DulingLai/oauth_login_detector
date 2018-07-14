import config.GlobalConfigs;
import org.apache.commons.cli.*;
import utils.FileUtils;

import java.io.File;
import java.io.IOException;

public class MainClass {

    private final Options options = new Options();

    // Files
    private static final String INPUT_APK_PATH_CONFIG = "i";
    private static final String OUTPUT_APK_PATH_CONFIG = "o";

    // Android
    private static final String ANDROID_SDK_PATH_CONFIG = "s";
    private static final String ANDROID_API_LEVEL_CONFIG = "l";

    // Program Config
    private static final String DEBUG_CONFIG = "d";
    private static final String HELP_CONFIG = "h";
    private static final String VERBOSE_CONFIG = "v";

    private MainClass(){
        setupCmdOptions();
    }

    /**
     *  setup the command line parser
     */
    private void setupCmdOptions() {
        // command line options
        Option input = Option.builder(INPUT_APK_PATH_CONFIG).required(true).longOpt("input").hasArg(true).desc("input apk path (required)").build();
        Option output = Option.builder(OUTPUT_APK_PATH_CONFIG).required(false).longOpt("output").hasArg(true).desc("output directory (required)").build();
        Option sdkPath = Option.builder(ANDROID_SDK_PATH_CONFIG).required(false).longOpt("sdk").hasArg(true).desc("path to android sdk (default value can be set in config file)").build();
        Option apiLevel = Option.builder(ANDROID_API_LEVEL_CONFIG).required(false).type(Number.class).longOpt("api").hasArg(true).desc("api level (default to 23)").build();
        Option debug = new Option(DEBUG_CONFIG, "debug", false, "debug mode (default disabled)");
        Option help = new Option(HELP_CONFIG, "help", false, "print the help message");
        Option verbose = new Option( VERBOSE_CONFIG,"verbose", false,"verbose mode: print more info (default: disabled)" );

        // add the options
        options.addOption(input);
        options.addOption(output);
        options.addOption(sdkPath);
        options.addOption(apiLevel);
        options.addOption(debug);
        options.addOption(help);
        options.addOption(verbose);
    }

    /**
     *  the main function - entry point of the program
     * @param args The command line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception{
        MainClass mainClass = new MainClass();
        mainClass.run(args);
    }

    /**
     * Parse the command line arguments.
     *
     * @param args the command line arguments passed from main().
     */
    private void run(String[] args) throws Exception {
        // Initial check for the number of arguments
        final HelpFormatter formatter = new HelpFormatter();
        if (args.length == 0) {
            formatter.printHelp("alode [OPTIONS]", options, true);
            return;
        }

        // parse the command line options
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            // display the help message if option is specified
            if (cmd.hasOption("h") || cmd.hasOption("help")) {
                formatter.printHelp("alode [OPTIONS]", options, true);
                return;
            }

            // instance of the config obj
            GlobalConfigs config = new GlobalConfigs();

            // parse the options to configs
            parseOptions(cmd, config);

            // print the options for debugging
            if(config.getDebugConfig()){
                System.out.println("[DEBUG] Project Dir: "+config.getProjectPath());
                System.out.println("[DEBUG] Input APK path: "+config.getInputApkPath());
                System.out.println("[DEBUG] Output path: "+config.getOutputPath());
                System.out.println("[DEBUG] Android SDK path: "+config.getAndroidSdkPath());
                System.out.println("[DEBUG] Android API level: "+config.getAndroidApiLevel());
                System.out.println("[DEBUG] Android JAR: "+config.getAndroidJarPath());
            }

        } catch (ParseException e) {
            // print the error message
            System.err.println(e.getMessage());
            formatter.printHelp("alode", options, true);
            System.exit(1);
        }
    }

    private void parseOptions(CommandLine cmd, GlobalConfigs config) {
        // Set the project path configuration variables in the config obj
        config.setProjectPath(System.getProperty("user.dir"));

        // Set apk path and output path
        if (cmd.hasOption(INPUT_APK_PATH_CONFIG) || cmd.hasOption("input")) {
            String apkFile = cmd.getOptionValue(INPUT_APK_PATH_CONFIG);
            if (apkFile != null && !apkFile.isEmpty() && FileUtils.validateFile(apkFile))
                config.setInputApkPath(apkFile);
        } else{
            System.out.println("ERROR: Input APK path is required!");
            System.exit(1);
        }

        if (cmd.hasOption(OUTPUT_APK_PATH_CONFIG) || cmd.hasOption("output")) {
            String outputPath = cmd.getOptionValue(OUTPUT_APK_PATH_CONFIG);
            if (outputPath != null && !outputPath.isEmpty())
                config.setOutputPath(outputPath);
        }

        // Android SDK
        if (cmd.hasOption(ANDROID_SDK_PATH_CONFIG) || cmd.hasOption("sdk")) {
            String adkPath = cmd.getOptionValue(ANDROID_SDK_PATH_CONFIG);
            if (adkPath != null && !adkPath.isEmpty())
                config.setAndroidSdkPath(adkPath);
        }
        if (cmd.hasOption(ANDROID_API_LEVEL_CONFIG) || cmd.hasOption("api")) {
            int apiLevel = Integer.parseInt(cmd.getOptionValue(ANDROID_API_LEVEL_CONFIG));
            config.setAndroidApiLevel(apiLevel);
        }

        // verbose setting
        if (cmd.hasOption(VERBOSE_CONFIG) || cmd.hasOption("verbose"))
            config.setVerboseConfig(true);
        else config.setVerboseConfig(false);

        // debug setting
        if (cmd.hasOption(DEBUG_CONFIG) || cmd.hasOption("debug"))
            config.setDebugConfig(true);
        else config.setDebugConfig(false);

        // load the config file
        String configFilePath = System.getProperty("user.dir") + "/res/config.properties";
        FileUtils.loadConfigFile(configFilePath, config);

        // validate command line options
        if (!FileUtils.validateDir(config.getOutputPath())){
            System.err.println("ERROR: Wrong output path!");
            System.exit(1);
        }
        if (!FileUtils.validateFile(config.getInputApkPath())){
            System.err.println("ERROR: Wrong input apk path!");
            System.exit(1);
        }
        if (!FileUtils.validateDir(config.getAndroidSdkPath())){
            System.err.println("ERROR: Wrong android SDK path!");
            System.exit(1);
        }

        // set the android JAR
        config.setAndroidJarPath(config.getAndroidSdkPath()+"/platforms/android-23/android.jar");
    }
}
