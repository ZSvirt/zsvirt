package org.zstack.test.integration.storage.ceph

import org.springframework.http.HttpEntity
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.ReconnectHostAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by AlanJager on 2017/6/20.
 */
class VmStateSyncCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }
            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }
            zone{
                name = "zone"
                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }

                    attachPrimaryStorage("ps")
                    attachL2Network("l2")
                }
                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

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

                cephPrimaryStorage {
                    name="ps"
                    description="Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                    url="ceph://pri"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls=["root:password@localhost/?monPort=7777"]

                }


                attachBackupStorage("bs")
            }

            cephBackupStorage {
                name="bs"
                description="Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid ="7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "test-iso"
                    url  = "http://zstack.org/download/test.iso"
                }
                image {
                    name = "image"
                    url  = "http://zstack.org/download/image.qcow2"
                }
            }

            vm {
                name = "vm"
                useCluster("cluster")
                useHost("host")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useRootDiskOffering("diskOffering")
                useImage("image")

            }
        }
    }

    @Override
    void test() {
        env.create {
            testDestroyVmAndRecoverItTheStateOfVmIsStopped()
        }
    }

    void testDestroyVmAndRecoverItTheStateOfVmIsStopped() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        String hostUuid = vm.getHostUuid()

        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { KVMAgentCommands.ConnectResponse rsp, HttpEntity<String> e ->
            rsp.success = false
            rsp.error = "failed"
            return rsp
        }
        // mock connect fail
        ReconnectHostAction action = new ReconnectHostAction()
        action.uuid = hostUuid
        action.sessionId = adminSession()
        ReconnectHostAction.Result ret = action.call()
        assert ret.error != null

        HostVO hostVO
        VmInstanceVO vmVO

        retryInSecs {
            hostVO = dbFindByUuid(hostUuid, HostVO.class)
            vmVO = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)

            assert hostVO.status == HostStatus.Disconnected
            assert vmVO.state == VmInstanceState.Unknown
        }

        destroyVmInstance {
            uuid = vm.getUuid()
        }

        recoverVmInstance {
            uuid = vm.getUuid()
        }

        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { KVMAgentCommands.ConnectResponse rsp, HttpEntity<String> e ->
            rsp.success = true
            return rsp
        }

        reconnectHost {
            uuid = hostUuid
        }

        retryInSecs {
            vmVO = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)
            assert vmVO.getHostUuid() == null
        }

        retryInSecs {
            hostVO = dbFindByUuid(hostUuid, HostVO.class)
            vmVO = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)

            assert hostVO.status == HostStatus.Connected
            assert vmVO.state == VmInstanceState.Stopped
        }
    }
}
