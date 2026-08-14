package org.zstack.header.sriov;

import org.zstack.header.vm.VmNicVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by GuoYi on 11/28/19.
 */
@StaticMetamodel(VmVfNicVO.class)
public class VmVfNicVO_ extends VmNicVO_ {
    public static volatile SingularAttribute<VmVfNicVO, String> pciDeviceUuid;
    public static volatile SingularAttribute<VmVfNicVO, VmVfNicHaState> haState;
}
