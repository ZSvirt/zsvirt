package org.zstack.storage.device.iscsi;

import org.zstack.core.convert.PasswordConverter;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Create by weiwang at 2018/8/1
 */
@Entity
@Table
@AutoDeleteTag
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = IscsiTargetVO.class, myField = "uuid", targetField = "iscsiServerUuid"),
                @EntityGraph.Neighbour(type = IscsiServerClusterRefVO.class, myField = "uuid", targetField = "iscsiServerUuid")
        }
)
public class IscsiServerVO extends ResourceVO implements ToInventory {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name="iscsiServerUuid", insertable=false, updatable=false)
    @NoView
    private Set<IscsiServerClusterRefVO> iscsiClusterRefs = new HashSet<IscsiServerClusterRefVO>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name="iscsiServerUuid", insertable=false, updatable=false)
    @NoView
    private Set<IscsiTargetVO> iscsiTargets = new HashSet<IscsiTargetVO>();

    @Column
    private String name;

    @Column
    private String ip;

    @Column
    private Integer port;

    @Column
    private String chapUserName;

    @Column
    @Convert(converter = PasswordConverter.class)
    private String chapUserPassword;

    @Column
    private String state;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public Set<IscsiServerClusterRefVO> getIscsiClusterRefs() {
        return iscsiClusterRefs;
    }

    public void setIscsiClusterRefs(Set<IscsiServerClusterRefVO> iscsiClusterRefs) {
        this.iscsiClusterRefs = iscsiClusterRefs;
    }

    public Set<IscsiTargetVO> getIscsiTargets() {
        return iscsiTargets;
    }

    public Set<IscsiLunVO> getIscsiLuns() {
        Set<IscsiLunVO> result = new HashSet<>();
        for (IscsiTargetVO targetVO : getIscsiTargets()) {
            result.addAll(targetVO.getIscsiLuns());
        }
        return result;
    }

    public void setIscsiTargets(Set<IscsiTargetVO> iscsiTargets) {
        this.iscsiTargets = iscsiTargets;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getChapUserName() {
        return chapUserName;
    }

    public void setChapUserName(String chapUserName) {
        this.chapUserName = chapUserName;
    }

    public String getChapUserPassword() {
        return chapUserPassword;
    }

    public void setChapUserPassword(String chapUserPassword) {
        this.chapUserPassword = chapUserPassword;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

