package org.zstack.test.integration.network.l2network.cascade

import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by heathhose on 17-3-23.
 */
class DoubleL2NetworksLocalStorageCase extends SubCase{
    def DOC = """
use:
1. create vm which has two l3 on two l2
2. detach one l2
3. attach l2 to another
4. start vm
5. confirm vm start in former cluster
"""

    EnvSpec env
    VmInstanceInventory vmi
    L2NetworkInventory l2i1
    ClusterInventory cluster1
    ClusterInventory cluster2

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
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
                    url  = "http://zstack.org/download/test.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
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
                    attachL2Network("l2-1")
                    attachL2Network("l2-2")
                }

                cluster{
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm3"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm4"
                        managementIp = "127.0.0.4"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2-1"
                    physicalInterface = "eth1"

                    l3Network {
                        name = "l3-1"

                        ip {
                            startIp = "10.101.10.10"
                            endIp = "10.101.10.100"
                            netmask = "255.255.255.0"
                            gateway = "10.101.10.1"
                        }
                    }
                }

                l2NoVlanNetwork {
                    name = "l2-2"
                    physicalInterface = "eth2"

                    l3Network {
                        name = "l3-2"

                        ip {
                            startIp = "10.101.20.10"
                            endIp = "10.101.20.100"
                            netmask = "255.255.255.0"
                            gateway = "10.101.20.1"
                        }
                    }
                }
                
                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useDefaultL3Network("l3-1")
                useL3Networks("l3-1","l3-2")
                useRootDiskOffering("diskOffering")
                useHost("kvm1")
                useCluster("cluster1")
            }
        }
    }

    @Override
    void test() {
        env.create {
            vmi = env.inventoryByName("vm")
            l2i1 = env.inventoryByName("l2-1")
            cluster1 = env.inventoryByName("cluster1")
            cluster2 = env.inventoryByName("cluster2")

            detachL2AndCheckVMState()
            attachL2ToAnotherClusterAndCheckVmCluster()
        }
    }

    void detachL2AndCheckVMState(){
        VmInstanceVO vmvo = dbFindByUuid(vmi.uuid,VmInstanceVO.class)
        int nicNumber = vmvo.getVmNics().size() 
        assert nicNumber >= 1

        env.simulator(KVMConstant.KVM_DETACH_NIC_PATH) {
            def rsp = new KVMAgentCommands.DetachNicRsp()
            rsp.setError("NIC device is still attached after 10 seconds. Please check virtio driver or stop VM and detach again.")
            return rsp
        }

        detachL2NetworkFromCluster {
            l2NetworkUuid = l2i1.uuid
            clusterUuid = cluster1.uuid
        }

        vmvo = dbFindByUuid(vmi.uuid,VmInstanceVO.class)
        assert VmInstanceState.Running == vmvo.getState()
        assert nicNumber > vmvo.getVmNics().size()
    }

    void attachL2ToAnotherClusterAndCheckVmCluster(){
        attachL2NetworkToCluster {
            l2NetworkUuid = l2i1.uuid
            clusterUuid = cluster2.uuid
        }
        rebootVmInstance {
            uuid = vmi.uuid
        }

        VmInstanceVO vmvo = dbFindByUuid(vmi.uuid,VmInstanceVO.class)
        assert VmInstanceState.Running ==  vmvo.getState()
        assert cluster1.getUuid() == vmvo.getClusterUuid()
    }

    @Override
    void clean() {
        env.delete()
    }
}