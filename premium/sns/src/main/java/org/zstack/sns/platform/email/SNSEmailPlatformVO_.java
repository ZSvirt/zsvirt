package org.zstack.sns.platform.email;

import org.zstack.sns.SNSApplicationPlatformVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SNSEmailPlatformVO.class)
public class SNSEmailPlatformVO_ extends SNSApplicationPlatformVO_ {
    public static volatile SingularAttribute<SNSEmailPlatformVO, String> smtpServer;
    public static volatile SingularAttribute<SNSEmailPlatformVO, String> smtpPort;
    public static volatile SingularAttribute<SNSEmailPlatformVO, String> username;
    public static volatile SingularAttribute<SNSEmailPlatformVO, String> password;
}
