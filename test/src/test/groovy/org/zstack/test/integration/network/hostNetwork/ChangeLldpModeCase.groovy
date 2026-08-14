package org.zstack.test.integration.network.hostNetwork

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.Platform
import org.zstack.core.db.SQL
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_
import org.zstack.network.hostNetworkInterface.lldp.LldpConstant
import org.zstack.network.hostNetworkInterface.lldp.LldpKvmAgentCommands
import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpVO
import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpVO_
import org.zstack.sdk.*
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.atomic.AtomicInteger

class ChangeLldpModeCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf

    @Override
    void setup() {
        spring {
            useSpring(HostNetworkTest.springSpec)
        }
    }

    @Override
    void environment() {
        env = makeEnv {
            zone {
                name = "zone1"
                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }

                    kvm {
                        name = "host2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }

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
            }
        }
    }

    @Override
    void test() {
        env.create {
            dbf = bean(DatabaseFacade.class)
            testChangeLldpMode()
            testReconnectHost()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testChangeLldpMode() {
        def host = env.inventoryByName("host1") as org.zstack.sdk.HostInventory
        def host2 = env.inventoryByName("host2") as org.zstack.sdk.HostInventory

        env.simulator(LldpConstant.CHANGE_LLDP_MODE_PATH) { HttpEntity<String> e, EnvSpec espec ->
            LldpKvmAgentCommands.ChangeLldpModeResponse rsp = new LldpKvmAgentCommands.ChangeLldpModeResponse()
            rsp.setSuccess(true)
            return rsp
        }

        HostNetworkInterfaceVO vo = new HostNetworkInterfaceVO();
        vo.setUuid(Platform.getUuid())
        vo.setHostUuid(host.getUuid())
        vo.setInterfaceName("enp101s0f2")
        vo.setSpeed(10000L)
        vo.setCarrierActive(true)
        vo.setMac("ac:1f:6b:93:6c:8e")
        vo.setPciDeviceAddress("0e:00.2")
        vo.setInterfaceType("noMaster")
        vo.setResourceName("test")
        dbf.persist(vo)

        List<HostNetworkInterfaceVO> interfaceVOS = Q.New(HostNetworkInterfaceVO.class)
                .eq(HostNetworkInterfaceVO_.hostUuid, host.getUuid())
                .list()
        assert interfaceVOS.size() == 1

        def lldpMode = changeHostNetworkInterfaceLldpMode {
            interfaceUuids = [vo.getUuid()]
            mode = "rx_only"
        } as List<HostNetworkInterfaceLldpInventory>

        assert lldpMode.get(0).mode == "rx_only"

        // do not support to set lldp mode of the interfaces with different hosts
        HostNetworkInterfaceVO vo1 = new HostNetworkInterfaceVO();
        vo1.setUuid(Platform.getUuid())
        vo1.setHostUuid(host2.getUuid())
        vo1.setInterfaceName("enp101s0f2")
        vo1.setSpeed(10000L)
        vo1.setCarrierActive(true)
        vo1.setMac("ac:1f:6b:93:6c:8e")
        vo1.setPciDeviceAddress("0e:00.2")
        vo1.setInterfaceType("noMaster")
        vo1.setResourceName("test")
        dbf.persist(vo1)

        expect(AssertionError.class) {
            changeHostNetworkInterfaceLldpMode {
                interfaceUuids = [vo.getUuid(), vo1.getUuid()]
                mode = "rx_only"
            }
        }

        dbf.remove(vo)
        dbf.remove(vo1)
        SQL.New(HostNetworkInterfaceLldpVO.class).hardDelete()
    }

    void testReconnectHost() {
        def host = env.inventoryByName("host1") as org.zstack.sdk.HostInventory

        HostNetworkInterfaceVO vo = new HostNetworkInterfaceVO();
        vo.setUuid(Platform.getUuid())
        vo.setHostUuid(host.getUuid())
        vo.setInterfaceName("enp101s0f2")
        vo.setSpeed(10000L)
        vo.setCarrierActive(true)
        vo.setMac("ac:1f:6b:93:6c:8e")
        vo.setPciDeviceAddress("0e:00.2")
        vo.setInterfaceType("noMaster")
        vo.setResourceName("test")
        dbf.persist(vo)

        AtomicInteger count = new AtomicInteger(0)
        env.simulator(LldpConstant.APPLY_LLDP_CONFIG_PATH) { HttpEntity<String> e, EnvSpec espec ->
            LldpKvmAgentCommands.ApplyLldpConfigResponse rsp = new LldpKvmAgentCommands.ApplyLldpConfigResponse()
            count.addAndGet(1)
            rsp.setSuccess(true)
            return rsp
        }

        reconnectHost {
            uuid = host.uuid
        }

        List<HostNetworkInterfaceLldpVO> lldpVOS = Q.New(HostNetworkInterfaceLldpVO.class)
                .eq(HostNetworkInterfaceLldpVO_.interfaceUuid, vo.getUuid())
                .list()
        assert lldpVOS.size() == 1

        assert count.get() == 1

        dbf.remove(vo)
        SQL.New(HostNetworkInterfaceLldpVO.class).hardDelete()
    }
}
