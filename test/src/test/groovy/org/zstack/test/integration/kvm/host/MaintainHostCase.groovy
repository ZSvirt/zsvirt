package org.zstack.test.integration.kvm.host


import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.EventCallback
import org.zstack.core.cloudbus.EventFacade
import org.zstack.core.cloudbus.EventFacadeImpl
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.allocator.AllocateHostReply
import org.zstack.header.allocator.DesignatedAllocateHostMsg
import org.zstack.header.host.HostCanonicalEvents
import org.zstack.header.host.HostState
import org.zstack.header.host.HostStateEvent
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.host.ReconnectHostMsg
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.storage.backup.PingBackupStorageMsg
import org.zstack.header.vm.StopVmInstanceMsg
import org.zstack.header.vm.StopVmInstanceReply
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.ChangeHostStateAction
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import static org.zstack.core.Platform.operr

/**
 * Created by heathhose on 17-4-7.
 */
class MaintainHostCase extends SubCase{

    EnvSpec env
    DatabaseFacade dbf
    HostInventory host
    VmInstanceInventory vm
    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url = "http://zstack.org/download/test.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
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

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING, NetworkServiceType.DHCP.toString()]
                        }

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

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        env.create {
            host = env.inventoryByName("kvm") as HostInventory
            vm = env.inventoryByName("vm") as VmInstanceInventory
            testHostOutOfMaintainStateWillReconnectHost()
            testMaintainHostWhileCreateingVm()
            testChangeHostStateRollback()
            testChangeStateIntoMaintainOneVMUnkown()
        }

    }

    void testHostOutOfMaintainStateWillReconnectHost() {
        def count = 0
        def cleanup = notifyWhenReceivedMessage(ReconnectHostMsg.class) { ReconnectHostMsg msg ->
            if (msg.hostUuid == host.uuid) {
                count ++
            }
        }

        changeHostState {
            uuid = host.uuid
            stateEvent = HostStateEvent.maintain
        }

        changeHostState {
            uuid = host.uuid
            stateEvent = HostStateEvent.enable
        }

        assert count == 1

        cleanup()

        retryInSecs {
            assert Q.New(HostVO.class).eq(HostVO_.uuid, host.uuid).select(HostVO_.status).findValue() == HostStatus.Connected
        }
    }

    void testMaintainHostWhileCreateingVm(){
        ImageInventory img = env.inventoryByName("image1") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        InstanceOfferingInventory instance = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        boolean success

        env.message(DesignatedAllocateHostMsg){DesignatedAllocateHostMsg msg, CloudBus bus ->
            AllocateHostReply r = new AllocateHostReply()
            r.setHost(org.zstack.header.host.HostInventory.valueOf(Q.New(HostVO.class).eq(HostVO_.uuid, host.uuid).find() as HostVO))

            Thread.start {
                changeHostState {
                    uuid = host.uuid
                    stateEvent = HostStateEvent.maintain
                }
                success = true
            }

            retryInSecs{
                // maintain host will stopVm first, then StopVmMsg will queue up behind CreateVmMsg
                assert Q.New(HostVO.class).eq(HostVO_.uuid, host.uuid).select(HostVO_.state).findValue() == HostState.PreMaintenance
            }

            bus.reply(msg, r)
        }

        def a = new CreateVmInstanceAction()
        a.name = "testVm"
        a.imageUuid = img.uuid
        a.instanceOfferingUuid = instance.uuid
        a.l3NetworkUuids = [l3.uuid]
        a.sessionId = adminSession()

        assert a.call().error != null
        retryInSecs{
            assert success
        }
        env.cleanMessageHandlers()
        recoverEnviroment()
    }

    void testChangeHostStateRollback(){
        assert HostState.Enabled.getTargetStateDrivenEvent(HostState.PreMaintenance) == HostStateEvent.preMaintain
        assert HostState.Enabled.getTargetStateDrivenEvent(HostState.Maintenance) == null

        env.message(StopVmInstanceMsg){ StopVmInstanceMsg msg, CloudBus bus ->
            def r = new StopVmInstanceReply()
            r.setError(operr("on purpose"))
            bus.reply(msg, r)
        }

        changeHostState {
            uuid = host.uuid
            stateEvent = HostStateEvent.disable
        }

        ChangeHostStateAction action = new ChangeHostStateAction()
        action.uuid = host.uuid
        action.stateEvent = HostStateEvent.maintain
        action.sessionId = adminSession()
        assert action.call().error != null
        assert Q.New(HostVO.class).eq(HostVO_.uuid, host.uuid).select(HostVO_.state).findValue() == HostState.Disabled
        
        env.cleanMessageHandlers()
        recoverEnviroment()
    }

    void testChangeStateIntoMaintainOneVMUnkown(){
        assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid).select(VmInstanceVO_.state).findValue() == VmInstanceState.Running

        env.simulator(KVMConstant.KVM_CONNECT_PATH){HttpEntity<String> entity,EnvSpec spec ->
            def rsp = new KVMAgentCommands.ConnectResponse()
            rsp.success = false
            return rsp
        }

        expect(AssertionError.class){
            reconnectHost {
                uuid = host.uuid
            }
        }
        retryInSecs {
            HostVO hvo = dbf.findByUuid(host.getUuid(), HostVO.class)
            assert HostStatus.Disconnected == hvo.getStatus()
            VmInstanceVO vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class)
            assert VmInstanceState.Unknown == vmvo.getState()
        }

        ChangeHostStateAction action = new ChangeHostStateAction()
        action.uuid = host.uuid
        action.stateEvent = HostStateEvent.maintain
        action.sessionId = adminSession()
        ChangeHostStateAction.Result result = action.call()
        assert result.error != null
        assert dbFindByUuid(host.uuid,HostVO.class).state == HostState.Enabled
    }

    private recoverEnviroment(){
        HostState originState = Q.New(HostVO.class).eq(HostVO_.uuid, host.uuid).select(HostVO_.state).findValue()
        if (originState == HostState.Maintenance){
            boolean connected = false
            EventFacade evtf = bean(EventFacadeImpl.class)
            evtf.on(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, new EventCallback() {
                @Override
                protected void run(Map tokens, Object data) {
                    HostCanonicalEvents.HostStatusChangedData d = (HostCanonicalEvents.HostStatusChangedData) data
                    if (d.newStatus == HostStatus.Connected.toString()) {
                        connected = true
                    }
                }
            })
            changeHostState {
                uuid = host.uuid
                stateEvent = HostStateEvent.enable
            }

            retryInSecs{
                assert connected
            }
        } else {
            changeHostState {
                uuid = host.uuid
                stateEvent = HostStateEvent.enable
            }
        }

        if (Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid).select(VmInstanceVO_.state).findValue() == VmInstanceState.Stopped){
            startVmInstance {
                uuid = vm.uuid
            }
        }
    }
    
    @Override
    void clean() {
        env.delete()
    }
}
