package org.zstack.softwarePackage.entity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SoftwarePackageVO.class)
public class SoftwarePackageVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SoftwarePackageVO, String> name;
    public static volatile SingularAttribute<SoftwarePackageVO, String> hostUuid;
    public static volatile SingularAttribute<SoftwarePackageVO, String> managementNodeUuid;
    public static volatile SingularAttribute<SoftwarePackageVO, String> installPath;
    public static volatile SingularAttribute<SoftwarePackageVO, String> unzipInstallPath;
    public static volatile SingularAttribute<SoftwarePackageVO, String> type;
    public static volatile SingularAttribute<SoftwarePackageVO, String> md5sum;
    public static volatile SingularAttribute<SoftwarePackageVO, String> status;
    public static volatile SingularAttribute<SoftwarePackageVO, Long> size;
    public static volatile SingularAttribute<SoftwarePackageVO, Timestamp> createDate;
    public static volatile SingularAttribute<SoftwarePackageVO, Timestamp> lastOpDate;
}