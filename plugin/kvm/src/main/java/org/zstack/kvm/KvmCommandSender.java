package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusSteppingCallback;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.AsyncBackup;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.utils.DebugUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by xing5 on 2016/4/19.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class KvmCommandSender {
    @Autowired
    private CloudBus bus;
    @Autowired
    private ApiTimeoutManager timeoutManager;
    @Autowired
    private ErrorFacade errf;

    private List<String> hostUuids;
    private boolean noStatusCheck;

    public static abstract class SteppingSendCallback<T> extends ReturnValueCompletion<T> {
        String hostUuid;

        public SteppingSendCallback() {
            super(null);
        }

        protected String getHostUuid() {
            return hostUuid;
        }
    }


    public KvmCommandSender(List<String> hostUuids, boolean noStatusCheck) {
        this.hostUuids = hostUuids;
        this.noStatusCheck = noStatusCheck;
    }

    public KvmCommandSender(List<String> hostUuids) {
        this(hostUuids, false);
    }

    public KvmCommandSender(String hostUuid) {
        this(hostUuid, false);
    }

    public KvmCommandSender(String hostUuid, boolean noStatusCheck) {
        this(list(hostUuid), noStatusCheck);
        DebugUtils.Assert(hostUuid != null, "hostUuid cannot be null");
    }

    public KvmCommandSender disableHostStatusCheck() {
        this.noStatusCheck = true;
        return this;
    }

    public void send(final Object cmd, final String path, final KvmCommandFailureChecker checker, final SteppingSendCallback<KvmResponseWrapper> completion) {
        List<KVMHostAsyncHttpCallMsg> msgs = hostUuids.stream().map(huuid -> {
            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setCommand(cmd);
            msg.setHostUuid(huuid);
            msg.setPath(path);
            msg.setNoStatusCheck(noStatusCheck);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, huuid);
            return msg;
        }).collect(Collectors.toList());

        bus.send(msgs, msgs.size(), new CloudBusSteppingCallback(completion) {
            @Override
            public void run(NeedReplyMessage msg, MessageReply reply) {
                KVMHostAsyncHttpCallMsg kmsg = (KVMHostAsyncHttpCallMsg) msg;
                completion.hostUuid = kmsg.getHostUuid();

                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply ar = reply.castReply();
                KvmResponseWrapper w = new KvmResponseWrapper(ar.getResponse());

                ErrorCode err = checker.getError(w);
                if (err != null) {
                    completion.fail(err);
                    return;
                }

                completion.success(w);
            }
        });
    }

    public void send(final Object cmd, final String path, final KvmCommandFailureChecker checker, final ReturnValueCompletion<KvmResponseWrapper> completion) {
        send(cmd, path, checker, TimeUnit.MINUTES.toMillis(5), completion);
    }

    public void send(final Object cmd, final String path, final KvmCommandFailureChecker checker , final long defaulTimeout, final ReturnValueCompletion<KvmResponseWrapper> completion) {
        if (hostUuids.isEmpty()) {
            throw new CloudRuntimeException("no host uuid given");
        }

        if (hostUuids.size() > 1) {
            throw new CloudRuntimeException("please use SteppingSendCallback");
        }

        String huuid = hostUuids.get(0);
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(huuid);
        msg.setPath(path);
        msg.setNoStatusCheck(noStatusCheck);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, huuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply ar = reply.castReply();
                KvmResponseWrapper w = new KvmResponseWrapper(ar.getResponse());

                ErrorCode err = checker.getError(w);
                if (err != null) {
                    completion.fail(err);
                    return;
                }

                completion.success(w);
            }
        });
    }
}
