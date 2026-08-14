package org.zstack.test.integration.core.cloudbus

import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.header.message.MessageReply
import org.zstack.header.message.NeedReplyMessage
import org.zstack.testlib.SubCase

/**
 * Created by heathhose on 17-3-22.
 */
class CloudBusProxyCase extends SubCase{

    CloudBus bus
    CloudBusAopProxy aop
    List<FakeNeedReplyMessage> msgs = new ArrayList<FakeNeedReplyMessage>(2)
    FakeNeedReplyMessage msg

    @Override
    void setup() {
        spring {
            include("CloudBusAopProxy.xml")
        }
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        bus = bean(CloudBus.class)
        aop = bean(CloudBusAopProxy.class)

        aop.addMessage(NeedReplyMessage.class, CloudBusAopProxy.Behavior.FAIL)
        testBusSendMsgsWhenCloudBusAopProxyBehaviorFAIL()
        testBusSendCallBackMgsWhenCloudBusAopProxyBehaviorFAIL()

        aop.addMessage(NeedReplyMessage.class, CloudBusAopProxy.Behavior.TIMEOUT)
        testBusSenMsgsWhenCloudBusAopProxyBehaviorTIMEOUT()
    }

    void testBusSendMsgsWhenCloudBusAopProxyBehaviorFAIL(){
        msg = new FakeNeedReplyMessage()
        msg.setServiceId("A fake service id not needed")
        msgs.add(msg)
        msg = new FakeNeedReplyMessage()
        msg.setServiceId("A fake service id not needed")
        msgs.add(msg)
        bus.send(msgs)
    }

    void testBusSendCallBackMgsWhenCloudBusAopProxyBehaviorFAIL(){
        boolean isSuccess = false
        msg = new FakeNeedReplyMessage()
        msg.setServiceId("A fake service id not needed")
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    isSuccess = true
                }
            }
        })

        msg = new FakeNeedReplyMessage()
        msg.setServiceId("A fake service id not needed")
        MessageReply reply = bus.call(msg)
        retryInSecs {
            assert !reply.isSuccess()
            assert isSuccess
        }
    }

    void testBusSenMsgsWhenCloudBusAopProxyBehaviorTIMEOUT(){
        msgs.clear()
        msg = new FakeNeedReplyMessage()
        msg.setServiceId("A fake service id not needed")
        msg.setTimeout(1)
        msgs.add(msg)
        msg = new FakeNeedReplyMessage()
        msg.setServiceId("A fake service id not needed")
        msg.setTimeout(1)
        msgs.add(msg)
        List<MessageReply> replies = bus.call(msgs)
        for (MessageReply r : replies) {
            assert !r.isSuccess()
        }
    }

    @Override
    void clean() {
    }

}
