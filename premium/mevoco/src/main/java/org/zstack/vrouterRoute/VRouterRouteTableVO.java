package org.zstack.vrouterRoute;

import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by weiwang on 15/06/2017.
 */
@Entity
@Table
public class VRouterRouteTableVO extends ResourceVO {
    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String type;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "routeTableUuid", insertable = false, updatable = false)
    @NoView
    private Set<VirtualRouterVRouterRouteTableRefVO> attachedRouterRefs = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "routeTableUuid", insertable = false, updatable = false)
    @NoView
    private Set<VRouterRouteEntryVO> routeEntries = new HashSet<>();

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public Set<VirtualRouterVRouterRouteTableRefVO> getAttachedRouterRefs() {
        return attachedRouterRefs;
    }

    public void setAttachedRouterRefs(Set<VirtualRouterVRouterRouteTableRefVO> attachedRouterRefs) {
        this.attachedRouterRefs = attachedRouterRefs;
    }

    public Set<VRouterRouteEntryVO> getRouteEntries() {
        return routeEntries;
    }

    public void setRouteEntries(Set<VRouterRouteEntryVO> routeEntries) {
        this.routeEntries = routeEntries;
    }
}
