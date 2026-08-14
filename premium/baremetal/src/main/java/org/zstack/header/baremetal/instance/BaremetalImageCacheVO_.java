package org.zstack.header.baremetal.instance;

import org.zstack.header.image.ImageConstant;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 7/20/18.
 */
@StaticMetamodel(BaremetalImageCacheVO.class)
public class BaremetalImageCacheVO_ {
    public static volatile SingularAttribute<BaremetalImageCacheVO, Long> id;
    public static volatile SingularAttribute<BaremetalImageCacheVO, String> pxeServerUuid;
    public static volatile SingularAttribute<BaremetalImageCacheVO, String> imageUuid;
    public static volatile SingularAttribute<BaremetalImageCacheVO, String> url;
    public static volatile SingularAttribute<BaremetalImageCacheVO, String> installUrl;
    public static volatile SingularAttribute<BaremetalImageCacheVO, ImageConstant.ImageMediaType> mediaType;
    public static volatile SingularAttribute<BaremetalImageCacheVO, Long> size;
    public static volatile SingularAttribute<BaremetalImageCacheVO, Long> actualSize;
    public static volatile SingularAttribute<BaremetalImageCacheVO, String> md5sum;
    public static volatile SingularAttribute<BaremetalImageCacheVO, Long> utilization;
    public static volatile SingularAttribute<BaremetalImageCacheVO, Timestamp> createDate;
    public static volatile SingularAttribute<BaremetalImageCacheVO, Timestamp> lastOpDate;
}
