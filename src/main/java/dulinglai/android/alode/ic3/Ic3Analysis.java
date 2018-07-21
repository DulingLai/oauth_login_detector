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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dulinglai.android.alode.callbacks.CallbackDefinition;
import org.pmw.tinylog.Logger;
import org.xmlpull.v1.XmlPullParserException;

import edu.psu.cse.siis.coal.Analysis;
import edu.psu.cse.siis.coal.AnalysisParameters;
import edu.psu.cse.siis.coal.FatalAnalysisException;
import edu.psu.cse.siis.coal.PropagationSceneTransformer;
import edu.psu.cse.siis.coal.PropagationSceneTransformerFilePrinter;
import edu.psu.cse.siis.coal.SymbolFilter;
import edu.psu.cse.siis.coal.arguments.ArgumentValueManager;
import edu.psu.cse.siis.coal.arguments.MethodReturnValueManager;
import edu.psu.cse.siis.coal.field.transformers.FieldTransformerManager;
import dulinglai.android.alode.ic3.manifest.ManifestPullParser;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Value;
import soot.jimple.StaticFieldRef;
import dulinglai.android.alode.sootData.AndroidMethod;
import dulinglai.android.alode.resources.manifest.ProcessManifest;
import soot.options.Options;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

public class Ic3Analysis extends Analysis<Ic3Config> {
    private static final String INTENT = "android.content.Intent";
    private static final String INTENT_FILTER = "android.content.IntentFilter";
    private static final String BUNDLE = "android.os.Bundle";
    private static final String COMPONENT_NAME = "android.content.ComponentName";
    private static final String ACTIVITY = "android.app.Activity";

    private static final String TAG = "IC3";

    private static final String[] frameworkClassesArray =
            { INTENT, INTENT_FILTER, BUNDLE, COMPONENT_NAME, ACTIVITY };
    protected static final List<String> frameworkClasses = Arrays.asList(frameworkClassesArray);

    private Ic3Data.Application.Builder ic3Builder;
    private Map<String, Ic3Data.Application.Component.Builder> componentNameToBuilderMap;

    protected String outputDir;
    protected Writer writer;
    protected ManifestPullParser detailedManifest;
    protected Map<String, Integer> componentToIdMap;
    protected String packageName;
    protected String apkPath;

    private Set<SootClass> entrypoints = null;
    private MultiMap<SootClass, CallbackDefinition> callbackMethods;

    // Constructor
    public Ic3Analysis(Ic3Config ic3Config, MultiMap<SootClass, CallbackDefinition> callbackMethods, Set<SootClass> entrypoints) {
        this.apkPath = ic3Config.getInput();
        this.packageName = ic3Config.getPackageName();
        this.outputDir = ic3Config.getProtobufDestination();
        this.entrypoints = entrypoints;
        this.callbackMethods = callbackMethods;
    }

    @Override
    protected void registerFieldTransformerFactories(Ic3Config ic3Config) {
        Timers.v().totalTimer.start();
        FieldTransformerManager.v().registerDefaultFieldTransformerFactories();
    }

    @Override
    protected void registerArgumentValueAnalyses(Ic3Config ic3Config) {
        ArgumentValueManager.v().registerDefaultArgumentValueAnalyses();
        ArgumentValueManager.v().registerArgumentValueAnalysis("classType",
                new ClassTypeValueAnalysis());
        ArgumentValueManager.v().registerArgumentValueAnalysis("authority",
                new AuthorityValueAnalysis());
        ArgumentValueManager.v().registerArgumentValueAnalysis("Set<authority>",
                new AuthorityValueAnalysis());
        ArgumentValueManager.v().registerArgumentValueAnalysis("path", new PathValueAnalysis());
        ArgumentValueManager.v().registerArgumentValueAnalysis("Set<path>", new PathValueAnalysis());
    }

    @Override
    protected void registerMethodReturnValueAnalyses(Ic3Config ic3Config) {
        MethodReturnValueManager.v().registerDefaultMethodReturnValueAnalyses();
    }

    @Override
    protected void initializeAnalysis(Ic3Config ic3Config)
            throws FatalAnalysisException {
        long startTime = System.currentTimeMillis() / 1000;
        outputDir = ic3Config.getProtobufDestination();

        // Prepare the manifest file
        detailedManifest = new ManifestPullParser();
        detailedManifest.loadManifestFile(apkPath);

        // prepare the output builder
        if (outputDir != null) {
            ic3Builder = Ic3Data.Application.newBuilder();
            ic3Builder.setAnalysisStart(startTime);
            componentNameToBuilderMap = detailedManifest.populateProtobuf(ic3Builder);
        } else {
            Logger.error("[{}] Failed to resolve the output path!", TAG);
        }

        // calculate the callback methods
//        Set<String> entryPointClasses = null;
//        //TODO Determine if we want to use FlowDroid's approach to process the manifest or IC3's
//        if (detailedManifest == null) {
//            entryPointClasses = ic3Config.getEntryPointClasses();
//            packageName = ic3Config.getPackageName();
//        } else {
//            entryPointClasses = detailedManifest.getEntryPointClasses();
//            packageName = detailedManifest.getPackageName();
//        }

        Timers.v().misc.start();

        // Application package name is now known.
        ArgumentValueManager.v().registerArgumentValueAnalysis("context",
                new ContextValueAnalysis(packageName));
        AndroidMethodReturnValueAnalyses.registerAndroidMethodReturnValueAnalyses(packageName);

        if (outputDir != null && packageName != null) {
            String outputFile = String.format("%s/%s.csv", outputDir, packageName);

            try {
                writer = new BufferedWriter(new FileWriter(outputFile, false));
            } catch (IOException e1) {
                Logger.error("Could not open file " + outputFile, e1);
            }
        }

        // reset Soot:
        soot.G.reset();

        Map<SootMethod, Set<String>> entryPointMap =
                ic3Config.computeComponents() ? new HashMap<>() : null;
        addSceneTransformer(entryPointMap);

        if (ic3Config.computeComponents()) {
            addEntryPointMappingSceneTransformer(entrypoints, callbackMethods, entryPointMap);
        }
        Options.v().set_ignore_resolution_errors(true);
        Options.v().set_debug(false);
        Options.v().set_verbose(false);
        Options.v().set_unfriendly_mode(true);

        Options.v().set_no_bodies_for_excluded(false);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_whole_program(true);

        Options.v().set_soot_classpath(ic3Config.getInput());

        Options.v().set_force_android_jar("ic3-android.jar");

        Options.v().set_ignore_resolution_errors(true);

        Options.v().setPhaseOption("cg.spark", "on");
        // do not merge variables (causes problems with PointsToSets)
        Options.v().setPhaseOption("jb.ulp", "off");
        Options.v().setPhaseOption("jb.uce", "remove-unreachable-traps:true");
        Options.v().setPhaseOption("cg", "trim-clinit:false");
        Options.v().set_prepend_classpath(true);
        Options.v().set_process_multiple_dex(true);

        if (AnalysisParameters.v().useShimple()) {
            Options.v().set_via_shimple(true);
            Options.v().set_whole_shimple(true);
        }

        Options.v().set_src_prec(Options.src_prec_apk);
        Timers.v().misc.end();

        Timers.v().classLoading.start();
        for (String frameworkClass : frameworkClasses) {
            SootClass c = Scene.v().loadClassAndSupport(frameworkClass);
            Scene.v().forceResolve(frameworkClass, SootClass.BODIES);
            c.setApplicationClass();
        }

        Scene.v().loadNecessaryClasses();
        Timers.v().classLoading.end();

        for (SootClass sc : Scene.v().getClasses()) {
            if (sc.resolvingLevel() == SootClass.DANGLING) {
                sc.setResolvingLevel(SootClass.BODIES);
                sc.setPhantomClass();
            }
        }
    }

    @Override
    protected void setApplicationClasses(Ic3Config ic3Config)
            throws FatalAnalysisException {
        AnalysisParameters.v()
                .addAnalysisClasses(computeAnalysisClasses(ic3Config.getInput()));
        AnalysisParameters.v().addAnalysisClasses(frameworkClasses);
    }

    @Override
    protected void handleFatalAnalysisException(Ic3Config ic3Config, FatalAnalysisException exception) {
        Logger.error("Could not process application {}: {}", packageName, exception);

        if (outputDir != null && packageName != null) {
            try {
                if (writer == null) {
                    String outputFile = String.format("%s/%s.csv", outputDir, packageName);

                    writer = new BufferedWriter(new FileWriter(outputFile, false));
                }

                writer.write(ic3Config.getInput() + " -1\n");
                writer.close();
            } catch (IOException e1) {
                Logger.error("Could not write to file after failure to process application: {}", e1);
            }
        }
    }

    @Override
    protected void processResults(Ic3Config ic3Config)
            throws FatalAnalysisException {
        if (ic3Config.getProtobufDestination() != null) {
            ProtobufResultProcessor resultProcessor = new ProtobufResultProcessor();
            try {
                resultProcessor.processResult(packageName, ic3Builder,
                        ic3Config.getProtobufDestination(), ic3Config.binary(),
                        componentNameToBuilderMap, AnalysisParameters.v().getAnalysisClasses().size(), writer);
            } catch (IOException e) {
                Logger.error("Could not process analysis results: {}", e);
                throw new FatalAnalysisException();
            }
        } else {
            Logger.error("Could not process analysis results! Output path not valid!");
            throw new FatalAnalysisException();
        }
    }

    @Override
    protected void finalizeAnalysis(Ic3Config ic3Config) throws FatalAnalysisException {

    }

    protected void addSceneTransformer(Map<SootMethod, Set<String>> entryPointMap) {
        Ic3ResultBuilder resultBuilder = new Ic3ResultBuilder();
        resultBuilder.setEntryPointMap(entryPointMap);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String debugDirPath = System.getProperty("user.home") + File.separator + "debug";
        File debugDir = new File(debugDirPath);
        if (!debugDir.exists()) {
            debugDir.mkdir();
        }

        String fileName = dateFormat.format(new Date()) + ".txt";
        String debugFilename = debugDirPath + File.separator + fileName;

        String pack = AnalysisParameters.v().useShimple() ? "wstp" : "wjtp";
        Transform transform =
                new Transform(pack + ".ifds", new PropagationSceneTransformer(resultBuilder,
                        new PropagationSceneTransformerFilePrinter(debugFilename, new SymbolFilter() {

                            @Override
                            public boolean filterOut(Value symbol) {
                                return symbol instanceof StaticFieldRef && ((StaticFieldRef) symbol).getField()
                                        .getDeclaringClass().getName().startsWith("android.provider");
                            }
                        })));
        if (PackManager.v().getPack(pack).get(pack + ".ifds") == null) {
            PackManager.v().getPack(pack).add(transform);
        } else {
            Iterator<?> it = PackManager.v().getPack(pack).iterator();
            while (it.hasNext()) {
                Object current = it.next();
                if (current instanceof Transform
                        && ((Transform) current).getPhaseName().equals(pack + ".ifds")) {
                    it.remove();
                    break;
                }

            }
            PackManager.v().getPack(pack).add(transform);
        }
    }

    protected void addEntryPointMappingSceneTransformer(Set<SootClass> entryPoints,
                                                        MultiMap<SootClass, CallbackDefinition> callbackMethods, Map<SootMethod, Set<String>> entryPointMap) {
        String pack = AnalysisParameters.v().useShimple() ? "wstp" : "wjtp";

        Transform transform = new Transform(pack + ".epm",
                new EntryPointMappingSceneTransformer(entryPoints, callbackMethods, entryPointMap));
        if (PackManager.v().getPack(pack).get(pack + ".epm") == null) {
            PackManager.v().getPack(pack).add(transform);
        } else {
            Iterator<?> it = PackManager.v().getPack(pack).iterator();
            while (it.hasNext()) {
                Object current = it.next();
                if (current instanceof Transform
                        && ((Transform) current).getPhaseName().equals(pack + ".epm")) {
                    it.remove();
                    break;
                }

            }
            PackManager.v().getPack(pack).add(transform);
        }
    }

    @Override
    protected Set<String> computeAnalysisClassesInApk(String apkPath) {
        Set<String> result = new HashSet<>();

        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_ignore_resolution_errors(true);
        Options.v().set_debug(false);
        Options.v().set_verbose(false);
        Options.v().set_unfriendly_mode(true);

        soot.options.Options.v().set_process_dir(Collections.singletonList(apkPath));

        soot.options.Options.v().set_allow_phantom_refs(true);

        Scene.v().loadNecessaryClasses();

        for (SootClass className : Scene.v().getApplicationClasses()) {
            String name = className.getName();
            if (!name.startsWith("android.app.FragmentManager")) {
                result.add(name);
            }
        }

        soot.G.reset();

        return result;
    }
}
