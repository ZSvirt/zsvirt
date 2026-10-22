package org.zstack.test.integration.allocator.domain

import org.zstack.allocator.domain.header.AllocatorDomainConstant
import org.zstack.allocator.domain.header.AllocatorDomainErrors
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainInventory
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainRuleInventory
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainRuleParam
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class AllocatorDomainNegativeUpdateCase extends SubCase {
    EnvSpec env
    ZoneInventory zone1
    ZoneInventory zone2
    HostInventory host1
    HostInventory host2
    ClusterInventory cluster1
    ClusterInventory cluster2
    PrimaryStorageInventory ps1
    PrimaryStorageInventory ps2
    L3NetworkInventory l31
    L3NetworkInventory l32
    AllocatorDomainInventory domain
    String missingUuid = "11111111111111111111111111111111"

    @Override
    void setup() {
        useSpring(AllocatorDomainTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            sftpBackupStorage {
                name = "sftp1"
                url = "/sftp1"
                username = "root"
                password = "password"
                hostname = "localhost"
            }

            zone {
                name = "zone1"
                description = "zone1"

                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local1")
                    attachL2Network("l2-1")
                }

                localPrimaryStorage {
                    name = "local1"
                    url = "/local_ps1"
                }

                l2NoVlanNetwork {
                    name = "l2-1"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3-1"

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                attachBackupStorage("sftp1")
            }

            zone {
                name = "zone2"
                description = "zone2"

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local2")
                    attachL2Network("l2-2")
                }

                localPrimaryStorage {
                    name = "local2"
                    url = "/local_ps2"
                }

                l2NoVlanNetwork {
                    name = "l2-2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3-2"

                        ip {
                            startIp = "12.16.20.10"
                            endIp = "12.16.20.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.20.1"
                        }
                    }
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            prepare()
            testUpdateResourceNotFound()
            testUpdateResourceWrongType()
            testUpdateResourceNotInZone()
            testUpdateIllegalQualifier()
            testUpdateListLevelErrors()
        }
    }

    void prepare() {
        zone1 = env.inventoryByName("zone1") as ZoneInventory
        zone2 = env.inventoryByName("zone2") as ZoneInventory
        host1 = env.inventoryByName("kvm1") as HostInventory
        host2 = env.inventoryByName("kvm2") as HostInventory
        cluster1 = env.inventoryByName("cluster1") as ClusterInventory
        cluster2 = env.inventoryByName("cluster2") as ClusterInventory
        ps1 = env.inventoryByName("local1") as PrimaryStorageInventory
        ps2 = env.inventoryByName("local2") as PrimaryStorageInventory
        l31 = env.inventoryByName("l3-1") as L3NetworkInventory
        l32 = env.inventoryByName("l3-2") as L3NetworkInventory

        domain = createAllocatorDomain {
            delegate.name = "keep-name"
            delegate.description = "keep-desc"
            delegate.zoneUuid = zone1.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "HostVO", effect: "Include")
            ]
            delegate.scenes = ["Start"]
        } as AllocatorDomainInventory
    }

    void testUpdateResourceNotFound() {
        logger.info("Test 001: update name plus missing host uuid, expect API failure 3103 and unchanged domain")
        expectApiFailure({
            updateAllocatorDomain {
                delegate.uuid = domain.uuid
                delegate.name = "should-not-stick"
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: missingUuid, resourceType: "HostVO", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.HOST_NOT_FOUND.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.HOST_UUID] == missingUuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_TYPE] == "HostVO"
        }
        def after001s = queryAllocatorDomain { delegate.conditions = ["uuid=${domain.uuid}"] } as List<AllocatorDomainInventory>
        assert after001s.size() == 1
        def after001 = after001s[0]
        assert after001.name == "keep-name"
        assert after001.description == "keep-desc"
        assert after001.state == "Enabled"
        assert after001.zoneUuid == zone1.uuid
        assert after001.rules.size() == 1
        assert after001.rules[0] instanceof AllocatorDomainRuleInventory
        assert after001.rules[0].priority == 0
        assert after001.rules[0].resourceType == "HostVO"
        assert after001.rules[0].resourceUuid == host1.uuid
        assert after001.rules[0].effect == "Include"
        assert after001.scenes.size() == 1
        assert after001.scenes[0].scene == "Start"

        logger.info("Test 002: update name plus missing cluster uuid, expect API failure 3203 and unchanged domain")
        expectApiFailure({
            updateAllocatorDomain {
                delegate.uuid = domain.uuid
                delegate.name = "should-not-stick"
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: missingUuid, resourceType: "ClusterVO", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.CLUSTER_NOT_FOUND.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.CLUSTER_UUID] == missingUuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_TYPE] == "ClusterVO"
        }
        def after002s = queryAllocatorDomain { delegate.conditions = ["uuid=${domain.uuid}"] } as List<AllocatorDomainInventory>
        assert after002s.size() == 1
        def after002 = after002s[0]
        assert after002.name == "keep-name"
        assert after002.description == "keep-desc"
        assert after002.state == "Enabled"
        assert after002.zoneUuid == zone1.uuid
        assert after002.rules.size() == 1
        assert after002.rules[0] instanceof AllocatorDomainRuleInventory
        assert after002.rules[0].priority == 0
        assert after002.rules[0].resourceType == "HostVO"
        assert after002.rules[0].resourceUuid == host1.uuid
        assert after002.rules[0].effect == "Include"
        assert after002.scenes.size() == 1
        assert after002.scenes[0].scene == "Start"

        logger.info("Test 003: update name plus missing primary storage uuid, expect API failure 3303 and unchanged domain")
        expectApiFailure({
            updateAllocatorDomain {
                delegate.uuid = domain.uuid
                delegate.name = "should-not-stick"
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: missingUuid, resourceType: "PrimaryStorageVO", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.PRIMARY_STORAGE_NOT_FOUND.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.PRIMARY_STORAGE_UUID] == missingUuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_TYPE] == "PrimaryStorageVO"
        }
        def after003s = queryAllocatorDomain { delegate.conditions = ["uuid=${domain.uuid}"] } as List<AllocatorDomainInventory>
        assert after003s.size() == 1
        def after003 = after003s[0]
        assert after003.name == "keep-name"
        assert after003.description == "keep-desc"
        assert after003.state == "Enabled"
        assert after003.zoneUuid == zone1.uuid
        assert after003.rules.size() == 1
        assert after003.rules[0] instanceof AllocatorDomainRuleInventory
        assert after003.rules[0].priority == 0
        assert after003.rules[0].resourceType == "HostVO"
        assert after003.rules[0].resourceUuid == host1.uuid
        assert after003.rules[0].effect == "Include"
        assert after003.scenes.size() == 1
        assert after003.scenes[0].scene == "Start"

        logger.info("Test 004: update name plus missing l3 uuid, expect API failure 3403 and unchanged domain")
        expectApiFailure({
            updateAllocatorDomain {
                delegate.uuid = domain.uuid
                delegate.name = "should-not-stick"
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: missingUuid, resourceType: "L3NetworkVO", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.L3_NOT_FOUND.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.L3_UUID] == missingUuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_TYPE] == "L3NetworkVO"
        }
        def after004s = queryAllocatorDomain { delegate.conditions = ["uuid=${domain.uuid}"] } as List<AllocatorDomainInventory>
        assert after004s.size() == 1
        def after004 = after004s[0]
        assert after004.name == "keep-name"
        assert after004.description == "keep-desc"
        assert after004.state == "Enabled"
        assert after004.zoneUuid == zone1.uuid
        assert after004.rules.size() == 1
        assert after004.rules[0] instanceof AllocatorDomainRuleInventory
        assert after004.rules[0].priority == 0
        assert after004.rules[0].resourceType == "HostVO"
        assert after004.rules[0].resourceUuid == host1.uuid
        assert after004.rules[0].effect == "Include"
        assert after004.scenes.size() == 1
        assert after004.scenes[0].scene == "Start"
    }

    void testUpdateResourceWrongType() {
        logger.info("Test 101: update name plus cluster uuid as HostVO, expect API failure 3103 and unchanged domain")
        expectApiFailure({
            updateAllocatorDomain {
                delegate.uuid = domain.uuid
                delegate.name = "should-not-stick"
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: cluster1.uuid, resourceType: "HostVO", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.HOST_NOT_FOUND.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.HOST_UUID] == cluster1.uuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_TYPE] == "HostVO"
        }
        def after101s = queryAllocatorDomain { delegate.conditions = ["uuid=${domain.uuid}"] } as List<AllocatorDomainInventory>
        assert after101s.size() == 1
        def after101 = after101s[0]
        assert after101.name == "keep-name"
        assert after101.description == "keep-desc"
        assert after101.state == "Enabled"
        assert after101.zoneUuid == zone1.uuid
        assert after101.rules.size() == 1
        assert after101.rules[0] instanceof AllocatorDomainRuleInventory
        assert after101.rules[0].priority == 0
        assert after101.rules[0].resourceType == "HostVO"
        assert after101.rules[0].resourceUuid == host1.uuid
        assert after101.rules[0].effect == "Include"
        assert after101.scenes.size() == 1
        assert after101.scenes[0].scene == "Start"
    }

    void testUpdateResourceNotInZone() {
        logger.info("Test 201: update name plus cluster in another zone, expect API failure 3201 and unchanged domain")
        expectApiFailure({
            updateAllocatorDomain {
                delegate.uuid = domain.uuid
                delegate.name = "should-not-stick"
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: cluster2.uuid, resourceType: "ClusterVO", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.CLUSTER_NOT_IN_ZONE.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.CLUSTER_UUID] == cluster2.uuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.ZONE_UUID] == zone1.uuid
        }
        def after201s = queryAllocatorDomain { delegate.conditions = ["uuid=${domain.uuid}"] } as List<AllocatorDomainInventory>
        assert after201s.size() == 1
        def after201 = after201s[0]
        assert after201.name == "keep-name"
        assert after201.description == "keep-desc"
        assert after201.state == "Enabled"
        assert after201.zoneUuid == zone1.uuid
        assert after201.rules.size() == 1
        assert after201.rules[0] instanceof AllocatorDomainRuleInventory
        assert after201.rules[0].priority == 0
        assert after201.rules[0].resourceType == "HostVO"
        assert after201.rules[0].resourceUuid == host1.uuid
        assert after201.rules[0].effect == "Include"
        assert after201.scenes.size() == 1
        assert after201.scenes[0].scene == "Start"

        logger.info("Test 202: update name plus host in another zone, expect API failure 3101 and unchanged domain")
        expectApiFailure({
            updateAllocatorDomain {
                delegate.uuid = domain.uuid
                delegate.name = "should-not-stick"
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: host2.uuid, resourceType: "HostVO", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.HOST_NOT_IN_ZONE.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.HOST_UUID] == host2.uuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.ZONE_UUID] == zone1.uuid
        }
        def after202s = queryAllocatorDomain { delegate.conditions = ["uuid=${domain.uuid}"] } as List<AllocatorDomainInventory>
        assert after202s.size() == 1
        def after202 = after202s[0]
        assert after202.name == "keep-name"
        assert after202.description == "keep-desc"
        assert after202.state == "Enabled"
        assert after202.zoneUuid == zone1.uuid
        assert after202.rules.size() == 1
        assert after202.rules[0] instanceof AllocatorDomainRuleInventory
        assert after202.rules[0].priority == 0
        assert after202.rules[0].resourceType == "HostVO"
        assert after202.rules[0].resourceUuid == host1.uuid
        assert after202.rules[0].effect == "Include"
        assert after202.scenes.size() == 1
        assert after202.scenes[0].scene == "Start"

        logger.info("Test 203: update name plus primary storage in another zone, expect API failure 3301 and unchanged domain")
        expectApiFailure({
            updateAllocatorDomain {
                delegate.uuid = domain.uuid
                delegate.name = "should-not-stick"
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: ps2.uuid, resourceType: "PrimaryStorageVO", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.PRIMARY_STORAGE_NOT_IN_ZONE.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.PRIMARY_STORAGE_UUID] == ps2.uuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.ZONE_UUID] == zone1.uuid
        }
        def after203s = queryAllocatorDomain { delegate.conditions = ["uuid=${domain.uuid}"] } as List<AllocatorDomainInventory>
        assert after203s.size() == 1
        def after203 = after203s[0]
        assert after203.name == "keep-name"
        assert after203.description == "keep-desc"
        assert after203.state == "Enabled"
        assert after203.zoneUuid == zone1.uuid
        assert after203.rules.size() == 1
        assert after203.rules[0] instanceof AllocatorDomainRuleInventory
        assert after203.rules[0].priority == 0
        assert after203.rules[0].resourceType == "HostVO"
        assert after203.rules[0].resourceUuid == host1.uuid
        assert after203.rules[0].effect == "Include"
        assert after203.scenes.size() == 1
        assert after203.scenes[0].scene == "Start"

        logger.info("Test 204: update name plus l3 in another zone, expect API failure 3401 and unchanged domain")
        expectApiFailure({
            updateAllocatorDomain {
                delegate.uuid = domain.uuid
                delegate.name = "should-not-stick"
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: l32.uuid, resourceType: "L3NetworkVO", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.L3_NOT_IN_ZONE.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.L3_UUID] == l32.uuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.ZONE_UUID] == zone1.uuid
        }
        def after204s = queryAllocatorDomain { delegate.conditions = ["uuid=${domain.uuid}"] } as List<AllocatorDomainInventory>
        assert after204s.size() == 1
        def after204 = after204s[0]
        assert after204.name == "keep-name"
        assert after204.description == "keep-desc"
        assert after204.state == "Enabled"
        assert after204.zoneUuid == zone1.uuid
        assert after204.rules.size() == 1
        assert after204.rules[0] instanceof AllocatorDomainRuleInventory
        assert after204.rules[0].priority == 0
        assert after204.rules[0].resourceType == "HostVO"
        assert after204.rules[0].resourceUuid == host1.uuid
        assert after204.rules[0].effect == "Include"
        assert after204.scenes.size() == 1
        assert after204.scenes[0].scene == "Start"
    }

    void testUpdateIllegalQualifier() {
        logger.info("Test 301: update name plus host resourceQualifier, expect API failure 3102 and unchanged domain")
        expectApiFailure({
            updateAllocatorDomain {
                delegate.uuid = domain.uuid
                delegate.name = "should-not-stick"
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "HostVO", resourceQualifier: "Root", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.HOST_QUALIFIER_NOT_ALLOWED.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.HOST_UUID] == host1.uuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_QUALIFIER] == "Root"
        }
        def after301s = queryAllocatorDomain { delegate.conditions = ["uuid=${domain.uuid}"] } as List<AllocatorDomainInventory>
        assert after301s.size() == 1
        def after301 = after301s[0]
        assert after301.name == "keep-name"
        assert after301.description == "keep-desc"
        assert after301.state == "Enabled"
        assert after301.zoneUuid == zone1.uuid
        assert after301.rules.size() == 1
        assert after301.rules[0] instanceof AllocatorDomainRuleInventory
        assert after301.rules[0].priority == 0
        assert after301.rules[0].resourceType == "HostVO"
        assert after301.rules[0].resourceUuid == host1.uuid
        assert after301.rules[0].effect == "Include"
        assert after301.scenes.size() == 1
        assert after301.scenes[0].scene == "Start"

        logger.info("Test 302: update name plus illegal primary storage resourceQualifier, expect API failure 3302 and unchanged domain")
        expectApiFailure({
            updateAllocatorDomain {
                delegate.uuid = domain.uuid
                delegate.name = "should-not-stick"
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: ps1.uuid, resourceType: "PrimaryStorageVO", resourceQualifier: "Cache", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.PRIMARY_STORAGE_INVALID_QUALIFIER.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.PRIMARY_STORAGE_UUID] == ps1.uuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_QUALIFIER] == "Cache"
        }
        def after302s = queryAllocatorDomain { delegate.conditions = ["uuid=${domain.uuid}"] } as List<AllocatorDomainInventory>
        assert after302s.size() == 1
        def after302 = after302s[0]
        assert after302.name == "keep-name"
        assert after302.description == "keep-desc"
        assert after302.state == "Enabled"
        assert after302.zoneUuid == zone1.uuid
        assert after302.rules.size() == 1
        assert after302.rules[0] instanceof AllocatorDomainRuleInventory
        assert after302.rules[0].priority == 0
        assert after302.rules[0].resourceType == "HostVO"
        assert after302.rules[0].resourceUuid == host1.uuid
        assert after302.rules[0].effect == "Include"
        assert after302.scenes.size() == 1
        assert after302.scenes[0].scene == "Start"
    }

    void testUpdateListLevelErrors() {
        logger.info("Test 401: update name plus Include and Exclude on the same selector, expect API failure 3002 and unchanged domain")
        expectApiFailure({
            updateAllocatorDomain {
                delegate.uuid = domain.uuid
                delegate.name = "should-not-stick"
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "HostVO", effect: "Include"),
                        new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "HostVO", effect: "Exclude")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.CONFLICTING_INCLUDE_EXCLUDE.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_TYPE] == "HostVO"
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_UUID] == host1.uuid
        }
        def after401s = queryAllocatorDomain { delegate.conditions = ["uuid=${domain.uuid}"] } as List<AllocatorDomainInventory>
        assert after401s.size() == 1
        def after401 = after401s[0]
        assert after401.name == "keep-name"
        assert after401.description == "keep-desc"
        assert after401.state == "Enabled"
        assert after401.zoneUuid == zone1.uuid
        assert after401.rules.size() == 1
        assert after401.rules[0] instanceof AllocatorDomainRuleInventory
        assert after401.rules[0].priority == 0
        assert after401.rules[0].resourceType == "HostVO"
        assert after401.rules[0].resourceUuid == host1.uuid
        assert after401.rules[0].effect == "Include"
        assert after401.scenes.size() == 1
        assert after401.scenes[0].scene == "Start"

        logger.info("Test 402: update name plus duplicate full-tuple rule, expect API failure 3003 and unchanged domain")
        expectApiFailure({
            updateAllocatorDomain {
                delegate.uuid = domain.uuid
                delegate.name = "should-not-stick"
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "HostVO", effect: "Include"),
                        new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "HostVO", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.DUPLICATE_RULE.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_TYPE] == "HostVO"
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_UUID] == host1.uuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.EFFECT] == "Include"
        }
        def after402s = queryAllocatorDomain { delegate.conditions = ["uuid=${domain.uuid}"] } as List<AllocatorDomainInventory>
        assert after402s.size() == 1
        def after402 = after402s[0]
        assert after402.name == "keep-name"
        assert after402.description == "keep-desc"
        assert after402.state == "Enabled"
        assert after402.zoneUuid == zone1.uuid
        assert after402.rules.size() == 1
        assert after402.rules[0] instanceof AllocatorDomainRuleInventory
        assert after402.rules[0].priority == 0
        assert after402.rules[0].resourceType == "HostVO"
        assert after402.rules[0].resourceUuid == host1.uuid
        assert after402.rules[0].effect == "Include"
        assert after402.scenes.size() == 1
        assert after402.scenes[0].scene == "Start"
    }

    @Override
    void clean() {
        queryAllocatorDomain {}.each { AllocatorDomainInventory domain ->
            if (domain != null) {
                deleteAllocatorDomain {
                    delegate.uuid = domain.uuid
                }
            }
        }
        env.delete()
    }
}
