package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.header.Component;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.HostReservedCapacityExtensionPoint;
import org.zstack.header.allocator.ReservedHostCapacity;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.RecalculateHostCapacityMsg;
import org.zstack.header.zone.ZoneVO;
import org.zstack.utils.SizeUtils;

/**
 */
public class KvmHostReserveExtension implements HostReservedCapacityExtensionPoint, Component {
    private ReservedHostCapacity reserve = new ReservedHostCapacity();

    @Autowired
    ResourceConfigFacade rcf;
    @Autowired
    CloudBus bus;

    @Override
    public String getHypervisorTypeForHostReserveCapacityExtension() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }

    @Override
    public ReservedHostCapacity getReservedHostCapacity(String hostUuid) {
        ReservedHostCapacity hc = new ReservedHostCapacity();
        hc.setReservedCpuCapacity(reserve.getReservedCpuCapacity());
        String reserveMem = rcf.getResourceConfigValue(KVMGlobalConfig.RESERVED_MEMORY_CAPACITY, hostUuid, String.class);
        hc.setReservedMemoryCapacity(SizeUtils.sizeStringToBytes(reserveMem));
        return hc;
    }

    @Override
    public boolean start() {
        long cpu = SizeUtils.sizeStringToBytes(KVMGlobalConfig.RESERVED_CPU_CAPACITY.value());
        long mem = SizeUtils.sizeStringToBytes(KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.value());
        reserve.setReservedCpuCapacity(cpu);
        reserve.setReservedMemoryCapacity(mem);

        KVMGlobalConfig.RESERVED_CPU_CAPACITY.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                reserve.setReservedCpuCapacity(SizeUtils.sizeStringToBytes(newConfig.value()));
            }
        });
        KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                reserve.setReservedMemoryCapacity(SizeUtils.sizeStringToBytes(newConfig.value()));
            }
        });

        ResourceConfig reservedConfig = rcf.getResourceConfig(KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.getIdentity());
        reservedConfig.installLocalUpdateExtension((config, resourceUuid, resourceType, oldValue, newValue) ->
                recalculateHostCapacity(resourceUuid, resourceType));
        reservedConfig.installLocalDeleteExtension((config, resourceUuid, resourceType, originValue) ->
                recalculateHostCapacity(resourceUuid, resourceType));

        return true;
    }

    private void recalculateHostCapacity(String resourceUuid, String resourceType) {
        RecalculateHostCapacityMsg msg = new RecalculateHostCapacityMsg();
        bus.makeTargetServiceIdByResourceUuid(msg, HostAllocatorConstant.SERVICE_ID, resourceUuid);
        if (resourceType.equals(ZoneVO.class.getSimpleName())) {
            msg.setZoneUuid(resourceUuid);
        } else if (resourceType.equals(ClusterVO.class.getSimpleName())) {
            msg.setClusterUuid(resourceUuid);
        } else if (resourceType.equals(HostVO.class.getSimpleName())) {
            msg.setHostUuid(resourceUuid);
        }
        bus.send(msg);
    }

    @Override
    public boolean stop() {
        return true;
    }
}
