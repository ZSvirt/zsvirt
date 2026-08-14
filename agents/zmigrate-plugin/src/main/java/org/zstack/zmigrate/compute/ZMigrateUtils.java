package org.zstack.zmigrate.compute;

import org.zstack.core.db.Q;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;
import org.zstack.softwarePackage.entity.SoftwarePackageVO_;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTag;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.zstack.core.Platform.err;
import static org.zstack.zmigrate.ZMigrateConstant.ZMIGRATE_SOFTWARE_PACKAGE_TYPE;
import static org.zstack.zmigrate.ZMigratePluginErrors.*;
import static org.zstack.zmigrate.ZMigrateSystemTags.*;

public class ZMigrateUtils {
    public static String findZMigrateVmUuid(SystemTag tag) {
        List<String> uuids = findZMigrateVmUuids(tag);
        if (uuids.isEmpty()) {
            return null;
        }
        if (uuids.size() > 1) {
            throw err(INVALID_ZMIGRATE_TAGS,
                    "expected exactly 1 VM with tag[%s], but found %d: %s. Please remove or untag the stale VMs before proceeding",
                    tag.getTagFormat(), uuids.size(), uuids)
                    .toException();
        }
        return uuids.get(0);
    }

    public static List<String> findZMigrateVmUuids(SystemTag tag) {
        return Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.tag, tag.getTagFormat())
                .eq(SystemTagVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .select(SystemTagVO_.resourceUuid)
                .listValues();
    }

    public static String getSoftwarePackageUuid() {
        return Q.New(SoftwarePackageVO.class)
                .eq(SoftwarePackageVO_.type, ZMIGRATE_SOFTWARE_PACKAGE_TYPE)
                .select(SoftwarePackageVO_.uuid).findValue();
    }

    public static Map<String, ImageInventory> getZMigrateImages() {
        return getImagesByTags(
                new PatternedSystemTag[]{ZMIGRATE_GATEWAY_IMAGE, ZMIGRATE_LINUX_BOOT_IMAGE, ZMIGRATE_WINDOWS_BOOT_IMAGE},
                new String[]{ZMIGRATE_GATEWAY_IMAGE_TOKEN, ZMIGRATE_LINUX_BOOT_IMAGE_TOKEN, ZMIGRATE_WINDOWS_BOOT_IMAGE_TOKEN}
        );
    }

    public static Map<String, ImageInventory> getZMigrateUpgradeImages() {
        return getImagesByTags(
                new PatternedSystemTag[]{ZMIGRATE_GATEWAY_UPGRADE_IMAGE, ZMIGRATE_LINUX_BOOT_UPGRADE_IMAGE, ZMIGRATE_WINDOWS_BOOT_UPGRADE_IMAGE},
                new String[]{ZMIGRATE_GATEWAY_UPGRADE_IMAGE_TOKEN, ZMIGRATE_LINUX_BOOT_UPGRADE_IMAGE_TOKEN, ZMIGRATE_WINDOWS_BOOT_UPGRADE_IMAGE_TOKEN}
        );
    }

    private static Map<String, ImageInventory> getImagesByTags(PatternedSystemTag[] tags, String[] tokenNames) {
        String softwarePackageUuid = getSoftwarePackageUuid();
        String[] imageUuidsByToken = new String[tags.length];
        for (int i = 0; i < tags.length; i++) {
            imageUuidsByToken[i] = getTokenFromTag(tags[i], tokenNames[i], softwarePackageUuid);
        }

        List<String> imageUuids = Stream.of(imageUuidsByToken).filter(Objects::nonNull).collect(Collectors.toList());
        if (imageUuids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ImageVO> images = Q.New(ImageVO.class).in(ImageVO_.uuid, imageUuids).list();

        Map<String, ImageInventory> imagesMap = new HashMap<>();
        for (ImageVO image : images) {
            for (int i = 0; i < tags.length; i++) {
                if (Objects.equals(image.getUuid(), imageUuidsByToken[i])) {
                    imagesMap.put(tokenNames[i], ImageInventory.valueOf(image));
                }
            }
        }
        return imagesMap;
    }

    public static String getTokenFromTag(PatternedSystemTag systemTag, String tokenName, String softwarePackageUuid) {
        if (softwarePackageUuid == null) {
            return null;
        }
        return systemTag.getTokenByResourceUuid(softwarePackageUuid, tokenName);
    }
}
