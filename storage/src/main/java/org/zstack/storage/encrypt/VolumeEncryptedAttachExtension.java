package org.zstack.storage.encrypt;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.AttachDataVolumeCmd;
import org.zstack.kvm.KVMAttachVolumeExtensionPoint;
import org.zstack.kvm.KVMConvertVolumeExtensionPoint;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.VolumeTO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 * On hot-attach of a LUKS-encrypted data volume, ensure the per-volume libvirt
 * secret exists on the destination host and stamp its UUID onto the
 * {@link VolumeTO} so {@code vm_plugin.filebased_volume} (the attach builder
 * at {@code vm_plugin.py:3185}) can emit
 * {@code <encryption format='luks'><secret type='passphrase' uuid='...'/></encryption>}.
 *
 * <p>Without this the agent issues a {@code blockdev-add} without
 * {@code encrypt.key-secret} and qemu aborts with
 * {@code "Parameter 'encrypt.key-secret' is required for cipher"}.
 *
 * <p>Lives in the storage module (where the helper sits) and registers via
 * {@link KVMAttachVolumeExtensionPoint} (the existing hook KVMHost.attachVolume
 * already fires) to avoid creating a storage -> kvm reverse dep.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeEncryptedAttachExtension implements KVMAttachVolumeExtensionPoint, KVMConvertVolumeExtensionPoint {

    private static final CLogger logger = Utils.getLogger(VolumeEncryptedAttachExtension.class);

    @Autowired
    private VolumeEncryptedSecretHelper secretHelper;

    @Override
    public VolumeTO convertVolumeIfNeed(KVMHostInventory host, VolumeInventory inventory, VolumeTO to) {
        return to;
    }

    @Override
    public void beforeAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume,
                                    AttachDataVolumeCmd cmd, Map data) {
        if (!Boolean.TRUE.equals(volume.getEncrypted())) {
            return;
        }
        String hostUuid = host.getUuid();
        String vmUuid = vm.getUuid();
        String volUuid = volume.getUuid();
        String secretUuid = secretHelper.resolveOrDefineSecretForVolume(hostUuid, vmUuid, volUuid);
        VolumeTO to = cmd.getVolume();
        if (to != null) {
            to.setLuksSecretUuid(secretUuid);
        }
        logger.debug(String.format(
                "LUKS-ATTACH-EXT stamped secret %s onto attach cmd for volume[uuid:%s] on host[uuid:%s]",
                secretUuid, volUuid, hostUuid));
    }

    @Override
    public void afterAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume,
                                   AttachDataVolumeCmd cmd) {
    }

    @Override
    public void attachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume,
                                    AttachDataVolumeCmd cmd, ErrorCode err, Map data) {
    }
}
