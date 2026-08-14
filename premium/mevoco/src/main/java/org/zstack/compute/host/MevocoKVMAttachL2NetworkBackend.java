package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.Completion;
import org.zstack.header.host.GetHostNetworkFactsMsg;
import org.zstack.header.host.GetHostNetworkFactsReply;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkAttachClusterExtensionPoint;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by GuoYi on 5/4/20.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class MevocoKVMAttachL2NetworkBackend implements L2NetworkAttachClusterExtensionPoint {
    private static final CLogger logger = Utils.getLogger(MevocoKVMAttachL2NetworkBackend.class);

    @Autowired
    protected CloudBus bus;

    @Override
    public void beforeAttach(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        completion.success();
    }

    @Override
    public void afterAttach(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        // get network facts from host after l2 network attached to the cluster
        GetHostNetworkFactsMsg msg = new GetHostNetworkFactsMsg();
        msg.setHostUuid(hostUuid);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to get network facts from host[uuid:%s] after l2 network[uuid:%s] attached to cluster: %s", hostUuid, l2Network.getUuid(), reply.getError()));
                } else {
                    GetHostNetworkFactsReply rly = reply.castReply();
                    if (!rly.isSuccess()) {
                        logger.warn(String.format("failed to get network facts from host[uuid:%s] after l2 network[uuid:%s] attached to cluster: %s", hostUuid, l2Network.getUuid(), rly.getError()));
                    } else {
                        logger.debug(String.format("successfully get network facts from host[uuid:%s] after l2 network[uuid:%s] attached to cluster", hostUuid, l2Network.getUuid()));
                    }
                }

                // do not stop the flow chain even if failed to get host facts
                completion.success();
            }
        });
    }
}
