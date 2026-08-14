package org.zstack.ipsec.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.ipsec.IPsecConnectionInventory;
import org.zstack.ipsec.IPsecConstants;
import org.zstack.ipsec.vyos.VyosIPsecConstants.Param;
import org.zstack.network.service.vip.ModifyVipAttributesStruct;
import org.zstack.network.service.vip.Vip;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 * Created by xing5 on 2016/11/10.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosReleaseVipFlow extends NoRollbackFlow {

    private static final CLogger logger = Utils.getLogger(VyosIPsecBackend.class);

    @Override
    public void run(FlowTrigger trigger, Map data) {
        boolean skip_vip = (boolean) data.get(Param.SKIP_VIP);
        if (skip_vip) {
            trigger.next();
            return;
        }

        VipInventory v = (VipInventory) data.get(Param.VIP);
        VyosIPsecBackend.IPsecInfo info = (VyosIPsecBackend.IPsecInfo) data.get(Param.IPSEC_INFO);
        IPsecConnectionInventory inv = (IPsecConnectionInventory)data.get(Param.IPSEC_STRUCT);
        String providerType = (String)data.get(Param.SERVICE_PROVIDER_TYPE);

        ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
        vipStruct.setUseFor(IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString());
        vipStruct.setServiceUuid(info.uuid);
        if (!inv.getLocalL3Networks().isEmpty()) {
            vipStruct.setPeerL3NetworkUuids(inv.getLocalL3Networks());
        }
        vipStruct.setServiceProvider(providerType);

        Vip vip = new Vip(v.getUuid());
        vip.setStruct(vipStruct);
        vip.release(new Completion(trigger) {
            @Override
            public void success() {
                trigger.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }
}
