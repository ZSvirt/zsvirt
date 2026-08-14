package org.zstack.sns.platform.wecom;

import org.zstack.sns.SNSApplicationEndpointVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SNSWeComEndpointVO.class)
public class SNSWeComEndpointVO_ extends SNSApplicationEndpointVO_ {
    public static volatile SingularAttribute<SNSWeComEndpointVO, String> url;
    public static volatile SingularAttribute<SNSWeComEndpointVO, Boolean> atAll;
}
