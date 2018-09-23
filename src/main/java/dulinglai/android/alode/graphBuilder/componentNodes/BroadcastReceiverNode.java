package dulinglai.android.alode.graphBuilder.componentNodes;

import dulinglai.android.alode.resources.axml.AXmlNode;

public class BroadcastReceiverNode extends AbstractComponentNode{
    public BroadcastReceiverNode(AXmlNode node, String packageName) {
        super(node, packageName);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;

        BroadcastReceiverNode other = (BroadcastReceiverNode) obj;

        if (name == null) {
            return other.name == null;
        } else if (other.name == null) {
            return false;
        } else return name.equals(other.name);
    }
}
