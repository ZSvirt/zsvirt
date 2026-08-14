package org.zstack.vrouterRoute;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by weiwang on 15/06/2017.
 */
@StaticMetamodel(VRouterRouteEntryVO.class)
public class VRouterRouteEntryVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VRouterRouteEntryVO, String> routeTableUuid;
    public static volatile SingularAttribute<VRouterRouteEntryVO, String> description;
    public static volatile SingularAttribute<VRouterRouteEntryVO, VRouterRouteEntryType> type;
    public static volatile SingularAttribute<VRouterRouteEntryVO, String> destination;
    public static volatile SingularAttribute<VRouterRouteEntryVO, String> target;
    public static volatile SingularAttribute<VRouterRouteEntryVO, Integer> distance;
    public static volatile SingularAttribute<VRouterRouteEntryVO, Timestamp> createDate;
    public static volatile SingularAttribute<VRouterRouteEntryVO, Timestamp> lastOpDate;
}
