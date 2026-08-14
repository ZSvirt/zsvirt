package org.zstack.storage.volume;

import org.zstack.header.volume.VolumeProvisioningStrategy;
import org.zstack.tag.SystemTagCreator;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class VolumeUtils {
    public static void SetVolumeProvisioningStrategy(String volumeUuid, VolumeProvisioningStrategy strategy) {
        SystemTagCreator tagCreator = VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.newSystemTagCreator(volumeUuid);
        tagCreator.setTagByTokens(
                map(
                        e(VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY_TOKEN, strategy.toString())
                )
        );
        tagCreator.inherent = false;
        tagCreator.recreate = true;
        tagCreator.create();
    }

    /**
     *  validate name
     *  1~128 characters, support uppercase and lowercase letters,
     *  digits, underscores, and hyphens; It can only start with
     *  uppercase and lowercase letters; It does not start or end with a space
     */
    public static boolean validateString(String name) {
        String pattern = "^[a-zA-Z][\\w-]{0,127}$";
        return name.matches(pattern);
    }
}
