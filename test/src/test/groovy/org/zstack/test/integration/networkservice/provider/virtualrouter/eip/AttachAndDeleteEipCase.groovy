package org.zstack.test.integration.networkservice.provider.virtualrouter.eip

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.core.db.SQLBatch
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.header.volume.VolumeType
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.portforwarding.PortForwardingRuleVO
import org.zstack.network.service.portforwarding.PortForwardingRuleVO_
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.EipInventory
import org.zstack.network.service.eip.EipVO
import org.zstack.network.service.eip.EipVO_
import org.zstack.sdk.HostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PortForwardingRuleInventory
import org.zstack.sdk.VipInventory
import org.zstack.network.service.vip.VipVO
import org.zstack.network.service.vip.VipVO_
import org.zstack.sdk.VirtualRouterVmInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by MaJin on 2017-04-08.
 */
class AttachAndDeleteEipCase extends SubCase{
    EnvSpec env
    VmInstanceInventory vm
    Boolean process = false

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
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
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
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
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                eip {
                    name = "eip"
                    useVip("pubL3")
                }

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }
            }

            vm {
                name = "vm"
                useImage("image")
                useHost("kvm1")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void test() {
        env.create {
            // if you are going to modify it, better to confirm with others
            assert EipConstant.noNeedApplyOnBackendVmStates.containsAll([VmInstanceState.Stopped, VmInstanceState.VolumeMigrating])
            assert EipConstant.attachableVmStates.containsAll(EipConstant.noNeedApplyOnBackendVmStates)
            assert EipConstant.attachableVmStates.containsAll([VmInstanceState.Running,
                                                               VmInstanceState.Paused,
                                                               VmInstanceState.Pausing,
                                                               VmInstanceState.Resuming,
                                                               VmInstanceState.Stopped,
                                                               VmInstanceState.VolumeMigrating])

            vm = env.inventoryByName("vm") as VmInstanceInventory
            testAttachAndDeleteEipWhenVmRunning()
            env.recreate("eip")
            testAttachAndDeleteEipWhenVmPausing()
            env.recreate("eip")
            testAttachAndDeleteEipWhenVmPaused()
            env.recreate("eip")
            testAttachAndDeleteEipWhenVmStopped()
            env.recreate("eip")
            testAttachAndDeleteEipWhenVmVolumeMigrating()
            env.recreate("eip")

            startVmInstance {
                uuid = vm.uuid
            }
            testCreatePortForwarding()
            env.recreate("eip")
            testVipInVirtualRouterInv()

            testDeleteEipAfterTheVmDestroyed()
        }
    }

    private void testAttachEipToVmAndDeleteEip(VmInstanceState state){
        EipInventory eipInv = env.inventoryByName("eip") as EipInventory
        boolean called = false
        env.simulator(VirtualRouterConstant.VR_CREATE_EIP) {
            called = true
            return new VirtualRouterCommands.CreateEipRsp()
        }

        attachEip {
            eipUuid = eipInv.uuid
            vmNicUuid = vm.vmNics.get(0).uuid
        }

        assert EipConstant.noNeedApplyOnBackendVmStates.contains(state) && !called ||
                !EipConstant.noNeedApplyOnBackendVmStates.contains(state) &&
                EipConstant.attachableVmStates.contains(state) && called

        called = false
        env.simulator(VirtualRouterConstant.VR_REMOVE_EIP) {
            called = true
            return new VirtualRouterCommands.RemoveEipRsp()
        }

        testDeleteEipAction()
        assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid).select(VmInstanceVO_.state).findValue() == state

        testDeleteVipAction(eipInv.vipUuid)
        assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid).select(VmInstanceVO_.state).findValue() == state
        assert !Q.New(EipVO.class).eq(EipVO_.uuid, eipInv.uuid).isExists()
        assert EipConstant.noNeedApplyOnBackendVmStates.contains(state) && !called ||
                !EipConstant.noNeedApplyOnBackendVmStates.contains(state) &&
                EipConstant.attachableVmStates.contains(state) && called
    }

    void testAttachAndDeleteEipWhenVmRunning(){
        testAttachEipToVmAndDeleteEip(VmInstanceState.Running)
    }

    void testAttachAndDeleteEipWhenVmPausing(){
        /*process = false

        env.simulator(KVMConstant.KVM_PAUSE_VM_PATH) {
            retryInSecs{
                assert process
            }
            return new KVMAgentCommands.PauseVmResponse()
        }

        def thread = Thread.start {
            pauseVmInstance {
                uuid = vm.uuid
            }
        }

        retryInSecs(3, 1){
            assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid)
                    .eq(VmInstanceVO_.state, VmInstanceState.Pausing).exists
        }
        testAttachEipToVmAndDeleteEip(VmInstanceState.Pausing)

        process = true
        thread.join()
        env.cleanSimulatorHandlers()
        */
        pauseVmInstance {
            uuid = vm.uuid
        }
    }

    void testAttachAndDeleteEipWhenVmPaused(){
        retryInSecs(3, 1){
            assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid)
                    .eq(VmInstanceVO_.state, VmInstanceState.Paused).exists
        }
        testAttachEipToVmAndDeleteEip(VmInstanceState.Paused)
    }

    void testAttachAndDeleteEipWhenVmStopped(){
        stopVmInstance {
            uuid = vm.uuid
        }
        testAttachEipToVmAndDeleteEip(VmInstanceState.Stopped)
    }

    void testAttachAndDeleteEipWhenVmVolumeMigrating(){
        HostInventory host2 = env.inventoryByName("kvm2") as HostInventory
        process = false

        env.simulator(LocalStorageKvmSftpBackupStorageMediatorImpl.DOWNLOAD_BIT_PATH) {
            while (!process) {
                Thread.sleep(100)
            }

            return new LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsRsp()
        }

        def thread = Thread.start {
            localStorageMigrateVolume {
                volumeUuid = vm.allVolumes.stream().filter{ volume ->
                    volume.type == VolumeType.Root.toString()
                }.findFirst().get().uuid
                destHostUuid = host2.uuid
            }
        }

        retryInSecs(5, 1){
            assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid)
                    .eq(VmInstanceVO_.state, VmInstanceState.VolumeMigrating).exists
        }
        testAttachEipToVmAndDeleteEip(VmInstanceState.VolumeMigrating)
        process = true
        thread.join()

        retryInSecs(5, 1){
            assert !Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid)
                    .eq(VmInstanceVO_.state, VmInstanceState.VolumeMigrating).exists
        }
        env.cleanSimulatorHandlers()
    }

    void testCreatePortForwarding(){
        VipInventory vipInv = createVip {
            name = "vip"
            l3NetworkUuid = (env.inventoryByName("pubL3") as L3NetworkInventory).uuid
            sessionId = currentEnvSpec.session.uuid
        } as VipInventory

        PortForwardingRuleInventory port =  createPortForwardingRule {
            vipUuid = vipInv.uuid
            vipPortStart = 21L
            protocolType = "UDP"
            name = "port"
            sessionId = currentEnvSpec.session.uuid
        } as PortForwardingRuleInventory

        attachPortForwardingRule {
            vmNicUuid = vm.vmNics.get(0).uuid
            ruleUuid = port.uuid
            sessionId = currentEnvSpec.session.uuid
        }

        testDeleteVipAction(vipInv.uuid)
        assert !Q.New(PortForwardingRuleVO.class).eq(PortForwardingRuleVO_.uuid, port.uuid).isExists()
        assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid).select(VmInstanceVO_.state).findValue() == VmInstanceState.Running
    }

    void testVipInVirtualRouterInv() {
        VirtualRouterVmInventory inv = queryVirtualRouterVm {}[0]
        assert inv.virtualRouterVips != null
    }

    void testDeleteEipAfterTheVmDestroyed() {
        EipInventory eipInv = env.inventoryByName("eip") as EipInventory
        attachEip {
            eipUuid = eipInv.uuid
            vmNicUuid = vm.vmNics.get(0).uuid
        }

        destroyVmInstance {
            uuid = vm.getUuid()
        }

        boolean called = false
        env.afterSimulator(VirtualRouterConstant.VR_REMOVE_EIP) { rsp, HttpEntity<String> entity ->
            called = true
            return rsp
        }

        deleteEip {
            uuid = eipInv.getUuid()
        }

        assert !dbIsExists(eipInv.getUuid(), EipVO.class)
        assert !called

        env.cleanAfterSimulatorHandlers()
    }

    private void testDeleteVipAction(String vipUuid){
        VipVO vip = dbFindByUuid(vipUuid, VipVO.class)

        deleteVip{
            uuid = UUID.randomUUID().toString().replace("-", "")
        }

        deleteVip{
            uuid = vipUuid
        }

        assert !Q.New(VipVO.class).eq(VipVO_.uuid, vipUuid).isExists()
        retryInSecs(){
            assert !Q.New(UsedIpVO.class).eq(UsedIpVO_.uuid, vip.usedIpUuid).isExists()
        }

    }

    private void testDeleteEipAction(){
        EipInventory eipInv = env.inventoryByName("eip") as EipInventory
        VipVO vipVO = dbFindByUuid(eipInv.vipUuid, VipVO.class)

        deleteEip{
            uuid = UUID.randomUUID().toString().replace("-", "")
        }
        deleteEip {
            uuid = eipInv.uuid
        }

        new SQLBatch(){
            @Override
            protected void scripts() {
                assert !q(EipVO.class).eq(EipVO_.uuid, eipInv.uuid).isExists()
                assert q(VipVO.class).eq(VipVO_.uuid, vipVO.uuid).isExists()
                assert q(UsedIpVO.class).eq(UsedIpVO_.uuid, vipVO.usedIpUuid).isExists()
            }
        }.execute()
    }
}
