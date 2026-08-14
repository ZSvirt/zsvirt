package org.zstack.network.service.portforwarding;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DetachPortForwardingFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(DetachPortForwardingFlow.class);

    @Autowired
    private PortForwardingManager pfMgr;

    public void run(final FlowTrigger trigger, Map data) {
        String providerType = (String) data.get(PortForwardingConstant.Params.PORTFORWARDING_SERVICE_PROVIDER_TYPE.toString());
        final PortForwardingStruct struct = (PortForwardingStruct) data.get(PortForwardingConstant.Params.PORTFORWARDING_STRUCT.toString());

        PortForwardingBackend bkd = pfMgr.getPortForwardingBackend(providerType);
        bkd.revokePortForwardingRule(struct, new Completion(trigger) {
            @Override
            public void success() {
                logger.debug(String.format("successfully detached %s", struct.toString()));
                trigger.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }
}
