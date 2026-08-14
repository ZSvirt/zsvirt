package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = "all")
public class APIGetVmXmlReply extends APIReply {
    private boolean match;

    private String runningXml;

    private String userDefinedXml;

    public boolean isMatch() {
        return match;
    }

    public void setMatch(boolean match) {
        this.match = match;
    }

    public String getRunningXml() {
        return runningXml;
    }

    public void setRunningXml(String runningXml) {
        this.runningXml = runningXml;
    }

    public String getUserDefinedXml() {
        return userDefinedXml;
    }

    public void setUserDefinedXml(String userDefinedXml) {
        this.userDefinedXml = userDefinedXml;
    }

    public static APIGetVmXmlReply __example__() {
        APIGetVmXmlReply reply = new APIGetVmXmlReply();

        reply.setMatch(false);

        reply.setRunningXml("<domain type='kvm' id='1' xmlns:qemu='http://libvirt.org/schemas/domain/qemu/1.0'>\n" +
                "  <name>c059963bca194587912d7ab8ed4e3c2e</name>\n" +
                "  <uuid>c059963b-ca19-4587-912d-7ab8ed4e3c2e</uuid>\n" +
                "  <description>wuxia-vpp</description>\n" +
                "  <metadata xmlns:zs=\"http://zstack.org\">\n" +
                "    <zs:zstack>\n" +
                "      <internalId>144</internalId>\n" +
                "      <hostManagementIp>172.31.6.14</hostManagementIp>\n" +
                "    </zs:zstack>\n" +
                "  </metadata>\n" +
                "  <memory unit='KiB'>8388608</memory>\n" +
                "  <currentMemory unit='KiB'>8388608</currentMemory>\n" +
                "  <vcpu placement='static'>8</vcpu>\n" +
                "  <cputune>\n" +
                "    <shares>512</shares>\n" +
                "  </cputune>\n" +
                "  <resource>\n" +
                "    <partition>/machine</partition>\n" +
                "  </resource>\n" +
                "  <sysinfo type='smbios'>\n" +
                "    <system>\n" +
                "      <entry name='serial'>a4819822-4c79-4bec-b7d0-b3a61d1b18d4</entry>\n" +
                "    </system>\n" +
                "    <chassis>\n" +
                "      <entry name='asset'>www.zstack.io</entry>\n" +
                "    </chassis>\n" +
                "  </sysinfo>\n" +
                "  <os>\n" +
                "    <type arch='x86_64' machine='pc-i440fx-rhel7.6.0'>hvm</type>\n" +
                "    <bootmenu enable='yes'/>\n" +
                "    <smbios mode='sysinfo'/>\n" +
                "  </os>\n" +
                "  <features>\n" +
                "    <acpi/>\n" +
                "    <apic/>\n" +
                "    <pae/>\n" +
                "    <hyperv>\n" +
                "      <relaxed state='on'/>\n" +
                "      <vapic state='on'/>\n" +
                "      <spinlocks state='on' retries='4096'/>\n" +
                "      <vendor_id state='on' value='ZStack_Org'/>\n" +
                "    </hyperv>\n" +
                "    <ioapic driver='kvm'/>\n" +
                "  </features>\n" +
                "  <cpu mode='host-passthrough' check='none'>\n" +
                "    <topology sockets='1' cores='8' threads='1'/>\n" +
                "  </cpu>\n" +
                "  <clock offset='utc'/>\n" +
                "  <on_poweroff>destroy</on_poweroff>\n" +
                "  <on_reboot>restart</on_reboot>\n" +
                "  <on_crash>restart</on_crash>\n" +
                "  <devices>\n" +
                "    <emulator>/usr/libexec/qemu-kvm</emulator>\n" +
                "    <disk type='network' device='disk'>\n" +
                "      <driver name='qemu' type='raw'/>\n" +
                "      <auth username='zstack'>\n" +
                "        <secret type='ceph' uuid='6a1846f5-d6f7-4cc9-a1e9-ec13e3b564db'/>\n" +
                "      </auth>\n" +
                "      <source protocol='rbd' name='pool-0d34b7947a9a460db5e6473e5ca08124/393739c4bc9f4d77a4ffb43d0b72379c'>\n" +
                "        <host name='192.168.10.13' port='6789'/>\n" +
                "        <host name='192.168.10.12' port='6789'/>\n" +
                "        <host name='192.168.10.11' port='6789'/>\n" +
                "      </source>\n" +
                "      <target dev='vda' bus='virtio'/>\n" +
                "      <boot order='1'/>\n" +
                "      <alias name='virtio-disk0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x0a' function='0x0'/>\n" +
                "    </disk>\n" +
                "    <disk type='file' device='cdrom'>\n" +
                "      <driver name='qemu'/>\n" +
                "      <target dev='hdc' bus='ide'/>\n" +
                "      <readonly/>\n" +
                "      <alias name='ide0-0-1'/>\n" +
                "      <address type='drive' controller='0' bus='0' target='0' unit='1'/>\n" +
                "    </disk>\n" +
                "    <controller type='scsi' index='0' model='virtio-scsi'>\n" +
                "      <alias name='scsi0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x04' function='0x0'/>\n" +
                "    </controller>\n" +
                "    <controller type='usb' index='0' model='piix3-uhci'>\n" +
                "      <alias name='usb'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x2'/>\n" +
                "    </controller>\n" +
                "    <controller type='usb' index='1' model='ehci'>\n" +
                "      <alias name='usb1'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x05' function='0x0'/>\n" +
                "    </controller>\n" +
                "    <controller type='usb' index='2' model='nec-xhci'>\n" +
                "      <alias name='usb2'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x06' function='0x0'/>\n" +
                "    </controller>\n" +
                "    <controller type='usb' index='3' model='ehci'>\n" +
                "      <alias name='usb3'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x07' function='0x0'/>\n" +
                "    </controller>\n" +
                "    <controller type='usb' index='4' model='nec-xhci'>\n" +
                "      <alias name='usb4'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x08' function='0x0'/>\n" +
                "    </controller>\n" +
                "    <controller type='pci' index='0' model='pci-root'>\n" +
                "      <alias name='pci.0'/>\n" +
                "    </controller>\n" +
                "    <controller type='ide' index='0'>\n" +
                "      <alias name='ide'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x1'/>\n" +
                "    </controller>\n" +
                "    <controller type='virtio-serial' index='0'>\n" +
                "      <alias name='virtio-serial0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x09' function='0x0'/>\n" +
                "    </controller>\n" +
                "    <interface type='bridge'>\n" +
                "      <mac address='fa:54:30:60:39:00'/>\n" +
                "      <source bridge='br_bond0_37'/>\n" +
                "      <target dev='vnic144.0'/>\n" +
                "      <model type='virtio'/>\n" +
                "      <mtu size='1500'/>\n" +
                "      <alias name='net0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x03' function='0x0'/>\n" +
                "    </interface>\n" +
                "    <interface type='hostdev' managed='yes'>\n" +
                "      <mac address='fa:15:c0:17:d2:01'/>\n" +
                "      <driver name='vfio'/>\n" +
                "      <source>\n" +
                "        <address type='pci' domain='0x0000' bus='0x19' slot='0x11' function='0x0'/>\n" +
                "      </source>\n" +
                "      <alias name='hostdev0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x0b' function='0x0'/>\n" +
                "    </interface>\n" +
                "    <interface type='hostdev' managed='yes'>\n" +
                "      <mac address='fa:c6:25:26:44:02'/>\n" +
                "      <driver name='vfio'/>\n" +
                "      <source>\n" +
                "        <address type='pci' domain='0x0000' bus='0x19' slot='0x11' function='0x3'/>\n" +
                "      </source>\n" +
                "      <alias name='hostdev1'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x0c' function='0x0'/>\n" +
                "    </interface>\n" +
                "    <serial type='pty'>\n" +
                "      <source path='/dev/pts/1'/>\n" +
                "      <target type='isa-serial' port='0'>\n" +
                "        <model name='isa-serial'/>\n" +
                "      </target>\n" +
                "      <alias name='serial0'/>\n" +
                "    </serial>\n" +
                "    <console type='pty' tty='/dev/pts/1'>\n" +
                "      <source path='/dev/pts/1'/>\n" +
                "      <target type='serial' port='0'/>\n" +
                "      <alias name='serial0'/>\n" +
                "    </console>\n" +
                "    <channel type='unix'>\n" +
                "      <source mode='bind' path='/var/lib/libvirt/qemu/c059963bca194587912d7ab8ed4e3c2e'/>\n" +
                "      <target type='virtio' name='org.qemu.guest_agent.0' state='connected'/>\n" +
                "      <alias name='channel0'/>\n" +
                "      <address type='virtio-serial' controller='0' bus='0' port='1'/>\n" +
                "    </channel>\n" +
                "    <channel type='spicevmc'>\n" +
                "      <target type='virtio' name='com.redhat.spice.0' state='disconnected'/>\n" +
                "      <alias name='channel1'/>\n" +
                "      <address type='virtio-serial' controller='0' bus='0' port='2'/>\n" +
                "    </channel>\n" +
                "    <input type='tablet' bus='usb'>\n" +
                "      <alias name='input0'/>\n" +
                "      <address type='usb' bus='0' port='1'/>\n" +
                "    </input>\n" +
                "    <input type='mouse' bus='ps2'>\n" +
                "      <alias name='input1'/>\n" +
                "    </input>\n" +
                "    <input type='keyboard' bus='ps2'>\n" +
                "      <alias name='input2'/>\n" +
                "    </input>\n" +
                "    <graphics type='vnc' port='5900' autoport='yes' listen='0.0.0.0'>\n" +
                "      <listen type='address' address='0.0.0.0'/>\n" +
                "    </graphics>\n" +
                "    <video>\n" +
                "      <model type='cirrus' vram='16384' heads='1' primary='yes'/>\n" +
                "      <alias name='video0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x02' function='0x0'/>\n" +
                "    </video>\n" +
                "    <redirdev bus='usb' type='spicevmc'>\n" +
                "      <alias name='redir0'/>\n" +
                "      <address type='usb' bus='3' port='1'/>\n" +
                "    </redirdev>\n" +
                "    <redirdev bus='usb' type='spicevmc'>\n" +
                "      <alias name='redir1'/>\n" +
                "      <address type='usb' bus='3' port='2'/>\n" +
                "    </redirdev>\n" +
                "    <redirdev bus='usb' type='spicevmc'>\n" +
                "      <alias name='redir2'/>\n" +
                "      <address type='usb' bus='4' port='1'/>\n" +
                "    </redirdev>\n" +
                "    <redirdev bus='usb' type='spicevmc'>\n" +
                "      <alias name='redir3'/>\n" +
                "      <address type='usb' bus='4' port='2'/>\n" +
                "    </redirdev>\n" +
                "    <memballoon model='virtio'>\n" +
                "      <stats period='10'/>\n" +
                "      <alias name='balloon0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x0d' function='0x0'/>\n" +
                "    </memballoon>\n" +
                "  </devices>\n" +
                "  <seclabel type='none' model='none'/>\n" +
                "  <seclabel type='dynamic' model='dac' relabel='yes'>\n" +
                "    <label>+0:+0</label>\n" +
                "    <imagelabel>+0:+0</imagelabel>\n" +
                "  </seclabel>\n" +
                "  <qemu:commandline>\n" +
                "    <qemu:arg value='-qmp'/>\n" +
                "    <qemu:arg value='unix:/var/lib/libvirt/qemu/zstack/c059963bca194587912d7ab8ed4e3c2e.sock,server,nowait'/>\n" +
                "  </qemu:commandline>");

        reply.setUserDefinedXml("<domain type='kvm' id='1' xmlns:qemu='http://libvirt.org/schemas/domain/qemu/1.0'>\n" +
                "  <name>c059963bca194587912d7ab8ed4e3c2e</name>\n" +
                "  <uuid>c059963b-ca19-4587-912d-7ab8ed4e3c2e</uuid>\n" +
                "  <description>wuxia-vpp</description>\n" +
                "  <metadata xmlns:zs=\"http://zstack.org\">\n" +
                "    <zs:zstack>\n" +
                "      <internalId>144</internalId>\n" +
                "      <hostManagementIp>172.31.6.14</hostManagementIp>\n" +
                "    </zs:zstack>\n" +
                "  </metadata>\n" +
                "  <memory unit='KiB'>8388608</memory>\n" +
                "  <currentMemory unit='KiB'>8388608</currentMemory>\n" +
                "  <vcpu placement='static'>8</vcpu>\n" +
                "  <cputune>\n" +
                "    <shares>512</shares>\n" +
                "  </cputune>\n" +
                "  <resource>\n" +
                "    <partition>/machine</partition>\n" +
                "  </resource>\n" +
                "  <sysinfo type='smbios'>\n" +
                "    <system>\n" +
                "      <entry name='serial'>a4819822-4c79-4bec-b7d0-b3a61d1b18d4</entry>\n" +
                "    </system>\n" +
                "    <chassis>\n" +
                "      <entry name='asset'>www.zstack.io</entry>\n" +
                "    </chassis>\n" +
                "  </sysinfo>\n" +
                "  <os>\n" +
                "    <type arch='x86_64' machine='pc-i440fx-rhel7.6.0'>hvm</type>\n" +
                "    <bootmenu enable='yes'/>\n" +
                "    <smbios mode='sysinfo'/>\n" +
                "  </os>\n" +
                "  <features>\n" +
                "    <acpi/>\n" +
                "    <apic/>\n" +
                "    <pae/>\n" +
                "    <hyperv>\n" +
                "      <relaxed state='on'/>\n" +
                "      <vapic state='on'/>\n" +
                "      <spinlocks state='on' retries='4096'/>\n" +
                "      <vendor_id state='on' value='ZStack_Org'/>\n" +
                "    </hyperv>\n" +
                "    <ioapic driver='kvm'/>\n" +
                "  </features>\n" +
                "  <cpu mode='host-passthrough' check='none'>\n" +
                "    <topology sockets='1' cores='8' threads='1'/>\n" +
                "  </cpu>\n" +
                "  <clock offset='utc'/>\n" +
                "  <on_poweroff>destroy</on_poweroff>\n" +
                "  <on_reboot>restart</on_reboot>\n" +
                "  <on_crash>restart</on_crash>\n" +
                "  <devices>\n" +
                "    <emulator>/usr/libexec/qemu-kvm</emulator>\n" +
                "    <disk type='network' device='disk'>\n" +
                "      <driver name='qemu' type='raw'/>\n" +
                "      <auth username='zstack'>\n" +
                "        <secret type='ceph' uuid='6a1846f5-d6f7-4cc9-a1e9-ec13e3b564db'/>\n" +
                "      </auth>\n" +
                "      <source protocol='rbd' name='pool-0d34b7947a9a460db5e6473e5ca08124/393739c4bc9f4d77a4ffb43d0b72379c'>\n" +
                "        <host name='192.168.10.13' port='6789'/>\n" +
                "        <host name='192.168.10.12' port='6789'/>\n" +
                "        <host name='192.168.10.11' port='6789'/>\n" +
                "      </source>\n" +
                "      <target dev='vda' bus='virtio'/>\n" +
                "      <boot order='1'/>\n" +
                "      <alias name='virtio-disk0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x0a' function='0x0'/>\n" +
                "    </disk>\n" +
                "    <disk type='file' device='cdrom'>\n" +
                "      <driver name='qemu'/>\n" +
                "      <target dev='hdc' bus='ide'/>\n" +
                "      <readonly/>\n" +
                "      <alias name='ide0-0-1'/>\n" +
                "      <address type='drive' controller='0' bus='0' target='0' unit='1'/>\n" +
                "    </disk>\n" +
                "    <controller type='scsi' index='0' model='virtio-scsi'>\n" +
                "      <alias name='scsi0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x04' function='0x0'/>\n" +
                "    </controller>\n" +
                "    <controller type='usb' index='0' model='piix3-uhci'>\n" +
                "      <alias name='usb'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x2'/>\n" +
                "    </controller>\n" +
                "    <controller type='usb' index='1' model='ehci'>\n" +
                "      <alias name='usb1'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x05' function='0x0'/>\n" +
                "    </controller>\n" +
                "    <controller type='usb' index='2' model='nec-xhci'>\n" +
                "      <alias name='usb2'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x06' function='0x0'/>\n" +
                "    </controller>\n" +
                "    <controller type='usb' index='3' model='ehci'>\n" +
                "      <alias name='usb3'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x07' function='0x0'/>\n" +
                "    </controller>\n" +
                "    <controller type='usb' index='4' model='nec-xhci'>\n" +
                "      <alias name='usb4'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x08' function='0x0'/>\n" +
                "    </controller>\n" +
                "    <controller type='pci' index='0' model='pci-root'>\n" +
                "      <alias name='pci.0'/>\n" +
                "    </controller>\n" +
                "    <controller type='ide' index='0'>\n" +
                "      <alias name='ide'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x1'/>\n" +
                "    </controller>\n" +
                "    <controller type='virtio-serial' index='0'>\n" +
                "      <alias name='virtio-serial0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x09' function='0x0'/>\n" +
                "    </controller>\n" +
                "    <interface type='bridge'>\n" +
                "      <mac address='fa:54:30:60:39:00'/>\n" +
                "      <source bridge='br_bond0_37'/>\n" +
                "      <target dev='vnic144.0'/>\n" +
                "      <model type='virtio'/>\n" +
                "      <mtu size='1500'/>\n" +
                "      <alias name='net0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x03' function='0x0'/>\n" +
                "    </interface>\n" +
                "    <interface type='hostdev' managed='yes'>\n" +
                "      <mac address='fa:15:c0:17:d2:01'/>\n" +
                "      <driver name='vfio'/>\n" +
                "      <source>\n" +
                "        <address type='pci' domain='0x0000' bus='0x19' slot='0x11' function='0x0'/>\n" +
                "      </source>\n" +
                "      <alias name='hostdev0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x0b' function='0x0'/>\n" +
                "    </interface>\n" +
                "    <interface type='hostdev' managed='yes'>\n" +
                "      <mac address='fa:c6:25:26:44:02'/>\n" +
                "      <driver name='vfio'/>\n" +
                "      <source>\n" +
                "        <address type='pci' domain='0x0000' bus='0x19' slot='0x11' function='0x3'/>\n" +
                "      </source>\n" +
                "      <alias name='hostdev1'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x0c' function='0x0'/>\n" +
                "    </interface>\n" +
                "    <serial type='pty'>\n" +
                "      <source path='/dev/pts/1'/>\n" +
                "      <target type='isa-serial' port='0'>\n" +
                "        <model name='isa-serial'/>\n" +
                "      </target>\n" +
                "      <alias name='serial0'/>\n" +
                "    </serial>\n" +
                "    <console type='pty' tty='/dev/pts/1'>\n" +
                "      <source path='/dev/pts/1'/>\n" +
                "      <target type='serial' port='0'/>\n" +
                "      <alias name='serial0'/>\n" +
                "    </console>\n" +
                "    <channel type='unix'>\n" +
                "      <source mode='bind' path='/var/lib/libvirt/qemu/c059963bca194587912d7ab8ed4e3c2e'/>\n" +
                "      <target type='virtio' name='org.qemu.guest_agent.0' state='connected'/>\n" +
                "      <alias name='channel0'/>\n" +
                "      <address type='virtio-serial' controller='0' bus='0' port='1'/>\n" +
                "    </channel>\n" +
                "    <channel type='spicevmc'>\n" +
                "      <target type='virtio' name='com.redhat.spice.0' state='disconnected'/>\n" +
                "      <alias name='channel1'/>\n" +
                "      <address type='virtio-serial' controller='0' bus='0' port='2'/>\n" +
                "    </channel>\n" +
                "    <input type='tablet' bus='usb'>\n" +
                "      <alias name='input0'/>\n" +
                "      <address type='usb' bus='0' port='1'/>\n" +
                "    </input>\n" +
                "    <input type='mouse' bus='ps2'>\n" +
                "      <alias name='input1'/>\n" +
                "    </input>\n" +
                "    <input type='keyboard' bus='ps2'>\n" +
                "      <alias name='input2'/>\n" +
                "    </input>\n" +
                "    <graphics type='vnc' port='5900' autoport='yes' listen='0.0.0.0'>\n" +
                "      <listen type='address' address='0.0.0.0'/>\n" +
                "    </graphics>\n" +
                "    <video>\n" +
                "      <model type='cirrus' vram='16384' heads='1' primary='yes'/>\n" +
                "      <alias name='video0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x02' function='0x0'/>\n" +
                "    </video>\n" +
                "    <redirdev bus='usb' type='spicevmc'>\n" +
                "      <alias name='redir0'/>\n" +
                "      <address type='usb' bus='3' port='1'/>\n" +
                "    </redirdev>\n" +
                "    <redirdev bus='usb' type='spicevmc'>\n" +
                "      <alias name='redir1'/>\n" +
                "      <address type='usb' bus='3' port='2'/>\n" +
                "    </redirdev>\n" +
                "    <redirdev bus='usb' type='spicevmc'>\n" +
                "      <alias name='redir2'/>\n" +
                "      <address type='usb' bus='4' port='1'/>\n" +
                "    </redirdev>\n" +
                "    <redirdev bus='usb' type='spicevmc'>\n" +
                "      <alias name='redir3'/>\n" +
                "      <address type='usb' bus='4' port='2'/>\n" +
                "    </redirdev>\n" +
                "    <memballoon model='virtio'>\n" +
                "      <stats period='10'/>\n" +
                "      <alias name='balloon0'/>\n" +
                "      <address type='pci' domain='0x0000' bus='0x00' slot='0x0d' function='0x0'/>\n" +
                "    </memballoon>\n" +
                "  </devices>\n" +
                "  <seclabel type='none' model='none'/>\n" +
                "  <seclabel type='dynamic' model='dac' relabel='yes'>\n" +
                "    <label>+0:+0</label>\n" +
                "    <imagelabel>+0:+0</imagelabel>\n" +
                "  </seclabel>\n");

        return reply;
    }
}
