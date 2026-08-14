package org.zstack.test.integration.networkservice.provider.flat

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.SimpleQuery
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.service.NetworkServiceProviderVO
import org.zstack.header.network.service.NetworkServiceProviderVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.flat.FlatNetworkSystemTags
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.HostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.NetworkServiceL3NetworkRefInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import static org.zstack.utils.CollectionDSL.list
/**
 * Created by lining on 2017/4/4.
 */
// base on TestMevoco20
class ChangeNetworkSerivceCase extends SubCase{

    EnvSpec env

    DatabaseFacade dbf

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
        spring {
            securityGroup()
        }
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
                    name = "image1"
                    url  = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    url  = "http://zstack.org/download/vr.qcow2"
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
                        managementIp = "127.0.0.1"
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
                    useImage("image1")
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
            testVr2Flat()
        }
    }

    void testVr2Flat() {
        dbf = bean(DatabaseFacade.class)

        VmInstanceInventory vm = env.inventoryByName("vm")
        VmNicInventory nic = vm.getVmNics().get(0);
        L3NetworkInventory l3 = env.inventoryByName("l3")
        Map<String, List<String>> services = new HashMap<String, List<String>>()
        for (NetworkServiceL3NetworkRefInventory ref : l3.getNetworkServices()) {
            List<String> types = services.get(ref.getNetworkServiceProviderUuid())
            if (types == null) {
                types = new ArrayList<String>()
                services.put(ref.getNetworkServiceProviderUuid(), types)
            }
            types.add(ref.getNetworkServiceType())
        }

        detachNetworkServiceFromL3Network {
            l3NetworkUuid = l3.uuid
            networkServices = services
        }

        SimpleQuery<NetworkServiceProviderVO> q = dbf.createQuery(NetworkServiceProviderVO.class);
        q.add(NetworkServiceProviderVO_.type, SimpleQuery.Op.EQ, FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
        NetworkServiceProviderVO vo = q.find();

        Map<String, List<String>> ntypes = new HashMap<String, List<String>>(1)
        ntypes.put(vo.uuid, list(NetworkServiceType.DHCP.toString()))
        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3.uuid
            networkServices = ntypes
        }

        FlatDhcpBackend.ApplyDhcpCmd acmd
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            FlatDhcpBackend.BatchApplyDhcpCmd batchApplyDhcpCmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)

            assert batchApplyDhcpCmd.dhcpInfos.size() == 1

            acmd = batchApplyDhcpCmd.dhcpInfos.get(0)
            assert !acmd.dhcp.empty

            assert acmd.dhcp.stream().filter({dhcp -> nic.ip == dhcp.ip})
                .filter({
                dhcp -> dhcp != null
                    dhcp.mac == nic.mac &&
                    dhcp.gateway == nic.gateway &&
                    dhcp.netmask == nic.netmask &&
                    dhcp.isDefaultL3Network &&
                    dhcp.dns != null &&
                    dhcp.bridgeName != null &&
                    dhcp.namespaceName != null
            }).count() == 1
            return rsp
        }

        FlatDhcpBackend.BatchPrepareDhcpCmd cmd
        env.afterSimulator(FlatDhcpBackend.BATCH_PREPARE_DHCP_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.BatchPrepareDhcpCmd.class)

            Map<String, String> tokens = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTokensByResourceUuid(l3.getUuid());
            String dhcpServerIp = tokens.get(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN);
            String dhcpServerIpUuid = tokens.get(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_UUID_TOKEN)
            UsedIpVO ipvo = dbf.findByUuid(dhcpServerIpUuid, UsedIpVO.class)
            assert ipvo.getIp() == dhcpServerIp
            assert dhcpServerIp == cmd.dhcpInfos.get(0).dhcpServerIp
            assert ipvo.getNetmask() == cmd.dhcpInfos.get(0).dhcpNetmask

            return rsp
        }

        HostInventory host = env.inventoryByName("kvm")

        retryInSecs(3) {
            reconnectHost {
                uuid = host.uuid
            }
        }

        assert FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.hasTag(l3.getUuid())
        Map<String, String> tokens = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTokensByResourceUuid(l3.getUuid());
        String dhcpServerIpUuid = tokens.get(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_UUID_TOKEN);
        UsedIpVO ipvo = dbf.findByUuid(dhcpServerIpUuid, UsedIpVO.class);
        assert ipvo != null

        assert null != acmd
        assert null != cmd
        FlatDhcpBackend.DhcpInfo dhcp = acmd.dhcp[0]
        assert dhcp.bridgeName == cmd.dhcpInfos.get(0).bridgeName
        assert dhcp.namespaceName == cmd.dhcpInfos.get(0).namespaceName
    }

    @Override
    void clean() {
        env.delete()
    }

}
