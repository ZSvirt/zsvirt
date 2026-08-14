package org.zstack.billing;

import java.util.ArrayList;

public class PrimaryStorageAllocatePoolNamesConfig {
    private String type;

    private String uuid;

    private ArrayList<String> poolNames = new ArrayList< String>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String primaryStorageUuid) {
        this.uuid = primaryStorageUuid;
    }

    public ArrayList<String> getPoolNames() {
        return poolNames;
    }

    public void setPoolNames(ArrayList< String> poolNames) {
        this.poolNames = poolNames;
    }
}
