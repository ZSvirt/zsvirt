package org.zstack.test.integration.core.webhook

import org.zstack.header.core.webhooks.WebhookVO
import org.zstack.sdk.WebhookInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by xing5 on 2017/5/7.
 */
class WebhookCase extends SubCase {
    EnvSpec envSpec

    @Override
    void clean() {
        envSpec.delete()
    }

    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
        spring {
            include("webhook.xml")
        }
    }

    @Override
    void environment() {
        envSpec = env {
            // nothing
        }
    }

    void testWebhooksCRUD() {
        WebhookInventory hook = null

        def testCreateWebhook = {
            def params = null

            hook = createWebhook {
                name = "webhook"
                type = "custom-type"
                url = "http://127.0.0.1:8080/webhook"
                description = "desc"
                opaque = "test data"

                params = delegate
            }

            assert dbIsExists(hook.uuid, WebhookVO.class)
            assert hook.name == params.name
            assert hook.type == params.type
            assert hook.url == params.url
            assert hook.description == params.description
            assert hook.opaque == params.opaque
        }

        def testQueryWebhook = {
            List<WebhookInventory> invs = queryWebhook {
                conditions = ["name=${hook.name}"]
            }

            assert invs.size() == 1
            assert invs[0].uuid == hook.uuid
        }

        def testDeleteWebhook = {
            deleteWebhook {
                uuid = hook.uuid
            }

            assert !dbIsExists(hook.uuid, WebhookVO.class)
        }

        testCreateWebhook()
        testQueryWebhook()
        testDeleteWebhook()
    }

    void testInvalidUrl() {
        expect(AssertionError.class) {
            createWebhook {
                name = "webhook"
                type = "custom-type"
                url = "this is not a url"
                description = "desc"
                opaque = "test data"
            }
        }
    }

    @Override
    void test() {
        envSpec.create {
            testWebhooksCRUD()
            testInvalidUrl()
        }
    }
}
