package org.zstack.test.integration.networkservice.provider.flat.dhcp

import org.zstack.core.db.Q
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.network.IPv6Constants

import static org.zstack.network.service.flat.FlatNetworkSystemTags.L3_NETWORK_DHCP_IP
import static org.zstack.network.service.flat.FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN

/**
 * Created by shixin on 2018/12/19.
 */
class CreateDhcpServerCase extends SubCase {

    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
        spring {
            ceph()
            flatNetwork()
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
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
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
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }
                    }

                    l3Network {
                        name = "l3-ipv6"
                        ipVersion = 6

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
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
            testAddDhcpServerIp()
            testAddDhcpv6ServerIp()
        }
    }

    void testAddDhcpServerIp() {
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        String dhcpIp = "192.168.0.105"
        expect (AssertionError.class) {
            addIpRange {
                l3NetworkUuid = l3.uuid
                name = "test-ip-range"
                startIp = "192.168.0.3"
                endIp = "192.168.0.254"
                netmask = "255.255.255.0"
                gateway = "192.168.0.1"
                systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", "192.168.1.2")]
            }  
        }

        IpRangeInventory ipr1 = addIpRange {
            l3NetworkUuid = l3.uuid
            name = "test-ip-range"
            startIp = "192.168.0.3"
            endIp = "192.168.0.30"
            netmask = "255.255.255.0"
            gateway = "192.168.0.1"
            systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", dhcpIp)]
        }

        expect (AssertionError.class) {
            addIpRange {
                l3NetworkUuid = l3.uuid
                name = "test-ip-range"
                startIp = "192.168.0.103"
                endIp = "192.168.0.130"
                netmask = "255.255.255.0"
                gateway = "192.168.0.1"
                systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", "192.168.1.2")]
            }
        }

        expect (AssertionError.class) {
            addIpRange {
                l3NetworkUuid = l3.uuid
                name = "test-ip-range"
                startIp = "192.168.0.103"
                endIp = "192.168.0.130"
                netmask = "255.255.255.0"
                gateway = "192.168.0.1"
                systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", "192.168.1.110")]
            }
        }
        String dhcpServerIp = L3_NETWORK_DHCP_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp == dhcpIp

        IpRangeInventory ipr2 = addIpRange {
            l3NetworkUuid = l3.uuid
            name = "test-ip-range"
            startIp = "192.168.0.103"
            endIp = "192.168.0.130"
            netmask = "255.255.255.0"
            gateway = "192.168.0.1"
        }
        assert Q.New(UsedIpVO.class).eq(UsedIpVO_.ip, dhcpIp).count() == 1

        IpRangeInventory ipr3 = addIpRange {
            l3NetworkUuid = l3.uuid
            name = "test-ip-range"
            startIp = "192.168.0.203"
            endIp = "192.168.0.230"
            netmask = "255.255.255.0"
            gateway = "192.168.0.1"
        }

        deleteIpRange {
            uuid = ipr1.uuid
        }

        dhcpServerIp = L3_NETWORK_DHCP_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp == dhcpIp

        expect (AssertionError.class) {
            addIpRange {
                l3NetworkUuid = l3.uuid
                name = "test-ip-range"
                startIp = "192.168.0.60"
                endIp = "192.168.0.70"
                netmask = "255.255.255.0"
                gateway = "192.168.0.1"
                systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", "192.168.1.2")]
            }
        }

        deleteIpRange {
            uuid = ipr2.uuid
        }

        deleteIpRange {
            uuid = ipr3.uuid
        }

        dhcpServerIp = L3_NETWORK_DHCP_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp == null

        IpRangeInventory ipr4 = addIpRangeByNetworkCidr {
            l3NetworkUuid = l3.uuid
            name = "test-ip-range"
            networkCidr = "192.168.0.0/24"
            systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", "192.168.0.4")]
        }

        dhcpServerIp = L3_NETWORK_DHCP_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp == "192.168.0.4"

        deleteIpRange {
            uuid = ipr4.uuid
        }

        dhcpServerIp = L3_NETWORK_DHCP_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp == null

        IpRangeInventory ipr5 = addIpRange {
            l3NetworkUuid = l3.uuid
            name = "test-ip-range"
            startIp = "192.168.0.60"
            endIp = "192.168.0.70"
            netmask = "255.255.255.0"
            gateway = "192.168.0.1"
        }
        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            defaultL3NetworkUuid = l3.uuid
        }

        dhcpServerIp = L3_NETWORK_DHCP_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp != null

        expect (AssertionError.class) {
            addIpRange {
                l3NetworkUuid = l3.uuid
                name = "test-ip-range"
                startIp = "192.168.0.103"
                endIp = "192.168.0.130"
                netmask = "255.255.255.0"
                gateway = "192.168.0.1"
                systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", "192.168.1.110")]
            }
        }
    }

    void testAddDhcpv6ServerIp() {
        L3NetworkInventory l3 = env.inventoryByName("l3-ipv6")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        expect (AssertionError.class) {
            addIpv6Range {
                l3NetworkUuid = l3.uuid
                name = "test-ipv6-range"
                startIp = "1000::10"
                endIp = "1000::20"
                prefixLen = 64
                gateway = "1000::1"
                addressMode = IPv6Constants.Stateful_DHCP
                systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", "2000--2")]
            }
        }

        IpRangeInventory ipr1 = addIpv6Range {
            l3NetworkUuid = l3.uuid
            name = "test-ipv6-range"
            startIp = "1000::10"
            endIp = "1000::20"
            prefixLen = 64
            gateway = "1000::1"
            addressMode = IPv6Constants.Stateful_DHCP
            systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", "1000--2")]
        }

        expect (AssertionError.class) {
            addIpv6Range {
                l3NetworkUuid = l3.uuid
                name = "test-ipv6-range"
                startIp = "1000::30"
                endIp = "1000::40"
                prefixLen = 64
                gateway = "1000::1"
                addressMode = IPv6Constants.Stateful_DHCP
                systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", "1000--2")]
            }
        }

        expect (AssertionError.class) {
            addIpv6Range {
                l3NetworkUuid = l3.uuid
                name = "test-ipv6-range"
                startIp = "1000::50"
                endIp = "1000::60"
                prefixLen = 64
                gateway = "1000::1"
                addressMode = IPv6Constants.Stateful_DHCP
                systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", "1000--12")]
            }
        }

        IpRangeInventory ipr2 = addIpv6Range {
            l3NetworkUuid = l3.uuid
            name = "test-ipv6-range-2"
            startIp = "1000::30"
            endIp = "1000::40"
            prefixLen = 64
            gateway = "1000::1"
            addressMode = IPv6Constants.Stateful_DHCP
        }

        IpRangeInventory ipr3 = addIpv6Range {
            l3NetworkUuid = l3.uuid
            name = "test-ipv6-range-2"
            startIp = "1000::60"
            endIp = "1000::70"
            prefixLen = 64
            gateway = "1000::1"
            addressMode = IPv6Constants.Stateful_DHCP
        }

        deleteIpRange {
            uuid = ipr1.uuid
        }

        String dhcpServerIp = L3_NETWORK_DHCP_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp == "1000--2"

        expect (AssertionError.class) {
            addIpv6Range {
                l3NetworkUuid = l3.uuid
                name = "test-ipv6-range"
                startIp = "1000::150"
                endIp = "1000::160"
                prefixLen = 64
                gateway = "1000::1"
                addressMode = IPv6Constants.Stateful_DHCP
                systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", "1000--155")]
            }
        }

        deleteIpRange {
            uuid = ipr2.uuid
        }

        deleteIpRange {
            uuid = ipr3.uuid
        }

        dhcpServerIp = L3_NETWORK_DHCP_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp == null

        IpRangeInventory ipr4 = addIpv6RangeByNetworkCidr {
            l3NetworkUuid = l3.uuid
            name = "test-ip-range"
            networkCidr = "1000::/64"
            addressMode = IPv6Constants.Stateful_DHCP
            systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", "1000--155")]
        }

        dhcpServerIp = L3_NETWORK_DHCP_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp == "1000--155"

        deleteIpRange {
            uuid = ipr4.uuid
        }

        dhcpServerIp = L3_NETWORK_DHCP_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp == null

        IpRangeInventory ipr5 = addIpv6Range {
            l3NetworkUuid = l3.uuid
            name = "test-ipv6-range"
            startIp = "1000::150"
            endIp = "1000::160"
            prefixLen = 64
            gateway = "1000::1"
            addressMode = IPv6Constants.Stateful_DHCP
        }
        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            defaultL3NetworkUuid = l3.uuid
        }

        dhcpServerIp = L3_NETWORK_DHCP_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp != null

        expect (AssertionError.class) {
            addIpv6Range {
                l3NetworkUuid = l3.uuid
                name = "test-ipv6-range"
                startIp = "1000::250"
                endIp = "1000::260"
                prefixLen = 64
                gateway = "1000::1"
                addressMode = IPv6Constants.Stateful_DHCP
                systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", "1000--12")]
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }

}
