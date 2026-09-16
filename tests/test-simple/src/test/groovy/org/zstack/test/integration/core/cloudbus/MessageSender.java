package org.zstack.test.integration.core.cloudbus;

import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.vm.StartVmInstanceMsg;

public class MessageSender {

    void sendMsg() {
        CloudBus bus = Platform.getComponentLoader().getComponent(CloudBus.class);
        ThreadFacade thread = Platform.getComponentLoader().getComponent(ThreadFacade.class);

        thread.syncSubmit(new SyncTask<Void>() {
            @Override
            public Void call() throws Exception {
                StartVmInstanceMsg msg = new StartVmInstanceMsg();
                bus.makeLocalServiceId(msg, "testFutureCompletionSend");
                FutureCompletion completion = bus.send(msg);
                completion.await(5000L);
                return null;
            }

            @Override
            public String getName() {
                return "MessageSender.sendMsg";
            }

            @Override
            public int getSyncLevel() {
                return 1;
            }

            @Override
            public String getSyncSignature() {
                return getName();
            }
        });
    }
}