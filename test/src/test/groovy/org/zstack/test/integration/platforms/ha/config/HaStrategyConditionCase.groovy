package org.zstack.test.integration.platforms.ha.config

import org.springframework.http.HttpEntity
import org.zstack.ha.entity.HaCommands
import org.zstack.ha.header.HaConstants
import org.zstack.ha.header.HaGlobalConfig
import org.zstack.ha.header.HaStrategyState
import org.zstack.ha.header.SelfFencerStrategy
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HaStrategyConditionInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.platforms.ha.HaTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class HaStrategyConditionCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = HaTest.smpCephNfsOneCluster()
    }

    @Override
    void test() {
        env.create {
            testHaStrategyCondition()
            testHaOpenOrClose()
            testUpdateHaStrategyCondition()
        }
    }

    void testHaStrategyCondition() {
        logger.info("Test 001: HaStrategyCondition rows exist")
        def haCondition = zqlQuery("query HaStrategyCondition")
        assert haCondition.size() == 2

        logger.info("Test 002: enable HA and one condition, expect success")
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.ALL.name
            delegate.value = "true"
        }
        updateHaStrategyCondition {
            delegate.uuid = haCondition[0].uuid
            delegate.state = HaStrategyState.Enable.toString()
        } as HaStrategyConditionInventory

        logger.info("Test 003: Force enables hostStorageState, Permissive disables it, closing HA disables every condition")
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.SELF_FENCER_STRATEGY.name
            delegate.value = SelfFencerStrategy.Permissive.toString()
        }

        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.SELF_FENCER_STRATEGY.name
            delegate.value = SelfFencerStrategy.Force.toString()
        }
        def storage = zqlQuery("query HaStrategyCondition where fencerName = '${HaConstants.HOST_STORAGE_STATE}'")
        assert storage.size() == 1
        assert storage[0].state == HaStrategyState.Enable.toString()

        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.SELF_FENCER_STRATEGY.name
            delegate.value = SelfFencerStrategy.Permissive.toString()
        }
        storage = zqlQuery("query HaStrategyCondition where fencerName = '${HaConstants.HOST_STORAGE_STATE}'")
        assert storage[0].state == HaStrategyState.Disable.toString()

        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.ALL.name
            delegate.value = "false"
        }
        assert zqlQuery("query HaStrategyCondition where state = '${HaStrategyState.Enable}'").size() == 0
    }

    void testHaOpenOrClose() {
        logger.info("Test 101: HA stays Permissive when opened, then Force is pushed to NFS and Ceph fencers")
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.SELF_FENCER_STRATEGY.name
            delegate.value = SelfFencerStrategy.Permissive.toString()
        }
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.ALL.name
            delegate.value = "true"
        }
        assert queryGlobalConfig {
            delegate.conditions = ["category=${HaGlobalConfig.CATEGORY}", "name=${HaGlobalConfig.SELF_FENCER_STRATEGY.name}"]
        }[0].value == SelfFencerStrategy.Permissive.toString()

        HaCommands.SetupSelfFencerCmd nfsCmd = null
        env.afterSimulator(HaConstants.SETUP_SELF_FENCER_PATH) { rsp, HttpEntity<String> e ->
            nfsCmd = json(e.body, HaCommands.SetupSelfFencerCmd.class)
            return rsp
        }
        CephPrimaryStorageBase.KvmSetupSelfFencerCmd cephCmd = null
        env.afterSimulator(CephPrimaryStorageBase.KVM_HA_SETUP_SELF_FENCER) { rsp, HttpEntity<String> e ->
            cephCmd = json(e.body, CephPrimaryStorageBase.KvmSetupSelfFencerCmd.class)
            return rsp
        }

        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.SELF_FENCER_STRATEGY.name
            delegate.value = SelfFencerStrategy.Force.toString()
        }

        retryInSecs {
            assert nfsCmd != null
            assert nfsCmd.fencers.size() == 1
            assert nfsCmd.fencers.contains(HaConstants.HOST_STORAGE_STATE)
            assert nfsCmd.strategy == SelfFencerStrategy.Force.toString()
            assert cephCmd.fencers.size() == 1
            assert cephCmd.fencers.contains(HaConstants.HOST_STORAGE_STATE)
            assert cephCmd.strategy == SelfFencerStrategy.Force.toString()
        }

        def haCondition = zqlQuery("query HaStrategyCondition")
        assert haCondition.size() == 2

        logger.info("Test 102: enable both fencers, expect hostStorageState and hostBusinessNic")
        haCondition.each { condition ->
            updateHaStrategyCondition {
                delegate.uuid = condition["uuid"]
                delegate.state = HaStrategyState.Enable.toString()
            }
        }
        retryInSecs(12) {
            assert nfsCmd.fencers.size() == 2
            assert nfsCmd.fencers.contains(HaConstants.HOST_STORAGE_STATE)
            assert nfsCmd.fencers.contains(HaConstants.HOST_BUSINESS_NIC)
            assert nfsCmd.strategy == SelfFencerStrategy.Force.toString()
            assert cephCmd.fencers.size() == 2
            assert cephCmd.strategy == SelfFencerStrategy.Force.toString()
        }

        logger.info("Test 103: disable hostStorageState, filesystem and Ceph strategy become Permissive and both fencers stay")
        def hostStorageState = zqlQuery("query HaStrategyCondition where fencerName = '${HaConstants.HOST_STORAGE_STATE}'")[0]
        updateHaStrategyCondition {
            delegate.uuid = hostStorageState.uuid
            delegate.state = HaStrategyState.Disable.toString()
        }
        retryInSecs {
            assert nfsCmd.fencers.size() == 2
            assert nfsCmd.strategy == SelfFencerStrategy.Permissive.toString()
            assert cephCmd.fencers.size() == 2
            assert cephCmd.strategy == SelfFencerStrategy.Permissive.toString()
        }
        assert queryGlobalConfig {
            delegate.conditions = ["category=${HaGlobalConfig.CATEGORY}", "name=${HaGlobalConfig.SELF_FENCER_STRATEGY.name}"]
        }[0].value == SelfFencerStrategy.Permissive.toString()

        logger.info("Test 104: disable hostBusinessNic, only hostStorageState remains")
        def hostBusinessNic = zqlQuery("query HaStrategyCondition where fencerName = '${HaConstants.HOST_BUSINESS_NIC}'")[0]
        updateHaStrategyCondition {
            delegate.uuid = hostBusinessNic.uuid
            delegate.state = HaStrategyState.Disable.toString()
        }
        retryInSecs {
            assert nfsCmd.fencers.size() == 1
            assert nfsCmd.fencers.contains(HaConstants.HOST_STORAGE_STATE)
            assert nfsCmd.strategy == SelfFencerStrategy.Permissive.toString()
            assert cephCmd.fencers.size() == 1
            assert cephCmd.strategy == SelfFencerStrategy.Permissive.toString()
        }

        logger.info("Test 105: enable both fencers and reconnect kvm2, expect Force and both fencers")
        updateHaStrategyCondition {
            delegate.uuid = hostBusinessNic.uuid
            delegate.state = HaStrategyState.Enable.toString()
        }
        updateHaStrategyCondition {
            delegate.uuid = hostStorageState.uuid
            delegate.state = HaStrategyState.Enable.toString()
        }
        def kvm2 = env.inventoryByName("kvm2")
        reconnectHost {
            delegate.uuid = kvm2.uuid
        }
        retryInSecs {
            assert nfsCmd.fencers.size() == 2
            assert nfsCmd.fencers.contains(HaConstants.HOST_STORAGE_STATE)
            assert nfsCmd.fencers.contains(HaConstants.HOST_BUSINESS_NIC)
            assert nfsCmd.strategy == SelfFencerStrategy.Force.toString()
            assert cephCmd.fencers.size() == 2
            assert cephCmd.strategy == SelfFencerStrategy.Force.toString()
        }

        logger.info("Test 106: detach and attach SMP, the filesystem fencer command stays Force")
        def cluster = env.inventoryByName("cluster") as ClusterInventory
        def smp = env.inventoryByName("smp") as PrimaryStorageInventory
        detachPrimaryStorageFromCluster {
            delegate.primaryStorageUuid = smp.uuid
            delegate.clusterUuid = cluster.uuid
        }
        attachPrimaryStorageToCluster {
            delegate.primaryStorageUuid = smp.uuid
            delegate.clusterUuid = cluster.uuid
        }
        retryInSecs {
            assert nfsCmd.fencers.size() == 2
            assert nfsCmd.strategy == SelfFencerStrategy.Force.toString()
        }

        logger.info("Test 107: global strategy Permissive is pushed to NFS and Ceph")
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.SELF_FENCER_STRATEGY.name
            delegate.value = SelfFencerStrategy.Permissive.toString()
        }
        retryInSecs {
            assert nfsCmd.fencers.size() == 2
            assert cephCmd.fencers.size() == 2
            assert nfsCmd.strategy == SelfFencerStrategy.Permissive.toString()
            assert cephCmd.strategy == SelfFencerStrategy.Permissive.toString()
        }
    }

    void testUpdateHaStrategyCondition() {
        def storage = zqlQuery("query HaStrategyCondition where fencerName = '${HaConstants.HOST_STORAGE_STATE}'")[0]

        logger.info("Test 201: enable hostStorageState, expect global strategy Force")
        updateHaStrategyCondition {
            delegate.uuid = storage.uuid
            delegate.state = HaStrategyState.Enable.toString()
        }
        assert queryGlobalConfig {
            delegate.conditions = ["category=${HaGlobalConfig.CATEGORY}", "name=${HaGlobalConfig.SELF_FENCER_STRATEGY.name}"]
        }[0].value == SelfFencerStrategy.Force.toString()

        logger.info("Test 202: disable hostStorageState, expect global strategy Permissive")
        updateHaStrategyCondition {
            delegate.uuid = storage.uuid
            delegate.state = HaStrategyState.Disable.toString()
        }
        assert queryGlobalConfig {
            delegate.conditions = ["category=${HaGlobalConfig.CATEGORY}", "name=${HaGlobalConfig.SELF_FENCER_STRATEGY.name}"]
        }[0].value == SelfFencerStrategy.Permissive.toString()

        logger.info("Test 203: enable hostStorageState again, expect global strategy Force")
        updateHaStrategyCondition {
            delegate.uuid = storage.uuid
            delegate.state = HaStrategyState.Enable.toString()
        }
        assert queryGlobalConfig {
            delegate.conditions = ["category=${HaGlobalConfig.CATEGORY}", "name=${HaGlobalConfig.SELF_FENCER_STRATEGY.name}"]
        }[0].value == SelfFencerStrategy.Force.toString()
    }

    @Override
    void clean() {
        env.delete()
    }
}
