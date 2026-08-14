package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.thread.AsyncTimer;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.ReconnectHostMsg;
import org.zstack.header.message.MessageReply;

import java.util.concurrent.TimeUnit;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class HostReconnectTask extends AsyncTimer {
    protected String uuid;
    protected NoErrorCompletion completion;

    @Autowired
    protected CloudBus bus;

    public enum CanDoAnswer {
        Ready,
        NotReady,
        NoReconnect
    }

    protected abstract CanDoAnswer canDoReconnect();

    public HostReconnectTask(String uuid, NoErrorCompletion completion) {
        super(TimeUnit.SECONDS, HostGlobalConfig.PING_HOST_INTERVAL.value(Long.class));
        this.uuid = uuid;
        this.completion = completion;

        __name__ = String.format("host-%s-reconnect-task", uuid);
    }

    private void reconnectNow(String uuid, Completion completion) {
        ReconnectHostMsg msg = new ReconnectHostMsg();
        msg.setHostUuid(uuid);
        msg.setSkipIfHostConnected(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, uuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    protected void execute() {
        CanDoAnswer answer = canDoReconnect();
        if (answer == CanDoAnswer.Ready) {
            reconnectNow(uuid, new Completion(completion) {
                @Override
                public void success() {
                    completion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    whenConnectFail(errorCode);
                }
            });
        } else if (answer == CanDoAnswer.NotReady) {
            // still not ready to reconnect the host, continue this reconnect task
            continueToRunThisTimer();
        } else if (answer == CanDoAnswer.NoReconnect) {
            completion.done();
        } else {
            throw new CloudRuntimeException(String.format("should not be here[%s]", answer));
        }
    }

    protected void whenConnectFail(ErrorCode errorCode) {
        // still fail to reconnect the host, continue this reconnect task
        continueToRunThisTimer();
    }
}
