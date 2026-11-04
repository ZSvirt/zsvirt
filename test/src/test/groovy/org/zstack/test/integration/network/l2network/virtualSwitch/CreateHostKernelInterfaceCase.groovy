package org.zstack.test.integration.network.l2network.virtualSwitch

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostSystemTags
import org.zstack.compute.host.MevocoKVMAgentCommands
import org.zstack.compute.host.MevocoKVMConstant
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.core.db.Q
import org.zstack.core.thread.ThreadFacade
import org.zstack.header.Constants
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.identity.AccountConstant
import org.zstack.header.message.DocUtils
import org.zstack.header.message.MessageReply
import org.zstack.header.network.l3.L3NetworkCategory
import org.zstack.header.network.l3.UsedIpTO
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.RefreshHostExtraIpsMsg
import org.zstack.network.hostNetworkInterface.HostKernelInterfaceTO
import org.zstack.network.l2.virtualSwitch.header.CreateHostKernelInterfaceMsg
import org.zstack.network.l2.virtualSwitch.header.CreateHostKernelInterfaceReply
import org.zstack.network.l2.virtualSwitch.header.HostKernelInterfaceStruct
import org.zstack.network.l2.virtualSwitch.header.HostKernelInterfaceTrafficType
import org.zstack.network.l2.virtualSwitch.header.VirtualSwitchConstant
import org.zstack.network.l2.virtualSwitch.header.VirtualSwitchSystemTags
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.*
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.premium.network.NetworkController
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.network.IPv6Constants

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import static org.zstack.utils.CollectionDSL.e
import static org.zstack.utils.CollectionDSL.map

class CreateHostKernelInterfaceCase extends SubCase {
    static final String OPERATION_ERROR_CODE = SysErrors.OPERATION_ERROR.toString()
    static final String INVALID_ARGUMENT_ERROR_CODE = SysErrors.INVALID_ARGUMENT_ERROR.toString()
    static final String MANAGEMENT_TRAFFIC_TYPE = HostKernelInterfaceTrafficType.Management.toString()
    static final String STORAGE_TRAFFIC_TYPE = HostKernelInterfaceTrafficType.Storage.toString()

    EnvSpec env
    NetworkController controller

    ClusterInventory cluster
    ZoneInventory zone
    KVMHostInventory host
    KVMHostInventory host2
    KVMHostInventory host3
    KVMHostInventory host4
    L2VirtualSwitchNetworkInventory vswitch
    HostNetworkBondingInventory bond0
    PortGroupInventory simpleL3
    PortGroupInventory normalL3
    HostKernelInterfaceStruct struct = new HostKernelInterfaceStruct()
    HostKernelInterfaceStruct struct2 = new HostKernelInterfaceStruct()
    Map<String, List<String>> netServices = new HashMap<>()

    @Override
    void setup() {
        useSpring(VirtualSwitchTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            zone {
                name = "zone"
                description = "test"

                preCreate {
                    this.controller = new NetworkController(this.env)
                }

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }
                }
            }
        }
    }

    void testCreateHostKernelInterfaceWithSimpleL3() {
        vswitch = createL2VirtualSwitch {
            name = "dvs-1"
            physicalInterface = "bond0"
            zoneUuid = zone.uuid
            systemTags = [VirtualSwitchSystemTags.UPLINK_BONDING.instantiateTag(map(
                    e(VirtualSwitchSystemTags.BONDING_MODE_TOKEN, bond0.mode)))]
        } as L2VirtualSwitchNetworkInventory

        attachL2NetworkToCluster {
            l2NetworkUuid = vswitch.uuid
            clusterUuid = cluster.uuid
        }

        def pg1 = createPortGroup {
            name = "pg-1"
            vSwitchUuid = vswitch.uuid
            vlan = 111
            category = L3NetworkCategory.Private
            enableIPAM = false
            systemTags = ["resourceConfig::l3Network::ipAllocateStrategy::RandomIpAllocatorStrategy"]
        } as PortGroupInventory

        MevocoKVMAgentCommands.SetHostKernelInterfaceCmd cmd = null
        env.afterSimulator(MevocoKVMConstant.SET_KERNEL_INTERFACE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, MevocoKVMAgentCommands.SetHostKernelInterfaceCmd.class)
            return rsp
        }

        def zskernel_1 = createHostKernelInterface {
            name = "zskernel-1-on-host1"
            description = "zskernel-1-description"
            hostUuid = host.uuid
            l3NetworkUuid = pg1.uuid
            requiredIp = "1.1.1.1"
            netmask = "255.255.255.0"
            trafficTypes = [MANAGEMENT_TRAFFIC_TYPE]
        } as HostKernelInterfaceInventory

        assert zskernel_1.name == "zskernel-1-on-host1"
        assertExtraIps(host, ["1.1.1.1"])
        assert zskernel_1.description == "zskernel-1-description"
        assert zskernel_1.hostUuid == host.uuid
        assert zskernel_1.l3NetworkUuid == pg1.uuid
        assert zskernel_1.usedIps.size() == 1
        assert zskernel_1.trafficTypes.size() == 1
        assert (queryHostKernelInterface {conditions=["uuid=${zskernel_1.uuid}"]} as List<HostKernelInterfaceInventory>).size() == 1
        assert zqlQuery("query usedIp where ip='1.1.1.1' and l3NetworkUuid='${pg1.uuid}'").size() == 1
        assert zqlQuery("query hostKernelInterfaceUsedIp where hostKernelInterfaceUuid='${zskernel_1.uuid}'").size() == 1
        assert zqlQuery("query hostKernelInterfaceTrafficType where hostKernelInterfaceUuid='${zskernel_1.uuid}'" +
                " and trafficType='${HostKernelInterfaceTrafficType.Management}'").size() == 1

        retryInSecs {
            assert cmd != null
            HostKernelInterfaceTO to = cmd.interfaces.get(0)
            assert to.interfaceName == "bond0"
            assert to.vlanId == 111

            assert to.ips.size() == 1
            assert to.ips.get(0).ipVersion == IPv6Constants.IPv4
            assert to.ips.get(0).ip == "1.1.1.1"
            assert to.ips.get(0).netmask == "255.255.255.0"
            assert to.ips.get(0).actionCode == UsedIpTO.ACTION_CODE_ADD
        }

        def zskernel_2 = createHostKernelInterface {
            name = "zskernel-2-on-host1"
            hostUuid = host.uuid
            l3NetworkUuid = pg1.uuid
            requiredIp = "2.2.2.2"
            netmask = "255.255.255.0"
        } as HostKernelInterfaceInventory

        assert zqlQuery("query usedIp where ip='2.2.2.2' and l3NetworkUuid='${pg1.uuid}'").size() == 1
        assertExtraIps(host, ["1.1.1.1", "2.2.2.2"])
        assert zqlQuery("query hostKernelInterfaceUsedIp where hostKernelInterfaceUuid='${zskernel_2.uuid}'").size() == 1

        retryInSecs {
            assert cmd != null
            HostKernelInterfaceTO to = cmd.interfaces.get(0)
            assert to.interfaceName == "bond0"
            assert to.vlanId == 111

            assert to.ips.size() == 1
            assert to.ips.get(0).ipVersion == IPv6Constants.IPv4
            assert to.ips.get(0).ip == "2.2.2.2"
            assert to.ips.get(0).netmask == "255.255.255.0"
            assert to.ips.get(0).actionCode == UsedIpTO.ACTION_CODE_ADD
        }

        zskernel_1 = updateHostKernelInterface {
            uuid = zskernel_1.uuid
            name = "zskernel-1-rename"
        } as HostKernelInterfaceInventory

        assert zskernel_1.name == "zskernel-1-rename"
        assertExtraIps(host, ["1.1.1.1", "2.2.2.2"])
        assert zskernel_1.description == "zskernel-1-description"
        assert zqlQuery("query usedIp where ip='1.1.1.1' and l3NetworkUuid='${pg1.uuid}'").size() == 1
        assert zqlQuery("query hostKernelInterfaceUsedIp where hostKernelInterfaceUuid='${zskernel_1.uuid}'").size() == 1
        assert zqlQuery("query hostKernelInterfaceTrafficType where hostKernelInterfaceUuid='${zskernel_1.uuid}'" +
                " and trafficType='${HostKernelInterfaceTrafficType.Management}'").size() == 1

        zskernel_1 = updateHostKernelInterface {
            uuid = zskernel_1.uuid
            name = "zskernel-1-remove-traffic-type"
            trafficTypes = []
        } as HostKernelInterfaceInventory

        assert zskernel_1.name == "zskernel-1-remove-traffic-type"
        assertExtraIps(host, ["1.1.1.1", "2.2.2.2"])
        assert zskernel_1.description == "zskernel-1-description"
        assert zskernel_1.trafficTypes.isEmpty()
        assert zqlQuery("query usedIp where ip='1.1.1.1' and l3NetworkUuid='${pg1.uuid}'").size() == 1
        assert zqlQuery("query hostKernelInterfaceUsedIp where hostKernelInterfaceUuid='${zskernel_1.uuid}'").size() == 1
        assert zqlQuery("query hostKernelInterfaceTrafficType where hostKernelInterfaceUuid='${zskernel_1.uuid}'").isEmpty()

        cmd = null as MevocoKVMAgentCommands.SetHostKernelInterfaceCmd
        zskernel_1 = updateHostKernelInterface {
            uuid = zskernel_1.uuid
            name = "zskernel-1-with-same-ip"
            requiredIp = "1.1.1.1"
            netmask = "255.255.255.0"
        } as HostKernelInterfaceInventory
        assert zskernel_1.name == "zskernel-1-with-same-ip"
        assert zqlQuery("query usedIp where ip='1.1.1.1' and l3NetworkUuid='${pg1.uuid}'").size() == 1
        assert zqlQuery("query hostKernelInterfaceUsedIp where hostKernelInterfaceUuid='${zskernel_1.uuid}'").size() == 1
        assert zqlQuery("query hostKernelInterfaceTrafficType where hostKernelInterfaceUuid='${zskernel_1.uuid}'").isEmpty()
        assert cmd == null
        assertExtraIps(host, ["1.1.1.1", "2.2.2.2"])

        cmd = null as MevocoKVMAgentCommands.SetHostKernelInterfaceCmd
        zskernel_1 = updateHostKernelInterface {
            uuid = zskernel_1.uuid
            name = "zskernel-1-reconfigure-ip"
            description = "zskernel-1-description"
            requiredIp = "1.1.1.3"
            netmask = "255.255.255.0"
            trafficTypes = [MANAGEMENT_TRAFFIC_TYPE]
        } as HostKernelInterfaceInventory
        assert zskernel_1.name == "zskernel-1-reconfigure-ip"
        assertExtraIps(host, ["1.1.1.3", "2.2.2.2"])
        assert zskernel_1.description == "zskernel-1-description"
        assert zqlQuery("query usedIp where ip='1.1.1.3' and l3NetworkUuid='${pg1.uuid}'").size() == 1
        assert zqlQuery("query hostKernelInterfaceUsedIp where hostKernelInterfaceUuid='${zskernel_1.uuid}'").size() == 1
        assert zqlQuery("query hostKernelInterfaceTrafficType where hostKernelInterfaceUuid='${zskernel_1.uuid}'" +
                " and trafficType='${HostKernelInterfaceTrafficType.Management}'").size() == 1
        retryInSecs {
            assert cmd != null
            HostKernelInterfaceTO to = cmd.interfaces.get(0)
            assert to.interfaceName == "bond0"
            assert to.vlanId == 111

            assert to.ips.size() == 2
            assert to.ips.every { it.ipVersion == IPv6Constants.IPv4 }
            assert to.ips.every { it.netmask == "255.255.255.0" }
            assert to.ips.find { it.actionCode == UsedIpTO.ACTION_CODE_ADD }.ip == "1.1.1.3"
            assert to.ips.find { it.actionCode == UsedIpTO.ACTION_CODE_REMOVE }.ip == "1.1.1.1"
        }

        cmd = null as MevocoKVMAgentCommands.SetHostKernelInterfaceCmd
        zskernel_1 = updateHostKernelInterface {
            uuid = zskernel_1.uuid
            name = "zskernel-1-reconfigure-netmask"
            description = "zskernel-1-description"
            requiredIp = "1.1.1.3"
            netmask = "255.255.0.0"
            trafficTypes = [MANAGEMENT_TRAFFIC_TYPE]
        } as HostKernelInterfaceInventory
        assert zskernel_1.name == "zskernel-1-reconfigure-netmask"
        assertExtraIps(host, ["1.1.1.3", "2.2.2.2"])
        assert zskernel_1.description == "zskernel-1-description"
        assert zqlQuery("query usedIp where ip='1.1.1.3' and netmask='255.255.0.0' and l3NetworkUuid='${pg1.uuid}'").size() == 1
        assert zqlQuery("query hostKernelInterfaceUsedIp where hostKernelInterfaceUuid='${zskernel_1.uuid}'").size() == 1
        assert zqlQuery("query hostKernelInterfaceTrafficType where hostKernelInterfaceUuid='${zskernel_1.uuid}'" +
                " and trafficType='${HostKernelInterfaceTrafficType.Management}'").size() == 1
        retryInSecs {
            assert cmd != null
            HostKernelInterfaceTO to = cmd.interfaces.get(0)
            assert to.interfaceName == "bond0"
            assert to.vlanId == 111

            assert to.ips.size() == 2
            assert to.ips.every { it.ipVersion == IPv6Constants.IPv4 }
            assert to.ips.every { it.ip == "1.1.1.3" }
            assert to.ips.find { it.actionCode == UsedIpTO.ACTION_CODE_ADD }.netmask == "255.255.0.0"
            assert to.ips.find { it.actionCode == UsedIpTO.ACTION_CODE_REMOVE }.netmask == "255.255.255.0"
        }

        expectApiFailure({
            updateHostKernelInterface {
                uuid = zskernel_1.uuid
                name = "zskernel-1-reconfigure-netmask-without-ip"
                description = "zskernel-1-description"
                netmask = "255.255.255.0"
                trafficTypes = [MANAGEMENT_TRAFFIC_TYPE]
            } as HostKernelInterfaceInventory
        }) {
            assert code == INVALID_ARGUMENT_ERROR_CODE
            assert details == "could not update host kernel interface[uuid:${zskernel_1.uuid}], because netmask cannot be set without requiredIp".toString()
        }
        cmd = null as MevocoKVMAgentCommands.SetHostKernelInterfaceCmd
        deleteHostKernelInterface {
            uuid = zskernel_1.uuid
        }

        assert (queryHostKernelInterface {conditions=["uuid=${zskernel_1.uuid}"]} as List<HostKernelInterfaceInventory>).isEmpty()
        assertExtraIps(host, ["2.2.2.2"])

        retryInSecs {
            assert cmd != null
            HostKernelInterfaceTO to = cmd.interfaces.get(0)
            assert to.interfaceName == "bond0"
            assert to.vlanId == 111

            assert to.ips.size() == 1
            assert to.ips.get(0).ipVersion == IPv6Constants.IPv4
            assert to.ips.get(0).ip == "1.1.1.3"
            assert to.ips.get(0).netmask == "255.255.0.0"
            assert to.ips.get(0).actionCode == UsedIpTO.ACTION_CODE_REMOVE
        }

        deleteHostKernelInterface {
            uuid = zskernel_2.uuid
        }
        assertExtraIps(host, [])
    }

    void testConcurrentHostExtraIpsRefreshOnDifferentL2Networks() {
        def pgs = [112, 113].collect { vlanId ->
            createPortGroup {
                name = "concurrent-pg-${vlanId}"
                vSwitchUuid = vswitch.uuid
                vlan = vlanId
                category = L3NetworkCategory.Private
                enableIPAM = false
            } as PortGroupInventory
        }
        assert pgs[0].l2NetworkUuid != pgs[1].l2NetworkUuid
        def ips = ["11.11.11.11", "12.12.12.12"]
        def firstEntered = new CountDownLatch(1)
        def secondEntered = new CountDownLatch(1)
        def secondIpApplied = new CountDownLatch(1)
        def secondRefreshRequested = new CountDownLatch(1)
        def releaseFirst = new CountDownLatch(1)
        def completed = [new CountDownLatch(1), new CountDownLatch(1)]
        def calls = new AtomicInteger()
        def refreshes = new AtomicInteger()
        def snapshots = Collections.synchronizedList([])
        def replies = Collections.synchronizedMap([:]) as Map<Integer, MessageReply>
        def previousSetHandler = env.getPostSimulator(MevocoKVMConstant.SET_KERNEL_INTERFACE_PATH) ?: { rsp -> rsp }
        def cleaner = notifyWhenReceivedMessage(RefreshHostExtraIpsMsg) { RefreshHostExtraIpsMsg msg ->
            if (msg.hostUuid == host.uuid && refreshes.incrementAndGet() == 2) {
                secondRefreshRequested.countDown()
            }
        }
        env.afterSimulator(MevocoKVMConstant.SET_KERNEL_INTERFACE_PATH) { rsp, HttpEntity<String> entity ->
            def cmd = JSONObjectUtil.toObject(entity.body, MevocoKVMAgentCommands.SetHostKernelInterfaceCmd.class)
            if (entity.headers.getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID) == host.uuid &&
                    cmd.interfaces.any { it.ips.any { it.ip == ips[1] } }) {
                secondIpApplied.countDown()
            }
            return rsp
        }
        env.hijackSimulator(KVMConstant.KVM_HOST_FACT_PATH) {
            KVMAgentCommands.HostFactResponse rsp, HttpEntity<String> entity ->
            if (entity.headers.getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID) == host.uuid) {
                snapshots.add(new ArrayList<String>(rsp.ipAddresses))
                if (calls.incrementAndGet() == 1) {
                    firstEntered.countDown()
                    assert releaseFirst.await(60, TimeUnit.SECONDS)
                } else {
                    secondEntered.countDown()
                }
            }
            return rsp
        }

        def bus = bean(CloudBus)
        int submitted = 0
        try {
            pgs.eachWithIndex { pg, index ->
                def msg = new CreateHostKernelInterfaceMsg()
                msg.name = "concurrent-kernel-${index}"
                msg.hostUuid = host.uuid
                msg.l2NetworkUuid = pg.l2NetworkUuid
                msg.l3NetworkUuid = pg.uuid
                msg.requiredIp = ips[index]
                msg.netmask = "255.255.255.0"
                msg.accountUuid = AccountConstant.INITIAL_SYSTEM_ADMIN_UUID
                bus.makeTargetServiceIdByResourceUuid(msg, VirtualSwitchConstant.VIRTUAL_SWITCH_SERVICE_ID, msg.l2NetworkUuid)
                bus.send(msg, new CloudBusCallBack(null) {
                    @Override
                    void run(MessageReply reply) {
                        replies[index] = reply
                        completed[index].countDown()
                    }
                })
                submitted++
                if (index == 0) {
                    assert firstEntered.await(10, TimeUnit.SECONDS)
                }
            }
            assert secondIpApplied.await(10, TimeUnit.SECONDS)
            assert secondRefreshRequested.await(10, TimeUnit.SECONDS)
            boolean overlappingSnapshots = false
            retryInSecs {
                if (secondEntered.count == 0) {
                    overlappingSnapshots = true
                } else {
                    def queue = bean(ThreadFacade).getChainTaskInfo("refresh-host-extra-ips-${host.uuid}".toString())
                    assert queue?.maxThreadNum == 1
                    assert queue?.runningTask?.size() == 1
                    assert queue?.pendingTask?.size() == 1
                }
            }
            if (overlappingSnapshots) {
                assert completed[1].await(30, TimeUnit.SECONDS)
                assert replies[1].success : replies[1].error
                assertExtraIps(host, ips)
            }
            releaseFirst.countDown()
            assert completed.every { it.await(30, TimeUnit.SECONDS) }
            assert replies.size() == 2
            assert replies.values().every { it.success } : replies.values()*.error
            pgs.eachWithIndex { pg, index ->
                def kernel = (replies[index] as CreateHostKernelInterfaceReply).inventory
                assert kernel.hostUuid == host.uuid
                assert kernel.l3NetworkUuid == pg.uuid
                assert kernel.usedIps*.ip == [ips[index]]
                assert zqlQuery("query hostKernelInterface where uuid='${kernel.uuid}'").size() == 1
                assert Q.New(UsedIpVO.class).eq(UsedIpVO_.ip, ips[index]).eq(UsedIpVO_.l3NetworkUuid, pg.uuid).isExists()
            }
            assert snapshots[0].toSet() == [host.managementIp, ips[0]].toSet()
            assert snapshots[1].toSet() == ([host.managementIp] + ips).toSet()
            assertExtraIps(host, ips)
        } finally {
            releaseFirst.countDown()
            try {
                assert (0..<submitted).collect { completed[it].await(30, TimeUnit.SECONDS) }.every { it }
            } finally {
                env.hijackSimulator(KVMConstant.KVM_HOST_FACT_PATH) { rsp -> rsp }
                env.afterSimulator(MevocoKVMConstant.SET_KERNEL_INTERFACE_PATH, previousSetHandler)
                cleaner.call()
            }
            pgs.each { pg -> deletePortGroup { uuid = pg.uuid } }
            assertExtraIps(host, [])
        }
    }

    void testCreateHostKernelInterfaceWithNormalL3() {
        def pg2 = createPortGroup {
            name = "pg-2"
            vSwitchUuid = vswitch.uuid
            vlan = 222
            category = L3NetworkCategory.Private
        } as PortGroupInventory

        attachNetworkServiceToL3Network {
            l3NetworkUuid = pg2.uuid
            networkServices = netServices
        }

        addIpRange {
            name = "ipr-1"
            l3NetworkUuid = pg2.uuid
            startIp = "192.168.100.101"
            endIp = "192.168.100.200"
            gateway = "192.168.100.1"
            netmask = "255.255.255.0"
        }

        def cmd = null as MevocoKVMAgentCommands.SetHostKernelInterfaceCmd
        env.afterSimulator(MevocoKVMConstant.SET_KERNEL_INTERFACE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, MevocoKVMAgentCommands.SetHostKernelInterfaceCmd.class)
            return rsp
        }

        def zskernel1_pg2 = createHostKernelInterface {
            name = "zskernel1-pg2-on-host1"
            hostUuid = host.uuid
            l3NetworkUuid = pg2.uuid
            requiredIp = "192.168.100.150"
            netmask = "255.255.255.0"
            trafficTypes = [MANAGEMENT_TRAFFIC_TYPE]
        } as HostKernelInterfaceInventory

        assert (queryHostKernelInterface {conditions=["uuid=${zskernel1_pg2.uuid}"]} as List<HostKernelInterfaceInventory>).size() == 1
        assertExtraIps(host, ["192.168.100.150"])
        assert zqlQuery("query usedIp where ip='192.168.100.150' and l3NetworkUuid='${pg2.uuid}'").size() == 1
        assert zqlQuery("query hostKernelInterfaceUsedIp where hostKernelInterfaceUuid='${zskernel1_pg2.uuid}'").size() == 1
        assert zqlQuery("query hostKernelInterfaceTrafficType where hostKernelInterfaceUuid='${zskernel1_pg2.uuid}'" +
                " and trafficType='${HostKernelInterfaceTrafficType.Management}'").size() == 1

        retryInSecs {
            assert cmd != null
            HostKernelInterfaceTO to = cmd.interfaces.get(0)
            assert to.interfaceName == "bond0"
            assert to.vlanId == 222

            assert to.ips.size() == 1
            assert to.ips.get(0).ipVersion == IPv6Constants.IPv4
            assert to.ips.get(0).ip == "192.168.100.150"
            assert to.ips.get(0).netmask == "255.255.255.0"
            assert to.ips.get(0).actionCode == UsedIpTO.ACTION_CODE_ADD
        }

        cmd = null as MevocoKVMAgentCommands.SetHostKernelInterfaceCmd
        def zskernel2_pg2 = createHostKernelInterface {
            name = "zskernel2-pg2-on-host2"
            hostUuid = host2.uuid
            l3NetworkUuid = pg2.uuid
        } as HostKernelInterfaceInventory

        assert (queryHostKernelInterface {conditions=["uuid=${zskernel2_pg2.uuid}"]} as List<HostKernelInterfaceInventory>).size() == 1
        assert zqlQuery("query hostKernelInterfaceUsedIp where hostKernelInterfaceUuid='${zskernel2_pg2.uuid}'").size() == 1
        def usedIpInv = zskernel2_pg2.usedIps.get(0) as HostKernelInterfaceUsedIpInventory
        assert usedIpInv != null
        assertExtraIps(host2, [usedIpInv.ip])

        retryInSecs {
            assert cmd != null
            HostKernelInterfaceTO to = cmd.interfaces.get(0)
            assert to.interfaceName == "bond0"
            assert to.vlanId == 222

            assert to.ips.size() == 1
            assert to.ips.get(0).ipVersion == IPv6Constants.IPv4
            assert to.ips.get(0).ip == usedIpInv.ip
            assert to.ips.get(0).netmask == usedIpInv.netmask
            assert to.ips.get(0).actionCode == UsedIpTO.ACTION_CODE_ADD
        }

        deleteHostKernelInterface {
            uuid = zskernel1_pg2.uuid
        }
        assertExtraIps(host, [])

        deleteHostKernelInterface {
            uuid = zskernel2_pg2.uuid
        }
        assertExtraIps(host2, [])

        assert (queryHostKernelInterface {conditions=["uuid=${zskernel1_pg2.uuid}"]} as List<HostKernelInterfaceInventory>).isEmpty()
        assert (queryHostKernelInterface {conditions=["uuid=${zskernel2_pg2.uuid}"]} as List<HostKernelInterfaceInventory>).isEmpty()
        assert zqlQuery("query usedIp where ip='192.168.100.150' and l3NetworkUuid='${pg2.uuid}'").isEmpty()
        assert zqlQuery("query usedIp where ip='${usedIpInv.ip}' and l3NetworkUuid='${pg2.uuid}'").isEmpty()
        assert zqlQuery("query hostKernelInterfaceUsedIp where ip='192.168.100.150'").isEmpty()
        assert zqlQuery("query hostKernelInterfaceUsedIp where ip='${usedIpInv.ip}'").isEmpty()
    }

    void testCreateHostKernelInterfaceError() {
        def pg3 = createPortGroup {
            name = "pg-3"
            vSwitchUuid = vswitch.uuid
            vlan = 333
            category = L3NetworkCategory.Private
        } as PortGroupInventory

        addIpRange {
            name = "ipr-1"
            l3NetworkUuid = pg3.uuid
            startIp = "192.168.200.2"
            endIp = "192.168.200.200"
            gateway = "192.168.200.1"
            netmask = "255.255.255.0"
        }

        attachNetworkServiceToL3Network {
            l3NetworkUuid = pg3.uuid
            networkServices = netServices
        }

        def defaultSetHandler = env.getSimulator(MevocoKVMConstant.SET_KERNEL_INTERFACE_PATH)
        env.simulator(MevocoKVMConstant.SET_KERNEL_INTERFACE_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def rsp = new MevocoKVMAgentCommands.SetHostKernelInterfaceRsp()
            rsp.setError("on purpose")
            return rsp
        }

        try {
            expectApiFailure({
                createHostKernelInterface {
                    name = "kernel-failed-on-purpose"
                    hostUuid = host.uuid
                    l3NetworkUuid = pg3.uuid
                    requiredIp = "192.168.200.11"
                    netmask = "255.255.255.0"
                    trafficTypes = [MANAGEMENT_TRAFFIC_TYPE]
                }
            }) {
                assert code == OPERATION_ERROR_CODE
                assert details == String.format("failed to create hostKernelInterface[name:%s] on the host[uuid:%s], %s",
                        "kernel-failed-on-purpose", host.uuid, "on purpose")
            }
        } finally {
            env.simulator(MevocoKVMConstant.SET_KERNEL_INTERFACE_PATH, defaultSetHandler)
        }

        assert !Q.New(UsedIpVO.class).eq(UsedIpVO_.ip, "192.168.200.11").isExists()
        assertExtraIps(host, [])

        expect(ApiException.class){
            createHostKernelInterface {
                name = "kernel-with-invalid-traffic-type"
                hostUuid = host.uuid
                l3NetworkUuid = pg3.uuid
                requiredIp = "192.168.200.11"
                netmask = "255.255.255.0"
                trafficTypes = ["Tenant"]
            }
        }
        assertExtraIps(host, [])
    }

    void testCheckIpAvailability() {
        def pg4 = createPortGroup {
            name = "pg-4"
            vSwitchUuid = vswitch.uuid
            vlan = 444
            category = L3NetworkCategory.Private
            enableIPAM = false
        } as PortGroupInventory

        def zskernel_pg4 = createHostKernelInterface {
            name = "zskernel-pg4-on-host1"
            hostUuid = host.uuid
            l3NetworkUuid = pg4.uuid
            requiredIp = "44.44.44.44"
            netmask = "255.255.255.0"
            trafficTypes = [MANAGEMENT_TRAFFIC_TYPE]
        } as HostKernelInterfaceInventory

        def res = checkIpAvailability {
            l3NetworkUuid = pg4.uuid
            ip = "44.44.44.44"
        } as CheckIpAvailabilityResult
        assert res.available
        assertExtraIps(host, ["44.44.44.44"])

        deleteHostKernelInterface {
            uuid = zskernel_pg4.uuid
        }
        assertExtraIps(host, [])
    }

    void testBatchCreateHostKernelInterfaceError() {
        simpleL3 = createPortGroup {
            name = "simple-l3"
            vSwitchUuid = vswitch.uuid
            vlan = 555
            category = L3NetworkCategory.Private
            enableIPAM = false
        } as PortGroupInventory

        struct.name = "batch-kernel-on-host1"
        struct.ip = "55.55.55.55"
        struct.netmask = "255.255.255.0"

        expectApiFailure({
            batchCreateHostKernelInterface {
                structs = [struct]
                l3NetworkUuid = simpleL3.uuid
                trafficTypes = [STORAGE_TRAFFIC_TYPE]
            }
        }) {
            assert code == INVALID_ARGUMENT_ERROR_CODE
            assert details == String.format("could not batch create host kernel interface, because hostUuid in struct should be set")
        }

        struct.hostUuid = DocUtils.createFixedUuid(HostInventory.class)
        expectApiFailure({
            batchCreateHostKernelInterface {
                structs = [struct]
                l3NetworkUuid = simpleL3.uuid
                trafficTypes = [STORAGE_TRAFFIC_TYPE]
            }
        }) {
            assert code == INVALID_ARGUMENT_ERROR_CODE
            assert details == String.format("could not create host kernel interface, because host[uuid:%s] not found", struct.hostUuid)
        }

        struct.hostUuid = host.uuid
        struct.name = null
        expectApiFailure({
            batchCreateHostKernelInterface {
                structs = [struct]
                l3NetworkUuid = simpleL3.uuid
                trafficTypes = [STORAGE_TRAFFIC_TYPE]
            }
        }) {
            assert code == INVALID_ARGUMENT_ERROR_CODE
            assert details == String.format("could not create host kernel interface for host[uuid:%s], because name should be set",  struct.hostUuid)
        }

        struct.name = "batch-kernel-on-host1"
        struct.ip = null
        struct.netmask = null
        expectApiFailure({
            batchCreateHostKernelInterface {
                structs = [struct]
                l3NetworkUuid = simpleL3.uuid
                trafficTypes = [STORAGE_TRAFFIC_TYPE]
            }
        }) {
            assert code == INVALID_ARGUMENT_ERROR_CODE
            assert details == String.format("could not batch create host kernel interface, because ip cannot be null with l3Network[uuid:%s] disable IPAM",
                    simpleL3.uuid)
        }

        struct.ip = "55.55.55.55"
        expectApiFailure({
            batchCreateHostKernelInterface {
                structs = [struct]
                l3NetworkUuid = simpleL3.uuid
                trafficTypes = [STORAGE_TRAFFIC_TYPE]
            }
        }) {
            assert code == INVALID_ARGUMENT_ERROR_CODE
            assert details == String.format("netmask must be set")
        }

        struct.netmask = "255.255.255.0"
        struct2.name = "batch-kernel-on-host2"
        struct2.hostUuid = host2.uuid
        struct2.ip = "55.55.55.55"
        struct2.netmask = "255.255.255.0"
        expectApiFailure({
            batchCreateHostKernelInterface {
                structs = [struct, struct2]
                l3NetworkUuid = simpleL3.uuid
                trafficTypes = [STORAGE_TRAFFIC_TYPE]
            }
        }) {
            assert code == INVALID_ARGUMENT_ERROR_CODE
            assert details == String.format("could not batch create host kernel interface, " +
                    "because duplicate ipv4 address[%s] in input structs", struct2.getIp())
        }

        struct2.ip = "55.55.55.56"
        def defaultSetHandler = env.getSimulator(MevocoKVMConstant.SET_KERNEL_INTERFACE_PATH)
        env.simulator(MevocoKVMConstant.SET_KERNEL_INTERFACE_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(entity.body, MevocoKVMAgentCommands.SetHostKernelInterfaceCmd.class)
            if (cmd.interfaces.ips.ip.flatten().contains(struct.ip)) {
                def rsp = new MevocoKVMAgentCommands.SetHostKernelInterfaceRsp()
                rsp.setError("on purpose")
                return rsp
            }
            return defaultSetHandler(entity, spec)
        }

        BatchCreateHostKernelInterfaceResult batchResult
        try {
            batchResult = batchCreateHostKernelInterface {
                structs = [struct, struct2]
                l3NetworkUuid = simpleL3.uuid
                trafficTypes = [STORAGE_TRAFFIC_TYPE]
            } as BatchCreateHostKernelInterfaceResult
        } finally {
            env.simulator(MevocoKVMConstant.SET_KERNEL_INTERFACE_PATH, defaultSetHandler)
        }

        def results = batchResult.results as List<HostKernelInterfaceResult>
        assert results.size() == 2
        assert results.inventory.count { it != null } == 1
        assert results.error.count { it != null } == 1
        assert results.error.find {it != null}.code == OPERATION_ERROR_CODE
        assert results.error.find {it != null}.details == String.format("failed to create hostKernelInterface[name:%s] on the host[uuid:%s], %s",
                struct.name, host.uuid, "on purpose")
        assertExtraIps(host, [])
        assertExtraIps(host2, ["55.55.55.56"])

        deleteHostKernelInterface {
            uuid = results.inventory.find { it != null }.uuid
        }
        assertExtraIps(host2, [])
    }

    void testBatchCreateHostKernelInterfaceWithSimpleL3() {
        struct.description = "batch-kernel-on-host1-description"
        struct2.description = "batch-kernel-on-host2-description"
        def batchResult = batchCreateHostKernelInterface {
            structs = [struct, struct2]
            l3NetworkUuid = simpleL3.uuid
            trafficTypes = [STORAGE_TRAFFIC_TYPE]
        } as BatchCreateHostKernelInterfaceResult

        def results = batchResult.results as List<HostKernelInterfaceResult>
        assert results.size() == 2
        assert results.error.count { it != null } == 0
        def inv = results.inventory.find { it.hostUuid == host.uuid }
        assert inv.name == struct.name
        assert inv.description == struct.description
        assert inv.trafficTypes == [STORAGE_TRAFFIC_TYPE]
        assert (inv.usedIps as List<HostKernelInterfaceUsedIpInventory>).get(0).ip == struct.ip
        def inv2 = results.inventory.find { it.hostUuid == host2.uuid }
        assert inv2.name == struct2.name
        assert inv2.description == struct2.description
        assert inv2.trafficTypes == [STORAGE_TRAFFIC_TYPE]
        assert (inv2.usedIps as List<HostKernelInterfaceUsedIpInventory>).get(0).ip == struct2.ip
        assertExtraIps(host, ["55.55.55.55"])
        assertExtraIps(host2, ["55.55.55.56"])
    }

    void testBatchCreateHostKernelInterfaceWithNormalL3() {
        normalL3 = createPortGroup {
            name = "normal-l3"
            vSwitchUuid = vswitch.uuid
            vlan = 666
            category = L3NetworkCategory.Private
        } as PortGroupInventory

        addIpRange {
            name = "ipr-1"
            l3NetworkUuid = normalL3.uuid
            startIp = "192.168.101.2"
            endIp = "192.168.101.200"
            gateway = "192.168.101.1"
            netmask = "255.255.255.0"
        }

        def freeIp4s = getFreeIp {
            l3NetworkUuid = normalL3.uuid
            ipVersion = IPv6Constants.IPv4
            limit = 2
        } as List<FreeIpInventory>

        def normalStruct = new HostKernelInterfaceStruct()
        normalStruct.name = "normal-kernel-on-host1"
        normalStruct.description = "normal-kernel-on-host1-description"
        normalStruct.hostUuid = host.uuid
        normalStruct.ip = freeIp4s.get(0).ip
        normalStruct.netmask = freeIp4s.get(0).netmask
        def normalStruct2 = new HostKernelInterfaceStruct()
        normalStruct2.name = "normal-kernel-on-host2"
        normalStruct2.description = "normal-kernel-on-host2-description"
        normalStruct2.hostUuid = host2.uuid
        normalStruct2.ip = freeIp4s.get(1).ip
        def hostList = [host, host2]
        def structList = [normalStruct, normalStruct2]
        3.upto(32) {
            def num = it.intValue()

            def hostInv = addKVMHost {
                username = "root"
                password = "password"
                name = "kvm" + num
                managementIp = "127.0.0." + num
                clusterUuid = cluster.uuid
            } as KVMHostInventory
            hostList.add(hostInv)

            def struct = new HostKernelInterfaceStruct()
            struct.name = "normal-kernel-on-host" + num
            struct.description = "normal-kernel-on-host" + num + "-description"
            struct.hostUuid = hostInv.uuid
            structList.add(struct)
        }

        def batchResult = batchCreateHostKernelInterface {
            structs = structList
            l3NetworkUuid = normalL3.uuid
            trafficTypes = [MANAGEMENT_TRAFFIC_TYPE, STORAGE_TRAFFIC_TYPE]
        } as BatchCreateHostKernelInterfaceResult
        def results = batchResult.results as List<HostKernelInterfaceResult>
        assert results.size() == 32
        assert results.error.count { it != null } == 0
        assert (results.inventory.usedIps.flatten() as List<HostKernelInterfaceUsedIpInventory>).ip.unique().size() == 32

        for (KVMHostInventory host : hostList) {
            def inv = results.inventory.find { it.hostUuid == host.uuid }
            assert inv.name == "normal-kernel-on-host" + host.name.replaceAll("kvm", "")
            assert inv.description == "normal-kernel-on-host" + host.name.replaceAll("kvm", "") + "-description"
            assert [MANAGEMENT_TRAFFIC_TYPE, STORAGE_TRAFFIC_TYPE].containsAll(inv.trafficTypes)
            assert inv.usedIps.size() == 1
            List<String> expectedIps = [inv.usedIps.get(0).ip]
            if (host.uuid == this.host.uuid) {
                expectedIps.add("55.55.55.55")
            } else if (host.uuid == host2.uuid) {
                expectedIps.add("55.55.55.56")
            }
            assertExtraIps(host, expectedIps)

            if (![this.host.uuid, host2.uuid].contains(host.uuid)) {
                deleteHost {
                    uuid = host.uuid
                }
            }
        }
    }

    void testGetCandidateHostKernelInterface() {
        host3 = addKVMHost {
            username = "root"
            password = "password"
            name = "kvm3"
            managementIp = "127.0.0.3"
            clusterUuid = cluster.uuid
        } as KVMHostInventory

        def host3Kernel = createHostKernelInterface {
            name = "kernel-simple-l3-on-host3-without-traffic-type"
            hostUuid = host3.uuid
            l3NetworkUuid = simpleL3.uuid
            requiredIp = "55.55.55.57"
            netmask = "255.255.255.0"
        } as HostKernelInterfaceInventory

        host4 = addKVMHost {
            username = "root"
            password = "password"
            name = "kvm4"
            managementIp = "127.0.0.4"
            clusterUuid = cluster.uuid
        } as KVMHostInventory

        def host4Kernel = createHostKernelInterface {
            name = "kernel-simple-l3-on-host4-with-extra-traffic-type"
            hostUuid = host4.uuid
            l3NetworkUuid = simpleL3.uuid
            requiredIp = "55.55.55.58"
            netmask = "255.255.255.0"
            trafficTypes = [MANAGEMENT_TRAFFIC_TYPE, STORAGE_TRAFFIC_TYPE]
        } as HostKernelInterfaceInventory

        def hostUuidList = [host.uuid, host2.uuid, host3.uuid, host4.uuid]
        assertExtraIps(host3, ["55.55.55.57"])
        assertExtraIps(host4, ["55.55.55.58"])
        expectApiFailure({
            getCandidateHostKernelInterfaces {
                hostUuids = hostUuidList
                cidr = "55.55.55.55.0/24"
            }
        }) {
            assert code == INVALID_ARGUMENT_ERROR_CODE
            assert details == "invalid CIDR: 55.55.55.55.0/24"
        }

        expect(ApiException.class) {
            // invalid traffic type
            getCandidateHostKernelInterfaces {
                hostUuids = hostUuidList
                cidr = "55.55.55.0/24"
                trafficTypes = ["Tenant"]
            }
        }

        def results = (getCandidateHostKernelInterfaces {
            hostUuids = hostUuidList
            cidr = "55.55.55.0/24"
        } as GetCandidateHostKernelInterfacesResult).results as List<CandidateResult>
        // 4 default kernels + 4 simple l3 kernels + 2 normal l3 kernels
        assert results.size() == 10
        assert results.count { it.finalDecision == CandidateDecision.ACCEPTED} == 4
        def rejectedList =  results.findAll() { it.finalDecision == CandidateDecision.REJECTED}
        assert rejectedList.size() == 6
        rejectedList.forEach { rejected ->
            def inv = rejected.candidate as HostKernelInterfaceInventory
            def entries = rejected.decisions as List<CandidateDecisionEntry>
            if (inv.uuid != host3Kernel.uuid) {
                assert entries.size() == 1
                assert entries.get(0).reason == "no ip in cidr"
                assert entries.get(0).decisionMaker == "filter-host-kernel-interface-by-cidr"
            }
        }

        results = (getCandidateHostKernelInterfaces {
            hostUuids = hostUuidList
            cidr = "55.55.55.0/24"
            trafficTypes = [STORAGE_TRAFFIC_TYPE]
        } as GetCandidateHostKernelInterfacesResult).results as List<CandidateResult>
        assert results.size() == 10
        assert results.count { it.finalDecision == CandidateDecision.ACCEPTED} == 3
        rejectedList =  results.findAll() { it.finalDecision == CandidateDecision.REJECTED}
        assert rejectedList.size() == 7
        rejectedList.forEach { rejected ->
            def inv = rejected.candidate as HostKernelInterfaceInventory
            def entries = rejected.decisions as List<CandidateDecisionEntry>
            if (inv.uuid != host3Kernel.uuid) {
                assert entries.size() == 1
                assert entries.get(0).reason == "no ip in cidr"
                assert entries.get(0).decisionMaker == "filter-host-kernel-interface-by-cidr"
            } else {
                assert entries.size() == 1
                assert entries.get(0).reason == String.format("missing traffic type[%s]", STORAGE_TRAFFIC_TYPE)
                assert entries.get(0).decisionMaker == "filter-host-kernel-interface-by-traffic-types"
            }
        }

        results = (getCandidateHostKernelInterfaces {
            hostUuids = hostUuidList
            cidr = "55.55.55.0/24"
            trafficTypes = [STORAGE_TRAFFIC_TYPE]
            containsRejected = false
        } as GetCandidateHostKernelInterfacesResult).results as List<CandidateResult>
        assert results.size() == 3
        assert results.finalDecision.every {it == CandidateDecision.ACCEPTED}
        def invList = results.candidate as List<HostKernelInterfaceInventory>
        assert invList.hostUuid.containsAll([host.uuid, host2.uuid, host4.uuid])
        def inv = invList.find { it.hostUuid == host.uuid }
        assert inv.name == struct.name
        assert inv.description == struct.description
        assert (inv.usedIps as List<HostKernelInterfaceUsedIpInventory>).ip.every {it == struct.ip}
        assert (inv.usedIps as List<HostKernelInterfaceUsedIpInventory>).netmask.every {it == struct.netmask}
        assert inv.trafficTypes.every { it == STORAGE_TRAFFIC_TYPE }
        def inv2 = invList.find { it.hostUuid == host2.uuid }
        assert inv2.name == struct2.name
        assert inv2.description == struct2.description
        assert (inv2.usedIps as List<HostKernelInterfaceUsedIpInventory>).ip.every {it == struct2.ip}
        assert (inv2.usedIps as List<HostKernelInterfaceUsedIpInventory>).netmask.every {it == struct2.netmask}
        assert inv2.trafficTypes.every { it == STORAGE_TRAFFIC_TYPE }
        def inv3 = invList.find { it.hostUuid == host4.uuid }
        assert inv3.uuid == host4Kernel.uuid
        assert inv3.name == host4Kernel.name
        assert inv3.description == host4Kernel.description
        assert (inv3.usedIps as List<HostKernelInterfaceUsedIpInventory>).ip == (host4Kernel.usedIps as List<HostKernelInterfaceUsedIpInventory>).ip
        assert inv3.trafficTypes.size() == host4Kernel.trafficTypes.size()
        assert inv3.trafficTypes.containsAll(host4Kernel.trafficTypes)
    }

    void testZqlQueryHostKernelInterface() {
        assert !zqlQuery("query hostKernelInterface where portGroup.vSwitchUuid='${vswitch.uuid}'").isEmpty()
        assert !zqlQuery("query hostKernelInterface where portGroup.uuid='${simpleL3.uuid}'").isEmpty()
    }

    void assertExtraIps(HostInventory host, List<String> expectedIps) {
        String value = HostSystemTags.EXTRA_IPS.getTokenByResourceUuid(host.uuid, HostSystemTags.EXTRA_IPS_TOKEN)
        assert (value == null ? [] : value.tokenize(",")).toSet() == expectedIps.toSet()
        if (expectedIps.isEmpty()) {
            assert !HostSystemTags.EXTRA_IPS.hasTag(host.uuid)
        }
    }

    @Override
    void test() {
        env.create {
            cluster = env.inventoryByName("cluster") as ClusterInventory
            zone = env.inventoryByName("zone") as ZoneInventory
            host = env.inventoryByName("kvm1") as KVMHostInventory
            host2 = env.inventoryByName("kvm2") as KVMHostInventory
            bond0 = controller.getHostNetworkBonding(host.uuid, "bond0")
            assertExtraIps(host, [])
            assertExtraIps(host2, [])

            def nsProviders = queryNetworkServiceProvider {
                delegate.conditions = ["type=Flat"]
            } as List<NetworkServiceProviderInventory>
            netServices.put(nsProviders.get(0).uuid,
                    [NetworkServiceType.DHCP.toString(),
                     NetworkServiceType.DNS.toString(),
                     UserdataConstant.USERDATA_TYPE.toString()])

            testCreateHostKernelInterfaceWithSimpleL3()
            testConcurrentHostExtraIpsRefreshOnDifferentL2Networks()
            testCreateHostKernelInterfaceWithNormalL3()
            testCreateHostKernelInterfaceError()
            testCheckIpAvailability()
            testBatchCreateHostKernelInterfaceError()
            testBatchCreateHostKernelInterfaceWithSimpleL3()
            testBatchCreateHostKernelInterfaceWithNormalL3()
            testGetCandidateHostKernelInterface()
            testZqlQueryHostKernelInterface()
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
