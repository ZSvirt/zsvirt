package org.zstack.upgrade;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmCanonicalEvents;
import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.appliancevm.ApplianceVmStatus;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.APIUpdateGlobalConfigMsg;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.upgrade.UpgradeGlobalConfig;
import org.zstack.header.agent.versioncontrol.AgentVersionVO;
import org.zstack.header.agent.versioncontrol.AgentVersionVO_;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.*;
import org.zstack.header.license.LicenseMessage;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO_;
import org.zstack.network.service.virtualrouter.vyos.VyosVersionCheckResult;
import org.zstack.network.service.virtualrouter.vyos.VyosVersionManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class UpgradeManagerImpl implements ManagementNodeReadyExtensionPoint, GlobalApiMessageInterceptor, PingHostFailedExtensionPoint {
    private static final CLogger logger = Utils.getLogger(UpgradeManagerImpl.class);

    @Autowired
    private VyosVersionManager vyosVersionManager;

    @Autowired
    private CloudBus bus;

    @Autowired
    private ThreadFacade thdf;

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    protected EventFacade evtf;

    @Autowired
    private ResourceDestinationMaker destinationMaker;

    @Autowired
    protected PluginRegistry pluginRgty;

    private Future vyosRouterVersionCheckTask;
    private boolean vyosRouterVersionChecking = false;

    public void installUpdateExtension() {
        UpgradeGlobalConfig.GRAYSCALE_UPGRADE.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                if (!newConfig.value(Boolean.class)) {
                    closeGrayscaleUpgrade();
                }

                if (newConfig.value(Boolean.class)) {
                    startVyosRouterVersionCheckTask();
                }
            }
        });
    }

    public void closeGrayscaleUpgrade() {
        if (vyosRouterVersionCheckTask != null) {
            vyosRouterVersionCheckTask.cancel(true);
        }

        logger.info("close grayscale upgrade, clean AgentVersionVO");
        SQL.New(AgentVersionVO.class).hardDelete();
    }

    @Override
    public void managementNodeReady() {
        installUpdateExtension();

        startVyosRouterVersionCheckTask();
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {

        boolean openGrayscaleUpgrade = UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class);

        if (!openGrayscaleUpgrade) {
            return msg;
        }

        interceptOperationAPI(msg);
        return msg;
    }

    private void interceptOperationAPI(APIMessage msg) {

        if (msg instanceof APISyncCallMessage) {
            return;
        }
        if (msg instanceof LicenseMessage) {
            return;
        }
        if (msg instanceof APIUpdateGlobalConfigMsg) {
            return;
        }

        String apiName = msg.getClass().getSimpleName();
        if (apiName.contains("APIReconnect")) {
            return;
        }

        String allowedApiStr = UpgradeGlobalConfig.ALLOWED_API_LIST_GRAYSCALE_UPGRADING.value(String.class);
        List<String> allowedApiLists = Arrays.asList(allowedApiStr.split(","));
        if (allowedApiLists.contains(apiName)) {
            return;
        }

        throw new ApiMessageInterceptionException(operr("Operation [%s] is forbidden during grayscale upgrade", msg.getClass().getName()));
    }

    private void changeApplianceVmStatus(String vrUuid, ApplianceVmStatus newStatus) {
        VirtualRouterVmVO vo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (newStatus == vo.getStatus()) {
            return;
        }

        ApplianceVmStatus oldStatus = vo.getStatus();
        logger.debug(String.format("ApplianceVm [%s] changed status, old status: [%s], new status: [%s]", vo.getUuid(), vo.getStatus(), newStatus.toString()));
        vo.setStatus(newStatus);
        vo = dbf.updateAndRefresh(vo);
        ApplianceVmCanonicalEvents.ApplianceVmStatusChangedData d = new ApplianceVmCanonicalEvents.ApplianceVmStatusChangedData();
        d.setApplianceVmUuid(vo.getUuid());
        d.setApplianceVmType(vo.getApplianceVmType());
        d.setOldStatus(oldStatus.toString());
        d.setNewStatus(newStatus.toString());
        d.setInv(ApplianceVmInventory.valueOf(vo));
        evtf.fire(ApplianceVmCanonicalEvents.APPLIANCEVM_STATUS_CHANGED_PATH, d);

        logger.debug(String.format("the virtual router [uuid:%s, name:%s] changed status from %s to %s",
                vo.getUuid(), vo.getName(), oldStatus.toString(), newStatus.toString()));
    }

    private synchronized void startVyosRouterVersionCheckTask() {
        if (!UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)) {
            return;
        }

        if (vyosRouterVersionCheckTask != null) {
            vyosRouterVersionCheckTask.cancel(true);
        }

        vyosRouterVersionCheckTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public void run() {
                if (vyosRouterVersionChecking) {
                    logger.debug("last version checking is in progress. Skip version checking");
                    return;
                }

                vyosRouterVersionChecking = true;

                try {
                    checkVyosRouterVersion(new Completion(null) {
                        @Override
                        public void success() {
                            vyosRouterVersionChecking = false;
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            vyosRouterVersionChecking = false;
                        }
                    });
                } catch (Throwable t) {
                    vyosRouterVersionChecking = false;
                    logger.debug(String.format("version check task failed, cause [%s]", t));
                }
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                if (CoreGlobalProperty.UNIT_TEST_ON) {
                    return 3;
                }
                return 10;
            }

            @Override
            public String getName() {
                return "check-vyos-router-version";
            }
        });
    }

    private void checkVyosRouterVersion(Completion callback) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            callback.success();
            return;
        }

        List<String> vrUuids = Q.New(VirtualRouterVmVO.class)
                .select(VirtualRouterVmVO_.uuid)
                .eq(VirtualRouterVmVO_.status, ApplianceVmStatus.Connected)
                .listValues();

        vrUuids = vrUuids.stream().filter(vrUuid ->
            destinationMaker.isManagedByUs(vrUuid)
        ).collect(Collectors.toList());

        if (vrUuids.isEmpty()) {
            callback.success();
            return;
        }

        logger.trace(String.format("Check the version of the following vyos [%s]", vrUuids));
        new While<>(vrUuids).step((vrUuid, whileComp) -> {
            vyosVersionManager.vyosRouterVersionCheck(vrUuid, new ReturnValueCompletion<VyosVersionCheckResult>(whileComp) {
                @Override
                public void success(VyosVersionCheckResult returnValue) {
                    if (!returnValue.isNeedReconnect()) {
                        whileComp.done();
                        return;
                    }

                    VirtualRouterVmVO vo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
                    if (vo.getStatus() != ApplianceVmStatus.Connected) {
                        whileComp.done();
                        return;
                    }

                    logger.debug(String.format("the vyos [%s] version is different from mn version, change status to disconnected", vrUuid));
                    changeApplianceVmStatus(vrUuid, ApplianceVmStatus.Disconnected);
                    whileComp.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.debug(String.format("check vyos [%s] version failed, cause [%s]", vrUuid, errorCode));
                    whileComp.done();
                }
            });
        }, 5).run(new WhileDoneCompletion(callback) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                callback.success();
            }
        });
    }

    @Override
    public void afterPingHostFailed(String hostUuid, ErrorCode reason) {
        boolean isGrayscaleUpgrade = UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class);
        if (!isGrayscaleUpgrade) {
            return;
        }

        handlingUnupgradedHostPingFailures(hostUuid, reason);
    }

    private void handlingUnupgradedHostPingFailures(String hostUuid, ErrorCode reason) {
        boolean hostUpgradeed = Q.New(AgentVersionVO.class)
                .eq(AgentVersionVO_.uuid, hostUuid)
                .isExists();
        if (hostUpgradeed) {
            return;
        }

        HostVO hostVO = dbf.findByUuid(hostUuid, HostVO.class);
        if (hostVO == null) {
            return;
        }

        CollectionUtils.safeForEach(pluginRgty.getExtensionList(AfterChangeHostStatusExtensionPoint.class),
                new ForEachFunction<AfterChangeHostStatusExtensionPoint>() {
                    @Override
                    public void run(AfterChangeHostStatusExtensionPoint arg) {
                        arg.afterChangeHostStatus(hostUuid, hostVO.getStatus(), HostStatus.Disconnected);
                    }
                }
        );
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return null;
        }

        boolean openGrayscaleUpgrade = UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class);

        if (openGrayscaleUpgrade) {
            return null;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }
}
