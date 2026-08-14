package org.zstack.compute.vm;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.Component;
import org.zstack.header.core.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.mevoco.MevocoConstants;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.compute.vm.VmGlobalConfig.*;

public class KvmUserVmClockSyncExtension implements VmInstanceStartNewCreatedVmExtensionPoint,
        VmInstanceStartExtensionPoint, VmInstanceResumeExtensionPoint,
        VmInstanceMigrateExtensionPoint, HostAfterConnectedExtensionPoint, Component {
    private static final CLogger logger = Utils.getLogger(KvmUserVmClockSyncExtension.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceConfigFacade rcf;


    @Override
    public void afterHostConnected(HostInventory host) {
        sendUpdateSyncClockTaskMsg(host.getUuid());
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        if (inv.getHostUuid().equals(srcHostUuid)) {
            return;
        }
        sendUpdateSyncClockTaskMsg(inv.getHostUuid());
    }

    @Override
    public void afterStartVm(VmInstanceInventory inv) {
        sendUpdateSyncClockTaskMsg(inv.getHostUuid());
    }


    @Override
    public void afterStartNewCreatedVm(VmInstanceInventory inv) {
        sendUpdateSyncClockTaskMsg(inv.getHostUuid());
    }


    @Override
    public String preStartVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStartVm(VmInstanceInventory inv) {

    }

    @Override
    public void failedToStartVm(VmInstanceInventory inv, ErrorCode reason) {

    }

    @Override
    public String preStartNewCreatedVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStartNewCreatedVm(VmInstanceInventory inv) {

    }


    @Override
    public void failedToStartNewCreatedVm(VmInstanceInventory inv, ErrorCode reason) {

    }

    private static String findHostUuidByVmUuid(String vmUuid) {
        return Q.New(VmInstanceVO.class).select(VmInstanceVO_.hostUuid).eq(VmInstanceVO_.uuid, vmUuid).findValue();
    }

    private void vmSyncClock(String vmUuid) {
        sendUpdateSyncClockTaskMsg(findHostUuidByVmUuid(vmUuid));
    }

    private void sendUpdateSyncClockTaskMsg(String hostUuid) {
        sendUpdateSyncClockTaskMsg(hostUuid, buildNeedSyncVmMap(hostUuid), new Completion(null) {
            @Override
            public void success() {
                // do-nothing
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to set vm clock sync task on host[%s] because: %s", hostUuid, errorCode));
            }
        });
    }

    private void sendUpdateSyncClockTaskMsg(String hostUuid, Map<String, Integer> vmIntervalMap, Completion completion) {
        if (StringUtils.isEmpty(hostUuid)) {
            logger.debug("host uuid is empty, skip to set sync clock task for host");
            completion.success();
            return;
        }

        UpdateHostClockSyncVmMsg msg = new UpdateHostClockSyncVmMsg();
        msg.setHostUuid(hostUuid);
        msg.setVmIntervalMap(vmIntervalMap);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                    return;
                }
                completion.fail(reply.getError());
            }
        });
    }

    public static Map<String, Integer> buildNeedSyncVmMap(String hostUuid) {
        // NOTE: In the current version, the appliance VM (VPC) will not perform time synchronization
        List<String> vmUuidList = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid)
                .eq(VmInstanceVO_.type, VmInstanceConstant.USER_VM_TYPE)
                .eq(VmInstanceVO_.hostUuid, hostUuid)
                .listValues();

        if (vmUuidList.isEmpty()) {
            return new HashMap<>();
        }

        ResourceConfigFacade rcf = getComponentLoader().getComponent(ResourceConfigFacade.class);
        Map<String, Integer> uuidIntervalMap = vmUuidList.stream().collect(Collectors.toMap(
                Function.identity(),
                uuid -> rcf.getResourceConfig(VM_CLOCK_SYNC_INTERVAL_IN_SECONDS.getIdentity())
                        .getResourceConfigValue(uuid, Integer.class)));
        uuidIntervalMap.entrySet().removeIf(entry -> VmClockSyncConstant.DISABLE_VM_CLOCK_SYNC.equals(entry.getValue()));
        return uuidIntervalMap;
    }

    private void setupGlobalConfig() {
        installConfigUpdateExtension();
    }

    private void installConfigUpdateExtension() {
        ResourceConfig interval = rcf.getResourceConfig(VM_CLOCK_SYNC_INTERVAL_IN_SECONDS.getIdentity());
        interval.installDeleteExtension((config, resourceUuid, resourceType, old) -> vmSyncClock(resourceUuid));

        VM_CLOCK_SYNC_INTERVAL_IN_SECONDS.installUpdateExtension((old, newConfig) -> vmSyncClockForAllHost());
    }

    private void vmSyncClockForAllHost() {
        Long hostCount = Q.New(HostVO.class).eq(HostVO_.state, HostState.Enabled).eq(HostVO_.status, HostStatus.Connected).count();
        if (hostCount == 0) {
            return;
        }
        SQL.New("select host.uuid from HostVO host where host.state =:hostState and host.status =:hostStatus", String.class)
                .param("hostState", HostState.Enabled).param("hostStatus", HostStatus.Connected)
                .limit(100).paginate(hostCount, (List<String> hostUuids, PaginateCompletion paginateCompletion) -> 
                    new While<>(hostUuids).step(this::syncVmClockOnHostStep, 20)
                            .run(new WhileDoneCompletion(paginateCompletion) {
                                @Override
                                public void done(ErrorCodeList errorCodeList) {
                                    paginateCompletion.done();
                                }
                            }),
                new NopeNoErrorCompletion());
    }

    private void syncVmClockOnHostStep(String hostUuid, WhileCompletion completion) {
        sendUpdateSyncClockTaskMsg(hostUuid, buildNeedSyncVmMap(hostUuid), new Completion(completion) {
            @Override
            public void success() {
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to set vm clock sync task on host[%s] because: %s", hostUuid, errorCode));
                completion.done();
            }
        });
    }

    @Override
    public boolean start() {
        setupGlobalConfig();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void afterResumeVm(VmInstanceInventory inv) {
        boolean syncAfterResume = rcf.getResourceConfigValue(VM_CLOCK_SYNC_AFTER_VM_RESUME, inv.getUuid(), Boolean.class);
        if (!syncAfterResume) {
            return;
        }
        SyncVmClockMsg msg = new SyncVmClockMsg();
        msg.setUuid(inv.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, MevocoConstants.SERVICE_ID, msg.getVmInstanceUuid());
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.debug(String.format("failed to sync vm[%s] clock, because %s", inv.getUuid(), reply.getError()));
                }
            }
        });
    }
}
