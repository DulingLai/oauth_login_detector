package dulinglai.android.alode.graphBuilder;

import dulinglai.android.alode.graphBuilder.componentNodes.*;
import dulinglai.android.alode.graphBuilder.widgetNodes.AbstractWidgetNode;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ComponentTransitionGraph {
    private List<TransitionEdge> transitionEdges;
    private List<ActivityNode> activityNodeList;
    private List<ServiceNode> serviceNodeList;
    private List<ContentProviderNode> providerNodeList;
    private List<BroadcastReceiverNode> receiverNodeList;
    private MultiMap<AbstractComponentNode, TransitionEdge> outEdgeMap;
    private MultiMap<AbstractComponentNode, TransitionEdge> inEdgeMap;

    public ComponentTransitionGraph(List<ActivityNode> activityNodeList, List<ServiceNode> serviceNodeList,
                                    List<ContentProviderNode> providerNodeList, List<BroadcastReceiverNode> receiverNodeList) {

        this.activityNodeList = activityNodeList;
        this.serviceNodeList = serviceNodeList;
        this.providerNodeList = providerNodeList;
        this.receiverNodeList = receiverNodeList;
        this.transitionEdges = new ArrayList<>();
        this.outEdgeMap = new HashMultiMap<>();
        this.inEdgeMap = new HashMultiMap<>();

        for (ActivityNode activity : activityNodeList) {
            if (activity.getParent()!=null) {
                ActivityNode parentNode = getActivityNodeByName(activity.getParent());
                if (parentNode!=null) {
                    addTransitionEdge(parentNode, activity);
                }
            }
        }
    }

    /**
     * Add a new transition edge
     * @param srcComp The source activity
     * @param tgtComp The target activity
     */
    public void addTransitionEdge(AbstractComponentNode srcComp, AbstractComponentNode tgtComp) {
        TransitionEdge newEdge = new TransitionEdge(srcComp, tgtComp);
        transitionEdges.add(newEdge);
        outEdgeMap.put(srcComp, newEdge);
        inEdgeMap.put(tgtComp, newEdge);
    }

    /**
     * Add a new transition edge
     * @param srcActivity The source activity
     * @param tgtActivity The target activity
     * @param widget The widget assigned to this edge
     */
    public void addTransitionEdge(AbstractComponentNode srcActivity, ActivityNode tgtActivity, AbstractWidgetNode widget) {
        TransitionEdge newEdge = new TransitionEdge(srcActivity, tgtActivity, widget);
        transitionEdges.add(newEdge);
        outEdgeMap.put(srcActivity, newEdge);
        inEdgeMap.put(tgtActivity, newEdge);
    }

    /**
     * Gets the component node by name
     * @param name The name to look up component node
     * @return The component node
     */
    public AbstractComponentNode getCompNodeByName(String name) {
        for (ActivityNode activityNode : activityNodeList) {
            if (activityNode.getName().equalsIgnoreCase(name))
                return activityNode;
        }
        for (ServiceNode serviceNode : serviceNodeList) {
            if (serviceNode.getName().equalsIgnoreCase(name))
                return serviceNode;
        }
        for (ContentProviderNode providerNode : providerNodeList) {
            if (providerNode.getName().equalsIgnoreCase(name))
                return providerNode;
        }
        for (BroadcastReceiverNode receiverNode : receiverNodeList) {
            if (receiverNode.getName().equalsIgnoreCase(name))
                return receiverNode;
        }
        return null;
    }

    /**
     * Gets an activity node by name
     * @param name The name to look up activity node
     * @return The activity node
     */
    public ActivityNode getActivityNodeByName(String name) {
        for (ActivityNode activityNode : activityNodeList) {
            if (activityNode.getName().equalsIgnoreCase(name))
                return activityNode;
        }
        return null;
    }

    /**
     * Gets the service node by name
     * @param name The name to look up service node
     * @return The service node
     */
    public ServiceNode getServiceNodeByName(String name) {
        for (ServiceNode serviceNode : serviceNodeList) {
            if (serviceNode.getName().equalsIgnoreCase(name))
                return serviceNode;
        }
        return null;
    }

    /**
     * Gets the content provider node by name
     * @param name The name to look up provider node
     * @return The provider node
     */
    public ContentProviderNode getProviderNodeByName(String name) {
        for (ContentProviderNode providerNode : providerNodeList) {
            if (providerNode.getName().equalsIgnoreCase(name))
                return providerNode;
        }
        return null;
    }

    /**
     * Gets the broadcast receiver node by name
     * @param name The name to look up broadcast receiver node
     * @return The receiver node
     */
    public BroadcastReceiverNode getReceiverNodeByName(String name) {
        for (BroadcastReceiverNode receiverNode : receiverNodeList) {
            if (receiverNode.getName().equalsIgnoreCase(name))
                return receiverNode;
        }
        return null;
    }

    /**
     * Gets all activity nodes in the graph
     * @return All activity nodes
     */
    public List<ActivityNode> getAllActivities(){
        return activityNodeList;
    }

    /**
     * Gets the transition edges with given source activity node
     * @param componentNode The source activity node
     * @return The set of transition edges that contain given source activity node
     */
    public Set<TransitionEdge> getEdgeWithSrcComponent(AbstractComponentNode componentNode) {
        Set<TransitionEdge> edgeSet = new HashSet<>();
        for (TransitionEdge edge : transitionEdges) {
            if (edge.getSrcComp().equals(componentNode))
                edgeSet.add(edge);
        }
        return edgeSet;
    }

    /**
     * Gets the transition edges with given target component node
     * @param componentNode The target component node
     * @return The set of transition edges that contain given target component node
     */
    public Set<TransitionEdge> getEdgeWithTgtComp(AbstractComponentNode componentNode) {
        Set<TransitionEdge> edgeSet = new HashSet<>();
        for (TransitionEdge edge : transitionEdges) {
            if (edge.getTgtComp().equals(componentNode))
                edgeSet.add(edge);
        }
        return edgeSet;
    }

    /**
     * Gets the transition edge with given source and target component nodes
     * @param srcNode The source component node
     * @param tgtNode The target component node
     * @return The transition edge
     */
    public TransitionEdge getEdge(AbstractComponentNode srcNode, AbstractComponentNode tgtNode) {
        for (TransitionEdge edge : transitionEdges) {
            if (edge.getTgtComp().equals(tgtNode))
                if (edge.getSrcComp().equals(srcNode))
                    return edge;
        }
        return null;
    }

    /**
     * Sets the widget assigned to the transition edge with given source and target component nodes
     * @param srcNode The source component node
     * @param tgtNode The target component node
     * @param widget The widget to be assigned to this transition
     */
    public void setEdgeWidget(AbstractComponentNode srcNode, AbstractComponentNode tgtNode, AbstractWidgetNode widget) {
        for (TransitionEdge edge : transitionEdges) {
            if (edge.getTgtComp().equals(tgtNode))
                if (edge.getSrcComp().equals(srcNode))
                    edge.setWidget(widget);
        }
    }

    /**
     * Sets the tag assigned to the transition edge with given source and target component nodes
     * @param srcNode The source component node
     * @param tgtNode The target component node
     * @param tag The tag to be assigned to this transition
     */
    public void setEdgeWidget(AbstractComponentNode srcNode, AbstractComponentNode tgtNode, EdgeTag tag) {
        for (TransitionEdge edge : transitionEdges) {
            if (edge.getTgtComp().equals(tgtNode))
                if (edge.getSrcComp().equals(srcNode))
                    edge.setEdgeTag(tag);
        }
    }

    /**
     * Gets the tag assigned to the transition edge with given source and target component nodes
     * @param srcNode The source component node
     * @param tgtNode The target component node
     * @return  The tag to be assigned to this transition
     */
    public EdgeTag setEdgeWidget(AbstractComponentNode srcNode, AbstractComponentNode tgtNode) {
        for (TransitionEdge edge : transitionEdges) {
            if (edge.getTgtComp().equals(tgtNode))
                if (edge.getSrcComp().equals(srcNode))
                    return edge.getEdgeTag();
        }
        return null;
    }

    /**
     * Gets the successor of the given component
     * @param componentNode The given component
     * @return The successor of the given component
     */
    public List<AbstractComponentNode> getSuccsOf(AbstractComponentNode componentNode) {
        List<AbstractComponentNode> compNodeList = new ArrayList<>();
        for (TransitionEdge edge : getEdgeWithSrcComponent(componentNode)) {
            compNodeList.add(edge.getTgtComp());
        }
        return compNodeList;
    }

    /**
     * Gets the predecessor of the given component
     * @param componentNode The given component
     * @return The predecessor of the given component
     */
    public List<AbstractComponentNode> getPredsOf(AbstractComponentNode componentNode) {
        List<AbstractComponentNode> compNodeList = new ArrayList<>();
        for (TransitionEdge edge : getEdgeWithTgtComp(componentNode)) {
            compNodeList.add(edge.getSrcComp());
        }
        return compNodeList;
    }

    /**
     * Get transition edge widget
     * @param edge The transition edge
     * @return The widget assigned to this transition edge
     */
    public AbstractWidgetNode getWidgetOf(TransitionEdge edge) {
        return edge.getWidget();
    }

    /**
     * Get transition edge widget with source component
     * @param srcActivity The source component
     * @param tgtActivity The target component
     * @return The widget assigned to this transition edge
     */
    public AbstractWidgetNode getWidgetOfActivities(ActivityNode srcActivity, ActivityNode tgtActivity) {
        return getEdge(srcActivity, tgtActivity).getWidget();
    }

    /**
     * Check if an activity is in the graph
     * @param activityNode The activity node to check
     * @return True if the given activity node can be found in the graph
     */
    public boolean isActivityInGraph(ActivityNode activityNode) {
        return activityNodeList.contains(activityNode);
    }
}
