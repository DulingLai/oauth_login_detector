package dulinglai.android.alode.graphBuilder;

import dulinglai.android.alode.graphBuilder.componentNodes.AbstractComponentNode;

public class EdgeTag {

    private AbstractComponentNode prevComp;

    public EdgeTag(AbstractComponentNode prevComp) {
        this.prevComp = prevComp;
    }

    public AbstractComponentNode getPrevComp() {
        return prevComp;
    }
}
