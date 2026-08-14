package org.zstack.storage.device.hba;


import org.zstack.header.host.HostAO;
import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/9/18 15:57
 */
@StaticMetamodel(HbaDeviceVO.class)
public class HbaDeviceVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<HbaDeviceVO, String> name;
    public static volatile SingularAttribute<HbaDeviceVO, String> hostUuid;
    public static volatile SingularAttribute<HbaDeviceVO, HbaType> hbaType;
    public static volatile SingularAttribute<HostAO, Timestamp> createDate;
    public static volatile SingularAttribute<HostAO, Timestamp> lastOpDate;

}
