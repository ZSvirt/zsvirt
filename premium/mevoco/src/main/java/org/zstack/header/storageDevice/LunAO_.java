package org.zstack.header.storageDevice;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(LunAO.class)
public class LunAO_ extends ResourceVO_ {
    public static volatile SingularAttribute<ScsiLunVO, String> name;
    public static volatile SingularAttribute<ScsiLunVO, String> wwid;
    public static volatile SingularAttribute<ScsiLunVO, String> vendor;
    public static volatile SingularAttribute<ScsiLunVO, String> model;
    public static volatile SingularAttribute<ScsiLunVO, String> wwn;
    public static volatile SingularAttribute<ScsiLunVO, String> serial;
    public static volatile SingularAttribute<ScsiLunVO, String> type;
    public static volatile SingularAttribute<ScsiLunVO, String> path;
    public static volatile SingularAttribute<ScsiLunVO, String> state;
    public static volatile SingularAttribute<ScsiLunVO, String> source;
    public static volatile SingularAttribute<ScsiLunVO, Long> size;
    public static volatile SingularAttribute<ScsiLunVO, String> multipathDeviceUuid;
}
