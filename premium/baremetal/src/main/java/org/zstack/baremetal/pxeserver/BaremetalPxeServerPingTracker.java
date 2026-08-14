package org.zstack.baremetal.pxeserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.Q;
import org.zstack.core.tacker.PingTracker;
import org.zstack.header.baremetal.pxeserver.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 2018-10-13.
 */
public class BaremetalPxeServerPingTracker extends PingTracker {
    @Autowired
    private CloudBus bus;

    @Override
    public String getResourceName() {
        return "Baremetal PxeServer";
    }

    @Override
    public NeedReplyMessage getPingMessage(String resUuid) {
        BaremetalPxeServerState state = Q.New(BaremetalPxeServerVO.class)
                .eq(BaremetalPxeServerVO_.uuid, resUuid)
                .select(BaremetalPxeServerVO_.state)
                .findValue();

        PingBaremetalPxeServerMsg msg = new PingBaremetalPxeServerMsg();
        msg.setUuid(resUuid);
        msg.setEnabled(state == BaremetalPxeServerState.Enabled);
        bus.makeTargetServiceIdByResourceUuid(msg, BaremetalPxeServerConstant.SERVICE_ID, resUuid);
        return msg;
    }

    @Override
    public int getPingInterval() {
        return BaremetalPxeServerGlobalConfig.PING_INTERVAL.value(Integer.class);
    }

    @Override
    public int getParallelismDegree() {
        return BaremetalPxeServerGlobalConfig.PING_PARALLELISM_DEGREE.value(Integer.class);
    }

    @Override
    public void handleReply(String resourceUuid, MessageReply reply) {
        // nothing to do
    }

    @Override
    protected void startHook() {
        BaremetalPxeServerGlobalConfig.PING_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                pingIntervalChanged();
            }
        });
    }
}
