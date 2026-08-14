package org.zstack.header.vm;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * @ Author : yh.w
 * @ Date   : Created in 14:48 2019/9/18
 */
@Entity
@Table
public class VmPriorityConfigVO extends ResourceVO implements OwnedByAccount {

    @Column
    @Enumerated(value = EnumType.STRING)
    private VmPriorityLevel level;

    @Column
    private int cpuShares;

    @Column
    private int oomScoreAdj;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public VmPriorityLevel getLevel() {
        return level;
    }

    public void setLevel(VmPriorityLevel level) {
        this.level = level;
    }

    public int getCpuShares() {
        return cpuShares;
    }

    public void setCpuShares(int cpuShares) {
        this.cpuShares = cpuShares;
    }

    public int getOomScoreAdj() {
        return oomScoreAdj;
    }

    public void setOomScoreAdj(int oomScoreAdj) {
        this.oomScoreAdj = oomScoreAdj;
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
