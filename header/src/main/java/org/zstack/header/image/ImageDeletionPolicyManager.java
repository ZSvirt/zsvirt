package org.zstack.header.image;

/**
 * Created by frank on 11/15/2015.
 */
public interface ImageDeletionPolicyManager {
    enum ImageDeletionPolicy {
        Direct,
        Delay,
        Never,
        DeleteReference
    }

    ImageDeletionPolicy getDeletionPolicy(String imageUuid);
}
