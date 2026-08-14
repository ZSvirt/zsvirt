package org.zstack.network.l2.virtualSwitch;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.DeleteL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkConstant;
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
    protected void doDeleteL3Network(DeleteL3NetworkMsg msg, Completion completion) {
        boolean otherPortGroupExists = Q.New(PortGroupVO.class)
                .eq(PortGroupVO_.l2NetworkUuid, getSelf().getL2NetworkUuid())
                .notEq(PortGroupVO_.uuid, msg.getUuid())
                .isExists();
        if (!otherPortGroupExists) {
            DeleteL2NetworkMsg dmsg = new DeleteL2NetworkMsg();
            dmsg.setUuid(getSelf().getL2NetworkUuid());
            dmsg.setForceDelete(msg.isForceDelete());
            bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
            bus.send(dmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                    }
                    completion.success();
                }
            });
        } else {
            super.doDeleteL3Network(msg, completion);
        }
    }
}
