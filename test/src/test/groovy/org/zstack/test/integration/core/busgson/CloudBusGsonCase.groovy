package org.zstack.test.integration.core.busgson

import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusGson
import org.zstack.header.host.ChangeVmPasswordMsg
import org.zstack.header.message.JsonSchemaBuilder
import org.zstack.header.vm.VmAccountPreference
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

import java.util.LinkedHashMap

class CloudBusGsonCase extends SubCase {
    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {}

    @Override
    void test() {
        testNoLogging()
    }

    void testNoLogging() {
        logger.info("Test 001: expect mask ChangeVmPasswordMsg.VmAccountPreference.accountPassword")
        def msg001 = new ChangeVmPasswordMsg()
        msg001.accountPerference = new VmAccountPreference()
        msg001.accountPerference.accountPassword = "123456"

        def result001 = CloudBusGson.toLogSafeJson(msg001)
        logger.info(result001)
        assert !result001.contains(msg001.accountPerference.accountPassword)

        logger.info("Test 002: wire json keeps password, log-safe json after http parse masks it")
        def msg002 = new ChangeVmPasswordMsg()
        msg002.hostUuid = "482b35ebad014ebbb575da9c83766e5e"
        msg002.accountPerference = new VmAccountPreference(
                "92bf51610d7c4527a5c85d26e628761d", "root", "admin@123")
        msg002.putHeaderEntry(CloudBus.HEADER_SCHEMA, new JsonSchemaBuilder(msg002).build())

        def body002 = CloudBusGson.toJson(msg002)
        logger.info(body002)
        assert body002.contains(msg002.accountPerference.accountPassword)

        def parsed002 = CloudBusGson.fromJson(body002)
        def raw002 = JSONObjectUtil.toObject(body002, LinkedHashMap.class)
        parsed002.restoreFromSchema(raw002.values().iterator().next() as Map)
        def result002 = CloudBusGson.toLogSafeJson(parsed002)
        logger.info(result002)
        assert !result002.contains(msg002.accountPerference.accountPassword)
        assert result002.contains("*****")
    }

    @Override
    void clean() {

    }
}
