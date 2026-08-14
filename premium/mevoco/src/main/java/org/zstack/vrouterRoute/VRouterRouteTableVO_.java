package org.zstack.vrouterRoute;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by weiwang on 15/06/2017.
 */
@StaticMetamodel(VRouterRouteTableVO.class)
public class VRouterRouteTableVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VRouterRouteTableVO, String> name;
    public static volatile SingularAttribute<VRouterRouteTableVO, String> description;
    public static volatile SingularAttribute<VRouterRouteTableVO, String> type;
    public static volatile SingularAttribute<VRouterRouteTableVO, Timestamp> createDate;
    public static volatile SingularAttribute<VRouterRouteTableVO, Timestamp> lastOpDate;
}
