package org.zstack.autoscaling.group.rule;

import org.zstack.autoscaling.AutoScalingConstants;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.tag.SystemTagVO;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Create by lining at 2018/9/11
 */
@PythonClassInventory
@Inventory(mappingVOClass = AddingNewInstanceRuleVO.class, collectionValueOfMethod = "valueOf2",
        parent = {@Parent(inventoryClass = AutoScalingRuleInventory.class, type = AutoScalingConstants.AutoScalingRule.RULE_TYPE_ADDING_NEW_INSTANCE)})
public class AddingNewVmRuleInventory extends AutoScalingRuleInventory {

    private String adjustmentType;

    private Integer adjustmentValue;

    public AddingNewVmRuleInventory() {
    }

    public AddingNewVmRuleInventory(AddingNewInstanceRuleVO vo) {
        super(vo);
        this.setAdjustmentType(vo.getAdjustmentType().toString());
        this.setAdjustmentValue(vo.getAdjustmentValue());
    }

    public static AddingNewVmRuleInventory valueOf(AddingNewInstanceRuleVO vo) {
        return new AddingNewVmRuleInventory(vo);
    }

    public static List<AddingNewVmRuleInventory> valueOf2(Collection<AddingNewInstanceRuleVO> vos) {
        List<AddingNewVmRuleInventory> invs = new ArrayList<AddingNewVmRuleInventory>();
        for (AddingNewInstanceRuleVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(String adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public Integer getAdjustmentValue() {
        return adjustmentValue;
    }

    public void setAdjustmentValue(Integer adjustmentValue) {
        this.adjustmentValue = adjustmentValue;
    }
}
