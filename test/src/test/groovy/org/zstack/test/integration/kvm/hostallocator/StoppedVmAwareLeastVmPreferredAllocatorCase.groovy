package org.zstack.test.integration.kvm.hostallocator

import org.zstack.header.allocator.HostAllocatorConstant
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.sdk.Completion
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.*
import org.zstack.utils.data.SizeUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


/**
 * Created by lining on 2018/09/27.
 */
class StoppedVmAwareLeastVmPreferredAllocatorCase extends SubCase {
    EnvSpec env
    String image
    String instOffering
    String l3uuid
    HostInventory host1, host2, host3

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
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 10
                allocatorStrategy = HostAllocatorConstant.STOPPED_VM_AWARE_LEAST_VM_PREFERRED_HOST_ALLOCATOR_STRATEGY_TYPE
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
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"

                        totalCpu = 3
                        totalMem = SizeUnit.GIGABYTE.toByte(3)
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"

                        totalCpu = 3
                        totalMem = SizeUnit.GIGABYTE.toByte(3)
                    }

                    kvm {
                        name = "kvm3"
                        managementIp = "127.0.0.4"
                        username = "root"
                        password = "password"

                        totalCpu = 3
                        totalMem = SizeUnit.GIGABYTE.toByte(3)
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "/nfs_root"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
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
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
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
            image = (env.specByName("image1") as ImageSpec).inventory.uuid
            instOffering = (env.specByName("instanceOffering") as InstanceOfferingSpec).inventory.uuid
            l3uuid = (env.specByName("pubL3") as L3NetworkSpec).inventory.uuid
            host1 = env.inventoryByName("kvm1") as HostInventory
            host2 = env.inventoryByName("kvm2") as HostInventory
            host3 = env.inventoryByName("kvm3") as HostInventory

            KVMGlobalConfig.RESERVED_CPU_CAPACITY.updateValue(0)
            KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.updateValue("0G")

            createStoppedVm()
            createRunningVm()
            batchCreateStoppedVm()
        }
    }

    void createStoppedVm() {
        List<String> vmUuids = []

        for (int i = 0; i < 9; i++) {
            VmInstanceInventory vm = createVmInstance {
                name = "stopped-vm"
                instanceOfferingUuid = instOffering
                l3NetworkUuids = [l3uuid]
                imageUuid = image
                strategy = "CreateStopped"
            }
            vmUuids.add(vm.getUuid())
        }

        for (String vmUuid : vmUuids) {
            startVmInstance {
                uuid = vmUuid
            }
        }

        for (String vmUuid : vmUuids) {
            stopVmInstance {
                uuid = vmUuid
            }
        }
    }

    void createRunningVm() {
        List<String> vmUuids = []

        for (int i = 0; i < 9; i++) {
            VmInstanceInventory vm = createVmInstance {
                name = "running-vm"
                instanceOfferingUuid = instOffering
                l3NetworkUuids = [l3uuid]
                imageUuid = image
            }
            vmUuids.add(vm.getUuid())
        }

        for (String vmUuid : vmUuids) {
            stopVmInstance {
                uuid = vmUuid
            }
        }
    }

    void batchCreateStoppedVm() {
        List<String> vmUuids = Collections.synchronizedList(new LinkedList())

        int errorNum = 0
        CountDownLatch latch = new CountDownLatch(9)
        for (int i = 0; i < 9; i++) {
            CreateVmInstanceAction action = new CreateVmInstanceAction(
                    name : "batch-vm-" + i,
                    instanceOfferingUuid : instOffering,
                    l3NetworkUuids : [l3uuid],
                    imageUuid : image,
                    strategy: "CreateStopped",
                    sessionId: Test.currentEnvSpec.session.uuid
            )

            action.call(new Completion<CreateVmInstanceAction.Result>() {
                @Override
                void complete(CreateVmInstanceAction.Result ret) {
                    if(ret.error != null){
                        errorNum ++
                    } else {
                        vmUuids.add(ret.value.inventory.uuid)
                    }

                    latch.countDown()
                }
            })
        }

        latch.await(10, TimeUnit.SECONDS)
        assert errorNum == 0
        assert vmUuids.size() == 9

        for (String vmUuid : vmUuids) {
            startVmInstance {
                uuid = vmUuid
            }
        }
    }
}
