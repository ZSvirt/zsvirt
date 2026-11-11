package org.zstack.test.integration.guesttools

import org.springframework.http.HttpEntity
import org.springframework.web.util.UriComponentsBuilder
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.compute.host.HostTrackImpl
import org.zstack.compute.host.MevocoKVMAgentCommands
import org.zstack.compute.host.MevocoKVMConstant
import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.DatabaseFacade
import org.zstack.guesttools.GetVmGuestToolsInfoMsg
import org.zstack.guesttools.GetVmZWatchStatusMsg
import org.zstack.guesttools.GuestToolsAgentStatus
import org.zstack.guesttools.GuestToolsConstant
import org.zstack.guesttools.GuestToolsManagerImpl
import org.zstack.guesttools.GuestToolsQgaState
import org.zstack.guesttools.GuestToolsServiceType
import org.zstack.guesttools.GuestToolsStateVO
import org.zstack.guesttools.GuestToolsZWatchState
import org.zstack.guesttools.UpdateGuestToolsStateMsg
import org.zstack.guesttools.UpdateGuestToolsStateReply
import org.zstack.guesttools.kvm.GuestToolsKvmCommands
import org.zstack.header.network.l3.L3NetworkConstant
import org.zstack.header.network.l3.L3NetworkVO
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.rest.RESTConstant
import org.zstack.header.rest.RESTFacade
import org.zstack.header.vm.DiskAO
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmNicVO
import org.zstack.image.ImageSystemTags
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.guesttools.GuestToolsStateInventory
import org.zstack.softwarePackage.entity.SoftwarePackageVO
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.network.IPv6Constants
import org.zstack.zmigrate.ZMigrateConstant
import org.zstack.zmigrate.ZMigrateSystemTags
import org.zstack.zwatch.api.GetMetricDataMsg
import org.zstack.zwatch.api.GetMetricDataReply
import org.zstack.zwatch.datatype.Datapoint
import org.zstack.zwatch.namespace.VmNamespace

class GuestToolsStateCase extends SubCase {
    EnvSpec env
    SoftwarePackageVO migrationPackage
    Integer originalPingInterval
    volatile boolean qgaRunning
    volatile boolean zsToolsFound
    volatile String qgaPlatform = "Linux"
    volatile String guestToolsVersion = "1.6.0"
    volatile boolean guestToolsQueryFails
    volatile GuestToolsAgentStatus zwatchStatus = GuestToolsAgentStatus.NOT_RUNNING
    Map<String, Double> metrics = [:]
    List<String> requests = Collections.synchronizedList([])
    List<String> qgaRequests = Collections.synchronizedList([])

    @Override
    void setup() {
        useSpring(GuestToolsTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                hostname = "localhost"
                username = "root"
                password = "password"
                image {
                    name = "image1"
                    url = "http://zstack.org/download/windows.qcow2"
                    platform = "Windows"
                }
                image {
                    name = "image2"
                    url = "http://zstack.org/download/linux.qcow2"
                    platform = "Linux"
                }
            }
            zone {
                name = "zone"
                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"
                    kvm {
                        name = "kvm"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }
                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }
                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }
                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"
                    l3Network {
                        name = "l3"
                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }
                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        originalPingInterval = HostGlobalConfig.PING_HOST_INTERVAL.value(Integer.class)
        HostGlobalConfig.PING_HOST_INTERVAL.updateValue(Integer.MAX_VALUE)
        env.create {
            env.message(GetMetricDataMsg.class) { GetMetricDataMsg msg, CloudBus bus ->
                Double value = metrics[msg.metricName]
                bus.reply(msg, new GetMetricDataReply(datas: value == null ? [] : [new Datapoint(value: value)]))
            }
            env.simulator(GuestToolsConstant.GET_VM_GUEST_TOOLS_INFO_PATH) {
                requests.add(GuestToolsConstant.GET_VM_GUEST_TOOLS_INFO_PATH)
                return new GuestToolsKvmCommands.GetVmGuestToolsInfoRsp(status: "Running", version: "1.6.0")
            }
            env.simulator(GuestToolsConstant.GET_VM_ZWATCH_STATUS_PATH) {
                requests.add(GuestToolsConstant.GET_VM_ZWATCH_STATUS_PATH)
                if (guestToolsQueryFails) {
                    return new GuestToolsKvmCommands.GetVmZWatchStatusRsp(
                            success: false, error: GuestToolsConstant.GUEST_TOOLS_NOT_CONNNETED)
                }
                return new GuestToolsKvmCommands.GetVmZWatchStatusRsp(status: zwatchStatus.toString(), version: guestToolsVersion)
            }
            env.simulator(MevocoKVMConstant.KVM_VM_GUESTTOOLS_STATE_PATH) { HttpEntity<String> entity ->
                def cmd = JSONObjectUtil.toObject(entity.body, MevocoKVMAgentCommands.GetGuestToolsStateCmd.class)
                qgaRequests.addAll(cmd.vmInstanceUuids)
                def states = cmd.vmInstanceUuids.collectEntries { String uuid ->
                    [(uuid): new MevocoKVMAgentCommands.GuestToolsState(
                            qgaRunning: qgaRunning, zsToolsFound: zsToolsFound,
                            platForm: qgaPlatform, osType: qgaPlatform ? "kylin v10" : null)]
                }
                return new MevocoKVMAgentCommands.GuestToolsStateResp(states: states)
            }

            testCreateVmWithVmTools()
            testAfterStartNewCreatedVmReusesExistingGuestToolsState()
            testZWatchState()
            testQgaState()
            testQgaStateAutoUpdated()
            testMissingGuestToolsStateCanBeRecovered()
            testWindowsVersionRefresh()
            testStaticIpv4SyncAfterDescriptionEdit(true)
            testStaticIpv4SyncAfterDescriptionEdit(false)
        }
    }

    void testCreateVmWithVmTools() {
        ["image1", "image2"].each { String imageName ->
            String imageUuid = env.inventoryByName(imageName).uuid
            createSystemTag {
                resourceUuid = imageUuid
                resourceType = "ImageVO"
                tag = ImageSystemTags.IMAGE_GUEST_TOOLS.instantiateTag(
                        [(ImageSystemTags.IMAGE_GUEST_TOOLS_VERSION_TOKEN): "4.6.21"])
            }
            def vm = createVm("installed-${imageName}", imageName)
            assertState(vm, "NotRunning", "NotUpgraded")
            updateGuestToolsState { vmInstanceUuid = vm.uuid }
            assertState(vm, "NotRunning", "NotUpgraded")
            ImageSystemTags.IMAGE_GUEST_TOOLS.delete(imageUuid)
        }
    }

    void testAfterStartNewCreatedVmReusesExistingGuestToolsState() {
        def vm = createVm("existing-guest-tools-state")
        def manager = bean(GuestToolsManagerImpl.class)
        def vmVO = bean(DatabaseFacade.class).findByUuid(vm.uuid, VmInstanceVO.class)
        manager.afterStartNewCreatedVm(org.zstack.header.vm.VmInstanceInventory.valueOf(vmVO))
        def states = queryGuestToolsState { conditions = ["vmInstanceUuid=${vm.uuid}"] }
        assert states.size() == 1 : "VM startup must reuse its tools state row: actual=${states.size()}"
    }

    void testZWatchState() {
        qgaPlatform = "Linux"
        def vm = createVm("zwatch-1")
        assertState(vm, "NotInstalled", "NotInstalled")
        List<Class> messages = Collections.synchronizedList([])
        def clearInfoObserver = notifyWhenReceivedMessage(GetVmGuestToolsInfoMsg.class) { GetVmGuestToolsInfoMsg msg ->
            if (msg.vmInstanceUuid == vm.uuid) {
                messages.add(GetVmGuestToolsInfoMsg.class)
            }
        }
        def clearStatusObserver = notifyWhenReceivedMessage(GetVmZWatchStatusMsg.class) { GetVmZWatchStatusMsg msg ->
            if (msg.vmInstanceUuid == vm.uuid) {
                messages.add(GetVmZWatchStatusMsg.class)
            }
        }

        [
                [GuestToolsAgentStatus.NOT_CONNECTED, "NotInstalled", "NotInstalled"],
                [GuestToolsAgentStatus.NOT_RUNNING, "NotInstalled", "NotInstalled"],
                [GuestToolsAgentStatus.NOT_SUPPORTED, "NotInstalled", "NotInstalled"],
                [GuestToolsAgentStatus.RUNNING, "Running", "NotUpgraded"],
                [GuestToolsAgentStatus.NOT_SUPPORTED, "Running", "NotUpgraded"],
                [GuestToolsAgentStatus.NOT_RUNNING, "NotRunning", "NotUpgraded"],
                [GuestToolsAgentStatus.RUNNING, "Running", "NotUpgraded"]
        ].each { transition ->
            zwatchStatus = transition[0]
            requests.clear()
            messages.clear()
            updateGuestToolsState { vmInstanceUuid = vm.uuid }
            assertState(vm, transition[1], transition[2])
            assert messages == [GetVmZWatchStatusMsg.class] :
                    "Monitoring refresh must use the dedicated MN message: actual=${messages}"
            assert requests == [GuestToolsConstant.GET_VM_ZWATCH_STATUS_PATH] :
                    "Monitoring refresh must use only the ZWatch endpoint: actual=${requests}"
        }

        zwatchStatus = GuestToolsAgentStatus.NOT_RUNNING
        updateGuestToolsState { vmInstanceUuid = vm.uuid }
        requests.clear()
        messages.clear()
        def info = getVmGuestToolsInfo { uuid = vm.uuid }
        assert messages == [GetVmGuestToolsInfoMsg.class] :
                "General tools queries must retain their original MN message: actual=${messages}"
        assert info.status == "Running" && info.version == "1.6.0" :
                "General tools queries must retain their host fallback without metrics: actual=${info.status}/${info.version}"
        assert info.features.containsKey("pvpanic") :
                "General tools queries must retain feature extensions: actual=${info.features}"
        assert requests == [GuestToolsConstant.GET_VM_GUEST_TOOLS_INFO_PATH] :
                "General tools queries must retain the original endpoint: actual=${requests}"
        assert VmSystemTags.VM_GUEST_TOOLS.getTokenByResourceUuid(vm.uuid,
                VmSystemTags.VM_GUEST_TOOLS_VERSION_TOKEN) == "1.6.0" :
                "General tools queries must retain version tag updates"

        requests.clear()
        messages.clear()
        updateGuestToolsState { vmInstanceUuid = vm.uuid }
        assertState(vm, "NotRunning", "NotUpgraded")
        assert messages == [GetVmZWatchStatusMsg.class] :
                "Monitoring refresh must not call the general tools interface: actual=${messages}"
        assert requests == [GuestToolsConstant.GET_VM_ZWATCH_STATUS_PATH] :
                "Monitoring refresh must remain independent of general tools: actual=${requests}"
        assert VmSystemTags.VM_GUEST_TOOLS.getTokenByResourceUuid(vm.uuid,
                VmSystemTags.VM_GUEST_TOOLS_VERSION_TOKEN) == "1.6.0" :
                "ZWatch refresh must preserve the VMTools version tag"

        clearInfoObserver()
        clearStatusObserver()
        zwatchStatus = GuestToolsAgentStatus.RUNNING
        updateGuestToolsState { vmInstanceUuid = vm.uuid }
        assertState(vm, "Running", "NotUpgraded")
        stopVmInstance { uuid = vm.uuid }
        assertState(vm, "NotRunning", "NotUpgraded")
    }

    void testQgaState() {
        qgaPlatform = "Linux"
        def vm = createVm("qga-1")
        [
                [false, false, GuestToolsAgentStatus.RUNNING, "Running", "NotUpgraded"],
                [false, false, GuestToolsAgentStatus.NOT_RUNNING, "NotRunning", "NotUpgraded"],
                [true, false, GuestToolsAgentStatus.RUNNING, "Running", "NotUpgraded"],
                [true, true, GuestToolsAgentStatus.NOT_RUNNING, "NotRunning", "Running"],
                [false, true, GuestToolsAgentStatus.RUNNING, "Running", "NotRunning"],
                [true, true, GuestToolsAgentStatus.RUNNING, "Running", "Running"],
                [true, false, GuestToolsAgentStatus.NOT_RUNNING, "NotRunning", "NotInstalled"]
        ].each { transition ->
            qgaRunning = transition[0]
            zsToolsFound = transition[1]
            zwatchStatus = transition[2]
            updateGuestToolsState { vmInstanceUuid = vm.uuid }
            assertState(vm, transition[3], transition[4])
        }
    }

    void testQgaStateAutoUpdated() {
        qgaRunning = false
        zsToolsFound = false
        qgaPlatform = "Linux"
        def vm = createVm("qga-auto-update")
        assertState(vm, "NotInstalled", "NotInstalled")
        zwatchStatus = GuestToolsAgentStatus.NOT_RUNNING
        updateGuestToolsState { vmInstanceUuid = vm.uuid }
        assertState(vm, "NotInstalled", "NotInstalled")
        zwatchStatus = GuestToolsAgentStatus.RUNNING
        qgaRequests.clear()
        try {
            HostTrackImpl.alwaysStartRightNow = true
            HostGlobalConfig.PING_HOST_INTERVAL.updateValue(1)
            retryInSecs {
                assertState(vm, "Running", "NotUpgraded")
                assert qgaRequests.isEmpty() : "Host ping must not actively query QGA: actual=${qgaRequests}"
            }
        } finally {
            HostTrackImpl.alwaysStartRightNow = false
            HostGlobalConfig.PING_HOST_INTERVAL.updateValue(Integer.MAX_VALUE)
        }

        updateGuestToolsState { vmInstanceUuid = vm.uuid }
        assertState(vm, "Running", "NotUpgraded")
        stopVmInstance { uuid = vm.uuid }
        2.times { updateGuestToolsState { vmInstanceUuid = vm.uuid } }
        startVmInstance { uuid = vm.uuid }
        assertState(vm, "NotRunning", "NotUpgraded")
    }

    void testMissingGuestToolsStateCanBeRecovered() {
        def vm = createVm("missing-state")
        // Imported VMs may predate GuestToolsStateVO; remove only that row to reproduce recovery.
        bean(DatabaseFacade.class).removeByPrimaryKey(vm.uuid, GuestToolsStateVO.class)
        def states = queryGuestToolsState { conditions = ["vmInstanceUuid=${vm.uuid}"] }
        assert states.isEmpty() : "Recovery requires a missing tools state row: actual=${states.size()}"
        metrics[VmNamespace.AgentLastResponseTimestampDelta.name] = 1d
        metrics[VmNamespace.ZWatchAgentVersion.name] = (5 << 24) as double
        requests.clear()
        updateGuestToolsState { vmInstanceUuid = vm.uuid }
        def state = assertState(vm, "Running", "NotUpgraded")
        assert state.version == "5.0.0" : "Recovered tools state must retain ZWatch version: actual=${state.version}"
        assert requests.isEmpty() : "Available metrics must avoid host fallback: actual=${requests}"
    }

    void testWindowsVersionRefresh() {
        def vm = createVm("windows-version-refresh")
        def bus = bean(CloudBus.class)
        def dbf = bean(DatabaseFacade.class)
        String statusMetric = VmNamespace.AgentLastResponseTimestampDelta.name
        String versionMetric = VmNamespace.ZWatchAgentVersion.name
        Double versionCode = ((1 << 24) | (6 << 16) | (1 << 8)) as double
        guestToolsVersion = "1.5.8"
        zwatchStatus = GuestToolsAgentStatus.NOT_RUNNING
        [
                [metrics: [:], zwatch: "NotInstalled", version: "1.5.8"],
                [metrics: [(statusMetric): 1d], zwatch: "Running", version: "1.5.8"],
                [metrics: [(versionMetric): versionCode], zwatch: "NotInstalled", version: "1.6.1"],
                [metrics: [(versionMetric): versionCode], failed: true,
                 zwatch: "Running", qga: "NotInstalled", version: "1.6.1"]
        ].each { scenario ->
            def stateVO = dbf.findByUuid(vm.uuid, GuestToolsStateVO.class)
            stateVO.qgaState = GuestToolsQgaState.valueOf(scenario.qga ?: "Running")
            stateVO.zwatchState = scenario.failed ? GuestToolsZWatchState.Running : GuestToolsZWatchState.NotInstalled
            stateVO.osType = "mswindows 2022"
            stateVO.version = null
            dbf.updateAndRefresh(stateVO)
            metrics.clear()
            metrics.putAll(scenario.metrics)
            guestToolsQueryFails = scenario.failed == true
            requests.clear()
            qgaRequests.clear()
            def msg = new UpdateGuestToolsStateMsg(vmInstanceUuid: vm.uuid, hostUuid: vm.hostUuid)
            msg.serviceTypeList.remove(GuestToolsServiceType.QGA)
            bus.makeTargetServiceIdByResourceUuid(msg, GuestToolsConstant.SERVICE_ID, vm.uuid)
            assert (bus.call(msg) as UpdateGuestToolsStateReply).success
            assert assertState(vm, scenario.zwatch, scenario.qga ?: "Running").version == scenario.version
            assert requests == [GuestToolsConstant.GET_VM_ZWATCH_STATUS_PATH]
            assert qgaRequests.isEmpty()
        }
    }

    void testStaticIpv4SyncAfterDescriptionEdit(boolean migrationTarget) {
        String ip = "192.168.101.20"
        String netmask = "255.255.255.0"
        String gateway = "192.168.101.1"
        String ipv6Cidr = "2001:db8::20/64"
        String l2Id = env.inventoryByName("l2").uuid
        String imageId = env.inventoryByName(migrationTarget ? "image2" : "image1").uuid
        String clusterId = env.inventoryByName("cluster").uuid
        def dbf = bean(DatabaseFacade.class)
        String l3Id = createL3Network {
            name = migrationTarget ? "migration-no-ipam" : "ordinary-no-ipam"
            l2NetworkUuid = l2Id
            type = L3NetworkConstant.L3_BASIC_NETWORK_TYPE
            category = "Private"
            enableIPAM = false
        }.uuid
        L3NetworkVO l3 = dbf.findByUuid(l3Id, L3NetworkVO.class)
        assert !l3.enableIpAllocation()
        assert !l3.enableIPAM

        if (migrationTarget) {
            migrationPackage = new SoftwarePackageVO(
                    uuid: Platform.uuid, name: "migration-test-package",
                    type: ZMigrateConstant.ZMIGRATE_SOFTWARE_PACKAGE_TYPE)
            migrationPackage = dbf.persist(migrationPackage)
            def creator = ZMigrateSystemTags.ZMIGRATE_LINUX_BOOT_IMAGE.newSystemTagCreator(migrationPackage.uuid)
            creator.inherent = true
            creator.setTagByTokens([(ZMigrateSystemTags.ZMIGRATE_LINUX_BOOT_IMAGE_TOKEN): imageId])
            creator.create()
        }

        VmInstanceInventory vm = createVmInstance {
            name = migrationTarget ? "migrated-dual-stack" : "ordinary-dual-stack"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = imageId
            platform = "Windows"
            diskAOs = [DiskAO.rootDisk()]
            l3NetworkUuids = [l3Id]
            clusterUuid = clusterId
            systemTags = ["staticIp::${l3Id}::${ip}", "ipv4Netmask::${l3Id}::${netmask}",
                          "ipv4Gateway::${l3Id}::${gateway}"]*.toString()
        }
        assert ZMigrateSystemTags.ZMIGRATE_TARGET.hasTag(vm.uuid) == migrationTarget
        updateVmInstance {
            uuid = vm.uuid
            description = migrationTarget ? "Updated migration VM description" : "Created By Migration Service"
        }
        String nicUuid = vm.vmNics[0].uuid
        String mac = vm.vmNics[0].mac
        List<String> ipv4Report = ["${ip}/24".toString(), "gateway:${gateway}".toString()]
        reportGuestAddresses(vm.uuid, mac, ipv4Report + [ipv6Cidr, "gateway6:2001:db8::1"])

        VmNicVO nic = dbf.findByUuid(nicUuid, VmNicVO.class)
        String usedIpUuid = nic.usedIpUuid
        assert usedIpUuid != null
        assert nic.usedIps.any { it.ipVersion == IPv6Constants.IPv6 }

        def assertStaticIpv4 = {
            VmNicVO updatedNic = dbf.findByUuid(nicUuid, VmNicVO.class)
            assert updatedNic.ip == ip
            assert updatedNic.netmask == netmask
            assert updatedNic.gateway == gateway
            assert updatedNic.ipVersion == IPv6Constants.IPv4
            assert updatedNic.usedIpUuid == usedIpUuid
            assert dbf.findByUuid(usedIpUuid, UsedIpVO.class).ip == ip
        }

        reportGuestAddresses(vm.uuid, mac, [""])
        if (!migrationTarget) {
            assert dbf.findByUuid(nicUuid, VmNicVO.class).ip == null
            assert dbf.findByUuid(usedIpUuid, UsedIpVO.class) == null
            return
        }
        assertStaticIpv4()
        assert !dbf.findByUuid(nicUuid, VmNicVO.class).usedIps.any { it.ipVersion == IPv6Constants.IPv6 }

        reportGuestAddresses(vm.uuid, mac, ipv4Report)
        assertStaticIpv4()

        reportGuestAddresses(vm.uuid, mac, ["", ipv6Cidr, "gateway6:2001:db8::1"])
        assertStaticIpv4()
        reportGuestAddresses(vm.uuid, mac, ["", ipv6Cidr, "gateway6:2001:db8::2"])
        assertStaticIpv4()
        assert dbf.findByUuid(nicUuid, VmNicVO.class).usedIps.any {
            it.ipVersion == IPv6Constants.IPv6 && it.gateway == "2001:db8::2"
        }
    }

    private void reportGuestAddresses(String vmUuid, String mac, List<String> addresses) {
        RESTFacade restf = bean(RESTFacade.class)
        String url = UriComponentsBuilder.fromHttpUrl(restf.getBaseUrl())
                .path(RESTConstant.COMMAND_CHANNEL_PATH).build().toUriString()
        def cmd = new GuestToolsKvmCommands.QGANicInfoResultCmd(
                vmUuid: vmUuid, nicInfo: JSONObjectUtil.toJsonString([(mac): addresses]))
        def headers = [(RESTConstant.COMMAND_PATH): GuestToolsKvmCommands.QGA_VM_SYNC_NETWORK_INFO_PATH]
        restf.syncJsonPost(url, JSONObjectUtil.toJsonString(cmd), headers, String.class)
    }

    private VmInstanceInventory createVm(String vmName, String imageName = "image1") {
        String imageId = env.inventoryByName(imageName).uuid
        String l3Id = env.inventoryByName("l3").uuid
        String clusterId = env.inventoryByName("cluster").uuid
        return createVmInstance {
            name = vmName
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = imageId
            diskAOs = [DiskAO.rootDisk()]
            l3NetworkUuids = [l3Id]
            clusterUuid = clusterId
        }
    }

    private GuestToolsStateInventory assertState(VmInstanceInventory vm, String zwatch, String qga) {
        def states = queryGuestToolsState { conditions = ["vmInstanceUuid=${vm.uuid}"] }
        assert states.size() == 1 : "VM must have one tools state row: vm=${vm.uuid} actual=${states.size()}"
        GuestToolsStateInventory state = states[0]
        assert state.zwatchState == zwatch : "ZWatch state: vm=${vm.uuid} expected=${zwatch} actual=${state.zwatchState}"
        assert state.qgaState == qga : "QGA state: vm=${vm.uuid} expected=${qga} actual=${state.qgaState}"
        return state
    }

    @Override
    void clean() {
        HostTrackImpl.alwaysStartRightNow = false
        if (originalPingInterval != null) {
            HostGlobalConfig.PING_HOST_INTERVAL.updateValue(originalPingInterval)
        }
        if (migrationPackage != null) {
            ZMigrateSystemTags.ZMIGRATE_LINUX_BOOT_IMAGE.deleteInherentTag(migrationPackage.uuid)
            bean(DatabaseFacade.class).remove(migrationPackage)
        }
        env.delete()
    }
}
