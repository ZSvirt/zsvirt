package org.zstack.header.vm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class VmInstanceType {
    private static Map<String, VmInstanceType> types = Collections.synchronizedMap(new HashMap<String, VmInstanceType>());
    private final String typeName;

    private boolean supportUpdateOnHypervisor = true;
    private boolean sriovSupported = false;

    public VmInstanceType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static VmInstanceType valueOf(String typeName) {
        VmInstanceType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("VmInstanceType type: " + typeName + " was not registered by any VmInstanceFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof VmInstanceType)) {
            return false;
        }

        VmInstanceType type = (VmInstanceType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public boolean isSupportUpdateOnHypervisor() {
        return supportUpdateOnHypervisor;
    }

    public void setSupportUpdateOnHypervisor(boolean supportUpdateOnHypervisor) {
        this.supportUpdateOnHypervisor = supportUpdateOnHypervisor;
    }

    public boolean isSriovSupported() {
        return sriovSupported;
    }

    public void setSriovSupported(boolean sriovSupported) {
        this.sriovSupported = sriovSupported;
    }
}
