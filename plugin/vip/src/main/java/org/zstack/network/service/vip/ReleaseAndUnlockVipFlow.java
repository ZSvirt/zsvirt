package org.zstack.network.service.vip;

/**
 */

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;

import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ReleaseAndUnlockVipFlow extends NoRollbackFlow {
    @Autowired
    private CloudBus bus;

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        /* this flow will be called after ReleaseNetworkServicesOnVipFlow
        * for now, there is no need to call this flow
        final VipInventory v = (VipInventory) data.get(VipConstant.Params.VIP.toString());
        ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
        struct.setUseFor(VipConstant.NETWORK_SERVICE_TYPE);
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
        });*/
    }
}
