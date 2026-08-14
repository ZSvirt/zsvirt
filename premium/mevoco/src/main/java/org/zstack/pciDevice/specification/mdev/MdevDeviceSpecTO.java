package org.zstack.pciDevice.specification.mdev;

import java.util.Map;

/**
 * Created by GuoYi on 2019-05-05.
 */
public class MdevDeviceSpecTO {
    private String name;
    private String description;
    private Map<String, String> specification;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getSpecification() {
        return specification;
    }

    public void setSpecification(Map<String, String> specification) {
        this.specification = specification;
    }
}
