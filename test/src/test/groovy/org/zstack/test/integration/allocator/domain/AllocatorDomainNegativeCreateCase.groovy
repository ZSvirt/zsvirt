package org.zstack.test.integration.allocator.domain

import org.zstack.allocator.domain.header.AllocatorDomainConstant
import org.zstack.allocator.domain.header.AllocatorDomainErrors
import org.zstack.sdk.BackupStorageInventory
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

class AllocatorDomainNegativeCreateCase extends SubCase {
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
    BackupStorageInventory bs1
    BackupStorageInventory bs2
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

            sftpBackupStorage {
                name = "sftp2"
                url = "/sftp2"
                username = "root"
                password = "password"
                hostname = "127.0.0.3"
            }

            zone {
                name = "zone1"
                description = "zone1"

                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "localhost"
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

                attachBackupStorage("sftp2")
            }
        }
    }

    @Override
    void test() {
        env.create {
            prepare()
            testCreateResourceNotFound()
            testCreateResourceWrongType()
            testCreateResourceNotInZone()
            testCreateIllegalQualifier()
            testCreateListLevelErrors()
            testCreatePortGroupRewrite()
            testCreateInvalidSystemTagsRollback()
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
        bs1 = env.inventoryByName("sftp1") as BackupStorageInventory
        bs2 = env.inventoryByName("sftp2") as BackupStorageInventory
    }

    void testCreateResourceNotFound() {
        logger.info("Test 001: create with missing host uuid, expect API failure 3103 and no domain row")
        String name = "bad-create-host-missing"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
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
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()

        logger.info("Test 002: create with missing cluster uuid, expect API failure 3203 and no domain row")
        name = "bad-create-cluster-missing"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
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
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()

        logger.info("Test 003: create with missing primary storage uuid, expect API failure 3303 and no domain row")
        name = "bad-create-ps-missing"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
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
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()

        logger.info("Test 004: create with missing l3 uuid, expect API failure 3403 and no domain row")
        name = "bad-create-l3-missing"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
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
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()

        logger.info("Test 005: create with missing backup storage uuid, expect API failure 3503 and no domain row")
        name = "bad-create-bs-missing"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: missingUuid, resourceType: "BackupStorageVO", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.BACKUP_STORAGE_NOT_FOUND.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.BACKUP_STORAGE_UUID] == missingUuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_TYPE] == "BackupStorageVO"
        }
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()
    }

    void testCreateResourceWrongType() {
        logger.info("Test 101: create with cluster uuid as HostVO, expect API failure 3103 and no domain row")
        String name = "bad-create-host-wrong-type"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
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
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()

        logger.info("Test 102: create with host uuid as ClusterVO, expect API failure 3203 and no domain row")
        name = "bad-create-cluster-wrong-type"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "ClusterVO", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.CLUSTER_NOT_FOUND.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.CLUSTER_UUID] == host1.uuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_TYPE] == "ClusterVO"
        }
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()

        logger.info("Test 103: create with host uuid as PrimaryStorageVO, expect API failure 3303 and no domain row")
        name = "bad-create-ps-wrong-type"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "PrimaryStorageVO", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.PRIMARY_STORAGE_NOT_FOUND.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.PRIMARY_STORAGE_UUID] == host1.uuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_TYPE] == "PrimaryStorageVO"
        }
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()

        logger.info("Test 104: create with host uuid as L3NetworkVO, expect API failure 3403 and no domain row")
        name = "bad-create-l3-wrong-type"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "L3NetworkVO", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.L3_NOT_FOUND.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.L3_UUID] == host1.uuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_TYPE] == "L3NetworkVO"
        }
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()

        logger.info("Test 105: create with host uuid as BackupStorageVO, expect API failure 3503 and no domain row")
        name = "bad-create-bs-wrong-type"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "BackupStorageVO", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.BACKUP_STORAGE_NOT_FOUND.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.BACKUP_STORAGE_UUID] == host1.uuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_TYPE] == "BackupStorageVO"
        }
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()
    }

    void testCreateResourceNotInZone() {
        logger.info("Test 201: create with host in another zone, expect API failure 3101 and no domain row")
        String name = "bad-create-host-zone"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
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
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()

        logger.info("Test 202: create with cluster in another zone, expect API failure 3201 and no domain row")
        name = "bad-create-cluster-zone"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
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
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()

        logger.info("Test 203: create with primary storage in another zone, expect API failure 3301 and no domain row")
        name = "bad-create-ps-zone"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
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
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()

        logger.info("Test 204: create with l3 in another zone, expect API failure 3401 and no domain row")
        name = "bad-create-l3-zone"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
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
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()

        logger.info("Test 205: create with backup storage not attached to the zone, expect API failure 3501 and no domain row")
        name = "bad-create-bs-zone"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: bs2.uuid, resourceType: "BackupStorageVO", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.BACKUP_STORAGE_NOT_IN_ZONE.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.BACKUP_STORAGE_UUID] == bs2.uuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.ZONE_UUID] == zone1.uuid
        }
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()
    }

    void testCreateIllegalQualifier() {
        logger.info("Test 301: create host rule with resourceQualifier, expect API failure 3102 and no domain row")
        String name = "bad-create-host-qualifier"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
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
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()

        logger.info("Test 302: create cluster rule with resourceQualifier, expect API failure 3202 and no domain row")
        name = "bad-create-cluster-qualifier"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: cluster1.uuid, resourceType: "ClusterVO", resourceQualifier: "Root", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.CLUSTER_QUALIFIER_NOT_ALLOWED.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.CLUSTER_UUID] == cluster1.uuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_QUALIFIER] == "Root"
        }
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()

        logger.info("Test 303: create primary storage rule with illegal resourceQualifier, expect API failure 3302 and no domain row")
        name = "bad-create-ps-qualifier"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
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
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()

        logger.info("Test 304: create l3 rule with resourceQualifier, expect API failure 3402 and no domain row")
        name = "bad-create-l3-qualifier"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: l31.uuid, resourceType: "L3NetworkVO", resourceQualifier: "Root", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.L3_QUALIFIER_NOT_ALLOWED.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.L3_UUID] == l31.uuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_QUALIFIER] == "Root"
        }
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()

        logger.info("Test 305: create backup storage rule with resourceQualifier, expect API failure 3502 and no domain row")
        name = "bad-create-bs-qualifier"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: bs1.uuid, resourceType: "BackupStorageVO", resourceQualifier: "Root", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.BACKUP_STORAGE_QUALIFIER_NOT_ALLOWED.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.BACKUP_STORAGE_UUID] == bs1.uuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_QUALIFIER] == "Root"
        }
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()
    }

    void testCreateListLevelErrors() {
        logger.info("Test 401: create with Include and Exclude on the same selector, expect API failure 3002 and no domain row")
        String name = "bad-create-include-exclude"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
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
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()

        logger.info("Test 402: create with duplicate full-tuple rule, expect API failure 3003 and no domain row")
        name = "bad-create-duplicate"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
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
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()

        logger.info("Test 403: create with invalid nested effect, expect API failure 3004 and no domain row")
        name = "bad-create-effect"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "HostVO", effect: "Nope")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.INVALID_RULE_EFFECT.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.EFFECT] == "Nope"
        }
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()

        logger.info("Test 404: create with unknown resourceType, expect API failure 3001 and no domain row")
        name = "bad-create-unknown-type"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
                delegate.rules = [
                        new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "VmInstanceVO", effect: "Include")
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.INVALID_RULE.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_TYPE] == "VmInstanceVO"
            assert delegate.opaque[AllocatorDomainConstant.Opaque.RESOURCE_UUID] == host1.uuid
        }
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()
    }

    void testCreatePortGroupRewrite() {
        logger.info("Test 501: create with resourceType PortGroupVO and an existing L3 uuid, expect success persisted as L3NetworkVO")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "portgroup-rewrite"
            delegate.zoneUuid = zone1.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: l31.uuid, resourceType: "PortGroupVO", effect: "Include")
            ]
            delegate.scenes = ["Start"]
        } as AllocatorDomainInventory
        assert domain.uuid != null
        assert domain.name == "portgroup-rewrite"
        assert domain.zoneUuid == zone1.uuid
        assert domain.state == "Enabled"
        assert domain.rules.size() == 1
        assert domain.rules[0] instanceof AllocatorDomainRuleInventory
        assert domain.rules[0].priority == 0
        assert domain.rules[0].resourceType == "L3NetworkVO"
        assert domain.rules[0].resourceUuid == l31.uuid
        assert domain.rules[0].effect == "Include"
        assert domain.scenes.size() == 1
        assert domain.scenes[0].scene == "Start"

        logger.info("Test 502: query after PortGroupVO rewrite, expect L3NetworkVO persisted")
        AllocatorDomainInventory queried = queryAllocatorDomain {
            delegate.conditions = ["uuid=${domain.uuid}"]
        }[0]
        assert queried.name == "portgroup-rewrite"
        assert queried.description == null
        assert queried.zoneUuid == zone1.uuid
        assert queried.state == "Enabled"
        assert queried.rules.size() == 1
        assert queried.rules[0] instanceof AllocatorDomainRuleInventory
        assert queried.rules[0].priority == 0
        assert queried.rules[0].resourceType == "L3NetworkVO"
        assert queried.rules[0].resourceUuid == l31.uuid
        assert queried.rules[0].effect == "Include"
        assert queried.scenes.size() == 1
        assert queried.scenes[0].scene == "Start"
    }

    void testCreateInvalidSystemTagsRollback() {
        logger.info("Test 601: create with unmatched systemTags, expect API failure and Flow rollback with no domain row")
        String name = "bad-create-system-tags"
        expectApiFailure({
            createAllocatorDomain {
                delegate.name = name
                delegate.zoneUuid = zone1.uuid
                delegate.systemTags = ["schedulingDomain::${missingUuid}".toString()]
            }
        }) {
            assert delegate.code != null
            assert delegate.opaque != null || delegate.details != null
        }
        assert queryAllocatorDomain { delegate.conditions = ["name=${name}"] }.isEmpty()
    }

    @Override
    void clean() {
        env.delete()
    }
}
