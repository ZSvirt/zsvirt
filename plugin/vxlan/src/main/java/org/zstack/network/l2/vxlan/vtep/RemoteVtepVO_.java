package org.zstack.network.l2.vxlan.vtep;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(RemoteVtepVO.class)
public class RemoteVtepVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<RemoteVtepVO, String> vtepIp;
    public static volatile SingularAttribute<RemoteVtepVO, Integer> port;
    public static volatile SingularAttribute<RemoteVtepVO, String> clusterUuid;
    public static volatile SingularAttribute<RemoteVtepVO, String> type;
    public static volatile SingularAttribute<RemoteVtepVO, String> poolUuid;
    public static volatile SingularAttribute<RemoteVtepVO, Timestamp> createDate;
    public static volatile SingularAttribute<RemoteVtepVO, Timestamp> lastOpDate;
}
