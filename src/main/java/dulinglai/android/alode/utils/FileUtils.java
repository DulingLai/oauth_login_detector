package dulinglai.android.alode.utils;

import dulinglai.android.alode.config.GlobalConfigs;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.pmw.tinylog.Logger;

import java.io.*;

public class FileUtils {
    // print the debug info to a text file
    public static void printFile(String fileName, String content){
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName,true))) {
            bw.write(content);
            // no need to close it.
            //bw.close();
            //System.out.println("Done");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // run command line apk tool to decompile the apk
    public static void decompile_apk(String apk_path) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", GlobalConfigs.APKTOOL_JAR,"d",apk_path,"-o", GlobalConfigs.TEMP_FOLDER,"-f","-s");
        Process p = pb.start();
    }

    public static boolean validateFile(String filePath) {
        File f = new File(filePath);
        return f.exists();
    }

    public static void constructWidgetHashmap(String widgetMapFile){

    }


    public static void loadConfigFile(String configFilePath, GlobalConfigs config){
        // load properties from the config file
        try {
            PropertiesConfiguration configProp = new PropertiesConfiguration(configFilePath);

            // get the output path
            String outputPathFile = configProp.getString("outputPath");
            String outputPathConfig = config.getAndroidSdkPath();
            // use the android sdk path in the config file if the user does not specifies a path for androidSdk
            if (outputPathConfig == null){
                if (outputPathFile!=null)
                    config.setOutputPath(outputPathFile);
                else {
                    Logger.error("*** ERROR: Output path needs to be specified either in config.properties or through command line options!");
                    System.exit(2);
                }
            } else {
                if (!outputPathFile.equals(outputPathConfig)) {
                    configProp.setProperty("outputPath", outputPathConfig);
                    configProp.save();
                }
            }

            // get the property values
            String androidSdkFile = configProp.getString("androidSdk");
            String androidSdkConfig = config.getAndroidSdkPath();
            // use the android sdk path in the config file if the user does not specifies a path for androidSdk
            if (androidSdkConfig == null){
                if (androidSdkFile!=null)
                    config.setAndroidSdkPath(androidSdkFile);
                else {
                    Logger.error("*** ERROR: Android SDK path needs to be specified either in config.properties or through command line options!");
                    System.exit(2);
                }
            } else {
                if (!androidSdkFile.equals(androidSdkConfig)) {
                    configProp.setProperty("androidSdk", androidSdkConfig);
                    configProp.save();
                }
            }

            // api level
            String androidApiFile = configProp.getString("androidApi");
            int androidApiConfig = config.getAndroidApiLevel();
            // use the android sdk path in the config file if the user does not specifies a path for androidSdk
            if (androidApiFile==null) {
                configProp.setProperty("androidApi", Integer.parseInt(androidApiFile));
                configProp.save();
            } else if (androidApiConfig == 23 && Integer.parseInt(androidApiFile)!=23) {
                    config.setAndroidApiLevel(Integer.parseInt(androidApiFile));
            }

        } catch (ConfigurationException ex){
            Logger.error(ex.getMessage());
        }
    }
}
