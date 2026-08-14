package org.zstack.header.vmscheduling;

import org.zstack.header.affinitygroup.AffinityGroupVO;

import javax.persistence.*;

@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
public class VmSchedulingRuleVO extends AffinityGroupVO {
    @Column
    @Enumerated(EnumType.STRING)
    private VMSchedulingRuleType rule;

    @Column
    @Enumerated(EnumType.STRING)
    private VMSchedulingRuleMode mode;

    public VmSchedulingRuleVO() {
    }

    public VmSchedulingRuleVO(AffinityGroupVO vo) {
        super(vo);
    }

    public VMSchedulingRuleType getRule() {
        return rule;
    }

    public void setRule(VMSchedulingRuleType rule) {
        this.rule = rule;
    }

    public VMSchedulingRuleMode getMode() {
        return mode;
    }

    public void setMode(VMSchedulingRuleMode mode) {
        this.mode = mode;
    }
}
