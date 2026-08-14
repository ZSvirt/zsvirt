package org.zstack.console;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.tacker.PingTracker;
import org.zstack.header.console.ConsoleBackend;
import org.zstack.header.console.PingConsoleProxyAgentMsg;
import org.zstack.header.console.PingConsoleProxyAgentReply;
import org.zstack.header.console.ReconnectConsoleProxyMsg;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 * Created by xing5 on 2016/4/8.
 */
public class ConsoleProxyAgentTracker extends PingTracker {
    private static final CLogger logger = Utils.getLogger(ConsoleProxyAgentTracker.class);

    @Autowired
    private ConsoleManager cmgr;

    @Override
    public String getResourceName() {
        return "console proxy agent";
    }

    @Override
    public NeedReplyMessage getPingMessage(String resUuid) {
        PingConsoleProxyAgentMsg msg = new PingConsoleProxyAgentMsg();
        msg.setAgentUuid(resUuid);

        ConsoleBackend bkd = cmgr.getConsoleBackend();
        msg.setServiceId(bkd.returnServiceIdForConsoleAgentMsg(msg, resUuid));
        return msg;
    }

    @Override
    public int getPingInterval() {
        return ConsoleGlobalConfig.PING_INTERVAL.value(Integer.class);
    }

    @Override
    public int getParallelismDegree() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected void startHook() {
        ConsoleGlobalConfig.PING_INTERVAL.installLocalUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                pingIntervalChanged();
            }
        });
    }

    @Override
    public void handleReply(final String resourceUuid, MessageReply reply) {
        if (!reply.isSuccess()) {
            logger.warn(String.format("unable to ping the console proxy agent[uuid:%s], %s", resourceUuid, reply.getError()));
            if (timeToReconnect()) {
                logger.debug("It's time to reconnect console proxy agent.");
                doReconnect(resourceUuid);
            }
            return;
        }

        PingConsoleProxyAgentReply pr = reply.castReply();
        if (pr.isDoReconnect()) {
            doReconnect(resourceUuid);
        }
    }

    private void doReconnect(String resourceUuid) {
        ReconnectConsoleProxyMsg rmsg = new ReconnectConsoleProxyMsg();
        rmsg.setAgentUuid(resourceUuid);

        ConsoleBackend bkd = cmgr.getConsoleBackend();
        rmsg.setServiceId(bkd.returnServiceIdForConsoleAgentMsg(rmsg, resourceUuid));
        bus.send(rmsg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to reconnect console proxy agent[uuid:%s], %s", resourceUuid,
                            reply.getError()));
                } else {
                    logger.info(String.format("successfully reconnected the console proxy agent[uuid:%s]", resourceUuid));
                }
            }
        });
    }

    private boolean timeToReconnect() {
        return System.currentTimeMillis() % TimeUnit.MINUTES.toMillis(30) < TimeUnit.SECONDS.toMillis(getPingInterval());
    }
}
