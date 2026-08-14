package org.zstack.header.vdpa;

import org.zstack.header.vm.VmNicVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VmVdpaNicVO.class)
public class VmVdpaNicVO_ extends VmNicVO_ {
    public static volatile SingularAttribute<VmVdpaNicVO, String> pciDeviceUuid;
    public static volatile SingularAttribute<VmVdpaNicVO, String> lastPciDeviceUuid;
    public static volatile SingularAttribute<VmVdpaNicVO, String> srcPath;
}
