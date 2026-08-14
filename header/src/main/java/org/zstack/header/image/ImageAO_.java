package org.zstack.header.image;

import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ImageAO.class)
public class ImageAO_ extends ResourceVO_ {
    public static volatile SingularAttribute<ImageAO, String> name;
    public static volatile SingularAttribute<ImageAO, String> description;
    public static volatile SingularAttribute<ImageAO, ImageStatus> status;
    public static volatile SingularAttribute<ImageAO, ImageState> state;
    public static volatile SingularAttribute<ImageAO, Long> size;
    public static volatile SingularAttribute<ImageAO, Long> actualSize;
    public static volatile SingularAttribute<ImageAO, String> md5Sum;
    public static volatile SingularAttribute<ImageAO, String> type;
    public static volatile SingularAttribute<ImageAO, String> url;
    public static volatile SingularAttribute<ImageAO, Boolean> system;
    public static volatile SingularAttribute<ImageAO, ImagePlatform> platform;
    public static volatile SingularAttribute<ImageAO, ImagePlatform> architecture;
    public static volatile SingularAttribute<ImageAO, ImageMediaType> mediaType;
    public static volatile SingularAttribute<ImageAO, Timestamp> createDate;
    public static volatile SingularAttribute<ImageAO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<ImageAO, String> hypervisorType;
    public static volatile SingularAttribute<ImageAO, String> format;
    public static volatile SingularAttribute<ImageAO, String> guestOsType;
    public static volatile SingularAttribute<ImageAO, String> backupStorageUuid;
    public static volatile SingularAttribute<ImageAO, Boolean> virtio;
}
