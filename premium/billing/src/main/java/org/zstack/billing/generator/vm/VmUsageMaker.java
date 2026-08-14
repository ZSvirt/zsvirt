package org.zstack.billing.generator.vm;

import org.zstack.billing.BillingConstants;
import org.zstack.billing.Usage;
import org.zstack.billing.generator.ResourceUsageMaker;
import org.zstack.billing.spendingcalculator.vm.VmUsageVO;
import org.zstack.core.db.Q;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yaoning.li on 2020/7/9.
 */
public class VmUsageMaker implements ResourceUsageMaker {

    @Override
    public Class getResourceVOClass() {
        return VmInstanceVO.class;
    }

    @Override
    public List<Usage> make(List<String> resourceUuids) {
        List<Usage> result = new ArrayList<>();

        List<VmInstanceVO> vmInstanceVOS = Q.New(VmInstanceVO.class)
                .in(VmInstanceVO_.uuid, resourceUuids)
                .list();

        for (VmInstanceVO vmInstanceVO : vmInstanceVOS) {
            String accountUuid = findOwnerUuidOfResource(vmInstanceVO.getUuid());

            if (!BillingConstants.VM_STATUS_ASSOCIATED_WITH_THE_BILL.contains(vmInstanceVO.getState().toString())) {
                continue;
            }

            VmUsageVO usageVO = new VmUsageVO();
            usageVO.setVmUuid(vmInstanceVO.getUuid());
            usageVO.setAccountUuid(accountUuid);
            usageVO.setName(vmInstanceVO.getName());
            usageVO.setDateInLong(System.currentTimeMillis());
            usageVO.setCpuNum(vmInstanceVO.getCpuNum());
            usageVO.setMemorySize(vmInstanceVO.getMemorySize());
            usageVO.setInventory(JSONObjectUtil.toJsonString(VmInstanceInventory.valueOf(vmInstanceVO)));
            usageVO.setState(vmInstanceVO.getState().toString());
            if (vmInstanceVO.getRootVolume() != null) {
                usageVO.setRootVolumeSize(vmInstanceVO.getRootVolume().getSize());
            }
            result.add(usageVO);
        }

        return result;
    }
}
