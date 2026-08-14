package org.zstack.header.configuration;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;


@StaticMetamodel(InstanceOfferingAO.class)
public class InstanceOfferingAO_ extends ResourceVO_ {
    public static volatile SingularAttribute<InstanceOfferingAO, Integer> cpuNum;
    public static volatile SingularAttribute<InstanceOfferingAO, Integer> cpuSpeed;
    public static volatile SingularAttribute<InstanceOfferingAO, Long> memorySize;
    public static volatile SingularAttribute<InstanceOfferingAO, String> allocatorStrategy;
    public static volatile SingularAttribute<InstanceOfferingAO, String> name;
    public static volatile SingularAttribute<InstanceOfferingAO, String> description;
    public static volatile SingularAttribute<InstanceOfferingAO, InstanceOfferingState> state;
    public static volatile SingularAttribute<InstanceOfferingAO, String> type;
    public static volatile SingularAttribute<InstanceOfferingAO, Integer> sortKey;
    public static volatile SingularAttribute<InstanceOfferingAO, Timestamp> createDate;
    public static volatile SingularAttribute<InstanceOfferingAO, Timestamp> lastOpDate;
}
