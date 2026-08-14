package org.zstack.appliancevm;

/**
 */
public interface ApplianceVmSubTypeFactory {
    ApplianceVmType getApplianceVmType();

    ApplianceVm getSubApplianceVm(ApplianceVmVO apvm);

    ApplianceVmVO persistApplianceVm(ApplianceVmSpec spec, ApplianceVmVO apvm);

    void removeApplianceVm(ApplianceVmSpec spec, ApplianceVmVO apvm);
}
