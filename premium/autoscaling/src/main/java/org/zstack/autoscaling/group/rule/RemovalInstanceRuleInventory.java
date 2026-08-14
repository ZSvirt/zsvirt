package org.zstack.autoscaling.group.rule;

import org.zstack.autoscaling.AutoScalingConstants;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.storage.ceph.CephConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Create by lining at 2018/9/12
 */
@PythonClassInventory
@Inventory(mappingVOClass = RemovalInstanceRuleVO.class, collectionValueOfMethod = "valueOf2",
        parent = {@Parent(inventoryClass = AutoScalingRuleInventory.class, type = AutoScalingConstants.AutoScalingRule.RULE_TYPE_REMOVAL_INSTANCE)})
public class RemovalInstanceRuleInventory extends AutoScalingRuleInventory {

    private String removalPolicy;

    private String adjustmentType;

    private Integer adjustmentValue;

    public RemovalInstanceRuleInventory() {
    }

    public RemovalInstanceRuleInventory(RemovalInstanceRuleVO vo) {
        super(vo);
        this.setRemovalPolicy(vo.getRemovalPolicy().toString());
        this.setAdjustmentValue(vo.getAdjustmentValue());
        this.setAdjustmentType(vo.getAdjustmentType().toString());
    }

    public static RemovalInstanceRuleInventory valueOf(RemovalInstanceRuleVO vo) {
        return new RemovalInstanceRuleInventory(vo);
    }

    public static List<RemovalInstanceRuleInventory> valueOf2(Collection<RemovalInstanceRuleVO> vos) {
        List<RemovalInstanceRuleInventory> invs = new ArrayList<RemovalInstanceRuleInventory>();
        for (RemovalInstanceRuleVO vo : vos) {
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

    public String getRemovalPolicy() {
        return removalPolicy;
    }

    public void setRemovalPolicy(String removalPolicy) {
        this.removalPolicy = removalPolicy;
    }
}
