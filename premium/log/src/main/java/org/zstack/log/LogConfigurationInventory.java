package org.zstack.log;

import org.zstack.header.search.Inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = LogConfigurationStruct.class)
public class LogConfigurationInventory {
    private String uuid;
    private String type;
    private List<String> managementNodeUuids;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getManagementNodeUuids() {
        return managementNodeUuids;
    }

    public void setManagementNodeUuids(List<String> managementNodeUuids) {
        this.managementNodeUuids = managementNodeUuids;
    }

    public LogConfigurationInventory() {
    }

    protected LogConfigurationInventory(LogConfigurationStruct vo) {
        this.setUuid(vo.getUuid());
        this.setType(vo.getType());
    }

    public static LogConfigurationInventory valueOf(LogConfigurationStruct vo) {
        return new LogConfigurationInventory(vo);
    }

    public static List<LogConfigurationInventory> valueOf(Collection<LogConfigurationStruct> vos) {
        List<LogConfigurationInventory> invs = new ArrayList<>(vos.size());
        for (LogConfigurationStruct vo : vos) {
            invs.add(new LogConfigurationInventory(vo));
        }
        return invs;
    }
}
