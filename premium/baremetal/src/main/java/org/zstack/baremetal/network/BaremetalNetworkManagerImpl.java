package org.zstack.baremetal.network;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.baremetal.network.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.network.l3.ReturnIpMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by GuoYi on 2019-01-03.
 */
public class BaremetalNetworkManagerImpl extends AbstractService implements BaremetalNetworkManager {
    private static final CLogger logger = Utils.getLogger(BaremetalNetworkManagerImpl.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage(msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(Message msg) {
        if (msg instanceof APICreateBaremetalBondingMsg) {
            handle((APICreateBaremetalBondingMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof DeleteBaremetalNicMsg) {
            handle((DeleteBaremetalNicMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APICreateBaremetalBondingMsg msg) {
        APICreateBaremetalBondingEvent evt = new APICreateBaremetalBondingEvent(msg.getId());
        BaremetalBondingVO bonding = new BaremetalBondingVO();
        if (msg.getResourceUuid() != null) {
            bonding.setUuid(msg.getResourceUuid());
        } else {
            bonding.setUuid(Platform.getUuid());
        }
        bonding.setChassisUuid(msg.getChassisUuid());
        bonding.setName(msg.getName());
        bonding.setMode(msg.getMode());
        bonding.setSlaves(msg.getSlaves());
        bonding.setOpts(msg.getOpts());
        bonding.setAccountUuid(msg.getSession().getAccountUuid());
        bonding = dbf.persistAndRefresh(bonding);
        evt.setInventory(bonding.toInventory());
        bus.publish(evt);
    }

    private void handle(DeleteBaremetalNicMsg msg) {
        DeleteBaremetalNicReply reply = new DeleteBaremetalNicReply();
        BaremetalNicVO nic = dbf.findByUuid(msg.getUuid(), BaremetalNicVO.class);
        // if pxe then no need to return ip
        if (nic.getPxe()) {
            dbf.removeByPrimaryKey(msg.getUuid(), BaremetalNicVO.class);
            bus.reply(msg, reply);
            return;
        }

        ReturnIpMsg rmsg = new ReturnIpMsg();
        rmsg.setUsedIpUuid(nic.getUsedIpUuid());
        rmsg.setL3NetworkUuid(nic.getL3NetworkUuid());
        bus.makeTargetServiceIdByResourceUuid(rmsg, L3NetworkConstant.SERVICE_ID, nic.getL3NetworkUuid());
        bus.send(rmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply rly) {
                dbf.removeByPrimaryKey(msg.getUuid(), BaremetalNicVO.class);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(BaremetalNetworkConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
