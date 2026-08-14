package org.zstack.billing.generator.pubip.vmnic;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.billing.Usage;
import org.zstack.billing.generator.ResourceUsageMaker;
import org.zstack.billing.spendingcalculator.vmnic.PubIpVmNicBandwidthUsageVO;
import org.zstack.compute.vm.VmInstanceManager;
import org.zstack.compute.vm.VmNicQosConfigBackend;
import org.zstack.compute.vm.VmNicQosStruct;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.network.l3.L3NetworkCategory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vm.*;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yaoning.li on 2020/7/9.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PubIpVmNicBandwidthUsageMaker implements ResourceUsageMaker {
    @Autowired
    DatabaseFacade dbf;

    @Autowired
    VmInstanceManager vmMgr;

    @Override
    public Class getResourceVOClass() {
        return VmNicVO.class;
    }

    @Override
    public List<Usage> make(List<String> resourceUuids) {
        List<Usage> result = new ArrayList<>();

        List<VmNicVO> vmNicVOS = Q.New(VmNicVO.class)
                .in(VmNicVO_.uuid, resourceUuids)
                .list();

        for (VmNicVO vmNicVO : vmNicVOS) {
            String l3Uuid = vmNicVO.getL3NetworkUuid();
            boolean isPubL3 = Q.New(L3NetworkVO.class)
                    .eq(L3NetworkVO_.uuid, l3Uuid)
                    .eq(L3NetworkVO_.category, L3NetworkCategory.Public)
                    .isExists();
            if (!isPubL3) {
                continue;
            }

            String vmType = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmNicVO.getVmInstanceUuid())
                    .select(VmInstanceVO_.type).findValue();
            VmNicQosConfigBackend backend = vmMgr.getVmNicQosConfigBackend(vmType);
            VmNicQosStruct struct = backend.getNicQos(vmNicVO.getVmInstanceUuid(), vmNicVO.getUuid());
            Long inbound = struct.inboundBandwidth;
            Long outbound = struct.outboundBandwidth;
            if (inbound == -1L && outbound == -1L) {
                continue;
            }

            VmInstanceVO vmInstanceVO = dbf.findByUuid(vmNicVO.getVmInstanceUuid(), VmInstanceVO.class);
            String accountUuid = findOwnerUuidOfResource(vmInstanceVO.getUuid());

            PubIpVmNicBandwidthUsageVO usageVO = new PubIpVmNicBandwidthUsageVO();
            usageVO.setVmNicUuid(vmNicVO.getUuid());
            usageVO.setAccountUuid(accountUuid);
            usageVO.setInventory(JSONObjectUtil.toJsonString(VmNicInventory.valueOf(vmNicVO)));
            usageVO.setL3NetworkUuid(vmNicVO.getL3NetworkUuid());
            List<String> ips = VmNicHelper.getIpAddresses(vmNicVO);
            usageVO.setVmNicIp(StringUtils.join(ips, ","));
            usageVO.setVmInstanceUuid(vmNicVO.getVmInstanceUuid());
            if (VmInstanceState.Running == vmInstanceVO.getState() || VmInstanceState.Paused == vmInstanceVO.getState()){
                usageVO.setVmNicStatus(VmInstanceState.Running.toString());
            } else {
                usageVO.setVmNicStatus(VmInstanceState.Stopped.toString());
            }
            usageVO.setDateInLong(System.currentTimeMillis());
            usageVO.setBandwidthIn(0L);
            usageVO.setBandwidthOut(0L);
            if (outbound != null && outbound != -1L) {
                usageVO.setBandwidthOut(outbound);
            }
            if (inbound != null && inbound != -1L) {
                usageVO.setBandwidthIn(inbound);
            }

            result.add(usageVO);
        }

        return result;
    }
}
