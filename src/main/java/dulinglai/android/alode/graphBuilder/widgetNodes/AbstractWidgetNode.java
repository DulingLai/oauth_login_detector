package dulinglai.android.alode.graphBuilder.widgetNodes;

public class AbstractWidgetNode {
    // components classes and constructors
    private int resourceId;
    private String resourceIdString;
    private String text;

    AbstractWidgetNode(int resourceId, String text) {
        this.resourceId = resourceId;
        this.text = text;
    }

    AbstractWidgetNode(int resourceId, String resourceIdString, String text) {
        this.resourceId = resourceId;
        this.resourceIdString = resourceIdString;
        this.text = text;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getResourceIdString() {
        return resourceIdString;
    }

    public void setResourceIdString(String resourceIdString) {
        this.resourceIdString = resourceIdString;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        result = prime * result + resourceId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        AbstractWidgetNode other = (AbstractWidgetNode) obj;
        if (text == null) {
            if (other.text != null)
                return false;
        } else if (!text.equals(other.text))
            return false;
        return resourceId == other.resourceId;
    }
}
