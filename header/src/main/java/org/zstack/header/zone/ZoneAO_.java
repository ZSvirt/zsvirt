package org.zstack.header.zone;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 */
@StaticMetamodel(ZoneAO.class)
public class ZoneAO_ extends ResourceVO_ {
    public static volatile SingularAttribute<ZoneVO, String> description;
    public static volatile SingularAttribute<ZoneVO, String> name;
    public static volatile SingularAttribute<ZoneVO, String> type;
    public static volatile SingularAttribute<ZoneVO, ZoneState> state;
    public static volatile SingularAttribute<ZoneVO, Boolean> isDefault;
    public static volatile SingularAttribute<ZoneVO, Timestamp> createDate;
    public static volatile SingularAttribute<ZoneVO, Timestamp> lastOpDate;
}
