package org.zstack.iam1.compute.accounts;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.iam1.api.accounts.APICreateAccountGroupEvent;
import org.zstack.iam1.api.accounts.APICreateAccountGroupMsg;
import org.zstack.iam1.api.accounts.AccountGroupMessage;
import org.zstack.iam1.entity.accounts.AccountGroupInventory;
import org.zstack.iam1.entity.accounts.AccountGroupVO;
import org.zstack.iam1.message.accounts.MoveAccountGroupMsg;

import static org.zstack.core.Platform.err;
import static org.zstack.iam1.compute.accounts.AccountGroupConstant.GROUP_FORM_UPDATE_TARGET;

/**
 * Created by Wenhao.Zhang on 2024/08/28
 */
public class AccountGroupManagerImpl extends AbstractService {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade databaseFacade;

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
    public void handleMessage(Message message) {
        if (message instanceof AccountGroupMessage) {
            passThrough((AccountGroupMessage) message);
        } else if (message instanceof APICreateAccountGroupMsg) {
            handle((APICreateAccountGroupMsg) message);
        } else {
            bus.dealWithUnknownMessage(message);
        }
    }

    private void passThrough(AccountGroupMessage message) {
        AccountGroupVO vo = databaseFacade.findByUuid(message.getAccountGroupUuid(), AccountGroupVO.class);
        if (vo == null) {
            bus.replyErrorByMessageType((Message) message, err(SysErrors.RESOURCE_NOT_FOUND,
                    "unable to find AccountGroup[uuid=%s]", message.getAccountGroupUuid()));
            return;
        }
        AccountGroupBase base = new AccountGroupBase(vo);
        base.handleMessage((Message) message);
    }

    private void handle(APICreateAccountGroupMsg message) {
        AccountGroupVO group = new AccountGroupVO();
        String uuid = message.getResourceUuid() == null ? Platform.getUuid() : message.getResourceUuid();

        group.setUuid(uuid);
        group.setName(message.getName());
        group.setDescription(message.getDescription());
        group.setParentUuid(null);
        group.setRootGroupUuid(uuid);
        group = databaseFacade.persistAndRefresh(group);

        if (message.getParentUuid() == null) {
            APICreateAccountGroupEvent event = new APICreateAccountGroupEvent(message.getId());
            event.setInventory(AccountGroupInventory.valueOf(group));
            bus.publish(event);
            return;
        }

        MoveAccountGroupMsg innerMsg = new MoveAccountGroupMsg();
        innerMsg.setUuid(group.getUuid());
        innerMsg.setParentUuid(message.getParentUuid());
        bus.makeTargetServiceIdByResourceUuid(innerMsg, AccountGroupConstant.SERVICE_ID, GROUP_FORM_UPDATE_TARGET);
        bus.send(innerMsg, new CloudBusCallBack(message) {
            @Override
            public void run(MessageReply reply) {
                APICreateAccountGroupEvent event = new APICreateAccountGroupEvent(message.getId());
                if (reply.isSuccess()) {
                    AccountGroupVO currentGroup = databaseFacade.findByUuid(uuid, AccountGroupVO.class);
                    event.setInventory(AccountGroupInventory.valueOf(currentGroup));
                } else {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(AccountGroupConstant.SERVICE_ID);
    }
}
