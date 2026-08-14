package org.zstack.zwatch.utils;

import org.zstack.header.baremetal.instance.BaremetalInstanceVO;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostVO;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.KVMHostVO;
import org.zstack.network.service.lb.LoadBalancerListenerVO;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.xdragon.XDragonHostVO;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.*;

import java.util.HashMap;
import java.util.Map;

import static org.zstack.core.Platform.argerr;

/**
 * Created by yaoning.li on 2020/10/16.
 */
public class ResourceVOToNamespaceMappingUtils {
    private static Map<Class, String[]> voToNamespaceMapping = new HashMap<>();

    static {
        mapVOToNamespace(BackupStorageVO.class, BackupStorageNamespace.NAME, BackupStorageNamespace.LabelNames.BackupStorageUuid.toString());
        mapVOToNamespace(HostVO.class, HostNamespace.NAME, HostNamespace.LabelNames.HostUuid.toString());
        mapVOToNamespace(L3NetworkVO.class, L3NetworkNamespace.NAME, L3NetworkNamespace.LabelNames.L3NetworkUuid.toString());
        mapVOToNamespace(PrimaryStorageVO.class, PrimaryStorageNamespace.NAME, PrimaryStorageNamespace.LabelNames.PrimaryStorageUuid.toString());
        mapVOToNamespace(VipVO.class, VipNamespace.NAME, VipNamespace.LabelNames.VipUUID.toString());
        mapVOToNamespace(VmInstanceVO.class, VmNamespace.NAME, VmNamespace.LabelNames.VMUuid.toString());
        mapVOToNamespace(VolumeVO.class, VolumeNamespace.NAME, VolumeNamespace.LabelNames.VolumeUuid.toString());
        mapVOToNamespace(LoadBalancerListenerVO.class, LoadBalancerNamespace.NAME, LoadBalancerNamespace.LabelNames.ListenerUuid.toString());
        mapVOToNamespace(VirtualRouterVmVO.class, VRouterNamespace.NAME, VRouterNamespace.LabelNames.VMUuid.toString());
        mapVOToNamespace(BaremetalInstanceVO.class, BaremetalVmNamespace.NAME, BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        mapVOToNamespace(KVMHostVO.class,KVMHostNamespace.NAME,KVMHostNamespace.LabelNames.KVMHostUuid.toString());
        mapVOToNamespace(XDragonHostVO.class,XDragonHostNamespace.NAME,XDragonHostNamespace.LabelNames.XDragonHostUuid.toString());
    }

    public static void mapVOToNamespace(Class clz, String namespaceSubname, String restrictLabelName) {
        voToNamespaceMapping.put(clz, new String[] { Namespace.zstackNamespaceName(namespaceSubname), restrictLabelName});
    }

    public static String[] getVOToNamespaceMapping(Class clz) {
        Class tmp = clz;
        while (tmp != Object.class) {
            String[] m = voToNamespaceMapping.get(tmp);
            if (m != null) {
                return m;
            }
            tmp = tmp.getSuperclass();
        }

        throw new OperationFailureException(argerr("resource[%s] doesn't support zwatch return with clause", clz.getName()));
    }

    public static String[] getVOToNamespaceMapping(String voClassSimpleName) {
        for (Class voClass : voToNamespaceMapping.keySet()) {
            if (voClass.getSimpleName().equals(voClassSimpleName)) {
                return getVOToNamespaceMapping(voClass);
            }
        }

        throw new OperationFailureException(argerr("resource[%s] doesn't support zwatch return with clause", voClassSimpleName));
    }

    public static String getRestrictLabelNames(String voClassSimpleName) {
        return ResourceVOToNamespaceMappingUtils.getVOToNamespaceMapping(voClassSimpleName)[1];
    }

    public static String getNamespaceName(String voClassSimpleName) {
        return ResourceVOToNamespaceMappingUtils.getVOToNamespaceMapping(voClassSimpleName)[0];
    }
}
