package org.zstack.network.service.portforwarding;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.network.service.vip.ModifyVipAttributesStruct;
import org.zstack.network.service.vip.Vip;
import org.zstack.network.service.vip.VipConstant;
import org.zstack.network.service.vip.VipInventory;

import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PortForwardingReturnVipFlow extends NoRollbackFlow {
    @Override
    public void run(final FlowTrigger trigger, Map data) {
        VipInventory v = (VipInventory) data.get(VipConstant.Params.VIP.toString());
        ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
        struct.setUseFor( PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE);
        //TODO add services uuid
        Vip vip = new Vip(v.getUuid());
        vip.setStruct(struct);
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
