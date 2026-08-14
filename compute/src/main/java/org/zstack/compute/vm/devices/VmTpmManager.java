package org.zstack.compute.vm.devices;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.image.ImageBootMode;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.tpm.entity.TpmVO_;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.zstack.compute.vm.VmGlobalConfig.ENABLE_UEFI_SECURE_BOOT;
import static org.zstack.header.vm.additions.VmHostFileType.NvRam;
import static org.zstack.header.vm.additions.VmHostFileType.TpmState;
import static org.zstack.utils.CollectionDSL.list;

public class VmTpmManager {
    private static final CLogger logger = Utils.getLogger(VmTpmManager.class);

    @Autowired
    private DatabaseFacade databaseFacade;
    @Autowired
    private ResourceConfigFacade resourceConfigFacade;

    public TpmVO persistTpmVO(String tpmUuid, String vmUuid) {
        if (tpmUuid == null) {
            tpmUuid = Platform.getUuid();
        }
        TpmVO tpm = new TpmVO();
        tpm.setUuid(tpmUuid);
        tpm.setResourceName("TPM-for-VM-" + vmUuid);
        tpm.setVmInstanceUuid(vmUuid);
        databaseFacade.persistAndRefresh(tpm);

        logger.debug("Persisted TpmVO for VM " + vmUuid + " with uuid=" + tpm.getUuid());
        return tpm;
    }

    public void deleteTpmVO(String tpmUuid) {
        databaseFacade.removeByPrimaryKey(tpmUuid, TpmVO.class);
    }

    public static String findTpmUuidForVmOrNull(String vmInstanceUuid) {
        return Q.New(TpmVO.class)
                .eq(TpmVO_.vmInstanceUuid, vmInstanceUuid)
                .select(TpmVO_.uuid)
                .findValue();
    }

    /**
     * @param bootMode boot mode, null is Legacy
     */
    public static boolean isUefiBootMode(String bootMode) {
        return Objects.equals(bootMode, ImageBootMode.UEFI.toString())
                || Objects.equals(bootMode, ImageBootMode.UEFI_WITH_CSM.toString());
    }

    public boolean needRegisterNvRam(String vmUuid) {
        return needRegister(NvRam, vmUuid);
    }

    public Set<VmHostFileType> vmHostFileTypeNeedRegisterForVm(String vmUuid) {
        String bootMode = VmSystemTags.BOOT_MODE.getTokenByResourceUuid(vmUuid, VmSystemTags.BOOT_MODE_TOKEN);
        if (!isUefiBootMode(bootMode)) {
            return Collections.emptySet();
        }

        boolean hasTpm = Q.New(TpmVO.class)
                .eq(TpmVO_.vmInstanceUuid, vmUuid)
                .isExists();
        if (hasTpm) {
            return new HashSet<>(list(NvRam, TpmState));
        }
        ResourceConfig resourceConfig = resourceConfigFacade.getResourceConfig(ENABLE_UEFI_SECURE_BOOT.getIdentity());
        return resourceConfig.getResourceConfigValue(vmUuid, Boolean.class) == Boolean.TRUE ?
                new HashSet<>(list(NvRam)) : Collections.emptySet();
    }

    public boolean needRegister(VmHostFileType type, String vmUuid) {
        return vmHostFileTypeNeedRegisterForVm(vmUuid).contains(type);
    }
}
