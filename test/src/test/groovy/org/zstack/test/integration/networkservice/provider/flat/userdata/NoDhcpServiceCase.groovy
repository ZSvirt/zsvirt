package org.zstack.test.integration.networkservice.provider.flat.userdata

import org.springframework.http.HttpEntity
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.flat.FlatUserdataBackend
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.network.service.userdata.UserdataGlobalConfig
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by weiwang on 22/09/2017
 */
class NoDhcpServiceCase extends SubCase {
    EnvSpec env

    VmInstanceInventory vm
    L3NetworkInventory l3
    String userdata = "this test user data"

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
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "local"
                    url = "127.0.0.1:/nfs"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
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
        }
    }

    @Override
    void test() {
        env.create {
            l3 = (env.specByName("l3") as L3NetworkSpec).inventory
            def oldValue =  FlatUserdataBackend.UserdataReleseGC.INTERVAL

            testSetUserdataSetWhenVmOperations()

            FlatUserdataBackend.UserdataReleseGC.INTERVAL = oldValue
        }
    }


    private void testSetUserdataSetWhenVmOperations() {
        UserdataGlobalConfig.OPEN_USERDATA_SERVICE_BY_DEFAULT.updateValue(false)
        FlatUserdataBackend.ApplyUserdataCmd cmd = null

        def prepareDhcp = false
        env.afterSimulator(FlatDhcpBackend.BATCH_PREPARE_DHCP_PATH) { rsp, HttpEntity<String> e ->
            prepareDhcp = true
            return rsp
        }

        def applyDhcp = false
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            applyDhcp = true
            return rsp
        }


        env.afterSimulator(FlatUserdataBackend.APPLY_USER_DATA) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, FlatUserdataBackend.ApplyUserdataCmd.class)
            return rsp
        }

        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            imageUuid = env.inventoryByName("image").uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            hostUuid = env.inventoryByName("kvm").uuid
        }

        assert cmd == null

        migrateVm {
            delegate.vmInstanceUuid = vm.uuid
            delegate.hostUuid = env.inventoryByName("kvm1").uuid
        }

        assert prepareDhcp == false
        assert applyDhcp == false

        destroyVmInstance {
            uuid = vm.uuid
        }

        /* if there is no dhcp, no ip address, then no userdata */
        UserdataGlobalConfig.OPEN_USERDATA_SERVICE_BY_DEFAULT.updateValue(true)
        vm = createVmInstance {
            name = "vm"
            imageUuid = env.inventoryByName("image").uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            hostUuid = env.inventoryByName("kvm").uuid
        }
        assert cmd == null
    }

    @Override
    void clean() {
        env.delete()
    }

}
