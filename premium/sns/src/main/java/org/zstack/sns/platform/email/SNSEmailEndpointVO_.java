package org.zstack.sns.platform.email;

import org.zstack.sns.SNSApplicationEndpointVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SNSEmailEndpointVO.class)
public class SNSEmailEndpointVO_ extends SNSApplicationEndpointVO_ {
    public static volatile SingularAttribute<SNSEmailEndpointVO, String> email;
}
