package org.zstack.log.server;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(LogServerVO.class)
public class LogServerVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<LogServerVO, String> name;
    public static volatile SingularAttribute<LogServerVO, String> description;
    public static volatile SingularAttribute<LogServerVO, LogCategory> category;
    public static volatile SingularAttribute<LogServerVO, LogType> type;
    public static volatile SingularAttribute<LogServerVO, LogLevel> level;
    public static volatile SingularAttribute<LogServerVO, String> configuration;
    public static volatile SingularAttribute<LogServerVO, Timestamp> createDate;
    public static volatile SingularAttribute<LogServerVO, Timestamp> lastOpDate;
}
