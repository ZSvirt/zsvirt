package org.zstack.storage.device.iscsi;

import org.zstack.header.storageDevice.ScsiLunVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.*;
import org.zstack.storage.device.multipath.MultipathDeviceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * Create by weiwang at 2018/8/1
 */
@Entity
@Table
@AutoDeleteTag
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = IscsiTargetVO.class, myField = "iscsiTargetUuid", targetField = "uuid"),
        }
)
public class IscsiLunVO extends ScsiLunVO {
    @Column
    @ForeignKey(parentEntityClass = IscsiTargetVO.class)
    private String iscsiTargetUuid;

    public String getIscsiTargetUuid() {
        return iscsiTargetUuid;
    }

    public void setIscsiTargetUuid(String iscsiTargetUuid) {
        this.iscsiTargetUuid = iscsiTargetUuid;
    }
}
