package org.zstack.pciDevice.virtual.vfio_mdev;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 2019-04-17.
 */
@StaticMetamodel(MdevDeviceVO.class)
public class MdevDeviceVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<MdevDeviceVO, String> name;
    public static volatile SingularAttribute<MdevDeviceVO, String> description;
    public static volatile SingularAttribute<MdevDeviceVO, String> parentUuid;
    public static volatile SingularAttribute<MdevDeviceVO, String> mttyUuid;
    public static volatile SingularAttribute<MdevDeviceVO, String> hostUuid;
    public static volatile SingularAttribute<MdevDeviceVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<MdevDeviceVO, String> mdevSpecUuid;
    public static volatile SingularAttribute<MdevDeviceVO, MdevDeviceType> type;
    public static volatile SingularAttribute<MdevDeviceVO, MdevDeviceState> state;
    public static volatile SingularAttribute<MdevDeviceVO, MdevDeviceStatus> status;
    public static volatile SingularAttribute<MdevDeviceVO, MdevDeviceChooser> chooser;
    public static volatile SingularAttribute<MdevDeviceVO, String> vendor;
    public static volatile SingularAttribute<MdevDeviceVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<MdevDeviceVO, Timestamp> createDate;
}
