package org.zstack.zwatch.thirdparty.entity;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SNSEndpointThirdpartyAlertHistoryVO.class)
public class SNSEndpointThirdpartyAlertHistoryVO_ {
    public static volatile SingularAttribute<SNSEndpointThirdpartyAlertHistoryVO, String> alertUuid;
    public static volatile SingularAttribute<SNSEndpointThirdpartyAlertHistoryVO, String> endpointUuid;
    public static volatile SingularAttribute<SNSEndpointThirdpartyAlertHistoryVO, String> subscriptionUuid;
    public static volatile SingularAttribute<SNSEndpointThirdpartyAlertHistoryVO, Timestamp> createDate;
}
