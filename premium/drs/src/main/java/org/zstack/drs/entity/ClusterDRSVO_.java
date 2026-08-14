package org.zstack.drs.entity;

import org.zstack.drs.data.BalancedState;
import org.zstack.drs.data.DRSAutomationLevel;
import org.zstack.drs.data.DRSState;
import org.zstack.header.vo.ResourceVO_;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by lining on 2019/12/12.
 */
@StaticMetamodel(ClusterDRSVO.class)
public class ClusterDRSVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<ClusterDRSVO, String> clusterUuid;
    public static volatile SingularAttribute<ClusterDRSVO, String> name;
    public static volatile SingularAttribute<ClusterDRSVO, String> thresholds;
    public static volatile SingularAttribute<ClusterDRSVO, Integer> thresholdDuration;
    public static volatile SingularAttribute<ClusterDRSVO, DRSState> state;
    public static volatile SingularAttribute<ClusterDRSVO, BalancedState> balancedState;
    public static volatile SingularAttribute<ClusterDRSVO, String> lastAdviceGroupUuid;
    public static volatile SingularAttribute<ClusterDRSVO, DRSAutomationLevel> automationLevel;
    public static volatile SingularAttribute<ClusterDRSVO, String> description;
    public static volatile SingularAttribute<ClusterDRSVO, Timestamp> createDate;
    public static volatile SingularAttribute<ClusterDRSVO, Timestamp> lastOpDate;
}
