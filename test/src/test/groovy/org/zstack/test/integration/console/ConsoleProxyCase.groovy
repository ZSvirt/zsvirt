package org.zstack.test.integration.console

import org.springframework.http.HttpEntity
import org.zstack.console.ConsoleGlobalConfig
import org.zstack.core.CoreGlobalProperty
import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.gc.GCStatus
import org.zstack.header.console.ConsoleConstants
import org.zstack.header.console.ConsoleProxyAgentVO
import org.zstack.header.console.ConsoleProxyCommands
import org.zstack.header.console.ConsoleProxyVO
import org.zstack.header.console.ConsoleProxyVO_
import org.zstack.sdk.ConsoleInventory
import org.zstack.sdk.GarbageCollectorInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase

class ConsoleProxyCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        dbf = bean(DatabaseFacade.class)
        env = env {
            account {
                name = "test"
                password = "password"
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
                cpu = 1
                memoryGB(1)
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testUpdateConsoleProxyAgent()
            testConsoleProxyGC()
        }
    }

    void testConsoleProxyGC() {
        ConsoleProxyAgentVO agent = dbf.listAll(ConsoleProxyAgentVO)[0]

        env.afterSimulator(ConsoleConstants.CONSOLE_PROXY_ESTABLISH_PROXY_PATH) { ConsoleProxyCommands.EstablishProxyRsp rsp, HttpEntity<String> e ->
            rsp.proxyPort = 5900
            rsp.token = "test token"
            return rsp
        }

        boolean error = true
        env.afterSimulator(ConsoleConstants.CONSOLE_PROXY_DELETE_PROXY_PATH) { rsp, HttpEntity<String> e ->
            if (error) {
                throw new HttpError(504, "on purpose")
            }

            return rsp
        }

        ConsoleGlobalConfig.DELETE_CONSOLE_PROXY_RETRY_DELAY.updateValue(100)
        VmInstanceInventory vm = env.inventoryByName("vm")

        ConsoleInventory console = requestConsoleAccess {
            vmInstanceUuid = vm.uuid
        } as ConsoleInventory

        assert console.port == 5900

        ConsoleProxyVO vo = Q.New(ConsoleProxyVO.class)
                .eq(ConsoleProxyVO_.vmInstanceUuid, console.getToken().split("_")[-1]).find()
        assert vo.proxyPort == 5900

        vo.setProxyPort(5901)
        dbf.update(vo)
        assert vo.proxyPort == 5901

        // request again
        console = requestConsoleAccess {
            vmInstanceUuid = vm.uuid
        } as ConsoleInventory

        assert console.port == 5900

        destroyVmInstance {
            uuid = vm.uuid
        }

        GarbageCollectorInventory gc = queryGCJob {
            conditions = ["status!=${GCStatus.Done}".toString(), "context~=%${vm.uuid}%".toString()]
        }[0] as GarbageCollectorInventory
        assert gc != null
        assert gc.status == GCStatus.Idle.toString()

        error = false
        triggerGCJob {
            uuid = gc.uuid
        }

        retryInSecs {
            gc = queryGCJob {
                conditions = ["status=${GCStatus.Done}".toString(), "context~=%${vm.uuid}%".toString()]
            }[0] as GarbageCollectorInventory
            assert gc.status == GCStatus.Done.toString()
        }
    }

    def testUpdateConsoleProxyAgent() {
        retryInSecs {
            assert dbf.count(ConsoleProxyAgentVO) == 1
        }

        ConsoleProxyAgentVO agent = dbf.listAll(ConsoleProxyAgentVO)[0]

        updateConsoleProxyAgent {
            uuid = agent.uuid
            consoleProxyOverriddenIp = "127.0.0.2"
        }
        agent = dbf.reload(agent)
        assert agent.consoleProxyOverriddenIp == "127.0.0.2"
        
        updateConsoleProxyAgent {
            uuid = agent.uuid
            consoleProxyOverriddenIp = "127.0.0.1"
            consoleProxyPort = 4789
        }

        assert Platform.getGlobalProperties().get("consoleProxyPort") == '4789'
        assert CoreGlobalProperty.CONSOLE_PROXY_PORT == 4789
        agent = dbf.reload(agent)
        assert agent.consoleProxyPort == 4789

        // update console proxy agent
        updateConsoleProxyAgent {
            uuid = agent.uuid
            consoleProxyOverriddenIp = "127.0.0.1"
            consoleProxyPort = 4900
        }

        assert Platform.getGlobalProperties().get("consoleProxyOverriddenIp") == '127.0.0.1'
        assert CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IP == '127.0.0.1'
        //When the console port is 0 (empty), the default CoreGlobalProperty port 4900 is set
        assert Platform.getGlobalProperties().get("consoleProxyPort") == '4900'
        assert CoreGlobalProperty.CONSOLE_PROXY_PORT == 4900
        agent = dbf.reload(agent)
        assert agent.consoleProxyOverriddenIp == "127.0.0.1"
        assert agent.consoleProxyPort == 4900

        // update console proxy agent by none admin account
        SessionInventory testAccountSession = logInByAccount {
            accountName = "test"
            password = "password"
        } as SessionInventory
        expect(AssertionError) {
            updateConsoleProxyAgent {
                sessionId = testAccountSession.uuid
                uuid = agent.uuid
                consoleProxyOverriddenIp = "127.0.0.1"
                consoleProxyPort = 4900
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
