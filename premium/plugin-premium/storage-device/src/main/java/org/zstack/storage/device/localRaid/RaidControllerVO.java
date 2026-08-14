package org.zstack.storage.device.localRaid;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table
@AutoDeleteTag
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = RaidPhysicalDriveVO.class, myField = "uuid", targetField = "raidControllerUuid"),
        }
)
public class RaidControllerVO extends ResourceVO implements ToInventory {
    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String productName;

    @Column
    private String sasAddress;

    @Column
    private String hostUuid;

    @Column
    private Integer adapterNumber;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name="raidControllerUuid", insertable=false, updatable=false)
    @NoView
    private Set<RaidPhysicalDriveVO> raidPhysicalDrives = new HashSet<RaidPhysicalDriveVO>();

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
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

    public String getSasAddress() {
        return sasAddress;
    }

    public void setSasAddress(String sasAddress) {
        this.sasAddress = sasAddress;
    }

    public Set<RaidPhysicalDriveVO> getRaidPhysicalDrives() {
        return raidPhysicalDrives;
    }

    public void setRaidPhysicalDrives(Set<RaidPhysicalDriveVO> raidPhysicalDrives) {
        this.raidPhysicalDrives = raidPhysicalDrives;
    }

    public Integer getAdapterNumber() {
        return adapterNumber;
    }

    public void setAdapterNumber(Integer adapterNumber) {
        this.adapterNumber = adapterNumber;
    }
}
