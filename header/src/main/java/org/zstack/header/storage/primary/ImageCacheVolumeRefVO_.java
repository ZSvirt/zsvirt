package org.zstack.header.storage.primary;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by frank on 8/5/2015.
 */
@StaticMetamodel(ImageCacheVolumeRefVO.class)
public class ImageCacheVolumeRefVO_ {
    public static volatile SingularAttribute<ImageCacheVolumeRefVO, Long> id;
    public static volatile SingularAttribute<ImageCacheVolumeRefVO, Long> imageCacheId;
    public static volatile SingularAttribute<ImageCacheVolumeRefVO, String> volumeUuid;
    public static volatile SingularAttribute<ImageCacheVolumeRefVO, String> primaryStorageUuid;
}
