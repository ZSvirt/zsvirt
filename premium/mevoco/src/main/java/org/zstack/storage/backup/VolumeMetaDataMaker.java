package org.zstack.storage.backup;

import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.storage.backup.VolumeMetadata;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

public class VolumeMetaDataMaker {
    public static String generateRootVolumeMetadata(final String volumeUuid, final List<String> dataVolumeUuids) {
        return generateMetadata(volumeUuid, dataVolumeUuids);
    }

    public static String generateDataVolumeMetadata(final String volumeUuid) {
        return generateMetadata(volumeUuid, null);
    }

    public static VolumeMetadata parseVolumeMetadata(final String metadata) {
        return JSONObjectUtil.toObject(metadata, VolumeMetadata.class);
    }

    private static String generateMetadata(final String volumeUuid, final List<String> dataVolumeUuids) {
        VolumeMetadata metadata = new VolumeMetadata();

        new SQLBatch() {
            @Override
            protected void scripts() {
                VolumeVO vvo = q(VolumeVO.class).eq(VolumeVO_.uuid, volumeUuid).find();
                VmInstanceVO vm = q(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vvo.getVmInstanceUuid()).find();

                metadata.setName(vvo.getName());
                metadata.setDescription(vvo.getDescription());
                metadata.setType(vvo.getType().toString());
                metadata.setRootImageUuid(vvo.getRootImageUuid());

                metadata.setVmInstanceUuid(vm.getUuid());
                metadata.setVmName(vm.getName());
                metadata.setVmDescription(vm.getDescription());
                metadata.setInstanceOfferingUuid(vm.getInstanceOfferingUuid());
                metadata.setCpuNum(vm.getCpuNum());
                metadata.setMemorySize(vm.getMemorySize());
                metadata.setPlatform(vm.getPlatform());
                metadata.setGuestOsType(vm.getGuestOsType());
                metadata.setVirtio(VmSystemTags.VIRTIO.hasTag(vm.getUuid()));
                metadata.setClusterUuid(vm.getClusterUuid());
                metadata.setPrimaryStorageUuid(vvo.getPrimaryStorageUuid());
                metadata.setDefaultL3NetworkUuid(vm.getDefaultL3NetworkUuid());
                metadata.setDataVolumeUuids(dataVolumeUuids);

                metadata.setActualSize(vvo.getActualSize());
                metadata.setSize(vvo.getSize());
                metadata.setFormat(vvo.getFormat());
                metadata.setAttachedVmUuid(vvo.getVmInstanceUuid());
                metadata.setOwnerAccountUuid(vvo.getAccountUuid());
                metadata.setShareable(vvo.isShareable());
                metadata.setDeviceId(vvo.getDeviceId());

                List<SystemTagVO> tags = Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, vvo.getUuid()).list();
                if (!tags.isEmpty()) {
                    List<SystemTagInventory> invs = transformAndRemoveNull(tags, arg -> arg.toInventory());
                    metadata.setSystemTags(invs);
                }

                if (vvo.getType() == VolumeType.Root) {
                    List<SystemTagVO> vos = q(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, vm.getUuid()).list();
                    metadata.setVmSystemTags(vos.stream().map(SystemTagVO::getTag).collect(Collectors.toList()));
                    metadata.setArchitecture(vm.getArchitecture());
                }

                List<VmNicVO> nics = q(VmNicVO.class)
                        .eq(VmNicVO_.vmInstanceUuid, vvo.getVmInstanceUuid())
                        .list();
                metadata.setVmNics(VmNicInventory.valueOf(nics));
            }
        }.execute();

        return JSONObjectUtil.toJsonString(metadata);
    }
}
