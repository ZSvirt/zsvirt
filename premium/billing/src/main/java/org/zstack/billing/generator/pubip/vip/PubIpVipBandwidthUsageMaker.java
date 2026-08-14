package org.zstack.billing.generator.pubip.vip;

import org.zstack.billing.Usage;
import org.zstack.billing.generator.ResourceUsageMaker;
import org.zstack.billing.spendingcalculator.vip.PubIpVipBandwidthUsageVO;
import org.zstack.core.db.Q;
import org.zstack.header.vipQos.VipQosVO;
import org.zstack.header.vipQos.VipQosVO_;
import org.zstack.network.service.vip.VipCanonicalEvents;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yaoning.li on 2020/7/9.
 */
public class PubIpVipBandwidthUsageMaker implements ResourceUsageMaker {

    @Override
    public Class getResourceVOClass() {
        return VipVO.class;
    }

    @Override
    public List<Usage> make(List<String> resourceUuids) {
        List<Usage> result = new ArrayList<>();

        List<VipVO> vipVOS = Q.New(VipVO.class)
                .in(VipVO_.uuid, resourceUuids)
                .list();

        for (VipVO vipVO : vipVOS) {
            List<VipQosVO> vipQosVOS = Q.New(VipQosVO.class)
                    .eq(VipQosVO_.vipUuid, vipVO.getUuid())
                    .list();
            if (vipQosVOS.isEmpty()) {
                continue;
            }

            String accountUuid = findOwnerUuidOfResource(vipVO.getUuid());

            PubIpVipBandwidthUsageVO usageVO = new PubIpVipBandwidthUsageVO();
            usageVO.setAccountUuid(accountUuid);
            usageVO.setDateInLong(System.currentTimeMillis());
            usageVO.setVipName(vipVO.getName());
            usageVO.setVipUuid(vipVO.getUuid());
            usageVO.setVipIp(vipVO.getIp());
            usageVO.setVipStatus(VipCanonicalEvents.VIP_STATUS_CREATED);
            usageVO.setL3NetworkUuid(vipVO.getL3NetworkUuid());
            usageVO.setInventory(JSONObjectUtil.toJsonString(VipInventory.valueOf(vipVO)));
            usageVO.setBandwidthIn(0L);
            usageVO.setBandwidthOut(0L);
            for (VipQosVO qosVO : vipQosVOS) {
                long bandwidthIn = usageVO.getBandwidthIn() + qosVO.getInboundBandwidth();
                long bandwidthOut = usageVO.getBandwidthOut() + qosVO.getOutboundBandwidth();
                usageVO.setBandwidthIn(bandwidthIn);
                usageVO.setBandwidthOut(bandwidthOut);
            }
            result.add(usageVO);
        }

        return result;
    }
}
