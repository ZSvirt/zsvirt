package org.zstack.header.baremetal.network;

import org.zstack.header.baremetal.chassis.BaremetalChassisVO;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.*;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 2019-01-03.
 */
@Entity
@Table
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = BaremetalChassisVO.class, myField = "chassisUuid", targetField = "uuid"),
        }
)
public class BaremetalBondingVO extends ResourceVO implements ToInventory, OwnedByAccount {
    @Column
    @ForeignKey(parentEntityClass = BaremetalChassisVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String chassisUuid;

    @Column
    private String name;

    @Column
    private Integer mode;

    @Column
    private String slaves;

    @Column
    private String opts;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    @Transient
    private String accountUuid;

    public String getChassisUuid() {
        return chassisUuid;
    }

    public void setChassisUuid(String chassisUuid) {
        this.chassisUuid = chassisUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMode() {
        return mode;
    }

    public void setMode(Integer mode) {
        this.mode = mode;
    }

    public String getSlaves() {
        return slaves;
    }

    public void setSlaves(String slaves) {
        this.slaves = slaves;
    }

    public String getOpts() {
        return opts;
    }

    public void setOpts(String opts) {
        this.opts = opts;
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

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
}
