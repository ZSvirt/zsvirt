package org.zstack.storage.primary.block.vendor.xstor;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.List;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/5/11 10:44
 */
public class Snapshot {
    public String name;
    public String access_mode;
    public List<String> deletion_strategy;
    public String create_mode;
    public String description;
    public Integer id;
    public String operation_state;
    public Integer snap_root_id;
    public String snap_root_name;
    public String snap_root_type;
    public Integer snap_source_id;
    public String snap_source_name;
    public String snap_source_type;
    public String snap_state;
    public long share_bytes;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSnapRootId() {
        return snap_root_id;
    }

    public void setSnapRootId(Integer snap_root_id) {
        this.snap_root_id = snap_root_id;
    }

    public Integer getSnapSourceId() {
        return snap_source_id;
    }

    public void setSnapSourceId(Integer snap_source_id) {
        this.snap_source_id = snap_source_id;
    }

    public List<String> getDeletionStrategy() {
        return deletion_strategy;
    }

    public void setDeletionStrategy(List<String> deletion_strategy) {
        this.deletion_strategy = deletion_strategy;
    }

    public String getAccessMode() {
        return access_mode;
    }

    public void setAccessMode(String access_mode) {
        this.access_mode = access_mode;
    }

    public String getCreateMode() {
        return create_mode;
    }

    public void setCreateMode(String create_mode) {
        this.create_mode = create_mode;
    }

    public String getOperationState() {
        return operation_state;
    }

    public void setOperationState(String operation_state) {
        this.operation_state = operation_state;
    }

    public String getSnapRootName() {
        return snap_root_name;
    }

    public void setSnapRootName(String snap_root_name) {
        this.snap_root_name = snap_root_name;
    }

    public String getSnapRootType() {
        return snap_root_type;
    }

    public void setSnapRootType(String snap_root_type) {
        this.snap_root_type = snap_root_type;
    }

    public void setSnapSourceType(String snap_source_type) {
        this.snap_source_type = snap_source_type;
    }

    public String getSnapSourceName() {
        return snap_source_name;
    }

    public void setSnapSourceName(String snap_source_name) {
        this.snap_source_name = snap_source_name;
    }

    public String getSnapState() {
        return snap_state;
    }

    public void setSnapState(String snap_state) {
        this.snap_state = snap_state;
    }

    public long getShareBytes() {
        return share_bytes;
    }

    public void setShareBytes(long share_bytes) {
        this.share_bytes = share_bytes;
    }
}
