package org.zstack.core.gc;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by xing5 on 2017/3/5.
 */
@Inventory(mappingVOClass = GarbageCollectorVO.class)
public class GarbageCollectorInventory {
    private String uuid;
    private String name;
    private String runnerClass;
    private String context;
    private String status;
    private String managementNodeUuid;
    private String type;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static GarbageCollectorInventory valueOf(GarbageCollectorVO vo) {
        GarbageCollectorInventory inv = new GarbageCollectorInventory();
        inv.uuid = vo.getUuid();
        inv.name = vo.getName();
        inv.runnerClass = vo.getRunnerClass();
        inv.context = vo.getContext();
        inv.status = vo.getStatus().toString();
        inv.managementNodeUuid = vo.getManagementNodeUuid();
        inv.type = vo.getType();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();

        return inv;
    }

    public static List<GarbageCollectorInventory> valueOf(Collection<GarbageCollectorVO> vos) {
        List<GarbageCollectorInventory> invs = new ArrayList<>();
        for (GarbageCollectorVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRunnerClass() {
        return runnerClass;
    }

    public void setRunnerClass(String runnerClass) {
        this.runnerClass = runnerClass;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
