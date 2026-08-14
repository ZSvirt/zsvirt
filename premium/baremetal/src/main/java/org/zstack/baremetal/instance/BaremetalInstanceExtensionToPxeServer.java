package org.zstack.baremetal.instance;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.baremetal.BaremetalConstant;
import org.zstack.header.baremetal.instance.*;
import org.zstack.header.baremetal.network.BaremetalNicVO;
import org.zstack.header.baremetal.network.BaremetalNicVO_;
import org.zstack.header.baremetal.pxeserver.*;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionUtils.transform;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by GuoYi on 2018-10-30.
 */
public class BaremetalInstanceExtensionToPxeServer implements BaremetalPxeServerAttachExtensionPoint, BaremetalPxeServerDetachExtensionPoint {
    private static final CLogger logger = Utils.getLogger(BaremetalInstanceExtensionToPxeServer.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected EventFacade evf;

    @Override
    public void preAttachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid) {

    }

    @Override
    public void beforeAttachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid) {

    }

    @Override
    public void failToAttachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid) {

    }

    @Override
    public void afterAttachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid) {
        // re-create proxy for all provisioned bm instances in this cluster
        List<BaremetalInstanceVO> bms = new SQLBatchWithReturn<List<BaremetalInstanceVO>>() {
            @Override
            protected List<BaremetalInstanceVO> scripts() {
                List<BaremetalInstanceVO> bms = q(BaremetalInstanceVO.class)
                        .eq(BaremetalInstanceVO_.status, BaremetalInstanceStatus.Provisioned)
                        .eq(BaremetalInstanceVO_.clusterUuid, clusterUuid)
                        .isNull(BaremetalInstanceVO_.pxeServerUuid)
                        .list();
                if (bms.isEmpty()) {
                    return bms;
                }

                // set pxeServerUuid of bm instances in cluster
                // FIXME WHEN WE DECIDE TO SUPPORT MORE THAN ONE PXESERVER IN A CLUSTER
                sql(BaremetalInstanceVO.class)
                        .isNull(BaremetalInstanceVO_.pxeServerUuid)
                        .eq(BaremetalInstanceVO_.clusterUuid, clusterUuid)
                        .set(BaremetalInstanceVO_.pxeServerUuid, inventory.getUuid())
                        .update();
                return bms;
            }
        }.execute();

        if (bms.isEmpty()) {
            return;
        }

        List<CreateBaremetalTerminalProxyMsg> cmsgs = transform(bms, bm -> {
            CreateBaremetalTerminalProxyMsg cmsg = new CreateBaremetalTerminalProxyMsg();
            cmsg.setBaremetalInstanceUuid(bm.getUuid());
            cmsg.setPxeServerUuid(inventory.getUuid());
            cmsg.setUpstream(String.format(
                    "location /%s { proxy_pass http://%s:%s/; }",
                    bm.getUuid(),
                    getPxeBootIpAddress(bm.getUuid()),
                    BaremetalConstant.SHELLINABOXD_PORT
            ));
            bus.makeTargetServiceIdByResourceUuid(cmsg, BaremetalPxeServerConstant.SERVICE_ID, inventory.getUuid());
            return cmsg;
        });

        if (cmsgs.isEmpty()) {
            return;
        }

        new While<>(cmsgs).all((cmsg, whileCompletion) -> bus.send(cmsg, new CloudBusCallBack(whileCompletion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.error(reply.getError().getDetails());
                }
                whileCompletion.done();
            }
        })).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                // new nginx proxy
                List<String> bmUuids = transformAndRemoveNull(cmsgs, CreateBaremetalTerminalProxyMsg::getBaremetalInstanceUuid);
                for (String bmUuid : bmUuids) {
                    evf.fire(BaremetalInstanceCanonicalEvents.CREATE_TERMINAL_NGINX_PROXY, bmUuid);
                }

                logger.debug(String.format(
                        "recreated terminal proxy on pxeserver[uuid:%s] for bm instances in cluster[uuid:%s]",
                        inventory.getUuid(), clusterUuid
                ));
            }
        });
    }

    @Override
    public void preDetachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid) {

    }

    @Override
    public void beforeDetachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid) {

    }

    @Override
    public void failToDetachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid) {

    }

    @Override
    public void afterDetachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid) {
        List<String> bmUuids = new SQLBatchWithReturn<List<String>>() {
            @Override
            protected List<String> scripts() {
                List<String> bmUuids = q(BaremetalInstanceVO.class)
                        .eq(BaremetalInstanceVO_.clusterUuid, clusterUuid)
                        .eq(BaremetalInstanceVO_.pxeServerUuid, inventory.getUuid())
                        .select(BaremetalInstanceVO_.uuid)
                        .listValues();
                if (bmUuids.isEmpty()) {
                    return bmUuids;
                }

                // set pxeserverUuid of the provisioned bm instances to null
                sql(BaremetalInstanceVO.class)
                        .in(BaremetalInstanceVO_.uuid, bmUuids)
                        .eq(BaremetalInstanceVO_.status, BaremetalInstanceStatus.Provisioned)
                        .set(BaremetalInstanceVO_.pxeServerUuid, null)
                        .update();
                return bmUuids;
            }
        }.execute();

        if (bmUuids.isEmpty()) {
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("bm-after-detach-baremetal-pxeserver");
        chain.then(new NoRollbackFlow() {
            String __name__ = "try to delete novnc proxy on pxeserver";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<DeleteBaremetalNoVNCProxyMsg> dmsgs = transform(bmUuids, bmUuid -> {
                    DeleteBaremetalNoVNCProxyMsg dmsg = new DeleteBaremetalNoVNCProxyMsg();
                    dmsg.setPxeServerUuid(inventory.getUuid());
                    dmsg.setBaremetalInstanceUuid(bmUuid);
                    bus.makeTargetServiceIdByResourceUuid(dmsg, BaremetalPxeServerConstant.SERVICE_ID, inventory.getUuid());
                    return dmsg;
                });
                if (dmsgs.isEmpty()) {
                    trigger.next();
                    return;
                }

                new While<>(dmsgs).all((dmsg, whileCompletion) -> bus.send(dmsg, new CloudBusCallBack(whileCompletion) {
                    @Override
                    public void run(MessageReply reply) {
                        whileCompletion.done();
                    }
                })).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        logger.debug(String.format("clean up novnc proxy on pxeserver[uuid:%s] after detaching from cluster[uuid:%s]",
                                inventory.getUuid(), clusterUuid
                        ));
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "try to delete terminal proxy on pxeserver";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<DeleteBaremetalTerminalProxyMsg> dmsgs = transform(bmUuids, bmUuid -> {
                    DeleteBaremetalTerminalProxyMsg dmsg = new DeleteBaremetalTerminalProxyMsg();
                    dmsg.setPxeServerUuid(inventory.getUuid());
                    dmsg.setBaremetalInstanceUuid(bmUuid);
                    bus.makeTargetServiceIdByResourceUuid(dmsg, BaremetalPxeServerConstant.SERVICE_ID, inventory.getUuid());
                    return dmsg;
                });
                if (dmsgs.isEmpty()) {
                    trigger.next();
                    return;
                }

                new While<>(dmsgs).all((dmsg, whileCompletion) -> bus.send(dmsg, new CloudBusCallBack(whileCompletion) {
                    @Override
                    public void run(MessageReply reply) {
                        whileCompletion.done();
                    }
                })).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        logger.debug(String.format("clean up terminal proxy on pxeserver[uuid:%s] after detaching from cluster[uuid:%s]",
                                inventory.getUuid(), clusterUuid
                        ));
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete bm instance that are not provisioned";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> unprovisioneds = Q.New(BaremetalInstanceVO.class)
                        .in(BaremetalInstanceVO_.uuid, bmUuids)
                        .notEq(BaremetalInstanceVO_.status, BaremetalInstanceStatus.Provisioned)
                        .select(BaremetalInstanceVO_.uuid)
                        .listValues();
                if (unprovisioneds.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<DestroyBaremetalInstanceMsg> dmsgs = transform(unprovisioneds, bmUuid -> {
                    DestroyBaremetalInstanceMsg dmsg = new DestroyBaremetalInstanceMsg();
                    dmsg.setUuid(bmUuid);
                    dmsg.setDeletionPolicy(BaremetalInstanceDeletionPolicyManager.BaremetalInstanceDeletionPolicy.Direct.toString());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, BaremetalInstanceConstant.SERVICE_ID, bmUuid);
                    return dmsg;
                });
                new While<>(dmsgs).all((dmsg, whileCompletion) -> bus.send(dmsg, new CloudBusCallBack(whileCompletion) {
                    @Override
                    public void run(MessageReply reply) {
                        whileCompletion.done();
                    }
                })).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        logger.debug(String.format("deleted all baremetal instance that are not provisioned in cluster[uuid:%s]", clusterUuid));
                        trigger.next();
                    }
                });
            }
        });

        chain.done(new FlowDoneHandler(null) {
            @Override
            public void handle(Map data) {
                logger.debug(String.format("successfully cleaned up proxys on pxeserver[uuid:%s] " +
                        "and deleted not provisioned bm instances in cluster[uuid:%s]",
                        inventory.getUuid(), clusterUuid
                ));
            }
        }).error(new FlowErrorHandler(null) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                logger.error(String.format("failed to clean up proxys on pxeserver[uuid:%s] " +
                                "and delete not provisioned bm instances in cluster[uuid:%s]",
                        inventory.getUuid(), clusterUuid
                ));
            }
        }).start();
    }

    private Object getPxeBootIpAddress(String bmUuid) {
        return Q.New(BaremetalNicVO.class)
                .eq(BaremetalNicVO_.baremetalInstanceUuid, bmUuid)
                .eq(BaremetalNicVO_.pxe, true)
                .select(BaremetalNicVO_.ip)
                .limit(1)
                .findValue();
    }
}
