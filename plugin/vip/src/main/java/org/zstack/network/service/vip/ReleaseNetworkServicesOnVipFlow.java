package org.zstack.network.service.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;

import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ReleaseNetworkServicesOnVipFlow extends NoRollbackFlow {
    @Autowired
    private VipManager vipMgr;

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        VipInventory vip = (VipInventory) data.get(VipConstant.Params.VIP.toString());

        VipReleaseExtensionPoint ext = vipMgr.getVipReleaseExtensionPoint(vip.getUseFor());
        ext.releaseServicesOnVip(vip, new Completion(trigger) {
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
