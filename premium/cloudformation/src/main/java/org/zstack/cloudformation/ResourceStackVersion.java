package org.zstack.cloudformation;

/**
 * Created by mingjian.deng on 2018/6/5.
 */
public enum ResourceStackVersion {
    v1("2018-06-18");

    String type;

    ResourceStackVersion(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

    public static ResourceStackVersion get(String type) {
        for (ResourceStackVersion tmp: ResourceStackVersion.values()) {
            if (tmp.toString().equalsIgnoreCase(type)) {
                return tmp;
            }
        }
        return null;
    }
}
