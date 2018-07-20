package dulinglai.android.alode;

import dulinglai.android.alode.config.GlobalConfigs;
import dulinglai.android.alode.config.soot.SootSettings;
import dulinglai.android.alode.utils.FileUtils;
import dulinglai.android.alode.resources.AppResources;
import org.apache.commons.cli.*;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;

public class MainClass {

    private final Options options = new Options();

    // Files
    private static final String INPUT_APK_PATH_CONFIG = "i";
    private static final String OUTPUT_APK_PATH_CONFIG = "o";

    // Analysis Config
    private static final String CG_ALGO = "cg";
    private static final String MAX_CALLBACK = "cb";
    private static final String MAX_TIMEOUT = "t";

    // Android
    private static final String ANDROID_SDK_PATH_CONFIG = "s";
    private static final String ANDROID_API_LEVEL_CONFIG = "l";

    // Program Config
    private static final String DEBUG_CONFIG = "d";
    private static final String HELP_CONFIG = "h";
    private static final String VERSION_CONFIG = "v";

    private MainClass(){
        setupCmdOptions();
    }

    /**
     *  setup the command line parser
     */
    private void setupCmdOptions() {
        // command line options
        Option input = Option.builder(INPUT_APK_PATH_CONFIG).required(true).longOpt("input").hasArg(true).desc("input apk path (required)").build();
        Option output = Option.builder(OUTPUT_APK_PATH_CONFIG).required(false).longOpt("output").hasArg(true).desc("output directory (default to \"sootOutput\")").build();
        Option sdkPath = Option.builder(ANDROID_SDK_PATH_CONFIG).required(false).longOpt("sdk").hasArg(true).desc("path to android sdk (default value can be set in config file)").build();
        Option apiLevel = Option.builder(ANDROID_API_LEVEL_CONFIG).required(false).type(Number.class).longOpt("api").hasArg(true).desc("api level (default to 23)").build();
        Option maxCallback = Option.builder(MAX_CALLBACK).required(false).type(Number.class).hasArg(true).desc("the maximum number of callbacks modeled for each component (default to 20)").build();
        Option cgAlgo = Option.builder(CG_ALGO).required(false).hasArg(true).desc("callgraph algorithm to use (AUTO, CHA, VTA, RTA, SPARK, GEOM); default: AUTO").build();
        Option timeOut = Option.builder(MAX_TIMEOUT).required(false).hasArg(true).desc("maximum timeout during callback analysis in seconds (default: 60)").build();
        Option debug = new Option(DEBUG_CONFIG, "debug", false, "debug mode (default disabled)");
        Option help = new Option(HELP_CONFIG, "help", false, "print the help message");
        Option version = new Option( VERSION_CONFIG,"version", false,"print version info" );

        // add the options
        options.addOption(input);
        options.addOption(output);
        options.addOption(sdkPath);
        options.addOption(apiLevel);
        options.addOption(cgAlgo);
        options.addOption(timeOut);
        options.addOption(maxCallback);
        options.addOption(debug);
        options.addOption(help);
        options.addOption(version);
    }

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

            // display version info and exit
            if (cmd.hasOption(VERSION_CONFIG) || cmd.hasOption("version")){
                System.out.println("alode " + getClass().getPackage().getImplementationVersion());
                return;
            }

            // instance of the config obj
            GlobalConfigs config = new GlobalConfigs();

            // parse the options to configs
            parseOptions(cmd, config);

            // print the options for debugging
            Logger.debug("Project Dir: "+config.getProjectPath());
            Logger.debug("Input APK path: "+config.getInputApkPath());
            Logger.debug("Output path: "+config.getOutputPath());
            Logger.debug("Android SDK path: "+config.getAndroidSdkPath());
            Logger.debug("Android API level: "+config.getAndroidApiLevel());
            Logger.debug("Android JAR: "+config.getAndroidJarPath());

            // Setup application for analysis
            SetupApplication app = new SetupApplication(config);

            // run the analysis
            app.runAnalysis();

        } catch (ParseException e) {
            // print the error message
            Logger.error(e.getMessage());
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
            if (apkFile != null && !apkFile.isEmpty())
                config.setInputApkPath(apkFile);
        } else{
            Logger.error("ERROR: Input APK path is required!");
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
            config.setForceAndroidJar(true);
        }

        // analysis setting
        if (cmd.hasOption(MAX_CALLBACK))
            config.setMaxCallbacksPerComponent(Integer.parseInt(cmd.getOptionValue(MAX_CALLBACK)));

        if (cmd.hasOption(CG_ALGO))
            config.setCallgraphAlgorithm(cmd.getOptionValue(CG_ALGO));

        if (cmd.hasOption(MAX_TIMEOUT))
            config.setMaxTimeout(Long.parseLong(cmd.getOptionValue(MAX_TIMEOUT)));


        // log level setting (debug/info/production)
        if (cmd.hasOption(DEBUG_CONFIG) || cmd.hasOption("debug"))
            Configurator.currentConfig().formatPattern("[{level}] {class_name}.{method}(): {message}").level(Level.DEBUG).activate();
        else
            Configurator.currentConfig().formatPattern("{level}: {message}").activate();

        // load the config file
//        String configFilePath = System.getProperty("user.dir") + "/res/config.properties";
        String configFilePath = "config.properties";
        FileUtils.loadConfigFile(configFilePath, config);

        // validate command line options
        if (!FileUtils.validateFile(config.getOutputPath())){
            Logger.error("Wrong output path!");
            System.exit(1);
        }
        if (!FileUtils.validateFile(config.getInputApkPath())){
            Logger.error("Wrong input apk path!");
            System.exit(1);
        }
        if (!FileUtils.validateFile(config.getAndroidSdkPath())){
            Logger.error("Wrong android SDK path!");
            System.exit(1);
        }

        // set the android JAR
        int targetApiLevel = config.getAndroidApiLevel();
        if(targetApiLevel==23)
            config.setAndroidJarPath(config.getAndroidSdkPath()+"/platforms/android-23/android.jar");
        else
            config.setAndroidJarPath(config.getAndroidSdkPath()+"/platforms/android-"+targetApiLevel+"/android.jar");

    }
}
