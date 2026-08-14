package org.zstack.accessKey;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(AccessKeyVO.class)
public class AccessKeyVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<AccessKeyVO, String> accountUuid;
    public static volatile SingularAttribute<AccessKeyVO, String> AccessKeyID;
    public static volatile SingularAttribute<AccessKeyVO, String> AccessKeySecret;
    public static volatile SingularAttribute<AccessKeyVO, String> description;
    public static volatile SingularAttribute<AccessKeyVO, AccessKeyState> state;
    public static volatile SingularAttribute<AccessKeyVO, Timestamp> createDate;
    public static volatile SingularAttribute<AccessKeyVO, Timestamp> lastOpDate;
}
