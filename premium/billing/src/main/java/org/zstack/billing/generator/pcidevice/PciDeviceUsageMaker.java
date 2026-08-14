package org.zstack.billing.generator.pcidevice;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.billing.BillingConstants;
import org.zstack.billing.Usage;
import org.zstack.billing.generator.ResourceUsageMaker;
import org.zstack.billing.spendingcalculator.pcidevice.PciDeviceUsageVO;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.pciDevice.PciDeviceInventory;
import org.zstack.pciDevice.PciDeviceStatus;
import org.zstack.pciDevice.PciDeviceVO;
import org.zstack.pciDevice.PciDeviceVO_;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yaoning.li on 2020/7/9.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PciDeviceUsageMaker implements ResourceUsageMaker {
    @Autowired
    protected DatabaseFacade dbf;

    @Override
    public Class getResourceVOClass() {
        return PciDeviceVO.class;
    }

    @Override
    public List<Usage> make(List<String> resourceUuids) {
        List<Usage> result = new ArrayList<>();

        List<PciDeviceVO> pciDeviceVOS = Q.New(PciDeviceVO.class)
                .in(PciDeviceVO_.uuid, resourceUuids)
                .list();

        for (PciDeviceVO pciDeviceVO : pciDeviceVOS) {
            if (pciDeviceVO.getVmInstanceUuid() == null) {
                continue;
            }

            VmInstanceVO vmInstanceVO = dbf.findByUuid(pciDeviceVO.getVmInstanceUuid(), VmInstanceVO.class);
            String accountUuid = findOwnerUuidOfResource(vmInstanceVO.getUuid());

            if (!BillingConstants.VM_STATUS_ASSOCIATED_WITH_THE_BILL.contains(vmInstanceVO.getState().toString())) {
                continue;
            }

            PciDeviceUsageVO usageVO = new PciDeviceUsageVO();
            usageVO.setAccountUuid(accountUuid);
            usageVO.setDateInLong(System.currentTimeMillis());
            usageVO.setPciDeviceUuid(pciDeviceVO.getUuid());
            usageVO.setVendorId(pciDeviceVO.getVendorId());
            usageVO.setDeviceId(pciDeviceVO.getDeviceId());
            usageVO.setSubvendorId(pciDeviceVO.getSubvendorId());
            usageVO.setSubdeviceId(pciDeviceVO.getSubdeviceId());
            usageVO.setDescription(pciDeviceVO.getDescription());
            usageVO.setVmUuid(pciDeviceVO.getVmInstanceUuid());
            usageVO.setVmName(Q.New(VmInstanceVO.class).select(VmInstanceVO_.name).eq(VmInstanceVO_.uuid, pciDeviceVO.getVmInstanceUuid()).findValue());
            if (VmInstanceState.Destroyed == vmInstanceVO.getState()) {
                usageVO.setStatus(PciDeviceStatus.System.toString());
            } else {
                usageVO.setStatus(pciDeviceVO.getStatus().toString());
            }
            usageVO.setInventory(JSONObjectUtil.toJsonString(PciDeviceInventory.valueOf(pciDeviceVO)));
            result.add(usageVO);
        }

        return result;
    }
}
