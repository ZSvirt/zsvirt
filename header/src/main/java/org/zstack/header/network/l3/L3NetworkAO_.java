package org.zstack.header.network.l3;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(L3NetworkAO.class)
public class L3NetworkAO_ extends ResourceVO_ {
    public static volatile SingularAttribute<L3NetworkAO, String> name;
    public static volatile SingularAttribute<L3NetworkAO, String> type;
    public static volatile SingularAttribute<L3NetworkAO, Boolean> system;
    public static volatile SingularAttribute<L3NetworkAO, L3NetworkState> state;
    public static volatile SingularAttribute<L3NetworkAO, L3NetworkCategory> category;
    public static volatile SingularAttribute<L3NetworkAO, String> description;
    public static volatile SingularAttribute<L3NetworkAO, String> dnsDomain;
    public static volatile SingularAttribute<L3NetworkAO, String> zoneUuid;
    public static volatile SingularAttribute<L3NetworkAO, String> l2NetworkUuid;
    public static volatile SingularAttribute<L3NetworkAO, Integer> ipVersion;
    public static volatile SingularAttribute<L3NetworkAO, Boolean> enableIPAM;
    public static volatile SingularAttribute<L3NetworkAO, Timestamp> createDate;
    public static volatile SingularAttribute<L3NetworkAO, Timestamp> lastOpDate;
}
