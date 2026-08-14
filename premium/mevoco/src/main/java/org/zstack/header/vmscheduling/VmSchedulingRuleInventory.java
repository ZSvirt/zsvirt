package org.zstack.header.vmscheduling;

import org.zstack.header.affinitygroup.AffinityGroupInventory;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = VmSchedulingRuleVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = AffinityGroupInventory.class, type = VmSchedulingRuleConstants.VM_SCHEDULING_RULE_TYPE)})
public class VmSchedulingRuleInventory extends AffinityGroupInventory {
    private String rule;
    private String mode;

    public VmSchedulingRuleInventory() {
    }

    protected VmSchedulingRuleInventory(VmSchedulingRuleVO vo){
        super(vo);
        this.setRule(vo.getRule().toString());
        this.setMode(vo.getMode().toString());

    }

    public static VmSchedulingRuleInventory valueOf(VmSchedulingRuleVO vo) {
        return new VmSchedulingRuleInventory(vo);
    }

    public static List<VmSchedulingRuleInventory> valueOf1(Collection<VmSchedulingRuleVO> vos) {
        List<VmSchedulingRuleInventory> invs = new ArrayList<VmSchedulingRuleInventory>();
        for (VmSchedulingRuleVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }


    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
