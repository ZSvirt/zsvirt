package org.zstack.compute.vm.devices;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.vm.*;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.vm.cdrom.VmCdRomVO_;
import org.zstack.header.vm.devices.*;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

public class VmInstanceResourceMetadataManagerImpl implements VmInstanceResourceMetadataManager {
    private static final CLogger logger = Utils.getLogger(VmInstanceResourceMetadataManagerImpl.class);
    @Autowired
    private DatabaseFacade dbf;

    private boolean deviceAddressRecordingDisabled() {
        return !VmGlobalConfig.ENABLE_VM_DEVICE_ADDRESS_RECORDING.value(Boolean.class);
    }

    @Override
    public VmInstanceResourceMetadataVO createOrUpdateVmResourceMetadata(String resourceUuid, DeviceAddress deviceAddress, String vmInstanceUuid, String metadata, String metadataClass) {
        if (deviceAddressRecordingDisabled()) {
            return null;
        }

        if (resourceUuid == null || vmInstanceUuid == null) {
            throw new OperationFailureException(operr("missing parameter, resourceUuid: %s, vmInstanceUuid: %s is requested", resourceUuid, vmInstanceUuid));
        }

        ErrorCode errorCode = checkParams(vmInstanceUuid, resourceUuid);
        if (errorCode != null) {
            throw new OperationFailureException(errorCode);
        }

        boolean addressExists = Q.New(VmInstanceResourceMetadataVO.class)
                .eq(VmInstanceResourceMetadataVO_.vmInstanceUuid, vmInstanceUuid)
                .eq(VmInstanceResourceMetadataVO_.resourceUuid, resourceUuid)
                .isExists();

        VmInstanceResourceMetadataVO vo;
        if (addressExists) {
            vo = Q.New(VmInstanceResourceMetadataVO.class)
                    .eq(VmInstanceResourceMetadataVO_.vmInstanceUuid, vmInstanceUuid)
                    .eq(VmInstanceResourceMetadataVO_.resourceUuid, resourceUuid)
                    .find();
        } else {
            vo = new VmInstanceResourceMetadataVO();
        }

        if (deviceAddress != null) {
            vo.setDeviceAddress(deviceAddress.toString());
        }

        vo.setResourceUuid(resourceUuid);
        vo.setVmInstanceUuid(vmInstanceUuid);
        if (metadata != null) {
            vo.setMetadata(metadata);
        }

        if (metadataClass != null) {
            Class clazz;
            try {
                clazz = Class.forName(metadataClass);
            } catch (ClassNotFoundException e) {
                logger.error(String.format("Failed to load metadata class: %s", metadataClass), e);
                throw new CloudRuntimeException(String.format("Metadata class [%s] not found in classpath", metadataClass), e);
            }

            vo.setMetadataClass(clazz.getCanonicalName());
        }

        if (addressExists) {
            vo = dbf.updateAndRefresh(vo);
        } else {
            vo = dbf.persist(vo);
        }

        return vo;
    }

    @Override
    public VmInstanceResourceMetadataVO createOrUpdateVmResourceMetadata(VirtualDeviceInfo virtualDeviceInfo, String vmInstanceUuid) {
        return createOrUpdateVmResourceMetadata(virtualDeviceInfo.getResourceUuid(), virtualDeviceInfo.getDeviceAddress(), vmInstanceUuid, null, null);
    }

    @Override
    public void saveVmXmlMetadata(String vmXml, String vmInstanceUuid) {
        if (deviceAddressRecordingDisabled()) {
            logger.debug("device address recording is disabled, skipping archive or update of vm xml for vmInstanceUuid: " + vmInstanceUuid);
            return;
        }

        if (vmXml == null) {
            logger.warn(String.format("vmXml is null for vmInstanceUuid: %s, skipping archive or update of vm xml", vmInstanceUuid));
            return;
        }

        VmInstanceResourceMetadataVO vo = Q.New(VmInstanceResourceMetadataVO.class)
                .eq(VmInstanceResourceMetadataVO_.vmInstanceUuid, vmInstanceUuid)
                .eq(VmInstanceResourceMetadataVO_.resourceUuid, vmInstanceUuid).find();
        if (vo != null) {
            ArchiveVmBundle archiveVmBundle;
            if (vo.getMetadata() == null || vo.getMetadata().isEmpty()) {
                archiveVmBundle = new ArchiveVmBundle();
            } else {
                archiveVmBundle = JSONObjectUtil.toObject(vo.getMetadata(), ArchiveVmBundle.class);
            }
            archiveVmBundle.setXml(vmXml);
            vo.setMetadata(JSONObjectUtil.toJsonString(archiveVmBundle));
            dbf.updateAndRefresh(vo);
        } else {
            vo = new VmInstanceResourceMetadataVO();
            vo.setMetadata(JSONObjectUtil.toJsonString(new ArchiveVmBundle(vmXml)));
            vo.setMetadataClass(ArchiveVmBundle.class.getCanonicalName());
            vo.setResourceUuid(vmInstanceUuid);
            vo.setVmInstanceUuid(vmInstanceUuid);
            dbf.persist(vo);
        }
    }

    @Override
    public DeviceAddress getVmDeviceAddress(String resourceUuid, String vmInstanceUuid) {
        VmInstanceResourceMetadataVO vo = Q.New(VmInstanceResourceMetadataVO.class)
                .eq(VmInstanceResourceMetadataVO_.resourceUuid, resourceUuid)
                .eq(VmInstanceResourceMetadataVO_.vmInstanceUuid, vmInstanceUuid)
                .find();

        return vo != null && vo.getDeviceAddress() != null ? DeviceAddress.fromString(vo.getDeviceAddress()) : null;
    }

    @Override
    public ErrorCode deleteVmResourceMetadata(String resourceUuid, String vmInstanceUuid) {
        if (resourceUuid == null || vmInstanceUuid == null) {
            return operr("missing parameter, resourceUuid: %s, vmInstanceUuid: %s is requested", resourceUuid, vmInstanceUuid);
        }

        ErrorCode errorCode = checkParams(vmInstanceUuid, resourceUuid);
        if (errorCode != null) {
            return errorCode;
        }

        SQL.New(VmInstanceResourceMetadataVO.class)
                .eq(VmInstanceResourceMetadataVO_.resourceUuid, resourceUuid)
                .eq(VmInstanceResourceMetadataVO_.vmInstanceUuid, vmInstanceUuid)
                .delete();

        return null;
    }

    @Override
    public ErrorCode deleteVmResourceMetadata(String resourceUuid) {
        if (resourceUuid == null) {
            return operr("missing parameter, resourceUuid is requested");
        }

        SQL.New(VmInstanceResourceMetadataVO.class)
                .eq(VmInstanceResourceMetadataVO_.resourceUuid, resourceUuid)
                .delete();

        return null;
    }

    @Override
    public ErrorCode deleteAllResourceMetadataByVm(String vmInstanceUuid) {
        if (vmInstanceUuid == null) {
            return operr("missing parameter, vmInstanceUuid: %s is requested", vmInstanceUuid);
        }

        if (!vmExists(vmInstanceUuid)) {
            return operr("cannot find vm with uuid: %s", vmInstanceUuid);
        }

        SQL.New(VmInstanceResourceMetadataVO.class)
                .eq(VmInstanceResourceMetadataVO_.vmInstanceUuid, vmInstanceUuid)
                .delete();

        return null;
    }

    @Override
    public ErrorCode deleteResourceMetadataByVmModifyVirtIO(String vmInstanceUuid) {
        if (vmInstanceUuid == null) {
            return operr("missing parameter, vmInstanceUuid: %s is requested", vmInstanceUuid);
        }

        if (!vmExists(vmInstanceUuid)) {
            return operr("cannot find vm with uuid: %s", vmInstanceUuid);
        }

        VmInstanceVO vo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmInstanceUuid).find();

        VmInstanceInventory inventory = vo.toInventory();
        inventory.getVmNics().forEach(vmNic -> deleteVmResourceMetadata(vmNic.getUuid(), vmInstanceUuid));
        inventory.getAllVolumes().forEach(volume -> deleteVmResourceMetadata(volume.getUuid(), vmInstanceUuid));
        return null;
    }

    @Override
    public VmInstanceResourceMetadataGroupVO archiveCurrentResourceMetadata(String vmInstanceUuid, String archiveForResourceUuid) {
        if (deviceAddressRecordingDisabled()) {
            return null;
        }

        return new SQLBatchWithReturn<VmInstanceResourceMetadataGroupVO>() {
            @Override
            protected VmInstanceResourceMetadataGroupVO scripts() {
                List<VmInstanceResourceMetadataVO> deviceAddressVOList = q(VmInstanceResourceMetadataVO.class)
                        .eq(VmInstanceResourceMetadataVO_.vmInstanceUuid, vmInstanceUuid)
                        .list();

                VmInstanceResourceMetadataGroupVO group = new VmInstanceResourceMetadataGroupVO();
                group.setResourceUuid(archiveForResourceUuid);
                group.setUuid(Platform.getUuid());
                group.setVmInstanceUuid(vmInstanceUuid);
                group = persist(group);

                for (VmInstanceResourceMetadataVO vo : deviceAddressVOList) {
                    VmInstanceResourceMetadataArchiveVO archiveVO = new VmInstanceResourceMetadataArchiveVO();
                    archiveVO.setDeviceAddress(vo.getDeviceAddress());
                    archiveVO.setResourceUuid(vo.getResourceUuid());
                    archiveVO.setVmInstanceUuid(vmInstanceUuid);
                    archiveVO.setAddressGroupUuid(group.getUuid());
                    archiveVO.setMetadata(vo.getMetadata());
                    archiveVO.setMetadataClass(vo.getMetadataClass());
                    persist(archiveVO);
                }

                return reload(group);
            }
        }.execute();
    }

    @Override
    public List<VmInstanceResourceMetadataVO> revertResourceMetadataFromArchive(String vmInstanceUuid, String archiveForResourceUuid) {
        if (deviceAddressRecordingDisabled()) {
            return Collections.emptyList();
        }

        return revertResourceMetadataWithFilter(vmInstanceUuid, archiveForResourceUuid);
    }

    private List<VmInstanceResourceMetadataVO> revertResourceMetadataWithFilter(String vmInstanceUuid, String archiveForResourceUuid) {

        VmInstanceResourceMetadataGroupVO group = Q.New(VmInstanceResourceMetadataGroupVO.class)
                .eq(VmInstanceResourceMetadataGroupVO_.resourceUuid, archiveForResourceUuid)
                .find();

        List<VmInstanceResourceMetadataVO> createdAddressList = new ArrayList<>();
        if (group == null) {
            return createdAddressList;
        }

        for (VmInstanceResourceMetadataArchiveVO archive : group.getAddressList()) {
            VmInstanceResourceMetadataVO vo = createOrUpdateVmResourceMetadata(archive.getResourceUuid(), DeviceAddress.fromString(archive.getDeviceAddress()), vmInstanceUuid, archive.getMetadata(), archive.getMetadataClass());
            createdAddressList.add(vo);
        }

        return createdAddressList;
    }

    @Override
    public List<VmInstanceResourceMetadataVO> revertExistingDeviceAddressFromArchive(String vmInstanceUuid, String archiveForResourceUuid) {
        return revertResourceMetadataWithFilter(vmInstanceUuid, archiveForResourceUuid);
    }

    @Override
    public List<VmInstanceResourceMetadataVO> revertRequestedDeviceAddressFromArchive(String vmInstanceUuid, String archiveForResourceUuid, List<String> needRevertResourceUuidList) {
        VmInstanceResourceMetadataGroupVO group = Q.New(VmInstanceResourceMetadataGroupVO.class)
                .eq(VmInstanceResourceMetadataGroupVO_.resourceUuid, archiveForResourceUuid)
                .find();

        List<VmInstanceResourceMetadataVO> createdAddressList = new ArrayList<>();
        if (group == null) {
            return createdAddressList;
        }

        for (VmInstanceResourceMetadataArchiveVO archive : group.getAddressList()) {
            if (!needRevertResourceUuidList.contains(archive.getResourceUuid())) {
                continue;
            }

            VmInstanceResourceMetadataVO vo = createOrUpdateVmResourceMetadata(archive.getResourceUuid(), DeviceAddress.fromString(archive.getDeviceAddress()), vmInstanceUuid, archive.getMetadata(), archive.getMetadataClass());
            createdAddressList.add(vo);
        }

        return createdAddressList;
    }

    @Override
    public List<VmInstanceResourceMetadataVO> createResourceMetadataFromArchive(String vmInstanceUuid, String archiveForResourceUuid, Map<String, String> resourceMap) {
        if (deviceAddressRecordingDisabled()) {
            return Collections.emptyList();
        }

        VmInstanceResourceMetadataGroupVO group = Q.New(VmInstanceResourceMetadataGroupVO.class)
                .eq(VmInstanceResourceMetadataGroupVO_.resourceUuid, archiveForResourceUuid)
                .find();

        List<VmInstanceResourceMetadataVO> createdAddressList = new ArrayList<>();
        if (group == null) {
            return createdAddressList;
        }

        for (VmInstanceResourceMetadataArchiveVO archive : group.getAddressList()) {
            String matchedResourceUuid = resourceMap.get(archive.getResourceUuid());

            // create device address request new resourceUuid if not found skip pci address create
            if (matchedResourceUuid == null) {
                continue;
            }

            VmInstanceResourceMetadataVO vo = createOrUpdateVmResourceMetadata(matchedResourceUuid, DeviceAddress.fromString(archive.getDeviceAddress()), vmInstanceUuid, archive.getMetadata(), archive.getMetadataClass());
            createdAddressList.add(vo);
        }

        return createdAddressList;
    }

    @Override
    public void updateVmResourceMetadataDeviceAddress(String vmInstanceUuid, String resourceUuid, String deviceAddress) {
        if (deviceAddress == null) {
            return;
        }
        createOrUpdateVmResourceMetadata(resourceUuid, DeviceAddress.fromString(deviceAddress), vmInstanceUuid, null, null);
    }

    @Override
    public void deleteArchiveVmInstanceResourceMetadataGroup(String archiveForResourceUuid) {
        SQL.New(VmInstanceResourceMetadataGroupVO.class).eq(VmInstanceResourceMetadataGroupVO_.resourceUuid, archiveForResourceUuid).hardDelete();
    }

    @Override
    public List<VmInstanceResourceMetadataArchiveVO> getArchivedResourceMetadataInfoFromArchiveForResourceUuid(String vmInstanceUuid, String archiveForResourceUuid, String metadataClass) {
        if (deviceAddressRecordingDisabled()) {
            return Collections.emptyList();
        }

        String vmInstanceResourceMetadataGroupUuid = Q.New(VmInstanceResourceMetadataGroupVO.class)
                .select(VmInstanceResourceMetadataGroupVO_.uuid)
                .eq(VmInstanceResourceMetadataGroupVO_.resourceUuid, archiveForResourceUuid)
                .findValue();

        if (vmInstanceResourceMetadataGroupUuid == null) {
            return new ArrayList<>();
        }

        return Q.New(VmInstanceResourceMetadataArchiveVO.class)
                .eq(VmInstanceResourceMetadataArchiveVO_.addressGroupUuid, vmInstanceResourceMetadataGroupUuid)
                .eq(VmInstanceResourceMetadataArchiveVO_.vmInstanceUuid, vmInstanceUuid)
                .eq(VmInstanceResourceMetadataArchiveVO_.metadataClass, metadataClass)
                .list();
    }

    private boolean vmExists(String vmInstanceUuid) {
        return dbf.isExist(vmInstanceUuid, VmInstanceVO.class);
    }

    private boolean vmDeviceExists(String resourceUuid) {
        return new SQLBatchWithReturn<Boolean>() {

            @Override
            protected Boolean scripts() {
                boolean volumeExists = q(VolumeVO.class).eq(VolumeVO_.uuid, resourceUuid).isExists();
                boolean nicExists = q(VmNicVO.class).eq(VmNicVO_.uuid, resourceUuid).isExists();
                boolean cdRomExists = q(VmCdRomVO.class).eq(VmCdRomVO_.uuid, resourceUuid).isExists();

                return volumeExists || nicExists || cdRomExists || vmExists(resourceUuid);
            }
        }.execute();
    }

    private ErrorCode checkParams(String vmInstanceUuid, String resourceUuid) {
        if (MEM_BALLOON_UUID.equals(resourceUuid)) {
            return null;
        }

        if (RESOURCE_CONFIG_UUID.equals(resourceUuid)) {
            return null;
        }

        if (GUEST_TOOLS_RESOURCE_CONFIG_UUID.equals(resourceUuid)) {
            return null;
        }

        if (!vmExists(vmInstanceUuid)) {
            return operr("cannot find vm with uuid: %s", vmInstanceUuid);
        }

        if (!vmDeviceExists(resourceUuid)) {
            return operr("cannot find vm device with uuid: %s", resourceUuid);
        }

        return null;
    }
}
