package org.zstack.guesttools;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataManager;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataVO;
import org.zstack.storage.memorySnapshot.MemorySnapshotResourceExtensionPoint;
import org.zstack.storage.memorySnapshot.ResourceMetadataBuilder;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.header.vm.devices.VmInstanceResourceMetadataManager.GUEST_TOOLS_RESOURCE_CONFIG_UUID;

public class VmGuestToolsExtension implements MemorySnapshotResourceExtensionPoint, ResourceMetadataBuilder, UpdateGuestToolsState {
    private static final CLogger logger = Utils.getLogger(VmGuestToolsExtension.class);
    @Autowired
    private VmInstanceResourceMetadataManager vidm;

    @Override
    public void archiveDeviceAddressByResources(String vmInstanceUuid, Completion completion) {
        GuestToolsStateVO vo = Q.New(GuestToolsStateVO.class).eq(GuestToolsStateVO_.vmInstanceUuid, vmInstanceUuid).find();
        if (vo == null) {
            logger.warn(String.format("no GuestToolsStateVO found for vm[uuid:%s], skip archiving guest tools state", vmInstanceUuid));
            completion.success();
            return;
        }
        ArchiveVmGuestToolsBundle archiveVmGuestToolsBundle = new ArchiveVmGuestToolsBundle(GuestToolsStateInventory.valueOf(vo));
        vidm.createOrUpdateVmResourceMetadata(GUEST_TOOLS_RESOURCE_CONFIG_UUID,
                null,
                vmInstanceUuid,
                JSONObjectUtil.toJsonString(archiveVmGuestToolsBundle),
                getArchiveBundleCanonicalName());
        completion.success();
    }

    @Override
    public void recoverDeviceByAddress(String vmInstanceUuid, String resourceUuid, List<?> bundles, Completion completion) {
        if (bundles == null || bundles.isEmpty() || !(bundles.get(0) instanceof ArchiveVmGuestToolsBundle)) {
            logger.warn(String.format("no valid ArchiveVmGuestToolsBundle for vm[uuid:%s], skip recover", vmInstanceUuid));
            completion.success();
            return;
        }
        ArchiveVmGuestToolsBundle archiveVmGuestToolsBundle = (ArchiveVmGuestToolsBundle) bundles.get(0);
        updateGuestToolsStateVO(vmInstanceUuid, archiveVmGuestToolsBundle.getGuestToolsState());
        completion.success();
    }

    private void updateGuestToolsStateVO(String vmInstanceUuid, GuestToolsStateInventory guestToolsState) {
        if (guestToolsState == null) {
            logger.warn(String.format("guestToolsState is null for vm[uuid:%s], skip update", vmInstanceUuid));
            return;
        }

        SQL.New(GuestToolsStateVO.class)
                .eq(GuestToolsStateVO_.vmInstanceUuid, vmInstanceUuid)
                .set(GuestToolsStateVO_.qgaState, GuestToolsQgaState.valueOf(guestToolsState.getQgaState()))
                .set(GuestToolsStateVO_.zwatchState, GuestToolsZWatchState.valueOf(guestToolsState.getZwatchState()))
                .set(GuestToolsStateVO_.version, guestToolsState.getVersion())
                .set(GuestToolsStateVO_.platform, guestToolsState.getPlatform())
                .set(GuestToolsStateVO_.osType, guestToolsState.getOsType())
                .update();
    }

    @Override
    public void rollBackResourceAndConfigs(String vmInstanceUuid, Map<String, VmInstanceResourceMetadataVO> originDeviceAddressByResourceUuid, Completion completion) {
        logger.debug(String.format("rollback GuestToolsState for vm[uuid:%s]", vmInstanceUuid));

        VmInstanceResourceMetadataVO meta = originDeviceAddressByResourceUuid.get(GUEST_TOOLS_RESOURCE_CONFIG_UUID);
        if (meta == null || meta.getMetadata() == null) {
            logger.warn(String.format("no guest-tools metadata to rollback for vm[uuid:%s]", vmInstanceUuid));
            completion.success();
            return;
        }

        try {
            ArchiveVmGuestToolsBundle bundle = JSONObjectUtil.toObject(meta.getMetadata(), ArchiveVmGuestToolsBundle.class);
            updateGuestToolsStateVO(vmInstanceUuid, bundle.getGuestToolsState());
        } catch (Exception e) {
            logger.warn(String.format("failed to parse guest-tools metadata for vm[uuid:%s], skip rollback. %s", vmInstanceUuid, e));
        }
        completion.success();
    }

    @Override
    public String getArchiveBundleCanonicalName() {
        return ArchiveVmGuestToolsBundle.class.getCanonicalName();
    }

    @Override
    public Class getArchiveBundleClass() {
        return ArchiveVmGuestToolsBundle.class;
    }

    @Override
    public List<VmInstanceResourceMetadataVO> buildResourceMetadataVOs(String vmInstanceUuid) {
        GuestToolsStateVO guestToolsStateVO = Q.New(GuestToolsStateVO.class).eq(GuestToolsStateVO_.vmInstanceUuid, vmInstanceUuid).find();
        if (guestToolsStateVO == null) {
            logger.warn(String.format("no GuestToolsStateVO found for vm[uuid:%s], skip archiving guest tools state", vmInstanceUuid));
            return Collections.emptyList();
        }

        VmInstanceResourceMetadataVO vo = new VmInstanceResourceMetadataVO();
        vo.setVmInstanceUuid(vmInstanceUuid);
        vo.setResourceUuid(GUEST_TOOLS_RESOURCE_CONFIG_UUID);
        vo.setMetadataClass(ArchiveVmGuestToolsBundle.class.getCanonicalName());

        ArchiveVmGuestToolsBundle archiveVmGuestToolsBundle = new ArchiveVmGuestToolsBundle(GuestToolsStateInventory.valueOf(guestToolsStateVO));
        vo.setMetadata(JSONObjectUtil.toJsonString(archiveVmGuestToolsBundle));
        return Collections.singletonList(vo);
    }

    @Override
    public boolean skipUpdate(String vmInstanceUuid) {
        GuestToolsStateVO guestToolsStateVO = Q.New(GuestToolsStateVO.class).eq(GuestToolsStateVO_.vmInstanceUuid, vmInstanceUuid).find();
        Timestamp createDate = guestToolsStateVO.getCreateDate();
        Timestamp lastOpDate = guestToolsStateVO.getLastOpDate();
        GuestToolsQgaState qgaState = guestToolsStateVO.getQgaState();
        GuestToolsZWatchState zwatchState = guestToolsStateVO.getZwatchState();
        if (createDate.getTime() < lastOpDate.getTime() && GuestToolsQgaState.NotInstalled == qgaState && GuestToolsZWatchState.NotInstalled == zwatchState) {
            if (lastOpDate.getTime() + TimeUnit.MINUTES.toMillis(3) > System.currentTimeMillis()) {
                logger.debug(String.format("Skipping update  GuestToolsState for VM [uuid:%s]: GuestTools not installed" +
                        " and within 3-minute grace period after reset", vmInstanceUuid));
                return true;
            }
        }
        return false;
    }
}