package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.header.host.CheckVmStateOnHypervisorMsg
import org.zstack.header.host.CheckVmStateOnHypervisorReply
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.header.volume.VolumeStatus
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.ExpungeVmInstanceAction
import org.zstack.sdk.DestroyVmInstanceAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
/**
 * Created by AlanJager on 2017/4/24.
 */
class VmDestroyingCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm
    ErrorFacade errf
    KVMAgentCommands.DestroyVmCmd cmd = null

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
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

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

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            vm = env.inventoryByName("vm") as VmInstanceInventory
            errf = bean(ErrorFacade.class)
            simulatorEnv()
            testFailedDestroyPreservesResourcesUntilSuccessfulRetry()
            testRecover()
            testFailedDestroyPreservesResourcesUntilSuccessfulRetry()
            testExpunge()
        }
    }

    void simulatorEnv() {
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { KVMAgentCommands.DestroyVmResponse rsp, HttpEntity<String> e->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.DestroyVmCmd.class)
            rsp.success = false
            rsp.error = "no purpose"
            return rsp
        }

        env.message(CheckVmStateOnHypervisorMsg.class) { CheckVmStateOnHypervisorMsg msg, CloudBus bus ->
            def reply = new CheckVmStateOnHypervisorReply()
            def list = new HashMap<String, String>()
            list.put(vm.uuid, VmInstanceState.Stopped.toString())
            reply.setStates(list)
            reply.success = true
            bus.reply(msg, reply)
        }
    }

    void testFailedDestroyPreservesResourcesUntilSuccessfulRetry() {
        simulatorEnv()
        cmd = null
        boolean resourceReleased = false
        def recordRelease = { rsp, HttpEntity<String> e ->
            resourceReleased = true
            return rsp
        }
        env.afterSimulator(VirtualRouterConstant.VR_REMOVE_DHCP_PATH, recordRelease)

        def result = new DestroyVmInstanceAction(uuid: vm.uuid, sessionId: adminSession()).call()
        assert result.error != null
        assert cmd != null && cmd.uuid == vm.uuid
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Stopped
        assert !resourceReleased
        assert dbFindByUuid(vm.rootVolumeUuid, VolumeVO.class).status == VolumeStatus.Ready
        assert new ExpungeVmInstanceAction(uuid: vm.uuid, sessionId: adminSession()).call().error != null

        // A successful retry must perform cleanup before exposing Destroyed.
        env.cleanSimulatorAndMessageHandlers()
        env.afterSimulator(VirtualRouterConstant.VR_REMOVE_DHCP_PATH, recordRelease)
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.DestroyVmCmd.class)
            return rsp
        }
        destroyVmInstance { uuid = vm.uuid }
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Destroyed
        assert resourceReleased
        assert dbFindByUuid(vm.rootVolumeUuid, VolumeVO.class).status == VolumeStatus.Deleted
    }

    void testRecover() {
        recoverVmInstance {
            uuid = vm.uuid
        }

        startVmInstance {
            uuid = vm.uuid
        }

        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Running

        env.cleanSimulatorAndMessageHandlers()
    }

    void testExpunge() {
        assert Q.New(VolumeVO.class).eq(VolumeVO_.uuid, vm.rootVolumeUuid)
                .select(VolumeVO_.status)
                .findValue() == VolumeStatus.Deleted
        expungeVmInstance {
            uuid = vm.uuid
        }
    }
}
