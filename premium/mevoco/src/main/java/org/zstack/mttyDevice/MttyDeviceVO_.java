package org.zstack.mttyDevice;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * @author yu.sun
 * @date 2022/11/17 09:09
 **/
@StaticMetamodel(MttyDeviceVO.class)
public class MttyDeviceVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<MttyDeviceVO, String> name;
    public static volatile SingularAttribute<MttyDeviceVO, String> description;
    public static volatile SingularAttribute<MttyDeviceVO, String> hostUuid;
    public static volatile SingularAttribute<MttyDeviceVO, MttyDeviceState> state;
    public static volatile SingularAttribute<MttyDeviceVO, MttyDeviceVirtStatus> virtStatus;
    public static volatile SingularAttribute<MttyDeviceVO, MttyDeviceType> type;
    public static volatile SingularAttribute<MttyDeviceVO, Timestamp> createDate;
    public static volatile SingularAttribute<MttyDeviceVO, Timestamp> lastOpDate;
}