package org.zstack.storage.migration;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.header.HasThreadContext;
import org.zstack.header.agent.AgentResponse;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.kvm.*;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class KvmBlockLiveMigrationWorkFlow implements KvmLiveMigrationWorkFlowTemplate {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    private static final CLogger logger = Utils.getLogger(KvmBlockLiveMigrationWorkFlow.class);

    public static final String KVM_BLOCK_LIVE_MIGRATION_PATH = "/vm/blklivemigration";
    public static final String STORAGE_MIGRATION_TYPE = "block_live_migration";

    public static class VmBlockLiveMigrationCmd implements HasThreadContext {
        private String vmUuid;
        private String oldVolumePath;
        private VolumeTO newVolume;
        private boolean reload;
        private long bandwidth;

        public boolean isReload() {
            return reload;
        }

        public void setReload(boolean reload) {
            this.reload = reload;
        }

        public String getOldVolumePath() {
            return oldVolumePath;
        }

        public void setOldVolumePath(String oldVolumePath) {
            this.oldVolumePath = oldVolumePath;
        }

        public VolumeTO getNewVolume() {
            return newVolume;
        }

        public void setNewVolume(VolumeTO newVolume) {
            this.newVolume = newVolume;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public long getBandwidth() {
            return bandwidth;
        }

        public void setBandwidth(Long bandwidth) {
            this.bandwidth = bandwidth;
        }
    }

    private boolean supportMigrateStorageOnly(ParamIn paramIn) {
        String clusterUuid = SQL.New("select h.clusterUuid from HostVO h where h.uuid = :huuid and h.clusterUuid in (" +
                "select ref.clusterUuid from PrimaryStorageClusterRefVO ref where ref.primaryStorageUuid = :psuuid)", String.class)
                .param("huuid", paramIn.getVmInstanceInventory().getHostUuid())
                .param("psuuid", paramIn.getMsg().getDstPrimaryStorageUuid()).find();

        return clusterUuid != null;
    }

    private VmBlockLiveMigrationCmd buildLiveMigrationCommand(VmInstanceInventory vmInstanceInventory, Map<String, VolumeTO> disks) {
        DebugUtils.Assert(disks != null && disks.size() == 1, "invaild disk map");
        VmBlockLiveMigrationCmd cmd = new VmBlockLiveMigrationCmd();
        cmd.setVmUuid(vmInstanceInventory.getUuid());
        disks.forEach((path, vol) -> {
            cmd.setOldVolumePath(path);
            cmd.setNewVolume(vol);
        });
        return cmd;
    }

    @Override
    public String getKvmStorageLiveMigrationType() {
        return STORAGE_MIGRATION_TYPE;
    }

    @Override
    public List<Flow> getPrepareFlows(ParamIn paramIn) {
        return Collections.emptyList();
    }

    @Override
    public List<Flow> getPreMigrateFlows(ParamIn paramIn) {
        return Collections.emptyList();
    }

    @Override
    public List<Flow> getMigrateFlows(ParamIn paramIn) {
        List<Flow> flows = new ArrayList<>();
        flows.add(new NoRollbackFlow() {
            String __name__ = String.format("kvm-block-live-migration-vm-%s-to-host", paramIn.getMsg().getVmInstanceUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                SQL.New(VolumeVO.class)
                        .in(VolumeVO_.uuid, paramIn.getVolumesToMigrate().stream().map(VolumeVO::getUuid).collect(Collectors.toList()))
                        .set(VolumeVO_.status, VolumeStatus.Migrating)
                        .update();

                KVMHostInventory inv = KVMHostInventory.valueOf(dbf.findByUuid(paramIn.getDstHostInventory().getUuid(), KVMHostVO.class));

                KVMHostAsyncHttpCallMsg hmsg = new KVMHostAsyncHttpCallMsg();
                Map migrateDisksMap = KvmStorageLiveMigrationFlowChain.getMigrateDiskTOList(inv, paramIn.getVolumeMappingDict(), paramIn.getVolumesToMigrate(), paramIn.getInitializedVolumes());
                VmBlockLiveMigrationCmd cmd = buildLiveMigrationCommand(paramIn.getVmInstanceInventory(), migrateDisksMap);
                cmd.setReload(paramIn.getJobContext().isMigrateStarted());
                cmd.setBandwidth(paramIn.getTotalBandwidth());

                hmsg.setCommand(cmd);
                hmsg.setPath(KVM_BLOCK_LIVE_MIGRATION_PATH);
                hmsg.setHostUuid(paramIn.getVmInstanceInventory().getHostUuid());
                bus.makeTargetServiceIdByResourceUuid(hmsg, HostConstant.SERVICE_ID, paramIn.getVmInstanceInventory().getHostUuid());
                bus.send(hmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        SQL.New(VolumeVO.class)
                                .in(VolumeVO_.uuid, paramIn.getVolumesToMigrate().stream().map(VolumeVO::getUuid).collect(Collectors.toList()))
                                .set(VolumeVO_.status, VolumeStatus.Ready)
                                .update();

                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        KVMHostAsyncHttpCallReply r = reply.castReply();
                        AgentResponse rsp = r.toResponse(AgentResponse.class);
                        if (!rsp.isSuccess()) {
                            trigger.fail(operr("vm block migrate failed: %s", rsp.getError()));
                            return;
                        }

                        paramIn.setMigrateSucceeded(true);
                        trigger.next();
                    }
                });
                paramIn.getJobContext().setMigrateStarted(true);
                LongJobContextUtil.saveContext(paramIn.getMsg().getLongJobUuid(), paramIn.getJobContext());
            }
        });
        return flows;
    }

    @Override
    public List<Flow> getAfterMigrateFlows(ParamIn paramIn) {
        return Collections.emptyList();
    }

    @Override
    public List<Flow> getCompleteFlows(ParamIn paramIn) {
        return Collections.emptyList();
    }

    @Override
    public ErrorCode preCheck(ParamIn paramIn) {
        return supportMigrateStorageOnly(paramIn) ? null : Platform.operr("target primary storage does not support migration for current host");
    }
}
