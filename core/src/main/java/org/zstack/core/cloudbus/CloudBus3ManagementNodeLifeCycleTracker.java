package org.zstack.core.cloudbus;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.AsyncTimer;
import org.zstack.header.Component;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.message.*;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.err;

public class CloudBus3ManagementNodeLifeCycleTracker implements BeforeSendMessageInterceptor, BeforeDeliveryMessageInterceptor,
        ManagementNodeChangeListener, Component {

    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private AsyncTimer timeoutCleanup;

    public void startCleanupTimer() {
        if (timeoutCleanup != null) {
            timeoutCleanup.cancel();
        }

        timeoutCleanup = new AsyncTimer(TimeUnit.SECONDS, CloudBusGlobalProperty.CLOUDBUS3_MESSAGE_TRACKER_CLEANUP_INTERVAL) {
            protected TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            protected long getPeriod() {
                return CloudBusGlobalProperty.CLOUDBUS3_MESSAGE_TRACKER_CLEANUP_INTERVAL;
            }

            @Override
            protected void execute() {
                Iterator<Map.Entry<String, MessageTracker>> it = messageTrackers.entrySet().iterator();

                long now = System.currentTimeMillis();
                while (it.hasNext()) {
                    MessageTracker t = it.next().getValue();
                    if (t.isTimeout(now)) {
                        it.remove();
                    }
                }

                continueToRunThisTimer();
            }
        };

        timeoutCleanup.start();
    }

    @Override
    public int orderOfBeforeDeliveryMessageInterceptor() {
        return 0;
    }

    @Override
    public void beforeDeliveryMessage(Message msg) {
        if (msg instanceof MessageReply) {
            MessageReply reply = (MessageReply) msg;
            messageTrackers.remove(reply.getCorrelationId());
        }
    }

    private class MessageTracker {
        Message message;
        String targetManagementNodeUUID;

        public MessageTracker(Message message) {
            this.message = message;
            targetManagementNodeUUID = CloudBusImpl3.getManagementNodeUUIDFromServiceID(message.getServiceId());
        }

        boolean isTimeout(long now) {
            if (message instanceof NeedReplyMessage) {
                NeedReplyMessage msg = (NeedReplyMessage) message;
                return now >= msg.getTimeout() + msg.getCreatedTime();
            }

            return false;
        }

        boolean nodeLeft(String nodeUUID) {
            if (!targetManagementNodeUUID.equals(nodeUUID)) {
                return false;
            }

            ErrorCode err = err(SysErrors.MANAGEMENT_NODE_UNAVAILABLE_ERROR,
                    "management node[uuid:%s] is unavailable", nodeUUID);

            if (message instanceof APISyncCallMessage) {
                APIReply reply = new APIReply();
                reply.setError(err);
                bus.reply(message, reply);
            } else if (message instanceof APIMessage) {
                APIMessage amsg = (APIMessage) message;
                APIEvent evt = new APIEvent(amsg.getId());
                evt.setError(err);
                bus.publish(evt);
            } else if (message instanceof NeedReplyMessage) {
                MessageReply reply = new MessageReply();
                reply.setError(err);
                bus.reply(message, reply);
            } else {
                throw new CloudRuntimeException(String.format("should not be here, %s", JSONObjectUtil.toJsonString(message)));
            }

            return true;
        }
    }

    private ConcurrentHashMap<String, MessageTracker> messageTrackers = new ConcurrentHashMap<>();

    @Override
    public int orderOfBeforeSendMessageInterceptor() {
        return 0;
    }

    @Override
    public void beforeSendMessage(Message msg) {
        boolean needReply = true;
        if (Objects.equals(msg.getHeaderEntry(CloudBusImpl3.HEADER_NO_NEED_REPLY_MSG), Boolean.TRUE.toString())) {
            needReply = false;
        }

        if (msg instanceof NeedReplyMessage &&
                msg.getServiceId().contains(CloudBusImpl3.SERVICE_ID_SPLITTER)
                && needReply) {
            messageTrackers.put(msg.getId(), new MessageTracker(msg));
        }
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
    }

    private void callMessageTracker(ManagementNodeInventory inv) {
        Iterator<Map.Entry<String, MessageTracker>> it = messageTrackers.entrySet().iterator();
        while (it.hasNext()) {
            MessageTracker t = it.next().getValue();
            if (t.nodeLeft(inv.getUuid())) {
                it.remove();
            }
        }
    }

    @Override
    @ExceptionSafe
    public void nodeLeft(ManagementNodeInventory inv) {
        callMessageTracker(inv);
    }

    @Override
    @ExceptionSafe
    public void iAmDead(ManagementNodeInventory inv) {
        callMessageTracker(inv);
    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {
    }

    @Override
    public boolean start() {
        bus.installBeforeSendMessageInterceptor(this);
        bus.installBeforeDeliveryMessageInterceptor(this);
        bus.subscribeEvent(e -> {
            if (e instanceof APIEvent) {
                APIEvent aevt = (APIEvent) e;
                messageTrackers.remove(aevt.getApiId());
            }

            return false;
        }, new APIEvent());

        startCleanupTimer();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
