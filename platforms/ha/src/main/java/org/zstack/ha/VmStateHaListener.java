package org.zstack.ha;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.ha.entity.VmHaVO;
import org.zstack.ha.entity.VmHaVO_;
import org.zstack.ha.header.VmHaLevel;
import org.zstack.header.Component;
import org.zstack.header.host.CheckVmStateOnHypervisorMsg;
import org.zstack.header.host.CheckVmStateOnHypervisorReply;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmCanonicalEvents;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.portal.managementnode.ManagementNodeGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class VmStateHaListener implements Component {
    private static final CLogger logger = Utils.getLogger(VmStateHaListener.class);

    @Autowired
    private EventFacade eventFacade;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private VmHaService vmHaService;

    // A dictionary (vmUuid -> targetHostUuid) of migration failed HA VMs
    final private Map<String, String> migrationFailedVMs = new ConcurrentHashMap<>();

    public void putMigrationFailedVm(String vmUuid, String targetHostUuid) {
        migrationFailedVMs.putIfAbsent(vmUuid, targetHostUuid);
    }

    @Override
    public boolean start() {
        setupCanonicalEvents();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void setupCanonicalEvents() {
        eventFacade.on(VmCanonicalEvents.VM_FULL_STATE_CHANGED_PATH, new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                VmCanonicalEvents.VmStateChangedData d = (VmCanonicalEvents.VmStateChangedData) data;
                if (!isVmManagedByUsForSure(d.getVmUuid())) {
                    return;
                }

                if (!HaHelper.isVmHaEnabled()) {
                    return;
                }

                VmHaVO ha = Q.New(VmHaVO.class)
                        .eq(VmHaVO_.uuid, d.getVmUuid())
                        .find();
                if (ha.getHaLevel().isDisabled() || null != ha.getInhibitionReason()) {
                    return;
                }

                VmHaLevel haLevel = ha.getHaLevel();

                logger.info(String.format("starting HA for VM[uuid:%s]", d.getVmUuid()));
                if (haLevel == VmHaLevel.OnHostFailure && VmInstanceState.Unknown.toString().equals(d.getNewState())) {
                    vmHaService.setupHaTrackerForOnHostFailureVm(d.getInventory().getUuid(), false);
                } else if (haLevel == VmHaLevel.NeverStop) {
                    if (VmInstanceState.Stopped.toString().equals(d.getNewState())) {
                        haOnStopVm(d.getInventory());
                    } else if (VmInstanceState.Unknown.toString().equals(d.getNewState())) {
                        vmHaService.setupHaTrackerForOnHostFailureVm(d.getInventory().getUuid(), true);
                    }
                } else {
                    if (VmInstanceState.Unknown.toString().equals(d.getNewState())) {
                        vmHaService.setupHaTrackerForOnHostFailureVm(d.getInventory().getUuid(), false);
                    }
                }
            }
        });
    }

    private boolean isVmManagedByUsForSure(String vmUuid) {
        if (destinationMaker.isManagedByUs(vmUuid)) {
            return true;
        }

        if (destinationMaker.getManagementNodeCount() > 1) {
            // we wait two ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL in case the VM is
            // managed by a node which is dying
            try {
                TimeUnit.SECONDS.sleep(2L * ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL.value(Integer.class));
            } catch (InterruptedException e) {
                logger.warn(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }
        return destinationMaker.isManagedByUs(vmUuid);
    }

    private void haOnStopVm(final VmInstanceInventory vmInv) {
        if (migrationFailedVMs.containsKey(vmInv.getUuid())) {
            handleMigrationFailure(vmInv, migrationFailedVMs.remove(vmInv.getUuid()));
            return;
        }

        final String vmUuid = vmInv.getUuid();
        final String hostUuid = vmInv.getHostUuid() != null ? vmInv.getHostUuid() : vmInv.getLastHostUuid();
        vmHaService.setupHaTrackerForNeverStopVm(vmUuid, hostUuid);
    }

    private void handleMigrationFailure(VmInstanceInventory vmInv, String hostUuid) {
        final String vmUuid = vmInv.getUuid();
        CheckVmStateOnHypervisorMsg msg = new CheckVmStateOnHypervisorMsg();
        msg.setVmInstanceUuids(Collections.singletonList(vmUuid));
        msg.setHostUuid(hostUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("cannot determine the VM[%s] status on host[%s]",
                            vmUuid, hostUuid));

                    // Set the host to migration destination, and HA manager will try to bring it up.
                    SQL.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.uuid, vmUuid)
                            .set(VmInstanceVO_.hostUuid, hostUuid)
                            .set(VmInstanceVO_.state, VmInstanceState.Unknown)
                            .update();
                    return;
                }

                CheckVmStateOnHypervisorReply r = reply.castReply();
                String state = r.getStates().get(vmUuid);
                if (state == null) {
                    logger.warn(String.format("VM[%s] not found on migration destination host[%s]",
                            vmUuid, hostUuid));

                    String reason = "vm is not found on migration destination host after migrate failing";
                    String hostUuid = vmInv.getHostUuid() == null ? vmInv.getLastHostUuid() : vmInv.getHostUuid();
                    vmHaService.haStartVmWithTracker(vmInv.getUuid(), hostUuid, reason, null);
                    return;
                }

                if (VmInstanceState.Running.toString().equals(state)) {
                    logger.warn(String.format("Migrated VM[%s] is running on host[%s]", vmUuid, hostUuid));

                    SQL.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.uuid, vmUuid)
                            .set(VmInstanceVO_.hostUuid, hostUuid)
                            .set(VmInstanceVO_.state, VmInstanceState.Running)
                            .update();
                    return;
                }

                if (VmInstanceState.Paused.toString().equals(state)) {
                    logger.warn(String.format("Migrated VM[%s] is paused on host[%s]", vmUuid, hostUuid));

                    SQL.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.uuid, vmUuid)
                            .set(VmInstanceVO_.hostUuid, hostUuid)
                            .set(VmInstanceVO_.state, VmInstanceState.Paused)
                            .update();
                    return;
                }

                if (VmInstanceState.Stopped.toString().equals(state)) {
                    logger.warn(String.format("Migrated VM[%s] is stopped on host[%s]", vmUuid, hostUuid));

                    // A stopped VM will be brought up by HA manager.
                    SQL.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.uuid, vmUuid)
                            .set(VmInstanceVO_.state, VmInstanceState.Stopped)
                            .update();
                }
            }
        });
    }
}
