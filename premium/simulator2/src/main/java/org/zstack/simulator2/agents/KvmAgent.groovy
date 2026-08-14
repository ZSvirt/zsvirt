package org.zstack.simulator2.agents

import com.google.common.collect.ImmutableMap
import org.springframework.http.HttpEntity
import org.zstack.compute.bonding.HostNetworkBondingConstant
import org.zstack.compute.host.MevocoKVMAgentCommands
import org.zstack.compute.host.MevocoKVMConstant
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.guesttools.GuestToolsAgentStatus
import org.zstack.guesttools.GuestToolsConstant
import org.zstack.guesttools.kvm.GuestToolsKvmCommands
import org.zstack.ha.HaKvmHostSiblingChecker
import org.zstack.ha.SelfFencerKvmBackend
import org.zstack.header.Constants
import org.zstack.header.agent.AgentResponse
import org.zstack.header.host.*
import org.zstack.header.network.l3.UsedIpTO
import org.zstack.header.storage.snapshot.TakeSnapshotsOnKvmJobStruct
import org.zstack.header.storage.snapshot.TakeSnapshotsOnKvmResultStruct
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.header.vm.additions.VmHostFileContentFormat
import org.zstack.header.vm.additions.VmHostFileType
import org.zstack.header.vm.devices.DeviceAddress
import org.zstack.header.vm.devices.VirtualDeviceInfo
import org.zstack.header.volume.VolumeType
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMGlobalProperty
import org.zstack.kvm.VolumeTO
import org.zstack.mttyDevice.KvmMttyDeviceBackend.MttyDeviceKvmBackend
import org.zstack.mttyDevice.MttyDeviceTO
import org.zstack.network.hostNetworkInterface.HostKernelInterfaceTO
import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceInventory
import org.zstack.network.hostNetworkInterface.lldp.LldpConstant
import org.zstack.network.hostNetworkInterface.lldp.LldpKvmAgentCommands
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanKvmAgentCommands
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant
import org.zstack.pciDevice.KvmPciDeviceBackend.PciDeviceKvmBackend
import org.zstack.pciDevice.PciDeviceTO
import org.zstack.pciDevice.PciDeviceType
import org.zstack.pciDevice.PciDeviceVO
import org.zstack.pciDevice.PciDeviceVO_
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceBase
import org.zstack.simulator2.Simulator
import org.zstack.simulator2.SimulatorGlobalProperty
import org.zstack.simulator2.config.device.FcHbaDevice
import org.zstack.simulator2.config.device.MdevDeviceSpec
import org.zstack.simulator2.config.device.MttyDevice
import org.zstack.simulator2.config.device.PciDevice
import org.zstack.simulator2.config.device.UsbDevice
import org.zstack.simulator2.config.host.KvmHost
import org.zstack.simulator2.config.primaryStorage.BlockDevice
import org.zstack.sshkeypair.SshKeyPairBase
import org.zstack.sshkeypair.SshKeyPairConstant
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.device.StorageDeviceKvmCommands
import org.zstack.storage.device.fibreChannel.FiberChannelLunStruct
import org.zstack.storage.device.hba.HbaDeviceStruct
import org.zstack.storage.device.iscsi.IscsiLunStruct
import org.zstack.storage.device.iscsi.IscsiTargetStruct
import org.zstack.storage.device.localRaid.RaidPhysicalDriveStruct
import org.zstack.storage.migration.KvmBlockLiveMigrationWorkFlow
import org.zstack.storage.primary.filesystem.AbstractFileSystemHostHeartbeatChecker
import org.zstack.storage.primary.shareblock.ShareBlockHostHeartbeatChecker
import org.zstack.storage.primary.sharedblock.HaSanlockHostChecker
import org.zstack.usbDevice.KvmUsbDeviceBackend.UsbDeviceKvmBackend
import org.zstack.usbDevice.UsbDeviceTO
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.zwatch.prometheus.KvmHostScrape

import javax.persistence.Tuple

import static org.zstack.kvm.KVMAgentCommands.*

/**
 * Created by xing5 on 2017/9/16.
 */
class KvmAgent extends Agent {
    private String version
    private String sendCommandUrl

    KvmAgent(Simulator simulator) {
        super(simulator)
        this.version = simulator.getVersion()
        this.sendCommandUrl = simulator.getSendCommandUrl()
        KVMGlobalProperty.AGENT_PORT = SimulatorGlobalProperty.SIMULATOR_AGENT_PORT
    }

    protected KvmHost find(HttpEntity<String> e) {
        def uuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)

        String ip = Q.New(HostVO.class).select(HostVO_.managementIp).eq(HostVO_.uuid, uuid).findValue()
        assert ip != null : "cannot find KVM host[uuid:${uuid}] in database"

        def ret = simulator.sqlite.find("select * from ${KvmHost.class.simpleName} where ip = '${ip}'", KvmHost.class)
        assert ret != null : "KVM host[ip:${ip}] has no simulator"

        return ret
    }

    @Override
    void setupAgentHandler() {
        handle(KVMConstant.KVM_HOST_CAPACITY_PATH) { HttpEntity<String> e ->
            def kvm = find(e)

            def rsp = new KVMAgentCommands.HostCapacityResponse()
            rsp.success = true
            rsp.usedCpu = kvm.usedCpu
            rsp.cpuNum = kvm.cpuNum
            rsp.totalMemory = kvm.totalMemory
            rsp.usedMemory = kvm.usedMemory
            rsp.cpuSpeed = 1
            rsp.cpuSockets = kvm.cpuSockets
            rsp.cpuCoreNum = kvm.cpuCores
            return rsp
        }

        handle(KVMConstant.KVM_HARDEN_CONSOLE_PATH) {
            return new KVMAgentCommands.AgentResponse()
        }

        handle(KVMConstant.KVM_DELETE_CONSOLE_FIREWALL_PATH) {
            return new KVMAgentCommands.AgentResponse()
        }

        handle(KVMConstant.KVM_VM_UPDATE_PRIORITY_PATH) {
            return new KVMAgentCommands.UpdateVmPriorityRsp()
        }

        handle(KVMConstant.KVM_VM_UPDATE_CPU_QUOTA_PATH) {
            return new KVMAgentCommands.UpdateVmCpuQuotaRsp()
        }

        handle(KVMConstant.KVM_VM_CHECK_STATE) { HttpEntity<String> e ->
            KVMAgentCommands.CheckVmStateCmd cmd = json(e, KVMAgentCommands.CheckVmStateCmd.class)
            List<VmInstanceVO> vms = Q.New(VmInstanceVO.class).in(VmInstanceVO_.uuid, cmd.vmUuids).list()
            KVMAgentCommands.CheckVmStateRsp rsp = new KVMAgentCommands.CheckVmStateRsp()
            rsp.states = [:]
            vms.each {
                def kstate = KVMConstant.KvmVmState.fromVmInstanceState(it.state)
                if (kstate != null) {
                    rsp.states[(it.uuid)] = kstate.toString()
                } else {
                    rsp.states[(it.uuid)] = KVMConstant.KvmVmState.Shutdown.toString()
                }
            }

            return rsp
        }

        handle(KVMConstant.KVM_ATTACH_NIC_PATH) {
            return new KVMAgentCommands.AttachNicResponse()
        }

        handle(KVMConstant.KVM_DETACH_NIC_PATH) {
            return new KVMAgentCommands.DetachNicRsp()
        }

        handle(KVMConstant.KVM_UPDATE_NIC_PATH) {
            return new KVMAgentCommands.UpdateNicRsp()
        }

        handle(KVMConstant.KVM_ATTACH_ISO_PATH) {
            return new KVMAgentCommands.AttachIsoRsp()
        }

        handle(KVMConstant.KVM_DETACH_ISO_PATH) {
            return new KVMAgentCommands.DetachIsoRsp()
        }

        handle(KVMConstant.KVM_MERGE_SNAPSHOT_PATH) {
            return new KVMAgentCommands.MergeSnapshotRsp()
        }

        handle(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH) {
            def rsp = new KVMAgentCommands.TakeSnapshotResponse()
            rsp.newVolumeInstallPath = "/new/volume/install/path"
            rsp.snapshotInstallPath = "/snapshot/install/path"
            rsp.size = 1
            return rsp
        }

        handle(KVMConstant.KVM_PING_PATH) { HttpEntity<String> e ->
            KVMAgentCommands.PingCmd cmd = json(e, KVMAgentCommands.PingCmd.class)
            assert null != cmd
            assert null != cmd.hostUuid

            def rsp = new KVMAgentCommands.PingResponse()
            rsp.hostUuid = cmd.hostUuid
            rsp.version = this.version
            rsp.sendCommandUrl = this.sendCommandUrl
            return rsp
        }

        handle(KVMConstant.KVM_CONNECT_PATH) { HttpEntity<String> e ->
            def rsp = new KVMAgentCommands.ConnectResponse()
            rsp.success = true
            rsp.libvirtVersion = "1.0.0"
            rsp.qemuVersion = "1.3.0"
            return rsp
        }

        handle(KVMConstant.KVM_ECHO_PATH) { HttpEntity<String> e ->
            return [:]
        }

        handle(KVMConstant.KVM_DETACH_VOLUME) {
            return new KVMAgentCommands.DetachDataVolumeResponse()
        }

        handle(KVMConstant.KVM_VM_SYNC_PATH) { HttpEntity<String> e ->
            def hostUuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)

            List<Tuple> states = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.uuid, VmInstanceVO_.state)
                    .in(VmInstanceVO_.state, [VmInstanceState.Running, VmInstanceState.Unknown, VmInstanceState.Paused])
                    .eq(VmInstanceVO_.hostUuid, hostUuid).listTuple()

            def rsp = new KVMAgentCommands.VmSyncResponse()
            rsp.states = [:]
            states.each {
                String vmUuid = it.get(0, String.class)
                VmInstanceState state = it.get(1, VmInstanceState.class)
                if (state == VmInstanceState.Unknown) {
                    // host reconnecting will set VMs to Unknown in DB
                    // the spec.simulator treat them as Running by default
                    rsp.states[(vmUuid)] = KVMConstant.KvmVmState.Running.toString()
                } else {
                    rsp.states[(vmUuid)] = KVMConstant.KvmVmState.fromVmInstanceState(state).toString()
                }
            }
            rsp.setVmInShutdowns(new ArrayList<String>())

            return rsp
        }

        handle(KVMConstant.KVM_VOLUME_SYNC_PATH) {
            return new VolumeSyncRsp()
        }

        handle(KVMConstant.KVM_ATTACH_VOLUME) {
            return new KVMAgentCommands.AttachDataVolumeResponse()
        }

        handle(KVMConstant.KVM_CHECK_PHYSICAL_NETWORK_INTERFACE_PATH) {
            return new KVMAgentCommands.CheckPhysicalNetworkInterfaceResponse()
        }

        handle(KVMConstant.KVM_ADD_INTERFACE_TO_BRIDGE_PATH) {
            return new KVMAgentCommands.AddInterfaceToBridgeResponse()
        }

        handle(KVMConstant.KVM_REALIZE_L2NOVLAN_NETWORK_PATH) {
            return new KVMAgentCommands.CreateBridgeResponse()
        }

        handle(KVMConstant.KVM_UPDATE_L2VLAN_NETWORK_PATH) {
            return new KVMAgentCommands.UpdateL2NetworkResponse()
        }
        handle(KVMConstant.KVM_UPDATE_L2VXLAN_NETWORK_PATH) {
            return new KVMAgentCommands.UpdateL2NetworkResponse()
        }

        handle(KVMConstant.KVM_MIGRATE_VM_PATH) {
            return new KVMAgentCommands.MigrateVmResponse()
        }

        handle(KVMConstant.KVM_GET_CPU_XML_PATH) {
            def rsp = new KVMAgentCommands.VmGetCpuXmlResponse()
            rsp.setCpuXml("<cpu mode='custom' match='exact'>\n" +
                    "  <model fallback='forbid'>Broadwell-IBRS</model>\n" +
                    "  <vendor>Intel</vendor>\n" +
                    "  <feature policy='require' name='vme'/>\n" +
                    "  <feature policy='require' name='ss'/>\n" +
                    "  <feature policy='require' name='ht'/>\n" +
                    "</cpu>")
            return rsp
        }

        handle(KVMConstant.KVM_COMPARE_CPU_FUNCTION_PATH) {
            return new KVMAgentCommands.VmCompareCpuFunctionResponse()
        }

        handle(KVMConstant.KVM_CHECK_L2NOVLAN_NETWORK_PATH) {
            return new KVMAgentCommands.CheckBridgeResponse()
        }

        handle(KVMConstant.KVM_CHECK_L2VLAN_NETWORK_PATH) {
            return new KVMAgentCommands.CheckVlanBridgeResponse()
        }

        handle(KVMConstant.KVM_CHECK_OVSDPDK_NETWORK_PATH) {
            return new KVMAgentCommands.CheckBridgeCmd()
        }

        handle(KVMConstant.KVM_REALIZE_L2VLAN_NETWORK_PATH) {
            return new KVMAgentCommands.CreateVlanBridgeResponse()
        }

        handle(KVMConstant.KVM_REALIZE_OVSDPDK_NETWORK_PATH) {
            return new KVMAgentCommands.CreateBridgeCmd()
        }

        handle(KVMConstant.KVM_SYNC_VM_DEVICEINFO_PATH) { HttpEntity<String> e ->
            SyncVmDeviceInfoCmd cmd = JSONObjectUtil.toObject(e.body, SyncVmDeviceInfoCmd.class)
            def rsp = new SyncVmDeviceInfoResponse()

            rsp.virtualizerInfo = new VirtualizerInfoTO()
            rsp.virtualizerInfo.uuid = cmd.vmInstanceUuid
            rsp.virtualizerInfo.virtualizer = "qemu-kvm"
            rsp.virtualizerInfo.version = "4.2.0-632.g6a6222b.el7"

            return rsp
         }

        handle(KVMConstant.KVM_START_VM_PATH) { HttpEntity<String> e ->
            StartVmCmd cmd = JSONObjectUtil.toObject(e.body, StartVmCmd.class)
            StartVmResponse  rsp = new StartVmResponse()
            rsp.virtualDeviceInfoList = []
            List<VolumeTO> pciInfo = new ArrayList<VolumeTO>()
            pciInfo.add(cmd.rootVolume)
            pciInfo.addAll(cmd.dataVolumes)

            Integer counter = 0
            pciInfo.each { to ->
                VirtualDeviceInfo info = new VirtualDeviceInfo()
                info.resourceUuid = to.volumeUuid
                info.deviceAddress = new DeviceAddress()
                info.deviceAddress.domain = "0000"
                info.deviceAddress.bus = "00"
                info.deviceAddress.slot = Integer.toHexString(counter)
                info.deviceAddress.function = "0"

                counter++

                rsp.virtualDeviceInfoList.add(info)
            }

            rsp.virtualizerInfo = new VirtualizerInfoTO()
            rsp.virtualizerInfo.uuid = cmd.vmInstanceUuid
            rsp.virtualizerInfo.virtualizer = "qemu-kvm"
            rsp.virtualizerInfo.version = "4.2.0-632.g6a6222b.el7"
            rsp.edkRpm = "edk2-ovmf-20220126gitbb1bba3d77-3.el8.noarch"

            return rsp
        }

        handle(KVMConstant.KVM_STOP_VM_PATH) {
            return new KVMAgentCommands.StopVmResponse()
        }

        handle(KVMConstant.KVM_PAUSE_VM_PATH) {
            return new KVMAgentCommands.PauseVmResponse()
        }

        handle(KVMConstant.KVM_RESUME_VM_PATH) {
            return new KVMAgentCommands.ResumeVmResponse()
        }

        handle(KVMConstant.KVM_REBOOT_VM_PATH) {
            return new KVMAgentCommands.RebootVmResponse()
        }

        handle(KVMConstant.KVM_DESTROY_VM_PATH) {
            return new KVMAgentCommands.DestroyVmResponse()
        }

        handle(KVMConstant.KVM_GET_VNC_PORT_PATH) {
            def rsp = new KVMAgentCommands.GetVncPortResponse()
            rsp.port = 5900
            return rsp
        }

        handle(KVMConstant.KVM_LOGOUT_ISCSI_PATH) {
            return new KVMAgentCommands.LogoutIscsiTargetRsp()
        }

        handle(KVMConstant.KVM_LOGIN_ISCSI_PATH) {
            return new KVMAgentCommands.LoginIscsiTargetRsp()
        }

        handle(KVMConstant.KVM_VM_ONLINE_INCREASE_CPU) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.IncreaseCpuCmd.class)
            def rsp = new KVMAgentCommands.IncreaseCpuResponse()
            rsp.setCpuNum(cmd.getCpuNum())
            return rsp
        }

        handle(KVMConstant.KVM_VM_ONLINE_INCREASE_MEMORY) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.IncreaseMemoryCmd.class)
            def rsp = new KVMAgentCommands.IncreaseMemoryResponse()
            rsp.setMemorySize(cmd.getMemorySize())
            return rsp
        }

        handle(SelfFencerKvmBackend.SETUP_SELF_FENCER_PATH) { HttpEntity<String> e ->
            def rsp = new SelfFencerKvmBackend.AgentRsp()
            rsp.success = true
            return rsp
        }

        handle(HaKvmHostSiblingChecker.SCAN_HOST_PATH) { HttpEntity<String> e ->
            def rsp = new HaKvmHostSiblingChecker.ScanRsp()
            rsp.result = HaKvmHostSiblingChecker.RET_SUCCESS
            return rsp
        }

        handle(HaKvmHostSiblingChecker.SYNC_HA_VM_LIST_PATH) {
            return new HaKvmHostSiblingChecker.AgentRsp()
        }

        handle(HaSanlockHostChecker.SANLOCK_SCAN_HOST_PATH) { HttpEntity<String> e ->
            def rsp = new HaSanlockHostChecker.ScanRsp()
            rsp.result = Collections.emptyMap()
            return rsp
        }

        handle(ShareBlockHostHeartbeatChecker.SANLOCK_SCAN_HOST_PATH) { HttpEntity<String> e ->
            def rsp = new ShareBlockHostHeartbeatChecker.ShareBlockCheckHostHeartbeatRsp()
            rsp.result = Collections.emptyMap()
            return rsp
        }

        handle(AbstractFileSystemHostHeartbeatChecker.FILESYSTEM_CHECK_VMSTATE_PATH) { HttpEntity<String> e ->
            def rsp = new AbstractFileSystemHostHeartbeatChecker.CheckFileSystemVmStateRsp()
            rsp.result = Collections.emptyMap()
            return rsp
        }

        handle(MevocoKVMConstant.KVM_VM_CHANGE_PASSWORD_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, MevocoKVMAgentCommands.ChangeVmPasswordCmd.class)
            def rsp = new MevocoKVMAgentCommands.ChangeVmPasswordResponse()
            rsp.success = true
            rsp.vmAccountPerference = cmd.accountPerference
            return rsp
        }

        handle(GuestToolsConstant.ATTACH_GUEST_TOOLS_ISO_TO_VM_PATH) { HttpEntity<String> e ->
            def rsp = new GuestToolsKvmCommands.AttachGuestToolsIsoToVmRsp()
            rsp.success = true
            return rsp
        }

        handle(GuestToolsConstant.DETACH_GUEST_TOOLS_ISO_FROM_VM_PATH) { HttpEntity<String> e ->
            def rsp = new GuestToolsKvmCommands.DetachGuestToolsIsoFromVmRsp()
            rsp.success = true
            return rsp
        }

        handle(MevocoKVMConstant.KVM_VM_CHECK_VOLUME_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, MevocoKVMAgentCommands.CheckVmVolumesCmd.class)
            List<String> usingVolsUuids = Q.New(VolumeVO.class).select(VolumeVO_.uuid)
                    .in(VolumeVO_.type, Arrays.asList(VolumeType.Root, VolumeType.Data))
                    .eq(VolumeVO_.vmInstanceUuid, cmd.uuid)
                    .listValues()
            assert cmd.volumes.size() == usingVolsUuids.size()
            assert cmd.volumes.stream().allMatch({vol -> usingVolsUuids.contains(vol.volumeUuid)})
            return new KVMAgentCommands.AgentResponse()
        }

        handle(MevocoKVMConstant.CHECK_MOUNT_DOMAIN) {
            def rsp = new MevocoKVMAgentCommands.CheckMountDomainResponse()
            rsp.active = true
            return rsp
        }

        handle(MevocoKVMConstant.RESIZE_VOLUME) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, MevocoKVMAgentCommands.ResizeVolumeCmd.class)
            def rsp = new MevocoKVMAgentCommands.ResizeVolumeResponse()
            rsp.success = true
            SQL.New(VolumeVO.class).set(VolumeVO_.size, cmd.getSize()).eq(VolumeVO_.installPath, cmd.getInstallPath())
                    .eq(VolumeVO_.vmInstanceUuid, cmd.getVmUuid()).update()

            return rsp
        }

        handle(KVMConstant.KVM_UPDATE_HOST_OS_PATH) { HttpEntity<String> e ->
            return new KVMAgentCommands.UpdateHostOSRsp()
        }

        handle(KVMConstant.KVM_HOST_UPDATE_DEPENDENCY_PATH) { HttpEntity<String> e ->
            return new KVMAgentCommands.UpdateDependencyRsp()
        }

        handle(KVMConstant.HOST_SHUTDOWN) { HttpEntity<String> e ->
            return new KVMAgentCommands.UpdateHostOSRsp()
        }

        handle(KVMConstant.HOST_REBOOT) { HttpEntity<String> e ->
            return new KVMAgentCommands.UpdateDependencyRsp()
        }

        handle(KVMConstant.HOST_UPDATE_SPICE_CHANNEL_CONFIG_PATH) { HttpEntity<String> e ->
            return new KVMAgentCommands.UpdateSpiceChannelConfigResponse()
        }

        handle(KvmHostScrape.COLLECTD_PATH) { HttpEntity<String> e ->
            KvmHostScrape.StartCollectdExporterRsp rsp = new KvmHostScrape.StartCollectdExporterRsp()
            return rsp
        }

        handle(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { HttpEntity<String> e ->
            def rsp = new VxlanKvmAgentCommands.CheckVxlanCidrResponse()
            def cmd = JSONObjectUtil.toObject(e.body, VxlanKvmAgentCommands.CheckVxlanCidrCmd.class)
            def hostUuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)

            rsp.success = true
            
            if (cmd.vtepip != null) {
                rsp.vtepIp = cmd.vtepip
            } else {
                rsp.vtepIp = Q.New(HostVO.class).select(HostVO_.managementIp).eq(HostVO_.uuid, hostUuid).findValue()
            }

            return rsp
        }

        handle(VxlanNetworkPoolConstant.VXLAN_KVM_REALIZE_L2VXLAN_NETWORK_PATH) {
            return new VxlanKvmAgentCommands.CreateVxlanBridgeResponse()
        }

        handle(VxlanNetworkPoolConstant.VXLAN_KVM_REALIZE_L2VXLAN_NETWORKS_PATH) {
            return new VxlanKvmAgentCommands.CreateVxlanBridgesCmd()
        }

        handle(VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORK_PATH) {
            return new VxlanKvmAgentCommands.PopulateVxlanFdbResponse()
        }

        handle(VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORKS_PATH) {
            return new VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd()
        }

        handle(MevocoKVMConstant.GET_HOST_PHYSICAL_CPU_FACTS) {
            MevocoKVMAgentCommands.GetHostPhysicalCpuFactsResponse rsp = new MevocoKVMAgentCommands.GetHostPhysicalCpuFactsResponse()
            return rsp
        }

        handle(MevocoKVMConstant.GET_HOST_PHYSICAL_MEMORY_FACTS) {
            MevocoKVMAgentCommands.GetHostPhysicalMemoryFactsResponse rsp = new MevocoKVMAgentCommands.GetHostPhysicalMemoryFactsResponse()
            return rsp
        }

        handle(MevocoKVMConstant.LOCATE_HOST_NETWORK_INTERFACE) {
            return new KVMAgentCommands.AgentResponse()
        }

        handle(MevocoKVMConstant.GET_HOST_NETWORK_FACTS) {
            def reply = new MevocoKVMAgentCommands.GetHostNetworkBondingResponse()

            def inv1 = new HostNetworkInterfaceInventory()
            inv1.interfaceName = "enp101s0f0"
            inv1.speed = 10000L
            inv1.carrierActive = true
            inv1.mac = "ac:1f:6b:93:6c:8c"
            inv1.pciDeviceAddress = "0e:00.0"
            inv1.interfaceType = NetworkInterfaceType.bondingSlave.toString()

            def inv2 = new HostNetworkInterfaceInventory()
            inv2.interfaceName = "enp101s0f1"
            inv2.speed = 10000L
            inv2.carrierActive = true
            inv2.mac = "ac:1f:6b:93:6c:8d"
            inv2.pciDeviceAddress = "0e:00.1"
            inv2.interfaceType = NetworkInterfaceType.bondingSlave.toString()

            def inv3 = new HostNetworkInterfaceInventory()
            inv3.interfaceName = "vmnic0"
            inv3.speed = 10000L
            inv3.carrierActive = true
            inv3.mac = "ac:1f:6b:93:6c:66"
            inv3.pciDeviceAddress = "10:00.0"
            inv3.interfaceType = NetworkInterfaceType.bondingSlave.toString()

            def inv4 = new HostNetworkInterfaceInventory()
            inv4.interfaceName = "vmnic1"
            inv4.speed = 10000L
            inv4.carrierActive = true
            inv4.mac = "ac:1f:6b:93:6c:67"
            inv4.pciDeviceAddress = "10:00.1"
            inv4.interfaceType = NetworkInterfaceType.noMaster.toString()

            reply.nics = [inv1, inv2, inv3, inv4] as List<HostNetworkInterfaceInventory>

            def bond0 = new HostNetworkBondingInventory()
            bond0.bondingName = "bond0"
            bond0.type = HostNetworkBondingConstant.LINUX_BONDING_TYPE.toString()
            bond0.mode = "active-backup 1"
            bond0.slaves =  [inv1, inv2] as List<HostNetworkInterfaceInventory>

            HostNetworkBondingInventory bondDefault = new HostNetworkBondingInventory()
            bondDefault.bondingName = "vmbond0"
            bondDefault.type = HostNetworkBondingConstant.LINUX_BONDING_TYPE.toString()
            bondDefault.mode = "active-backup 1"
            bondDefault.xmitHashPolicy = "layer 3+4"
            bondDefault.slaves = [inv3] as List<HostNetworkInterfaceInventory>

            reply.bondings = [bond0, bondDefault] as List<HostNetworkBondingInventory>

            return reply
        }

        handle(MevocoKVMConstant.UPDATE_HOST_OVS_CPU_PINNING) {
            return new MevocoKVMAgentCommands.UpdateOvsCpuPinningResponse()
        }

        handle(MevocoKVMConstant.SET_BRIDGE_ROUTER_PORT) {
            def rsp = new MevocoKVMAgentCommands.BridgeRouterPortResponse()
            rsp.success = true
            return rsp
        }

        handle(StorageDeviceKvmCommands.ISCSI_LOGIN_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, StorageDeviceKvmCommands.IscsiLoginCmd.class)
            IscsiLunStruct s1 = new IscsiLunStruct()
            s1.wwids = ["lvm-pv-uuid-bC0n7f-4w0O-6COA-31ee-KR34-jn2z-XPb8wj", "scsi-c455277466d4362662d56664130" + cmd.iscsiServerIp + cmd.iscsiServerPort]
            s1.type = "disk"
            s1.wwn = cmd.iscsiServerIp + cmd.iscsiServerPort
            s1.model = "VIRTUAL-DISK"
            s1.vendor = "OPNFILER"
            s1.hctl = "4:0:0:0"
            s1.serial = "4f504e4649" + cmd.iscsiServerIp + cmd.iscsiServerPort + "41302d49393041"
            s1.path = "ip-" + cmd.iscsiServerIp + ":" + cmd.iscsiServerPort + "-iscsi-iqn.2018-02.io.zstack:tsn.00004-lun-0"
            s1.size = 44023414784l
            def rsp = new StorageDeviceKvmCommands.IscsiLoginRsp()
            IscsiTargetStruct ss1 = new IscsiTargetStruct()
            ss1.iqn = "iqn.2018-02.io.zstack:tsn.00004"
            ss1.iscsiLunStructList = [s1]
            rsp.iscsiTargetStructList = [ss1]
            def rets = simulator.sqlite.query("select * from ${BlockDevice.class.simpleName} where wwn = '${s1.wwn}'", BlockDevice.class)
            if (rets.isEmpty()) {
                simulator.sqlite.execute("insert into ${BlockDevice.class.simpleName} ('wwn','hctl','model','vendor','type','serial','size','wwids') VALUES ('${s1.wwn}', '${s1.hctl}', '${s1.model}', '${s1.vendor}','${s1.type}','${s1.serial}','${s1.size}','${s1.wwids}')")
            }
            return rsp
        }

        handle(StorageDeviceKvmCommands.ISCSI_LOGOUT_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, StorageDeviceKvmCommands.IscsiLogoutCmd.class)
            String wwn = cmd.iscsiServerIp + cmd.iscsiServerPort
            simulator.sqlite.execute("delete from ${BlockDevice.class.simpleName} where wwn = '${wwn}'")
            return new StorageDeviceKvmCommands.IscsiLogoutRsp()
        }

        handle(StorageDeviceKvmCommands.NVME_SCAN_PATH) {
            StorageDeviceKvmCommands.NvmeScanRsp rsp = new StorageDeviceKvmCommands.NvmeScanRsp()
            return rsp
        }

        handle(StorageDeviceKvmCommands.NVME_CONNECT_PATH) {
            StorageDeviceKvmCommands.NvmeServerConnectRsp rsp = new StorageDeviceKvmCommands.NvmeServerConnectRsp()
            return rsp
        }

        handle(StorageDeviceKvmCommands.NVME_DISCONNECT_PATH) {
            StorageDeviceKvmCommands.NvmeServerDisconnectRsp rsp = new StorageDeviceKvmCommands.NvmeServerDisconnectRsp()
            return rsp
        }

        handle(StorageDeviceKvmCommands.FC_SCAN_PATH) { HttpEntity<String> e ->
            def rsp = new StorageDeviceKvmCommands.FcScanRsp()
            String hostUuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)

            if (hostUuid == null || hostUuid.isEmpty()) {
                List<String> hosts = Q.New(HostVO.class).select(HostVO_.uuid).listValues()
                if (hosts.isEmpty()) {
                    return rsp
                }
                hosts.forEach({ host ->
                    def noHostRets = simulator.sqlite.query("select * from ${BlockDevice.class.simpleName} where hostIp = '$hostIp'", BlockDevice.class)

                    if (noHostRets.size() < 1) {
                        return
                    }

                    for (BlockDevice fcLun : noHostRets) {
                        FiberChannelLunStruct fcLunStruct = new FiberChannelLunStruct()
                        fcLunStruct.wwids = [fcLun.wwids]
                        fcLunStruct.multipathDeviceUuid = fcLun.multipathDeviceUuid
                        fcLunStruct.type = fcLun.type
                        fcLunStruct.wwn = fcLun.wwn
                        fcLunStruct.model = fcLun.model
                        fcLunStruct.vendor = fcLun.vendor
                        fcLunStruct.serial = fcLun.serial
                        fcLunStruct.path = fcLun.hctl
                        fcLunStruct.size = fcLun.size
                        fcLunStruct.storageWwnn = fcLun.storageWwnn
                        rsp.fiberChannelLunStructs.add(fcLunStruct)
                    }
                })
                return rsp
            }

            String hostIp = Q.New(HostVO.class).select(HostVO_.managementIp).eq(HostVO_.uuid, hostUuid).findValue()
            def rets = simulator.sqlite.query("select * from ${BlockDevice.class.simpleName} where hostIp = '$hostIp'", BlockDevice.class)

            if (rets.size() < 1) {
                return rsp
            }

            for (BlockDevice fcLun : rets) {
                FiberChannelLunStruct fcLunStruct = new FiberChannelLunStruct()
                fcLunStruct.wwids = [fcLun.wwids]
                fcLunStruct.multipathDeviceUuid = fcLun.multipathDeviceUuid
                fcLunStruct.type = fcLun.type
                fcLunStruct.wwn = fcLun.wwn
                fcLunStruct.model = fcLun.model
                fcLunStruct.vendor = fcLun.vendor
                fcLunStruct.serial = fcLun.serial
                fcLunStruct.path = fcLun.hctl
                fcLunStruct.size = fcLun.size
                fcLunStruct.storageWwnn = fcLun.storageWwnn
                rsp.fiberChannelLunStructs.add(fcLunStruct)
            }

            return rsp
        }

        handle(StorageDeviceKvmCommands.RAID_SCAN_PATH) {
            RaidPhysicalDriveStruct s110 = new RaidPhysicalDriveStruct()
            s110.setRaidLevel("raid0")
            s110.setRaidControllerProductName("LSI 2208 MegaRAID")
            s110.setRaidControllerSasAddreess("500304801f948100")
            s110.setRaidControllerNumber(0)
            s110.setDeviceId(0)
            s110.setEnclosureDeviceId(252)
            s110.setSlotNumber(0)
            s110.setDiskGroup(0)
            s110.setWwn("50014ee059f2f0c0")
            s110.setSerialNumber("WMC6M0K8519Y")
            s110.setDeviceModel("HGST HUS722T1TALA604")
            s110.setSize(1000204886016L)
            s110.setDriveState("Online, Spun Up")
            s110.setDriveType("SATA")
            s110.setMediaType("HDD")
            s110.setRotationRate(7200)
            def rsp = new StorageDeviceKvmCommands.RaidScanRsp()
            rsp.raidPhysicalDriveStructs = [s110]
            return rsp
        }

        handle(StorageDeviceKvmCommands.RAID_SMART_PATH) {
            def rsp = new StorageDeviceKvmCommands.RaidPhysicalDriveSmartRsp()
            org.zstack.storage.device.localRaid.SmartDataStruct s0 = new org.zstack.storage.device.localRaid.SmartDataStruct()
            s0.setId(1)
            s0.setAttributeName("Raw_Read_Error_Rate")
            s0.setFlag("0x002f")
            s0.setValue(200)
            s0.setWorst(200)
            s0.setThresh(051)
            s0.setType("Pre-fail")
            s0.setUpdated("Always")
            s0.setWhenFailed("-")
            s0.setRawValue(0L)
            rsp.smartDataStructs = Arrays.asList(s0)
            return rsp
        }

        handle(StorageDeviceKvmCommands.RAID_LOCATE_PATH) {
            return new KVMAgentCommands.AgentResponse()
        }

        handle(StorageDeviceKvmCommands.RAID_SELF_TEST_PATH) {
            def rsp = new StorageDeviceKvmCommands.RaidPhysicalDriveSmartTestRsp()
            rsp.result = "Completed without error"
        }

        handle(StorageDeviceKvmCommands.MULTIPATH_ENABLE_PATH) {
            return new StorageDeviceKvmCommands.MultipathEnableRsp()
        }

        handle(StorageDeviceKvmCommands.MULTIPATH_DISABLE_PATH) {
            return new KVMAgentCommands.AgentResponse()
        }

        handle(StorageDeviceKvmCommands.ATTACH_SCSI_LUN_PATH) {
            return new StorageDeviceKvmCommands.AttachScsiLunToVmRsp()
        }

        handle(StorageDeviceKvmCommands.GET_MULTIPATH_TOPOLOGY_PATH) {
            return new StorageDeviceKvmCommands.GetMultipathTopologyRsp()
        }


        handle(MevocoKVMConstant.ENABLE_HUGEPAGE) {
            return new MevocoKVMAgentCommands.EnableHugePageResponse()
        }

        handle(MevocoKVMConstant.DISABLE_HUGEPAGE) {
            return new MevocoKVMAgentCommands.DisableHugePageResponse()
        }

        handle(MevocoKVMConstant.IDENTIFY_HOST) {
            return new KVMAgentCommands.AgentResponse()
        }

        handle(MevocoKVMConstant.ENABLE_ZERO_COPY) {
            return new MevocoKVMAgentCommands.EnableHugePageResponse()
        }

        handle(MevocoKVMConstant.DISABLE_ZERO_COPY) {
            return new MevocoKVMAgentCommands.DisableHugePageResponse()
        }

        handle(KVMConstant.GET_DEV_CAPACITY) {
            KVMAgentCommands.GetDevCapacityResponse rsp = new KVMAgentCommands.GetDevCapacityResponse()
            rsp.totalSize = SizeUnit.GIGABYTE.toByte(100)
            rsp.availableSize = SizeUnit.GIGABYTE.toByte(80)
            return rsp
        }

        handle(KVMConstant.GET_VM_DEVICE_ADDRESS_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.GetVmDeviceAddressCmd.class)
            def rsp = new KVMAgentCommands.GetVmDeviceAddressRsp()
            if (cmd.deviceTOs.keySet().contains(VolumeVO.class.simpleName)) {
                rsp.addresses = ["VolumeVO": []]
                for (Object o : cmd.deviceTOs.get(VolumeVO.class.simpleName)) {
                    VolumeTO to = JSONObjectUtil.rehashObject(o, VolumeTO.class)
                    rsp.addresses[VolumeVO.class.simpleName].add(new KVMAgentCommands.VmDeviceAddressTO(
                            addressType: "pci",
                            address: String.format("0000:%02d:00:0", to.deviceId),
                            deviceType: "disk",
                            uuid: to.volumeUuid
                    ))
                }
            }

            return rsp
        }

        handle(KVMConstant.GET_VIRTUALIZER_INFO_PATH) { HttpEntity<String> e ->
            def rsp = new GetVirtualizerInfoRsp()
            rsp.hostInfo = new VirtualizerInfoTO()
            rsp.hostInfo.version = "4.2.0-627.g36ee592.el7"
            rsp.hostInfo.virtualizer = "qemu-kvm"
            String hostUuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            rsp.hostInfo.uuid = hostUuid

            def cmd = JSONObjectUtil.toObject(e.body, GetVirtualizerInfoCmd.class)
            rsp.vmInfoList = cmd.vmUuids.collect { vmUuid ->
                def to = new VirtualizerInfoTO()
                to.uuid = vmUuid
                to.version = "4.2.0-627.g36ee592.el7"
                to.virtualizer = "qemu-kvm"
                return to
            }

            return rsp
        }

        handle(KVMConstant.KVM_HOST_FACT_PATH) { HttpEntity<String> e ->
            def rsp = new HostFactResponse()

            rsp.osDistribution = "zstack"
            rsp.osRelease = "kvmSimulator"
            rsp.osVersion = "0.1"
            rsp.qemuImgVersion = "2.0.0"
            rsp.libvirtVersion = "1.2.9"
            rsp.cpuModelName = "Broadwell"
            rsp.cpuProcessorNum = 10
            rsp.cpuThreadsPerCore = 2
            rsp.cpuCoresPerSocket = 5
            rsp.cpuSockets = 1
            rsp.cpuGHz = "2.10"
            rsp.hostCpuModelName = "Broadwell @ 2.10GHz"
            rsp.ipmiAddress = "0.0.0.0"
            rsp.eptFlag = "ept"
            rsp.libvirtCapabilities = ["incrementaldrivemirror", "blockcopynetworktarget"]
            rsp.powerSupplyModelName = ""
            rsp.powerSupplyManufacturer = ""
            rsp.hvmCpuFlag = ""
            rsp.cpuCache = "64.0,4096.0,16384.0"
            rsp.iscsiInitiatorName = "iqn.2015-01.io.helix:a6e4508d2378"
            rsp.nqn = "nqn.2014-08.org.nvmexpress:uuid:748d0363-8366-44db-803b-146effb96988"

            rsp.virtualizerInfo = new VirtualizerInfoTO()
            rsp.virtualizerInfo.version = "4.2.0-627.g36ee592.el7"
            rsp.virtualizerInfo.virtualizer = "qemu-kvm"
            return rsp
        }

        handle(MevocoKVMConstant.ADD_BRIDGE_FDB_ENTRY_PATH) {
            return new MevocoKVMAgentCommands.AddBridgeFdbEntryRsp()
        }

        handle(MevocoKVMConstant.SET_VM_EMULATOR_PINNING) {
            return new KVMAgentCommands.AgentResponse()
        }

        handle(MevocoKVMConstant.SET_HOST_PHYSICAL_NIC_MONITOR) {
            return new MevocoKVMAgentCommands.SetHostPhysicalNicMonitorRsp()
        }

        handle(MevocoKVMConstant.DEL_BRIDGE_FDB_ENTRY_PATH) {
            return new MevocoKVMAgentCommands.AddBridgeFdbEntryRsp()
        }

        handle(PciDeviceKvmBackend.GET_PCI_DEVICES) { HttpEntity<String> e ->
            PciDeviceKvmBackend.GetPciDevicesRsp rsp = new PciDeviceKvmBackend.GetPciDevicesRsp()
            String hostUuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            String hostIp = Q.New(HostVO.class).select(HostVO_.managementIp).eq(HostVO_.uuid, hostUuid).findValue()
            def rets = simulator.sqlite.query("select * from ${PciDevice.class.simpleName} where hostIp = '$hostIp'", PciDevice.class)
            
            List<PciDeviceTO> pdList = []
            if (rets.size() < 1) {
                rsp.setPciDevicesInfo(pdList)
                rsp.hostIommuStatus = true
                rsp.setSuccess(true)
                return rsp
            }

            for (PciDevice pd : rets) {
                List<Map<String, String>> typeIdsList = []
                for (String specId : pd.deviceSpecTypeIds.isEmpty() ? " " : pd.deviceSpecTypeIds.split(";")) {
                    List<MdevDeviceSpec> idRets = simulator.sqlite.query("select * from ${MdevDeviceSpec.class.simpleName} where id = $specId", MdevDeviceSpec.class)
                    idRets.each { idRet ->
                        Map<String, String> mdevSpecification = new HashMap<>()
                        mdevSpecification.put("Name", (idRet as MdevDeviceSpec).name)
                        mdevSpecification.put("Vendor", (idRet as MdevDeviceSpec).vendor)
                        mdevSpecification.put("Max Instances", (idRet as MdevDeviceSpec).maxInstances)
                        mdevSpecification.put("Device ID", (idRet as MdevDeviceSpec).deviceID)
                        mdevSpecification.put("Sub System ID", (idRet as MdevDeviceSpec).subSystemID)
                        mdevSpecification.put("Display Heads", (idRet as MdevDeviceSpec).displayHeads)
                        mdevSpecification.put("Maximum X Resolution", (idRet as MdevDeviceSpec).maxResolutionX)
                        mdevSpecification.put("Maximum Y Resolution", (idRet as MdevDeviceSpec).maxResolutionY)
                        mdevSpecification.put("FB Memory", (idRet as MdevDeviceSpec).FBMemory)
                        mdevSpecification.put("Frame Rate Limit", (idRet as MdevDeviceSpec).frameRateLimit)
                        mdevSpecification.put("GRID License", (idRet as MdevDeviceSpec).GRIDLicense)
                        typeIdsList.add(mdevSpecification)
                    }
                }

                PciDeviceTO struct = new PciDeviceTO()
                struct.name = pd.name
                struct.description = pd.description
                struct.vendorId = pd.vendorId
                struct.deviceId = pd.deviceId
                struct.hostUuid = hostUuid
                struct.subdeviceId = pd.subdeviceId
                struct.subvendorId = pd.subvendorId
                struct.parentAddress = pd.parentAddress
                struct.pciDeviceAddress = pd.pciDeviceAddress
                struct.type = pd.type
                struct.virtStatus = pd.virtStatus
                struct.iommuGroup = pd.iommuGroup
                struct.maxPartNum = pd.maxPartNum
                struct.ramSize = pd.ramSize
                struct.setMdevSpecifications(typeIdsList)

                pdList.add(struct)
            }

            rsp.setPciDevicesInfo(pdList)
            rsp.hostIommuStatus = true
            rsp.setSuccess(true)
            return rsp
        }
        handle(MttyDeviceKvmBackend.GET_MTTY_DEVICES) { HttpEntity<String> e ->
            MttyDeviceKvmBackend.GetMttyDevicesRsp rsp = new MttyDeviceKvmBackend.GetMttyDevicesRsp()
            String hostUuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            String hostIp = Q.New(HostVO.class).select(HostVO_.managementIp).eq(HostVO_.uuid, hostUuid).findValue()
            def rets = simulator.sqlite.query("select * from ${MttyDevice.class.simpleName} where hostIp = '$hostIp'", MttyDevice.class)

            List<MttyDeviceTO> pdList = []
            if (rets.size() < 1) {
                rsp.setMttyDeviceInfo(pdList)
                rsp.setSuccess(true)
                return rsp
            }
            if (rets.size() > 1) {
                rsp.setMttyDeviceInfo(pdList)
                rsp.setSuccess(false)
                return rsp
            }

            MttyDeviceTO struct = new MttyDeviceTO()
            struct.name = pd.name
            struct.description = pd.description
            struct.hostUuid = hostUuid
            struct.type = pd.type
            struct.virtStatus = pd.virtStatus

            rsp.setMttyDeviceInfo(struct)
            rsp.setSuccess(true)
            return rsp
        }
        handle(UsbDeviceKvmBackend.GET_USB_DEVICES_PATH) { HttpEntity<String> e ->
            UsbDeviceKvmBackend.GetUsbDevicesRsp rsp = new UsbDeviceKvmBackend.GetUsbDevicesRsp()
            String hostUuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            String hostIp = Q.New(HostVO.class).select(HostVO_.managementIp).eq(HostVO_.uuid, hostUuid).findValue()
            def rets = simulator.sqlite.query("select * from ${UsbDevice.class.simpleName} where hostIp = '$hostIp'", UsbDevice.class)

            List<UsbDeviceTO> usbTos = new ArrayList<>()
            rsp.usbDevicesInfo = usbTos
            for (UsbDevice ud : rets) {
                String[] udInfo = [
                        ud.busNum,
                        ud.devNum,
                        ud.idVendor,
                        ud.idProduct,
                        ud.iManufacturer,
                        ud.iProduct,
                        ud.iSerial,
                        ud.usbVersion
                ]
                UsbDeviceTO usbTo = new UsbDeviceTO(udInfo)
                usbTos.add(usbTo)
            }
            rsp.setSuccess(true)
            return rsp
        }

        handle(PciDeviceKvmBackend.CREATE_PCI_DEVICE_ROM_FILE) {
            return new PciDeviceKvmBackend.CreatePciDeviceRomFileRsp()
        }

        handle(PciDeviceKvmBackend.ATTACH_PCI_DEVICE_TO_HOST) {
            return new PciDeviceKvmBackend.AttachPciDeviceToHostRsp()
        }

        handle(PciDeviceKvmBackend.DETACH_PCI_DEVICE_FROM_HOST) {
            return new PciDeviceKvmBackend.DetachPciDeviceFromHostRsp()
        }

        handle(PciDeviceKvmBackend.GENERATE_SRIOV_PCI_DEVICES) { HttpEntity<String> e ->
            def rets
            def cmd = JSONObjectUtil.toObject(e.body, PciDeviceKvmBackend.GenerateSriovPciDevicesCommand.class)
            def num = cmd.getVirtPartNum()
            PciDevice pciRet = simulator.sqlite.find("select * from ${PciDevice.class.simpleName} where pciDeviceAddress = '${cmd.getPciDeviceAddress()}' and type = '${cmd.getPciDeviceType()}'", PciDevice.class)

            //AMD will generate the other AMD which comes from the same host
            if (pciRet.type.equals(PciDeviceType.GPU_Video_Controller.name()) || pciRet.type.equals(PciDeviceType.GPU_3D_Controller.name())) {
                def AMDRets = simulator.sqlite.query("select * from ${PciDevice.class.simpleName} where hostIp = '${pciRet.hostIp}' and type = '${pciRet.type}' and virtStatus ='SRIOV_VIRTUALIZABLE'", PciDevice.class)
                rets = AMDRets
            } else {
                rets = pciRet
            }

            for (PciDevice ret : (rets as List<PciDevice>)) {
                for (def i = 0; i < num; i++) {
                    PciDevice maxId = simulator.sqlite.find("select * from PciDevice p order by cast(p.id as int) desc limit 1", PciDevice.class)
                    simulator.sqlite.execute("insert into ${PciDevice.class.simpleName} " +
                            "(name,description,hostIp,vendorId,deviceId,subvendorId,subdeviceId,pciDeviceAddress,parentAddress,iommuGroup,type,virtStatus,maxPartNum,ramSize,deviceSpecTypeIds,id)" +
                            " VALUES ('vf-${ret.name}-$i', 'virtual function for ${ret.pciDeviceAddress}', '${ret.hostIp}', '${ret.vendorId}'," +
                            "'${ret.deviceId}-$i','${ret.subdeviceId}','${ret.subdeviceId}','${ret.pciDeviceAddress}$i','${ret.pciDeviceAddress}'," +
                            "'${ret.iommuGroup}$i','${ret.type}','SRIOV_VIRTUAL','0','${ret.ramSize}','0','${maxId.id.toLong() + 1}')")
                    simulator.sqlite.execute("update PciDevice set virtStatus = 'SRIOV_VIRTUALIZED' where id = ${ret.id} and virtStatus ='SRIOV_VIRTUALIZABLE';")
                }
            }

            return new PciDeviceKvmBackend.GenerateSriovPciDevicesRsp()
        }

        handle(PciDeviceKvmBackend.UNGENERATE_SRIOV_PCI_DEVICES) { HttpEntity<String> e ->
            def rets
            def cmd = JSONObjectUtil.toObject(e.body, PciDeviceKvmBackend.UngenerateSriovPciDevicesCommand)
            PciDevice pciRet = simulator.sqlite.find("select * from ${PciDevice.class.simpleName} where pciDeviceAddress = '${cmd.getPciDeviceAddress()}' and type = '${cmd.getPciDeviceType()}'", PciDevice.class)

            //AMD will ungenerate the other AMD which comes from the same host
            if (pciRet.getType().equals(PciDeviceType.GPU_Video_Controller.name()) || pciRet.getType().equals(PciDeviceType.GPU_3D_Controller.name())) {
                def AMDRets = simulator.sqlite.query("select * from ${PciDevice.class.simpleName} where hostIp = '${pciRet.hostIp}' and type = '${pciRet.type}' and virtStatus ='SRIOV_VIRTUALIZED'", PciDevice.class)
                rets = AMDRets
            } else {
                rets = pciRet
            }

            for (PciDevice ret : (rets as List<PciDevice>)) {
                simulator.sqlite.execute("update PciDevice set virtStatus = 'SRIOV_VIRTUALIZABLE' where id = ${(ret as PciDevice).id} and virtStatus ='SRIOV_VIRTUALIZED';")
                simulator.sqlite.execute("delete from ${PciDevice.class.simpleName} where parentAddress = '${ret.pciDeviceAddress}' and virtStatus = 'SRIOV_VIRTUAL'")
            }
            return new PciDeviceKvmBackend.UngenerateSriovPciDevicesRsp()
        }

        handle(PciDeviceKvmBackend.GENERATE_VFIO_MDEV_DEVICES) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, PciDeviceKvmBackend.GenerateVfioMdevDevicesCommand)
            List<PciDevice> rets = simulator.sqlite.query("select * from ${PciDevice.class.simpleName} where pciDeviceAddress = '${cmd.getPciDeviceAddress()}' and virtStatus ='VFIO_MDEV_VIRTUALIZABLE'", PciDevice.class)
            for(PciDevice ret : rets) {
                simulator.sqlite.execute("update PciDevice set virtStatus ='VFIO_MDEV_VIRTUALIZED' where id = ${(ret as PciDevice).id} and virtStatus ='VFIO_MDEV_VIRTUALIZABLE'")
            }

            def rsp = new PciDeviceKvmBackend.GenerateVfioMdevDevicesRsp()
            rsp.setMdevUuids([Platform.getUuid(), Platform.getUuid(), Platform.getUuid(), Platform.getUuid()])
            rsp.setSuccess(true)
            return rsp
        }

        handle(PciDeviceKvmBackend.UNGENERATE_VFIO_MDEV_DEVICES) {HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, PciDeviceKvmBackend.UngenerateVfioMdevDevicesCommand)
            List<PciDevice> rets = simulator.sqlite.query("select * from ${PciDevice.class.simpleName} where pciDeviceAddress = '${cmd.getPciDeviceAddress()}' and virtStatus ='VFIO_MDEV_VIRTUALIZED'", PciDevice.class)
            for(PciDevice ret : rets) {
                simulator.sqlite.execute("update PciDevice set virtStatus = 'VFIO_MDEV_VIRTUALIZABLE' where id = ${(ret as PciDevice).id} and virtStatus ='VFIO_MDEV_VIRTUALIZED'")
            }
            return new PciDeviceKvmBackend.UngenerateVfioMdevDevicesRsp()
        }

        handle(MttyDeviceKvmBackend.GENERATE_SE_VFIO_MDEV_DEVICES) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, MttyDeviceKvmBackend.GenerateSeVfioMdevDevicesCommand)
            List<MttyDevice> rets = simulator.sqlite.query("select * from ${MttyDevice.class.simpleName} where uuid = '${cmd.getMttyDeviceUuid()}' and virtStatus ='VFIO_MDEV_VIRTUALIZABLE'", MttyDevice.class)
            for(MttyDevice ret : rets) {
                simulator.sqlite.execute("update MttyDevice set virtStatus ='VFIO_MDEV_VIRTUALIZED' where id = ${(ret as MttyDevice).id} and virtStatus ='VFIO_MDEV_VIRTUALIZABLE'")
            }

            def rsp = new MttyDeviceKvmBackend.GenerateSeVfioMdevDevicesRsp()
            rsp.setMdevUuids([Platform.getUuid(), Platform.getUuid(), Platform.getUuid(), Platform.getUuid()])
            rsp.setSuccess(true)
            return rsp
        }

        handle(MttyDeviceKvmBackend.UNGENERATE_SE_VFIO_MDEV_DEVICES) {HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, MttyDeviceKvmBackend.UngenerateSeVfioMdevDevicesCommand)
            List<MttyDevice> rets = simulator.sqlite.query("select * from ${MttyDevice.class.simpleName} where uuid = '${cmd.getMttyDeviceUuid()}' and virtStatus ='VFIO_MDEV_VIRTUALIZED'", MttyDevice.class)
            for(MttyDevice ret : rets) {
                simulator.sqlite.execute("update MttyDevice set virtStatus = 'VFIO_MDEV_VIRTUALIZABLE' where id = ${(ret as MttyDevice).id} and virtStatus ='VFIO_MDEV_VIRTUALIZED'")
            }
            return new MttyDeviceKvmBackend.UngenerateSeVfioMdevDevicesRsp()
        }

        handle(PciDeviceKvmBackend.HOT_PLUG_PCI_DEVICE) {
            return new PciDeviceKvmBackend.HotPlugPciDeviceRsp()
        }

        handle(PciDeviceKvmBackend.HOT_UNPLUG_PCI_DEVICE) {
            return new PciDeviceKvmBackend.HotUnplugPciDeviceRsp()
        }

        handle(MdevDeviceBase.HOT_PLUG_MDEV_DEVICE) {
            return new MdevDeviceBase.HotPlugMdevDeviceRsp()
        }

        handle(MdevDeviceBase.HOT_UNPLUG_MDEV_DEVICE) {
            return new MdevDeviceBase.HotUnplugMdevDeviceRsp()
        }

        handle(MdevDeviceBase.DELETE_VFIO_MDEV_DEVICE) {
            return new MdevDeviceBase.DeleteVfioMdevDeviceRsp()
        }

        handle(UsbDeviceKvmBackend.KVM_ATTACH_USB_DEVICE_PATH) {
            return new UsbDeviceKvmBackend.KvmAttachUsbDeviceRsp()
        }

        handle(UsbDeviceKvmBackend.KVM_DETACH_USB_DEVICE_PATH) {
            return new UsbDeviceKvmBackend.KvmDetachUsbDeviceRsp()
        }

        handle(UsbDeviceKvmBackend.CHECK_USB_REDIRECT_PORT) {
            UsbDeviceKvmBackend.CheckUsbRedirectPortRsp rsp = new UsbDeviceKvmBackend.CheckUsbRedirectPortRsp()
            rsp.uuids = []
            rsp.setSuccess(true)
            return rsp
        }

        handle(UsbDeviceKvmBackend.RELOAD_USB_REDIRECT_PATH) {
            return new UsbDeviceKvmBackend.ReloadRedirectUsbRsp()
        }

        handle(UsbDeviceKvmBackend.HOST_STOP_USB_REDIRECT_PATH) {
            return new UsbDeviceKvmBackend.StopUsbServerRsp()
        }

        handle(UsbDeviceKvmBackend.HOST_START_USB_REDIRECT_PATH) { HttpEntity<String> e ->
            UsbDeviceKvmBackend.StartUsbServerRsp rsp = new UsbDeviceKvmBackend.StartUsbServerRsp()
            rsp.port = 4100
            rsp.setSuccess(true)
            return rsp
        }

        handle(KVMConstant.KVM_DELETE_L2NOVLAN_NETWORK_PATH) {
            return new KVMAgentCommands.DeleteBridgeResponse()
        }

        handle(KVMConstant.KVM_DELETE_L2VLAN_NETWORK_PATH) {
            return new KVMAgentCommands.DeleteVlanBridgeResponse()
        }
        
        handle(VxlanNetworkPoolConstant.VXLAN_KVM_DELETE_L2VXLAN_NETWORK_PATH) {
            return new VxlanKvmAgentCommands.DeleteVxlanBridgeResponse()
        }

        handle(KVMConstant.KVM_DELETE_OVSDPDK_NETWORK_PATH) {
            return new KVMAgentCommands.DeleteBridgeResponse()
        }

        handle(KVMConstant.KVM_UPDATE_HOST_NQN_PATH) { HttpEntity<String> e ->
            return new KVMAgentCommands.UpdateHostNqnRsp()
        }

        handle(MevocoKVMConstant.SET_VM_VF_NIC_STATE) {
            return new MevocoKVMAgentCommands.ChangeVfNicHaStateRsp()
        }

        handle(MevocoKVMConstant.ATTACH_NIC_TO_BONDING_PATH) {
            return new MevocoKVMAgentCommands.AttachNicToBondingRsp()
        }

        handle(MevocoKVMConstant.DETACH_NIC_FROM_BONDING_PATH) {
            return new MevocoKVMAgentCommands.DetachNicFromBondingRsp()
        }

        handle(MevocoKVMConstant.PRECONFIGURE_OVSDPDK_PATH) {
            return new MevocoKVMAgentCommands.PreconfigureOvsDpdkCmdRsp()
        }

        handle(MevocoKVMConstant.KVM_SYNC_VDPA_PATH) {
            return new MevocoKVMAgentCommands.SyncVdpaRsp()
        }

        handle(MevocoKVMConstant.GET_KERNEL_INTERFACE_PATH) { HttpEntity<String> e ->
            def hostUuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            def ip = Q.New(HostVO)
                    .eq(HostVO_.uuid, hostUuid)
                    .select(HostVO_.managementIp)
                    .findValue() as String

            def reply = new MevocoKVMAgentCommands.GetHostKernelInterfaceRsp()
            def to = new HostKernelInterfaceTO()
            to.interfaceName = "vmbond0"
            to.vlanId = 0

            def uTo = new UsedIpTO()
            uTo.ip = ip
            uTo.netmask = "255.255.255.0"

            to.ips = [uTo]
            reply.interfaces = [to]
            return reply
        }

        handle(MevocoKVMConstant.SET_KERNEL_INTERFACE_PATH) {
            return new MevocoKVMAgentCommands.SetHostKernelInterfaceRsp()
        }

        handle(MevocoKVMConstant.KVM_UPDATE_HOST_ISCSI_INITIATOR_NAME_PATH) { HttpEntity<String> e ->
            return new MevocoKVMAgentCommands.UpdateHostIscsiInitiatorNameRsp()
        }

        handle(MevocoKVMConstant.SET_HOST_PHYSICAL_MEMORY_MONITOR) { HttpEntity<String> e ->
            return new AgentResponse()
        }

        handle(MevocoKVMConstant.BATCH_UPDATE_BRIDGE_PATH) {
            return new MevocoKVMAgentCommands.BatchUpdateBridgeRsp()
        }

        handle(MevocoKVMConstant.ADD_BONDING_PATH) {
            return new MevocoKVMAgentCommands.CreateBondingRsp()
        }

        handle(MevocoKVMConstant.UPDATE_BONDING_PATH) {
            return new MevocoKVMAgentCommands.UpdateBondingRsp()
        }

        handle(MevocoKVMConstant.GET_HOST_BONDING_FACTS) { HttpEntity<String> e ->
            MevocoKVMAgentCommands.GetHostBondingFactsCmd cmd = json(e, MevocoKVMAgentCommands.GetHostBondingFactsCmd.class)
            def rsp = new MevocoKVMAgentCommands.GetHostBondingFactsResponse()
            def bonding = new HostNetworkBondingInventory()
            bonding.setBondingName(cmd.getBondName())
            bonding.setType("LinuxBonding")
            bonding.setBondingType("noBridge")
            bonding.setMode("active-backup")
            bonding.setSpeed(10000L)
            rsp.setBonding(bonding)
            return rsp
        }

        handle(MevocoKVMConstant.DEL_BONDING_PATH) {
            return new MevocoKVMAgentCommands.DeleteBondingRsp()
        }

        handle(LldpConstant.CHANGE_LLDP_MODE_PATH)  { HttpEntity<String> e ->
            return new LldpKvmAgentCommands.ChangeLldpModeResponse()
        }

        handle(LldpConstant.GET_LLDP_INFO_PATH)  { HttpEntity<String> e ->
            return new LldpKvmAgentCommands.GetLldpInfoResponse()
        }

        handle(LldpConstant.APPLY_LLDP_CONFIG_PATH)  { HttpEntity<String> e ->
            return new LldpKvmAgentCommands.ApplyLldpConfigResponse()
        }


        handle(MevocoKVMConstant.SET_IP_ON_HOST_NETWORK_INTERFACE) {
            return new MevocoKVMAgentCommands.SetIpOnHostNetworkInterfaceRsp()
        }

        handle(MevocoKVMConstant.SET_SERVICE_TYPE_ON_HOST_NETWORK_INTERFACE) {
            return new MevocoKVMAgentCommands.SetServiceTypeOnHostNetworkInterfaceRsp()
        }

        handle(MevocoKVMConstant.CHECK_INTERFACE_VLAN) {
            return new MevocoKVMAgentCommands.CheckInterfaceVlanRsp()
        }
        handle(MevocoKVMConstant.GET_INTERFACE_VLAN) {
            return new MevocoKVMAgentCommands.GetInterfaceVlanRsp()
        }

        handle(MevocoKVMConstant.GET_INTERFACE_NAME) {
            return new MevocoKVMAgentCommands.GetInterfaceNameRsp()
        }

        handle(CephPrimaryStorageBase.CHECK_HOST_STORAGE_CONNECTION_PATH) {
            return new CephPrimaryStorageBase.CheckHostStorageConnectionRsp()
        }

        handle(GuestToolsConstant.GET_VM_GUEST_TOOLS_INFO_PATH) { HttpEntity<String> e ->
            def rsp = new GuestToolsKvmCommands.GetVmGuestToolsInfoRsp()
            rsp.version = "1.0.0"
            rsp.status = GuestToolsAgentStatus.RUNNING.toString()
            return rsp
        }

        handle(GuestToolsConstant.GET_VM_METRICS_ROUTING_STATUS_PATH) { HttpEntity<String> e ->
            def rsp = new GuestToolsKvmCommands.GetVmMetricsRoutingStatusRsp()
            rsp.values = [
                "lighttpd.ebtables": "-p ARP --arp-ip-dst 169.254.169.254 -j USERDATA-br_eth0-f98191c4",
                "lighttpd.pid": "20763",
                "pushgateway.bind_address": ":::9092, 10.0.123.105:9092",
                "pushgateway.guest_tools.last_time_bias_in_seconds": "5.30664730072",
                "pushgateway.guest_tools.metrics.push_time_seconds": "1.6663232945372508e+09",
                "pushgateway.pid": "20464"
            ]
            return rsp
        }

        handle(KVMConstant.KVM_HOST_NUMA_PATH) { HttpEntity<String> e ->
            def rsp = new KVMAgentCommands.GetHostNUMATopologyResponse()
            HostNUMANode node0 = new HostNUMANode();
            node0.setNodeID("0")
            node0.setCpus(Arrays.asList("0","1","2","3","4","5","6","7"))
            node0.setDistance(Arrays.asList("10", "21", "21", "21"))
            node0.setFree(3889268)
            node0.setSize(38892686)

            HostNUMANode node1 = new HostNUMANode();
            node1.setNodeID("1")
            node1.setCpus(Arrays.asList("8","9","10","11","12","13","14","15"))
            node1.setDistance(Arrays.asList("21", "10", "21", "21"))
            node1.setFree(2889268)
            node1.setSize(48892686)

            HostNUMANode node2 = new HostNUMANode();
            node2.setNodeID("2")
            node2.setCpus(Arrays.asList("16","17","18","19","20","21","22","23"))
            node2.setDistance(Arrays.asList("21", "21", "10", "21"))
            node2.setFree(2889268)
            node2.setSize(48892686)

            HostNUMANode node3 = new HostNUMANode();
            node3.setNodeID("3")
            node3.setCpus(Arrays.asList("24","25","26","27","28","29","30","31"))
            node3.setDistance(Arrays.asList("21", "21", "21", "10"))
            node3.setFree(2889268)
            node3.setSize(48892686)

            Map<String, HostNUMANode> topology = ImmutableMap.of("0", node0, "1", node1, "2", node2, "3", node3)
            rsp.setTopology(topology)

            return rsp
        }

        handle(KVMConstant.KVM_BLOCK_COMMIT_VOLUME_PATH)  { HttpEntity<String> e ->
            def rsp = new BlockCommitResponse()
            rsp.size = SizeUnit.GIGABYTE.toByte(8)
            return rsp
        }

        handle(KVMConstant.KVM_BLOCK_PULL_VOLUME_PATH)  { HttpEntity<String> e ->
            def rsp = new BlockPullResponse()
            rsp.size = SizeUnit.GIGABYTE.toByte(1)
            return rsp
        }

        handle(KVMConstant.TAKE_VM_CONSOLE_SCREENSHOT_PATH) { HttpEntity<String> e ->
            def rsp = new KVMAgentCommands.TakeVmConsoleScreenshotRsp()
            rsp.imageData = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAHgAAABECAIAAADFvmZTAAACA0lEQVR4nO3VMYviQBQH8Hkvk5hE2ZUEU+juFlbKauH3/wYWNhYWKiiIRBDcZDckJJmZK6Zb7q44uJW7/f+qCcOQzP89XoSAL0FPT09VVTFzEARhGKZp2rat67qO4xhjlFJ5nidJcr1eHx8fPc+r65qZ27Zt2zbP83t//z9DRlFUVRURGWN83/c8z/O8Xq/HzFEUvb29aa211sxsgxZCKKWIKMuyPM9tSWypHMdRSmmtP72DmTudTlmWRCSl1Forpe5x2XuSSZJcLpeiKPr9flEUdV0LIZqm+fj4GI/H+/1+Npsdj8fX11chRFVV5/N5MBgIIZg5juP5fN7tdpfL5WAwiKJovV4Ph8MgCDabzWQyKcvycDgsFoskSVarlZTy+fl5t9udz2db2jvf/gsRETGz1tp13bZtbT8yszHGdqiNw66Z2W4ZY7TWUkqbl5TSGGOPSymFEHb+2Oa1u47jEFHTNFrrbxWxRQ8PD1LKuq7Lsux2u8zcNE1d10EQFEXR6XRc1yWiPM9937cxua6rlPrplIBfkS8vL2EYZlmWpul0OvV9/3Q6vb+/j0aj4/EYx3EYhkqp7XY7HA6TJDHG3G43IjocDnbsfsP2/OuIyP4P4U8Q0W8eAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADgf/QDviYImvcayGAAAAAASUVORK5CYII="
            return rsp
        }

        handle(KvmBlockLiveMigrationWorkFlow.KVM_BLOCK_LIVE_MIGRATION_PATH) { HttpEntity<String> e ->
            return new AgentResponse()
        }

        handle(MevocoKVMConstant.SYNC_VM_CLOCK_PATH) { HttpEntity<String> e ->
            return new MevocoKVMAgentCommands.SyncVmClockRsp()
        }

        handle(MevocoKVMConstant.SET_SYNC_VM_CLOCK_TASK_PATH) { HttpEntity<String> e ->
            return new MevocoKVMAgentCommands.SetSyncVmClockTaskRsp()
        }

        handle(MevocoKVMConstant.INIT_ZWATCH_METRIC_MONITOR) { HttpEntity<String> e ->
            return new AgentResponse()
        }

        handle(MevocoKVMConstant.TAKE_VOLUMES_SNAPSHOT) { HttpEntity<String> e ->
            def rsp = new MevocoKVMAgentCommands.TakeSnapshotsResponse()
            def snapshotsCmd = JSONObjectUtil.toObject(e.body, MevocoKVMAgentCommands.TakeSnapshotsCmd)
            assert(snapshotsCmd != null)
            rsp.snapshots = new ArrayList<>()
            List<TakeSnapshotsOnKvmJobStruct> snapshotJobs = snapshotsCmd.getSnapshotJobs()

            for (TakeSnapshotsOnKvmJobStruct snapshotJob : snapshotJobs) {
                TakeSnapshotsOnKvmResultStruct snapshot = new TakeSnapshotsOnKvmResultStruct()

                snapshot.setInstallPath(snapshotJob.getInstallPath())
                snapshot.setPreviousInstallPath(snapshotJob.getPreviousInstallPath())
                snapshot.setVolumeUuid(snapshotJob.getVolumeUuid())
                rsp.getSnapshots().add(snapshot)
            }

            return rsp
        }

        handle(MevocoKVMConstant.KVM_VM_CONFIG_SYNC_PORTS_PATH) {
            return new MevocoKVMAgentCommands.SyncVmPortsResponse()
        }

        handle(MevocoKVMConstant.KVM_VM_CONFIG_SYNC_SPECIFICATION_PATH) {
            return new MevocoKVMAgentCommands.SyncVmSpecificationResponse()
        }

        handle(MevocoKVMConstant.SET_VM_HOSTNAME_PATH) {
            return new MevocoKVMAgentCommands.SetVmHostnameResponse()
        }

        handle(MevocoKVMConstant.APPLY_MEMORY_BALLOON_PATH) {
            return new MevocoKVMAgentCommands.ApplyMemoryBalloonResponse()
        }

        handle(MevocoKVMConstant.SET_VM_IOTHREAD_PIN_PATH) { HttpEntity<String> e ->
            return new MevocoKVMAgentCommands.SetVmIoThreadPinRsp()
        }

        handle(MevocoKVMConstant.DEL_VM_IOTHREAD_PIN_PATH) { HttpEntity<String> e ->
            return new MevocoKVMAgentCommands.DelVmIoThreadPinRsp()
        }

        handle(MevocoKVMConstant.GET_VM_IOTHREAD_PIN_PATH) { HttpEntity<String> e ->
            return new MevocoKVMAgentCommands.GetVmIoThreadPinRsp()
        }

        handle(MevocoKVMConstant.SET_VM_SCSI_CONTROLLER_PATH) { HttpEntity<String> e ->
            return new MevocoKVMAgentCommands.SetScsiControllerRsp()
        }

        handle(MevocoKVMConstant.DEL_VM_SCSI_CONTROLLER_PATH) { HttpEntity<String> e ->
            return new MevocoKVMAgentCommands.DelScsiControllerRsp()
        }

        handle(SshKeyPairConstant.SSH_KEY_PAIR_ATTACH_TO_VM) { HttpEntity<String> e ->
            return new SshKeyPairBase.AttachSshKeyPairToVmInstanceRsp()
        }

        handle(SshKeyPairConstant.SSH_KEY_PAIR_DETACH_FROM_VM) { HttpEntity<String> e ->
            return new SshKeyPairBase.DetachSshKeyPairFromVmInstanceRsp()
        }

        handle(StorageDeviceKvmCommands.HBA_SCAN_PATH) {  HttpEntity<String> e ->
            StorageDeviceKvmCommands.HbaScanRsp rsp = new StorageDeviceKvmCommands.HbaScanRsp()
            String hostUuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            String hostIp = Q.New(HostVO.class).select(HostVO_.managementIp).eq(HostVO_.uuid, hostUuid).findValue()
            List<FcHbaDevice> hbaDevices = simulator.sqlite.query("select * from ${FcHbaDevice.class.simpleName} where hostIp = '$hostIp'", FcHbaDevice.class)
            for (FcHbaDevice hba : hbaDevices) {
                HbaDeviceStruct struct = new HbaDeviceStruct()
                struct.setHbaType(hba.hbaType)
                struct.setPortName(hba.portName)
                struct.setName(hba.name)
                struct.setSupportedClasses(hba.supportedClasses)
                struct.setSupportedSpeeds(hba.supportedSpeeds)
                struct.setSymbolicName(hba.symbolicName)
                struct.setPortState(hba.portState)
                struct.setSpeed(hba.speed)
                struct.setNodeName(hba.nodeName)
                rsp.hbaDeviceStructs.add(struct)
            }
            return rsp
        }

        handle(KVMConstant.KVM_HOST_FILE_DOWNLOAD_PATH) { HttpEntity<String> e ->
            DownloadFileResponse rsp = new DownloadFileResponse()
            rsp.md5sum = "00df1327d49e4631a21f4467aa729c11"
            rsp.size = 1024
            return rsp
        }

        handle(KVMConstant.KVM_HOST_FILE_UPLOAD_PATH) { HttpEntity<String> e ->
            UploadFileResponse rsp = new UploadFileResponse()
            rsp.directUploadUrl = "http://172.1.1.1:7070/host/file/direct-upload"
            return rsp
        }

        handle(KVMConstant.KVM_HOST_FILE_DOWNLOAD_PROGRESS_PATH) { HttpEntity<String> e ->
            GetDownloadFileProgressResponse rsp = new GetDownloadFileProgressResponse()
            rsp.completed = false
            rsp.downloadSize = 1
            rsp.size = 1024
            rsp.lastOpTime = System.currentTimeMillis()
            return rsp
        }

        handle(KVMConstant.KVM_UPDATE_HOSTNAME_PATH) { HttpEntity<String> e ->
            return new UpdateHostnameRsp()
        }

        handle(KVMConstant.READ_VM_HOST_FILE_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, ReadVmHostFileContentCmd)

            def rsp = new ReadVmHostFileContentResponse()
            for (final def param in cmd.hostFiles) {
                def to = new VmHostFileTO()
                to.path = param.path
                to.type = param.type
                to.fileFormat = VmHostFileContentFormat.Raw.toString()
                to.contentBase64 = "dGVzdA=="
                rsp.hostFiles.add(to)
            }
            return rsp
        }

        handle(KVMConstant.WRITE_VM_HOST_FILE_PATH) { HttpEntity<String> e ->
            return new WriteVmHostFileContentResponse()
        }

        handle(KVMConstant.BACKUP_VM_HOST_FILE_PATH) { HttpEntity<String> e ->
            return new BackupVmHostFileResponse()
        }
    }
}
