package org.zstack.network.l2.vxlan.vtep;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by weiwang on 02/03/2017.
 */
@StaticMetamodel(VtepVO.class)
public class VtepVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VtepVO, String> hostUuid;
    public static volatile SingularAttribute<VtepVO, String> vtepIp;
    public static volatile SingularAttribute<VtepVO, Integer> port;
    public static volatile SingularAttribute<VtepVO, String> clusterUuid;
    public static volatile SingularAttribute<VtepVO, String> type;
    public static volatile SingularAttribute<VtepVO, String> poolUuid;
    public static volatile SingularAttribute<VtepVO, String> physicalInterface;
    public static volatile SingularAttribute<VtepVO, Timestamp> createDate;
    public static volatile SingularAttribute<VtepVO, Timestamp> lastOpDate;
}
