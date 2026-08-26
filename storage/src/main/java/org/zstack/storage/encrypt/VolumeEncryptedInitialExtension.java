package org.zstack.storage.encrypt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.keyprovider.EncryptedResourceKeyManager;
import org.zstack.header.storage.primary.InstantiateVolumeOnPrimaryStorageMsg;
import org.zstack.header.volume.AfterInstantiateVolumeExtensionPoint;
import org.zstack.header.volume.CreateDataVolumeExtensionPoint;
import org.zstack.header.volume.InstantiateVolumeMsg;
import org.zstack.header.volume.InstantiateTemporaryRootVolumeMsg;
import org.zstack.header.volume.PreInstantiateVolumeExtensionPoint;
import org.zstack.header.volume.VolumeCreateMessage;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;

import static org.zstack.core.Platform.operr;

/**
 * Encrypted volume instantiate: {@link #preInstantiateVolume} ensures the volume key exists;
 * {@link #afterInstantiateVolume} defines the libvirt secret on the host when a VM uuid is already known.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeEncryptedInitialExtension implements PreInstantiateVolumeExtensionPoint,
        AfterInstantiateVolumeExtensionPoint, CreateDataVolumeExtensionPoint {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VolumeEncryptedResourceKeyBackend volumeEncryptedResourceKeyBackend;
    @Autowired
    private VolumeEncryptedSecretHelper secretHelper;
    @Autowired
    private VolumeSnapshotEncryptionHelper snapshotEncryptionHelper;

    @Override
    public void preInstantiateVolume(InstantiateVolumeMsg msg) {
        String volUuid = msg.getVolumeUuid();
        VolumeVO volume = dbf.findByUuid(volUuid, VolumeVO.class);

        if (volume != null && volume.isEncrypted()) {
            // Temporary root images created from snapshots own the image cache before
            // the final root volume exists. Persisting the cache key on ImageVO lets
            // the root volume inherit the same key, so its top layer can match the
            // encrypted image cache.
            snapshotEncryptionHelper.inheritFromTemporarySnapshotImageKeyIfPossible(volume);
            inheritTemporaryRootVolumeKeyFromOrigin(msg, volume);
            String kpUuid = volumeEncryptedResourceKeyBackend.findKeyProviderUuidByVolume(volUuid);
            if (StringUtils.isBlank(kpUuid)) {
                kpUuid = volumeEncryptedResourceKeyBackend.defaultKeyProviderUuid();
                if (StringUtils.isBlank(kpUuid)) {
                    throw new OperationFailureException(operr(
                            "encrypted volume[uuid:%s] has no key provider binding and no default key provider configured",
                            volUuid));
                }
                volumeEncryptedResourceKeyBackend.attachKeyProviderToVolume(volUuid, kpUuid);
            }

            EncryptedResourceKeyManager.ResourceKeyResult keyResult = secretHelper.materializeDek(volUuid, kpUuid);
            String dekBase64 = keyResult.getDekBase64();
            if (StringUtils.isBlank(dekBase64)) {
                throw new OperationFailureException(operr(
                        "encrypted volume[uuid:%s]: key manager returned empty DEK after materialization",
                        volUuid));
            }

        }
    }

    private void inheritTemporaryRootVolumeKeyFromOrigin(InstantiateVolumeMsg msg, VolumeVO volume) {
        if (!(msg instanceof InstantiateTemporaryRootVolumeMsg) || volume == null || !volume.isEncrypted()) {
            return;
        }

        // A backup or snapshot temporary image can already provide the target root
        // volume key. Keep that key as the source of truth; falling back to the
        // origin root would overwrite it or fail if the origin key binding is gone.
        if (volumeEncryptedResourceKeyBackend.checkVolumeKeyProviderAttached(volume.getUuid())) {
            return;
        }

        String originVolumeUuid = ((InstantiateTemporaryRootVolumeMsg) msg).getOriginVolumeUuid();
        if (StringUtils.isBlank(originVolumeUuid)) {
            return;
        }

        VolumeVO originVolume = dbf.findByUuid(originVolumeUuid, VolumeVO.class);
        if (originVolume == null || !originVolume.isEncrypted()) {
            return;
        }

        if (!volumeEncryptedResourceKeyBackend.checkVolumeKeyProviderAttached(originVolumeUuid)) {
            throw new OperationFailureException(operr(
                    "encrypted origin root volume[uuid:%s] has no key provider binding for temporary root volume[uuid:%s]",
                    originVolumeUuid, volume.getUuid()));
        }

        volumeEncryptedResourceKeyBackend.copyVolumeKeyRefToVolume(originVolumeUuid, volume.getUuid());
    }

    @Override
    public void afterInstantiateVolume(InstantiateVolumeOnPrimaryStorageMsg msg) {
        VolumeInventory volInv = msg.getVolume();
        if (volInv == null || !Boolean.TRUE.equals(volInv.getEncrypted())) {
            return;
        }
        String volUuid = volInv.getUuid();
        HostInventory destHost = msg.getDestHost();
        if (destHost == null || StringUtils.isBlank(destHost.getUuid())) {
            return;
        }

        VolumeVO volume = dbf.findByUuid(volUuid, VolumeVO.class);
        if (volume == null || !volume.isEncrypted()) {
            return;
        }
        // VolumeInventory already carries vmInstanceUuid when present, so we
        // skip the extra select(VolumeVO_.vmInstanceUuid) round-trip.
        // VmInstantiateOtherDiskFlow's "create empty data volume" path
        // (setupCreateVolumeFromDiskSizeFlows) does NOT set
        // VolumeVO.vmInstanceUuid before this hook fires — vmInstanceUuid is
        // backfilled only after the volume is attached, well after this
        // afterInstantiateVolume runs. Without vmUuid we cannot key the
        // libvirt secret (SecretHostDefineMsg requires it), so skip the
        // early-define here; VolumeEncryptedStartExtension on the start_vm
        // path will define the secret then, when vmUuid is known.
        String vmInstanceUuid = volInv.getVmInstanceUuid();
        if (StringUtils.isBlank(vmInstanceUuid)) {
            return;
        }
        String kpUuid = volumeEncryptedResourceKeyBackend.findKeyProviderUuidByVolume(volUuid);
        secretHelper.defineSecretFromBinding(destHost.getUuid(), vmInstanceUuid, volUuid, kpUuid);
    }

    @Override
    public void preCreateVolume(VolumeCreateMessage msg) {
    }

    @Override
    public void beforeCreateVolume(VolumeInventory volume) {
    }

    @Override
    public void afterCreateVolume(VolumeVO volume) {
    }

    @Override
    public void afterCreateVolume(VolumeVO volume, String snapshotUuid) {
        snapshotEncryptionHelper.inheritFromRelatedSnapshotKeyIfPossible(volume, snapshotUuid);
    }
}
