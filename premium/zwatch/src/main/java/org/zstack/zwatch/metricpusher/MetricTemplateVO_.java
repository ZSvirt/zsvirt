package org.zstack.zwatch.metricpusher;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(MetricTemplateVO.class)
public class MetricTemplateVO_ {
    public static volatile SingularAttribute<MetricTemplateVO, String> uuid;
    public static volatile SingularAttribute<MetricTemplateVO, String> receiverUuid;
    public static volatile SingularAttribute<MetricTemplateVO, String> template;
    public static volatile SingularAttribute<MetricTemplateVO, String> namespace;
    public static volatile SingularAttribute<MetricTemplateVO, String> metricName;
    public static volatile SingularAttribute<MetricTemplateVO, String> labelsJsonStr;
    public static volatile SingularAttribute<MetricTemplateVO, String> description;
    public static volatile SingularAttribute<MetricTemplateVO, Timestamp> createDate;
    public static volatile SingularAttribute<MetricTemplateVO, Timestamp> lastOpDate;
}
