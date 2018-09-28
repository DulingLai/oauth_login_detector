package dulinglai.android.alode.analyzers;

import dulinglai.android.alode.resources.resources.LayoutFileParser;
import dulinglai.android.alode.sootData.values.ResourceValueProvider;
import heros.solver.Pair;
import org.pmw.tinylog.Logger;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

/**
 * A widget analyzer that creates widgets by analyzing the jimple file
 */
public class FastJimpleAnalyzer extends AbstractJimpleAnalyzer {

    private static final String ANALYZER = "FastJimpleAnalyzer";

    public FastJimpleAnalyzer(Set<SootClass> entryPointClasses, Set<String> activityList,
                              LayoutFileParser layoutFileParser, ResourceValueProvider resourceValueProvider,
                              MultiMap<SootClass, Pair<Unit, SootMethod>> iccUnitsForWidgetAnalysis)
            throws IOException {
        super(entryPointClasses, 0, activityList, layoutFileParser, resourceValueProvider, iccUnitsForWidgetAnalysis);
        this.editTextWidgetList = new ArrayList<>();
        this.clickWidgetNodeList = new ArrayList<>();
        this.ownershipEdges = new HashMultiMap<>();
    }

    @Override
    public void analyzeJimpleClasses() {
        super.analyzeJimpleClasses();
        Logger.info("[{}] Analyzing Jimple classes in FAST mode...", ANALYZER);

        // Check and add nested activities
        checkAndAddNestedActivities();

        // Find the mappings between classes and layouts
        findClassLayoutMappings();

        // Find all callbacks
        for (SootClass sc : Scene.v().getApplicationClasses()) {
            if (!sc.isConcrete())
                continue;

            for (SootMethod sm : sc.getMethods()) {
                if (sm.isConcrete()) {
                    analyzeMethodForCallbackRegistrations(sc, sm);
                    analyzeMethodForDynamicBroadcastReceiver(sm);
                    analyzeMethodForServiceConnection(sm);
                }
            }
            // Check for method overrides
            analyzeMethodOverrideCallbacks(sc);
        }

        // Find all UI callbacks and assign them to widgets
        for (SootClass callback : callbackMethods.keySet()){
            for (CallbackDefinition callbackDefinition : callbackMethods.get(callback)){
                if (callbackDefinition.getCallbackType() == CallbackDefinition.CallbackType.Widget){
                    uicallbacks.put(callback, callbackDefinition);
                }
            }
        }
    }

    /**
     * Finds the mappings between classes and their respective layout files
     */
    private void findClassLayoutMappings() {
        // Check base activities first, as their sentContentView methods might be overridden
        for (SootClass sc : baseActivitySet) {
            for (SootMethod sm : sc.getMethods()) {
                if (!sm.isConcrete())
                    continue;

                try {
                    checkAndAddClassLayoutMappings(sm, sc,true);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }

        for (SootClass sc : Scene.v().getApplicationClasses()) {
            // We only care about the exported activities
            if (!isExportedActivityClass(sc.getName()))
                continue;

            if (sc.getName().equalsIgnoreCase("com.pizzapizza.activity.LoginActivity"))
                Logger.debug("Here");

            for (SootMethod sm : sc.getMethods()) {
                if (!sm.isConcrete())
                    continue;

                try{
                    // Here we add the method wrappers (for findViewById and setContentView)
                    addMethodWrappers(sm);
                    // Check for class layout mappings
                    checkAndAddClassLayoutMappings(sm, sc,false);
                } catch (RuntimeException e){
                    e.printStackTrace();
                }
            }
        }
    }



    @Override
    public void excludeEntryPoint(SootClass entryPoint) {
        // not supported
    }

}
