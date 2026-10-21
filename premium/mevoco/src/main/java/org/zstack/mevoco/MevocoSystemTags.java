package org.zstack.mevoco;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.network.service.vip.VipVO;
import org.zstack.tag.SystemTag;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by frank on 9/20/2015.
 */
@TagDefinition
public class MevocoSystemTags {
    public static final String NETWORK_OUTBOUND_BANDWIDTH_TOKEN = "networkOutboundBandwidth";
    public static SystemTag NETWORK_OUTBOUND_BANDWIDTH = new SystemTag(
            String.format("networkOutboundBandwidth::{%s}", NETWORK_OUTBOUND_BANDWIDTH_TOKEN),
            InstanceOfferingVO.class
    );
    public static SystemTag _NETWORK_OUTBOUND_BANDWIDTH = new SystemTag(
            String.format("networkOutboundBandwidth::{%s}", NETWORK_OUTBOUND_BANDWIDTH_TOKEN),
            VmInstanceVO.class
    );

    public static final String NETWORK_INBOUND_BANDWIDTH_TOKEN = "networkInboundBandwidth";
    public static SystemTag NETWORK_INBOUND_BANDWIDTH = new SystemTag(
            String.format("networkInboundBandwidth::{%s}", NETWORK_INBOUND_BANDWIDTH_TOKEN),
            InstanceOfferingVO.class
    );
    public static SystemTag _NETWORK_INBOUND_BANDWIDTH = new SystemTag(
            String.format("networkInboundBandwidth::{%s}", NETWORK_INBOUND_BANDWIDTH_TOKEN),
            VmInstanceVO.class
    );

    public static final String VIP_OUTBOUND_BANDWIDTH_TOKEN = "vipOutboundBandwidth";
    public static SystemTag VIP_OUTBOUND_BANDWIDTH = new SystemTag(
            String.format("vipOutboundBandwidth::{%s}", VIP_OUTBOUND_BANDWIDTH_TOKEN),
            VipVO.class
    );

    public static final String VIP_INBOUND_BANDWIDTH_TOKEN = "vipInboundBandwidth";
    public static SystemTag VIP_INBOUND_BANDWIDTH = new SystemTag(
            String.format("vipInboundBandwidth::{%s}", VIP_INBOUND_BANDWIDTH_TOKEN),
            VipVO.class
    );

    public static final String VIP_QOS_CLASS_TOKEN = "vipQosClass";
    public static SystemTag VIP_QOS_CLASS = new SystemTag(
            String.format("vipQosClass::{%s}", VIP_QOS_CLASS_TOKEN),
            VipVO.class
    );

    public static final String VOLUME_TOTAL_BANDWIDTH_TOKEN = "volumeTotalBandwidth";
    public static SystemTag _VOLUME_TOTAL_BANDWIDTH = new SystemTag(
            String.format("volumeTotalBandwidth::{%s}", VOLUME_TOTAL_BANDWIDTH_TOKEN),
            InstanceOfferingVO.class
    );

    public static final String VOLUME_READ_BANDWIDTH_TOKEN = "volumeReadBandwidth";
    public static SystemTag _VOLUME_READ_BANDWIDTH = new SystemTag(
            String.format("volumeReadBandwidth::{%s}", VOLUME_READ_BANDWIDTH_TOKEN),
            InstanceOfferingVO.class
    );

    public static final String VOLUME_WRITE_BANDWIDTH_TOKEN = "volumeWriteBandwidth";
    public static SystemTag _VOLUME_WRITE_BANDWIDTH = new SystemTag(
            String.format("volumeWriteBandwidth::{%s}", VOLUME_WRITE_BANDWIDTH_TOKEN),
            InstanceOfferingVO.class
    );

    public static final String VOLUME_TOTAL_IOPS_TOKEN = "volumeTotalIops";
    public static SystemTag VOLUME_TOTAL_IOPS = new SystemTag(
            String.format("volumeTotalIops::{%s}", VOLUME_TOTAL_IOPS_TOKEN),
            VolumeVO.class
    );
    public static SystemTag _VOLUME_TOTAL_IOPS = new SystemTag(
            String.format("volumeTotalIops::{%s}", VOLUME_TOTAL_IOPS_TOKEN),
            InstanceOfferingVO.class
    );

    public static final String VOLUME_READ_IOPS_TOKEN = "volumeReadIops";
    public static SystemTag _VOLUME_READ_IOPS = new SystemTag(
            String.format("volumeReadIops::{%s}", VOLUME_READ_IOPS_TOKEN), InstanceOfferingVO.class);

    public static final String VOLUME_WRITE_IOPS_TOKEN = "volumeWriteIops";
    public static SystemTag _VOLUME_WRITE_IOPS = new SystemTag(
            String.format("volumeWriteIops::{%s}", VOLUME_WRITE_IOPS_TOKEN), InstanceOfferingVO.class);

    public static final String CLUSTER_DISPLAY_NETWORK_TOKEN = "displayNetwork";
    public static SystemTag CLUSTER_DISPLAY_NETWORK = new SystemTag(
            String.format("display::network::cidr::{%s}", CLUSTER_DISPLAY_NETWORK_TOKEN),
            ClusterVO.class
    );

    public static String VM_CONSOLE_MODE_TOKEN = "vmConsoleMode";
    public static SystemTag VM_CONSOLE_MODE = new SystemTag(String.format("vmConsoleMode::{%s}", VM_CONSOLE_MODE_TOKEN), VmInstanceVO.class);

    public static String CLUSTER_MIGRATE_NETWORK_CIDR_TOKEN = "migrateCidr";
    public static SystemTag CLUSTER_MIGRATE_NETWORK_CIDR = new SystemTag(String.format(
            "cluster::migrate::network::cidr::{%s}", CLUSTER_MIGRATE_NETWORK_CIDR_TOKEN), ClusterVO.class);

    public static String CLUSTER_CBT_BACKUP_NETWORK_CIDR_TOKEN = "cbtBackupCidr";
    public static SystemTag CLUSTER_CBT_BACKUP_NETWORK_CIDR = new SystemTag(String.format(
            "cluster::cbt::backup::network::cidr::{%s}", CLUSTER_CBT_BACKUP_NETWORK_CIDR_TOKEN), ClusterVO.class);

    public static SystemTag CONFIRM_CALL_API = SystemTag.makeEphemeralTag("IReallyWantToCallThisApi");

    /**
     * guestToolsHasAttached system tag will replace by {@link VmCdRomVO#getOccupant()} soon.
     */
    @Deprecated
    public static SystemTag GUEST_TOOLS_HAS_ATTACHED = new SystemTag("guestToolsHasAttached", VmInstanceVO.class);

    public static String PLATFORM_ID_TOKEN = "platformId";
    public static SystemTag PLATFORM_ID = new SystemTag(String.format("platformId::{%s}", PLATFORM_ID_TOKEN), AccountVO.class);

    public static final String KVM_HOST_SPICE_TLS_STATUS_TOKEN = "status";
    public static SystemTag KVM_HOST_SPICE_TLS_STATUS = new SystemTag(String.format("kvmHostSpiceTlsStatus::{%s}", KVM_HOST_SPICE_TLS_STATUS_TOKEN), HostVO.class);

    public static final String KVM_HOST_CGROUP_DEVICE_ACL_STATUS_TOKEN = "status";
    public static SystemTag KVM_HOST_CGROUP_DEVICE_ACL_STATUS = new SystemTag(String.format("kvmHostCgroupDeviceAclStatus::{%s}", KVM_HOST_CGROUP_DEVICE_ACL_STATUS_TOKEN), HostVO.class);

    public static Map<String, SystemTag> generateIopsSystemTagMap() {
        Map<String, SystemTag> map = new HashMap<>();
        map.put(VOLUME_TOTAL_IOPS_TOKEN, _VOLUME_TOTAL_IOPS);
        map.put(VOLUME_WRITE_IOPS_TOKEN, _VOLUME_WRITE_IOPS);
        map.put(VOLUME_READ_IOPS_TOKEN, _VOLUME_READ_IOPS);
        return map;
    }

    public static Map<String, SystemTag> generateVolumeSystemTagMap() {
        Map<String, SystemTag> map = new HashMap<>();
        map.put(VOLUME_TOTAL_BANDWIDTH_TOKEN, _VOLUME_TOTAL_BANDWIDTH);
        map.put(VOLUME_READ_BANDWIDTH_TOKEN, _VOLUME_READ_BANDWIDTH);
        map.put(VOLUME_WRITE_BANDWIDTH_TOKEN, _VOLUME_WRITE_BANDWIDTH);
        return map;
    }
}
