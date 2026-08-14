package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.host.*;
import org.zstack.header.host.ChangeVmPasswordMsg;
import org.zstack.header.storage.snapshot.TakeVolumesSnapshotOnKvmMsg;
import org.zstack.header.vm.*;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostContext;
import org.zstack.kvm.KVMHostFactory;
import org.zstack.mevoco.MevocoSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * Created by mingjian.deng on 16/12/1.
 */
public class MevocoHostBaseFactory implements HostBaseExtensionFactory, MigrateNetworkExtensionPoint{
    protected static final CLogger logger = Utils.getLogger(MevocoHostBaseFactory.class);
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public Host getHost(HostVO vo) {
        for (HypervisorFactory f : pluginRgty.getExtensionList(HypervisorFactory.class)) {
            if (f.getHypervisorType().toString().equals(KVMConstant.KVM_HYPERVISOR_TYPE)
            && KVMConstant.KVM_HYPERVISOR_TYPE.equals(vo.getHypervisorType())) {
                KVMHostContext context = ((KVMHostFactory)f).getHostContext(vo.getUuid());
                return new MevocoHostBase(context);
            }

            if (f.getHypervisorType().toString().equals("baremetal2") && vo.getHypervisorType().equals("baremetal2")) {
                KVMHostContext context = ((KVMHostFactory)f).getHostContext(vo.getUuid());
                return new MevocoHostBase(context);
            }
        }
        return null;
    }

    @Override
    public List<Class> getMessageClasses() {
        return asList(ChangeVmPasswordMsg.class, CheckVmVolumesMsg.class, SetVolumeQosOnKVMHostMsg.class,
                DeleteVolumeQosOnKVMHostMsg.class, CheckMountDomainMsg.class,
                SetNicQosOnKVMHostMsg.class, GetVolumeQosOnKVMHostMsg.class, GetNicQosOnKVMHostMsg.class,
                ResizeVolumeOnKvmMsg.class, BlockStreamVolumeMsg.class, TakeVolumesSnapshotOnKvmMsg.class,
                IdentifyHostMsg.class, GetHostNetworkFactsMsg.class, SetBridgeRouterPortMsg.class,
                PowerOffHostMsg.class, ChangeHostPasswordMsg.class, LocateHostNetworkInterfaceMsg.class,
                GetHostPhysicalMemoryFactsMsg.class, ChangeVmEmulatorPinningMsg.class,
                SetHostPhysicalNicMonitorMsg.class, ReserveEthernetVfMsg.class,
                UpdateHostClockSyncVmMsg.class, VmPortsConfigSyncOnHypervisorMsg.class,
                VmSpecificationConfigSyncOnHypervisorMsg.class,
                InitQgaZWatchMonitorMsg.class, SetVmHostnameOnHypervisorMsg.class, SetVmDnsOnHypervisorMsg.class,
                ApplyMemoryBalloonDecisionMsg.class, StartHostPhysicalMemoryMonitorMsg.class,
                GetHostPhysicalCpuFactsMsg.class, UpdateHostIscsiInitiatorNameMsg.class
                );
    }

    private Map<String, String> getHostIpsInCidr(String cidr, String ...hostUuids){
        if (cidr == null) {
            return null;
        }

        Map<String, String> results = new HashMap<>();
        for (String huuid : hostUuids) {
            String mnIp = Q.New(HostVO.class).eq(HostVO_.uuid, huuid).select(HostVO_.managementIp).findValue();
            final String extraIps = HostSystemTags.EXTRA_IPS.getTokenByResourceUuid(huuid, HostSystemTags.EXTRA_IPS_TOKEN);

            List<String> ips = new ArrayList<>();
            ips.add(mnIp);
            Optional.ofNullable(extraIps).ifPresent(it -> ips.addAll(Arrays.asList(it.split(","))));
            List<String> cidrIps = NetworkUtils.filterIpv4sInCidr(ips, cidr);
            boolean hasIpInCidr = !cidrIps.isEmpty();
            results.put(huuid, hasIpInCidr ? cidrIps.get(0) : null);
        }

        return results;
    }

    private String getSameMigrateCidr(String ...hostUuids) {
        // cidr has been format before
        Set<String> result = Arrays.stream(hostUuids).map(it -> MevocoSystemTags.CLUSTER_MIGRATE_NETWORK_CIDR.getTokenByResourceUuid(
                Q.New(HostVO.class).eq(HostVO_.uuid, it).select(HostVO_.clusterUuid).findValue(),
                MevocoSystemTags.CLUSTER_MIGRATE_NETWORK_CIDR_TOKEN)).collect(Collectors.toSet());

        return result.size() == 1 ? result.iterator().next() : null;
    }

    @Transactional
    @Override
    public MigrateInfo getMigrationAddressForVM(String srcHostUuid, String dstHostUuid) {
        MigrateInfo result = new MigrateInfo();
        result.srcMigrationAddress = Q.New(HostVO.class).eq(HostVO_.uuid, srcHostUuid).select(HostVO_.managementIp).findValue();
        result.dstMigrationAddress = Q.New(HostVO.class).eq(HostVO_.uuid, dstHostUuid).select(HostVO_.managementIp).findValue();

        String migrateCidr = getSameMigrateCidr(srcHostUuid, dstHostUuid);
        if (migrateCidr == null) {
            return result;
        }

        Map<String, String> migrateCidrIp = getHostIpsInCidr(migrateCidr, srcHostUuid, dstHostUuid);

        boolean bothInSameMigrateCidr = migrateCidrIp != null && migrateCidrIp.values().stream().noneMatch(Objects::isNull);
        if (bothInSameMigrateCidr) {
            result.srcMigrationAddress = migrateCidrIp.get(srcHostUuid);
            result.dstMigrationAddress = migrateCidrIp.get(dstHostUuid);
        }

        return result;
    }
}
