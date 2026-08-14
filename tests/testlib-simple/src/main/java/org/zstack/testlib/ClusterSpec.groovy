package org.zstack.testlib

import org.zstack.kvm.KVMConstant
import org.zstack.sdk.AttachL2NetworkToClusterAction
import org.zstack.sdk.AttachPrimaryStorageToClusterAction
import org.zstack.sdk.ClusterInventory

/**
 * Created by xing5 on 2017/2/12.
 */
class ClusterSpec extends Spec {
    @SpecParam(required = true)
    String name
    @SpecParam
    String description
    @SpecParam
    String hypervisorType = KVMConstant.KVM_HYPERVISOR_TYPE
    @SpecParam
    String architecture = "x86_64"
    List<HostSpec> hosts = []

    List<String> primaryStorageToAttach = []
    List<String> l2NetworkToAttach = []

    ClusterInventory inventory

    ClusterSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    KVMHostSpec kvm(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = KVMHostSpec.class) Closure c) {
        def hspec = new KVMHostSpec(envSpec)
        c.delegate = hspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(hspec)
        hosts.add(hspec)
        return hspec
    }

    @SpecMethod
    void attachPrimaryStorage(String... names) {
        names.each { String primaryStorageName ->
            preCreate {
                addDependency(primaryStorageName, PrimaryStorageSpec.class)
            }

            primaryStorageToAttach.add(primaryStorageName)
        }
    }

    @SpecMethod
    void attachL2Network(String ...names) {
        names.each { String l2NetworkName ->
            preCreate {
                addDependency(l2NetworkName, L2NetworkSpec.class)
            }

            l2NetworkToAttach.add(l2NetworkName)
        }
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createCluster {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.hypervisorType = hypervisorType
            delegate.zoneUuid = (parent as ZoneSpec).inventory.uuid
            delegate.sessionId = sessionId
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.architecture = architecture
        } as ClusterInventory

        primaryStorageToAttach.each { String primaryStorageName ->
            def ps = findSpec(primaryStorageName, PrimaryStorageSpec.class) as PrimaryStorageSpec
            def a = new AttachPrimaryStorageToClusterAction()
            a.clusterUuid = inventory.uuid
            a.primaryStorageUuid = ps.inventory.uuid
            a.sessionId = sessionId
            errorOut(a.call())
        }

        l2NetworkToAttach.each { String l2NetworkName ->
            def l2 = findSpec(l2NetworkName, L2NetworkSpec.class) as L2NetworkSpec
            def a = new AttachL2NetworkToClusterAction()
            a.clusterUuid = inventory.uuid
            a.l2NetworkUuid = l2.inventory.uuid
            a.sessionId = sessionId
            errorOut(a.call())
        }

        postCreate {
            inventory = queryCluster {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteCluster {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
