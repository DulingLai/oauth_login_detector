package dulinglai.android.alode.graphBuilder.componentNodes;

import dulinglai.android.alode.resources.axml.AXmlNode;

import java.util.List;

import static dulinglai.android.alode.graphBuilder.NodeUtils.processIntentFilter;

public class ServiceNode extends AbstractComponentNode{
    private List<String> intentFilters_action;
    private List<String> intentFilters_category;

    public ServiceNode (AXmlNode node, String packageName) {
        super(node, packageName);
        this.intentFilters_action = processIntentFilter(node, "action");
        this.intentFilters_category = processIntentFilter(node, "category");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((intentFilters_action == null) ? 0 : intentFilters_action.hashCode());
        result = prime * result + ((intentFilters_category == null) ? 0 : intentFilters_category.hashCode());
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

        if (name == null) {
            return other.name == null;
        } else
        return name.equals(other.name);
    }
}
