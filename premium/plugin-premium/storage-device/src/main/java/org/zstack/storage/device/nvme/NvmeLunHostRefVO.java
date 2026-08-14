package org.zstack.storage.device.nvme;

import org.zstack.header.host.HostVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.storage.device.common.header.LunLocate;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Create by weiwang at 2018/8/1
 *
 * Note: Move to "LunHostRefVO" soon
 */
@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = NvmeLunVO.class, myField = "nvmeLunUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = HostVO.class, myField = "hostUuid", targetField = "uuid"),
        }
)
public class NvmeLunHostRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = HostVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    @ForeignKey(parentEntityClass = NvmeLunVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String nvmeLunUuid;

    @Column
    private String hctl;

    @Column
    private String path;

    @Column
    @Enumerated(EnumType.STRING)
    private LunLocate locate;

    /**
     * Maybe: "", "PCIE", "TCP", "RDMA", "TCP/RDMA"
     */
    @Column
    private String transport;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public NvmeLunHostRefVO() {
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getNvmeLunUuid() {
        return nvmeLunUuid;
    }

    public void setNvmeLunUuid(String nvmeLunUuid) {
        this.nvmeLunUuid = nvmeLunUuid;
    }

    public LunLocate getLocate() {
        return locate;
    }

    public void setLocate(LunLocate locate) {
        this.locate = locate;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public String getHctl() {
        return hctl;
    }

    public void setHctl(String hctl) {
        this.hctl = hctl;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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
