package org.zstack.ha;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * @Author: DaoDao
 * @Date: 2023/4/4
 */
@StaticMetamodel(HaStrategyConditionVO.class)
public class HaStrategyConditionVO_ {
    public static volatile SingularAttribute<HaStrategyConditionVO, String> uuid;
    public static volatile SingularAttribute<HaStrategyConditionVO, String> name;
    public static volatile SingularAttribute<HaStrategyConditionVO, String> fencerName;
    public static volatile SingularAttribute<HaStrategyConditionVO, HaStrategyState> state;
    public static volatile SingularAttribute<HaStrategyConditionVO, Timestamp> createDate;
    public static volatile SingularAttribute<HaStrategyConditionVO, Timestamp> lastOpDate;
}
