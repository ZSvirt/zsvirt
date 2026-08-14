package org.zstack.ipsec.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.ipsec.IPsecConnectionInventory;
import org.zstack.ipsec.IPsecConstants;
import org.zstack.ipsec.vyos.VyosIPsecConstants.Param;
import org.zstack.network.service.vip.ModifyVipAttributesStruct;
import org.zstack.network.service.vip.Vip;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2016/11/10.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosAcquireVipFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VyosAcquireVipFlow.class);

    private static final String SUCCESS = VyosAcquireVipFlow.class.getName();

    @Override
    public void run(FlowTrigger trigger, Map data) {
        VipInventory v = (VipInventory) data.get(Param.VIP);
        List<L3NetworkInventory> l3Invs = (List<L3NetworkInventory>) data.get(Param.GUEST_L3);
        String provideType = (String) data.get(Param.SERVICE_PROVIDER_TYPE);
        IPsecConnectionInventory inv = (IPsecConnectionInventory) data.get(Param.IPSEC_STRUCT);

        List<ErrorCode> errs = new ArrayList<>();
        new While<>(l3Invs).each((l3Inv, whileComplection) -> {
            ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
            vipStruct.setUseFor(IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString());
            vipStruct.setServiceUuid(inv.getUuid());
            vipStruct.setPeerL3NetworkUuid(l3Inv.getUuid());
            vipStruct.setServiceProvider(provideType);
            Vip vip = new Vip(v.getUuid());
            vip.setStruct(vipStruct);
            vip.acquire(new Completion(whileComplection) {
                @Override
                public void success() {
                    whileComplection.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    errs.add(errorCode);
                    whileComplection.done();
                }
            });
        }).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errs.size() > 0) {
                    trigger.fail(errs.get(0));
                } else {
                    data.put(SUCCESS, true);
                    trigger.next();
                }
            }
        });
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        if (!data.containsKey(SUCCESS)) {
            trigger.rollback();
            return;
        }

        List<L3NetworkInventory> l3Invs = (List<L3NetworkInventory>) data.get(Param.GUEST_L3);
        String provideType = (String) data.get(Param.SERVICE_PROVIDER_TYPE);
        VipInventory v = (VipInventory) data.get(Param.VIP);
        IPsecConnectionInventory inv = (IPsecConnectionInventory) data.get(Param.IPSEC_STRUCT);

        ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
        vipStruct.setUseFor(IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString());
        vipStruct.setServiceUuid(inv.getUuid());
        if (!l3Invs.isEmpty()) {
            vipStruct.setPeerL3NetworkUuids(l3Invs.stream().map(L3NetworkInventory::getUuid).collect(Collectors.toList()));
        }
        vipStruct.setServiceProvider(provideType);
        Vip vip = new Vip(v.getUuid());
        vip.setStruct(vipStruct);
        vip.release(new Completion(trigger) {
            @Override
            public void success() {
                trigger.rollback();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                //TODO: add GC
                logger.warn(errorCode.getReadableDetails());
                trigger.rollback();
            }
        });
    }
}
