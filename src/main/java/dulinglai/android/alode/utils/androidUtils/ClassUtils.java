package dulinglai.android.alode.utils.androidUtils;

import dulinglai.android.alode.iccparser.IccLink;
import dulinglai.android.alode.resources.androidConstants.ComponentConstants;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import java.util.*;

/**
 * Class containing common utility methods for dealing with Android entry points
 *
 * @author Steven Arzt
 */
public class ClassUtils {

    private Map<SootClass, ComponentType> componentTypeCache = new HashMap<SootClass, ComponentType>();

    private SootClass osClassApplication;
    private SootClass osClassActivity;
    private SootClass osClassService;
    private SootClass osClassFragment;
    private SootClass osClassSupportFragment;
    private SootClass osClassBroadcastReceiver;
    private SootClass osClassContentProvider;
    private SootClass osClassGCMBaseIntentService;
    private SootClass osClassGCMListenerService;
    private SootClass osInterfaceServiceConnection;
    private SootClass osClickListener;

    private Set<Object> grey;
    private List<SootClass> predActivityClasses;

    /**
     * Array containing all types of components supported in Android lifecycles
     */
    public enum ComponentType {
        Application, Activity, Service, Fragment, BroadcastReceiver, ContentProvider,
        GCMBaseIntentService, GCMListenerService, ServiceConnection, Plain, ClickListener
    }

    /**
     * Creates a new instance of the {@link ClassUtils} class. Soot must
     * already be running when this constructor is invoked.
     */
    public ClassUtils() {
        // Get some commonly used OS classes
        osClassApplication = Scene.v().getSootClassUnsafe(ComponentConstants.APPLICATIONCLASS);
        osClassActivity = Scene.v().getSootClassUnsafe(ComponentConstants.ACTIVITYCLASS);
        osClassService = Scene.v().getSootClassUnsafe(ComponentConstants.SERVICECLASS);
        osClassFragment = Scene.v().getSootClassUnsafe(ComponentConstants.FRAGMENTCLASS);
        osClassSupportFragment = Scene.v().getSootClassUnsafe(ComponentConstants.SUPPORTFRAGMENTCLASS);
        osClassBroadcastReceiver = Scene.v().getSootClassUnsafe(ComponentConstants.BROADCASTRECEIVERCLASS);
        osClassContentProvider = Scene.v().getSootClassUnsafe(ComponentConstants.CONTENTPROVIDERCLASS);
        osClassGCMBaseIntentService = Scene.v()
                .getSootClassUnsafe(ComponentConstants.GCMBASEINTENTSERVICECLASS);
        osClassGCMListenerService = Scene.v().getSootClassUnsafe(ComponentConstants.GCMLISTENERSERVICECLASS);
        osInterfaceServiceConnection = Scene.v()
                .getSootClassUnsafe(ComponentConstants.SERVICECONNECTIONINTERFACE);
        osClickListener = Scene.v().getSootClassUnsafe(ComponentConstants.CLICKLISTENER);
    }

    /**
     * Gets the type of component represented by the given Soot class
     *
     * @param currentClass The class for which to get the component type
     * @return The component type of the given class
     */
    public ComponentType getComponentType(SootClass currentClass) {
        if (componentTypeCache.containsKey(currentClass))
            return componentTypeCache.get(currentClass);

        // Check the type of this class
        ComponentType ctype = ComponentType.Plain;

        // (1) android.app.Application
        if (osClassApplication != null && Scene.v().getOrMakeFastHierarchy().canStoreType(currentClass.getType(),
                osClassApplication.getType()))
            ctype = ComponentType.Application;
            // (2) android.app.Activity
        else if (osClassActivity != null
                && Scene.v().getOrMakeFastHierarchy().canStoreType(currentClass.getType(), osClassActivity.getType()))
            ctype = ComponentType.Activity;
            // (3) android.app.Service
        else if (osClassService != null
                && Scene.v().getOrMakeFastHierarchy().canStoreType(currentClass.getType(), osClassService.getType()))
            ctype = ComponentType.Service;
            // (4) android.app.BroadcastReceiver
        else if (osClassFragment != null
                && Scene.v().getOrMakeFastHierarchy().canStoreType(currentClass.getType(), osClassFragment.getType()))
            ctype = ComponentType.Fragment;
        else if (osClassSupportFragment != null && Scene.v().getOrMakeFastHierarchy()
                .canStoreType(currentClass.getType(), osClassSupportFragment.getType()))
            ctype = ComponentType.Fragment;
            // (5) android.app.BroadcastReceiver
        else if (osClassBroadcastReceiver != null && Scene.v().getOrMakeFastHierarchy()
                .canStoreType(currentClass.getType(), osClassBroadcastReceiver.getType()))
            ctype = ComponentType.BroadcastReceiver;
            // (6) android.app.ContentProvider
        else if (osClassContentProvider != null && Scene.v().getOrMakeFastHierarchy()
                .canStoreType(currentClass.getType(), osClassContentProvider.getType()))
            ctype = ComponentType.ContentProvider;
            // (7) com.google.android.gcm.GCMBaseIntentService
        else if (osClassGCMBaseIntentService != null && Scene.v().getOrMakeFastHierarchy()
                .canStoreType(currentClass.getType(), osClassGCMBaseIntentService.getType()))
            ctype = ComponentType.GCMBaseIntentService;
            // (8) com.google.android.gms.gcm.GcmListenerService
        else if (osClassGCMListenerService != null && Scene.v().getOrMakeFastHierarchy()
                .canStoreType(currentClass.getType(), osClassGCMListenerService.getType()))
            ctype = ComponentType.GCMListenerService;
            // (9) android.content.ServiceConnection
        else if (osInterfaceServiceConnection != null && Scene.v().getOrMakeFastHierarchy()
                .canStoreType(currentClass.getType(), osInterfaceServiceConnection.getType()))
            ctype = ComponentType.ServiceConnection;
		else if (osClickListener != null
                && Scene.v().getOrMakeFastHierarchy().canStoreType(currentClass.getType(), osClickListener.getType()))
            ctype = ComponentType.ClickListener;
        componentTypeCache.put(currentClass, ctype);
        return ctype;
    }

    /**
     * Checks whether the given class is derived from android.app.Application
     *
     * @param clazz The class to check
     * @return True if the given class is derived from android.app.Application,
     * otherwise false
     */
    public boolean isApplicationClass(SootClass clazz) {
        return osClassApplication != null
                && Scene.v().getOrMakeFastHierarchy().canStoreType(clazz.getType(), osClassApplication.getType());
    }

    /**
     * Checks whether the given method is an Android entry point, i.e., a lifecycle
     * method
     *
     * @param method The method to check
     * @return True if the given method is a lifecycle method, otherwise false
     */
    public boolean isEntryPointMethod(SootMethod method) {
        if (method == null)
            throw new IllegalArgumentException("Given method is null");
        ComponentType componentType = getComponentType(method.getDeclaringClass());
        String subsignature = method.getSubSignature();

        if (componentType == ComponentType.Activity
                && ComponentConstants.getActivityLifecycleMethods().contains(subsignature))
            return true;
        if (componentType == ComponentType.Service
                && ComponentConstants.getServiceLifecycleMethods().contains(subsignature))
            return true;
        if (componentType == ComponentType.Fragment
                && ComponentConstants.getFragmentLifecycleMethods().contains(subsignature))
            return true;
        if (componentType == ComponentType.BroadcastReceiver
                && ComponentConstants.getBroadcastLifecycleMethods().contains(subsignature))
            return true;
        if (componentType == ComponentType.ContentProvider
                && ComponentConstants.getContentproviderLifecycleMethods().contains(subsignature))
            return true;
        if (componentType == ComponentType.GCMBaseIntentService
                && ComponentConstants.getGCMIntentServiceMethods().contains(subsignature))
            return true;
        if (componentType == ComponentType.GCMListenerService
                && ComponentConstants.getGCMListenerServiceMethods().contains(subsignature))
            return true;
        return componentType == ComponentType.ServiceConnection
                && ComponentConstants.getServiceConnectionMethods().contains(subsignature);

    }

    /**
     * Traverse the ICC links to find the predecessor class
     * @param iccLinks All ICC links to loop through
     * @param destClass The target destination class
     * @return The predecessor classes of the target destination class
     */
    public List<SootClass> getPredClassesOf(List<IccLink> iccLinks, SootClass destClass) {
        List<SootClass> predClasses = new ArrayList<>();
        for (IccLink iccLink : iccLinks) {
            if (iccLink.getDestinationC().getName().equals(destClass.getName())) {
                predClasses.add(iccLink.getFromC());
            }
        }
        return predClasses;
    }

    /**
     * Traverse the ICC links to find the predecessor activity class
     * @param iccLinks All ICC links to loop through
     * @param destClass The target destination class
     * @return The predecessor activity class of the target destination class
     */
    public List<SootClass> getPredActivityClassOf(List<IccLink> iccLinks, SootClass destClass) {
        depthFirstSearch(destClass, iccLinks);
        return predActivityClasses;
    }

    /**
     * Depth first search on the iterator
     * @param destClass The destination class to start with
     * @param iccLinks The ICC links to loop through
     */
    private void depthFirstSearch(SootClass destClass, List<IccLink> iccLinks) {
        // reset grey set and result set
        grey = new HashSet<>();
        predActivityClasses = new ArrayList<>();

        for (SootClass s : getPredClassesOf(iccLinks, destClass)) {
            if (!grey.contains(s))
                visitNode(s, iccLinks);
        }
    }

    private void visitNode(SootClass s, List<IccLink> iccLinks) {
        grey.add(s);
        List<SootClass> predClasses = getPredClassesOf(iccLinks, s);
        Iterator it = predClasses.iterator();

        if (predClasses.size() > 0) {
            while (it.hasNext()) {
                Object pred = it.next();
                ComponentType predType = getComponentType((SootClass) pred);
                if (predType.equals(ComponentType.Activity))
                    predActivityClasses.add((SootClass)pred);
                else if (!grey.contains(pred))
                    visitNode((SootClass)pred, iccLinks);
            }
        }
    }

}
