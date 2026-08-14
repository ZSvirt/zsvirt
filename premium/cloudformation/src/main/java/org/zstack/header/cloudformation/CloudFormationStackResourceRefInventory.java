package org.zstack.header.cloudformation;

import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by mingjian.deng on 2018/6/12.
 */
@Inventory(mappingVOClass = CloudFormationStackResourceRefVO.class)
public class CloudFormationStackResourceRefInventory implements Serializable {
    private long id;
    private String stackUuid;
    private String resourceUuid;
    private String resourceName;
    private String resourceType;
    private boolean reserve;
    private int round;

    public static CloudFormationStackResourceRefInventory valueOf(CloudFormationStackResourceRefVO vo) {
        CloudFormationStackResourceRefInventory inv = new CloudFormationStackResourceRefInventory();
        inv.setId(vo.getId());
        inv.setStackUuid(vo.getStackUuid());
        inv.setReserve(vo.getReserve());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setResourceName(vo.getResourceName());
        inv.setResourceType(vo.getResourceType());
        inv.setRound(vo.getRound());
        return inv;
    }

    public static List<CloudFormationStackResourceRefInventory> valueOf(Collection<CloudFormationStackResourceRefVO> vos) {
        List<CloudFormationStackResourceRefInventory> invs = new ArrayList<>();
        for (CloudFormationStackResourceRefVO vo : vos) {
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

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public boolean isReserve() {
        return reserve;
    }

    public void setReserve(boolean reserve) {
        this.reserve = reserve;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
}
