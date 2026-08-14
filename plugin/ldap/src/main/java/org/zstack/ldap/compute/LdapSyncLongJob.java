package org.zstack.ldap.compute;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.identity.imports.AccountImportsConstant;
import org.zstack.identity.imports.entity.SyncCreatedAccountStrategy;
import org.zstack.identity.imports.entity.SyncDeletedAccountStrategy;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;
import org.zstack.identity.imports.message.SyncThirdPartyAccountMsg;
import org.zstack.identity.imports.message.SyncThirdPartyAccountReply;
import org.zstack.ldap.api.APISyncAccountsFromLdapServerEvent;
import org.zstack.ldap.api.APISyncAccountsFromLdapServerMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;
import static org.zstack.longjob.LongJobUtils.*;

@LongJobFor(APISyncAccountsFromLdapServerMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LdapSyncLongJob implements LongJob {
    private static final CLogger logger = Utils.getLogger(LdapSyncLongJob.class);

    private final SyncThirdPartyAccountMsg innerMsg = new SyncThirdPartyAccountMsg();

    @Autowired
    private CloudBus bus;
    @Autowired
    private EventFacade eventFacade;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        APISyncAccountsFromLdapServerMsg apiMessage =
                JSONObjectUtil.toObject(job.getJobData(), APISyncAccountsFromLdapServerMsg.class);
        APISyncAccountsFromLdapServerEvent event = new APISyncAccountsFromLdapServerEvent(job.getApiId());

        innerMsg.setSourceUuid(apiMessage.getUuid());
        if (apiMessage.getCreateAccountStrategy() != null) {
            innerMsg.setCreateAccountStrategy(SyncCreatedAccountStrategy.valueOf(apiMessage.getCreateAccountStrategy()));
        }
        if (apiMessage.getDeleteAccountStrategy() != null) {
            innerMsg.setDeleteAccountStrategy(SyncDeletedAccountStrategy.valueOf(apiMessage.getDeleteAccountStrategy()));
        }

        bus.makeTargetServiceIdByResourceUuid(innerMsg, AccountImportsConstant.SERVICE_ID, apiMessage.getUuid());
        bus.send(innerMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                SyncThirdPartyAccountReply castReply = reply.castReply();
                event.setResult(castReply.getResult());
                setJobResult(job.getUuid(), castReply.getResult());
                completion.success(event);
            }
        });
    }

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        completion.fail(operr("not support"));
    }

    @Override
    public void resume(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        completion.fail(operr("not support"));
    }

    @Override
    public void clean(LongJobVO job, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public Class<?> getAuditType() {
        return ThirdPartyAccountSourceVO.class;
    }

    @Override
    public String getAuditResourceUuid() {
        return innerMsg.getSourceUuid();
    }
}
