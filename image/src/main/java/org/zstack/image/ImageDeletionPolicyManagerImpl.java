package org.zstack.image;

import org.zstack.header.image.ImageDeletionPolicyManager;

/**
 * Created by frank on 11/15/2015.
 */
public class ImageDeletionPolicyManagerImpl implements ImageDeletionPolicyManager {
    @Override
    public ImageDeletionPolicy getDeletionPolicy(String imageUuid) {
        return ImageDeletionPolicy.valueOf(ImageGlobalConfig.DELETION_POLICY.value());
    }
}
