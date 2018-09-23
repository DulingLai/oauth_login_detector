package dulinglai.android.alode.graphBuilder;

import dulinglai.android.alode.graphBuilder.componentNodes.AbstractComponentNode;
import dulinglai.android.alode.graphBuilder.widgetNodes.AbstractWidgetNode;

public class TransitionEdge {

    private AbstractComponentNode srcComp;
    private AbstractComponentNode tgtComp;
    private AbstractWidgetNode widget;
    private EdgeTag edgeTag;

    public TransitionEdge(AbstractComponentNode srcComp, AbstractComponentNode tgtComp) {
        this.srcComp = srcComp;
        this.tgtComp = tgtComp;
    }

    public TransitionEdge(AbstractComponentNode srcComp, AbstractComponentNode tgtComp, AbstractWidgetNode widget) {
        this.srcComp = srcComp;
        this.tgtComp = tgtComp;
        this.widget = widget;
    }

    public String toString() {
        return "Transition Edge: " + srcComp.getName() + " ==> " + tgtComp.getName();
    }

    @Override
    public boolean equals(Object other) {
        TransitionEdge o = (TransitionEdge) other;
        if (o == null)
            return false;
        if (o.getSrcComp() != srcComp)
            return false;
        if (o.getTgtComp() != tgtComp)
            return false;
        return o.getWidget() == widget;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((srcComp == null) ? 0 : srcComp.hashCode());
        result = prime * result + ((tgtComp == null) ? 0 : tgtComp.hashCode());
        result = prime * result + ((widget == null) ? 0 : widget.hashCode());
        return result;
    }

    /**
     * Gets the source activity node of the edge
     * @return the source activity node of the edge
     */
    public AbstractComponentNode getSrcComp() {
        return srcComp;
    }

    /**
     * Gets the target activity node of the edge
     * @return the target activity node of the edge
     */
    public AbstractComponentNode getTgtComp() {
        return tgtComp;
    }

    /**
     * Gets the widget assigned to this transition
     * @return the widget assigned to this transition
     */
    public AbstractWidgetNode getWidget() {
        return widget;
    }

    /**
     * Sets the widget assigned to this transition
     * @param widget The widget assigned to this transition
     */
    public void setWidget(AbstractWidgetNode widget) {
        this.widget = widget;
    }

    public EdgeTag getEdgeTag() {
        return edgeTag;
    }

    public void setEdgeTag(EdgeTag edgeTag) {
        this.edgeTag = edgeTag;
    }
}
