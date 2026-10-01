package org.zstack.ha;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.BeforeStopVmOnHypervisorExtensionPoint;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.ha.entity.VmHaVO;
import org.zstack.ha.entity.VmHaVO_;
import org.zstack.ha.header.HaGlobalConfig;
import org.zstack.ha.header.VmHaLevel;
import org.zstack.ha.spi.VmFencerRuleExtensionPoint;
import org.zstack.ha.spi.VmHaVOUpdater;
import org.zstack.header.cluster.ClusterUpdateOSExtensionPoint;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostErrors;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.OrderVmBeforeMigrationDuringHostMaintenanceExtensionPoint;
import org.zstack.header.message.Message;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.vm.APIRebootVmInstanceMsg;
import org.zstack.header.vm.APIStopVmInstanceMsg;
import org.zstack.header.vm.ApplianceVmInstanceCreateExtensionPoint;
import org.zstack.header.vm.BeforeStartNewCreatedVmExtensionPoint;
import org.zstack.header.vm.CreateVmInstanceMsg;
import org.zstack.header.vm.KvmReportVmShutdownEventExtensionPoint;
import org.zstack.header.vm.RebootVmInstanceMsg;
import org.zstack.header.vm.StopVmInstanceMsg;
import org.zstack.header.vm.StopVmOnHypervisorMsg;
import org.zstack.header.vm.VmCreationStrategy;
import org.zstack.header.vm.VmHaExtensionPoint;
import org.zstack.header.vm.VmInstanceCreateExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceStartExtensionPoint;
import org.zstack.header.vm.VmInstanceStartNewCreatedVmExtensionPoint;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceStopExtensionPoint;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMDestroyVmExtensionPoint;
import org.zstack.kvm.KVMException;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.argerr;
import static org.zstack.ha.header.HaGlobalConfig.VM_HA_LEVEL;

public class HaExtensions implements
        VmInstanceStartExtensionPoint
        , VmInstanceStartNewCreatedVmExtensionPoint
        , VmInstanceStopExtensionPoint
        , KVMDestroyVmExtensionPoint
        , BeforeStartNewCreatedVmExtensionPoint
        , BeforeStopVmOnHypervisorExtensionPoint
        , KvmReportVmShutdownEventExtensionPoint
        , VmInstanceCreateExtensionPoint
        , ApplianceVmInstanceCreateExtensionPoint
        , VmInstanceMigrateExtensionPoint
        , VmHaExtensionPoint
        , OrderVmBeforeMigrationDuringHostMaintenanceExtensionPoint
        , ClusterUpdateOSExtensionPoint
{

    @Autowired
    private PluginRegistry pluginRegistry;
    @Autowired
    private ResourceConfigFacade resourceConfigFacade;
    @Autowired
    private VmHaVOUpdater vmHAExecutor;
    @Autowired
    private VmHaService vmHaService;
    @Autowired
    private TimeHelper timeHelper;
    @Autowired
    private DatabaseFacade dbf;

    private final Map<String, Boolean> haStatus = new HashMap<>();

    // ------------------------------------- VmInstanceStartExtensionPoint ------------------------------------

    @Override
    public void beforeStartVm(VmInstanceInventory inv) {}

    @Override
    public void afterStartVm(VmInstanceInventory inv) {
        vmHaService.syncHaVmListOnHost(inv.getHostUuid(), new NopeCompletion());
        vmHAExecutor.fenceForVM(inv.getUuid())
                .clear()
                .update();
        vmHAExecutor.setHALevelForVM(inv.getUuid())
                .clearInhibitHABlocking("After starting VM")
                .update();

        boolean haEnabled = Q.New(VmHaVO.class)
                .eq(VmHaVO_.uuid, inv.getUuid())
                .eq(VmHaVO_.haLevel, VmHaLevel.NeverStop)
                .isExists();
        if (!haEnabled) {
            return;
        }

        // <- addVmFencerRuleToHost
        CollectionUtils.safeForEach(pluginRegistry.getExtensionList(VmFencerRuleExtensionPoint.class),
                extension -> extension.addVmFencerRuleToHost(Collections.singletonList(inv.getUuid()), inv.getHostUuid()));
    }

    @Override
    public void failedToStartVm(VmInstanceInventory inv, ErrorCode reason) {
        vmHAExecutor.fenceForVM(inv.getUuid())
                .clear()
                .update();
    }

    // ------------------------------------- VmInstanceStartNewCreatedVmExtensionPoint ------------------------------------

    @Override
    public String preStartNewCreatedVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStartNewCreatedVm(VmInstanceInventory inv) {}

    @Override
    public void afterStartNewCreatedVm(VmInstanceInventory inv) {
        vmHaService.syncHaVmListOnHost(inv.getHostUuid(), new NopeCompletion());
        updateHaLevelByClusterConfig(inv);

        // <- addVmFencerRuleToHost
        CollectionUtils.safeForEach(pluginRegistry.getExtensionList(VmFencerRuleExtensionPoint.class),
                extension -> extension.addVmFencerRuleToHost(Collections.singletonList(inv.getUuid()), inv.getHostUuid()));
    }

    @Override
    public void failedToStartNewCreatedVm(VmInstanceInventory inv, ErrorCode reason) {}

    private void updateHaLevelByClusterConfig(VmInstanceInventory inventory) {
        boolean haUndefined = Q.New(VmHaVO.class)
                .eq(VmHaVO_.uuid, inventory.getUuid())
                .eq(VmHaVO_.haLevel, VmHaLevel.Undefined)
                .isExists();
        if (!haUndefined) {
            return;
        }

        String clusterHAConfig = resourceConfigFacade.getResourceConfigValue(VM_HA_LEVEL,
                inventory.getClusterUuid(), String.class);
        boolean haEnabled = !VmHaLevel.None.toString().equals(clusterHAConfig);

        if (haEnabled) {
            vmHAExecutor.setHALevelForVM(inventory.getUuid())
                    .toNeverStop()
                    .update();
        } else {
            vmHAExecutor.setHALevelForVM(inventory.getUuid())
                    .toDisabled()
                    .update();
        }
    }

    // ------------------------------------- VmInstanceStopExtensionPoint ------------------------------------

    @Override
    public String preStopVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStopVm(VmInstanceInventory inv) {
        CollectionUtils.safeForEach(pluginRegistry.getExtensionList(VmFencerRuleExtensionPoint.class),
                extension -> extension.removeVmFencerRuleFromHost(inv, inv.getHostUuid()));
    }

    @Override
    public void afterStopVm(VmInstanceInventory inv) {
        vmHaService.syncHaVmListOnHost(inv.getLastHostUuid(), new NopeCompletion());
    }

    @Override
    public void failedToStopVm(VmInstanceInventory inv, ErrorCode reason) {
        final String realHostUuid = inv.getHostUuid() == null ? inv.getLastHostUuid() : inv.getHostUuid();

        // <- addVmFencerRuleToHost
        CollectionUtils.safeForEach(pluginRegistry.getExtensionList(VmFencerRuleExtensionPoint.class),
                extension -> extension.addVmFencerRuleToHost(Collections.singletonList(inv.getUuid()), realHostUuid));

        boolean vmRunning = VmInstanceState.Running.toString().equals(inv.getState());
        if (!vmRunning) {
            return;
        }

        boolean errorMatched = !HostErrors.OPERATION_FAILURE_GC_ELIGIBLE.isEqual(reason.getCode());
        if (errorMatched) {
            vmHAExecutor.setHALevelForVM(inv.getUuid())
                    .clearInhibitHABlocking("Failed to stop VM")
                    .update();
        }
    }

    // ------------------------------------- KVMDestroyVmExtensionPoint ------------------------------------

    @Override
    public void beforeDestroyVmOnKvm(KVMHostInventory host, VmInstanceInventory vm, KVMAgentCommands.DestroyVmCmd cmd) throws KVMException {
        CollectionUtils.safeForEach(pluginRegistry.getExtensionList(VmFencerRuleExtensionPoint.class),
                extension -> extension.removeVmFencerRuleFromHost(vm, host.getUuid()));

        vmHAExecutor.setHALevelForVM(vm.getUuid())
                .inhibitHATemporarily("Before destroying VM")
                .update();
    }

    @Override
    public void beforeDirectlyDestroyVmOnKvm(KVMAgentCommands.DestroyVmCmd cmd) {}

    @Override
    public void destroyVmOnKvmSuccess(KVMHostInventory host, VmInstanceInventory vm) {}

    @Override
    public void destroyVmOnKvmFailed(KVMHostInventory host, VmInstanceInventory vm, ErrorCode err) {
        // <- addVmFencerRuleToHost
        CollectionUtils.safeForEach(pluginRegistry.getExtensionList(VmFencerRuleExtensionPoint.class),
                extension -> extension.addVmFencerRuleToHost(Collections.singletonList(vm.getUuid()), host.getUuid()));
    }

    // ------------------------------------- BeforeStartNewCreatedVmExtensionPoint ------------------------------------

    @Override
    public void beforeStartNewCreatedVm(VmInstanceSpec spec) {
        final String vmUuid = spec.getVmInventory().getUuid();
        final boolean createdStopped = spec.getStrategy() == VmCreationStrategy.CreateStopped;

        if (createdStopped) {
            vmHAExecutor.setHALevelForVM(vmUuid)
                    .inhibitHATemporarily("Create VM with CreateStopped strategy")
                    .update();
        }
    }

    // ------------------------------------- BeforeStopVmOnHypervisorExtensionPoint ------------------------------------

    private static class StopVmItem {
        final String vmUuid;
        final long time;
        public StopVmItem(String vmUuid, long time) {
            this.vmUuid = vmUuid;
            this.time = time;
        }
    }
    private final Object stopVmItemLock = new Object();
    private final List<StopVmItem> apiStopVmCache = new ArrayList<>();

    @Override
    public void beforeStopVmOnHypervisor(VmInstanceSpec spec, StopVmOnHypervisorMsg msg) {
        final Message parentMsg = spec.getMessage();
        // Reboot's guest shutdown is queued behind the VM reboot task; keep the
        // uuid cached so the delayed event does not create a leftover inhibitHA tag.
        if (parentMsg instanceof APIStopVmInstanceMsg
                || parentMsg instanceof RebootVmInstanceMsg
                || parentMsg instanceof APIRebootVmInstanceMsg) {
            synchronized (stopVmItemLock) {
                apiStopVmCache.add(new StopVmItem(spec.getVmInventory().getUuid(), timeHelper.getCurrentTimeMillis()));
            }
        }

        boolean stopHa = false;
        if (parentMsg instanceof StopVmInstanceMsg) {
            stopHa = ((StopVmInstanceMsg) parentMsg).isStopHA();
        } else if (parentMsg instanceof APIStopVmInstanceMsg) {
            stopHa = Objects.equals(((APIStopVmInstanceMsg) parentMsg).getStopHA(), Boolean.TRUE.toString());
        }

        if (stopHa) {
            vmHAExecutor.setHALevelForVM(msg.getVmInventory().getUuid())
                    .inhibitHATemporarily("Before VM stopping on hypervisor")
                    .update();
        }
    }

    // ------------------------------------- KvmReportVmShutdownEventExtensionPoint ------------------------------------

    @Override
    public void kvmReportVmShutdownEvent(ShutdownDetail detail) {
        if (!detail.triggerByGuest) {
            return;
        }

        synchronized (stopVmItemLock) {
            long now = timeHelper.getCurrentTimeMillis();
            long invalidateTime = now - TimeUnit.MINUTES.toMillis(10);
            apiStopVmCache.removeIf(it -> it.time < invalidateTime);
            boolean anyMatch = apiStopVmCache.removeIf(it -> Objects.equals(it.vmUuid, detail.vmUuid));
            if (anyMatch) {
                return;
            }
        }

        vmHAExecutor.setHALevelForVM(detail.vmUuid)
                .inhibitHATemporarily("KVM reporting VM shutdown")
                .update();
    }

    // ------------------------------------- VmInstanceCreateExtensionPoint ------------------------------------

    @Override
    public void preCreateVmInstance(CreateVmInstanceMsg msg) {}

    @Override
    public void afterPersistVmInstanceVO(VmInstanceVO vo, CreateVmInstanceMsg msg) {
        persistUndefinedVmHa(vo);
    }

    @Override
    public void afterPersistApplianceVmInstanceVO(VmInstanceVO vo) {
        persistUndefinedVmHa(vo);
    }

    private void persistUndefinedVmHa(VmInstanceVO vo) {
        VmHaVO ha = new VmHaVO();
        ha.setUuid(vo.getUuid());
        ha.setHaLevel(VmHaLevel.Undefined);
        ha.setHaLevelUpdateTime(vo.getCreateDate());
        dbf.persist(ha);
    }

    // ------------------------------------- VmInstanceMigrateExtensionPoint ------------------------------------

    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        CollectionUtils.safeForEach(pluginRegistry.getExtensionList(VmFencerRuleExtensionPoint.class),
                extension -> extension.removeVmFencerRuleFromHost(inv, inv.getHostUuid()));
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        CollectionUtils.safeForEach(pluginRegistry.getExtensionList(VmFencerRuleExtensionPoint.class),
                extension -> extension.addVmFencerRuleToHost(Collections.singletonList(inv.getUuid()), inv.getHostUuid()));
        vmHaService.syncHaVmListOnHost(inv.getHostUuid(), new NopeCompletion());
        vmHaService.syncHaVmListOnHost(srcHostUuid, new NopeCompletion());
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
        if (destHostUuid == null) {
            return;
        }
        CollectionUtils.safeForEach(pluginRegistry.getExtensionList(VmFencerRuleExtensionPoint.class),
                extension -> extension.addVmFencerRuleToHost(Collections.singletonList(inv.getUuid()), inv.getHostUuid()));
    }

    // ------------------------------------- VmHaExtensionPoint ------------------------------------

    @Override
    public ErrorCode preHaStartVm(String vmUuid) {
        String sql = "select uuid from PrimaryStorageVO where uuid in (" +
                " select distinct(primaryStorageUuid) from VolumeVO" +
                " where vmInstanceUuid = :vmUuid and primaryStorageUuid is not null)" +
                " and state = :psState";
        List<String> result = SQL.New(sql, String.class)
                .param("vmUuid", vmUuid)
                .param("psState", PrimaryStorageState.Maintenance)
                .list();
        if (result != null && !result.isEmpty()) {
            return argerr("the VM[uuid:%s] volume stored location primary storage is in a state of maintenance", vmUuid);
        }
        return null;
    }

    // ------------------------------------- OrderVmBeforeMigrationDuringHostMaintenanceExtensionPoint ------------------------------------

    @Override
    public List<String> orderVmBeforeMigrationDuringHostMaintenance(HostInventory host, List<String> vmUuids) {
        List<VmHaVO> haList = Q.New(VmHaVO.class)
                .in(VmHaVO_.uuid, vmUuids)
                .in(VmHaVO_.haLevel, Arrays.asList(VmHaLevel.NeverStop, VmHaLevel.OnHostFailure))
                .list();
        if (haList.isEmpty()) {
            return vmUuids;
        }

        List<String> neverStopped = new ArrayList<>();
        List<String> onHostFailure = new ArrayList<>();
        List<String> haVmFirst = new ArrayList<>();

        for (VmHaVO ha : haList) {
            if (ha.getHaLevel() == VmHaLevel.NeverStop) {
                neverStopped.add(ha.getUuid());
            } else if (ha.getHaLevel() == VmHaLevel.OnHostFailure) {
                onHostFailure.add(ha.getUuid());
            }
        }

        haVmFirst.addAll(neverStopped);
        haVmFirst.addAll(onHostFailure);
        for (String uuid : vmUuids) {
            if (!haVmFirst.contains(uuid)) {
                haVmFirst.add(uuid);
            }
        }
        return haVmFirst;
    }

    // ------------------------------------- ClusterUpdateOSExtensionPoint ------------------------------------

    @Override
    public String preUpdateClusterOS(ClusterVO cls) {
        return null;
    }

    @Override
    public void beforeUpdateClusterOS(ClusterVO cls) {
        if (HaHelper.isVmHaEnabled()) {
            haStatus.put(cls.getUuid(), true);
            HaGlobalConfig.ALL.updateValue(false);
        } else {
            haStatus.put(cls.getUuid(), false);
        }
    }

    @Override
    public void afterUpdateClusterOS(ClusterVO cls) {
        if (haStatus.containsKey(cls.getUuid())) {
            Boolean isHaEnabled = haStatus.get(cls.getUuid());
            if (isHaEnabled) {
                HaGlobalConfig.ALL.updateValue(true);
            }
        }
    }
}
