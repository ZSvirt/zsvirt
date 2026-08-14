package org.zstack.test.integration.network.l3network.getfreeip

import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.identity.AccountConstant
import org.zstack.network.l3.L3NetworkGlobalConfig
import org.zstack.header.network.l3.L3NetworkConstant
import org.zstack.sdk.GetIpAddressCapacityResult
import org.zstack.sdk.FreeIpInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.header.network.l3.IpRangeVO
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.IpRangeSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.sdk.*
import org.zstack.testlib.SubCase
import org.zstack.utils.network.NetworkUtils
import org.zstack.utils.network.IPv6Constants

class StripNetworkAndBroadcastAddressCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = "zone"

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        category = "Public"
                        name = "l3"
                    }
                }
            }
        }
    }


    @Override
    void test() {
        env.create {

            def l3 = env.inventoryByName("l3") as L3NetworkInventory
            def zone = env.inventoryByName("zone") as ZoneInventory
            DatabaseFacade dbf = bean(DatabaseFacade.class)
            updateResourceConfig {
                name = L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.name
                category= L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.category
                resourceUuid= l3.uuid
                value = L3NetworkConstant.FIRST_AVAILABLE_IP_ALLOCATOR_STRATEGY
            }

            IpRangeVO fakeIpr_1 = new IpRangeVO()
            fakeIpr_1.uuid = Platform.uuid
            fakeIpr_1.startIp = '192.168.10.0'
            fakeIpr_1.endIp = '192.168.10.255'
            fakeIpr_1.gateway = '192.168.10.1'
            fakeIpr_1.netmask = '255.255.255.0'
            fakeIpr_1.ipVersion = 4
            fakeIpr_1.l3NetworkUuid = l3.uuid
            fakeIpr_1.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
            dbf.persist(fakeIpr_1)
            expect(AssertionError.class) {
                createVip {
                    name = "address-pool-vip"
                    l3NetworkUuid = l3.uuid
                    ipRangeUuid = fakeIpr_1.uuid
                    requiredIp = "192.168.10.0"
                }
            }
            expect(AssertionError.class) {
                createVip {
                    name = "address-pool-vip"
                    l3NetworkUuid = l3.uuid
                    ipRangeUuid = fakeIpr_1.uuid
                    requiredIp = "192.168.10.255"
                }
            }
            List<FreeIpInventory> freeIps = getFreeIpOfIpRange {
                ipRangeUuid = fakeIpr_1.uuid
                limit = 260
            }
            assert freeIps.size() == 254
            assert freeIps[0].ip == "192.168.10.1"
            VipInventory vip1 = createVip {
                name = "address-pool-vip"
                l3NetworkUuid = l3.uuid
                ipRangeUuid = fakeIpr_1.uuid
            }
            assert vip1.ip == "192.168.10.1"
            GetIpAddressCapacityResult result = getIpAddressCapacity {
                l3NetworkUuids = [l3.uuid]
            }
            assert result.availableCapacity == 253
            result = getIpAddressCapacity {
                ipRangeUuids = [fakeIpr_1.uuid]
            }
            assert result.availableCapacity == 253
            result = getIpAddressCapacity {
                zoneUuids = [zone.uuid]
            }
            assert result.availableCapacity == 253
            deleteIpRange { uuid = fakeIpr_1.uuid }

            //startIp == endIp == network address
            IpRangeVO fakeIpr_2 = new IpRangeVO()
            fakeIpr_2.uuid = Platform.uuid
            fakeIpr_2.startIp = '192.168.10.0'
            fakeIpr_2.endIp = '192.168.10.0'
            fakeIpr_2.gateway = '192.168.10.0'
            fakeIpr_2.netmask = '255.255.255.0'
            fakeIpr_2.ipVersion = 4
            fakeIpr_2.l3NetworkUuid = l3.uuid
            fakeIpr_2.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
            dbf.persist(fakeIpr_2)
            freeIps = getFreeIpOfIpRange {
                ipRangeUuid = fakeIpr_2.uuid
                limit = 260
            }
            assert freeIps.size() == 0
            result = getIpAddressCapacity {
                l3NetworkUuids = [l3.uuid]
            }
            assert result.availableCapacity == 0
            result = getIpAddressCapacity {
                ipRangeUuids = [fakeIpr_2.uuid]
            }
            assert result.availableCapacity == 0
            result = getIpAddressCapacity {
                zoneUuids = [zone.uuid]
            }
            assert result.availableCapacity == 0
            expect(AssertionError.class) {
                createVip {
                    name = "address-pool-vip"
                    l3NetworkUuid = l3.uuid
                    ipRangeUuid = fakeIpr_2.uuid
                }
            }
            deleteIpRange { uuid = fakeIpr_2.uuid }

            //startIp == endIp == broadcast address
            IpRangeVO fakeIpr_3 = new IpRangeVO()
            fakeIpr_3.uuid = Platform.uuid
            fakeIpr_3.startIp = '192.168.10.255'
            fakeIpr_3.endIp = '192.168.10.255'
            fakeIpr_3.gateway = '192.168.10.0'
            fakeIpr_3.netmask = '255.255.255.0'
            fakeIpr_3.ipVersion = 4
            fakeIpr_3.l3NetworkUuid = l3.uuid
            fakeIpr_3.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
            dbf.persist(fakeIpr_3)
            freeIps = getFreeIpOfIpRange {
                ipRangeUuid = fakeIpr_3.uuid
                limit = 260
            }
            assert freeIps.size() == 0
            deleteIpRange { uuid = fakeIpr_3.uuid }

            expect(AssertionError.class) {
                addIpRange {
                    name = "address-pool"
                    l3NetworkUuid = l3.uuid
                    startIp = "100.64.101.0"
                    endIp = "100.64.101.255"
                    netmask = "255.255.255.0"
                    ipRangeType = IpRangeType.AddressPool.toString()
                }
            }

            expect(AssertionError.class) {
                addIpRange {
                    name = "address-pool"
                    l3NetworkUuid = l3.uuid
                    startIp = "100.64.101.0"
                    endIp = "100.64.101.10"
                    netmask = "255.255.255.0"
                    ipRangeType = IpRangeType.AddressPool.toString()
                }
            }

            expect(AssertionError.class) {
                addIpRange {
                    name = "address-pool"
                    l3NetworkUuid = l3.uuid
                    startIp = "100.64.101.240"
                    endIp = "100.64.101.255"
                    netmask = "255.255.255.0"
                    ipRangeType = IpRangeType.AddressPool.toString()
                }
            }

            //normal case
            //restore l3.ipVersion from 0 to 4
            IpRangeInventory ipr_normal = addIpRange {
                name = "normal"
                l3NetworkUuid = l3.uuid
                startIp = "192.168.1.1"
                endIp = "192.168.1.2"
                gateway = "192.168.1.3"
                netmask = "255.255.255.0"
                ipRangeType = IpRangeType.Normal.toString()
            }
            IpRangeInventory ipr_1 = addIpRange {
                name = "address-pool"
                l3NetworkUuid = l3.uuid
                startIp = "100.64.101.1"
                endIp = "100.64.101.254"
                netmask = "255.255.255.0"
                ipRangeType = IpRangeType.AddressPool.toString()
            }
            freeIps = getFreeIpOfIpRange {
                ipRangeUuid = ipr_1.uuid
                limit = 260
            }
            assert freeIps.size() == 254
            assert freeIps[0].ip == "100.64.101.1"
            IpRangeInventory ipr_2 = addIpRange {
                name = "address-pool"
                l3NetworkUuid = l3.uuid
                startIp = "100.64.102.1"
                endIp = "100.64.102.254"
                netmask = "255.255.255.0"
                ipRangeType = IpRangeType.AddressPool.toString()
            }
            freeIps = getFreeIpOfL3Network {
                l3NetworkUuid = l3.uuid
                limit = 1000
            }
            assert freeIps.size() == 2 + 254 + 254
            VipInventory vip2 = createVip {
                name = "address-pool-vip"
                ipRangeUuid = ipr_1.uuid
                l3NetworkUuid = l3.uuid
            }
            assert vip2.ip == "100.64.101.1"

            VipInventory vip3 = createVip {
                name = "normal-vip"
                l3NetworkUuid = l3.uuid
            }
            assert vip3.ip == "192.168.1.1"

            result = getIpAddressCapacity {
                ipRangeUuids = [ipr_normal.uuid]
            }
            assert result.availableCapacity == 2 - 1
            result = getIpAddressCapacity {
                l3NetworkUuids = [l3.uuid]
            }
            assert result.availableCapacity == (2 - 1) + (254 - 1) + 254
            result = getIpAddressCapacity {
                zoneUuids = [zone.uuid]
            }
            assert result.availableCapacity == (2 - 1) + (254 - 1) + 254
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
