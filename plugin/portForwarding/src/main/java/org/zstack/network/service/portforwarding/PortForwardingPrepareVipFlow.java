package org.zstack.network.service.portforwarding;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.network.service.portforwarding.PortForwardingConstant.Params;
import org.zstack.network.service.vip.ModifyVipAttributesStruct;
import org.zstack.network.service.vip.Vip;
import org.zstack.network.service.vip.VipConstant;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PortForwardingPrepareVipFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(PortForwardingPrepareVipFlow.class);

    private static final String SUCCESS = PortForwardingPrepareVipFlow.class.getName();

    public void run(final FlowTrigger trigger, final Map data) {
        final VipInventory v = (VipInventory) data.get(VipConstant.Params.VIP.toString());
        final String serviceProviderType = (String) data.get(VipConstant.Params.VIP_SERVICE_PROVIDER_TYPE.toString());
        final L3NetworkInventory peerL3 = (L3NetworkInventory) data.get(VipConstant.Params.GUEST_L3NETWORK_VIP_FOR.toString());
        boolean needLockVip = data.containsKey(Params.NEED_LOCK_VIP.toString());

        ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
        struct.setUseFor( PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE);
        //TODO add services uuid
        Vip vip = new Vip(v.getUuid());
        vip.setStruct(struct);
        vip.acquire(new Completion(trigger) {
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

    @Override
    public void rollback(final FlowRollback trigger, Map data) {
        if (!data.containsKey(SUCCESS)) {
            trigger.rollback();
            return;
        }

        VipInventory v = (VipInventory) data.get(VipConstant.Params.VIP.toString());
        ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
        struct.setUseFor( PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE);
        //TODO add services uuid
        Vip vip = new Vip(v.getUuid());
        vip.setStruct(struct);
        vip.release(new Completion(trigger) {
            @Override
            public void success() {
                trigger.rollback();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                //TODO add GC
                logger.warn(errorCode.getReadableDetails());
                trigger.rollback();
            }
        });
    }
}
