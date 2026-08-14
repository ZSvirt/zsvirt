package org.zstack.monitoring.media;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by xing5 on 2017/6/11.
 */
@StaticMetamodel(EmailMediaVO.class)
public class EmailMediaVO_ extends MediaVO_ {
    public static volatile SingularAttribute<EmailMediaVO, String> smtpServer;
    public static volatile SingularAttribute<EmailMediaVO, Integer> smtpPort;
    public static volatile SingularAttribute<EmailMediaVO, String> username;
    public static volatile SingularAttribute<EmailMediaVO, String> password;
}
