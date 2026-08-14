package org.zstack.test.integration.network.l3network

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.vm.VmNicState
import org.zstack.header.vm.VmNicVO
import org.zstack.header.vm.VmNicVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class DisableAndEnableNicFromVmCase extends SubCase{
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
            simulatorEnv()

            /**
             * 1. add ipRange on L3 and create vm
             * 2. disable vm nic
             * 3. enable vm nic
             **/
            addIpRangeAndCreateVm()
            disableAndEnableNicFromVm()
            disableAndEnableNicFromStopVm()
        }
    }

    void addIpRangeAndCreateVm() {
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")

        addIpRange {
            name = "ipr-4"
            l3NetworkUuid = pubL3.getUuid()
            startIp = "192.168.101.101"
            endIp = "192.168.101.200"
            gateway = "192.168.101.1"
            netmask = "255.255.255.0"
        }

        vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [pubL3.uuid]
        } as VmInstanceInventory
    }

    void disableAndEnableNicFromVm() {
        vm = queryVmInstance { conditions = ["name=vm"] }[0]
        vm = startVmInstance {
            uuid = vm.uuid
        } as VmInstanceInventory
        def nicUuid = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, vm.uuid).select(VmNicVO_.uuid).findValue()

        changeVmNicState {
            vmNicUuid = nicUuid
            state = VmNicState.disable.toString()
        }
        assert Q.New(VmNicVO.class).eq(VmNicVO_.uuid, nicUuid).eq(VmNicVO_.state, VmNicState.disable).count() == 1L

        changeVmNicState {
            vmNicUuid = nicUuid
            state = VmNicState.enable.toString()
        }
        assert Q.New(VmNicVO.class).eq(VmNicVO_.uuid, nicUuid).eq(VmNicVO_.state, VmNicState.enable).count() == 1L
    }

    void disableAndEnableNicFromStopVm() {
        vm = queryVmInstance { conditions = ["name=vm"] }[0]
        vm = stopVmInstance {
            uuid = vm.uuid
        } as VmInstanceInventory
        def nicUuid = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, vm.uuid).select(VmNicVO_.uuid).findValue()

        changeVmNicState {
            vmNicUuid = nicUuid
            state = VmNicState.disable.toString()
        }
        assert Q.New(VmNicVO.class).eq(VmNicVO_.uuid, nicUuid).eq(VmNicVO_.state, VmNicState.disable).count() == 1L

        changeVmNicState {
            vmNicUuid = nicUuid
            state = VmNicState.enable.toString()
        }
        assert Q.New(VmNicVO.class).eq(VmNicVO_.uuid, nicUuid).eq(VmNicVO_.state, VmNicState.enable).count() == 1L
    }

    private void simulatorEnv() {
        env.simulator(KVMConstant.KVM_CHANGE_NIC_STATE_PATH) { HttpEntity<String> e, EnvSpec espec ->
            def rsp = new KVMAgentCommands.AgentResponse()
            rsp.success = true
            return rsp
        }
    }
}
