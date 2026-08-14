package org.zstack.header.storage.snapshot.reference;


import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.volume.VolumeEO;
import org.zstack.header.volume.VolumeVO;

import javax.persistence.*;
import java.sql.Timestamp;


@Entity
@Table
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VolumeVO.class, myField = "volumeUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = VolumeVO.class, myField = "referenceVolumeUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = VolumeSnapshotVO.class, myField = "volumeSnapshotUuid", targetField = "uuid"),
        }
)
public class VolumeSnapshotReferenceVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    private String volumeUuid;

    @Column
    private String volumeSnapshotUuid;

    @Column
    private String volumeSnapshotInstallUrl;

    @Column
    private String directSnapshotUuid;

    @Column
    private String directSnapshotInstallUrl;

    @Column
    @ForeignKey(parentEntityClass = VolumeSnapshotReferenceTreeVO.class, onDeleteAction = ForeignKey.ReferenceOption.RESTRICT)
    private String treeUuid;

    @Column
    @ForeignKey(parentEntityClass = VolumeSnapshotReferenceVO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private Long parentId;


    /**
     * the UUID of resource referencing @volumeUuid.
     */
    @Column
    private String referenceUuid;

    @Column
    private String referenceType;

    @Column
    private String referenceInstallUrl;

    @Column
    @ForeignKey(parentEntityClass = VolumeEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String referenceVolumeUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getVolumeSnapshotUuid() {
        return volumeSnapshotUuid;
    }

    public void setVolumeSnapshotUuid(String volumeSnapshotUuid) {
        this.volumeSnapshotUuid = volumeSnapshotUuid;
    }

    public String getTreeUuid() {
        return treeUuid;
    }

    public void setTreeUuid(String treeUuid) {
        this.treeUuid = treeUuid;
    }

    public String getVolumeSnapshotInstallUrl() {
        return volumeSnapshotInstallUrl;
    }

    public void setVolumeSnapshotInstallUrl(String volumeSnapshotInstallUrl) {
        this.volumeSnapshotInstallUrl = volumeSnapshotInstallUrl;
    }

    public String getReferenceUuid() {
        return referenceUuid;
    }

    public void setReferenceUuid(String referenceUuid) {
        this.referenceUuid = referenceUuid;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceInstallUrl() {
        return referenceInstallUrl;
    }

    public void setReferenceInstallUrl(String referenceInstallUrl) {
        this.referenceInstallUrl = referenceInstallUrl;
    }

    public String getReferenceVolumeUuid() {
        return referenceVolumeUuid;
    }

    public void setReferenceVolumeUuid(String referenceVolumeUuid) {
        this.referenceVolumeUuid = referenceVolumeUuid;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public VolumeSnapshotReferenceVO clone() {
        VolumeSnapshotReferenceVO vo = new VolumeSnapshotReferenceVO();
        vo.volumeUuid = this.volumeUuid;
        vo.volumeSnapshotUuid = this.volumeSnapshotUuid;
        vo.directSnapshotUuid = this.directSnapshotUuid;
        vo.directSnapshotInstallUrl = this.directSnapshotInstallUrl;
        vo.volumeSnapshotInstallUrl = this.volumeSnapshotInstallUrl;
        vo.referenceUuid = this.referenceUuid;
        vo.referenceType = this.referenceType;
        vo.referenceInstallUrl = this.referenceInstallUrl;
        vo.referenceVolumeUuid = this.referenceVolumeUuid;
        return vo;
    }

    public String getDirectSnapshotUuid() {
        return directSnapshotUuid;
    }

    public void setDirectSnapshotUuid(String directSnapshotUuid) {
        this.directSnapshotUuid = directSnapshotUuid;
    }

    public String getDirectSnapshotInstallUrl() {
        return directSnapshotInstallUrl;
    }

    public void setDirectSnapshotInstallUrl(String directSnapshotInstallUrl) {
        this.directSnapshotInstallUrl = directSnapshotInstallUrl;
    }
}
