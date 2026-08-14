package org.zstack.ovf.datatype;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by Qi Le on 2022/4/27
 */
@StaticMetamodel(ImagePackageVO.class)
public class ImagePackageVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<ImagePackageVO, String> name;
    public static volatile SingularAttribute<ImagePackageVO, String> description;
    public static volatile SingularAttribute<ImagePackageVO, String> vmUuid;
    public static volatile SingularAttribute<ImagePackageVO, String> backupStorageUuid;
    public static volatile SingularAttribute<ImagePackageVO, String> exportUrl;
    public static volatile SingularAttribute<ImagePackageVO, String> md5Sum;
    public static volatile SingularAttribute<ImagePackageVO, String> format;
    public static volatile SingularAttribute<ImagePackageVO, Long> size;
    public static volatile SingularAttribute<ImagePackageVO, Timestamp> createDate;
    public static volatile SingularAttribute<ImagePackageVO, Timestamp> lastOpDate;
}
