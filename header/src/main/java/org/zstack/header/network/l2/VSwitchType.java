package org.zstack.header.network.l2;

import org.zstack.header.vm.VmMacVlanNicConstant;
import org.zstack.header.vm.VmNicType;
import org.zstack.header.vm.VmOvsNicConstant;

import java.util.*;

public class VSwitchType {
    private static Map<String, VSwitchType> types = Collections.synchronizedMap(new HashMap<String, VSwitchType>());
    private final String typeName;
    private static Map<String, List<VmNicType>> vSwitchSupportNicTypesMap = Collections.synchronizedMap(new HashMap<String, List<VmNicType>>());
    private boolean exposed = true;

    public static boolean hasType(String typeName) {
        return types.containsKey(typeName);
    }

    public VSwitchType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public VSwitchType(String typeName, boolean exposed) {
        this(typeName);
        this.exposed = exposed;
    }

    public VSwitchType(String typeName, VmNicType nicType) {
        this.typeName = typeName;
        types.put(typeName, this);
        vSwitchSupportNicTypesMap.computeIfAbsent(typeName, k -> new ArrayList<VmNicType>()).add(nicType);
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    public List<VmNicType> getSupVmNicTypes() {
        return vSwitchSupportNicTypesMap.get(typeName);
    }

    public VmNicType getVmNicTypeWithCondition(boolean enableSRIOV, boolean enableVhostUser) {
        List<VmNicType> types = getSupVmNicTypes();

        if (typeName.equals(L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK)) {
            if (enableVhostUser && types.contains(VmNicType.valueOf(VmOvsNicConstant.ACCEL_TYPE_VHOST_USER_SPACE))) {
                return VmNicType.valueOf(VmOvsNicConstant.ACCEL_TYPE_VHOST_USER_SPACE);
            } else if (types.contains(VmNicType.valueOf(VmOvsNicConstant.ACCEL_TYPE_VDPA))){
                return VmNicType.valueOf(VmOvsNicConstant.ACCEL_TYPE_VDPA);
            }
        } else if (typeName.equals(L2NetworkConstant.VSWITCH_TYPE_LINUX_BRIDGE)){
            for (VmNicType type : types) {
                if (type.isUseSRIOV() == enableSRIOV) {
                    return type;
                }
            }
        } else if (typeName.equals(L2NetworkConstant.VSWITCH_TYPE_MACVLAN)) {
            if (types.contains(VmNicType.valueOf(VmMacVlanNicConstant.MACVLAN_NIC_TYPE))) {
                return VmNicType.valueOf(VmMacVlanNicConstant.MACVLAN_NIC_TYPE);
            }
        }

        return null;
    }

    public static VSwitchType valueOf(String typeName) {
        VSwitchType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("VSwitchType type: " + typeName + " was not registered by any L2NetworkFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof VSwitchType)) {
            return false;
        }

        VSwitchType type = (VSwitchType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static Set<String> getAllTypeNames() {
        HashSet<String> exposedTypes = new HashSet<String>();
        for (VSwitchType type : types.values()) {
            if (type.isExposed()) {
                exposedTypes.add(type.toString());
            }
        }
        return exposedTypes;
    }
}
