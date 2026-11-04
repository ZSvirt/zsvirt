package org.zstack.test.integration.allocator.domain

import org.zstack.allocator.domain.entity.AllocatorDomainRuleVO
import org.zstack.allocator.domain.entity.AllocatorDomainRuleVO_
import org.zstack.allocator.domain.entity.AllocatorDomainSceneVO
import org.zstack.allocator.domain.entity.AllocatorDomainSceneVO_
import org.zstack.allocator.domain.entity.AllocatorDomainVO
import org.zstack.allocator.domain.header.AllocatorDomainState
import org.zstack.core.db.Q
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainInventory
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainRuleParam
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class AllocatorDomainCreateCase extends SubCase {
    EnvSpec env
    ZoneInventory zone
    HostInventory host
    ClusterInventory cluster
    AllocatorDomainInventory placeholder
    AllocatorDomainInventory domain

    @Override
    void setup() {
        useSpring(AllocatorDomainTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"
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

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
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
            prepare()
            testCreatePlaceholderDomain()
            testCreateDomainWithRulesAndScenes()
            testUpdateNameDescriptionState()
            testRewriteRules()
            testRewriteScenes()
            testUpdateOmittingRulesAndScenes()
            testDeleteUnusedDomains()
        }
    }

    void prepare() {
        zone = env.inventoryByName("zone") as ZoneInventory
        host = env.inventoryByName("kvm") as HostInventory
        cluster = env.inventoryByName("cluster") as ClusterInventory
    }

    void testCreatePlaceholderDomain() {
        logger.info("Test 001: create allocator domain without rules and scenes, expect success")
        placeholder = createAllocatorDomain {
            delegate.name = "placeholder-domain"
            delegate.description = "placeholder"
            delegate.zoneUuid = zone.uuid
        } as AllocatorDomainInventory
        assert placeholder.uuid != null
        assert placeholder.name == "placeholder-domain"
        assert placeholder.description == "placeholder"
        assert placeholder.zoneUuid == zone.uuid
        assert placeholder.state == AllocatorDomainState.Enabled.toString()
        assert (placeholder.rules ?: []).isEmpty()
        assert (placeholder.scenes ?: []).isEmpty()

        AllocatorDomainVO vo = dbFindByUuid(placeholder.uuid, AllocatorDomainVO.class)
        assert vo != null
        assert vo.name == "placeholder-domain"
        assert vo.description == "placeholder"
        assert vo.zoneUuid == zone.uuid
        assert vo.state == AllocatorDomainState.Enabled

        logger.info("Test 002: query placeholder domain, expect empty rules and scenes persisted")
        AllocatorDomainInventory queried = queryDomain(placeholder.uuid)
        assert queried.name == "placeholder-domain"
        assert queried.description == "placeholder"
        assert queried.zoneUuid == zone.uuid
        assert queried.state == AllocatorDomainState.Enabled.toString()
        assert (queried.rules ?: []).isEmpty()
        assert (queried.scenes ?: []).isEmpty()
    }

    void testCreateDomainWithRulesAndScenes() {
        logger.info("Test 101: create allocator domain with host/cluster Include and all scenes, expect priority 0,1")
        domain = createAllocatorDomain {
            delegate.name = "domain-with-rules"
            delegate.description = "with rules"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: host.uuid, resourceType: "HostVO", effect: "Include"),
                    new AllocatorDomainRuleParam(resourceUuid: cluster.uuid, resourceType: "ClusterVO", effect: "Include")
            ]
            delegate.scenes = ["NewCreate", "Start", "Migrate", "Ha", "HostMaintenance", "Drs"]
        } as AllocatorDomainInventory
        assert domain.uuid != null
        assert domain.name == "domain-with-rules"
        assert domain.description == "with rules"
        assert domain.zoneUuid == zone.uuid
        assert domain.state == AllocatorDomainState.Enabled.toString()
        assertRules(domain, [host.uuid, cluster.uuid], ["HostVO", "ClusterVO"])
        assertScenes(domain, ["NewCreate", "Start", "Migrate", "Ha", "HostMaintenance", "Drs"])

        AllocatorDomainVO vo = dbFindByUuid(domain.uuid, AllocatorDomainVO.class)
        assert vo != null
        assert vo.name == "domain-with-rules"
        assert vo.state == AllocatorDomainState.Enabled

        logger.info("Test 102: query domain, expect rules expanded and ordered by priority")
        AllocatorDomainInventory queried = queryDomain(domain.uuid)
        assert queried.name == "domain-with-rules"
        assertRules(queried, [host.uuid, cluster.uuid], ["HostVO", "ClusterVO"])
        assertScenes(queried, ["NewCreate", "Start", "Migrate", "Ha", "HostMaintenance", "Drs"])
    }

    void testUpdateNameDescriptionState() {
        logger.info("Test 201: update name, description and state, expect children unchanged")
        domain = updateAllocatorDomain {
            delegate.uuid = domain.uuid
            delegate.name = "domain-updated"
            delegate.description = "updated"
            delegate.state = AllocatorDomainState.Disabled.toString()
        } as AllocatorDomainInventory
        assert domain.name == "domain-updated"
        assert domain.description == "updated"
        assert domain.state == AllocatorDomainState.Disabled.toString()
        assert domain.zoneUuid == zone.uuid
        assertRules(domain, [host.uuid, cluster.uuid], ["HostVO", "ClusterVO"])
        assertScenes(domain, ["NewCreate", "Start", "Migrate", "Ha", "HostMaintenance", "Drs"])

        AllocatorDomainVO vo = dbFindByUuid(domain.uuid, AllocatorDomainVO.class)
        assert vo != null
        assert vo.name == "domain-updated"
        assert vo.description == "updated"
        assert vo.state == AllocatorDomainState.Disabled
        assert vo.zoneUuid == zone.uuid

        logger.info("Test 202: query after update, expect persisted name/state and unchanged children")
        AllocatorDomainInventory queried = queryDomain(domain.uuid)
        assert queried.name == "domain-updated"
        assert queried.description == "updated"
        assert queried.state == AllocatorDomainState.Disabled.toString()
        assert queried.zoneUuid == zone.uuid
        assertRules(queried, [host.uuid, cluster.uuid], ["HostVO", "ClusterVO"])
        assertScenes(queried, ["NewCreate", "Start", "Migrate", "Ha", "HostMaintenance", "Drs"])
    }

    void testRewriteRules() {
        logger.info("Test 301: rewrite rules in reverse order, expect priorities 0,1")
        domain = updateAllocatorDomain {
            delegate.uuid = domain.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: cluster.uuid, resourceType: "ClusterVO", effect: "Include"),
                    new AllocatorDomainRuleParam(resourceUuid: host.uuid, resourceType: "HostVO", effect: "Include")
            ]
        } as AllocatorDomainInventory
        assertRules(domain, [cluster.uuid, host.uuid], ["ClusterVO", "HostVO"])
        assertScenes(domain, ["NewCreate", "Start", "Migrate", "Ha", "HostMaintenance", "Drs"])

        logger.info("Test 302: query after rule rewrite, expect persisted order and unchanged scenes")
        AllocatorDomainInventory queried = queryDomain(domain.uuid)
        assertRules(queried, [cluster.uuid, host.uuid], ["ClusterVO", "HostVO"])
        assertScenes(queried, ["NewCreate", "Start", "Migrate", "Ha", "HostMaintenance", "Drs"])
    }

    void testRewriteScenes() {
        logger.info("Test 401: rewrite scenes to a subset, expect scene table matches list")
        domain = updateAllocatorDomain {
            delegate.uuid = domain.uuid
            delegate.scenes = ["Start", "NewCreate"]
        } as AllocatorDomainInventory
        assertScenes(domain, ["Start", "NewCreate"])
        assertRules(domain, [cluster.uuid, host.uuid], ["ClusterVO", "HostVO"])

        logger.info("Test 402: query after scene rewrite, expect persisted subset and unchanged rules")
        AllocatorDomainInventory queried = queryDomain(domain.uuid)
        assertScenes(queried, ["Start", "NewCreate"])
        assertRules(queried, [cluster.uuid, host.uuid], ["ClusterVO", "HostVO"])
    }

    void testUpdateOmittingRulesAndScenes() {
        logger.info("Test 501: update name without rules and scenes, expect children unchanged")
        domain = updateAllocatorDomain {
            delegate.uuid = domain.uuid
            delegate.name = "domain-name-only"
        } as AllocatorDomainInventory
        assert domain.name == "domain-name-only"
        assertRules(domain, [cluster.uuid, host.uuid], ["ClusterVO", "HostVO"])
        assertScenes(domain, ["Start", "NewCreate"])

        AllocatorDomainVO vo = dbFindByUuid(domain.uuid, AllocatorDomainVO.class)
        assert vo != null
        assert vo.name == "domain-name-only"

        logger.info("Test 502: query after name-only update, expect persisted name and unchanged children")
        AllocatorDomainInventory queried = queryDomain(domain.uuid)
        assert queried.name == "domain-name-only"
        assertRules(queried, [cluster.uuid, host.uuid], ["ClusterVO", "HostVO"])
        assertScenes(queried, ["Start", "NewCreate"])
    }

    void testDeleteUnusedDomains() {
        logger.info("Test 601: delete unused placeholder domain, expect it gone")
        deleteAllocatorDomain {
            delegate.uuid = placeholder.uuid
        }
        assert queryAllocatorDomain {
            delegate.conditions = ["uuid=${placeholder.uuid}"]
        }.isEmpty()
        assert dbFindByUuid(placeholder.uuid, AllocatorDomainVO.class) == null

        logger.info("Test 602: delete unused domain with rules and scenes, expect it and children gone")
        String domainUuid = domain.uuid
        deleteAllocatorDomain {
            delegate.uuid = domainUuid
        }
        assert queryAllocatorDomain {
            delegate.conditions = ["uuid=${domainUuid}"]
        }.isEmpty()
        assert dbFindByUuid(domainUuid, AllocatorDomainVO.class) == null
        assert !Q.New(AllocatorDomainRuleVO.class)
                .eq(AllocatorDomainRuleVO_.domainUuid, domainUuid)
                .isExists()
        assert !Q.New(AllocatorDomainSceneVO.class)
                .eq(AllocatorDomainSceneVO_.domainUuid, domainUuid)
                .isExists()

        logger.info("Test 699: query all allocator domains, expect empty")
        assert (queryAllocatorDomain {} as List<AllocatorDomainInventory>).isEmpty()
    }

    private static void assertRules(AllocatorDomainInventory inv, List<String> resourceUuids, List<String> resourceTypes) {
        assert inv.rules.size() == resourceUuids.size()
        resourceUuids.eachWithIndex { String resourceUuid, int index ->
            assert inv.rules[index].priority == index
            assert inv.rules[index].resourceUuid == resourceUuid
            assert inv.rules[index].resourceType == resourceTypes[index]
            assert inv.rules[index].effect == "Include"
        }
    }

    private static void assertScenes(AllocatorDomainInventory inv, List<String> expected) {
        assert inv.scenes.size() == expected.size()
        assert inv.scenes.collect { it.scene }.toSet() == expected.toSet()
    }

    private AllocatorDomainInventory queryDomain(String uuid) {
        List<AllocatorDomainInventory> inventories = queryAllocatorDomain {
            delegate.conditions = ["uuid=${uuid}"]
        } as List<AllocatorDomainInventory>
        assert inventories.size() == 1
        return inventories[0]
    }

    @Override
    void clean() {
        queryAllocatorDomain {}.each {
            deleteAllocatorDomain {
                delegate.uuid = it.uuid
            }
        }
        env.delete()
    }
}
