package org.zstack.storage.device.fibreChannel;

import org.zstack.core.Platform;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.storageDevice.ScsiLunVO;
import org.zstack.storage.device.StorageDeviceState;

import javax.persistence.*;
import java.sql.Timestamp;

import static org.zstack.header.storageDevice.StorageDeviceConstants.FIBER_CHANNEL;

/**
 * Create by weiwang at 2018/10/18
 */
@Entity
@Table
@AutoDeleteTag
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = FiberChannelStorageVO.class, myField = "fiberChannelStorageUuid", targetField = "uuid"),
        }
)
public class FiberChannelLunVO extends ScsiLunVO {
    @Column
    @ForeignKey(parentEntityClass = FiberChannelStorageVO.class)
    private String fiberChannelStorageUuid;

    public String getFiberChannelStorageUuid() {
        return fiberChannelStorageUuid;
    }

    public void setFiberChannelStorageUuid(String fiberChannelStorageUuid) {
        this.fiberChannelStorageUuid = fiberChannelStorageUuid;
    }

    public FiberChannelLunVO() {
    }

    public FiberChannelLunVO(FiberChannelLunStruct s, String storageUuid) {
        this.setName(String.format("fc-lun-%s", s.getWwid()));
        this.setUuid(Platform.getUuid());
        this.setWwid(s.getWwid());
        this.setVendor(s.getVendor());
        this.setModel(s.getModel());
        this.setWwn(s.getWwn());
        this.setSerial(s.getSerial());
        this.setType(s.getType());
        this.setPath(s.getPath());
        this.setSize(s.getSize());
//        this.setMultipathDeviceUuid(s.getMultipathDeviceUuid());
        this.setCreateDate(new Timestamp(System.currentTimeMillis()));
        this.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        this.setState(StorageDeviceState.Enabled.toString());
        this.setFiberChannelStorageUuid(storageUuid);
        this.setSource(FIBER_CHANNEL);
    }
}
