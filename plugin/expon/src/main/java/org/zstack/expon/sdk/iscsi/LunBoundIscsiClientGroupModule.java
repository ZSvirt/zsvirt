package org.zstack.expon.sdk.iscsi;

/**
 * @example {
 * "chap_username": "",
 * "create_time": 1705314720041,
 * "description": "",
 * "id": "413f59f4-90ae-4ee7-9388-07065d25bd3e",
 * "is_chap": false,
 * "is_readonly": false,
 * "name": "iscsi_zstack_heartbeat",
 * "node_num": 3,
 * "update_time": 1708411159610,
 * "volume_id": "40b388e3-c7ee-4c44-bed9-6d4b88088f8f"
 * }
 */
public class LunBoundIscsiClientGroupModule {
    private String id;
    private String name;
    private String description;
    private String volumeId;
    private boolean isChap;
    private boolean isReadonly;
    private String chapUsername;
    private int nodeNum;
    private long createTime;
    private long updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    public boolean isChap() {
        return isChap;
    }

    public void setChap(boolean chap) {
        isChap = chap;
    }

    public boolean isReadonly() {
        return isReadonly;
    }

    public void setReadonly(boolean readonly) {
        isReadonly = readonly;
    }

    public String getChapUsername() {
        return chapUsername;
    }

    public void setChapUsername(String chapUsername) {
        this.chapUsername = chapUsername;
    }

    public int getNodeNum() {
        return nodeNum;
    }

    public void setNodeNum(int nodeNum) {
        this.nodeNum = nodeNum;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
}
