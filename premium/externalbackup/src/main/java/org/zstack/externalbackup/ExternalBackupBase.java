package org.zstack.externalbackup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.mevoco.MevocoGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by MaJin on 2019/12/4.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class ExternalBackupBase implements ExternalBackup {
    private static final CLogger logger = Utils.getLogger(ExternalBackupBase.class);

    @Autowired
    protected CloudBus bus;

    @Autowired
    protected DatabaseFacade dbf;

    protected ExternalBackupVO self;

    public ExternalBackupBase() {
    }

    public ExternalBackupBase(ExternalBackupVO self) {
        this.self = self;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage)msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteExternalBackupMsg) {
            handle((APIDeleteExternalBackupMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof InitExternalBackupMsg) {
            handle((InitExternalBackupMsg) msg);
        } else if (msg instanceof CompleteExternalBackupMsg) {
            handle((CompleteExternalBackupMsg) msg);
        } else if (msg instanceof CancelExternalBackupMsg) {
            handle((CancelExternalBackupMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }

    }

    private void handle(InitExternalBackupMsg msg) {
        InitExternalBackupReply reply = new InitExternalBackupReply();
        MevocoGlobalConfig.PAUSE_THE_WORLD.updateValue(true);
        initHook(new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(CompleteExternalBackupMsg msg) {
        CompleteExternalBackupReply reply = new CompleteExternalBackupReply();
        completeHook(new Completion(msg) {
            @Override
            public void success() {
                MevocoGlobalConfig.PAUSE_THE_WORLD.updateValue(false);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });

    }

    private void handle(CancelExternalBackupMsg msg) {
        CancelExternalBackupReply reply = new CancelExternalBackupReply();
        MevocoGlobalConfig.PAUSE_THE_WORLD.updateValue(false);
        cancelHook(msg.getCancellationApiId(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APIDeleteExternalBackupMsg msg) {
        APIDeleteExternalBackupEvent event = new APIDeleteExternalBackupEvent(msg.getId());
        self.setState(ExternalBackupState.Deleting);
        self = dbf.updateAndRefresh(self);
        deleteHook(new Completion(msg) {
            @Override
            public void success() {
                dbf.remove(self);
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }
}
