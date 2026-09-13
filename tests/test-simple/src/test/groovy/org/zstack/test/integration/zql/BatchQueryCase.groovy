package org.zstack.test.integration.zql

import org.zstack.sdk.BatchQueryAction
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class BatchQueryCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(ZqlTest.springSpec)
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
                    name = "image"
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
                        name = "host"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "host2"
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

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                cpu = 4
                memoryGB(8)
                useImage("image")
                useL3Networks("l3")
                useHost("host")
                disk {
                    sizeGB(20)
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            testRemoteCodeExecution()
        }
    }

    static void testRemoteCodeExecution() {
        String queryScript = '''@groovy.transform.ASTTest(value={assert java.lang.Runtime.getRuntime().exec("touch /tmp/pwned")}) def x'''

        BatchQueryAction action = new BatchQueryAction(
                sessionId: currentEnvSpec.session.uuid,
                script: queryScript
        )
        BatchQueryAction.Result result = action.call()
        assert result.error != null
        result.error.details.contains("Annotation groovy.transform.ASTTest cannot be used in the sandbox")

        queryScript = '''
import groovy.transform.*
@ASTTest(value={assert java.lang.Runtime.getRuntime().exec("touch /tmp/pwned")}) def x
'''
        action = new BatchQueryAction(
                sessionId: currentEnvSpec.session.uuid,
                script: queryScript
        )
        result = action.call()
        result.error.details.contains("Annotation groovy.transform.ASTTest cannot be used in the sandbox")
    }

    @Override
    void clean() {
        env.delete()
    }
}
