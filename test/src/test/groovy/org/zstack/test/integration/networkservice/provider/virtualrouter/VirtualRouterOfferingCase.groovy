package org.zstack.test.integration.networkservice.provider.virtualrouter

import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.IpRangeInventory
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.VirtualRouterOfferingInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
/**
 * Created by weiwang on 20/11/2017
 */
class VirtualRouterOfferingCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env{
            account {
                name = "test1"
                password = "password"
                predefineRole("vrouter")
            }

            account {
                name = "test2"
                password = "password"
                predefineRole("vrouter")
            }

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
                    name = "vr-image"
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
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2-2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3-1"

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        service {
                            provider = VyosConstants.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.SNAT.toString()]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3-1"
                        category = "Public"

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }

                    l3Network {
                        name = "pubL3-2"
                        category = "Public"

                        ip {
                            startIp = "12.16.20.10"
                            endIp = "12.16.20.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.20.1"
                        }
                    }
                }

                l2VlanNetwork {
                    name = "l2-2"
                    physicalInterface = "eth0"
                    vlan = 200
                }

                virtualRouterOffering {
                    name = "vr"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3-1")
                    usePublicL3Network("pubL3-1")
                    useImage("vr-image")
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            TestCreateVirtualRouterOfferingParameter()
            testVirtualRouterOfferingQueryByOtherAccount()
        }
    }

    void  TestCreateVirtualRouterOfferingParameter() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def image = env.inventoryByName("vr-image") as ImageInventory
        def l2 = env.inventoryByName("l2") as L2NetworkInventory
        def pub = env.inventoryByName("pubL3-1") as L3NetworkInventory

        L3NetworkInventory l3_11 = createL3Network {
            delegate.category = "Public"
            delegate.l2NetworkUuid = l2.uuid
            delegate.name = "pubL3-11"
        }

        IpRangeInventory iprInv = addIpRange {
            delegate.name = "TestIpRange"
            delegate.l3NetworkUuid = l3_11.uuid
            delegate.startIp = "12.16.10.101"
            delegate.endIp = "12.16.10.200"
            delegate.gateway = "12.16.10.1"
            delegate.netmask = "255.255.255.0"
        }

        expect(AssertionError.class) {
            createVirtualRouterOffering {
                name = "vro"
                memorySize = SizeUnit.MEGABYTE.toByte(512)
                cpuNum = 2
                managementNetworkUuid = pub.uuid
                publicNetworkUuid = l3_11.uuid
                imageUuid = image.uuid
                zoneUuid = zone.uuid
                isDefault = true
            }
        }
    }

    void testVirtualRouterOfferingQueryByOtherAccount() {
        L3NetworkInventory mgmtL3 = env.inventoryByName("pubL3-1")
        ImageInventory imageVr = env.inventoryByName("vr-image")
        VirtualRouterOfferingInventory vrOffering = env.inventoryByName("vr")

        SessionInventory test1AccountSession = logInByAccount {
            accountName = "test1"
            password = "password"
        }

        SessionInventory test2AccountSession = logInByAccount {
            accountName = "test2"
            password = "password"
        }

        List<VirtualRouterOfferingInventory> result1 = queryVirtualRouterOffering {
            sessionId = test1AccountSession.uuid
        }

        assert result1.size() == 0

        shareResource {
            delegate.accountUuids = [test1AccountSession.accountUuid]
            delegate.resourceUuids = [imageVr.uuid, mgmtL3.uuid]
        }

        createVirtualRouterOffering {
            delegate.name = "vr-2"
            delegate.memorySize = SizeUnit.MEGABYTE.toByte(512)
            delegate.cpuNum = 2
            delegate.managementNetworkUuid = mgmtL3.uuid
            delegate.publicNetworkUuid = mgmtL3.uuid
            delegate.imageUuid = imageVr.uuid
            delegate.zoneUuid = vrOffering.zoneUuid
            delegate.sessionId = test1AccountSession.uuid
        }

        result1 = queryVirtualRouterOffering { sessionId = test1AccountSession.uuid }

        assert result1.size() == 1

        List<VirtualRouterOfferingInventory> result2 = queryVirtualRouterOffering { sessionId = test2AccountSession.uuid }

        assert result2.size() == 0
    }
}
