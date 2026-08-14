package org.zstack.guesttools;

import org.zstack.header.configuration.PythonClass;
import org.zstack.kvm.KVMConstant;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 2019-09-17.
 */
@PythonClass
public interface GuestToolsConstant {
    String SERVICE_ID = "guest.tools";
    String ACTION_CATEGORY = "guest.tools";

    String ATTACH_GUEST_TOOLS_ISO_TO_VM_PATH = "/vm/guesttools/attachiso";
    String DETACH_GUEST_TOOLS_ISO_FROM_VM_PATH = "/vm/guesttools/detachiso";
    String GET_VM_GUEST_TOOLS_INFO_PATH = "/vm/guesttools/getinfo";
    String GET_VM_METRICS_ROUTING_STATUS_PATH = "/vm/guesttools/getroutingstatus";

    // to download guesttools iso to host
    String ANSIBLE_PLAYBOOK_NAME = "guesttools.py";
    String ANSIBLE_MODULE_PATH = "ansible/guesttools";

    String GUEST_TOOLS_ISO_DST_PATH = "/var/lib/zstack/guesttools/GuestTools.iso";
    String GUEST_TOOLS_LINUX_ISO_DST_PATH = "/var/lib/zstack/guesttools/GuestTools_linux.iso";

    // only support guest tools on kvm platform for now
    List<String> HYPERVISOR_TYPES_SUPPORT_GUEST_TOOLS = Collections.singletonList(
            KVMConstant.KVM_HYPERVISOR_TYPE
    );

    String GUEST_TOOLS_NOT_CONNNETED = "QEMU guest agent is not connected";
    String FLOW_CHAIN_KEY_GUEST_TOOLS_VERSION = "version";

    String GUEST_TOOLS_ARCH_GENERAL = "general";

    String QGA_NIC_INFO_IPV4_GATEWAY_PREFIX = "gateway:";
    String QGA_NIC_INFO_IPV6_GATEWAY_PREFIX = "gateway6:";

    String OU_PATTERN = "^(OU=[^,=]+,)*(DC=[^,=]+)(,DC=[^,=]+)*$";

    String VM_CUSTOM_SPECIFICATION_NAME_PREFIX = "custom-specification-for-vm-";

    String WINDOWS_ADMIN_USERNAME = "Administrator";
    String LINUX_ROOT_USERNAME = "root";

    String CUSTOM_SPECIFICATION_SUPPORT_VERSION_FOR_WINDOWS = "1.5.7";

    enum VmOsPlatform {
        Linux,
        Windows
    }
}
