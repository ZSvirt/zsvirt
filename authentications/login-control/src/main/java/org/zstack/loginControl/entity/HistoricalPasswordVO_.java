package org.zstack.loginControl.entity;

import org.zstack.core.config.GlobalConfigVO;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(HistoricalPasswordVO.class)
public class HistoricalPasswordVO_ {
    public static volatile SingularAttribute<GlobalConfigVO, Long> id;
    public static volatile SingularAttribute<GlobalConfigVO, String> uuid;
    public static volatile SingularAttribute<GlobalConfigVO, String> password;
    public static volatile SingularAttribute<GlobalConfigVO, Timestamp> createDate;
}
