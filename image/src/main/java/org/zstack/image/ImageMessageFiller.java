package org.zstack.image;

import org.zstack.compute.vm.VmExtraInfoGetter;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.config.schema.GuestOsCategory;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.image.*;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;

import java.util.Collections;

/**
 * Created by MaJin on 2021/3/16.
 */
public class ImageMessageFiller {

    public static void fillDefault(AddImageMessage msg) {
        if (ImageConstant.ImageMediaType.DataVolumeTemplate.toString().equals(msg.getMediaType())) {
            return;
        }

        if (msg.getPlatform() == null) {
            msg.setPlatform(ImageDefaultBehavior.getDefaultPlatform());
        }
        if (msg.getPlatform() != null && msg.getPlatform().equals(ImagePlatform.Paravirtualization.toString())) {
            msg.setPlatform(ImagePlatform.Other.toString());
        }
        if (msg.getPlatform() != null && msg.getPlatform().equals(ImagePlatform.WindowsVirtio.toString())) {
            msg.setPlatform(ImagePlatform.Windows.toString());
        }

        if (msg.getArchitecture() == null && !ImageConstant.ImageMediaType.DataVolumeTemplate.toString().equals(msg.getMediaType())) {
            msg.setArchitecture(CoreGlobalProperty.UNIT_TEST_ON ?
                    ImageArchitecture.x86_64.toString() : ImageArchitecture.defaultArch());
        }

        if (msg.getGuestOsType() == null) {
            msg.setGuestOsType(msg.getPlatform() == null ? null : GuestOsCategory.getDefaultGuestOsTypeByPlatform(msg.getPlatform()));
        }

        setBootModeIfAarch64(msg);
    }

    public static void fillFromVm(AddImageMessage msg, String vmUuid) {
        if (!ImageConstant.ImageMediaType.RootVolumeTemplate.toString().equals(msg.getMediaType())) {
            return;
        }

        if (msg.getPlatform() == null) {
            String platform = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).select(VmInstanceVO_.platform).findValue();
            msg.setPlatform(platform == null ? ImagePlatform.Linux.toString() : platform);
        }

        if (msg.getGuestOsType() == null) {
            String guestOsType = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).select(VmInstanceVO_.guestOsType).findValue();
            msg.setGuestOsType(guestOsType);
        }

        if (msg.getArchitecture() == null) {
            String architecture = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).select(VmInstanceVO_.architecture).findValue();
            msg.setArchitecture(architecture);
        }

        msg.setVirtio(VmExtraInfoGetter.New(vmUuid).isVirtio());

        setBootModeIfAarch64(msg);
    }

    public static void fillFromVolume(AddImageMessage msg, String volumeUuid) {
        String vmUuid = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volumeUuid).select(VolumeVO_.vmInstanceUuid).findValue();
        if (vmUuid == null) {
            fillDefault(msg);
        } else {
            fillFromVm(msg, vmUuid);
        }
    }

    public static void fillFromSnapshot(AddImageMessage msg, String volumeSnapshotUuid) {
        String vmUuid = SQL.New("select vol.vmInstanceUuid from VolumeVO vol, VolumeSnapshotVO snap" +
                " where snap.uuid = :snapUuid" +
                " and vol.uuid = snap.volumeUuid", String.class)
                .param("snapUuid", volumeSnapshotUuid)
                .find();

        if (vmUuid == null) {
            fillDefault(msg);
        } else {
            fillFromVm(msg, vmUuid);
        }
    }

    private static void setBootModeIfAarch64(AddImageMessage msg) {
        if (ImageArchitecture.aarch64.toString().equals(msg.getArchitecture())) {
            if (msg.getSystemTags() != null) {
                msg.getSystemTags().removeIf(tag -> ImageSystemTags.BOOT_MODE.isMatch(tag));
            }

            msg.addSystemTag(ImageSystemTags.BOOT_MODE.instantiateTag(Collections.singletonMap(
                    ImageSystemTags.BOOT_MODE_TOKEN, ImageBootMode.UEFI.toString()
            )));
        }
    }
}
