package org.zstack.pciDevice.specification.mdev;

import org.zstack.header.vo.ResourceVO_;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceType;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 2019-04-17.
 */
@StaticMetamodel(MdevDeviceSpecVO.class)
public class MdevDeviceSpecVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<MdevDeviceSpecVO, String> name;
    public static volatile SingularAttribute<MdevDeviceSpecVO, String> description;
    public static volatile SingularAttribute<MdevDeviceSpecVO, String> specification;
    public static volatile SingularAttribute<MdevDeviceSpecVO, MdevDeviceType> type;
    public static volatile SingularAttribute<MdevDeviceSpecVO, MdevDeviceSpecState> state;
    public static volatile SingularAttribute<MdevDeviceSpecVO, Timestamp> createDate;
    public static volatile SingularAttribute<MdevDeviceSpecVO, Timestamp> lastOpDate;
}
