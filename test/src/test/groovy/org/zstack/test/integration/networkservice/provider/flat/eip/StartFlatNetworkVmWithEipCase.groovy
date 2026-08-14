package org.zstack.test.integration.networkservice.provider.flat.eip

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatEipBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.network.service.vip.StopVipMsg
import org.zstack.network.service.vip.StopVipReply
import org.zstack.sdk.EipInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import static org.zstack.core.Platform.operr

/**
 * Created by weiwang on 04/12/2017
 */
class StartFlatNetworkVmWithEipCase extends SubCase {

    EnvSpec env
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

                    kvm {
                        name = "kvm2"
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

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
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
                eip {
                    name = "eip-1"
                    useVip("pubL3")
                }
            }

            vm {
                name = "vm"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }
            vm {
                name = "vm-1"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useHost("kvm")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testStartVmWithEip()
            testRecoverVmWithEip()
            testRecoverVmWithEipWithError()
            testMigrateVmWithEip()
        }
    }

    void testStartVmWithEip(){
        def eip = env.inventoryByName("eip") as EipInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory

        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = vm.vmNics[0].uuid
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        startVmInstance {
            uuid = vm.uuid
        }
    }

    void testRecoverVmWithEip(){
        def eip = env.inventoryByName("eip-1") as EipInventory
        def vm = env.inventoryByName("vm-1") as VmInstanceInventory

        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = vm.vmNics[0].uuid
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        destroyVmInstance {
            uuid = vm.uuid
        }

        EipInventory eip1 = queryEip { conditions=["name=eip-1"] }[0]
        assert eip1.vmNicUuid == null
        assert eip1.guestIp == null

        recoverVmInstance {
            uuid = vm.uuid
        }

        startVmInstance {
            uuid = vm.uuid
        }
    }

    void testRecoverVmWithEipWithError(){
        def eip = env.inventoryByName("eip-1") as EipInventory
        def vm = env.inventoryByName("vm-1") as VmInstanceInventory

        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = vm.vmNics[0].uuid
        }

        env.message(StopVipMsg.class) { StopVipMsg msg, CloudBus bus ->
            def reply = new StopVipReply()
            reply.setError(operr("on purpose"))
            bus.reply(msg, reply)
        }

        destroyVmInstance {
            uuid = vm.uuid
        }

        EipInventory eip1 = queryEip { conditions=["name=eip-1"] }[0]
        assert eip1.vmNicUuid == null
        assert eip1.guestIp == null

        recoverVmInstance {
            uuid = vm.uuid
        }

        startVmInstance {
            uuid = vm.uuid
        }
    }

    void testMigrateVmWithEip() {
        def eip = env.inventoryByName("eip-1") as EipInventory
        def vm = env.inventoryByName("vm-1") as VmInstanceInventory
        def host1 = env.inventoryByName("kvm") as HostInventory
        def host2 = env.inventoryByName("kvm2") as HostInventory

        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = vm.vmNics[0].uuid
        }

        boolean deleteEipOnSrcHostFailed = false
        env.afterSimulator(FlatEipBackend.BATCH_DELETE_EIP_PATH) {
            deleteEipOnSrcHostFailed = true
            throw new HttpError(403, "on purpose")
        }

        boolean applyEipOnDestHostSuccessed = false
        env.afterSimulator(FlatEipBackend.BATCH_APPLY_EIP_PATH) { rsp, HttpEntity<String> e ->
            applyEipOnDestHostSuccessed = true
            return rsp
        }

        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = host2.uuid
        }

        retryInSecs {
            assert deleteEipOnSrcHostFailed == true
            assert applyEipOnDestHostSuccessed == true
        }

        applyEipOnDestHostSuccessed = false
        env.afterSimulator(FlatEipBackend.BATCH_DELETE_EIP_PATH) { rsp, HttpEntity<String> e ->
            deleteEipOnSrcHostFailed = false
            return rsp
        }

        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = host1.uuid
        }

        retryInSecs {
            assert deleteEipOnSrcHostFailed == false
            assert applyEipOnDestHostSuccessed == true
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
