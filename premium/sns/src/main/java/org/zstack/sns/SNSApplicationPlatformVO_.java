package org.zstack.sns;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SNSApplicationPlatformVO.class)
public class SNSApplicationPlatformVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SNSApplicationPlatformVO, String> name;
    public static volatile SingularAttribute<SNSApplicationPlatformVO, String> description;
    public static volatile SingularAttribute<SNSApplicationPlatformVO, String> type;
    public static volatile SingularAttribute<SNSApplicationPlatformVO, SNSApplicationPlatformState> state;
    public static volatile SingularAttribute<SNSApplicationPlatformVO, Timestamp> createDate;
    public static volatile SingularAttribute<SNSApplicationPlatformVO, Timestamp> lastOpDate;
}

