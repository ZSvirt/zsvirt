package org.zstack.storage.cbt;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.cbt.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CbtBackupBase {
    private static final CLogger logger = Utils.getLogger(CbtBackupBase.class);
    private final Map<String, CbtBackupFactory> bkFactories = new ConcurrentHashMap<>();

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;

    protected CbtTaskVO self;

    protected String syncThreadName;

    public CbtBackupBase(CbtTaskVO self) {
        this.self = self;
        this.syncThreadName = "Cbt-Task-" + self.getUuid();
    }

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteCbtTaskMsg) {
            handle((APIDeleteCbtTaskMsg) msg);
        } else if (msg instanceof APIEnableCbtTaskMsg) {
            handle((APIEnableCbtTaskMsg) msg);
        } else if (msg instanceof APIDisableCbtTaskMsg) {
            handle((APIDisableCbtTaskMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final APIEnableCbtTaskMsg msg) {
        final APIEnableCbtTaskEvent evt = new APIEnableCbtTaskEvent(msg.getId());
        EnableCbtTaskMsg emsg = new EnableCbtTaskMsg();
        emsg.setUuid(msg.getUuid());
        emsg.setAccountUuid(msg.getSession().getAccountUuid());
        emsg.setBitmapName(msg.getBitmapName());
        bus.makeLocalServiceId(emsg, CbtBackupConstant.SERVICE_ID);
        bus.send(emsg, new CloudBusCallBack(emsg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    EnableCbtTaskReply r = reply.castReply();
                    evt.setVolumeCbtBackupInfos(r.getVolumeCbtBackupInfos());
                    bus.publish(evt);
                } else {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                }
            }
        });
    }

    private void handle(final APIDeleteCbtTaskMsg msg) {
        final APIDeleteCbtTaskEvent evt = new APIDeleteCbtTaskEvent(msg.getId());
        DeleteCbtTaskMsg dmsg = new DeleteCbtTaskMsg();
        dmsg.setUuid(msg.getUuid());
        dmsg.setForce(msg.isForce());
        bus.makeLocalServiceId(dmsg, CbtBackupConstant.SERVICE_ID);
        bus.send(dmsg, new CloudBusCallBack(dmsg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    bus.publish(evt);
                } else {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                }
            }
        });
    }

    private void handle(final APIDisableCbtTaskMsg msg) {
        final APIDisableCbtTaskEvent evt = new APIDisableCbtTaskEvent(msg.getId());
        DisableCbtTaskMsg dmsg = new DisableCbtTaskMsg();
        dmsg.setUuid(msg.getUuid());
        dmsg.setForce(msg.isForce());
        bus.makeLocalServiceId(dmsg, CbtBackupConstant.SERVICE_ID);
        bus.send(dmsg, new CloudBusCallBack(dmsg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    bus.publish(evt);
                } else {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                }
            }
        });
    }
}
