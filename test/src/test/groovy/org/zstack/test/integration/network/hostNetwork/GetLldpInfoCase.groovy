package org.zstack.test.integration.network.hostNetwork

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.SQL
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO
import org.zstack.network.hostNetworkInterface.lldp.LldpConstant
import org.zstack.network.hostNetworkInterface.lldp.LldpInfoStruct
import org.zstack.network.hostNetworkInterface.lldp.LldpKvmAgentCommands
import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpVO
import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpVO_
import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpRefVO
import org.zstack.sdk.*
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.sql.Timestamp

class GetLldpInfoCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf
    GetHostNetworkInterfaceLldpResult ms
    GetHostNetworkInterfaceLldpResult ms2
    GetHostNetworkInterfaceLldpResult ms3
    String TEST_UUID = "36c22e8ff05c2780bf6d2fa65700f22e"
    String TEST_UUID_2 = "8b7844d7367c41dd86ebdd59052af8b9"

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
            testGetLldpInfo()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testGetLldpInfo() {
        def host = env.inventoryByName("host1") as org.zstack.sdk.HostInventory
        def host2 = env.inventoryByName("host2") as org.zstack.sdk.HostInventory

        HostNetworkInterfaceVO vo = new HostNetworkInterfaceVO();
        vo.setUuid(TEST_UUID)
        vo.setHostUuid(host.getUuid())
        vo.setInterfaceName("enp101s0f2")
        vo.setSpeed(10000L)
        vo.setCarrierActive(true)
        vo.setMac("ac:1f:6b:93:6c:8e")
        vo.setPciDeviceAddress("0e:00.2")
        vo.setInterfaceType("noMaster")
        vo.setResourceName("test")
        dbf.persist(vo)

        env.simulator(LldpConstant.CHANGE_LLDP_MODE_PATH) { HttpEntity<String> e, EnvSpec espec ->
            LldpKvmAgentCommands.ChangeLldpModeResponse rsp = new LldpKvmAgentCommands.ChangeLldpModeResponse()
            rsp.setSuccess(true)
            return rsp
        }

        def lldpMode = changeHostNetworkInterfaceLldpMode {
            interfaceUuids = [vo.getUuid()]
            mode = "rx_only"
        } as List<HostNetworkInterfaceLldpInventory>

        assert lldpMode.get(0).mode == "rx_only"
        
        env.simulator(LldpConstant.GET_LLDP_INFO_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def reply = new LldpKvmAgentCommands.GetLldpInfoResponse()
            reply.lldpInfo = new LldpInfoStruct()
            reply.lldpInfo.chassisId = "mac 00:1e:08:1d:05:ba"
            reply.lldpInfo.timeToLive = 120
            reply.lldpInfo.managementAddress = "172.25.2.4"
            reply.lldpInfo.systemName = "BM-MN-3"
            reply.lldpInfo.systemDescription = "CentecOS software, E530, Version 7.4.7 Copyright (C) 2004-2021 Centec Networks Inc.  All Rights Reserved."
            reply.lldpInfo.systemCapabilities = "Bridge, on  Router, on"
            reply.lldpInfo.portId = "ifname eth-0-5"
            reply.lldpInfo.portDescription = "eth-0-4"
            reply.lldpInfo.vlanId = 3999
            reply.lldpInfo.aggregationPortId = 4294965248L
            reply.lldpInfo.mtu = 9600
            return reply
        }

        ms = getHostNetworkInterfaceLldp {
            interfaceUuid = TEST_UUID
        } as GetHostNetworkInterfaceLldpResult

        assert ms.lldp.portId == "ifname eth-0-5"

        HostNetworkInterfaceVO vo1 = new HostNetworkInterfaceVO();
        vo1.setUuid(TEST_UUID_2)
        vo1.setHostUuid(host2.getUuid())
        vo1.setInterfaceName("enp101s0f2")
        vo1.setSpeed(10000L)
        vo1.setCarrierActive(true)
        vo1.setMac("ac:1f:6b:93:6c:8e")
        vo1.setPciDeviceAddress("0e:00.2")
        vo1.setInterfaceType("noMaster")
        vo1.setResourceName("test")
        dbf.persist(vo1)

        HostNetworkInterfaceLldpVO lldpVO1 = new HostNetworkInterfaceLldpVO();
        lldpVO1.setUuid(Platform.uuid)
        lldpVO1.setInterfaceUuid(TEST_UUID_2)
        lldpVO1.setMode("rx_only")
        lldpVO1.setAccountUuid(currentEnvSpec.session.getAccountUuid())
        lldpVO1.setResourceName("test")
        dbf.persist(lldpVO1)

        env.simulator(LldpConstant.GET_LLDP_INFO_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def reply = new LldpKvmAgentCommands.GetLldpInfoResponse()
            reply.lldpInfo = new LldpInfoStruct()
            reply.lldpInfo.chassisId = "mac 00:1e:08:1d:05:ba"
            reply.lldpInfo.timeToLive = 119
            reply.lldpInfo.managementAddress = "172.25.2.4"
            reply.lldpInfo.systemName = "BM-MN-3"
            reply.lldpInfo.systemDescription = "CentecOS software, E530, Version 7.4.7 Copyright (C) 2004-2021 Centec Networks Inc.  All Rights Reserved."
            reply.lldpInfo.systemCapabilities = "Bridge, on  Router, on"
            reply.lldpInfo.portId = "ifname eth-0-5"
            reply.lldpInfo.portDescription = "eth-0-4"
            reply.lldpInfo.vlanId = 3999
            reply.lldpInfo.aggregationPortId = 4294965248L
            reply.lldpInfo.mtu = 9600
            return reply
        }

        ms2 = getHostNetworkInterfaceLldp {
            interfaceUuid = TEST_UUID_2
        } as GetHostNetworkInterfaceLldpResult

        ms3 = getHostNetworkInterfaceLldp {
            interfaceUuid = TEST_UUID_2
        } as GetHostNetworkInterfaceLldpResult

        assert ms2.lldp.managementAddress == "172.25.2.4"
        assert ms.lldp.timeToLive != ms2.lldp.timeToLive
        assert ms2.lldp.chassisId == ms3.lldp.chassisId

        def thread1 = Thread.start {
            getHostNetworkInterfaceLldp {
                interfaceUuid = TEST_UUID
            }
        }
        def thread2 = Thread.start {
            getHostNetworkInterfaceLldp {
                interfaceUuid = TEST_UUID_2
            }
        }
        [thread1, thread2].each {it.join()}

        SQL.New(HostNetworkInterfaceLldpVO.class)
                .eq(HostNetworkInterfaceLldpVO_.interfaceUuid, TEST_UUID_2)
                .set(HostNetworkInterfaceLldpVO_.mode, "tx_only")
                .update()

        // do not support to get lldp info of the interface which mode is not in receive mode
        expect(AssertionError.class) {
            getHostNetworkInterfaceLldp {
                interfaceUuid = TEST_UUID_2
            }
        }

        SQL.New(HostNetworkInterfaceVO.class).hardDelete()
        SQL.New(HostNetworkInterfaceLldpVO.class).hardDelete()
        SQL.New(HostNetworkInterfaceLldpRefVO.class).hardDelete()
    }
}
