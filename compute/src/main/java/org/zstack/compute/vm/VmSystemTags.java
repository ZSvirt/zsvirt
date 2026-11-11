package org.zstack.compute.vm;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.zstack.header.core.NonCloneable;
import org.zstack.header.tag.AdminOnlyTag;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.SensitiveTagOutputHandler;
import org.zstack.tag.SensitiveTag;
import org.zstack.tag.SystemTag;
import org.zstack.utils.Tags;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 */
@TagDefinition
public class VmSystemTags {
    private static final Set<String> SENSITIVE_TAG_HEADS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "consolePassword",
            "sshkey",
            "userdata"
    )));

    public static boolean isSensitiveTagHead(String tagHead) {
        return tagHead != null && SENSITIVE_TAG_HEADS.contains(tagHead);
    }

    public static boolean isSensitiveVmTag(String tag) {
        if (tag == null) {
            return false;
        }
        String tagHead = Tags.first(tag);
        if (Objects.equals(tagHead, tag)) {
            return false;
        }
        return isSensitiveTagHead(tagHead);
    }

    public static String HOSTNAME_TOKEN = "hostname";
    public static SystemTag HOSTNAME = new SystemTag(String.format("hostname::{%s}", HOSTNAME_TOKEN), VmInstanceVO.class);

    public static String SYNC_PORTS_TOKEN = "syncPorts";
    public static SystemTag SYNC_PORTS = new SystemTag(String.format("syncPorts::{%s}", SYNC_PORTS_TOKEN), VmInstanceVO.class);

    public static String SYNC_HOSTNAME_TOKEN = "syncHostname";
    public static SystemTag SYNC_HOSTNAME = new SystemTag(String.format("syncHostname::{%s}", SYNC_HOSTNAME_TOKEN), VmInstanceVO.class);

    // no need to set vmInstanceUuid
    public static SystemTag SYNC_DNS = new SystemTag("syncConfig::dns", VmInstanceVO.class);

    public static String STATIC_IP_L3_UUID_TOKEN = "l3NetworkUuid";
    public static String STATIC_IP_TOKEN = "staticIp";
    public static SystemTag STATIC_IP = new SystemTag(String.format("staticIp::{%s}::{%s}", STATIC_IP_L3_UUID_TOKEN, STATIC_IP_TOKEN), VmInstanceVO.class);

    public static String IPV4_GATEWAY_L3_UUID_TOKEN = "l3NetworkUuid";
    public static String IPV4_GATEWAY_TOKEN = "ipv4Gateway";
    public static SystemTag IPV4_GATEWAY = new SystemTag(String.format("ipv4Gateway::{%s}::{%s}", IPV4_GATEWAY_L3_UUID_TOKEN, IPV4_GATEWAY_TOKEN), VmInstanceVO.class);

    public static String IPV6_GATEWAY_L3_UUID_TOKEN = "l3NetworkUuid";
    public static String IPV6_GATEWAY_TOKEN = "ipv6Gateway";
    public static SystemTag IPV6_GATEWAY = new SystemTag(String.format("ipv6Gateway::{%s}::{%s}", IPV6_GATEWAY_L3_UUID_TOKEN, IPV6_GATEWAY_TOKEN), VmInstanceVO.class);

    public static String IPV4_NETMASK_L3_UUID_TOKEN = "l3NetworkUuid";
    public static String IPV4_NETMASK_TOKEN = "ipv4Netmask";
    public static SystemTag IPV4_NETMASK = new SystemTag(String.format("ipv4Netmask::{%s}::{%s}", IPV4_NETMASK_L3_UUID_TOKEN, IPV4_NETMASK_TOKEN), VmInstanceVO.class);

    public static String IPV6_PREFIX_L3_UUID_TOKEN = "l3NetworkUuid";
    public static String IPV6_PREFIX_TOKEN = "ipv6Prefix";
    public static SystemTag IPV6_PREFIX = new SystemTag(String.format("ipv6Prefix::{%s}::{%s}", IPV6_PREFIX_L3_UUID_TOKEN, IPV6_PREFIX_TOKEN), VmInstanceVO.class);

    public static String MAC_TOKEN = "customMac";
    public static SystemTag CUSTOM_MAC = new SystemTag(String.format("customMac::{%s}::{%s}", STATIC_IP_L3_UUID_TOKEN, MAC_TOKEN), VmInstanceVO.class);

    public static String NIC_UUID_TOKEN = "customNicUuid";
    public static SystemTag CUSTOM_NIC_UUID = new SystemTag(String.format("customNicUuid::{%s}::{%s}", STATIC_IP_L3_UUID_TOKEN, NIC_UUID_TOKEN), VmInstanceVO.class);

    public static SystemTag WINDOWS_VOLUME_ON_VIRTIO = new SystemTag("windows::virtioVolume", VmInstanceVO.class);

    public static String USERDATA_TOKEN = "userdata";
    @SensitiveTag(tokens = {"userdata"}, customizeOutput = UserdataTagOutputHandler.class)
    public static SystemTag USERDATA = new SystemTag(String.format("userdata::{%s}", USERDATA_TOKEN), VmInstanceVO.class);

    @Deprecated
    public static String SSHKEY_TOKEN = "sshkey";
    @Deprecated
    @SensitiveTag(tokens = {"sshkey"})
    public static SystemTag SSHKEY = new SystemTag(String.format("sshkey::{%s}", SSHKEY_TOKEN), VmInstanceVO.class);

    public static String ROOT_PASSWORD_TOKEN = "rootPassword";
    public static SystemTag ROOT_PASSWORD = new SystemTag(String.format("rootPassword::{%s}", ROOT_PASSWORD_TOKEN), VmInstanceVO.class);

    @Deprecated
    public static String ISO_DEVICEID_TOKEN = "deviceId";
    @Deprecated
    public static String ISO_TOKEN = "iso";
    @Deprecated
    public static SystemTag ISO = new SystemTag(String.format("iso::{%s}::{%s}", ISO_TOKEN, ISO_DEVICEID_TOKEN), VmInstanceVO.class);

    public static String BOOT_ORDER_TOKEN = "bootOrder";
    @AdminOnlyTag
    public static SystemTag BOOT_ORDER = new SystemTag(String.format("bootOrder::{%s}", BOOT_ORDER_TOKEN), VmInstanceVO.class);

    //this tag is deprecated.
    public static String CDROM_BOOT_ONCE_TOKEN = "cdromBootOnce";
    public static SystemTag CDROM_BOOT_ONCE = new SystemTag(String.format("cdromBootOnce::{%s}", CDROM_BOOT_ONCE_TOKEN), VmInstanceVO.class);

    public static String BOOT_ORDER_ONCE_TOKEN = "bootOrderOnce";
    public static SystemTag BOOT_ORDER_ONCE = new SystemTag(String.format("bootOrderOnce::{%s}", BOOT_ORDER_ONCE_TOKEN), VmInstanceVO.class);

    public static String CONSOLE_PASSWORD_TOKEN = "consolePassword";
    @SensitiveTag(tokens = {"consolePassword"})
    public static SystemTag CONSOLE_PASSWORD = new SystemTag(String.format("consolePassword::{%s}",CONSOLE_PASSWORD_TOKEN),VmInstanceVO.class);

    public static SystemTag getSensitivePatternedTagByHead(String tagHead) {
        if (CONSOLE_PASSWORD_TOKEN.equals(tagHead)) {
            return CONSOLE_PASSWORD;
        }
        if (SSHKEY_TOKEN.equals(tagHead)) {
            return SSHKEY;
        }
        if (USERDATA_TOKEN.equals(tagHead)) {
            return USERDATA;
        }
        return null;
    }

    public static List<SystemTag> sensitivePatternedTags() {
        return Arrays.asList(CONSOLE_PASSWORD, SSHKEY, USERDATA);
    }

    /**
     * Returns a plaintext tag for create/clone messages. The persist hook encrypts when vmEncryption=true.
     */
    public static String plainTagForCreateMessage(String resourceUuid, SystemTag systemTag, String rawTag) {
        if (!(systemTag.hasPattern) || !isSensitiveVmTag(rawTag)) {
            return rawTag;
        }

        String tagHead = Tags.first(rawTag);
        String plain = systemTag.getTokenByResourceUuid(resourceUuid, tagHead);
        if (plain == null) {
            return rawTag;
        }
        return systemTag.instantiateTag(map(e(tagHead, plain)));
    }

    public static String VM_ENCRYPTION_TOKEN = "vmEncryption";
    @NonCloneable
    public static SystemTag VM_ENCRYPTION = new SystemTag(String.format("vmEncryption::{%s}", VM_ENCRYPTION_TOKEN), VmInstanceVO.class);

    // set usbRedirect::true to enable usb redirect
    public static String USB_REDIRECT_TOKEN = "usbRedirect";
    public static SystemTag USB_REDIRECT = new SystemTag(String.format("usbRedirect::{%s}",USB_REDIRECT_TOKEN),VmInstanceVO.class);

    // set securityElementEnable::true to enable se redirect
    public static String SECURITY_ELEMENT_ENABLE_TOKEN = "securityElementEnable";
    public static SystemTag SECURITY_ELEMENT_ENABLE = new SystemTag(String.format("securityElementEnable::{%s}", SECURITY_ELEMENT_ENABLE_TOKEN),VmInstanceVO.class);

    // set rdpEnable::true to enable RDP tag
    public static String RDP_ENABLE_TOKEN = "RDPEnable";
    public static SystemTag RDP_ENABLE = new SystemTag(String.format("RDPEnable::{%s}",RDP_ENABLE_TOKEN),VmInstanceVO.class);

    // set VDIMonitorNumber::Integer to set how many monitor will be support for VDI
    public static String VDI_MONITOR_NUMBER_TOKEN = "VDIMonitorNumber";
    public static SystemTag VDI_MONITOR_NUMBER = new SystemTag(String.format("VDIMonitorNumber::{%s}",VDI_MONITOR_NUMBER_TOKEN),VmInstanceVO.class);

    public static String INSTANCEOFFERING_ONLINECHANGE_TOKEN = "instanceOfferingOnliechange";
    public static SystemTag INSTANCEOFFERING_ONLIECHANGE = new SystemTag(String.format("instanceOfferingOnlinechange::{%s}",INSTANCEOFFERING_ONLINECHANGE_TOKEN),VmInstanceVO.class);

    public static String PENDING_CAPACITY_CHNAGE_CPU_NUM_TOKEN = "cpuNum";
    public static String PENDING_CAPACITY_CHNAGE_CPU_SPEED_TOKEN = "cpuSpeed";
    public static String PENDING_CAPACITY_CHNAGE_MEMORY_TOKEN = "memory";
    public static SystemTag PENDING_CAPACITY_CHANGE = new SystemTag(
            String.format("pendingCapacityChange::cpuNum::{%s}::cpuSpeed::{%s}::memory::{%s}",  PENDING_CAPACITY_CHNAGE_CPU_NUM_TOKEN, PENDING_CAPACITY_CHNAGE_CPU_SPEED_TOKEN, PENDING_CAPACITY_CHNAGE_MEMORY_TOKEN),
            VmInstanceVO.class
    );
    public static String VM_INJECT_QEMUGA_TOKEN = "qemuga";
    public static SystemTag VM_INJECT_QEMUGA = new SystemTag(String.format("%s", VM_INJECT_QEMUGA_TOKEN), VmInstanceVO.class);

    @Deprecated
    public static String PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN = "primaryStorageUuidForDataVolume";
    /**
     * use {@link org.zstack.header.vm.DiskAO#setPrimaryStorageUuid(String)}
     */
    @Deprecated
    public static SystemTag PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME = new SystemTag(String.format("primaryStorageUuidForDataVolume::{%s}", PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN), VmInstanceVO.class);

    public static final String VM_SYSTEM_SERIAL_NUMBER_TOKEN = "vmSystemSerialNumber";
    public static SystemTag VM_SYSTEM_SERIAL_NUMBER = new SystemTag(String.format("vmSystemSerialNumber::{%s}", VM_SYSTEM_SERIAL_NUMBER_TOKEN), VmInstanceVO.class);

    public static String RELEASE_NIC_AFTER_DETACH_NIC_TOKEN = "releaseVmNicAfterDetachVmNic";
    public static SystemTag RELEASE_NIC_AFTER_DETACH_NIC = new SystemTag(String.format("releaseVmNicAfterDetachVmNic::{%s}", RELEASE_NIC_AFTER_DETACH_NIC_TOKEN), VmInstanceVO.class);

    public static String BOOT_MODE_TOKEN = "bootMode";
    public static SystemTag BOOT_MODE = new SystemTag(String.format("bootMode::{%s}", BOOT_MODE_TOKEN), VmInstanceVO.class);

    public static String BOOT_VOLUME_TOKEN = "bootVolume";
    public static SystemTag BOOT_VOLUME = new SystemTag(String.format("bootVolume::{%s}", BOOT_VOLUME_TOKEN), VmInstanceVO.class);

    public static String MAX_INSTANCE_PER_HOST_TOKEN = "maxInstancePerHost";
    public static SystemTag MAX_INSTANCE_PER_HOST = new SystemTag(String.format("maxInstancePerHost::{%s}", MAX_INSTANCE_PER_HOST_TOKEN), VmInstanceVO.class);

    public static SystemTag ADDITIONAL_QMP_ADDED = new SystemTag("additionalQmp", VmInstanceVO.class);

    public static String CLEAN_TRAFFIC_TOKEN = "cleanTraffic";
    public static SystemTag CLEAN_TRAFFIC = new SystemTag(String.format("cleanTraffic::{%s}", CLEAN_TRAFFIC_TOKEN), VmInstanceVO.class);

    public static String AUTO_SCALING_GROUP_UUID_TOKEN = "autoScalingGroupUuid";
    public static SystemTag AUTO_SCALING_GROUP_UUID = new SystemTag(String.format("autoScalingGroupUuid::{%s}", AUTO_SCALING_GROUP_UUID_TOKEN), VmInstanceVO.class);

    // only use by create vm
    public static String CD_ROM_LIST_TOKEN = "cdroms";
    public static String CD_ROM_0 = "cdrom0";
    public static String CD_ROM_1 = "cdrom1";
    public static String CD_ROM_2 = "cdrom2";
    public static SystemTag CREATE_VM_CD_ROM_LIST = new SystemTag(String.format("cdroms::{%s}::{%s}::{%s}", CD_ROM_0, CD_ROM_1, CD_ROM_2), VmInstanceVO.class);

    @Deprecated
    public static String CREATE_WITHOUT_CD_ROM_TOKEN = "createWithoutCdRom";
    /**
     * Deprecated. If VM has no cdrom, it will have 0 "CdRomVO".
     * Please use "cdroms::None::None::None" system tag instead when creating VM.
     */
    @Deprecated
    public static SystemTag CREATE_WITHOUT_CD_ROM = new SystemTag(String.format("createWithoutCdRom::{%s}", CREATE_WITHOUT_CD_ROM_TOKEN), VmInstanceVO.class);

    public static String CD_ROM_UUID_TOKEN = "cdromUuid";
    public static SystemTag CD_ROM = new SystemTag(String.format("cdromUuid::{%s}", CD_ROM_UUID_TOKEN), VmInstanceVO.class);

    public static String VM_NIC_MULTIQUEUE_TOKEN = "nicMultiQueueNum";
    public static String VM_NIC_MULTIQUEUE_L3_TOKEN = "l3Uuid";
    public static SystemTag VM_NIC_MULTIQUEUE = new SystemTag(String.format("nicMultiQueueNum::{%s}::{%s}", VM_NIC_MULTIQUEUE_L3_TOKEN, VM_NIC_MULTIQUEUE_TOKEN), VmInstanceVO.class);

    public static final String MACHINE_TYPE_TOKEN = "vmMachineType";
    public static SystemTag MACHINE_TYPE = new SystemTag(
            String.format("vmMachineType::{%s}", MACHINE_TYPE_TOKEN),
            VmInstanceVO.class
    );

    public static SystemTag PACKER_BUILD = new SystemTag("packer", VmInstanceVO.class);
    public static final String VM_PRIORITY_TOKEN = "vmPriority";
    public static SystemTag VM_PRIORITY = new SystemTag(String.format("vmPriority::{%s}", VM_PRIORITY_TOKEN), VmInstanceVO.class);

    public static String SOUND_TYPE_TOKEN = "soundType";
    public static SystemTag SOUND_TYPE = new SystemTag(String.format("soundType::{%s}", SOUND_TYPE_TOKEN), VmInstanceVO.class);

    public static String QXL_RAM_TOKEN = "ram";
    public static String QXL_VRAM_TOKEN = "vram";
    public static String QXL_VGAMEM_TOKEN = "vgamem";
    public static SystemTag QXL_MEMORY = new SystemTag(String.format("qxlMemory::{%s}::{%s}::{%s}", QXL_RAM_TOKEN, QXL_VRAM_TOKEN, QXL_VGAMEM_TOKEN), VmInstanceVO.class);

    public static String MULTIPLE_GATEWAY_TOKEN = "vmMultipleGateway";
    public static SystemTag MULTIPLE_GATEWAY = new SystemTag(String.format("vmMultipleGateway::{%s}", MULTIPLE_GATEWAY_TOKEN), VmInstanceVO.class);

    public static String VM_GUEST_TOOLS_VERSION_TOKEN = "guestToolsVersion";
    public static SystemTag VM_GUEST_TOOLS =
            new SystemTag(String.format("GuestTools::{%s}", VM_GUEST_TOOLS_VERSION_TOKEN), VmInstanceVO.class);

    public static String VM_RESOURCE_BINGDING_TOKEN = "resourceUUids";
    public static SystemTag VM_RESOURCE_BINGDING =
            new SystemTag(String.format("resourceBindings::{%s}", VM_RESOURCE_BINGDING_TOKEN), VmInstanceVO.class);

    public static String RX_SIZE_TOKEN = "rxBufferSize";
    public static String TX_SIZE_TOKEN = "txBufferSize";
    public static SystemTag VM_VRING_BUFFER_SIZE = new SystemTag(String.format("vRingBufferSize::{%s}::{%s}", RX_SIZE_TOKEN, TX_SIZE_TOKEN), VmInstanceVO.class);

    public static SystemTag VIRTIO = new SystemTag("driver::virtio", VmInstanceVO.class);

    public static String VM_IP_CHANGED_TOKEN = "ipChanged";
    public static SystemTag VM_IP_CHANGED =
            new SystemTag(String.format("ipChanged::{%s}", VM_IP_CHANGED_TOKEN), VmInstanceVO.class);

    public static String L3_UUID_TOKEN = "l3Uuid";
    public static String SECURITY_GROUP_UUIDS_TOKEN = "securityGroupUuids";
    public static SystemTag L3_NETWORK_SECURITY_GROUP_UUIDS_REF =
            new SystemTag(String.format("l3::{%s}::SecurityGroupUuids::{%s}", L3_UUID_TOKEN, SECURITY_GROUP_UUIDS_TOKEN),
                    VmInstanceVO.class);

    public static String SECURITY_GROUP_INGRESS_POLICY_TOKEN = "securityGroupIngressPolicy";
    public static String SECURITY_GROUP_EGRESS_POLICY_TOKEN = "securityGroupEgressPolicy";
    public static SystemTag SECURITY_GROUP_POLICY =
            new SystemTag(String.format("l3::{%s}::securityGroupIngressPolicy::{%s}::securityGroupEgressPolicy::{%s}",
                    L3_UUID_TOKEN, SECURITY_GROUP_INGRESS_POLICY_TOKEN, SECURITY_GROUP_EGRESS_POLICY_TOKEN),
                    VmInstanceVO.class);

    public static String DIRECTORY_UUID_TOKEN = "directoryUuid";
    public static SystemTag DIRECTORY_UUID = new SystemTag(String.format("directoryUuid::{%s}", DIRECTORY_UUID_TOKEN), VmInstanceVO.class);

    public static String USBDEVICE_UUID_TOKEN = "usbDeviceUuid";
    public static String usbDevice_attach_type_token = "attachType";
    public static SystemTag VM_ATTACH_USB = new SystemTag(String.format("usbDeviceUuid::{%s}::attachType::{%s}",
            USBDEVICE_UUID_TOKEN, usbDevice_attach_type_token), VmInstanceVO.class);

    public static class UserdataTagOutputHandler implements SensitiveTagOutputHandler {
        private final String chpasswd = "chpasswd";
        private final String list = "list";

        @Override
        public String desensitizeTag(SystemTag systemTag, String tag) {
            if (!systemTag.hasPattern || !(systemTag instanceof SystemTag)) {
                return tag;
            }
            SystemTag patternedSystemTag = (SystemTag) systemTag;

            String[] sensitiveTokens = patternedSystemTag.annotation.tokens();
            if (sensitiveTokens == null || sensitiveTokens.length == 0) {
                return tag;
            }

            Map<String, String> tokens = patternedSystemTag.getTokensByTag(tag);
            if (tokens == null || tokens.isEmpty()) {
                return tag;
            }

            for (String t : sensitiveTokens) {
                String base64Pattern = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$";
                String userdata = tokens.get(t);
                if (userdata != null && userdata.startsWith(VmSensitiveTagEncryptor.ENC_PREFIX)) {
                    return tag.replace(userdata, "*****");
                }
                if (Pattern.matches(base64Pattern, userdata)) {
                    userdata = new String(Base64.getDecoder().decode(userdata.getBytes()));
                }

                Yaml yaml = new Yaml();
                Object obj = yaml.load(userdata);
                if (!(obj instanceof LinkedHashMap)) {
                    return tag;
                }
                LinkedHashMap<String, Object> userdataMap = (LinkedHashMap<String, Object>) obj;
                if (userdataMap.isEmpty()) {
                    return tag;
                }

                Object chpasswdValue = userdataMap.get(chpasswd);
                if (!(chpasswdValue instanceof LinkedHashMap)) {
                    return tag;
                }
                LinkedHashMap<String, Object> chpasswdMap = (LinkedHashMap<String, Object>) chpasswdValue;

                Object listValue = chpasswdMap.get(list);
                if (!(listValue instanceof String) || listValue.equals("")) {
                    return tag;
                }
                /*
                 * #cloud-config
                 * chpasswd:
                 *   list: |
                 *     root:password  ——>  *****:*****
                 *   expire: False
                 * */
                chpasswdMap.replace(list, "*****:*****\n");

                DumperOptions options = new DumperOptions();
                options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);

                yaml = new Yaml(options);
                String maskedUserdata = yaml.dump(userdataMap);
                maskedUserdata = "#cloud-config\n" + maskedUserdata;
                tokens.put(t, new String(Base64.getEncoder().encode(maskedUserdata.getBytes())));
            }
            return patternedSystemTag.instantiateTag(tokens);
        }
    }

    public static SystemTag VM_STATE_PAUSED_AFTER_MIGRATE = new SystemTag(("vmPausedAfterMigrate"), VmInstanceVO.class);

    public static String VM_METADATA_REGISTERING_MN_UUID_TOKEN = "registeringMnUuid";
    public static SystemTag VM_METADATA_REGISTERING_MN_UUID = new SystemTag(
            String.format("vmMetadata::registeringMnUuid::{%s}", VM_METADATA_REGISTERING_MN_UUID_TOKEN), VmInstanceVO.class);
}
