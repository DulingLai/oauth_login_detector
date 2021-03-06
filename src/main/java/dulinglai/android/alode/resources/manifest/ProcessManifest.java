package dulinglai.android.alode.resources.manifest;

import dulinglai.android.alode.graphBuilder.componentNodes.ActivityNode;
import dulinglai.android.alode.graphBuilder.componentNodes.BroadcastReceiverNode;
import dulinglai.android.alode.graphBuilder.componentNodes.ContentProviderNode;
import dulinglai.android.alode.graphBuilder.componentNodes.ServiceNode;
import dulinglai.android.alode.resources.axml.AXmlAttribute;
import dulinglai.android.alode.resources.axml.AXmlHandler;
import dulinglai.android.alode.resources.axml.AXmlNode;
import dulinglai.android.alode.resources.axml.ApkHandler;
import dulinglai.android.alode.utils.androidUtils.SystemClassHandler;
import pxb.android.axml.AxmlVisitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;

import static dulinglai.android.alode.graphBuilder.NodeUtils.expandClassName;
import static dulinglai.android.alode.graphBuilder.NodeUtils.isValidComponentName;

/**
 * This class provides easy access to all data of an AppManifest.<br />
 * Nodes and attributes of a parsed manifest can be changed. A new byte
 * compressed manifest considering the changes can be generated.
 *
 * @author Steven Arzt
 * @author Stefan Haas, Mario Schlipf
 * @see <a href=
 * "http://developer.android.com/guide/topics/manifest/manifest-intro.html">App
 * Manifest</a>
 */
public class ProcessManifest {

    public String packageName;

    /**
     * Enumeration containing the various component types supported in Android
     */
    public enum ComponentType {
        Activity, Service, ContentProvider, BroadcastReceiver
    }

    /**
     * Handler for zip-like apk files
     */
    protected ApkHandler apk = null;

    /**
     * Handler for android xml files
     */
    protected AXmlHandler axml;

    // android manifest data
    protected AXmlNode manifest;
    protected AXmlNode application;

    // Components in the manifest file
    private List<AXmlNode> providerNodes = null;
    private List<AXmlNode> serviceNodes = null;
    private List<AXmlNode> activityNodes = null;
    private List<AXmlNode> receiverNodes = null;

    private Set<String> launchableActivities = new HashSet<>();
    private List<ActivityNode> activityNodeList = new ArrayList<>();
    private List<ServiceNode> serviceNodeList = new ArrayList<>();
    private List<ContentProviderNode> providerNodeList = new ArrayList<>();
    private List<BroadcastReceiverNode> receiverNodeList = new ArrayList<>();

    /**
     * Processes an AppManifest which is within the file identified by the given
     * path.
     *
     * @param apkPath file path to an APK.
     * @throws IOException            if an I/O error occurs.
     */
    public ProcessManifest(String apkPath) throws IOException {
        this(new File(apkPath));
    }

    /**
     * Processes an AppManifest which is within the given {@link File}.
     *
     * @param apkFile the AppManifest within the given APK will be parsed.
     * @throws IOException if an I/O error occurs.
     * @see {@link ProcessManifest#ProcessManifest(InputStream)}
     */
    public ProcessManifest(File apkFile) throws IOException {
        if (!apkFile.exists())
            throw new RuntimeException(
                    String.format("The given APK file %s does not exist", apkFile.getCanonicalPath()));

        this.apk = new ApkHandler(apkFile);
        try (InputStream is = this.apk.getInputStream("AndroidManifest.xml")) {
            if (is == null)
                throw new FileNotFoundException(
                        String.format("The file %s does not contain an Android Manifest", apkFile.getAbsolutePath()));
            this.handle(is);
        }
    }

    /**
     * Processes an AppManifest which is provided by the given
     * {@link InputStream}.
     *
     * @param manifestIS InputStream for an AppManifest.
     * @throws IOException if an I/O error occurs.
     */
    public ProcessManifest(InputStream manifestIS) throws IOException {
        this.handle(manifestIS);
    }

    /**
     * Initialises the {@link ProcessManifest} by parsing the manifest provided
     * by the given {@link InputStream}.
     *
     * @param manifestIS InputStream for an AppManifest.
     * @throws IOException if an I/O error occurs.
     */
    private void handle(InputStream manifestIS) throws IOException {
        this.axml = new AXmlHandler(manifestIS);

        // get manifest node
        List<AXmlNode> manifests = this.axml.getNodesWithTag("manifest");
        if (manifests.isEmpty())
            throw new RuntimeException("Manifest contains no manifest node");
        else if (manifests.size() > 1)
            throw new RuntimeException("Manifest contains more than one manifest node");
        this.manifest = manifests.get(0);

        // get application node
        List<AXmlNode> applications = this.manifest.getChildrenWithTag("application");
        if (applications.isEmpty())
            throw new RuntimeException("Manifest contains no application node");
        else if (applications.size() > 1)
            throw new RuntimeException("Manifest contains more than one application node");
        this.application = applications.get(0);

        this.packageName = getManifestPackageName();

        // Get components
        this.providerNodes = this.axml.getNodesWithTag("provider");
        this.serviceNodes = this.axml.getNodesWithTag("service");
        this.activityNodes = this.axml.getNodesWithTag("activity");
        this.receiverNodes = this.axml.getNodesWithTag("receiver");

        // Process activityNodes and serviceNodes
        for (AXmlNode activityNode : activityNodes) {
            activityNodeList.add(new ActivityNode(activityNode, packageName));
        }
        for (AXmlNode serviceNode : serviceNodes) {
            serviceNodeList.add(new ServiceNode(serviceNode, packageName));
        }
        for (AXmlNode providerNode : providerNodes) {
            providerNodeList.add(new ContentProviderNode(providerNode, packageName));
        }
        for (AXmlNode receiverNode : receiverNodes) {
            receiverNodeList.add(new BroadcastReceiverNode(receiverNode, packageName));
        }

        // launchable activities
        Set<AXmlNode> launchableNodes = getLaunchableActivityNodes();
        for (AXmlNode node : launchableNodes) {
            launchableActivities.add(findName(node));
        }
    }

    private String findName(AXmlNode node) {
        String className = null;
        AXmlAttribute<?> attr = node.getAttribute("name");
        if (attr != null) {
            className = expandClassName((String) attr.getValue(), packageName);
        }
        return className;
    }

    /**
     * Returns the handler which parsed and holds the manifest's data.
     *
     * @return Android XML handler
     */
    public AXmlHandler getAXml() {
        return this.axml;
    }

    /**
     * Returns the handler which opened the APK file. If {@link ProcessManifest}
     * was instanciated directly with an {@link InputStream} this will return
     * <code>null</code>.
     *
     * @return APK Handler
     */
    public ApkHandler getApk() {
        return this.apk;
    }

    /**
     * The unique <code>manifest</code> node of the AppManifest.
     *
     * @return manifest node
     */
    public AXmlNode getManifest() {
        return this.manifest;
    }

    /**
     * The unique <code>application</code> node of the AppManifest.
     *
     * @return application node
     */
    public AXmlNode getApplication() {
        return this.application;
    }

    /**
     * Returns a list containing all nodes with tag <code>provider</code>.
     *
     * @return list with all providerNodes
     */
    public ArrayList<AXmlNode> getProviderNodes() {
        return new ArrayList<AXmlNode>(this.providerNodes);
    }

    /**
     * Returns a list containing all nodes with tag <code>service</code>.
     *
     * @return list with all serviceNodes
     */
    public ArrayList<AXmlNode> getServiceNodes() {
        return new ArrayList<AXmlNode>(this.serviceNodes);
    }

    /**
     * Gets all activity classes in this applications
     *
     * @return All activity classes in this applications
     */
    public Set<String> getAllActivityClasses() {
        // If the application is not enabled, there are no entry points
        if (!isApplicationEnabled())
            return Collections.emptySet();

        // Collect the components
        Set<String> actitityClasses = new HashSet<>();
        for (AXmlNode node : this.activityNodes)
            checkAndAddComponent(actitityClasses, node);

        return actitityClasses;
    }

    /**
     * Add all components
     *
     * @param entryPoints The set of strings to add to
     * @param node        the source AXmlNode
     */
    private void checkAndAddComponent(Set<String> entryPoints, AXmlNode node) {
        AXmlAttribute<?> attrEnabled = node.getAttribute("enabled");
        if (attrEnabled == null || !attrEnabled.getValue().equals(Boolean.FALSE)) {
            AXmlAttribute<?> attr = node.getAttribute("name");
            if (attr != null) {
                String className = expandClassName((String) attr.getValue(), packageName);
                if (!SystemClassHandler.isClassInSystemPackage(className))
                    entryPoints.add(className);
            } else {
                // This component does not have a name, so this might be
                // obfuscated malware. We apply a heuristic.
                for (Entry<String, AXmlAttribute<?>> a : node.getAttributes().entrySet())
                    if (a.getValue().getName().isEmpty() && a.getValue().getType() == AxmlVisitor.TYPE_STRING) {
                        String name = (String) a.getValue().getValue();
                        if (isValidComponentName(name)) {
                            String expandedName = expandClassName(name, packageName);
                            if (!SystemClassHandler.isClassInSystemPackage(expandedName))
                                entryPoints.add(expandedName);
                        }
                    }
            }
        }
    }

    /**
     * Gets the type of the component identified by the given class name
     *
     * @param className The class name for which to get the component type
     * @return The component type of the given class if this class has been
     * registered as a component in the manifest file, otherwise null
     */
    public ComponentType getComponentType(String className) {
        for (AXmlNode node : this.activityNodes)
            if (node.getAttribute("name").getValue().equals(className))
                return ComponentType.Activity;
        for (AXmlNode node : this.serviceNodes)
            if (node.getAttribute("name").getValue().equals(className))
                return ComponentType.Service;
        for (AXmlNode node : this.receiverNodes)
            if (node.getAttribute("name").getValue().equals(className))
                return ComponentType.BroadcastReceiver;
        for (AXmlNode node : this.providerNodes)
            if (node.getAttribute("name").getValue().equals(className))
                return ComponentType.ContentProvider;
        return null;
    }

    /**
     * Returns a list containing all nodes with tag <code>activity</code>.
     *
     * @return list with all activityNodes
     */
    public ArrayList<AXmlNode> getActivityNodes() {
        return new ArrayList<>(this.activityNodes);
    }

    /**
     * Returns a list containing all nodes with tag <code>receiver</code>.
     *
     * @return list with all receiverNodes
     */
    public ArrayList<AXmlNode> getReceiverNodes() {
        return new ArrayList<>(this.receiverNodes);
    }

    /**
     * Returns the <code>provider</code> which has the given <code>name</code>.
     *
     * @param name the provider's name
     * @return provider with <code>name</code>
     */
    public AXmlNode getProvider(String name) {
        return this.getNodeWithName(this.providerNodes, name);
    }

    /**
     * Returns the <code>service</code> which has the given <code>name</code>.
     *
     * @param name the service's name
     * @return service with <code>name</code>
     */
    public AXmlNode getService(String name) {
        return this.getNodeWithName(this.serviceNodes, name);
    }

    /**
     * Returns the <code>activity</code> which has the given <code>name</code>.
     *
     * @param name the activitie's name
     * @return activitiy with <code>name</code>
     */
    public AXmlNode getActivity(String name) {
        return this.getNodeWithName(this.activityNodes, name);
    }

    /**
     * Returns the <code>receiver</code> which has the given <code>name</code>.
     *
     * @param name the receiver's name
     * @return receiver with <code>name</code>
     */
    public AXmlNode getReceiver(String name) {
        return this.getNodeWithName(this.receiverNodes, name);
    }

    /**
     * Iterates over <code>list</code> and checks which node has the given
     * <code>name</code>.
     *
     * @param list contains nodes.
     * @param name the node's name.
     * @return node with <code>name</code>.
     */
    protected AXmlNode getNodeWithName(List<AXmlNode> list, String name) {
        for (AXmlNode node : list) {
            Object attr = node.getAttributes().get("name");
            if (attr != null && attr.equals(name))
                return node;
        }

        return null;
    }

    /**
     * Returns the Manifest as a compressed android xml byte array. This will
     * consider all changes made to the manifest and application nodes
     * respectively to their child nodes.
     *
     * @return byte compressed AppManifest
     * @see AXmlHandler#toByteArray()
     */
    public byte[] getOutput() {
        return this.axml.toByteArray();
    }

    /**
     * Gets the application's package name
     *
     * @return The package name of the application
     */
    private String getManifestPackageName() {
        String packageName = null;
        AXmlAttribute<?> attr = this.manifest.getAttribute("package");
        if (attr != null)
            packageName = (String) attr.getValue();
        return packageName;
    }

    public String getPackageName() {
        return packageName;
    }

    /**
     * Gets the version code of the application. This code is used to compare
     * versions for updates.
     *
     * @return The version code of the application
     */
    public int getVersionCode() {
        AXmlAttribute<?> attr = this.manifest.getAttribute("versionCode");
        return attr == null ? -1 : Integer.parseInt("" + attr.getValue());
    }

    /**
     * Gets the application's version name as it is displayed to the user
     *
     * @return The application#s version name as in pretty print
     */
    public String getVersionName() {
        AXmlAttribute<?> attr = this.manifest.getAttribute("versionName");
        return attr == null ? null : (String) attr.getValue();
    }

    /**
     * Gets the name of the Android application class
     *
     * @return The name of the Android application class
     */
    public String getApplicationName() {
        AXmlAttribute<?> attr = this.application.getAttribute("name");
        return attr == null ? null : expandClassName((String) attr.getValue(), packageName);
    }

    /**
     * Gets whether this Android application is enabled
     *
     * @return True if this application is enabled, otherwise false
     */
    public boolean isApplicationEnabled() {
        AXmlAttribute<?> attr = this.application.getAttribute("enabled");
        return attr == null || !attr.getValue().equals(Boolean.FALSE);
    }

    /**
     * Gets the minimum SDK version on which this application is supposed to run
     *
     * @return The minimum SDK version on which this application is supposed to
     * run
     */
    public int getMinSdkVersion() {
        List<AXmlNode> usesSdk = this.manifest.getChildrenWithTag("uses-sdk");
        if (usesSdk == null || usesSdk.isEmpty())
            return -1;
        AXmlAttribute<?> attr = usesSdk.get(0).getAttribute("minSdkVersion");

        if (attr == null)
            return -1;

        if (attr.getValue() instanceof Integer)
            return (Integer) attr.getValue();

        return Integer.parseInt("" + attr.getValue());
    }

    /**
     * Gets the target SDK version for which this application was developed
     *
     * @return The target SDK version for which this application was developed
     */
    public int targetSdkVersion() {
        List<AXmlNode> usesSdk = this.manifest.getChildrenWithTag("uses-sdk");
        if (usesSdk == null || usesSdk.isEmpty())
            return -1;
        AXmlAttribute<?> attr = usesSdk.get(0).getAttribute("targetSdkVersion");

        if (attr == null)
            return -1;

        if (attr.getValue() instanceof Integer)
            return (Integer) attr.getValue();

        return Integer.parseInt("" + attr.getValue());
    }

    /**
     * Gets the permissions this application requests
     *
     * @return
     */
    public Set<String> getPermissions() {
        List<AXmlNode> usesPerms = this.manifest.getChildrenWithTag("uses-permission");
        Set<String> permissions = new HashSet<String>();
        for (AXmlNode perm : usesPerms) {
            AXmlAttribute<?> attr = perm.getAttribute("name");
            if (attr != null)
                permissions.add((String) attr.getValue());
            else {
                // The required "name" attribute is missing, so we collect all
                // empty
                // attributes as a best-effort solution for broken malware apps
                for (AXmlAttribute<?> a : perm.getAttributes().values())
                    if (a.getType() == AxmlVisitor.TYPE_STRING && a.getName().isEmpty())
                        permissions.add((String) a.getValue());
            }
        }
        return permissions;
    }

    /**
     * Gets the activity node set for AWTG
     *
     * @return The activity node set for AWTG
     */
    public List<ActivityNode> getActivityNodeList() {
        return activityNodeList;
    }

    /**
     * Gets the service node set for AWTG
     *
     * @return The service node set for AWTG
     */
    public List<ServiceNode> getServiceNodeList() {
        return serviceNodeList;
    }

    /**
     * Gets the provider node set for AWTG
     *
     * @return The provider node set for AWTG
     */
    public List<ContentProviderNode> getProviderNodeList() {
        return providerNodeList;
    }

    /**
     * Gets the receiver node set for AWTG
     *
     * @return The receiver node set for AWTG
     */
    public List<BroadcastReceiverNode> getReceiverNodeList() {
        return receiverNodeList;
    }

    /**
     * Gets the launchable activities from Manifest
     *
     * @return The launchable activities from Manifest
     */
    public Set<String> getLaunchableActivities() {
        return launchableActivities;
    }

    public void setLaunchableActivities(Set<String> launchableActivities) {
        this.launchableActivities = launchableActivities;
    }

    /**
     * Gets all classes the contain entry points in this applications
     *
     * @return All classes the contain entry points in this applications
     */
    public Set<String> getEntryPointClasses() {
        // If the application is not enabled, there are no entry points
        if (!isApplicationEnabled())
            return Collections.emptySet();

        // Collect the components
        Set<String> entryPoints = new HashSet<String>();
        for (AXmlNode node : this.activityNodes)
            checkAndAddComponent(entryPoints, node);
        for (AXmlNode node : this.providerNodes)
            checkAndAddComponent(entryPoints, node);
        for (AXmlNode node : this.serviceNodes)
            checkAndAddComponent(entryPoints, node);
        for (AXmlNode node : this.receiverNodes)
            checkAndAddComponent(entryPoints, node);

        String appName = getApplicationName();
        if (appName != null && !appName.isEmpty())
            entryPoints.add(appName);

        return entryPoints;
    }

    /**
     * Closes this apk file and all resources associated with it
     */
    public void close() {
        if (this.apk != null)
            this.apk.close();
    }

    /**
     * Returns all activity nodes that are "launchable", i.e. that are called
     * when the user clicks on the button in the launcher.
     *
     * @return
     */
    public Set<AXmlNode> getLaunchableActivityNodes() {
        Set<AXmlNode> allLaunchableActivities = new HashSet<AXmlNode>();

        for (AXmlNode activity : activityNodes) {
            for (AXmlNode activityChildren : activity.getChildren()) {
                if (activityChildren.getTag().equals("intent-filter")) {
                    boolean actionFilter = false;
                    boolean categoryFilter = false;
                    for (AXmlNode intentFilter : activityChildren.getChildren()) {
                        if (intentFilter.getTag().equals("action")
                                && intentFilter.getAttribute("name").getValue().toString()
                                .equals("android.intent.action.MAIN"))
                            actionFilter = true;
                        else if (intentFilter.getTag().equals("category")
                                && intentFilter.getAttribute("name").getValue().toString()
                                .equals("android.intent.category.LAUNCHER"))
                            categoryFilter = true;
                    }

                    if (actionFilter && categoryFilter)
                        allLaunchableActivities.add(activity);
                }
            }

        }

        return allLaunchableActivities;
    }
}