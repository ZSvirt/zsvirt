package org.zstack.test.integration.core.config

import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.Platform
import org.zstack.core.cloudbus.EventFacade
import org.zstack.core.config.GlobalConfigCanonicalEvents
import org.zstack.core.config.GlobalConfigException
import org.zstack.core.config.GlobalConfigFacadeImpl
import org.zstack.core.config.GlobalConfigVO
import org.zstack.core.config.GlobalConfigVO_
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.db.SimpleQuery
import org.zstack.core.db.UpdateQuery
import org.zstack.header.vm.APICreateVmNicMsg
import org.zstack.image.ImageGlobalConfig
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

import static org.zstack.utils.CollectionDSL.e
import static org.zstack.utils.CollectionDSL.map
import static org.zstack.utils.StringDSL.s

/**
 * Created by miao on 17-5-4.
 */
class GlobalConfigCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = "zone"
            }
        }
    }

    @Override
    void test() {
        env.create {
            testSdk()
            testTolerateDatabaseDirtyData()
            testUpdateIntegerConfigWithFloatValue()
            testFloatPointNumberTolerance()
            testBooleanValidator()
            testBorderValue()
            testImageGlobalConfig()
            testResetGlobalConfig()
            testSyncConfigUponEvent()
            testNormalized()
            testUpdateApiTimeoutDefaultValue()
            testUpdateValueSkipValidation()
            testGetConfigOptions()
        }
    }

    void testSdk() {
        def zone = env.inventoryByName("zone") as ZoneInventory

        int port = 8989
        String WEB_HOOK_PATH = "http://127.0.0.1:$port/sdk/webook"

        def config = new ZSConfig.Builder()
                .setHostname("127.0.0.1")
                .setPort(port)
                .setDefaultPollingInterval(100, TimeUnit.MILLISECONDS)
                .setDefaultPollingTimeout(300000, TimeUnit.MILLISECONDS)
                .setReadTimeout(10, TimeUnit.MINUTES)
                .setWriteTimeout(10, TimeUnit.MINUTES)
                .build()

        LogInByAccountAction a = new LogInByAccountAction();
        a.accountName = "admin"
        a.password = "password"
        a.call();
        
        a = new LogInByAccountAction();
        a.accountName = "admin"
        a.password = "password"
        a.call();

        ApiResult r = ZSClient.callWithConfig(a, config)
        a = new LogInByAccountAction();
        a.accountName = "admin"
        a.password = "password"
        a.call();
        ZSClient.callWithConfig(a, ZSClient.getConfig())

        CreateClusterAction ca = new CreateClusterAction();
        ca.name = "cluster"
        ca.zoneUuid = zone.getUuid()
        ca.hypervisorType = "KVM"
        ca.sessionId = adminSession()

        ZSClient.callWithConfig(ca, config)
    }

    void testGetConfigOptions() {
        testGetValidValue()
        testGetNumberRange()
        testGetNumberBoundary()
    }

    void testGetValidValue() {
        def configOptions = getGlobalConfigOptions {
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.NESTED_VIRTUALIZATION.name
        } as GetGlobalConfigOptionsResult

        // there are 26 valid values for category "vm.cpuMode" from KVMGlobalConfig.java
        assert configOptions.options.validValue.size() == 26
    }

    void testGetNumberRange() {
        def configOptions = getGlobalConfigOptions {
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.VM_CREATE_CONCURRENCY.name
        } as GetGlobalConfigOptionsResult

        // number range for this category is {1, 10},  1 <= value <= 10
        assert configOptions.options.numberGreaterThanOrEqual == 1
        assert configOptions.options.numberLessThanOrEqual == 10
    }

    void testGetNumberBoundary() {
        def configOptions = getGlobalConfigOptions {
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.HOST_SYNC_LEVEL.name
        } as GetGlobalConfigOptionsResult

        // number boundary for this category is >=2 , range [2, 2147483647]
        assert configOptions.options.numberGreaterThanOrEqual == 2
        assert configOptions.options.numberLessThanOrEqual == (long) Integer.MAX_VALUE
    }

    void testFloatPointNumberTolerance() {
        // test if the global config can convert float to int
        KVMGlobalConfig.VM_MIGRATION_QUANTITY.@value = 1.0
        assert KVMGlobalConfig.VM_MIGRATION_QUANTITY.value(int.class) == 1
    }

    void testUpdateIntegerConfigWithFloatValue() {
        expect(AssertionError.class) {
            updateGlobalConfig {
                category = KVMGlobalConfig.VM_MIGRATION_QUANTITY.category
                name = KVMGlobalConfig.VM_MIGRATION_QUANTITY.name
                value = "1.0"
            }
        }
    }

    void testTolerateDatabaseDirtyData() {
        // make dirty data in database
        GlobalConfigVO vo = KVMGlobalConfig.VM_MIGRATION_QUANTITY.reload()
        vo.setValue("1.0")
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        dbf.update(vo)

        GlobalConfigFacadeImpl gcf = bean(GlobalConfigFacadeImpl.class)
        gcf.start()

        assert KVMGlobalConfig.VM_MIGRATION_QUANTITY.value(int.class) == 1
    }

    void testBooleanValidator() {
        updateGlobalConfig {
            category = GlobalConfigForTest.CATEGORY
            name = GlobalConfigForTest.TEST_GlobalConfig2_Boolean.name
            value = "TRUE"
            sessionId = adminSession()
        }


        UpdateGlobalConfigAction action = new UpdateGlobalConfigAction()
        action.category = GlobalConfigForTest.CATEGORY
        action.name = GlobalConfigForTest.TEST_GlobalConfig2_Boolean.name
        action.value = "miao"
        action.sessionId = adminSession()
        UpdateGlobalConfigAction.Result result = action.call()
        assert result.error != null
    }

    void testBorderValue() {
        def action1 = new UpdateGlobalConfigAction() as UpdateGlobalConfigAction
        action1.category = GlobalConfigForTest.CATEGORY
        action1.name = GlobalConfigForTest.TEST_GLOBALCONFIG_BORDER.name
        action1.value = 0
        action1.sessionId = adminSession()
        UpdateGlobalConfigAction.Result result1 = action1.call()

        assert result1.error == null

        def action2 = new UpdateGlobalConfigAction() as UpdateGlobalConfigAction
        action2.category = GlobalConfigForTest.CATEGORY
        action2.name = GlobalConfigForTest.TEST_GLOBALCONFIG_BORDER.name
        action2.value = 100
        action2.sessionId = adminSession()
        UpdateGlobalConfigAction.Result result2 = action2.call()

        assert result2.error == null
    }

    void testImageGlobalConfig(){
        updateGlobalConfig {
            category = ImageGlobalConfig.CATEGORY
            name = ImageGlobalConfig.EXPUNGE_INTERVAL.name
            value = 1800
        }

        expect(AssertionError.class) {
            updateGlobalConfig {
                category = ImageGlobalConfig.CATEGORY
                name = ImageGlobalConfig.EXPUNGE_INTERVAL.name
                value = 0
            }
        }
    }

    void testResetGlobalConfig(){
        updateGlobalConfig {
            category = KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.category
            name = KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.name
            value = 10
        }

        GlobalConfigInventory gci = queryGlobalConfig {
            conditions = ["category=${KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.category}","name=${KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.name}"]
        }[0]
        assert KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.value(int.class) == 10


        resetGlobalConfig {}

        gci = queryGlobalConfig {
            conditions = ["category=${KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.category}","name=${KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.name}"]
        }[0]
        assert KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.value(int.class) == 0
    }

    private static String makeUpdateEventPath(String category, String name) {
        return s(GlobalConfigCanonicalEvents.UPDATE_EVENT_PATH).formatByMap(map(
                e("nodeUuid", Platform.getUuid()),
                e("category", category),
                e("name", name)
        ))
    }

    void testSyncConfigUponEvent() {
        EventFacade evtf = bean(EventFacade.class)
        String name = KVMGlobalConfig.LIBVIRT_CACHE_MODE.name

        SQL.New(GlobalConfigVO.class).eq(GlobalConfigVO_.category, "kvm")
                .eq(GlobalConfigVO_.@name, "vm.cacheMode")
                .set(GlobalConfigVO_.value, "writeback")
                .update()

        GlobalConfigCanonicalEvents.UpdateEvent d = new GlobalConfigCanonicalEvents.UpdateEvent()
        d.oldValue = KVMGlobalConfig.LIBVIRT_CACHE_MODE.value()
        d.newValue = "writethrough"
        evtf.fire(makeUpdateEventPath(KVMGlobalConfig.CATEGORY, name), d)

        retryInSecs {
            // get from db not from event.
            assert KVMGlobalConfig.LIBVIRT_CACHE_MODE.value() == "writeback"
        }
    }

    void testNormalized() {
        UpdateQuery.New(GlobalConfigVO.class).condAnd(GlobalConfigVO_.category, SimpleQuery.Op.EQ, "quota").
                condAnd(GlobalConfigVO_.@name, SimpleQuery.Op.EQ, "snapshot.volume.num").set(GlobalConfigVO_.value, "200.0").update()

        GlobalConfigFacadeImpl gcf = bean(GlobalConfigFacadeImpl.class)
        gcf.start()

        assert gcf.getConfigValue("quota", "snapshot.volume.num", Long.class) == 200L
        assert Q.New(GlobalConfigVO.class).eq(GlobalConfigVO_.category, "quota").
                eq(GlobalConfigVO_.@name, "snapshot.volume.num").select(GlobalConfigVO_.value).findValue() == "200"
    }

    void testUpdateApiTimeoutDefaultValue() {
        SQL.New(GlobalConfigVO.class).eq(GlobalConfigVO_.@name, APICreateVmNicMsg.class.name)
                .set(GlobalConfigVO_.defaultValue, "5m")
                .set(GlobalConfigVO_.value, "10800000")
                .update()

        GlobalConfigFacadeImpl gcf = bean(GlobalConfigFacadeImpl.class)
        gcf.start()
        // do not update value
        assert Q.New(GlobalConfigVO.class).select(GlobalConfigVO_.value)
                .eq(GlobalConfigVO_.@name, APICreateVmNicMsg.class.name)
                .eq(GlobalConfigVO_.defaultValue, "30m")
                .findValue() == "10800000"

        SQL.New(GlobalConfigVO.class).eq(GlobalConfigVO_.@name, APICreateVmNicMsg.class.name)
                .set(GlobalConfigVO_.defaultValue, "10800000")
                .set(GlobalConfigVO_.value, "10800000")
                .update()

        gcf.start()
        // update to new default value
        assert Q.New(GlobalConfigVO.class).select(GlobalConfigVO_.value)
                .eq(GlobalConfigVO_.@name, APICreateVmNicMsg.class.name)
                .eq(GlobalConfigVO_.defaultValue, "30m")
                .findValue() == "30m"
    }

    void testUpdateValueSkipValidation() {
        expect(GlobalConfigException.class) {
            VmGlobalConfig.VM_DEFAULT_CD_ROM_NUM.updateValue(-1)
        }

        VmGlobalConfig.VM_DEFAULT_CD_ROM_NUM.updateValueSkipValidation(-1)
        VmGlobalConfig.VM_DEFAULT_CD_ROM_NUM.resetValue()
    }
}
