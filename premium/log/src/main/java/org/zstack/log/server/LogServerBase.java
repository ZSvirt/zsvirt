package org.zstack.log.server;

import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;

import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;

public class LogServerBase {
    private CloudBus bus;
    private DatabaseFacade dbf;
    protected ThreadFacade thdf;
    private CascadeFacade casf;

    protected LogServerVO self;

    protected String syncThreadName;

    public LogServerBase(LogServerVO self, CloudBus bus, DatabaseFacade dbf, ThreadFacade thdf, CascadeFacade casf) {
        this.self = self;
        this.bus = bus;
        this.dbf = dbf;
        this.thdf = thdf;
        this.casf = casf;
        this.syncThreadName = "log-server-" + self.getUuid();
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
        if (msg instanceof DeleteLogServerConfigurationMsg) {
            handle((DeleteLogServerConfigurationMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(DeleteLogServerConfigurationMsg msg) {
        DeleteLogServerConfigurationReply reply = new DeleteLogServerConfigurationReply();
        deleteLogServer(new Completion(msg) {
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

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateLogServerMsg) {
            handle((APIUpdateLogServerMsg) msg);
        } else if (msg instanceof APIDeleteLogServerMsg) {
            handle((APIDeleteLogServerMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIDeleteLogServerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                final APIDeleteLogServerEvent evt = new APIDeleteLogServerEvent(msg.getId());
                deleteLogServer(new Completion(msg) {
                    @Override
                    public void success() {
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void deleteLogServer(Completion completion) {
        final String issuer = LogServerVO.class.getSimpleName();
        final List<LogServerInventory> ctx = list(LogServerInventory.rawValueOf(self));

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-log-server-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(APIUpdateLogServerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIUpdateLogServerEvent evt = new APIUpdateLogServerEvent(msg.getId());
                LogServerVO vo = dbf.reload(self);
                if (vo == null) {
                    evt.setError(org.zstack.core.Platform.err(org.zstack.header.errorcode.SysErrors.RESOURCE_NOT_FOUND,
                            "LogServer[uuid:%s] not found", msg.getUuid()));
                    bus.publish(evt);
                    chain.next();
                    return;
                }
                if (msg.getName() != null) {
                    vo.setName(msg.getName());
                }
                if (msg.getDescription() != null) {
                    vo.setDescription(msg.getDescription());
                }
                vo = dbf.updateAndRefresh(vo);
                evt.setInventory(LogServerInventory.valueOf(vo));
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }
}
