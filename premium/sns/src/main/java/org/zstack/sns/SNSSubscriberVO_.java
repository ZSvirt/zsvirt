package org.zstack.sns;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SNSSubscriberVO.class)
public class SNSSubscriberVO_ {
    public static volatile SingularAttribute<SNSSubscriberVO, String> topicUuid;
    public static volatile SingularAttribute<SNSSubscriberVO, String> endpointUuid;
}
