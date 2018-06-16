package config;

import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.options.Options;

import java.util.Collections;

public class Settings {

    // TODO: insert config params that can be set on runtime

    public static void initializeSoot(String apkPath){
        G.reset();
        Options.v().set_force_overwrite(true);
        Options.v().set_allow_phantom_refs(true);       // allow soot to create phantom ref for unknown classes
        Options.v().set_prepend_classpath(true);        //prepend the VM's classpath to Soot's own classpath
        //prefer Android APK files// -src-prec apk
        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_whole_program(true);
        Options.v().set_app(true);

        //output as APK, too//-f J
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_process_multiple_dex(true);

        Options.v().set_android_jars(Constants.ANDROID_JAR);       //Set android jar location
        Options.v().set_force_android_jar(Constants.ANDROID_JAR + "android-21/android.jar");
        Options.v().set_process_dir(Collections.singletonList(apkPath));        // set target APK

//        Options.v().set_validate(true);
        // Set basic class
        Scene.v().addBasicClass("java.io.PrintStream", SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.System",SootClass.SIGNATURES);

        Scene.v().loadNecessaryClasses();
    }
}
