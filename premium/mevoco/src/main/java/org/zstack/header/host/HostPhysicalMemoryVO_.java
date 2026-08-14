package org.zstack.header.host;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * author:kaicai.hu
 * Date:2021/8/5
 */
@StaticMetamodel(HostPhysicalMemoryVO.class)
public class HostPhysicalMemoryVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<HostPhysicalMemoryVO, String> hostUuid;
    public static volatile SingularAttribute<HostPhysicalMemoryVO, String> manufacturer;
    public static volatile SingularAttribute<HostPhysicalMemoryVO, String> size;
    public static volatile SingularAttribute<HostPhysicalMemoryVO, String> speed;
    public static volatile SingularAttribute<HostPhysicalMemoryVO, String> clockSpeed;
    public static volatile SingularAttribute<HostPhysicalMemoryVO, String> locator;
    public static volatile SingularAttribute<HostPhysicalMemoryVO, String> serialNumber;
    public static volatile SingularAttribute<HostPhysicalMemoryVO, String> rank;
    public static volatile SingularAttribute<HostPhysicalMemoryVO, String> voltage;
    public static volatile SingularAttribute<HostPhysicalMemoryVO, String> type;
    public static volatile SingularAttribute<HostPhysicalMemoryVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostPhysicalMemoryVO, Timestamp> lastOpDate;
}
