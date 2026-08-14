package org.zstack.network.service.virtualrouter.ha;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.service.VirtualRouterHaGroupExtensionPoint;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerRefVO;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerRefVO_;

import java.util.ArrayList;
import java.util.List;

public abstract class VirtualRouterConfigProxy {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry  pluginRgty;

    protected abstract void attachNetworkServiceToNoHaVirtualRouter(String vrUuid, String type, List<String> serviceUuids);
    protected abstract void detachNetworkServiceFromNoHaVirtualRouter(String vrUuid, String type, List<String> serviceUuids);
    protected abstract List<String> getNoHaVirtualRouterUuidsByNetworkService(String serviceUuid);
    protected abstract List<String> getServiceUuidsByNoHaVirtualRouter(String vrUuid);

    public void attachNetworkService(String vrUuid, String type, List<String> serviceUuids) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVo.isHaEnabled()) {
            attachNetworkServiceToNoHaVirtualRouter(vrUuid, type, serviceUuids);
        } else {
            for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
                ext.attachNetworkServiceToHaRouter(type, serviceUuids, vrUuid);
            }
        }
    }

    final public void detachNetworkService(String vrUuid, String type, List<String> serviceUuids) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVo.isHaEnabled()) {
            detachNetworkServiceFromNoHaVirtualRouter(vrUuid, type, serviceUuids);
        } else {
            for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
                ext.detachNetworkServiceFromHaRouter(type, serviceUuids, vrUuid);
            }
        }
    }

    final public List<String> getVrUuidsByNetworkService(String type, String serviceUuid) {
        List<String> vrUuids = getNoHaVirtualRouterUuidsByNetworkService(serviceUuid);
        if (vrUuids != null && vrUuids.size() == 1) {
            return vrUuids;
        }

        List<VirtualRouterHaGroupExtensionPoint> exps = pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class);
        if (exps != null && !exps.isEmpty()) {
            vrUuids = exps.get(0).getHaVrUuidsFromNetworkService(type, serviceUuid);
        }

        return vrUuids;
    }

    final public List<String> getVrUuidsByNetworkService(String type) {
        List<String> vrUuids  = Q.New(VirtualRouterLoadBalancerRefVO.class).select(VirtualRouterLoadBalancerRefVO_.virtualRouterVmUuid).listValues();

        List<VirtualRouterHaGroupExtensionPoint> exps = pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class);
        if (exps != null && !exps.isEmpty()) {
            List<String> vrUuidsFromHaGroup = exps.get(0).getHaVrUuidsFromNetworkService(type);
            if ( !vrUuidsFromHaGroup.isEmpty() ) {
                vrUuids.addAll(vrUuidsFromHaGroup);
            }
        }

        return vrUuids;
    }


    final public List<String> getServiceUuidsByRouterUuid(String vrUuid, String type) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVo.isHaEnabled()) {
            return getServiceUuidsByNoHaVirtualRouter(vrUuid);
        } else {
            List<String> services = new ArrayList<>();
            for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
                services = ext.getNetworkServicesFromHaVrUuid(type, vrUuid);
            }

            return services;
        }
    }

    final public List<String> getServiceUuidsByHaGrupUuid(String haGroupUuid, String type) {
        List<String> services = new ArrayList<>();
        for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
            services = ext.getNetworkServicesFromHaGroupUuid(type, haGroupUuid);
        }

        return services;
    }
}

