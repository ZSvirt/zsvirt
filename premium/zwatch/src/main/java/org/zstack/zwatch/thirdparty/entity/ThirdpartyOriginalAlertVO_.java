package org.zstack.zwatch.thirdparty.entity;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ThirdpartyOriginalAlertVO.class)
public class ThirdpartyOriginalAlertVO_ {
    public static volatile SingularAttribute<ThirdpartyOriginalAlertVO, String> uuid;
    public static volatile SingularAttribute<ThirdpartyOriginalAlertVO, String> thirdpartyPlatformUuid;
    public static volatile SingularAttribute<ThirdpartyOriginalAlertVO, String> product;
    public static volatile SingularAttribute<ThirdpartyOriginalAlertVO, String> service;
    public static volatile SingularAttribute<ThirdpartyOriginalAlertVO, String> metric;
    public static volatile SingularAttribute<ThirdpartyOriginalAlertVO, String> alertLevel;
    public static volatile SingularAttribute<ThirdpartyOriginalAlertVO, Timestamp> alertTime;
    public static volatile SingularAttribute<ThirdpartyOriginalAlertVO, String> dimensions;
    public static volatile SingularAttribute<ThirdpartyOriginalAlertVO, String> message;
    public static volatile SingularAttribute<ThirdpartyOriginalAlertVO, String> dataSource;
    public static volatile SingularAttribute<ThirdpartyOriginalAlertVO, String> sourceText;
    public static volatile SingularAttribute<ThirdpartyOriginalAlertVO, String> readStatus;
    public static volatile SingularAttribute<ThirdpartyOriginalAlertVO, Timestamp> createDate;
}
