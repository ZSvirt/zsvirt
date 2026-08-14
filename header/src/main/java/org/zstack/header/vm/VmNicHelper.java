package org.zstack.header.vm;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class VmNicHelper {
    private static final CLogger logger = Utils.getLogger(VmNicHelper.class);

    /* for some vm, it nic has l3 network, but there is no ip address */
    public static List<String> getL3Uuids(VmNicInventory nic) {
        List<String> ret = new ArrayList<>();
        for (UsedIpInventory ip : nic.getUsedIps()) {
            ret.add(ip.getL3NetworkUuid());
        }
        ret.add(nic.getL3NetworkUuid());

        return ret.stream().distinct().collect(Collectors.toList());
    }

    public static List<String> getL3Uuids(VmNicVO nic) {
        List<String> ret = new ArrayList<>();
        for (UsedIpVO ip : nic.getUsedIps()) {
            ret.add(ip.getL3NetworkUuid());
        }
        ret.add(nic.getL3NetworkUuid());

        return ret.stream().distinct().collect(Collectors.toList());
    }

    public static List<String> getL3Uuids(List<VmNicInventory> nics) {
        List<String> ret = new ArrayList<>();
        for (VmNicInventory nic : nics) {
            ret.addAll(getL3Uuids(nic));
        }

        return ret;
    }

    public static List<String> getIpAddresses(VmNicInventory nic) {
        List<String> ret = new ArrayList<>();
        for (UsedIpInventory ip : nic.getUsedIps()) {
            ret.add(ip.getIp());
        }

        return ret;
    }

    public static List<String> getIpAddresses(VmNicVO nic) {
        List<String> ret = new ArrayList<>();
        for (UsedIpVO ip : nic.getUsedIps()) {
            ret.add(ip.getIp());
        }

        return ret;
    }
    
    public static boolean isL3AttachedToVmNic(VmNicInventory nic, String l3Uuid) {
        for (UsedIpInventory ip : nic.getUsedIps()) {
            if (ip.getL3NetworkUuid().equals(l3Uuid)) {
                return true;
            }
        }

        if (nic.getL3NetworkUuid() == null) {
            return false;
        }

        return nic.getL3NetworkUuid().equals(l3Uuid);
    }

    public static List<String> getUsedIpUuids(VmNicInventory nic) {
        return nic.getUsedIps().stream().map(UsedIpInventory::getUuid).collect(Collectors.toList());
    }

    public static List<String> getUsedIpUuids(VmNicVO nic) {
        return nic.getUsedIps().stream().map(UsedIpVO::getUuid).collect(Collectors.toList());
    }

    public static boolean isDefaultNic(VmNicInventory nic, VmInstanceInventory vm) {
        if (nic.getVmInstanceUuid() == null || vm.getDefaultL3NetworkUuid() == null
                || !nic.getVmInstanceUuid().equals(vm.getUuid())) {
            return false;
        }

        for (UsedIpInventory ip : nic.getUsedIps()) {
            if (ip.getL3NetworkUuid().equals(vm.getDefaultL3NetworkUuid())) {
                return true;
            }
        }

        return false;
    }
    
    public static VmNicInventory getDefaultNic(VmInstanceInventory vm) {
        return vm.getVmNics().stream()
                .filter(nic -> isDefaultNic(nic, vm))
                .findAny()
                .orElse(null);
    }

    public static String getPrimaryL3Uuid(VmNicInventory nic) {
        return nic.getL3NetworkUuid();
    }

    public static List<VmNicParam> getVmNicParamsWithVfNic(VmInstanceVO vmVo) {
        List<VmNicParam> ret = new ArrayList<>();
        for (VmNicVO nic : vmVo.getVmNics()) {
            VmNicType nicType = VmNicType.valueOf(nic.getType());
            if (nicType.isUseSRIOV()) {
                VmNicParam param = new VmNicParam();
                param.setL3NetworkUuid(nic.getL3NetworkUuid());
                param.setDriverType(nic.getDriverType());
                ret.add(param);
            }
        }

        return ret;
    }
}
