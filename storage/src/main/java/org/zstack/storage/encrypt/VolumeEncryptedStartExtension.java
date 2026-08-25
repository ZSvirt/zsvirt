package org.zstack.storage.encrypt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.vm.VmBeforeCreateOnHypervisorExtensionPoint;
import org.zstack.header.vm.VmBeforeStartOnHypervisorExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.zstack.core.Platform.operr;

/**
 * Per start_vm: resolve the libvirt LUKS secret UUID for every encrypted volume
 * on the destination host and stash the mapping into
 * {@link VmInstanceSpec#putExtensionData} under {@link #EXT_DATA_KEY} so
 * {@code KVMHost.handleStart} can inline it into the {@code VolumeTO} that
 * ships with {@code StartVmCmd}.
 *
 * <p>Why every start (not persisted)? libvirt secret <i>values</i> are RAM-only;
 * libvirtd restart / host reboot / live-migrate-to-new-host all wipe them. The
 * UUID itself would persist but without the value the secret is useless to qemu.
 * So on every start_vm we delegate to
 * {@link VolumeEncryptedSecretHelper#resolveOrDefineSecretForVolume}, which
 * first asks the host (idempotent {@code SecretHostGetMsg} → key-agent
 * {@code GetSecret}) and falls back to materialize-DEK + define on miss.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeEncryptedStartExtension
        implements VmBeforeStartOnHypervisorExtensionPoint, VmBeforeCreateOnHypervisorExtensionPoint {

    private static final CLogger logger = Utils.getLogger(VolumeEncryptedStartExtension.class);

    /** {@code Map<volumeUuid, libvirtSecretUuid>} consumed by KVMHost.handleStart. */
    public static final String EXT_DATA_KEY = "VolumeLuksSecrets";

    @Autowired
    private VolumeEncryptedSecretHelper secretHelper;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void beforeStartVmOnHypervisor(VmInstanceSpec spec) {
        HostInventory destHost = spec.getDestHost();
        if (destHost == null || StringUtils.isBlank(destHost.getUuid())) {
            return;
        }

        List<VolumeInventory> encryptedVolumes = collectEncryptedVolumes(spec);
        if (encryptedVolumes.isEmpty()) {
            return;
        }

        String hostUuid = destHost.getUuid();
        String vmUuid = spec.getVmInventory().getUuid();
        Map<String, String> resolved = new HashMap<>();

        for (VolumeInventory vol : encryptedVolumes) {
            String volUuid = vol.getUuid();
            String secretUuid = secretHelper.resolveOrDefineSecretForVolume(hostUuid, vmUuid, volUuid);
            if (StringUtils.isBlank(secretUuid)) {
                throw new OperationFailureException(operr(
                        "failed to resolve libvirt LUKS secret for encrypted volume[uuid:%s] on host[uuid:%s]",
                        volUuid, hostUuid));
            }
            resolved.put(volUuid, secretUuid);
        }

        if (!resolved.isEmpty()) {
            spec.putExtensionData(EXT_DATA_KEY, resolved);
            logger.debug(String.format("LUKS-START-EXT stashed %d secrets into spec.extensionData[%s]: %s",
                    resolved.size(), EXT_DATA_KEY, resolved));
        }
    }

    /**
     * "Create VM" (provisioning) path uses {@link org.zstack.compute.vm.VmCreateOnHypervisorFlow}
     * which fires {@link VmBeforeCreateOnHypervisorExtensionPoint}, NOT the start-vm hook.
     * Delegate to the same logic: encrypted root volumes need their libvirt secret
     * resolved on the destination host before the agent receives StartVmCmd.
     */
    @Override
    public void beforeCreateVmOnHypervisor(VmInstanceSpec spec) {
        beforeStartVmOnHypervisor(spec);
    }

    private List<VolumeInventory> collectEncryptedVolumes(VmInstanceSpec spec) {
        List<VolumeInventory> result = new ArrayList<>();
        Map<String, VolumeVO> latestVolumes = latestVolumes(spec);
        VolumeInventory root = spec.getDestRootVolume();
        if (root != null && isEncrypted(latestVolumes, root)) {
            result.add(root);
        }
        if (spec.getDestDataVolumes() != null) {
            for (VolumeInventory v : spec.getDestDataVolumes()) {
                if (v != null && isEncrypted(latestVolumes, v)) {
                    result.add(v);
                }
            }
        }
        return result;
    }

    private Map<String, VolumeVO> latestVolumes(VmInstanceSpec spec) {
        Set<String> volumeUuids = new HashSet<>();
        collectVolumeUuid(volumeUuids, spec.getDestRootVolume());
        if (spec.getDestDataVolumes() != null) {
            for (VolumeInventory v : spec.getDestDataVolumes()) {
                collectVolumeUuid(volumeUuids, v);
            }
        }

        Map<String, VolumeVO> ret = new HashMap<>();
        if (volumeUuids.isEmpty()) {
            return ret;
        }

        for (VolumeVO volume : dbf.listByPrimaryKeys(volumeUuids, VolumeVO.class)) {
            ret.put(volume.getUuid(), volume);
        }
        return ret;
    }

    private void collectVolumeUuid(Set<String> volumeUuids, VolumeInventory volume) {
        if (volume != null && StringUtils.isNotBlank(volume.getUuid())) {
            volumeUuids.add(volume.getUuid());
        }
    }

    private boolean isEncrypted(Map<String, VolumeVO> latestVolumes, VolumeInventory volume) {
        VolumeVO latest = latestVolumes.get(volume.getUuid());
        if (latest != null) {
            return latest.isEncrypted();
        }
        return Boolean.TRUE.equals(volume.getEncrypted());
    }
}
