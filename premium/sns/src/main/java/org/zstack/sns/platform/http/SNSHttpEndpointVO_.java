package org.zstack.sns.platform.http;

import org.zstack.sns.SNSApplicationEndpointVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SNSHttpEndpointVO.class)
public class SNSHttpEndpointVO_ extends SNSApplicationEndpointVO_ {
    public static volatile SingularAttribute<SNSHttpEndpointVO, String> url;
    public static volatile SingularAttribute<SNSHttpEndpointVO, String> username;
    public static volatile SingularAttribute<SNSHttpEndpointVO, String> password;
}
