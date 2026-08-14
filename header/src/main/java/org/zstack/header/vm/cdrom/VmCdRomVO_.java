package org.zstack.header.vm.cdrom;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Create by lining at 2018/12/26
 */
@StaticMetamodel(VmCdRomVO.class)
public class VmCdRomVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VmCdRomVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<VmCdRomVO, String> name;
    public static volatile SingularAttribute<VmCdRomVO, String> isoUuid;
    public static volatile SingularAttribute<VmCdRomVO, String> isoInstallPath;
    public static volatile SingularAttribute<VmCdRomVO, String> description;
    public static volatile SingularAttribute<VmCdRomVO, Integer> deviceId;
    public static volatile SingularAttribute<VmCdRomVO, String> occupant;
    public static volatile SingularAttribute<VmCdRomVO, String> protocol;
    public static volatile SingularAttribute<VmCdRomVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmCdRomVO, Timestamp> lastOpDate;
}
