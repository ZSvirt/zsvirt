package org.zstack.header.vm;

import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = VmPriorityConfigVO.class)
public class VmPriorityConfigInventory implements Serializable {
    private String uuid;
    private String accountUuid;
    private VmPriorityLevel level;
    private int cpuShares;
    private int oomScoreAdj;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static VmPriorityConfigInventory valueOf(VmPriorityConfigVO vo) {
        VmPriorityConfigInventory inv = new VmPriorityConfigInventory();
        inv.setUuid(vo.getUuid());
        inv.setLevel(vo.getLevel());
        inv.setCpuShares(vo.getCpuShares());
        inv.setOomScoreAdj(vo.getOomScoreAdj());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setAccountUuid(vo.getAccountUuid());
        return inv;
    }

    public static List<VmPriorityConfigInventory> valueOf(Collection<VmPriorityConfigVO> vos) {
        List<VmPriorityConfigInventory> invs = new ArrayList<VmPriorityConfigInventory>(vos.size());
        for (VmPriorityConfigVO vo : vos) {
            invs.add(VmPriorityConfigInventory.valueOf(vo));
        }
        return invs;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public VmPriorityLevel getLevel() {
        return level;
    }

    public void setLevel(VmPriorityLevel level) {
        this.level = level;
    }

    public int getCpuShares() {
        return cpuShares;
    }

    public void setCpuShares(int cpuShares) {
        this.cpuShares = cpuShares;
    }

    public int getOomScoreAdj() {
        return oomScoreAdj;
    }

    public void setOomScoreAdj(int oomScoreAdj) {
        this.oomScoreAdj = oomScoreAdj;
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
