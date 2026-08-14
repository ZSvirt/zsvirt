package org.zstack.core.cloudbus;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import org.zstack.core.Platform;
import org.zstack.header.Component;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DeadMessageManagerImpl implements DeadMessageManager, ManagementNodeChangeListener, Component {
    private static final CLogger logger = Utils.getLogger(DeadMessageManager.class);

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
        dealWithManagementNodeFoundErrorMessages(inv);
    }

    private synchronized void dealWithManagementNodeFoundErrorMessages(ManagementNodeInventory inv) {
        managementNodeNotFoundHandlers.asMap().forEach((k, handler) -> {
            if (inv.getUuid().equals(handler.managementNodeUuid)) {
                handler.handle();
                managementNodeNotFoundHandlers.invalidate(k);
            }
        });
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        previousOfflinedManagementNodes.put(inv.getUuid(), inv);
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {
    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {
        previousOfflinedManagementNodes.invalidate(inv.getUuid());
    }

    @Override
    public boolean start() {
        buildCache();

        CloudBusGlobalConfig.MAX_MANAGEMENTNODE_NOTFOUND_ERROR_HANDLER_NUM.installUpdateExtension((oldConfig, newValue) -> buildCache());
        CloudBusGlobalConfig.MAX_MANAGEMENTNODE_NOTFOUND_ERROR_HANDLER_TIMEOUT.installUpdateExtension((o, n) -> buildCache());

        return true;
    }

    private synchronized void buildCache() {
        Map<String, ManagementNodeNotFoundHandler> oldEntries = null;
        if (managementNodeNotFoundHandlers != null) {
            oldEntries = managementNodeNotFoundHandlers.asMap();
        }

        long maxNum = CloudBusGlobalConfig.MAX_MANAGEMENTNODE_NOTFOUND_ERROR_HANDLER_NUM.value(Long.class);
        long timeout = CloudBusGlobalConfig.MAX_MANAGEMENTNODE_NOTFOUND_ERROR_HANDLER_TIMEOUT.value(Long.class);

        managementNodeNotFoundHandlers = CacheBuilder.newBuilder()
                .maximumSize(maxNum)
                .expireAfterWrite(timeout, TimeUnit.SECONDS)
                .removalListener((RemovalListener<String, ManagementNodeNotFoundHandler>) removalNotification -> {
                    if (removalNotification.getCause() == RemovalCause.SIZE || removalNotification.getCause() == RemovalCause.EXPIRED || removalNotification.getCause() == RemovalCause.COLLECTED) {
                        ManagementNodeNotFoundHandler handler = removalNotification.getValue();
                        logger.warn(String.format("A message failing to send to the management node[uuid:%s] because the node is offline while the message being sent. Now the message is being dropped " +
                                        "because the cache policy[%s] requires and the management node is not online til now. The message dump:\n %s", handler.managementNodeUuid,
                                removalNotification.getCause(), CloudBusGson.toJson(handler.message)));
                    }
                }).build();


        if (oldEntries != null) {
            oldEntries.forEach((k, v) -> managementNodeNotFoundHandlers.put(k, v));
        }

        logger.debug(String.format("build cache of ManagementNodeNotFoundHandler[maxNum:%s, timeout: %ss, current entries: %s]", maxNum, timeout, managementNodeNotFoundHandlers.size()));
    }

    @Override
    public boolean stop() {
        return true;
    }

    private class ManagementNodeNotFoundHandler {
        Runnable resendFunc;
        Message message;
        String managementNodeUuid;

        @ExceptionSafe
        void handle() {
            resendFunc.run();
            logger.debug(String.format("successfully resend the message[id:%s] to a previous-offline management node[uuid:%s]", message.getId(), managementNodeUuid));
        }
    }

    // the cache key is meaningless, ignore it
    private Cache<String, ManagementNodeNotFoundHandler> managementNodeNotFoundHandlers;
    private Cache<String, ManagementNodeInventory> previousOfflinedManagementNodes = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    @Override
    public synchronized boolean handleManagementNodeNotFoundError(String managementNodeUuid, Message message, Runnable rsendFunc) {
        assert rsendFunc != null : "rsendFunc cannot be null";

        if (previousOfflinedManagementNodes.getIfPresent(managementNodeUuid) == null) {
            // the node never gets online, no need to handle this message
            return false;
        }

        logger.debug(String.format("save message[uuid:%s] to offline-node[uuid:%s] for later delivery, managementNodeNotFoundHandlers has %s entries. %s", message.getId(), managementNodeUuid,
                managementNodeNotFoundHandlers.size(), CloudBusGson.toJson(message)));
        ManagementNodeNotFoundHandler handler = new ManagementNodeNotFoundHandler();
        handler.managementNodeUuid = managementNodeUuid;
        handler.message = message;
        handler.resendFunc = rsendFunc;
        managementNodeNotFoundHandlers.put(Platform.getUuid(), handler);
        return true;
    }
}
