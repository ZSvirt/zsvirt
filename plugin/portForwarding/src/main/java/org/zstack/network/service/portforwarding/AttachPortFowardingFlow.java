package org.zstack.network.service.portforwarding;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AttachPortFowardingFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(AttachPortFowardingFlow.class);

    @Autowired
    private PortForwardingManager pfMgr;

    private static final String SUCCESS = AttachPortFowardingFlow.class.getName();

    public void run(final FlowTrigger trigger, final Map data) {
        String providerType = (String) data.get(PortForwardingConstant.Params.PORTFORWARDING_SERVICE_PROVIDER_TYPE.toString());
        final PortForwardingStruct struct = (PortForwardingStruct) data.get(PortForwardingConstant.Params.PORTFORWARDING_STRUCT.toString());

        PortForwardingBackend bkd = pfMgr.getPortForwardingBackend(providerType);
        bkd.applyPortForwardingRule(struct, new Completion(trigger) {
            @Override
            public void success() {
                logger.debug(String.format("successfully attached %s", struct.toString()));
                data.put(SUCCESS, true);
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

        String providerType = (String) data.get(PortForwardingConstant.Params.PORTFORWARDING_SERVICE_PROVIDER_TYPE.toString());
        final PortForwardingStruct struct = (PortForwardingStruct) data.get(PortForwardingConstant.Params.PORTFORWARDING_STRUCT.toString());
        PortForwardingBackend bkd = pfMgr.getPortForwardingBackend(providerType);
        bkd.revokePortForwardingRule(struct, new Completion(trigger) {
            @Override
            public void success() {
                logger.debug(String.format("successfully revoked %s", struct.toString()));
                trigger.rollback();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("successfully revoked %s, service provider should clean up", struct.toString()));
                trigger.rollback();
            }
        });
    }
}
