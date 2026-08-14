package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmNicManager
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit


class ChangeVmNicFailedCase extends SubCase {
    EnvSpec env
    VmNicManager nicMgr

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
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
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
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
                        category = "Public"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }

                    l3Network {
                        name = "l3-without-ip-range"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }
                    }
                }

                virtualRouterOffering {
                    name = "vr"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        nicMgr = bean(VmNicManager.class)
        env.create {
            testChangeNicFail()
        }
    }

    /* We do not allow create vm in network which system = true,
     * but in network which system = false, category = Public is allowed
     */
    void testChangeNicFail() {
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")
        L3NetworkInventory l3 = env.inventoryByName("l3")

        VmInstanceInventory vm = createVmInstance {
            name = "test"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [pubL3.uuid]
        }

        VmNicInventory nic = vm.vmNics.get(0)
        env.simulator(KVMConstant.KVM_UPDATE_NIC_PATH) { HttpEntity<String> e, EnvSpec espec ->
            KVMAgentCommands.UpdateNicRsp rsp = new KVMAgentCommands.UpdateNicRsp()
            rsp.setSuccess(false)
            rsp.setError("on purpose")
            return rsp
        }

        expectError {
            changeVmNicNetwork {
                vmNicUuid = nic.uuid
                destL3NetworkUuid = l3.uuid
            }
        }
        vm = queryVmInstance { conditions = ["uuid=${vm.uuid}"] }[0]
        VmNicInventory nic1 = vm.vmNics.get(0)

        assert nic1.l3NetworkUuid == nic.l3NetworkUuid
        assert nic1.usedIps.size() == 1
        assert nic1.ip == nic.ip
        assert nic1.getUsedIps().get(0).uuid == nic.getUsedIps().get(0).uuid

        env.simulator(KVMConstant.KVM_UPDATE_NIC_PATH) { HttpEntity<String> e, EnvSpec espec ->
            KVMAgentCommands.UpdateNicRsp rsp = new KVMAgentCommands.UpdateNicRsp()
            return rsp
        }

        def l3WithoutIpRange = env.inventoryByName("l3-without-ip-range") as L3NetworkInventory
        def vmNic = vm.getVmNics().get(0) as VmNicInventory
        expectError {
            changeVmNicNetwork {
                vmNicUuid = vmNic.uuid
                destL3NetworkUuid = l3WithoutIpRange.uuid
            }
        }

        deleteL3Network {
            uuid = pubL3.uuid
        }
        vm = queryVmInstance { conditions = ["uuid=${vm.uuid}"] }[0]
        assert vm.getVmNics().size() == 0
    }
}
