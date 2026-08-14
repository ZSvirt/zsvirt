package org.zstack.managements.compute.ha2;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.header.AbstractService;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.managements.api.common.APIGetManagementNodesStatusMsg;
import org.zstack.managements.api.common.APIGetManagementNodesStatusReply;
import org.zstack.managements.api.ha2.*;
import org.zstack.managements.entity.common.ManagementNodeStatusView;
import org.zstack.managements.entity.common.ManagementsStatusView;
import org.zstack.managements.entity.ha2.ZSha2StatusView;
import org.zstack.managements.header.h2.GetZSha2StatusMsg;
import org.zstack.managements.header.h2.GetZSha2StatusReply;

import static org.zstack.managements.header.PremiumManagementsConstant.*;
import static org.zstack.utils.CollectionDSL.list;

public class ManagementsHa2Manager extends AbstractService {
    @Autowired
    private CloudBus bus;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            if (msg instanceof APIGetManagementNodesStatusMsg) {
                handle((APIGetManagementNodesStatusMsg) msg);
            } else if (msg instanceof APIGetZSha2StatusMsg) {
                handle((APIGetZSha2StatusMsg) msg);
            } else if (msg instanceof APIZSha2DemoteMsg) {
                handle((APIZSha2DemoteMsg) msg);
            } else {
                bus.dealWithUnknownMessage(msg);
            }
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof GetZSha2StatusMsg) {
            handle((GetZSha2StatusMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(HA2_SERVICE_ID);
    }

    private void handle(APIGetManagementNodesStatusMsg msg) {
        APIGetManagementNodesStatusReply reply = new APIGetManagementNodesStatusReply();

        ZSha2Client client = createLocalClient();
        if (client.isHa2Installed()) {
            final ErrorableValue<ZSha2StatusView> holder = client.getStatusInfo();
            if (holder.isSuccess()) {
                reply.setInventory(holder.result.toManagementsStatusView());
            } else {
                reply.setError(holder.error);
            }
        } else {
            ManagementsStatusView view = new ManagementsStatusView();
            ManagementNodeStatusView currentNode = new ManagementNodeStatusView();
            currentNode.setIp(Platform.getManagementServerIp());
            currentNode.setManagementsNodeStatus("running");
            view.setNodes(list(currentNode));
            reply.setInventory(view);
        }

        bus.reply(msg, reply);
    }

    private void handle(APIGetZSha2StatusMsg msg) {
        APIGetZSha2StatusReply reply = new APIGetZSha2StatusReply();

        ZSha2Client client = createLocalClient();
        final ErrorableValue<ZSha2StatusView> holder = client.getStatusInfo();
        if (holder.isSuccess()) {
            reply.setInventory(holder.result);
        } else {
            reply.setError(holder.error);
        }

        bus.reply(msg, reply);
    }

    private void handle(GetZSha2StatusMsg msg) {
        GetZSha2StatusReply reply = new GetZSha2StatusReply();

        ZSha2Client client = createLocalClient();
        if (client.isHa2Installed()) {
            final ErrorableValue<ZSha2StatusView> holder = client.getStatusInfo();
            if (holder.isSuccess()) {
                reply.setInventory(holder.result.toManagementsStatusView());
            } else {
                reply.setError(holder.error);
            }
        } else {
            ManagementsStatusView view = new ManagementsStatusView();
            ManagementNodeStatusView currentNode = new ManagementNodeStatusView();
            currentNode.setIp(Platform.getManagementServerIp());
            currentNode.setManagementsNodeStatus("running");
            view.setNodes(list(currentNode));
            reply.setInventory(view);
        }
        bus.reply(msg, reply);
    }

    private void handle(APIZSha2DemoteMsg msg) {
        APIZSha2DemoteEvent event = new APIZSha2DemoteEvent(msg.getId());

        ZSha2Client client = createLocalClient()
                .withSession(msg.getSession().getUuid());
        final ErrorCode errorCode = client.demote();
        if (errorCode != null) {
            event.setError(errorCode);
        }

        bus.publish(event);
    }

    private ZSha2Client createLocalClient() {
        return Platform.New(ZSha2Client::new);
    }
}
