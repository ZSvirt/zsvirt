package org.zstack.header.storage.primary;

import org.zstack.header.image.ImageConstant.ImageMediaType;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ImageCacheVO.class)
public class ImageCacheVO_ {
    public static volatile SingularAttribute<ImageCacheVO, Long> id;
    public static volatile SingularAttribute<ImageCacheVO, String> primaryStorageUuid;
    public static volatile SingularAttribute<ImageCacheVO, String> imageUuid;
    public static volatile SingularAttribute<ImageCacheVO, String> installUrl;
    public static volatile SingularAttribute<ImageCacheVO, String> md5sum;
    public static volatile SingularAttribute<ImageCacheVO, Long> size;
    public static volatile SingularAttribute<ImageCacheVO, Timestamp> createDate;
    public static volatile SingularAttribute<ImageCacheVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<ImageCacheVO, ImageMediaType> mediaType;
    public static volatile SingularAttribute<ImageCacheVO, ImageCacheState> state;
}
