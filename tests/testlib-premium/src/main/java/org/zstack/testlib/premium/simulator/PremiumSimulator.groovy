package org.zstack.testlib.premium.simulator

import org.springframework.http.HttpEntity
import org.zstack.compute.bonding.HostNetworkBondingConstant
import org.zstack.compute.host.HostXfsFragReader
import org.zstack.compute.host.MevocoKVMAgentCommands
import org.zstack.compute.host.MevocoKVMConstant
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.ha.entity.HaCommands
import org.zstack.ha.header.HaConstants
import org.zstack.header.Constants
import org.zstack.header.agent.AgentResponse
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.host.NetworkInterfaceType
import org.zstack.header.network.l3.UsedIpTO
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.storage.primary.PrimaryStorageVO_
import org.zstack.header.volume.VolumeInventory
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.macvlan.KvmAgentL2NetworkMacVlanCommands
import org.zstack.macvlan.L2NetworkMacVlanConstant
import org.zstack.mttyDevice.KvmMttyDeviceBackend.MttyDeviceKvmBackend
import org.zstack.network.hostNetworkInterface.HostKernelInterfaceTO
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.pciDevice.KvmPciDeviceBackend.PciDeviceKvmBackend
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceBase
import org.zstack.routeProtocol.RouteProtocolOspfBackend
import org.zstack.sdk.HostNetworkBondingInventory
import org.zstack.sdk.HostNetworkInterfaceInventory
import org.zstack.sshkeypair.SshKeyPairBase
import org.zstack.sshkeypair.SshKeyPairConstant
import org.zstack.storage.device.StorageDeviceKvmCommands
import org.zstack.storage.migration.KvmBlockLiveMigrationWorkFlow
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.KVMSimulator
import org.zstack.testlib.Simulator
import org.zstack.testlib.vfs.extensions.VFSPrimaryStorageTakeSnapshotBackend
import org.zstack.usbDevice.KvmUsbDeviceBackend.UsbDeviceKvmBackend
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.vpc.ha.vyos.VpcHaRouterCommands
import org.zstack.zwatch.prometheus.KvmHostScrape

import static org.zstack.ha.entity.HaCommands.*

/**
 * Created by weiwang on 08/11/2017
 */
class PremiumSimulator implements Simulator {
    @Override
    void registerSimulators(EnvSpec spec) {
        spec.simulator(PciDeviceKvmBackend.GET_PCI_DEVICES) {
            PciDeviceKvmBackend.GetPciDevicesRsp rsp = new PciDeviceKvmBackend.GetPciDevicesRsp()
            rsp.hostIommuStatus = true
            rsp.setSuccess(true)
            return rsp
        }

        spec.simulator(PciDeviceKvmBackend.HOT_PLUG_PCI_DEVICE) {
            PciDeviceKvmBackend.HotPlugPciDeviceRsp rsp = new PciDeviceKvmBackend.HotPlugPciDeviceRsp()
            rsp.setSuccess(true)
            return rsp
        }

        spec.simulator(PciDeviceKvmBackend.HOT_UNPLUG_PCI_DEVICE) {
            PciDeviceKvmBackend.HotUnplugPciDeviceRsp rsp = new PciDeviceKvmBackend.HotUnplugPciDeviceRsp()
            rsp.setSuccess(true)
            return rsp
        }

        spec.simulator(PciDeviceKvmBackend.ATTACH_PCI_DEVICE_TO_HOST) {
            PciDeviceKvmBackend.AttachPciDeviceToHostRsp rsp = new PciDeviceKvmBackend.AttachPciDeviceToHostRsp()
            rsp.setSuccess(true)
            return rsp
        }

        spec.simulator(PciDeviceKvmBackend.DETACH_PCI_DEVICE_FROM_HOST) {
            PciDeviceKvmBackend.DetachPciDeviceFromHostRsp rsp = new PciDeviceKvmBackend.DetachPciDeviceFromHostRsp()
            rsp.setSuccess(true)
            return rsp
        }

        spec.simulator(PciDeviceKvmBackend.CREATE_PCI_DEVICE_ROM_FILE) {
            PciDeviceKvmBackend.CreatePciDeviceRomFileRsp rsp = new PciDeviceKvmBackend.CreatePciDeviceRomFileRsp()
            rsp.setSuccess(true)
            return rsp
        }

        spec.simulator(PciDeviceKvmBackend.GENERATE_SRIOV_PCI_DEVICES) {
            PciDeviceKvmBackend.GenerateSriovPciDevicesRsp rsp = new PciDeviceKvmBackend.GenerateSriovPciDevicesRsp()
            rsp.setSuccess(true)
            return rsp
        }

        spec.simulator(PciDeviceKvmBackend.UNGENERATE_SRIOV_PCI_DEVICES) {
            PciDeviceKvmBackend.UngenerateSriovPciDevicesRsp rsp = new PciDeviceKvmBackend.UngenerateSriovPciDevicesRsp()
            rsp.setSuccess(true)
            return rsp
        }

        spec.simulator(PciDeviceKvmBackend.GENERATE_VFIO_MDEV_DEVICES) {
            PciDeviceKvmBackend.GenerateVfioMdevDevicesRsp rsp = new PciDeviceKvmBackend.GenerateVfioMdevDevicesRsp()
            rsp.setMdevUuids([Platform.getUuid(), Platform.getUuid(), Platform.getUuid(), Platform.getUuid()])
            rsp.setSuccess(true)
            return rsp
        }

        spec.simulator(PciDeviceKvmBackend.UNGENERATE_VFIO_MDEV_DEVICES) {
            PciDeviceKvmBackend.UngenerateVfioMdevDevicesRsp rsp = new PciDeviceKvmBackend.UngenerateVfioMdevDevicesRsp()
            rsp.setSuccess(true)
            return rsp
        }

        spec.simulator(MttyDeviceKvmBackend.GET_MTTY_DEVICES) {
            MttyDeviceKvmBackend.GetMttyDevicesRsp rsp = new MttyDeviceKvmBackend.GetMttyDevicesRsp()
            rsp.setSuccess(true)
            return rsp
        }

        spec.simulator(MttyDeviceKvmBackend.GENERATE_SE_VFIO_MDEV_DEVICES) {
            MttyDeviceKvmBackend.GenerateSeVfioMdevDevicesRsp rsp = new MttyDeviceKvmBackend.GenerateSeVfioMdevDevicesRsp()
            rsp.setMdevUuids([Platform.getUuid(), Platform.getUuid(), Platform.getUuid(), Platform.getUuid()])
            rsp.setSuccess(true)
            return rsp
        }

        spec.simulator(MttyDeviceKvmBackend.UNGENERATE_SE_VFIO_MDEV_DEVICES) {
            MttyDeviceKvmBackend.UngenerateSeVfioMdevDevicesRsp rsp = new MttyDeviceKvmBackend.UngenerateSeVfioMdevDevicesRsp()
            rsp.setSuccess(true)
            return rsp
        }

        spec.simulator(MdevDeviceBase.HOT_PLUG_MDEV_DEVICE) {
            MdevDeviceBase.HotPlugMdevDeviceRsp rsp = new MdevDeviceBase.HotPlugMdevDeviceRsp()
            rsp.setSuccess(true)
            return rsp
        }

        spec.simulator(MdevDeviceBase.HOT_UNPLUG_MDEV_DEVICE) {
            MdevDeviceBase.HotUnplugMdevDeviceRsp rsp = new MdevDeviceBase.HotUnplugMdevDeviceRsp()
            rsp.setSuccess(true)
            return rsp
        }

        spec.simulator(MdevDeviceBase.DELETE_VFIO_MDEV_DEVICE) {
            MdevDeviceBase.DeleteVfioMdevDeviceRsp rsp = new MdevDeviceBase.DeleteVfioMdevDeviceRsp()
            rsp.setSuccess(true)
            return rsp
        }

        spec.simulator(MevocoKVMConstant.GET_HOST_NETWORK_FACTS) { HttpEntity<String> entity, EnvSpec env ->
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
            bond0.slaves = [inv1, inv2] as List<HostNetworkInterfaceInventory>

            def bondDefault = new HostNetworkBondingInventory()
            bondDefault.bondingName = "vmbond0"
            bondDefault.type = HostNetworkBondingConstant.LINUX_BONDING_TYPE.toString()
            bondDefault.mode = "active-backup 1"
            bondDefault.xmitHashPolicy = "layer 3+4"
            bondDefault.slaves = [inv3] as List<HostNetworkInterfaceInventory>

            reply.bondings = [bond0, bondDefault] as List<HostNetworkBondingInventory>
            return reply
        }

        spec.simulator(MevocoKVMConstant.GET_HOST_PHYSICAL_CPU_FACTS) {
            def rsp = new MevocoKVMAgentCommands.GetHostPhysicalCpuFactsResponse()
            rsp.physicalCpuFacts = []
            rsp.setSuccess(true)
            return rsp
        }

        spec.simulator(MevocoKVMConstant.GET_HOST_PHYSICAL_MEMORY_FACTS) {
            def rsp = new MevocoKVMAgentCommands.GetHostPhysicalMemoryFactsResponse()
            rsp.physicalMemoryFacts = []
            rsp.setSuccess(true)
            return rsp
        }

        spec.simulator(MevocoKVMConstant.LOCATE_HOST_NETWORK_INTERFACE) {
            def rsp = new KVMAgentCommands.AgentResponse()
            rsp.setSuccess(true)
            return rsp
        }

        spec.simulator(MevocoKVMConstant.KVM_VM_CHECK_VOLUME_PATH) {
            return new KVMAgentCommands.AgentResponse()
        }

        spec.simulator(UsbDeviceKvmBackend.GET_USB_DEVICES_PATH) {
            UsbDeviceKvmBackend.GetUsbDevicesRsp rsp = new UsbDeviceKvmBackend.GetUsbDevicesRsp()
            rsp.usbDevicesInfo = []
            rsp.setSuccess(true)
            return rsp
        }

        spec.simulator(VirtualRouterConstant.VR_CONFIGURE_NIC_FIREWALL_DEFAULT_ACTION_PATH) {
            VirtualRouterCommands.ConfigureNicFirewallDefaultActionRsp rsp =
                    new VirtualRouterCommands.ConfigureNicFirewallDefaultActionRsp()
            return rsp
        }

        spec.simulator(KvmHostScrape.COLLECTD_PATH) {
            KvmHostScrape.StartCollectdExporterRsp rsp = new KvmHostScrape.StartCollectdExporterRsp()
            return rsp
        }

        spec.simulator(StorageDeviceKvmCommands.ISCSI_LOGIN_PATH) {
            StorageDeviceKvmCommands.IscsiLoginRsp rsp = new StorageDeviceKvmCommands.IscsiLoginRsp()
            return rsp
        }

        spec.simulator(StorageDeviceKvmCommands.DETACH_SCSI_DEV_PATH) {
            KVMAgentCommands.AgentResponse rsp = new KVMAgentCommands.AgentResponse()
            return rsp
        }

        spec.simulator(StorageDeviceKvmCommands.ISCSI_LOGOUT_PATH) {
            StorageDeviceKvmCommands.IscsiLogoutRsp rsp = new StorageDeviceKvmCommands.IscsiLogoutRsp()
            return rsp
        }

        spec.simulator(StorageDeviceKvmCommands.FC_SCAN_PATH) {
            StorageDeviceKvmCommands.FcScanRsp rsp = new StorageDeviceKvmCommands.FcScanRsp()
            return rsp
        }

        spec.simulator(StorageDeviceKvmCommands.NVME_SCAN_PATH) {
            StorageDeviceKvmCommands.NvmeScanRsp rsp = new StorageDeviceKvmCommands.NvmeScanRsp()
            return rsp
        }

        spec.simulator(StorageDeviceKvmCommands.NVME_CONNECT_PATH) {
            StorageDeviceKvmCommands.NvmeServerConnectRsp rsp = new StorageDeviceKvmCommands.NvmeServerConnectRsp()
            return rsp
        }

        spec.simulator(StorageDeviceKvmCommands.NVME_DISCONNECT_PATH) {
            StorageDeviceKvmCommands.NvmeServerDisconnectRsp rsp = new StorageDeviceKvmCommands.NvmeServerDisconnectRsp()
            return rsp
        }

        spec.simulator(StorageDeviceKvmCommands.MULTIPATH_ENABLE_PATH) {
            return new StorageDeviceKvmCommands.MultipathEnableRsp()
        }

        spec.simulator(StorageDeviceKvmCommands.GET_MULTIPATH_TOPOLOGY_PATH) {
            return new StorageDeviceKvmCommands.GetMultipathTopologyRsp()
        }

        spec.simulator(StorageDeviceKvmCommands.MULTIPATH_DISABLE_PATH) {
            return new KVMAgentCommands.AgentResponse()
        }

        spec.simulator(HaConstants.SETUP_SELF_FENCER_PATH) {
            return new AgentResponse()
        }

        spec.simulator(HaConstants.CANCEL_SELF_FENCER_PATH) {
            return new AgentResponse()
        }

        spec.simulator(HaConstants.SYNC_SELF_FENCER_STATE_PATH) {
            return new GetFencerStateFromHostRsp()
        }

        spec.simulator(RouteProtocolOspfBackend.ROUTER_PROTOCOL_REFRESH_OSPF) {
            return new RouteProtocolOspfBackend.SetOspfRsp()
        }

        spec.simulator(RouteProtocolOspfBackend.ROUTER_PROTOCOL_GET_OSPF_NEIGHBOR) {
            return new RouteProtocolOspfBackend.GetOspfNeighborRsp()
        }

        spec.simulator(VpcHaRouterCommands.VYOS_HA_ENABLE_PATH) {
            return new VpcHaRouterCommands.VyosHaEnableRsp()
        }

        spec.simulator(VpcHaRouterCommands.SYNC_VPC_ROUTER_HA_PATH) {
            return new VpcHaRouterCommands.SyncVpcRouterHaRsp()
        }

        spec.simulator(VpcHaRouterCommands.RESTART_KEEPALIVED_PATH) {
            return new VpcHaRouterCommands.RestartKeepalivedRsp()
        }

        spec.simulator(KvmBlockLiveMigrationWorkFlow.KVM_BLOCK_LIVE_MIGRATION_PATH) {
            return new AgentResponse()
        }

        spec.simulator(VirtualRouterConstant.VR_CHANGE_DEFAULT_ROUTE_NETWORK) {
            return new VirtualRouterCommands.ChangeDefaultNicRsp()
        }

        spec.simulator(MevocoKVMConstant.ENABLE_ZERO_COPY) {
            return new MevocoKVMAgentCommands.EnableZeroCopyResponse()
        }

        spec.simulator(MevocoKVMConstant.DISABLE_ZERO_COPY) {
            return new MevocoKVMAgentCommands.DisableZeroCopyResponse()
        }

        spec.simulator(MevocoKVMConstant.ENABLE_HUGEPAGE) {
            return new MevocoKVMAgentCommands.EnableHugePageResponse()
        }

        spec.simulator(MevocoKVMConstant.DISABLE_HUGEPAGE) {
            return new MevocoKVMAgentCommands.DisableHugePageResponse()
        }

        spec.simulator(MevocoKVMConstant.ADD_BRIDGE_FDB_ENTRY_PATH) {
            return new MevocoKVMAgentCommands.AddBridgeFdbEntryRsp()
        }

        spec.simulator(MevocoKVMConstant.SYNC_VM_CLOCK_PATH) {
            return new MevocoKVMAgentCommands.SyncVmClockRsp()
        }

        spec.simulator(MevocoKVMConstant.SET_SYNC_VM_CLOCK_TASK_PATH) {
            return new MevocoKVMAgentCommands.SetSyncVmClockTaskRsp()
        }

        spec.simulator(MevocoKVMConstant.DEL_BRIDGE_FDB_ENTRY_PATH) {
            return new MevocoKVMAgentCommands.AddBridgeFdbEntryRsp()
        }

        spec.simulator(MevocoKVMConstant.BATCH_UPDATE_BRIDGE_PATH) {
            return new MevocoKVMAgentCommands.BatchUpdateBridgeRsp()
        }

        spec.simulator(MevocoKVMConstant.SET_VM_VF_NIC_STATE) {
            return new MevocoKVMAgentCommands.ChangeVfNicHaStateRsp()
        }

        spec.simulator(MevocoKVMConstant.KVM_VM_CONFIG_SYNC_PORTS_PATH) {
            return new MevocoKVMAgentCommands.SyncVmPortsResponse()
        }

        spec.simulator(MevocoKVMConstant.KVM_VM_CONFIG_SYNC_SPECIFICATION_PATH) {
            return new MevocoKVMAgentCommands.SyncVmSpecificationResponse()
        }

        spec.simulator(MevocoKVMConstant.ADD_BONDING_PATH) {
            return new MevocoKVMAgentCommands.CreateBondingRsp()
        }

        spec.simulator(MevocoKVMConstant.UPDATE_BONDING_PATH) {
            return new MevocoKVMAgentCommands.UpdateBondingRsp()
        }

        spec.simulator(MevocoKVMConstant.DEL_BONDING_PATH) {
            return new MevocoKVMAgentCommands.DeleteBondingRsp()
        }

        spec.simulator(MevocoKVMConstant.ATTACH_NIC_TO_BONDING_PATH) {
            return new MevocoKVMAgentCommands.AttachNicToBondingRsp()
        }

        spec.simulator(MevocoKVMConstant.DETACH_NIC_FROM_BONDING_PATH) {
            return new MevocoKVMAgentCommands.DetachNicFromBondingRsp()
        }

        spec.simulator(MevocoKVMConstant.SET_IP_ON_HOST_NETWORK_INTERFACE) {
            return new MevocoKVMAgentCommands.SetIpOnHostNetworkInterfaceRsp()
        }

        spec.simulator(MevocoKVMConstant.SET_SERVICE_TYPE_ON_HOST_NETWORK_INTERFACE) {
            return new MevocoKVMAgentCommands.SetServiceTypeOnHostNetworkInterfaceRsp()
        }

        spec.simulator(MevocoKVMConstant.GET_INTERFACE_NAME) {
            return new MevocoKVMAgentCommands.GetInterfaceNameRsp()
        }

        spec.simulator(MevocoKVMConstant.CHECK_INTERFACE_VLAN) {
            return new MevocoKVMAgentCommands.CheckInterfaceVlanRsp()
        }

        spec.simulator(MevocoKVMConstant.GET_INTERFACE_VLAN) {
            return new MevocoKVMAgentCommands.GetInterfaceVlanRsp()
        }

        spec.simulator(MevocoKVMConstant.SET_HOST_PHYSICAL_NIC_MONITOR) {
            return new MevocoKVMAgentCommands.SetHostPhysicalNicMonitorRsp()
        }

        spec.simulator(MevocoKVMConstant.GET_KERNEL_INTERFACE_PATH) { HttpEntity<String> entity, EnvSpec env ->
            def hostUuid = entity.headers.getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            def ip = Q.New(HostVO.class)
                    .eq(HostVO_.uuid, hostUuid)
                    .select(HostVO_.managementIp)
                    .findValue() as String

            def to = new HostKernelInterfaceTO()
            to.interfaceName = "vmbond0"
            to.vlanId = 0
            to.ips = [new UsedIpTO(ip: ip, netmask: "255.255.255.0")]
            return new MevocoKVMAgentCommands.GetHostKernelInterfaceRsp(interfaces: [to])
        }

        spec.simulator(MevocoKVMConstant.SET_KERNEL_INTERFACE_PATH) {
            return new MevocoKVMAgentCommands.SetHostKernelInterfaceRsp()
        }

        spec.simulator(MevocoKVMConstant.KVM_UPDATE_HOST_ISCSI_INITIATOR_NAME_PATH) {
            return new MevocoKVMAgentCommands.UpdateHostIscsiInitiatorNameRsp()
        }

        spec.simulator(HostXfsFragReader.HOST_XFS_SCRAPE_PATH) {
            HostXfsFragReader.GetXfsDataRsp rsp = new HostXfsFragReader.GetXfsDataRsp()
            rsp.fsType = HostXfsFragReader.XFS_TYPE
            rsp.hostFrag = null
            rsp.volumeFragMap = null
            rsp.setSuccess(true)
        }

        spec.simulator(MevocoKVMConstant.TAKE_VOLUMES_SNAPSHOT) { HttpEntity<String> e, EnvSpec espec ->
            def rsp = new MevocoKVMAgentCommands.TakeSnapshotsResponse(snapshots: []) as MevocoKVMAgentCommands.TakeSnapshotsResponse
            def cmd = JSONObjectUtil.toObject(e.body, MevocoKVMAgentCommands.TakeSnapshotsCmd.class) as MevocoKVMAgentCommands.TakeSnapshotsCmd
            rsp.snapshots = KVMSimulator.takeSnapshotByPrimaryStorage(e, espec, cmd.snapshotJobs)
            return rsp
        }

        spec.simulator(MevocoKVMConstant.BLOCK_STREAM) { HttpEntity<String> e, EnvSpec espec ->
            def rsp = new KVMAgentCommands.AgentResponse() as KVMAgentCommands.AgentResponse
            MevocoKVMAgentCommands.BlockStreamVolumeCmd cmd = JSONObjectUtil.toObject(e.body, MevocoKVMAgentCommands.BlockStreamVolumeCmd.class) as MevocoKVMAgentCommands.BlockStreamVolumeCmd
            VolumeVO volume = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, cmd.volume.volumeUuid).find()
            String primaryStorageType = Q.New(PrimaryStorageVO.class)
                    .select(PrimaryStorageVO_.type)
                    .eq(PrimaryStorageVO_.uuid, volume.primaryStorageUuid)
                    .findValue()
            VFSPrimaryStorageTakeSnapshotBackend backend = KVMSimulator.getVFSPrimaryStorageTakeSnapshotBackend(primaryStorageType)
            backend.blockStream(e, espec, VolumeInventory.valueOf(volume))
            return rsp
        }

        spec.simulator(MevocoKVMConstant.APPLY_MEMORY_BALLOON_PATH) {
            return new MevocoKVMAgentCommands.ApplyMemoryBalloonResponse()
        }

        spec.simulator(MevocoKVMConstant.SET_VM_IOTHREAD_PIN_PATH) {
            return new MevocoKVMAgentCommands.SetVmIoThreadPinRsp()
        }

        spec.simulator(MevocoKVMConstant.GET_VM_IOTHREAD_PIN_PATH) {
            return new MevocoKVMAgentCommands.GetVmIoThreadPinRsp()
        }

        spec.simulator(MevocoKVMConstant.DEL_VM_IOTHREAD_PIN_PATH) {
            return new MevocoKVMAgentCommands.DelVmIoThreadPinRsp()
        }

        spec.simulator(MevocoKVMConstant.SET_VM_SCSI_CONTROLLER_PATH) {
            return new MevocoKVMAgentCommands.SetScsiControllerRsp()
        }

        spec.simulator(MevocoKVMConstant.DEL_VM_SCSI_CONTROLLER_PATH) {
            return new MevocoKVMAgentCommands.DelScsiControllerRsp()
        }

        spec.simulator(HaCommands.AddVmFencerRuleToHostCmd.PATH) {
            return new HaCommands.AddVmFencerRuleToHostRsp()
        }

        spec.simulator(HaCommands.RemoveVmFencerRuleFromHostCmd.PATH) {
            return new HaCommands.RemoveVmFencerRuleFromHostRsp()
        }

        spec.simulator(HaConstants.SCAN_HOST_PATH) { HttpEntity<String> e ->
            def rsp = new ScanRsp()
            rsp.result = ScanRsp.RET_SUCCESS
            return rsp
        }

        spec.simulator(HaConstants.SYNC_HA_VM_LIST_PATH) {
            return new AgentResponse()
        }

        spec.simulator(L2NetworkMacVlanConstant.KVM_CHECK_L2VLAN_NETWORK_MACVLAN_PATH) {
            return new KvmAgentL2NetworkMacVlanCommands.CheckVlanBridgeResponse();
        }

        spec.simulator(L2NetworkMacVlanConstant.KVM_REALIZE_L2VLAN_NETWORK_MACVLAN_PATH) {
            return new KvmAgentL2NetworkMacVlanCommands.CreateVlanBridgeResponse();
        }

        spec.simulator(L2NetworkMacVlanConstant.KVM_DELETE_L2VLAN_NETWORK_MACVLAN_PATH) {
            return new KvmAgentL2NetworkMacVlanCommands.DeleteVlanBridgeResponse();
        }

        spec.simulator(MevocoKVMConstant.PRECONFIGURE_OVSDPDK_PATH) {
            return new MevocoKVMAgentCommands.PreconfigureOvsDpdkCmdRsp()
        }

        spec.simulator(MevocoKVMConstant.KVM_SYNC_VDPA_PATH) {
            return new MevocoKVMAgentCommands.SyncVdpaRsp()
        }

        spec.simulator(SshKeyPairConstant.SSH_KEY_PAIR_ATTACH_TO_VM) {
            return new SshKeyPairBase.AttachSshKeyPairToVmInstanceRsp()
        }

        spec.simulator(SshKeyPairConstant.SSH_KEY_PAIR_DETACH_FROM_VM) {
            return new SshKeyPairBase.DetachSshKeyPairFromVmInstanceRsp()
        }

        spec.simulator(StorageDeviceKvmCommands.HBA_SCAN_PATH) {
            KVMAgentCommands.AgentResponse rsp = new StorageDeviceKvmCommands.HbaScanRsp()
            return rsp
        }
    }
}
