package org.zstack.test.integration.core.canonicalevent

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CanonicalEvent
import org.zstack.core.cloudbus.EventFacade
import org.zstack.sdk.WebhookInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by xing5 on 2017/5/8.
 */
class CanonicalEventWebhookCase extends SubCase {
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

    String WEBHOOK_PATH = "/canonical-event-webhook"

    void testErrorToCreateWebhookifOpaqueFieldMissing() {
        expect(AssertionError.class) {
            createWebhook {
                name = "webhook1"
                url = "http://127.0.0.1:8989$WEBHOOK_PATH"
                type = EventFacade.WEBHOOK_TYPE
            }
        }
    }

    void testCanonicalEventWithVariableInPath() {
        String path = "/test/{uuid}/event"

        int count = 0
        WebhookInventory hook1 = createWebhook {
            name = "webhook1"
            url = "http://127.0.0.1:8989$WEBHOOK_PATH"
            type = EventFacade.WEBHOOK_TYPE
            opaque = path
        }

        // this webhook will not be called because path unmatching
        WebhookInventory hook2 = createWebhook {
            name = "webhook1"
            url = "http://127.0.0.1:8989$WEBHOOK_PATH"
            type = EventFacade.WEBHOOK_TYPE
            opaque = "/this-path-does-not-match"
        }

        CanonicalEvent evt
        envSpec.simulator(WEBHOOK_PATH) { HttpEntity<String> e ->
            evt = json(e.getBody(), CanonicalEvent.class)
            count ++
            return [:]
        }

        String content = "hello world"
        String eventPath = "/test/${Platform.uuid}/event"
        bean(EventFacade.class).fire(eventPath, content)

        retryInSecs {
            assert count == 1
            assert evt != null
            assert evt.path == eventPath
            assert evt.content == content
            assert evt.managementNodeId == Platform.getManagementServerId()
        }
    }

    void testCanonicalEventUseWebhook() {
        String path = "/test/event"

        WebhookInventory hook1 = createWebhook {
            name = "webhook1"
            url = "http://127.0.0.1:8989$WEBHOOK_PATH"
            type = EventFacade.WEBHOOK_TYPE
            opaque = path
        }

        WebhookInventory hook2 = createWebhook {
            name = "webhook2"
            url = "http://127.0.0.1:8989$WEBHOOK_PATH"
            type = EventFacade.WEBHOOK_TYPE
            opaque = path
        }

        def testFireTwoEvents = {
            List<CanonicalEvent> evts = []
            envSpec.simulator(WEBHOOK_PATH) { HttpEntity<String> e ->
                CanonicalEvent evt = json(e.getBody(), CanonicalEvent.class)
                evts.add(evt)
                return [:]
            }

            String content = "hello world"
            bean(EventFacade.class).fire(path, content)

            retryInSecs {
                assert evts.size() == 2
                CanonicalEvent evt1 = evts[0]
                CanonicalEvent evt2 = evts[1]
                assert evt1.path == path
                assert evt1.content == content
                assert evt1.managementNodeId == Platform.getManagementServerId()
                assert evt2.path == path
                assert evt2.content == content
                assert evt2.managementNodeId == Platform.getManagementServerId()
            }
        }

        def testOneEventsGetAfterDeleteOneHook = {
            deleteWebhook { uuid = hook1.uuid }

            List<CanonicalEvent> evts = []
            envSpec.simulator(WEBHOOK_PATH) { HttpEntity<String> e ->
                CanonicalEvent evt = json(e.getBody(), CanonicalEvent.class)
                evts.add(evt)
                return [:]
            }

            String content = "hello world"
            bean(EventFacade.class).fire(path, content)

            retryInSecs {
                assert evts.size() == 1
            }
        }

        def testNoEventGetAfterDeleteAllHooks = {
            deleteWebhook { uuid = hook2.uuid }

            List<CanonicalEvent> evts = []
            envSpec.simulator(WEBHOOK_PATH) { HttpEntity<String> e ->
                CanonicalEvent evt = json(e.getBody(), CanonicalEvent.class)
                evts.add(evt)
                return [:]
            }

            String content = "hello world"
            bean(EventFacade.class).fire(path, content)

            retryInSecs {
                assert evts.size() == 0
            }
        }

        testFireTwoEvents()
        testOneEventsGetAfterDeleteOneHook()
        testNoEventGetAfterDeleteAllHooks()
    }

    @Override
    void environment() {
        envSpec = env {
            // nothing
        }
    }

    @Override
    void test() {
        envSpec.create {
            testCanonicalEventUseWebhook()
            testCanonicalEventWithVariableInPath()
            testErrorToCreateWebhookifOpaqueFieldMissing()
        }
    }
}
