package dulinglai.android.alode.analyzers;

import dulinglai.android.alode.analyzers.CallbackDefinition.CallbackType;
import dulinglai.android.alode.analyzers.filters.ICallbackFilter;
import dulinglai.android.alode.memory.IMemoryBoundedSolver;
import dulinglai.android.alode.memory.ISolverTerminationReason;
import dulinglai.android.alode.resources.androidConstants.ComponentConstants;
import dulinglai.android.alode.resources.resources.LayoutFileParser;
import dulinglai.android.alode.sootData.values.ResourceValueProvider;
import dulinglai.android.alode.utils.androidUtils.ClassUtils;
import dulinglai.android.alode.utils.androidUtils.SystemClassHandler;
import org.pmw.tinylog.Logger;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.util.Chain;
import soot.util.HashMultiMap;
import soot.util.MultiMap;
import soot.util.queue.QueueReader;

import java.io.IOException;
import java.util.*;

/**
 * Default implementation of the callback analyzer class. This implementation
 * aims for precision. It tries to rule out callbacks registered in unreachable
 * code. The mapping between components and callbacks is as precise as possible.
 *
 * @author Steven Arzt
 *
 */
public class DefaultJimpleAnalyzer extends AbstractJimpleAnalyzer implements IMemoryBoundedSolver {

    private MultiMap<SootClass, SootMethod> callbackWorklist = null;
    private ClassUtils entryPointUtils = new ClassUtils();
    private Set<IMemoryBoundedSolverStatusNotification> notificationListeners = new HashSet<>();
    private ISolverTerminationReason isKilled = null;

    //TODO remove result writer

    public DefaultJimpleAnalyzer(Set<SootClass> entryPointClasses, int maxCallbacksPerComponent,
                                 Set<String> activityList, LayoutFileParser layoutFileParser,
                                 ResourceValueProvider resourceValueProvider) throws IOException {
        super(entryPointClasses, maxCallbacksPerComponent, activityList, layoutFileParser,
                resourceValueProvider);
    }

    public DefaultJimpleAnalyzer(Set<SootClass> entryPointClasses,
                                 String callbackFile, int maxCallbacksPerComponent,
                                 Set<String> activityList, LayoutFileParser layoutFileParser,
                                 ResourceValueProvider resourceValueProvider) throws IOException {
        super(entryPointClasses, callbackFile, maxCallbacksPerComponent, activityList,
                layoutFileParser, resourceValueProvider);
    }

    public DefaultJimpleAnalyzer(Set<SootClass> entryPointClasses,
                                 Set<String> androidCallbacks, int maxCallbacksPerComponent,
                                 Set<String> activityList, LayoutFileParser layoutFileParser,
                                 ResourceValueProvider resourceValueProvider) {
        super(entryPointClasses, androidCallbacks, maxCallbacksPerComponent, activityList,
                layoutFileParser, resourceValueProvider);
    }

    /**
     * Collects the callback methods for all Android default handlers implemented in
     * the source code. Note that this operation runs inside Soot, so this method
     * only registers a new phase that will be executed when Soot is next run
     */
    @Override
    public void analyzeJimpleClasses() {
        super.analyzeJimpleClasses();

        Transform transform = new Transform("wjtp.ajc", new SceneTransformer() {
            protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
                // Notify the listeners that the solver has been started
                for (IMemoryBoundedSolverStatusNotification listener : notificationListeners)
                    listener.notifySolverStarted(DefaultJimpleAnalyzer.this);

                // Do we have to start from scratch or do we have a worklist to
                // process?
                if (callbackWorklist == null) {
                    Logger.info("Collecting callbacks in DEFAULT mode...");
                    callbackWorklist = new HashMultiMap<>();

                    // Find the mappings between classes and layouts
                    findClassLayoutMappings();

                    // Process the callback classes directly reachable from the
                    // entry points
                    for (SootClass sc : entryPointClasses) {
                        // Check whether we're still running
                        if (isKilled != null)
                            break;

                        List<MethodOrMethodContext> methods = new ArrayList<MethodOrMethodContext>(
                                getLifecycleMethods(sc));

                        // Check for callbacks registered in the code
                        analyzeRechableMethods(sc, methods);

                        // Check for method overrides
                        analyzeMethodOverrideCallbacks(sc);
                    }
                    Logger.info("Callback analysis done.");
                } else {
                    // Incremental mode, only process the worklist
                    Logger.info(String.format("Running incremental callback analysis for %d components...",
                            callbackWorklist.size()));
                    for (Iterator<SootClass> classIt = callbackWorklist.keySet().iterator(); classIt.hasNext();) {
                        // Check whether we're still running
                        if (isKilled != null)
                            break;

                        SootClass componentClass = classIt.next();
                        Set<SootMethod> callbacks = callbackWorklist.get(componentClass);

                        // Check whether we're already beyond the maximum number
                        // of callbacks
                        // for the current component
                        if (maxCallbacksPerComponent > 0
                                && callbacks.size() > maxCallbacksPerComponent) {
                            callbackMethods.remove(componentClass);
                            entryPointClasses.remove(componentClass);
                            classIt.remove();
                            continue;
                        }

                        List<MethodOrMethodContext> entryClasses = new ArrayList<>(callbacks.size());
                        for (SootMethod sm : callbacks)
                            entryClasses.add(sm);

                        analyzeRechableMethods(componentClass, entryClasses);
                        classIt.remove();
                    }
                    Logger.info("Incremental callback analysis done.");
                }

                // Notify the listeners that the solver has been terminated
                for (IMemoryBoundedSolverStatusNotification listener : notificationListeners)
                    listener.notifySolverTerminated(DefaultJimpleAnalyzer.this);
            }

        });
        PackManager.v().getPack("wjtp").add(transform);
    }

    /**
     * Gets all lifecycle methods in the given entry point class
     *
     * @param sc
     *            The class in which to look for lifecycle methods
     * @return The set of lifecycle methods in the given class
     */
    private Collection<? extends MethodOrMethodContext> getLifecycleMethods(SootClass sc) {
        switch (entryPointUtils.getComponentType(sc)) {
            case Activity:
                return getLifecycleMethods(sc, ComponentConstants.getActivityLifecycleMethods());
            case Service:
                return getLifecycleMethods(sc, ComponentConstants.getServiceLifecycleMethods());
            case Application:
                return getLifecycleMethods(sc, ComponentConstants.getApplicationLifecycleMethods());
            case BroadcastReceiver:
                return getLifecycleMethods(sc, ComponentConstants.getBroadcastLifecycleMethods());
            case Fragment:
                return getLifecycleMethods(sc, ComponentConstants.getFragmentLifecycleMethods());
            case ContentProvider:
                return getLifecycleMethods(sc, ComponentConstants.getContentproviderLifecycleMethods());
            case GCMBaseIntentService:
                return getLifecycleMethods(sc, ComponentConstants.getGCMIntentServiceMethods());
            case GCMListenerService:
                return getLifecycleMethods(sc, ComponentConstants.getGCMListenerServiceMethods());
            case ServiceConnection:
                return getLifecycleMethods(sc, ComponentConstants.getServiceConnectionMethods());
            case Plain:
                return Collections.emptySet();
        }
        return Collections.emptySet();
    }

    /**
     * This method takes a lifecycle class and the list of lifecycle method
     * subsignatures. For each subsignature, it checks whether the given class or
     * one of its superclass overwrites the respective methods. All findings are
     * collected in a set and returned.
     *
     * @param sc
     *            The class in which to look for lifecycle method implementations
     * @param methods
     *            The list of lifecycle method subsignatures for the type of
     *            component that the given class corresponds to
     * @return The set of implemented lifecycle methods in the given class
     */
    private Collection<? extends MethodOrMethodContext> getLifecycleMethods(SootClass sc, List<String> methods) {
        Set<MethodOrMethodContext> lifecycleMethods = new HashSet<>();
        SootClass currentClass = sc;
        while (currentClass != null) {
            for (String sig : methods) {
                SootMethod sm = currentClass.getMethodUnsafe(sig);
                if (sm != null)
                    if (!SystemClassHandler.isClassInSystemPackage(sm.getDeclaringClass().getName()))
                        lifecycleMethods.add(sm);
            }
            currentClass = currentClass.hasSuperclass() ? currentClass.getSuperclass() : null;
        }
        return lifecycleMethods;
    }

    private void analyzeRechableMethods(SootClass lifecycleElement, List<MethodOrMethodContext> methods) {
        // Make sure to exclude all other edges in the callgraph except for the
        // edges start in the lifecycle methods we explicitly pass in
        ComponentReachableMethods rm = new ComponentReachableMethods(lifecycleElement, methods);
        rm.update();

        // Scan for listeners in the class hierarchy
        QueueReader<MethodOrMethodContext> reachableMethods = rm.listener();
        while (reachableMethods.hasNext()) {
            // Check whether we're still running
            if (isKilled != null)
                break;

            for (ICallbackFilter filter : callbackFilters)
                filter.setReachableMethods(rm);

            SootMethod method = reachableMethods.next().method();
            analyzeMethodForCallbackRegistrations(lifecycleElement, method);
            analyzeMethodForDynamicBroadcastReceiver(method);
            analyzeMethodForServiceConnection(method);
            analyzeMethodForFragmentTransaction(lifecycleElement, method);
        }
    }

    @Override
    protected boolean checkAndAddMethod(SootMethod method, SootMethod parentMethod, SootClass lifecycleClass,
                                        CallbackType callbackType) {
        if (!this.excludedEntryPoints.contains(lifecycleClass)) {
            if (super.checkAndAddMethod(method, parentMethod, lifecycleClass, callbackType)) {
                // Has this entry point been excluded?
                this.callbackWorklist.put(lifecycleClass, method);
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the mappings between classes and their respective layout files
     */
    private void findClassLayoutMappings() {
        Iterator<SootClass> classIterator = Scene.v().getApplicationClasses().iterator();
        Iterator<MethodOrMethodContext> rmIterator = Scene.v().getReachableMethods().listener();
        while (rmIterator.hasNext()) {
            SootMethod sm = rmIterator.next().method();
            if (!sm.isConcrete())
                continue;
            if (SystemClassHandler.isClassInSystemPackage(sm.getDeclaringClass().getName()))
                continue;

            // Here we try to prevent the multi-catch bug from happening
            try{
                Chain<Unit> units = sm.retrieveActiveBody().getUnits();
                for (Unit u : units)
                    if (u instanceof Stmt) {
                        Stmt stmt = (Stmt) u;
                        if (stmt.containsInvokeExpr()) {
                            InvokeExpr inv = stmt.getInvokeExpr();
                            if (invokesSetContentView(inv) || invokesInflate(inv)) { // check
                                for (Value val : inv.getArgs()) {
                                    Integer intValue = valueProvider.getValue(sm, stmt, val, Integer.class);
                                    if (intValue != null)
                                        this.layoutClasses.put(sm.getDeclaringClass(), intValue);
                                }
                            }
                        }
                    }
            } catch (RuntimeException e){
                Logger.warn(e.getMessage());
            }
        }
    }

    @Override
    public void forceTerminate(ISolverTerminationReason reason) {
        this.isKilled = reason;
    }

    @Override
    public boolean isTerminated() {
        return isKilled != null;
    }

    @Override
    public boolean isKilled() {
        return isKilled != null;
    }

    @Override
    public void reset() {
        this.isKilled = null;
    }

    @Override
    public void addStatusListener(IMemoryBoundedSolverStatusNotification listener) {
        this.notificationListeners.add(listener);
    }

    @Override
    public void excludeEntryPoint(SootClass entryPoint) {
        super.excludeEntryPoint(entryPoint);
        this.callbackWorklist.remove(entryPoint);
        this.callbackMethods.remove(entryPoint);
    }

    @Override
    public ISolverTerminationReason getTerminationReason() {
        return isKilled;
    }

}
