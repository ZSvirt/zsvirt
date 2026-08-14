package org.zstack.testlib

import org.zstack.core.db.Q
import org.zstack.header.zone.ZoneVO
import org.zstack.header.zone.ZoneVO_
import org.zstack.sdk.AttachBackupStorageToZoneAction
import org.zstack.sdk.ZoneInventory
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by xing5 on 2017/2/12.
 */
class ZoneSpec extends Spec {
    @SpecParam(required = true)
    String name
    @SpecParam
    String description
    List<ClusterSpec> clusters = []
    List<PrimaryStorageSpec> primaryStorage = []
    List<L2NetworkSpec> l2Networks = []
    List<VirtualRouterOfferingSpec> virtualRouterOfferingSpecs = []

    protected List<String> backupStorageToAttach = []

    ZoneInventory inventory

    ZoneSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    ClusterSpec cluster(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ClusterSpec.class) Closure c) {
        def cspec = new ClusterSpec(envSpec)
        c.delegate = cspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(cspec)
        clusters.add(cspec)
        return cspec
    }

    PrimaryStorageSpec nfsPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = NfsPrimaryStorageSpec.class) Closure c) {
        def nspec = new NfsPrimaryStorageSpec(envSpec)
        c.delegate = nspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(nspec)
        primaryStorage.add(nspec)
        return nspec
    }

    PrimaryStorageSpec localPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = LocalStorageSpec.class) Closure c) {
        def nspec = new LocalStorageSpec(envSpec)
        c.delegate = nspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(nspec)
        primaryStorage.add(nspec)
        return nspec
    }

    PrimaryStorageSpec cephPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = CephPrimaryStorageSpec.class) Closure c) {
        def nspec = new CephPrimaryStorageSpec(envSpec)
        c.delegate = nspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(nspec)
        primaryStorage.add(nspec)
        return nspec
    }

    PrimaryStorageSpec externalPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ExternalPrimaryStorageSpec.class) Closure c) {
        def nspec = new ExternalPrimaryStorageSpec(envSpec)
        c.delegate = nspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(nspec)
        primaryStorage.add(nspec)
        return nspec
    }

    PrimaryStorageSpec smpPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = SharedMountPointPrimaryStorageSpec.class) Closure c) {
        def nspec = new SharedMountPointPrimaryStorageSpec(envSpec)
        c.delegate = nspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(nspec)
        primaryStorage.add(nspec)
        return nspec
    }

    L2NetworkSpec l2NoVlanNetwork(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = L2NoVlanNetworkSpec.class) Closure c) {
        def lspec = new L2NoVlanNetworkSpec(envSpec)
        c.delegate = lspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(lspec)
        l2Networks.add(lspec)
        return lspec
    }

    L2NetworkSpec l2VlanNetwork(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = L2VlanNetworkSpec.class) Closure c) {
        def lspec = new L2VlanNetworkSpec(envSpec)
        c.delegate = lspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(lspec)
        l2Networks.add(lspec)
        return lspec
    }

    SecurityGroupSpec securityGroup(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = SecurityGroupSpec.class) Closure c) {
        def spec = new SecurityGroupSpec(envSpec)
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        return spec
    }

    @SpecMethod
    void attachBackupStorage(String...names) {
        names.each { String bsName ->
            preCreate {
                addDependency(bsName, BackupStorageSpec.class)
            }

            backupStorageToAttach.add(bsName)
        }
    }

    VirtualRouterOfferingSpec virtualRouterOffering(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = VirtualRouterOfferingSpec.class) Closure c) {
        def spec = new VirtualRouterOfferingSpec(envSpec)
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        virtualRouterOfferingSpecs.add(spec)
        return spec
    }

    EipSpec eip(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = EipSpec.class) Closure c) {
        def spec = new EipSpec(envSpec)
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        return spec
    }

    PortForwardingSpec portForwarding(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = PortForwardingSpec.class) Closure c) {
        def spec = new PortForwardingSpec(envSpec)
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        return spec
    }

    LoadBalancerSpec lb(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = LoadBalancerSpec.class) Closure cl) {
        def spec = new LoadBalancerSpec(envSpec)
        cl.delegate = spec
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
        addChild(spec)
        return spec
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createZone {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.sessionId = sessionId
            delegate.description = description
            delegate.isDefault = true
            delegate.userTags = userTags
            delegate.systemTags = systemTags
        } as ZoneInventory

        backupStorageToAttach.each { String bsName ->
            BackupStorageSpec bs = findSpec(bsName, BackupStorageSpec.class)
            def a = new AttachBackupStorageToZoneAction()
            a.zoneUuid = inventory.uuid
            a.backupStorageUuid = bs.inventory.uuid
            a.sessionId = sessionId
            def res = a.call()
            assert res.error == null : "AttachBackupStorageToZoneAction failure: ${JSONObjectUtil.toJsonString(res.error)}"
        }

        doPost(sessionId)

        return id(name, inventory.uuid)
    }

    void doPost(sessionId) {
        postCreate {
            inventory = queryZone {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            List<String> zoneUuidList = Q.New(ZoneVO.class)
                    .select(ZoneVO_.uuid)
                    .eq(ZoneVO_.isDefault, true)
                    .listValues()
            assert zoneUuidList.size() <= 1 : "duplicate default zone found ${zoneUuidList}"

            deleteZone {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
