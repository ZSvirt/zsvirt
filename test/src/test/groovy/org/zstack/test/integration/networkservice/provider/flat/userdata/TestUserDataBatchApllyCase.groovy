package org.zstack.test.integration.networkservice.provider.flat.userdata

import org.springframework.http.HttpEntity
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.flat.FlatUserdataBackend
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.*
import org.zstack.utils.data.SizeUnit

/**
 * Created by shixin on 18-04-19.
 */
class TestUserDataBatchApllyCase extends SubCase {
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
                        name = "l3-1"

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
                        name = "l3-2"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.110.10"
                            endIp = "192.168.110.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.110.1"
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
            testReconnectHost()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testReconnectHost() {
        L3NetworkInventory l31 = env.inventoryByName("l3-1")
        L3NetworkInventory l32 = env.inventoryByName("l3-2")
        ImageInventory image = env.inventoryByName("image")
        InstanceOfferingInventory offer = env.inventoryByName("instanceOffering")
        HostInventory host = env.inventoryByName("kvm")

        VmInstanceInventory vm1 = createVmInstance {
            name = "vm-1"
            instanceOfferingUuid = offer.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l31.uuid]
        }
        VmNicInventory nic1 = vm1.vmNics.get(0)

        VmInstanceInventory vm2 = createVmInstance {
            name = "vm-2"
            instanceOfferingUuid = offer.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l31.uuid]
        }
        VmNicInventory nic2 = vm2.vmNics.get(0)

        VmInstanceInventory vm3 = createVmInstance {
            name = "vm-3"
            instanceOfferingUuid = offer.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l32.uuid]
        }
        VmNicInventory nic3 = vm3.vmNics.get(0)

        FlatUserdataBackend.BatchApplyUserdataCmd cmd = null
        env.afterSimulator(FlatUserdataBackend.BATCH_APPLY_USER_DATA) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, FlatUserdataBackend.BatchApplyUserdataCmd.class)
            return rsp
        }

        reconnectHost {
            uuid = host.uuid
        }

        assert cmd.userdata.size() == 3
        for (FlatUserdataBackend.UserdataTO to : cmd.userdata) {
            assert to.vmIp == nic1.ip || to.vmIp == nic2.ip || to.vmIp == nic3.ip
            assert to.netmask == nic1.netmask || to.netmask == nic2.netmask || to.netmask == nic3.netmask
            assert to.l3NetworkUuid == nic1.l3NetworkUuid || to.l3NetworkUuid == nic2.l3NetworkUuid || to.l3NetworkUuid == nic3.l3NetworkUuid
        }

        FlatUserdataBackend.CleanupUserdataCmd ccmd = null
        env.afterSimulator(FlatUserdataBackend.CLEANUP_USER_DATA) { rsp, HttpEntity<String> e ->
            ccmd = json(e.body, FlatUserdataBackend.CleanupUserdataCmd.class)
            return rsp
        }

        deleteL3Network {
            uuid = l31.uuid
        }

        retryInSecs {
            assert ccmd.l3NetworkUuid == l31.uuid
        }
    }
}


