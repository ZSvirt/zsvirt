package org.zstack.storage.device.hba;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/9/25 09:48
 */
@StaticMetamodel(FcHbaDeviceVO.class)
public class FcHbaDeviceVO_ extends HbaDeviceVO_ {

    public static volatile SingularAttribute<HbaDeviceVO, String> portName;
    public static volatile SingularAttribute<HbaDeviceVO, String> portState;
    public static volatile SingularAttribute<HbaDeviceVO, String> speed;
    public static volatile SingularAttribute<HbaDeviceVO, String> supportedSpeeds;
    public static volatile SingularAttribute<HbaDeviceVO, String> symbolicName;
    public static volatile SingularAttribute<HbaDeviceVO, String> supportedClasses;
    public static volatile SingularAttribute<HbaDeviceVO, String> nodeName;
}
