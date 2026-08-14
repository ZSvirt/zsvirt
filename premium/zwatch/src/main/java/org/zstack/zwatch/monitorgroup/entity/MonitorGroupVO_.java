package org.zstack.zwatch.monitorgroup.entity;

import org.zstack.header.vo.ResourceVO_;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupState;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(MonitorGroupVO.class)
public class MonitorGroupVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<MonitorGroupVO, String> name;
    public static volatile SingularAttribute<MonitorGroupVO, MonitorGroupState> state;
    public static volatile SingularAttribute<MonitorGroupVO, String> actions;
    public static volatile SingularAttribute<MonitorGroupVO, String> description;
    public static volatile SingularAttribute<MonitorGroupVO, Timestamp> createDate;
    public static volatile SingularAttribute<MonitorGroupVO, Timestamp> lastOpDate;
}
