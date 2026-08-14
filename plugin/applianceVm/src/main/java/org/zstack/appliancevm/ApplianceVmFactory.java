package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.vm.*;
import org.zstack.utils.DebugUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 12:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class ApplianceVmFactory implements VmInstanceFactory, Component {
    public static VmInstanceType type = new VmInstanceType(ApplianceVmConstant.APPLIANCE_VM_TYPE);

    static {
        type.setSriovSupported(true);
    }

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    private Map<String, ApplianceVmSubTypeFactory> subTypeFactories = new HashMap<String, ApplianceVmSubTypeFactory>();

    @Override
    public VmInstanceType getType() {
        return type;
    }

    @Override
    @Transactional
    public VmInstanceVO createVmInstance(VmInstanceVO vo, CreateVmInstanceMsg msg) {
        dbf.getEntityManager().persist(vo);
        return vo;
    }

    @Override
    public VmInstance getVmInstance(VmInstanceVO vo) {
        ApplianceVmVO self = dbf.findByUuid(vo.getUuid(), ApplianceVmVO.class);
        ApplianceVmSubTypeFactory subTypeFactory = getApplianceVmSubTypeFactory(self.getApplianceVmType());
        return subTypeFactory.getSubApplianceVm(self);
    }

    @Override
    public boolean start() {
        for (ApplianceVmSubTypeFactory ext : pluginRgty.getExtensionList(ApplianceVmSubTypeFactory.class)) {
            ApplianceVmSubTypeFactory old = subTypeFactories.get(ext.getApplianceVmType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate ApplianceVmSubTypeFactory[%s, %s] for type[%s]",
                        old.getClass(), ext.getClass(), ext.getApplianceVmType()));
            }
            subTypeFactories.put(ext.getApplianceVmType().toString(), ext);
        }
        return true;
    }

    public ApplianceVmSubTypeFactory getApplianceVmSubTypeFactory(String subtype) {
        ApplianceVmSubTypeFactory subTypeFactory = subTypeFactories.get(subtype);
        DebugUtils.Assert(subTypeFactory!=null, String.format("cannot find ApplianceVmSubTypeFactory for type[%s]", subtype));
        return subTypeFactory;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
