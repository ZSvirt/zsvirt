package org.zstack.header.cloudformation.monitor;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by mingjian.deng on 2019/11/22.
 */
@Inventory(mappingVOClass = ResourceStackVmPortRefVO.class)
public class ResourceStackVmPortRefInventory {
    private long id;
    private String stackUuid;
    private String vmInstanceUuid;
    private int port;
    private String status;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static ResourceStackVmPortRefInventory valueOf(ResourceStackVmPortRefVO vo) {
        ResourceStackVmPortRefInventory inv = new ResourceStackVmPortRefInventory();
        inv.setId(vo.getId());
        inv.setStackUuid(vo.getStackUuid());
        inv.setVmInstanceUuid(vo.getVmInstanceUuid());
        inv.setPort(vo.getPort());
        inv.setStatus(vo.getStatus());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setCreateDate(vo.getCreateDate());
        return inv;
    }

    public static List<ResourceStackVmPortRefInventory> valueOf(Collection<ResourceStackVmPortRefVO> vos) {
        List<ResourceStackVmPortRefInventory> invs = new ArrayList<>();
        for (ResourceStackVmPortRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getStackUuid() {
        return stackUuid;
    }

    public void setStackUuid(String stackUuid) {
        this.stackUuid = stackUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
