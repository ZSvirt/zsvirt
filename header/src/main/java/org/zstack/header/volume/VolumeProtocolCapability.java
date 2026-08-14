package org.zstack.header.volume;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VolumeProtocolCapability {
    private static Map<String, VolumeProtocolCapability> types = new ConcurrentHashMap<>();

    private final String protocol;
    private final String hypervisor;

    private boolean supportQosOnHypervisor;

    private boolean supportResizeOnHypervisor;

    private boolean supportReadonly;

    public VolumeProtocolCapability(String protocol, String hypervisor) {
        this.protocol = protocol;
        this.hypervisor = hypervisor;
    }

    private static String makeKey(String protocol, String hypervisor) {
        return String.format("%s-%s", protocol, hypervisor);
    }

    public static VolumeProtocolCapability get(String protocol, String hypervisor) {
        String key = makeKey(protocol, hypervisor);
        VolumeProtocolCapability capability = types.get(key);
        if (capability == null) {
            return null;
        }
        return capability;
    }

    public static VolumeProtocolCapability register(String protocol, String hypervisor) {
        String key = makeKey(protocol, hypervisor);
        if (types.containsKey(key)) {
            throw new IllegalArgumentException(String.format("Duplicate VolumeProtocolCapability for protocol[%s] hypervisor[%s]", protocol, hypervisor));
        }
        types.put(key, new VolumeProtocolCapability(protocol, hypervisor));
        return types.get(key);
    }

    public boolean isSupportQosOnHypervisor() {
        return supportQosOnHypervisor;
    }

    public void setSupportQosOnHypervisor(boolean supportQosOnHypervisor) {
        this.supportQosOnHypervisor = supportQosOnHypervisor;
    }

    public boolean isSupportResizeOnHypervisor() {
        return supportResizeOnHypervisor;
    }

    public void setSupportResizeOnHypervisor(boolean supportResizeOnHypervisor) {
        this.supportResizeOnHypervisor = supportResizeOnHypervisor;
    }

    public boolean isSupportReadonly() {
        return supportReadonly;
    }

    public void setSupportReadonly(boolean supportReadonly) {
        this.supportReadonly = supportReadonly;
    }
}
