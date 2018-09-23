package dulinglai.android.alode.graphBuilder.componentNodes;

import dulinglai.android.alode.resources.axml.AXmlNode;

import static dulinglai.android.alode.graphBuilder.NodeUtils.processAuthorities;

public class ContentProviderNode extends AbstractComponentNode{
    private String authorities;

    public ContentProviderNode(AXmlNode node, String packageName) {
        super(node, packageName);
        this.authorities = processAuthorities(node);
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
        int result = super.hashCode();
        result = prime * result + ((authorities == null) ? 0 : authorities.hashCode());
        return result;
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

        ContentProviderNode other = (ContentProviderNode) obj;

        if (name == null) {
            return other.name == null;
        }
        if (authorities == null) {
            return  other.authorities == null;
        } else if (other.authorities == null) {
            return false;
        } else {
            if (!authorities.equals(other.authorities))
                return false;
            return name.equals(other.name);
        }
    }
}
