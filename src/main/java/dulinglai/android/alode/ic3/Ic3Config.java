/*
 * Copyright (C) 2015 The Pennsylvania State University and the University of Wisconsin
 * Systems and Internet Infrastructure Security Laboratory
 *
 * Author: Damien Octeau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dulinglai.android.alode.ic3;

import edu.psu.cse.siis.coal.CommandLineArguments;

import java.util.Set;

/**
 * Command line arguments for IC3.
 */
public class Ic3Config extends CommandLineArguments {

    private static final String DEFAULT_COMPILED_MODEL_PATH = "/res/icc.cmodel";

    private String inputApkPath;
    private final boolean computeComponents = true;
    private String protobufDestination;
    private boolean binary;
    private String appCategory="Default";

    // soot configs
    private String packageName;
    private Set<String> entryPointClasses;

    public Ic3Config(String inputApkPath, String packageName, String androidJar, String outputDir, Set<String> entryPointClasses){
        this.inputApkPath = inputApkPath;
        this.setAndroidJar(androidJar);
        setCompiledModel(DEFAULT_COMPILED_MODEL_PATH);
        this.protobufDestination = outputDir + "/ic3results";
        this.packageName = packageName;
        this.entryPointClasses = entryPointClasses;
    }

    /**
     * Gets the path to the input .apk file.
     *
     * @return The path to the input .apk file.
     */
    public String getInput() {
        return inputApkPath;
    }

    /**
     * Gets the entry point classes of the given .apk file.
     *
     * @return The entry point classes of the given .apk file.
     */
    public Set<String> getEntryPointClasses() {
        return entryPointClasses;
    }

    /**
     * Gets the package name of the input .apk file.
     *
     * @return The package name of the input .apk file.
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Determines if mappings between ICC-sending locations and the components that contain them
     * should be computed.
     *
     * @return True if the components that contain ICC-sending locations should be determined.
     */
    public boolean computeComponents() {
        return computeComponents;
    }

    /**
     * Returns the destination protocol buffer file path.
     *
     * @return The destination path if any, otherwise null.
     */
    public String getProtobufDestination() {
        return protobufDestination;
    }

    /**
     * Determines if the output should be binary, in the case of a protobuf output.
     *
     * @return True if the output should be binary.
     */
    public boolean binary() {
        return binary;
    }

    /**
     * Returns the name of the app category.
     *
     * @return The app category name, if any
     */
    public String getAppCategory() {
        return appCategory;
    }


    /**
     * Process the command line arguments after initial parsing. This should be called be actually
     * using the arguments contained in this class.
     */
    public void processConfig() {

        if (hasOption("category")) {
            appCategory = getOptionValue("category","Default");
        }

        // computeComponents = hasOption("computecomponents") || db != null || protobufDestination !=
        // null;
        // binary = hasOption("binary");
    }
}
