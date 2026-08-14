package org.zstack.test.integration.network.l3network

import org.zstack.header.errorcode.SysErrors
import org.zstack.sdk.AttachL3NetworkToVmAction
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.sdk.DetachL3NetworkFromVmAction
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class DetachL3NetworkFromVmCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
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
                        name = "pubL3"
                        category = "Public"
                    }

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
        env.create {
            /**
             * 1. create vm with no ipRange L3
             * 2. add ipRange on L3 and create vm
             * 3. attach L3 with no ipRange to vm
             **/

            detachL3()
        }
    }
    void detachL3() {
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")
        L3NetworkInventory l3Inv = env.inventoryByName("l3")

        vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3Inv.uuid]
        } as VmInstanceInventory

        stopVmInstance {
            uuid = vm.uuid
        }

        //simulate null hostUuid
        SQL.New("update VmInstanceVO set hostUuid=null where uuid='${vm.uuid}'").execute()
        SQL.New("update VmInstanceVO set lastHostUuid=null where uuid='${vm.uuid}'").execute()

        DetachL3NetworkFromVmAction action = new DetachL3NetworkFromVmAction()
        action.vmNicUuid = vm.getVmNics().get(0).uuid
        action.sessionId = adminSession()
        DetachL3NetworkFromVmAction.Result ret = action.call()
        assert ret.error == null

    }
}