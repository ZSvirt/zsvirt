package org.zstack.storage.device.nvme;

import org.zstack.core.Platform;
import org.zstack.header.storageDevice.LunVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.NoView;
import org.zstack.storage.device.StorageDeviceState;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import static org.zstack.header.storageDevice.StorageDeviceConstants.NVME;

/**
 * Create by weiwang at 2018/10/18
 */
@Entity
@Table
@AutoDeleteTag
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = NvmeTargetVO.class, myField = "nvmeTargetUuid", targetField = "uuid"),
        },
        friends = {
            @EntityGraph.Neighbour(type = NvmeLunHostRefVO.class, myField = "uuid", targetField = "nvmeLunUuid"),
        }
)
public class NvmeLunVO extends LunVO {
    @Column
    @ForeignKey(parentEntityClass = NvmeTargetVO.class)
    private String nvmeTargetUuid;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name="nvmeLunUuid", insertable=false, updatable=false)
    @NoView
    private Set<NvmeLunHostRefVO> nvmeLunHostRefs = new HashSet<>();


    public String getNvmeTargetUuid() {
        return nvmeTargetUuid;
    }

    public void setNvmeTargetUuid(String nvmeTargetUuid) {
        this.nvmeTargetUuid = nvmeTargetUuid;
    }

    public Set<NvmeLunHostRefVO> getNvmeLunHostRefs() {
        return nvmeLunHostRefs;
    }

    public void setNvmeLunHostRefs(Set<NvmeLunHostRefVO> nvmeLunHostRefs) {
        this.nvmeLunHostRefs = nvmeLunHostRefs;
    }

    public NvmeLunVO() {
    }

    public NvmeLunVO(NvmeLunStruct s, String storageUuid) {
        this.setName(String.format("nvme-lun-%s", s.getWwid()));
        this.setUuid(Platform.getUuid());
        this.setWwid(s.getWwid());
        this.setVendor(s.getVendor());
        this.setModel(s.getModel());
        this.setWwn(s.getWwn());
        this.setSerial(s.getSerial());
        this.setType(s.getType());
        this.setPath(s.getPath());
        this.setSize(s.getSize());
        this.setCreateDate(new Timestamp(System.currentTimeMillis()));
        this.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        this.setState(StorageDeviceState.Enabled.toString());
        this.setNvmeTargetUuid(storageUuid);
        this.setSource(NVME);
    }
}
