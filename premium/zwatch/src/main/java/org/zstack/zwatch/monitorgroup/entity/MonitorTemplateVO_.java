package org.zstack.zwatch.monitorgroup.entity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(MonitorTemplateVO.class)
public class MonitorTemplateVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<MonitorTemplateVO, String> name;
    public static volatile SingularAttribute<MonitorTemplateVO, String> description;
    public static volatile SingularAttribute<MonitorTemplateVO, Timestamp> createDate;
    public static volatile SingularAttribute<MonitorTemplateVO, Timestamp> lastOpDate;
}
