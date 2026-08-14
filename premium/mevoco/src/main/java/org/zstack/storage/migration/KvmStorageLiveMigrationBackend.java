package org.zstack.storage.migration;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.CancelHostTasksMsg;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.KVMConstant;
import org.zstack.storage.migration.primary.PrimaryStorageCancelLiveMigrateVmMsg;
import org.zstack.storage.migration.primary.PrimaryStorageCancelLiveMigrateVmReply;
import org.zstack.storage.migration.primary.PrimaryStorageLiveMigrateVmMsg;
import org.zstack.storage.migration.primary.PrimaryStorageLiveMigrateVmReply;
import org.zstack.storage.primary.local.LocalStorageConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class KvmStorageLiveMigrationBackend implements HypervisorStorageLiveMigrationBackend, Component {
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public String getHypervisorType() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }

    @Override
    public void handle(PrimaryStorageLiveMigrateVmMsg msg) {
        VmInstanceVO vmInstanceVO = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);

        final List<VolumeVO> volumesToMigrate = getVolumesToMigrate(vmInstanceVO, msg);
        PrimaryStorageLiveMigrateVmReply reply = new PrimaryStorageLiveMigrateVmReply();
        if (volumesToMigrate.isEmpty()) {
            reply.setInventory(vmInstanceVO.toInventory());
            bus.reply(msg, reply);
            return;
        }

        HostVO hostVO = SQL.New("select h from HostVO h where h.uuid = (select v.hostUuid from VmInstanceVO v where v.uuid = :vmuuid)")
                .param("vmuuid", msg.getVmInstanceUuid()).find();

        KvmLiveMigrationWorkFlowTemplate.ParamIn paramIn = new KvmLiveMigrationWorkFlowTemplate.ParamIn();
        String type;
        if (hostVO.getUuid().equals(msg.getDstHostUuid())) {
            type = KvmBlockLiveMigrationWorkFlow.STORAGE_MIGRATION_TYPE;
            paramIn.setDstHostInventory(HostInventory.valueOf(hostVO));
        } else {
            type = KvmMigrateVmWithStorageWorkFlow.STORAGE_MIGRATION_TYPE;
        }

        KvmLiveMigrationWorkFlowTemplate template = pluginRgty.getExtensionFromMap(type, KvmLiveMigrationWorkFlowTemplate.class);
        paramIn.setMsg(msg);
        List<VolumeVO> volumesToKeep = vmInstanceVO.getAllDiskVolumes().stream().filter(vol -> !volumesToMigrate.contains(vol)).collect(Collectors.toList());
        paramIn.setVolumesToKeep(VolumeInventory.valueOf(volumesToKeep));
        paramIn.setVolumesToMigrate(volumesToMigrate);
        paramIn.setTemplate(template);
        paramIn.setVmInstanceInventory(VmInstanceInventory.valueOf(vmInstanceVO));
        paramIn.setJobContext(LongJobContextUtil.loadContext(msg.getLongJobUuid(), PrimaryStorageMigrateVmContext.class));
        paramIn.setTotalBandwidth(msg.getBandwidth());

        ErrorCode errorCode = template.preCheck(paramIn);
        if (errorCode != null) {
            reply.setError(errorCode);
            bus.reply(msg, reply);
            return;
        }

        AtomicInteger succeedVolumeCount = new AtomicInteger(0);
        if (KvmBlockLiveMigrationWorkFlow.STORAGE_MIGRATION_TYPE.equals(type)) {
            ArrayList<VolumeVO> volumeVOS = new ArrayList<>(paramIn.getVolumesToMigrate());
            new While<>(volumeVOS)
                    .enableProgressReport("kvm-storage-live-migrate")
                    .each((vol, compl) -> {
                KvmLiveMigrationWorkFlowTemplate.ParamIn param = paramIn.copy();
                param.setVolumesToMigrate(Collections.singletonList(vol));
                param.setJobContext(LongJobContextUtil.loadContext(msg.getLongJobUuid(), PrimaryStorageMigrateVmContext.class));
                KvmStorageLiveMigrationFlowChain.getFlow(param).run(new Completion(msg, compl) {
                    @Override
                    public void success() {
                        succeedVolumeCount.incrementAndGet();
                        compl.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        compl.addError(errorCode);
                        compl.allDone();
                    }
                });
            }).run(new WhileDoneCompletion(msg) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                    if (!errorCodeList.getCauses().isEmpty()) {
                        ErrorCode err = errorCodeList.getCauses().get(0);
                        List<String> volUuids = volumeVOS.stream().map(ResourceVO::getUuid).collect(Collectors.toList());
                        err.setDetails(String.format("%s, successfully migrated %d volumes[%s] and failed to migrate %d volumes[%s]", err.getDetails(),
                                succeedVolumeCount.get(), volUuids.subList(0, succeedVolumeCount.get()), volUuids.size() - succeedVolumeCount.get(), volUuids.subList(succeedVolumeCount.get(), volUuids.size())));
                        reply.setError(err);
                        bus.reply(msg, reply);
                        return;
                    }
                    VmInstanceInventory vm = VmInstanceInventory.valueOf(dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class));
                    reply.setInventory(vm);
                    bus.reply(msg, reply);
                }
            });
            return;
        }

        KvmStorageLiveMigrationFlowChain.getFlow(paramIn).run(new Completion(msg) {
            @Override
            public void success() {
                VmInstanceInventory vm = VmInstanceInventory.valueOf(dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class));
                reply.setInventory(vm);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    public void handle(PrimaryStorageCancelLiveMigrateVmMsg msg) {
        PrimaryStorageCancelLiveMigrateVmReply reply = new PrimaryStorageCancelLiveMigrateVmReply();
        CancelHostTasksMsg cmsg = new CancelHostTasksMsg();
        cmsg.setCancellationApiId(msg.getCancellationApiId());
        bus.makeLocalServiceId(cmsg, HostConstant.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    reply.setError(r.getError());
                }

                bus.reply(msg, reply);
            }
        });
    }

    private List<Long> getProgressWeight(List<VolumeVO> volumeVOS) {
        List<Long> res = new ArrayList<>();
        volumeVOS.forEach(vol -> res.add(vol.getSize()));
        return res;
    }

    private boolean requireMigration(VolumeVO vo, PrimaryStorageLiveMigrateVmMsg msg) {
        String srcPsType = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, vo.getPrimaryStorageUuid())
                .select(PrimaryStorageVO_.type).findValue();
        String dstPsType = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, msg.getDstPrimaryStorageUuid())
                .select(PrimaryStorageVO_.type).findValue();

        return pluginRgty.getExtensionList(VmStorageMigrationMetric.class).stream().anyMatch(ext ->
                ext.isCapable(srcPsType, dstPsType) && ext.needMigration(vo, msg.getDstPrimaryStorageUuid(), msg.getDstHostUuid(), msg.getSystemTags()));
    }

    private List<VolumeVO> getVolumesToMigrate(VmInstanceVO vmInstanceVO, PrimaryStorageLiveMigrateVmMsg msg) {
        List<VolumeVO> volumes = new ArrayList<>();
        if (!msg.isWithDataVolumes()) {
            volumes.add(vmInstanceVO.getRootVolume());
        } else {
            volumes.addAll(vmInstanceVO.getAllDiskVolumes());
        }

        List<VolumeVO> vols = volumes.stream().filter(vol -> requireMigration(vol, msg)).collect(Collectors.toList());

        return PrimaryStorageMigrationRecoverHelper.getVolumesToMigrateFromJobContext(msg, volumes, vols);
    }

    private boolean psIsLocal(String psUuid) {
        return Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.uuid, psUuid)
                .eq(PrimaryStorageVO_.type, LocalStorageConstants.LOCAL_STORAGE_TYPE)
                .isExists();
    }

    @Override
    public boolean start() {
        pluginRgty.saveExtensionAsMap(KvmLiveMigrationWorkFlowTemplate.class, KvmLiveMigrationWorkFlowTemplate::getKvmStorageLiveMigrationType);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
