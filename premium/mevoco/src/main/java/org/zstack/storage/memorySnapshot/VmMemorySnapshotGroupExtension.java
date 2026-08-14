package org.zstack.storage.memorySnapshot;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataManager;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataVO;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataVO_;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.zstack.storage.memorySnapshot.MemorySnapshotGroupConfigsUtils.getSystemTagsForArchive;
import static org.zstack.storage.memorySnapshot.MemorySnapshotGroupConfigsUtils.getVmResourceConfigsForArchive;

/**
 * Created by LiangHanYu on 2022/9/19 10:20
 */
public class VmMemorySnapshotGroupExtension implements MemorySnapshotResourceExtensionPoint, ResourceMetadataBuilder {
    private static final CLogger logger = Utils.getLogger(VmMemorySnapshotGroupExtension.class);
    @Autowired
    private VmInstanceResourceMetadataManager vidm;
    @Autowired
    private CloudBus bus;

    @Override
    public void archiveDeviceAddressByResources(String vmInstanceUuid, Completion completion) {
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmInstanceUuid)
                .find();

        VmInstanceResourceMetadataVO vo = Q.New(VmInstanceResourceMetadataVO.class)
                .eq(VmInstanceResourceMetadataVO_.vmInstanceUuid, vmInstanceUuid)
                .eq(VmInstanceResourceMetadataVO_.resourceUuid, vmInstanceUuid).find();
        ArchiveVmBundle archiveVmBundle;
        if (vo != null) {
            archiveVmBundle = JSONObjectUtil.toObject(vo.getMetadata(), ArchiveVmBundle.class);
        } else {
            archiveVmBundle = new ArchiveVmBundle();
        }
        archiveVmBundle.setResourceConfigBundles(getVmResourceConfigsForArchive(vmInstanceUuid));
        archiveVmBundle.setSystemTagBundles(getSystemTagsForArchive(vmInstanceUuid));
        archiveVmBundle.setVmInventory(VmInstanceInventory.valueOf(vmInstanceVO));

        vidm.createOrUpdateVmResourceMetadata(vmInstanceVO.getUuid(),
                null,
                vmInstanceVO.getUuid(),
                JSONObjectUtil.toJsonString(archiveVmBundle),
                getArchiveBundleCanonicalName());
        completion.success();
    }

    public static VmInstanceResourceMetadataVO buildCurrentVmResourceMetadataVO(String vmInstanceUuid) {
        if (vmInstanceUuid == null) {
            return null;
        }
        VmInstanceResourceMetadataVO vo =new VmInstanceResourceMetadataVO();
        vo.setVmInstanceUuid(vmInstanceUuid);
        vo.setResourceUuid(vmInstanceUuid);
        vo.setMetadataClass(ArchiveVmBundle.class.getCanonicalName());

        ArchiveVmBundle archiveVmBundle = new ArchiveVmBundle();
        archiveVmBundle.setResourceConfigBundles(getVmResourceConfigsForArchive(vmInstanceUuid));
        archiveVmBundle.setSystemTagBundles(getSystemTagsForArchive(vmInstanceUuid));
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmInstanceUuid).find();
        archiveVmBundle.setVmInventory(VmInstanceInventory.valueOf(vmInstanceVO));
        vo.setMetadata(JSONObjectUtil.toJsonString(archiveVmBundle));
        return vo;
    }

    @Override
    public void recoverDeviceByAddress(String vmInstanceUuid, String resourceUuid, List<?> bundles, Completion completion) {
        if (bundles.isEmpty()) {
            completion.success();
            return;
        }

        // restore vm configs
        ArchiveVmBundle archiveVmBundle = (ArchiveVmBundle) bundles.get(0);

        // revert system tags and resource configs
        MemorySnapshotGroupConfigsUtils.restoreConfigs(archiveVmBundle.getVmInventory().getUuid(), archiveVmBundle);

        // revert vm xml
        vidm.revertRequestedDeviceAddressFromArchive(vmInstanceUuid, resourceUuid, Collections.singletonList(vmInstanceUuid));

        UpdateVmInstanceMsg umsg = ((ArchiveVmBundle) bundles.get(0)).getUpdateVmMessage();
        bus.makeTargetServiceIdByResourceUuid(umsg, VmInstanceConstant.SERVICE_ID, umsg.getVmInstanceUuid());
        bus.send(umsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                String originImageUuid = archiveVmBundle.getVmInventory().getImageUuid();
                String nowImageUuid = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmInstanceUuid)
                        .select(VmInstanceVO_.imageUuid).findValue();
                if (Objects.equals(originImageUuid, nowImageUuid)) {
                    completion.success();
                    return;
                }
                if (originImageUuid == null) {
                    updateVmImageUuid(vmInstanceUuid, null);
                    updateRootVolumeRootImageUuid(vmInstanceUuid, null);
                    completion.success();
                    return;
                }
                if (Q.New(ImageVO.class).eq(ImageVO_.uuid, originImageUuid).isExists()) {
                    updateVmImageUuid(vmInstanceUuid, originImageUuid);
                    updateRootVolumeRootImageUuid(vmInstanceUuid, originImageUuid);
                } else {
                    updateVmImageUuid(vmInstanceUuid, null);
                    updateRootVolumeRootImageUuid(vmInstanceUuid, null);
                }
                completion.success();
            }
        });
    }

    private void updateVmImageUuid(String vmInstanceUuid, String imageUuid) {
        SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmInstanceUuid)
                .set(VmInstanceVO_.imageUuid, imageUuid).update();
    }

    private void updateRootVolumeRootImageUuid(String vmInstanceUuid, String imageUuid) {
        SQL.New(VolumeVO.class).eq(VolumeVO_.vmInstanceUuid, vmInstanceUuid).eq(VolumeVO_.type, VolumeType.Root)
                .set(VolumeVO_.rootImageUuid, imageUuid).update();
    }

    @Override
    public void rollBackResourceAndConfigs(String vmInstanceUuid, Map<String, VmInstanceResourceMetadataVO> originDeviceAddressByResourceUuid, Completion completion) {
        logger.debug(String.format("rollback resource configs for vm[uuid:%s]", vmInstanceUuid));

        VmInstanceResourceMetadataVO vmInstanceResourceMetadataVO = originDeviceAddressByResourceUuid.get(vmInstanceUuid);
        ArchiveVmBundle archiveVmBundle = JSONObjectUtil.toObject(vmInstanceResourceMetadataVO.getMetadata(), ArchiveVmBundle.class);
        MemorySnapshotGroupConfigsUtils.restoreConfigs(vmInstanceUuid, archiveVmBundle);

        vidm.saveVmXmlMetadata(archiveVmBundle.getXml(), vmInstanceUuid);

        UpdateVmInstanceMsg umsg = archiveVmBundle.getUpdateVmMessage();
        bus.makeTargetServiceIdByResourceUuid(umsg, VmInstanceConstant.SERVICE_ID, umsg.getVmInstanceUuid());
        bus.send(umsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                completion.success();
            }
        });
    }

    @Override
    public String getArchiveBundleCanonicalName() {
        return ArchiveVmBundle.class.getCanonicalName();
    }

    @Override
    public Class getArchiveBundleClass() {
        return ArchiveVmBundle.class;
    }

    @Override
    public List<VmInstanceResourceMetadataVO> buildResourceMetadataVOs(String vmInstanceUuid) {
        return Collections.singletonList(buildCurrentVmResourceMetadataVO(vmInstanceUuid));
    }
}