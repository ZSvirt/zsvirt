package org.zstack.test.integration.networkservice.provider.virtualrouter.dhcp

import org.springframework.http.HttpEntity
import org.zstack.appliancevm.ApplianceVmVO
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.network.l2.L2NetworkVO
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmNicInventory
import org.zstack.header.vm.VmNicVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMSystemTags;
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO_
import org.zstack.network.service.virtualrouter.dns.VirtualRouterCentralizedDnsBackend
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.TagUtils
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by AlanJager on 2017/7/8.
 */
class VirtualRouterFlatDhcpCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.getSpringSpec())
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
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
                    attachL2Network("l22")
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
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.Centralized_DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString()]
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
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }
                }

                l2VlanNetwork {
                    name = "l22"
                    physicalInterface = "eth1"
                    vlan = 222

                    l3Network {
                        name = "l32"

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.Centralized_DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     NetworkServiceType.DHCP.toString(),
                                     PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.101.10"
                            endIp = "192.168.101.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.101.1"
                        }
                    }

                    l3Network {
                        name = "pubL32"

                        ip {
                            startIp = "11.168.101.10"
                            endIp = "11.168.101.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.101.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                eip {
                    name = "eip"
                    useVip("pubL3")
                }

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                virtualRouterOffering {
                    name = "vro2"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL32")
                    usePublicL3Network("pubL32")
                    useImage("vr")
                }
            }

            vm {
                name = "vm"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            /* centerlizedDns is not used any more, remove the case
            testCreateVmWillSetForwardDns()
            testCreateVmFailWillReleaseDnsForward()
            testVRouterDhcpWontSendCmd()*/
        }
    }

    void testCreateVmWillSetForwardDns() {
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        HostInventory host = env.inventoryByName("kvm") as HostInventory

        VirtualRouterCommands.SetForwardDnsCmd cmd = null
        env.afterSimulator(VirtualRouterCentralizedDnsBackend.SET_DNS_FORWARD_PATH) { VirtualRouterCommands.SetForwardDnsRsp rsp, HttpEntity<String> e ->
            cmd = json(e.body, VirtualRouterCommands.SetForwardDnsCmd.class)
            return rsp
        }

        addDnsToL3Network {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.dns = "8.8.8.8"
        }

        createVmInstance {
            name = "test"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host.uuid
        }

        assert cmd != null
        ApplianceVmVO vr = Q.New(ApplianceVmVO.class).find()
        String bridgeName = SQL.New("select t.tag from SystemTagVO t, L3NetworkVO l3 where t.resourceType = :ttype and t.tag like :tag" +
                " and t.resourceUuid = l3.l2NetworkUuid and l3.uuid = :l3Uuid", String.class)
                .param("tag", TagUtils.tagPatternToSqlPattern(KVMSystemTags.L2_BRIDGE_NAME.getTagFormat()))
                .param("l3Uuid", l3.getUuid())
                .param("ttype", L2NetworkVO.class.getSimpleName())
                .find()
        assert cmd.bridgeName == KVMSystemTags.L2_BRIDGE_NAME.getTokenByTag(bridgeName, KVMSystemTags.L2_BRIDGE_NAME_TOKEN).toString()
        assert cmd.nameSpace == makeNamespaceName(KVMSystemTags.L2_BRIDGE_NAME.getTokenByTag(bridgeName, KVMSystemTags.L2_BRIDGE_NAME_TOKEN).toString()
        , l3.uuid)
        def dns = ""
        for (VmNicVO nic : vr.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(l3.uuid)) {
                dns = nic.getIp()
            }
        }
        assert cmd.dns.equals(dns)

        env.cleanAfterSimulatorHandlers()
    }

    String makeNamespaceName(String brName, String l3Uuid) {
        return String.format("%s_%s", brName, l3Uuid)
    }

    void testCreateVmFailWillReleaseDnsForward() {
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        HostInventory host = env.inventoryByName("kvm") as HostInventory

        VirtualRouterCommands.RemoveForwardDnsCmd cmd = null
        env.afterSimulator(VirtualRouterCentralizedDnsBackend.REMOVE_DNS_FORWARD_PATH) { VirtualRouterCommands.RemoveForwardDnsRsp rsp, HttpEntity<String> e ->
            cmd = json(e.body, VirtualRouterCommands.RemoveForwardDnsCmd.class)
            return rsp
        }

        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { KVMAgentCommands.StartVmResponse rsp, HttpEntity<String> e ->
            throw new Exception("fail to start a VM on purpose")
        }

        CreateVmInstanceAction action = new CreateVmInstanceAction()
        action.name = "test"
        action.instanceOfferingUuid = instanceOffering.uuid
        action.imageUuid = image.uuid
        action.l3NetworkUuids = [l3.uuid]
        action.hostUuid = host.uuid
        action.sessionId = adminSession()
        CreateVmInstanceAction.Result ret = action.call()
        assert ret.error != null

        retryInSecs {
            assert cmd != null
            ApplianceVmVO vr = Q.New(ApplianceVmVO.class).find()
            String bridgeName = SQL.New("select t.tag from SystemTagVO t, L3NetworkVO l3 where t.resourceType = :ttype and t.tag like :tag" +
                    " and t.resourceUuid = l3.l2NetworkUuid and l3.uuid = :l3Uuid", String.class)
                    .param("tag", TagUtils.tagPatternToSqlPattern(KVMSystemTags.L2_BRIDGE_NAME.getTagFormat()))
                    .param("l3Uuid", l3.getUuid())
                    .param("ttype", L2NetworkVO.class.getSimpleName())
                    .find()
            assert cmd.bridgeName == KVMSystemTags.L2_BRIDGE_NAME.getTokenByTag(bridgeName, KVMSystemTags.L2_BRIDGE_NAME_TOKEN).toString()
            assert cmd.nameSpace == makeNamespaceName(KVMSystemTags.L2_BRIDGE_NAME.getTokenByTag(bridgeName, KVMSystemTags.L2_BRIDGE_NAME_TOKEN).toString()
                    , l3.uuid)
        }

        env.cleanAfterSimulatorHandlers()
    }

    void testVRouterDhcpWontSendCmd() {
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l32") as L3NetworkInventory
        HostInventory host = env.inventoryByName("kvm") as HostInventory

        VirtualRouterCommands.SetForwardDnsCmd cmd = null
        env.afterSimulator(VirtualRouterCentralizedDnsBackend.SET_DNS_FORWARD_PATH) { VirtualRouterCommands.SetForwardDnsRsp rsp, HttpEntity<String> e ->
            cmd = json(e.body, VirtualRouterCommands.SetForwardDnsCmd.class)
            return rsp
        }

        addDnsToL3Network {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.dns = "8.8.8.8"
        }

        createVmInstance {
            name = "test"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }

        assert cmd == null

        env.cleanAfterSimulatorHandlers()
    }
}
