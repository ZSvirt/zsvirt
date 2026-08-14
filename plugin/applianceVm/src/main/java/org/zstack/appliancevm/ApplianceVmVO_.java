package org.zstack.appliancevm;

import org.zstack.header.vm.VmInstanceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 12:13 AM
 * To change this template use File | Settings | File Templates.
 */
@StaticMetamodel(ApplianceVmVO.class)
public class ApplianceVmVO_ extends VmInstanceVO_ {
    public static volatile SingularAttribute<ApplianceVmVO, String> applianceVmType;
    public static volatile SingularAttribute<ApplianceVmVO, String> managementNetworkUuid;
    public static volatile SingularAttribute<ApplianceVmVO, String> defaultRouteL3NetworkUuid;
    public static volatile SingularAttribute<ApplianceVmVO, ApplianceVmStatus> status;
    public static volatile SingularAttribute<ApplianceVmVO, ApplianceVmHaStatus> haStatus;
}
