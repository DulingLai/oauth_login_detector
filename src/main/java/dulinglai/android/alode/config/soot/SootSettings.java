package dulinglai.android.alode.config.soot;

import dulinglai.android.alode.config.GlobalConfigs;
import dulinglai.android.alode.utils.sootUtils.LibraryClassPatcher;
import org.pmw.tinylog.Logger;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.options.Options;

import java.util.Collections;

public class SootSettings {

    private static final String TAG = "SOOT";

    // TODO: insert config params that can be set on runtime
    private SootHelper sootHelper = new SootHelper();
    private GlobalConfigs configs = new GlobalConfigs();

    public SootSettings(GlobalConfigs config){
        this.configs = config;
    }

    public void initializeSoot(){
        Logger.info("[{}] Initializing Soot...",TAG);

        final String androidJar = configs.getAndroidJarPath();
        final String apkFilePath = configs.getInputApkPath();

        Logger.debug("Using Android Jar: {}", androidJar);
        Logger.debug("Input APK file: {}", apkFilePath);

        // clean up soot instance
        G.reset();

        Options.v().set_force_overwrite(true);
        Options.v().set_allow_phantom_refs(true);       // allow soot to create phantom ref for unknown classes
        //prefer Android APK files// -src-prec apk
        Options.v().set_src_prec(Options.src_prec_apk_class_jimple);
        Options.v().set_whole_program(true);

        Options.v().set_output_format(Options.output_format_none);      //output as none
        Options.v().set_process_multiple_dex(true);     // enable analysis on multi-dex APKs

        Options.v().set_android_jars(androidJar);       //Set android jar location

        // set target APK
        Options.v().set_process_dir(Collections.singletonList(apkFilePath));

        // other Soot settings
        Options.v().set_keep_line_number(false);
        Options.v().set_keep_offset(false);
        Options.v().set_throw_analysis(Options.throw_analysis_dalvik);
        Options.v().set_ignore_resolution_errors(true);

        if (sootHelper != null)
            sootHelper.setSootExcludeLibs(Options.v());

        // set classpath
        Options.v().set_soot_classpath(getClasspath(androidJar, configs.getAndroidSdkPath(), apkFilePath, configs.getForceAndroidJar()));

        soot.Main.v().autoSetOptions();

        // Configure the callgraph algorithm
        if (sootHelper != null)
            sootHelper.setSootCallgraphAlgorithm(configs.getCallgraphAlgorithm());
            // Add basic classes
            sootHelper.loadSootClasses();

        Logger.info("[{}] Loading dex files...",TAG);
        Scene.v().loadNecessaryClasses();

        // Make sure that we have valid Jimple bodies
        PackManager.v().getPack("wjpp").apply();

        // Patch the callgraph to support additional edges. We do this now,
        // because during callback discovery, the context-insensitive callgraph
        // algorithm would flood us with invalid edges.
        LibraryClassPatcher patcher = new LibraryClassPatcher();
        patcher.patchLibraries();

        Logger.info("[{}] Complete soot initialization...",TAG);
    }

    /**
     * Builds the classpath for this analysis
     *
     * @return The classpath to be used for the taint analysis
     */
    private String getClasspath(String androidJar, String androidSdk, String apkFileLocation, boolean forceAndroidJar) {
//        final String additionalClasspath = configs.getAdditionalClasspath();
        String androidPlatform = androidSdk + "platforms";

        String classpath = forceAndroidJar? androidJar : Scene.v().getAndroidJarPath(androidPlatform, apkFileLocation);
//        if (additionalClasspath != null && !additionalClasspath.isEmpty())
//            classpath += File.pathSeparator + additionalClasspath;
        Logger.debug("[{}] Soot classpath: " + classpath,TAG);
        return classpath;
    }
}
