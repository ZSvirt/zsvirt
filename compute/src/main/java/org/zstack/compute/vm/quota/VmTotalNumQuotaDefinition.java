package org.zstack.compute.vm.quota;

import org.zstack.compute.vm.VmQuotaConstant;
import org.zstack.compute.vm.VmQuotaGlobalConfig;
import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.identity.ResourceHelper;

import java.util.List;

public class VmTotalNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return VmQuotaConstant.VM_TOTAL_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return VmQuotaGlobalConfig.VM_TOTAL_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        List<VmInstanceVO> list = ResourceHelper.findOwnResources(VmInstanceVO.class, accountUuid,
                q -> q.notEq(VmInstanceVO_.type, "baremetal2").notEq(VmInstanceVO_.state, VmInstanceState.Destroyed));
        list.removeIf(vm -> vm.getHostUuid() == null && vm.getLastHostUuid() == null && vm.getRootVolumeUuid() == null);
        return (long) list.size();
    }
}
