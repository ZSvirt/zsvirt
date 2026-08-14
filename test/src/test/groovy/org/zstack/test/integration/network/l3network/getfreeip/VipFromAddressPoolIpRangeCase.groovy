package org.zstack.test.integration.network.l3network.getfreeip

import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.sdk.*
import org.zstack.testlib.SubCase
import org.zstack.utils.network.IPv6Constants
import org.zstack.utils.data.SizeUnit

class VipFromAddressPoolIpRangeCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(2)
                cpu = 2
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
                        totalCpu = 40
                        totalMem = SizeUnit.GIGABYTE.toByte(32)
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
                        category = "Public"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.HostRoute.toString(),
                                     UserdataConstant.USERDATA_TYPE_STRING]
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
            AllocateIpRequiredIpRangeRequiredIp();
            AllocateIpRequiredIpRangeNotRequiredIp();
            AllocateIpNotRequiredIpRangeRequiredIp();
            AllocateIpNotRequiredIpRangeNotRequiredIp();
            VmDoNotUseAddressPoolIp();
        }
    }

    void AllocateIpRequiredIpRangeRequiredIp() {
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        IpRangeInventory ipr4_pool = addPoolIpRangesToL3Network(l3)
        IpRangeInventory ipr4_normal = addNormalIpRangesToL3Network(l3)

        VipInventory vip1 = createVip {
            name = "vip1"
            l3NetworkUuid = l3.uuid
            ipRangeUuid = ipr4_pool.uuid
            requiredIp = "192.168.0.2"
        }
        EipInventory eip1 = createEip {
            name = "eip1"
            vipUuid = vip1.uuid
        }

        List<FreeIpInventory> ipv4FreeIps = getFreeIp {
            l3NetworkUuid = l3.getUuid()
            ipRangeUuid = ipr4_normal.uuid
            ipVersion = IPv6Constants.IPv4
            limit = 1
        } as List<FreeIpInventory>
        FreeIpInventory ip = ipv4FreeIps.get(0)
        VipInventory vip2 = createVip {
            name = "vip2"
            l3NetworkUuid = l3.uuid
            ipRangeUuid = ipr4_normal.uuid
            requiredIp = ip.ip
        }
        EipInventory eip2 = createEip {
            name = "eip2"
            vipUuid = vip2.uuid
        }

        deleteIpRangeFromL3Network(ipr4_pool.uuid, ipr4_normal.uuid)
    }

    void AllocateIpRequiredIpRangeNotRequiredIp() {
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        IpRangeInventory ipr4_pool = addPoolIpRangesToL3Network(l3)
        IpRangeInventory ipr4_normal = addNormalIpRangesToL3Network(l3)

        VipInventory vip1 = createVip {
            name = "vip1"
            l3NetworkUuid = l3.uuid
            ipRangeUuid = ipr4_pool.uuid
        }
        EipInventory eip1 = createEip {
            name = "eip1"
            vipUuid = vip1.uuid
        }

        VipInventory vip2 = createVip {
            name = "vip2"
            l3NetworkUuid = l3.uuid
            ipRangeUuid = ipr4_normal.uuid
        }
        EipInventory eip2 = createEip {
            name = "eip2"
            vipUuid = vip2.uuid
        }

        deleteIpRangeFromL3Network(ipr4_pool.uuid, ipr4_normal.uuid)
    }

    void AllocateIpNotRequiredIpRangeRequiredIp()  {
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        IpRangeInventory ipr4_pool = addPoolIpRangesToL3Network(l3)
        IpRangeInventory ipr4_normal = addNormalIpRangesToL3Network(l3)

        List<FreeIpInventory> ipv4FreeIps = getFreeIp {
            l3NetworkUuid = l3.getUuid()
            ipVersion = IPv6Constants.IPv4
            ipRangeType = IpRangeType.Normal.toString()
            limit = 1
        } as List<FreeIpInventory>
        FreeIpInventory ip = ipv4FreeIps.get(0)
        VipInventory vip1 = createVip {
            name = "vip1"
            l3NetworkUuid = l3.uuid
            requiredIp = ip.ip
        }
        EipInventory eip1 = createEip {
            name = "eip1"
            vipUuid = vip1.uuid
        }

        expect(AssertionError.class) {
            VipInventory vip2 = createVip {
                name = "vip2"
                l3NetworkUuid = l3.uuid
                requiredIp = ip.ip
            }
        }

        deleteIpRangeFromL3Network(ipr4_pool.uuid, ipr4_normal.uuid)
    }

    void AllocateIpNotRequiredIpRangeNotRequiredIp() {
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        IpRangeInventory ipr4_pool = addPoolIpRangesToL3Network(l3)
        IpRangeInventory ipr4_normal = addNormalIpRangesToL3Network(l3)

        VipInventory vip1 = createVip {
            name = "vip1"
            l3NetworkUuid = l3.uuid
        }
        EipInventory eip1 = createEip {
            name = "eip1"
            vipUuid = vip1.uuid
        }

        expect(AssertionError.class) {
            VipInventory vip2 = createVip {
                name = "vip2"
                l3NetworkUuid = l3.uuid
            }
        }

        deleteIpRangeFromL3Network(ipr4_pool.uuid, ipr4_normal.uuid)
    }

    void VmDoNotUseAddressPoolIp() {
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        IpRangeInventory ipr4_pool = addPoolIpRangesToL3Network(l3)
        IpRangeInventory ipr4_normal = addNormalIpRangesToL3Network(l3)

        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }

        //assert(vm2 can not be created because of no available normal range ip, address pool will not be used)
        expect(AssertionError.class) {
            createVmInstance {
                name = "vm"
                instanceOfferingUuid = instanceOffering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
            }
        }
        deleteIpRangeFromL3Network(ipr4_pool.uuid, ipr4_normal.uuid)
    }

    IpRangeInventory addPoolIpRangesToL3Network(L3NetworkInventory l3) {
        IpRangeInventory ipr4_pool =  addIpRange {
            name = "address-pool"
            l3NetworkUuid = l3.uuid
            startIp = "192.168.0.1"
            endIp = "192.168.0.20"
            netmask = "255.255.255.0"
            ipRangeType = IpRangeType.AddressPool.toString()
        }
        return ipr4_pool
    }

    IpRangeInventory addNormalIpRangesToL3Network(L3NetworkInventory l3) {
        IpRangeInventory ipr4_normal = addIpRange {
            name = "ipr-4"
            l3NetworkUuid = l3.uuid
            startIp = "192.168.1.2"
            endIp = "192.168.1.3"
            gateway = "192.168.1.1"
            netmask = "255.255.255.0"
        }
        return ipr4_normal
    }

    void deleteIpRangeFromL3Network(String poolIpRangeUuid, String normalIpRangeUuid) {
        deleteIpRange { uuid = poolIpRangeUuid }
        deleteIpRange { uuid = normalIpRangeUuid }
    }

    @Override
    void clean() {
        env.delete()
    }
}


