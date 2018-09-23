package dulinglai.android.alode.sootData.values;

import java.util.Map;

public class ResourceValueProvider {

    private final Map<Integer, String> stringResource;
    private final Map<Integer, String> resourceId;
    private final Map<String, Integer> layoutResource;

    public ResourceValueProvider(Map<Integer, String> stringResource, Map<Integer,String> resourceId,
                                 Map<String, Integer> layoutResource) {

        this.stringResource = stringResource;
        this.resourceId = resourceId;
        this.layoutResource = layoutResource;
    }

    public String getStringById(Integer id) {
        return stringResource.get(id);
    }

    public String getResourceIdString(Integer id) {
        return resourceId.get(id);
    }

    public Integer getResouceIdByString(String resourceString) {
        for (Integer key : resourceId.keySet()) {
            if (resourceId.get(key).equalsIgnoreCase(resourceString))
                return key;
        }
        return null;
    }

    public Integer getLayoutResourceId(String filename) {
        return layoutResource.get(filename);
    }

}
