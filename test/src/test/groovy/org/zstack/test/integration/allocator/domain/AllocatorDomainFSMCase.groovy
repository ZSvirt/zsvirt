package org.zstack.test.integration.allocator.domain

import org.zstack.allocator.domain.header.AllocatorDomainState
import org.zstack.header.identity.AccountConstant
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.AccountResourceRefInventory
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainInventory
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainRuleInventory
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainRuleParam
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainSceneInventory
import org.zstack.testlib.FSMCase

class AllocatorDomainFSMCase extends FSMCase {
    SessionInventory adminSession
    SessionInventory user1Session
    SessionInventory user2Session
    String user1AccountUuid
    String user2AccountUuid
    ZoneInventory zone
    HostInventory host
    ClusterInventory cluster

    @Override
    void setup() {
        useSpring(AllocatorDomainTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
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
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"
                }
            }
        }

        afterEnvCreate {
            prepareAccountsAndSessions()
        }

        beforeEnvDelete {
            env.session = adminSession
            (queryAllocatorDomain {
                delegate.sessionId = adminSession.uuid
            } as List<AllocatorDomainInventory>)?.each { AllocatorDomainInventory d ->
                deleteAllocatorDomain {
                    delegate.uuid = d.uuid
                    delegate.sessionId = adminSession.uuid
                }
            }
            if (user1AccountUuid != null) {
                deleteAccount {
                    delegate.uuid = user1AccountUuid
                    delegate.sessionId = adminSession.uuid
                }
                user1AccountUuid = null
            }
            if (user2AccountUuid != null) {
                deleteAccount {
                    delegate.uuid = user2AccountUuid
                    delegate.sessionId = adminSession.uuid
                }
                user2AccountUuid = null
            }
        }

        state("init") {
            markAsInitialState()
            checker {
                assertNoAllocatorDomain()
            }
        }

        state("userOwned") {
            checker {
                assertOwnedDomain(user1Session, user1AccountUuid)
            }
        }

        state("adminOwned") {
            checker {
                assertOwnedDomain(adminSession, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
            }
        }

        action("user create") {
            run = { createByUser() }
            registerEvent("init", "userOwned")
        }

        action("admin create") {
            run = { createByAdmin() }
            registerEvent("init", "adminOwned")
        }

        action("user update") {
            run = { updateByUser() }
            registerEvent("userOwned", "userOwned")
        }

        action("admin update") {
            run = { updateByAdmin() }
            registerEvent("userOwned", "userOwned")
            registerEvent("adminOwned", "adminOwned")
        }

        action("user delete") {
            run = { deleteByUser() }
            registerEvent("userOwned", "init")
        }

        action("admin delete") {
            run = { deleteByAdmin() }
            registerEvent("userOwned", "init")
            registerEvent("adminOwned", "init")
        }
    }

    void prepareAccountsAndSessions() {
        adminSession = env.session
        zone = env.inventoryByName("zone") as ZoneInventory
        host = env.inventoryByName("kvm") as HostInventory
        cluster = env.inventoryByName("cluster") as ClusterInventory

        def user1 = createAccount {
            delegate.name = "user1"
            delegate.password = "password"
            delegate.sessionId = adminSession.uuid
        } as AccountInventory
        def user2 = createAccount {
            delegate.name = "user2"
            delegate.password = "password"
            delegate.sessionId = adminSession.uuid
        } as AccountInventory
        user1AccountUuid = user1.uuid
        user2AccountUuid = user2.uuid

        attachPredefineRoles(user1.uuid, "allocator.domain")

        shareResource {
            delegate.resourceUuids = [zone.uuid]
            delegate.accountUuids = [user1.uuid]
            delegate.sessionId = adminSession.uuid
        }

        user1Session = logInByAccount {
            delegate.accountName = "user1"
            delegate.password = "password"
        } as SessionInventory
        user2Session = logInByAccount {
            delegate.accountName = "user2"
            delegate.password = "password"
        } as SessionInventory

        env.session = user1Session
    }

    void createByUser() {
        rememberDomain(createAllocatorDomain {
            delegate.name = "user-domain"
            delegate.description = "keep-desc"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: host.uuid, resourceType: "HostVO", effect: "Include")
            ]
            delegate.scenes = ["Start"]
        } as AllocatorDomainInventory, user1AccountUuid, "user-domain", "keep-desc", "HostVO", host.uuid, "Include")
    }

    void createByAdmin() {
        rememberDomain(createAllocatorDomain {
            delegate.name = "admin-domain"
            delegate.description = "keep-desc"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: host.uuid, resourceType: "HostVO", effect: "Include")
            ]
            delegate.scenes = ["Start"]
            delegate.sessionId = adminSession.uuid
        } as AllocatorDomainInventory, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, "admin-domain", "keep-desc", "HostVO", host.uuid, "Include")
    }

    void updateByUser() {
        def domainUuid = getNotNull("domain.uuid") as String
        updateAllocatorDomain {
            delegate.uuid = domainUuid
            delegate.name = "user-updated"
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: cluster.uuid, resourceType: "ClusterVO", effect: "Include")
            ]
        }
        save("expected.name", "user-updated")
        save("expected.resource.type", "ClusterVO")
        save("expected.resource.uuid", cluster.uuid)
        save("expected.effect", "Include")
    }

    void updateByAdmin() {
        def domainUuid = getNotNull("domain.uuid") as String
        updateAllocatorDomain {
            delegate.uuid = domainUuid
            delegate.name = "admin-updated"
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: host.uuid, resourceType: "HostVO", effect: "Include")
            ]
            delegate.sessionId = adminSession.uuid
        }
        save("expected.name", "admin-updated")
        save("expected.resource.type", "HostVO")
        save("expected.resource.uuid", host.uuid)
        save("expected.effect", "Include")
    }

    void deleteByUser() {
        deleteAllocatorDomain {
            delegate.uuid = getNotNull("domain.uuid") as String
        }
    }

    void deleteByAdmin() {
        deleteAllocatorDomain {
            delegate.uuid = getNotNull("domain.uuid") as String
            delegate.sessionId = adminSession.uuid
        }
    }

    void rememberDomain(AllocatorDomainInventory domain, String ownerAccountUuid, String name, String description,
                        String resourceType, String resourceUuid, String effect) {
        save("domain.uuid", domain.uuid)
        save("owner.account.uuid", ownerAccountUuid)
        save("expected.name", name)
        save("expected.description", description)
        save("expected.resource.type", resourceType)
        save("expected.resource.uuid", resourceUuid)
        save("expected.effect", effect)
        save("expected.scene", "Start")
    }

    void assertNoAllocatorDomain() {
        def domains = (queryAllocatorDomain {
            delegate.sessionId = adminSession.uuid
        } ?: []) as List<AllocatorDomainInventory>
        assert domains.isEmpty()

        def refs = (queryAccountResourceRef {
            delegate.conditions = ["resourceType=AllocatorDomainVO"]
            delegate.sessionId = adminSession.uuid
        } ?: []) as List<AccountResourceRefInventory>
        assert refs.isEmpty()
    }

    void assertOwnedDomain(SessionInventory ownerSession, String ownerAccountUuid) {
        def domainUuid = getNotNull("domain.uuid") as String
        def expectedName = getNotNull("expected.name") as String
        def expectedDescription = getNotNull("expected.description") as String
        def expectedResourceType = getNotNull("expected.resource.type") as String
        def expectedResourceUuid = getNotNull("expected.resource.uuid") as String
        def expectedEffect = getNotNull("expected.effect") as String
        def expectedScene = getNotNull("expected.scene") as String

        def ownerDomains = (queryAllocatorDomain {
            delegate.conditions = ["uuid=${domainUuid}".toString()]
            delegate.sessionId = ownerSession.uuid
        } ?: []) as List<AllocatorDomainInventory>
        assert ownerDomains.size() == 1
        def inv = ownerDomains[0]
        assert inv.name == expectedName
        assert inv.description == expectedDescription
        assert inv.zoneUuid == zone.uuid
        assert inv.state == AllocatorDomainState.Enabled.toString()
        assert inv.rules.size() == 1
        assert inv.rules[0] instanceof AllocatorDomainRuleInventory
        def rule = inv.rules[0] as AllocatorDomainRuleInventory
        assert rule.priority == 0
        assert rule.resourceType == expectedResourceType
        assert rule.resourceUuid == expectedResourceUuid
        assert rule.effect == expectedEffect
        assert inv.scenes.size() == 1
        assert inv.scenes[0] instanceof AllocatorDomainSceneInventory
        def scene = inv.scenes[0] as AllocatorDomainSceneInventory
        assert scene.scene == expectedScene

        def user2Domains = (queryAllocatorDomain {
            delegate.conditions = ["uuid=${domainUuid}".toString()]
            delegate.sessionId = user2Session.uuid
        } ?: []) as List<AllocatorDomainInventory>
        assert user2Domains.isEmpty()

        if (ownerSession.uuid != user1Session.uuid) {
            def user1Domains = (queryAllocatorDomain {
                delegate.conditions = ["uuid=${domainUuid}".toString()]
                delegate.sessionId = user1Session.uuid
            } ?: []) as List<AllocatorDomainInventory>
            assert user1Domains.isEmpty()
        }

        // QueryAccountResourceRef is not account-scoped; isolation is asserted on queryAllocatorDomain above.
        def ownerRefs = (queryAccountResourceRef {
            delegate.conditions = ["resourceUuid=${domainUuid}".toString(), "type=Own"]
            delegate.sessionId = ownerSession.uuid
        } ?: []) as List<AccountResourceRefInventory>
        assert ownerRefs.size() == 1
        assert ownerRefs[0].accountUuid == ownerAccountUuid
        assert ownerRefs[0].resourceType == "AllocatorDomainVO"
        assert ownerRefs[0].resourceUuid == domainUuid
        assert ownerRefs[0].type == "Own"
    }
}
