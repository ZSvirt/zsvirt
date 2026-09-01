package org.zstack.identity.imports.source;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.Q;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.identity.imports.AccountImportsConstant;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO_;
import org.zstack.identity.imports.message.SyncThirdPartyAccountMsg;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.zstack.identity.imports.AccountImportsGlobalConfig.*;

public class AccountSourceSyncingExtensions implements Component,
        CreateAccountSourceExtensionPoint,
        ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(AccountSourceSyncingExtensions.class);

    private Map<String, Future<Void>> autoSyncTaskMap = new ConcurrentHashMap<>();

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade threadFacade;
    @Autowired
    private ResourceConfigFacade resourceConfigFacade;
    @Autowired
    private ResourceDestinationMaker resourceDestinationMaker;

    @Override
    public boolean start() {
        startAutoSyncTask();
        installGlobalConfigValidator();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void startAutoSyncTask() {
        if (!autoSyncTaskMap.isEmpty()) {
            autoSyncTaskMap.values().forEach(t -> t.cancel(true));
        }

        List<String> sourceUuids = Q.New(ThirdPartyAccountSourceVO.class)
                .select(ThirdPartyAccountSourceVO_.uuid)
                .listValues();

        for (String sourceUuid : sourceUuids) {
            updateAutoSyncTask(sourceUuid);
        }
    }

    private synchronized void updateAutoSyncTask(String sourceUuid) {
        Future<Void> task = autoSyncTaskMap.get(sourceUuid);
        if (task != null) {
            task.cancel(true);
        }

        autoSyncTaskMap.put(sourceUuid, createSyncingTask(sourceUuid));
    }

    private synchronized void removeAutoSyncTask(String sourceUuid) {
        Future<Void> task = autoSyncTaskMap.get(sourceUuid);
        if (task != null) {
            task.cancel(true);
        }

        autoSyncTaskMap.remove(sourceUuid);
    }

    private void installGlobalConfigValidator() {
        AUTO_SYNC_INTERVAL_SECONDS.installUpdateExtension((oldConfig, newConfig) -> startAutoSyncTask());

        ResourceConfig intervalResourceConfig = resourceConfigFacade.getResourceConfig(
                AUTO_SYNC_INTERVAL_SECONDS.getIdentity());
        intervalResourceConfig.installUpdateExtension((config, resourceUuid, resourceType, oldValue, newValue) ->
                updateAutoSyncTask(resourceUuid));
        intervalResourceConfig.installDeleteExtension((config, resourceUuid, resourceType, value) ->
                updateAutoSyncTask(resourceUuid));
    }

    private Future<Void> createSyncingTask(String sourceUuid) {
        return threadFacade.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return accountSourceAutoSyncIntervalInSeconds(sourceUuid);
            }

            @Override
            public String getName() {
                return String.format("account-source-%s-auto-sync-task", sourceUuid);
            }

            @Override
            public void run() {
                boolean sourceExists = Q.New(ThirdPartyAccountSourceVO.class)
                        .eq(ThirdPartyAccountSourceVO_.uuid, sourceUuid)
                        .isExists();
                if (!sourceExists) {
                    removeAutoSyncTask(sourceUuid);
                    return;
                }

                boolean manageByMe = resourceDestinationMaker.isManagedByUs(sourceUuid);
                boolean syncEnabled = resourceConfigFacade.getResourceConfigValue(AUTO_SYNC_ENABLE, sourceUuid, Boolean.class);
                if (!manageByMe || !syncEnabled) {
                    return;
                }

                sendSyncAccountMessage(sourceUuid);
            }
        });
    }

    protected long accountSourceAutoSyncIntervalInSeconds(String sourceUuid) {
        return resourceConfigFacade.getResourceConfigValue(AUTO_SYNC_INTERVAL_SECONDS, sourceUuid, Long.class);
    }

    private void sendSyncAccountMessage(String sourceUuid) {
        SyncThirdPartyAccountMsg msg = new SyncThirdPartyAccountMsg();
        msg.setSourceUuid(sourceUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, AccountImportsConstant.SERVICE_ID, msg.getSourceUuid());
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.debug(String.format("Failed to sync third party account source[uuid:%s]", sourceUuid));
                }
            }
        });
    }

    @Override
    public void afterCreatingAccountSource(ThirdPartyAccountSourceVO source) {
        updateAutoSyncTask(source.getUuid());
    }

    @Override
    public void managementNodeReady() {
        List<String> sourceUuids = Q.New(ThirdPartyAccountSourceVO.class)
                .select(ThirdPartyAccountSourceVO_.uuid)
                .listValues();

        for (String sourceUuid : sourceUuids) {
            boolean needSyncWhenMNStart = resourceConfigFacade.getResourceConfigValue(
                SYNC_ACCOUNTS_ON_START, sourceUuid, Boolean.class);

            if (needSyncWhenMNStart) {
                sendSyncAccountMessage(sourceUuid);
            }
        }
    }
}
