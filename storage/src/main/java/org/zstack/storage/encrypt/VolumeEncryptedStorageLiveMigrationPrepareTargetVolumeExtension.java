package org.zstack.storage.encrypt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.migration.KvmStorageLiveMigrationExtensionPoint;
import org.zstack.header.storage.primary.InstantiateTemporaryVolumeOnPrimaryStorageMsg;
import org.zstack.header.volume.CreateVolumeMsg;
import org.zstack.header.volume.VolumeAO;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostSyncHttpCallMsg;
import org.zstack.kvm.KVMHostSyncHttpCallReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class VolumeEncryptedStorageLiveMigrationPrepareTargetVolumeExtension implements KvmStorageLiveMigrationExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VolumeEncryptedStorageLiveMigrationPrepareTargetVolumeExtension.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private VolumeEncryptedResourceKeyBackend volumeEncryptedResourceKeyBackend;

    @Override
    public List<Flow> getAllocateTemporaryVolumeFlow(String vmUuid,
                                                     String srcHostUuid,
                                                     List<VolumeVO> volumesToMigrate,
                                                     Map<String, Long> sourceVolumeVirtualSizes) {
        return Collections.singletonList(new NoRollbackFlow() {
            String __name__ = "query-active-volume-size-for-encrypted-vm-" + vmUuid;

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<VolumeVO> encryptedVolumes = volumesToMigrate.stream()
                        .filter(VolumeVO::isEncrypted)
                        .collect(Collectors.toList());
                if (encryptedVolumes.isEmpty()) {
                    trigger.next();
                    return;
                }

                if (StringUtils.isBlank(srcHostUuid)) {
                    trigger.fail(operr("cannot query active volume size for encrypted vm[uuid:%s], source host uuid is empty",
                            vmUuid));
                    return;
                }

                KVMAgentCommands.GetActiveVolumeSizeCmd cmd = new KVMAgentCommands.GetActiveVolumeSizeCmd();
                cmd.setVmUuid(vmUuid);
                cmd.setInstallPaths(encryptedVolumes.stream().map(VolumeAO::getInstallPath).collect(Collectors.toList()));

                KVMHostSyncHttpCallMsg hmsg = new KVMHostSyncHttpCallMsg();
                hmsg.setCommand(cmd);
                hmsg.setHostUuid(srcHostUuid);
                hmsg.setPath(KVMConstant.KVM_GET_ACTIVE_VOLUME_SIZE_PATH);
                bus.makeTargetServiceIdByResourceUuid(hmsg, HostConstant.SERVICE_ID, srcHostUuid);
                bus.send(hmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        KVMHostSyncHttpCallReply r = reply.castReply();
                        KVMAgentCommands.GetActiveVolumeSizeRsp rsp =
                                r.toResponse(KVMAgentCommands.GetActiveVolumeSizeRsp.class);
                        if (!rsp.isSuccess()) {
                            trigger.fail(operr("failed to query active volume size for encrypted vm[uuid:%s] on host[uuid:%s], %s",
                                    vmUuid, srcHostUuid, rsp.getError()));
                            return;
                        }

                        Map<String, Long> volumeSizes = rsp.getVolumeSizes();
                        for (VolumeVO volume : encryptedVolumes) {
                            Long size = volumeSizes == null ? null : volumeSizes.get(volume.getInstallPath());
                            if (size == null || size <= 0) {
                                trigger.fail(operr("failed to query active volume[uuid:%s, path:%s] virtual size for encrypted vm[uuid:%s] on host[uuid:%s]",
                                        volume.getUuid(), volume.getInstallPath(), vmUuid, srcHostUuid));
                                return;
                            }

                            sourceVolumeVirtualSizes.put(volume.getInstallPath(), size);
                            logger.debug(String.format("use active volume[uuid:%s, path:%s] virtual size[%s] for live storage migration temporary volume",
                                    volume.getUuid(), volume.getInstallPath(), size));
                        }

                        trigger.next();
                    }
                });
            }
        });
    }

    @Override
    public ErrorCode preCreateTemporaryVolume(CreateVolumeMsg msg,
                                              VolumeVO sourceVolume,
                                              Map<String, Long> sourceVolumeVirtualSizes) {
        if (!sourceVolume.isEncrypted()) {
            return null;
        }

        msg.setEncrypted(true);
        msg.setSize(sourceVolumeVirtualSizes.getOrDefault(sourceVolume.getInstallPath(), sourceVolume.getSize()));
        return null;
    }

    @Override
    public ErrorCode afterCreateTemporaryVolume(VolumeVO sourceVolume, VolumeInventory temporaryVolume) {
        if (!sourceVolume.isEncrypted()) {
            return null;
        }

        try {
            volumeEncryptedResourceKeyBackend.copyVolumeKeyRefToVolume(sourceVolume.getUuid(), temporaryVolume.getUuid());
            temporaryVolume.setEncrypted(true);
            return null;
        } catch (Exception e) {
            return operr("failed to copy encryption key ref from volume[uuid:%s] to live migration temporary volume[uuid:%s]: %s",
                    sourceVolume.getUuid(), temporaryVolume.getUuid(), e.getMessage());
        }
    }

    @Override
    public ErrorCode beforeInstantiateTemporaryVolume(String hostUuid,
                                                      VolumeInventory temporaryVolume,
                                                      InstantiateTemporaryVolumeOnPrimaryStorageMsg msg) {
        return null;
    }
}
