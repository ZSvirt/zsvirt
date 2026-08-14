package org.zstack.kvm;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by MaJin on 2020/7/23.
 */
public class KVMVmDeviceType {
    private static Map<String, KVMVmDeviceType> types = new ConcurrentHashMap<>();
    private final String resourceType;
    private final List<String> deviceTypes;
    private final GetTOFunction getTOFunction;

    interface GetTOFunction {
        List call(List inventories, KVMHostInventory host);
    }

    public static void New(String resourceType, List<String> deviceTypes, GetTOFunction getTOFunction) {
        types.put(resourceType, new KVMVmDeviceType(resourceType, deviceTypes, getTOFunction));
    }

    public KVMVmDeviceType(String resourceType, List<String> deviceTypes, GetTOFunction getTOFunction) {
        this.resourceType = resourceType;
        this.deviceTypes = deviceTypes;
        this.getTOFunction = getTOFunction;
    }

    public List getDeviceTOs(List inventories, KVMHostInventory host) {
        return getTOFunction.call(inventories, host);
    }

    @Override
    public String toString() {
        return resourceType;
    }

    public static KVMVmDeviceType fromResourceType(String resourceType) {
        return types.get(resourceType);
    }

    public String toResourceType() {
        return resourceType;
    }
}
