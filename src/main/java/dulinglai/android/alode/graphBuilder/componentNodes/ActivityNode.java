package dulinglai.android.alode.graphBuilder.componentNodes;

import dulinglai.android.alode.resources.axml.AXmlNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dulinglai.android.alode.graphBuilder.NodeUtils.processIntentFilter;
import static dulinglai.android.alode.graphBuilder.NodeUtils.processNodeParent;

public class ActivityNode extends AbstractComponentNode {
    // components classes and constructors
    private Set<Integer> resourceId = new HashSet<>();
    private List<String> intentFilters_action;
    private List<String> intentFilters_category;
    private String parent;
    private boolean containLogin = false;

    public ActivityNode (AXmlNode node, String packageName) {
        super(node, packageName);
        this.intentFilters_action = processIntentFilter(node, "action");
        this.intentFilters_category = processIntentFilter(node, "category");
        this.parent = processNodeParent(node, packageName);
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Integer> getResourceId(){ return resourceId; }

    public void setResourceId(Set<Integer> resourceId){ this.resourceId = resourceId; }

    public List<String> getIntentFiltersAction() {
        return intentFilters_action;
    }

    public void setIntentFiltersAction(List<String> intentFilters_action) {
        this.intentFilters_action = intentFilters_action;
    }

    public List<String> getIntentFiltersCategory() {
        return intentFilters_category;
    }

    public void setIntentFiltersCategory(List<String> intentFilters_category) {
        this.intentFilters_category = intentFilters_category;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public boolean isContainLogin() {
        return containLogin;
    }

    public void setContainLogin(boolean containLogin) {
        this.containLogin = containLogin;
    }

    @Override
    public String toString(){
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((intentFilters_action == null) ? 0 : intentFilters_action.hashCode());
        result = prime * result + ((intentFilters_category == null) ? 0 : intentFilters_category.hashCode());
        result = prime * result + resourceId.hashCode();
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

        ActivityNode other = (ActivityNode) obj;

        if (intentFilters_action == null) {
            if (other.intentFilters_action != null)
                return false;
        } else if (!intentFilters_action.equals(other.intentFilters_action))
            return false;
        if (intentFilters_category == null) {
            if (other.intentFilters_category != null)
                return false;
        } else if (!intentFilters_category.equals(other.intentFilters_category))
            return false;
        if (resourceId != other.resourceId)
            return false;
        return name.equals(other.name);
    }
}
