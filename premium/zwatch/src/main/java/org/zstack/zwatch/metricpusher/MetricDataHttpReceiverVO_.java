package org.zstack.zwatch.metricpusher;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(MetricDataHttpReceiverVO.class)
public class MetricDataHttpReceiverVO_ {
    public static volatile SingularAttribute<MetricDataHttpReceiverVO, String> uuid;
    public static volatile SingularAttribute<MetricDataHttpReceiverVO, String> name;
    public static volatile SingularAttribute<MetricDataHttpReceiverVO, String> url;
    public static volatile SingularAttribute<MetricDataHttpReceiverVO, String> description;
    public static volatile SingularAttribute<MetricDataHttpReceiverVO, Timestamp> createDate;
    public static volatile SingularAttribute<MetricDataHttpReceiverVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<MetricDataHttpReceiverVO, ReceiverState> state;
}
