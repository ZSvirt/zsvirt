package org.zstack.header.storage.primary;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(HistoricalUsageAO.class)
public class HistoricalUsageAO_ {
    public static volatile SingularAttribute<HistoricalUsageAO, Long> id;
    public static volatile SingularAttribute<HistoricalUsageAO, String> resourceUuid;
    public static volatile SingularAttribute<HistoricalUsageAO, String> resourceType;
    public static volatile SingularAttribute<HistoricalUsageAO, Long> totalPhysicalCapacity;
    public static volatile SingularAttribute<HistoricalUsageAO, Long> usedPhysicalCapacity;
    public static volatile SingularAttribute<HistoricalUsageAO, Long> historicalForecast;
    public static volatile SingularAttribute<HistoricalUsageAO, Timestamp> recordDate;
    public static volatile SingularAttribute<HistoricalUsageAO, Timestamp> createDate;
    public static volatile SingularAttribute<HistoricalUsageAO, Timestamp> lastOpDate;
}
