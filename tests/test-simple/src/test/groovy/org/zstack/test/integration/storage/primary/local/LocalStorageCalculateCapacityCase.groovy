package org.zstack.test.integration.storage.primary.local

import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.GetLocalStorageHostDiskCapacityAction
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class LocalStorageCalculateCapacityCase extends SubCase {
    EnvSpec env
    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        spring {
            sftpBackupStorage()
            localStorage()
            virtualRouter()
            securityGroup()
            kvm()
        }
    }

    @Override
    void environment() {
        env = makeEnv {
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    architecture = "x86_64"
                    url = "http://zstack.org/download/test.qcow2"
                    virtio = true
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

            vm {
                name = "vm"
                virtio = true
                cpu = 4
                memoryGB(8)
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testCalculateCapacityWhenAddLocalStorage()
        }
    }

    void testCalculateCapacityWhenAddLocalStorage(){
        ClusterInventory cluster = env.inventoryByName("cluster")
        PrimaryStorageInventory ps = env.inventoryByName("local")

        boolean temp = false

        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { rsp, HttpEntity<String> entity ->
            rsp.success = true

            def cmd = json(entity.getBody(),KVMAgentCommands.ConnectCmd.class)
            GetLocalStorageHostDiskCapacityAction action = new GetLocalStorageHostDiskCapacityAction()
            action.hostUuid = cmd.hostUuid
            action.primaryStorageUuid = ps.uuid
            action.sessionId = adminSession()
            GetLocalStorageHostDiskCapacityAction.Result res = action.call()

            assert res.error == null
            temp = true

            return rsp
        }

        addKVMHost {
            username = "test"
            password = "password"
            name = "adding-host"
            managementIp = "127.0.0.2"
            clusterUuid  = cluster.uuid
        }

        assert temp
    }
}
