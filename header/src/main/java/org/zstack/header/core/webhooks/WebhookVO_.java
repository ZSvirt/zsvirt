package org.zstack.header.core.webhooks;

/**
 * Created by xing5 on 2017/5/7.
 */

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(WebhookVO.class)
public class WebhookVO_ {
    public static volatile SingularAttribute<WebhookVO, String> uuid;
    public static volatile SingularAttribute<WebhookVO, String> name;
    public static volatile SingularAttribute<WebhookVO, String> description;
    public static volatile SingularAttribute<WebhookVO, String> type;
    public static volatile SingularAttribute<WebhookVO, String> url;
    public static volatile SingularAttribute<WebhookVO, String> opaque;
    public static volatile SingularAttribute<WebhookVO, Timestamp> createDate;
    public static volatile SingularAttribute<WebhookVO, Timestamp> lastOpDate;
}
