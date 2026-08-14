package org.zstack.header.cloudformation;

import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by mingjian.deng on 2018/6/14.
 */
@Inventory(mappingVOClass = CloudFormationStackEventVO.class)
public class CloudFormationStackEventInventory implements Serializable {
    private long id;
    private String description;
    private String action;
    private String content;
    private String resourceName;
    private String actionStatus;
    private String stackUuid;
    private String duration;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static CloudFormationStackEventInventory valueOf(CloudFormationStackEventVO vo) {
        CloudFormationStackEventInventory inv = new CloudFormationStackEventInventory();
        inv.setId(vo.getId());
        inv.setAction(vo.getAction());
        inv.setResourceName(vo.getResourceName());
        inv.setDescription(vo.getDescription());
        inv.setContent(vo.getContent());
        inv.setActionStatus(vo.getActionStatus().toString());
        inv.setStackUuid(vo.getStackUuid());
        inv.setDuration(vo.getDuration());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<CloudFormationStackEventInventory> valueOf(Collection<CloudFormationStackEventVO> vos) {
        List<CloudFormationStackEventInventory> invs = new ArrayList<>();
        for (CloudFormationStackEventVO vo : vos) {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getActionStatus() {
        return actionStatus;
    }

    public void setActionStatus(String actionStatus) {
        this.actionStatus = actionStatus;
    }

    public String getStackUuid() {
        return stackUuid;
    }

    public void setStackUuid(String stackUuid) {
        this.stackUuid = stackUuid;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
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
