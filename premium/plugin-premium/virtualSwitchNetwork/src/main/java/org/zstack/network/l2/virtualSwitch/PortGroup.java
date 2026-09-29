package org.zstack.network.l2.virtualSwitch;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l3.APIUpdateL3NetworkMsg;
import org.zstack.network.l2.virtualSwitch.header.APIUpdatePortGroupMsg;
import org.zstack.network.l2.virtualSwitch.header.APIUpdatePortGroupEvent;
import org.zstack.network.l2.virtualSwitch.header.L2PortGroupNetworkInventory;
import org.zstack.network.l2.virtualSwitch.header.L2PortGroupNetworkVO;
import org.zstack.header.core.Completion;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.DeleteL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.DeleteL3NetworkMsg;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.network.l2.virtualSwitch.header.PortGroupInventory;
import org.zstack.network.l2.virtualSwitch.header.PortGroupVO;
import org.zstack.network.l2.virtualSwitch.header.PortGroupVO_;
import org.zstack.network.l2.virtualSwitch.header.VirtualSwitchConstant;
import org.zstack.network.l3.L3BasicNetwork;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.network.l2.virtualSwitch.KVMRealizeL2PortGroupLinuxBridgeBackend.findBackend;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PortGroup extends L3BasicNetwork {
    private static final CLogger logger = Utils.getLogger(PortGroup.class);

    @Autowired
    private ThreadFacade thdf;

    public PortGroup(L3NetworkVO vo) {
        super(vo);
    }

    @Override
    protected PortGroupVO getSelf() {
        return (PortGroupVO) self;
    }

    @Override
    protected L3NetworkInventory getSelfInventory() {
        return PortGroupInventory.valueOf(getSelf());
    }

    @Override
    protected String getSyncId() {
        return String.format(VirtualSwitchConstant.PORT_GROUP_SYNC_ID_FORMAT, getSelf().getvSwitchUuid(), getSelf().getVlanId());
    }

    @Override
    protected String generateChainName() {
        return String.format("port-group-%s", getSelf().getUuid());
    }

    @Override
    protected void handle(APIUpdateL3NetworkMsg msg) {
        if (!(msg instanceof APIUpdatePortGroupMsg)
                || ((APIUpdatePortGroupMsg) msg).getTenantRouterUuid() == null) {
            super.handle(msg);
            return;
        }
        APIUpdatePortGroupMsg updateMsg = (APIUpdatePortGroupMsg) msg;
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public String getName() {
                return "update-port-group-tenant-router-" + msg.getUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                updateTenantRouter(updateMsg, chain);
            }
        });
    }

    private void updateTenantRouter(APIUpdatePortGroupMsg msg, SyncTaskChain chain) {
        PortGroupBackend backend = findBackend(pluginRgty, getSelf().getvSwitchUuid());
        L2PortGroupNetworkVO l2 = dbf.findByUuid(getSelf().getL2NetworkUuid(), L2PortGroupNetworkVO.class);
        if (backend == null || l2 == null) {
            publishTenantRouterUpdateFailure(msg, argerr("tenantRouterUuid requires a ZNS port group"));
            chain.next();
            return;
        }
        backend.updateTenantRouter(L2PortGroupNetworkInventory.valueOf(l2),
                msg.getTenantRouterUuid(), tenantRouterUpdateCompletion(msg, chain));
    }

    private Completion tenantRouterUpdateCompletion(APIUpdatePortGroupMsg msg, SyncTaskChain chain) {
        return new Completion(chain) {
            @Override
            public void success() {
                PortGroup.super.handle(msg);
                chain.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                publishTenantRouterUpdateFailure(msg, errorCode);
                chain.next();
            }
        };
    }

    private void publishTenantRouterUpdateFailure(APIUpdatePortGroupMsg msg, ErrorCode errorCode) {
        APIUpdatePortGroupEvent event = new APIUpdatePortGroupEvent(msg.getId());
        event.setError(errorCode);
        bus.publish(event);
    }

    @Override
    protected void doDeleteL3Network(DeleteL3NetworkMsg msg, Completion completion) {
        boolean otherPortGroupExists = Q.New(PortGroupVO.class)
                .eq(PortGroupVO_.l2NetworkUuid, getSelf().getL2NetworkUuid())
                .notEq(PortGroupVO_.uuid, msg.getUuid())
                .isExists();
        if (!otherPortGroupExists) {
            PortGroupBackend backend = findBackend(
                    pluginRgty, getSelf().getvSwitchUuid());
            if (backend == null) {
                deleteLocalL2Network(msg, completion);
                return;
            }
            L2PortGroupNetworkVO l2 = dbf.findByUuid(getSelf().getL2NetworkUuid(), L2PortGroupNetworkVO.class);
            if (l2 == null) {
                completion.fail(operr("PortGroup L2 network no longer exists"));
                return;
            }
            L2PortGroupNetworkInventory inventory = L2PortGroupNetworkInventory.valueOf(l2);
            Completion checked = new Completion(completion) {
                @Override
                public void success() {
                    backend.beforeLocalDelete(inventory, new Completion(completion) {
                        @Override
                        public void success() {
                            deleteLocalL2Network(msg, completion);
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
                        }
                    });
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            };
            if (msg.isForceDelete()) {
                checked.success();
            } else {
                casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, L2NetworkVO.class.getSimpleName(),
                        Collections.singletonList(inventory), checked);
            }
        } else {
            super.doDeleteL3Network(msg, completion);
        }
    }

    private void deleteLocalL2Network(DeleteL3NetworkMsg msg, Completion completion) {
        DeleteL2NetworkMsg dmsg = new DeleteL2NetworkMsg();
        dmsg.setUuid(getSelf().getL2NetworkUuid());
        dmsg.setForceDelete(msg.isForceDelete());
        bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
        bus.send(dmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                completion.success();
            }
        });
    }
}
