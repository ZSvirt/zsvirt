package org.zstack.test.integration.kvm.globalconfig

import org.zstack.core.db.Q
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.GetCpuMemoryCapacityAction
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.*
import org.zstack.utils.data.SizeUnit

/**
 * Created by camile on 2017/4.
 */
class BatchCreateVmWhenSetReservedMemoryCase extends SubCase {
    EnvSpec env
    int correctCount = 10

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
                cpu = 1
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
                        name = "kvm1"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(10)
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
            testOrderCreateVms(10)
            testConcurrentCreateVMs(10)
        }
    }
    void testOrderCreateVms(Long num){
        def thisImageUuid = (env.specByName("image1") as ImageSpec).inventory.uuid
        def _1CPU1G = (env.specByName("instanceOffering") as InstanceOfferingSpec).inventory.uuid
        def l3uuid = (env.specByName("pubL3") as L3NetworkSpec).inventory.uuid

        for (int i = 0; i < num; i++) {
            createVmInstance {
                def vmName = "VM"+i
                name = vmName
                instanceOfferingUuid = _1CPU1G
                imageUuid = thisImageUuid
                l3NetworkUuids = [l3uuid]
            }
        }
        assert correctCount == Q.New(VmInstanceVO.class).select(VmInstanceVO_.uuid).eq(VmInstanceVO_.state, VmInstanceState.Running).listValues().size()
        expect(AssertionError.class) {
            createVmInstance {
                name = "VMM"
                instanceOfferingUuid = _1CPU1G
                imageUuid = thisImageUuid
                l3NetworkUuids = [l3uuid]
            }
        }
        List<String> vmUuids = Q.New(VmInstanceVO.class).select(VmInstanceVO_.uuid).eq(VmInstanceVO_.state, VmInstanceState.Running).listValues()
        assert vmUuids.size() == correctCount
        //free resource
        for (String vmUuid : vmUuids) {
            destroyVmInstance {
                uuid = vmUuid
            }
        }
    }

    void testConcurrentCreateVMs(Long num) {
        def thisImageUuid = (env.specByName("image1") as ImageSpec).inventory.uuid
        def _1CPU1G = (env.specByName("instanceOffering") as InstanceOfferingSpec).inventory.uuid
        def l3uuid = (env.specByName("pubL3") as L3NetworkSpec).inventory.uuid

        def threads = []
        1.upto(num, {
            def vmName = "VM-${it}".toString()
            def thread = Thread.start {
                createVmInstance {
                    name = vmName
                    instanceOfferingUuid = _1CPU1G
                    imageUuid = thisImageUuid
                    l3NetworkUuids = [l3uuid]
                }
            }
            threads.add(thread)
        })

        threads.each { it.join() }

        assert correctCount  == Q.New(VmInstanceVO.class).select(VmInstanceVO_.uuid).eq(VmInstanceVO_.state, VmInstanceState.Running).listValues().size()

        GetCpuMemoryCapacityAction getCpuMemoryCapacityAction = new GetCpuMemoryCapacityAction()
        getCpuMemoryCapacityAction.all = true
        getCpuMemoryCapacityAction.sessionId = adminSession()
        GetCpuMemoryCapacityAction.Result res = getCpuMemoryCapacityAction.call()
        assert res.error == null
        assert res.value.availableMemory == SizeUnit.GIGABYTE.toByte(0)

        CreateVmInstanceAction createVmInstanceAction = new CreateVmInstanceAction()
        createVmInstanceAction.name = "VMM"
        createVmInstanceAction.instanceOfferingUuid = _1CPU1G
        createVmInstanceAction.imageUuid = thisImageUuid
        createVmInstanceAction.l3NetworkUuids = [l3uuid]
        createVmInstanceAction.sessionId = adminSession()
        CreateVmInstanceAction.Result res2 = createVmInstanceAction.call()
        assert res2.error !=null
    }
}
