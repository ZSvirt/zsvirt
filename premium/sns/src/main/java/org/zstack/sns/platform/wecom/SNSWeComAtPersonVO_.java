package org.zstack.sns.platform.wecom;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SNSWeComAtPersonVO.class)
public class SNSWeComAtPersonVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SNSWeComAtPersonVO, String> userId;
    public static volatile SingularAttribute<SNSWeComAtPersonVO, String> endpointUuid;
}
