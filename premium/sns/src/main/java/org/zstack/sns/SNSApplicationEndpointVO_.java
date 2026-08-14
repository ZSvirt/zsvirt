package org.zstack.sns;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SNSApplicationEndpointVO.class)
public class SNSApplicationEndpointVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SNSApplicationEndpointVO, String> name;
    public static volatile SingularAttribute<SNSApplicationEndpointVO, String> description;
    public static volatile SingularAttribute<SNSApplicationEndpointVO, String> type;
    public static volatile SingularAttribute<SNSApplicationEndpointVO, String> platformUuid;
    public static volatile SingularAttribute<SNSApplicationEndpointVO, SNSApplicationEndpointOwnerType> ownerType;
    public static volatile SingularAttribute<SNSApplicationEndpointVO, SNSApplicationEndpointState> state;
    public static volatile SingularAttribute<SNSApplicationEndpointVO, Timestamp> createDate;
    public static volatile SingularAttribute<SNSApplicationEndpointVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<SNSApplicationEndpointVO, String> connectionStatus;
}
