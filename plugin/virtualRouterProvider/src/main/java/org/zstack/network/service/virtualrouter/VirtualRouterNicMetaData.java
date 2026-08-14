package org.zstack.network.service.virtualrouter;

import org.zstack.header.configuration.PythonClass;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;

import java.util.ArrayList;
import java.util.List;

@PythonClass
public class VirtualRouterNicMetaData {
    public static final Integer PUBLIC_NIC_MASK = 1;
    public static final Integer MANAGEMENT_NIC_MASK = 1 << 1;
    public static final Integer GUEST_NIC_MASK = 1 << 2;
    public static final Integer ADDITIONAL_PUBLIC_NIC_MASK = 1 << 3;
    public static final Integer PUBLIC_AND_MANAGEMENT_NIC_MASK = PUBLIC_NIC_MASK | MANAGEMENT_NIC_MASK;
    public static final Integer PUBLIC_MANAGEMENT_GUEST_NIC_MASK = PUBLIC_NIC_MASK | MANAGEMENT_NIC_MASK | GUEST_NIC_MASK;
    public static final Integer PUBLIC_AND_GUEST_NIC_MASK = PUBLIC_NIC_MASK | GUEST_NIC_MASK;
    public static final Integer MANAGEMENT_AND_GUEST_NIC_MASK = MANAGEMENT_NIC_MASK | GUEST_NIC_MASK;

    public static final List<String> PUBLIC_NIC_MASK_STRING_LIST = new ArrayList<String>();
    public static final List<String> MANAGEMENT_NIC_MASK_STRING_LIST = new ArrayList<String>();
    public static final List<String> GUEST_NIC_MASK_STRING_LIST = new ArrayList<String>();
    public static final List<String> ALL_PUBLIC_NIC_MASK_STRING_LIST = new ArrayList<String>();

    static {
        PUBLIC_NIC_MASK_STRING_LIST.add(PUBLIC_NIC_MASK.toString());
        PUBLIC_NIC_MASK_STRING_LIST.add(PUBLIC_AND_MANAGEMENT_NIC_MASK.toString());
        PUBLIC_NIC_MASK_STRING_LIST.add(PUBLIC_AND_GUEST_NIC_MASK.toString());
        PUBLIC_NIC_MASK_STRING_LIST.add(PUBLIC_MANAGEMENT_GUEST_NIC_MASK.toString());

        MANAGEMENT_NIC_MASK_STRING_LIST.add(MANAGEMENT_NIC_MASK.toString());
        MANAGEMENT_NIC_MASK_STRING_LIST.add(PUBLIC_AND_MANAGEMENT_NIC_MASK.toString());
        MANAGEMENT_NIC_MASK_STRING_LIST.add(MANAGEMENT_AND_GUEST_NIC_MASK.toString());
        MANAGEMENT_NIC_MASK_STRING_LIST.add(PUBLIC_MANAGEMENT_GUEST_NIC_MASK.toString());

        GUEST_NIC_MASK_STRING_LIST.add(GUEST_NIC_MASK.toString());
        GUEST_NIC_MASK_STRING_LIST.add(PUBLIC_AND_GUEST_NIC_MASK.toString());
        GUEST_NIC_MASK_STRING_LIST.add(MANAGEMENT_AND_GUEST_NIC_MASK.toString());
        GUEST_NIC_MASK_STRING_LIST.add(PUBLIC_MANAGEMENT_GUEST_NIC_MASK.toString());

        ALL_PUBLIC_NIC_MASK_STRING_LIST.addAll(PUBLIC_NIC_MASK_STRING_LIST);
        ALL_PUBLIC_NIC_MASK_STRING_LIST.add(ADDITIONAL_PUBLIC_NIC_MASK.toString());
    }

    @PythonClass
    public static final String VR_PUBLIC_NIC_META = String.valueOf(PUBLIC_NIC_MASK);
    @PythonClass
    public static final String VR_MANAGEMENT_NIC_META = String.valueOf(MANAGEMENT_NIC_MASK);
    @PythonClass
    public static final String VR_MANAGEMENT_AND_PUBLIC_NIC_META = String.valueOf(PUBLIC_AND_MANAGEMENT_NIC_MASK);
    @PythonClass
    public static final String VR_ADDITIONAL_PUBLIC_NIC_META = String.valueOf(ADDITIONAL_PUBLIC_NIC_MASK);

    public static boolean isPublicNic(VmNicVO nic) {
        String meta = nic.getMetaData();
        if (meta == null) {
            return false;
        }

        int mask = Integer.parseInt(meta);
        return (mask & PUBLIC_NIC_MASK) != 0;
    }

    public static boolean isManagementNic(VmNicVO nic) {
        String meta = nic.getMetaData();
        if (meta == null) {
            return false;
        }

        int mask = Integer.parseInt(meta);
        return (mask & MANAGEMENT_NIC_MASK) != 0;
    }

    public static boolean isGuestNic(VmNicVO nic) {
        String meta = nic.getMetaData();
        if (meta == null) {
            return false;
        }

        int mask = Integer.parseInt(meta);
        return (mask & GUEST_NIC_MASK) != 0;
    }

    public static boolean isPublicNic(VmNicInventory nic) {
        String meta = nic.getMetaData();
        if (meta == null) {
            return false;
        }

        int mask = Integer.parseInt(meta);
        return (mask & PUBLIC_NIC_MASK) != 0;
    }

    public static boolean isAddinitionalPublicNic(VmNicInventory nic) {
        String meta = nic.getMetaData();
        if (meta == null) {
            return false;
        }

        int mask = Integer.parseInt(meta);
        return (mask & ADDITIONAL_PUBLIC_NIC_MASK) != 0;
    }

    public static boolean isAddinitionalPublicNic(VmNicVO nic) {
        String meta = nic.getMetaData();
        if (meta == null) {
            return false;
        }

        int mask = Integer.parseInt(meta);
        return (mask & ADDITIONAL_PUBLIC_NIC_MASK) != 0;
    }

    public static boolean isManagementNic(VmNicInventory nic) {
        String meta = nic.getMetaData();
        if (meta == null) {
            return false;
        }

        int mask = Integer.parseInt(meta);
        return (mask & MANAGEMENT_NIC_MASK) != 0;
    }

    public static boolean isGuestNic(VmNicInventory nic) {
        String meta = nic.getMetaData();
        if (meta == null) {
            return false;
        }

        int mask = Integer.parseInt(meta);
        return (mask & GUEST_NIC_MASK) != 0;
    }

    public static void removePublicToNic(VmNicVO nic) {
        String meta = nic.getMetaData();
        if (meta == null) {
            return;
        }

        int mask = Integer.parseInt(meta) & ~PUBLIC_NIC_MASK;
        nic.setMetaData(String.valueOf(mask));
    }

    public static void addPublicToNic(VmNicVO nic) {
        String meta = nic.getMetaData();
        if (meta == null) {
            return;
        }

        int mask = Integer.parseInt(meta) | PUBLIC_NIC_MASK;
        nic.setMetaData(String.valueOf(mask));
    }

    public static void removeAdditionalPublicToNic(VmNicVO nic) {
        String meta = nic.getMetaData();
        if (meta == null) {
            return;
        }

        int mask = Integer.parseInt(meta) & ~ADDITIONAL_PUBLIC_NIC_MASK;
        nic.setMetaData(String.valueOf(mask));
    }

    public static void addAdditionalPublicToNic(VmNicVO nic) {
        String meta = nic.getMetaData();
        if (meta == null) {
            return;
        }

        int mask = Integer.parseInt(meta) | ADDITIONAL_PUBLIC_NIC_MASK;
        nic.setMetaData(String.valueOf(mask));
    }

    public static boolean isManagementNicOnly(VmNicInventory nic) {
        String meta = nic.getMetaData();
        if (meta == null) {
            return false;
        }

        return Integer.parseInt(meta)== MANAGEMENT_NIC_MASK;
    }

    public static boolean isManagementNicOnly(VmNicVO nic) {
        String meta = nic.getMetaData();
        if (meta == null) {
            return false;
        }

        return Integer.parseInt(meta)== MANAGEMENT_NIC_MASK;
    }
}
