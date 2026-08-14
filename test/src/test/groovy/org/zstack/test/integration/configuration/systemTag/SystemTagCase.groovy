package org.zstack.test.integration.configuration.systemTag

import org.zstack.header.identity.AccountVO
import org.zstack.header.zone.ZoneVO
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.CreateSystemTagAction
import org.zstack.testlib.*
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 02/03/2017.
 */
class SystemTagCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
    }

    @Override
    void environment() {
        env = env{
            zone{
                name = "zone"
            }

            account {
                name = "test-account"
                password = "password"
            }
        }
    }

    @Override
    void test() {
        env.create {
            testCreateSystemTag()
            testRepeatCreateSameResourceUuidSystemTag()
            testCreateSystemTagForTypeNotMatchedResource()
        }

    }

    void testCreateSystemTag(){
        ZoneSpec zone = env.specByName('zone')

        CreateSystemTagAction a = new CreateSystemTagAction(
                resourceType: ZoneVO.getSimpleName(),
                resourceUuid: zone.inventory.uuid,
                tag: "host::reservedCpu::{capacity}",
                sessionId: Test.currentEnvSpec.session.uuid
        )
        CreateSystemTagAction.Result res = a.call()

        assert res.error == null
        assert res.value.inventory.resourceUuid == a.resourceUuid
        assert res.value.inventory.tag == a.tag
        assert res.value.inventory.resourceType == a.resourceType
    }

    void testRepeatCreateSameResourceUuidSystemTag(){
        ZoneSpec zone = env.specByName('zone')

        CreateSystemTagAction a = new CreateSystemTagAction(
                resourceType: ZoneVO.getSimpleName(),
                resourceUuid: zone.inventory.uuid,
                tag: "host::reservedCpu::{capacity}",
                sessionId: Test.currentEnvSpec.session.uuid
        )

        CreateSystemTagAction.Result res = a.call()

        assert res.error == null
    }

    void testCreateSystemTagForTypeNotMatchedResource() {
        logger.info("Test-011: host::reservedCpu:: is cluster system tag, expect fail if attach tag to account")
        def account = env.inventoryByName("test-account") as AccountInventory

        expectApiFailure({
            createSystemTag {
                delegate.resourceType = AccountVO.getSimpleName()
                delegate.resourceUuid = account.uuid
                delegate.tag = "host::reservedCpu::${SizeUnit.GIGABYTE.toByte(8)}".toString()
            }
        }) {
            assert delegate.code == "SYS.1007"
            assert delegate.opaque
            assert delegate.opaque["resource.type"] == AccountVO.getSimpleName()
            assert delegate.opaque["tag"] == "host::reservedCpu::${SizeUnit.GIGABYTE.toByte(8)}".toString()
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
