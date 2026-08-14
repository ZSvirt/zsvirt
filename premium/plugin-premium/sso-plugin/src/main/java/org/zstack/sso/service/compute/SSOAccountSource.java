package org.zstack.sso.service.compute;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.message.Message;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;
import org.zstack.identity.imports.header.SyncTaskResult;
import org.zstack.identity.imports.header.SyncTaskSpec;
import org.zstack.identity.imports.source.AbstractAccountSourceBase;
import org.zstack.sso.SSOConstants;
import org.zstack.sso.header.APICreateSSORedirectTemplateEvent;
import org.zstack.sso.header.APICreateSSORedirectTemplateMsg;
import org.zstack.sso.header.APIDeleteSSORedirectTemplateEvent;
import org.zstack.sso.header.APIDeleteSSORedirectTemplateMsg;
import org.zstack.sso.header.APIUpdateSSORedirectTemplateEvent;
import org.zstack.sso.header.APIUpdateSSORedirectTemplateMsg;
import org.zstack.sso.header.SSORedirectTemplateInventory;
import org.zstack.sso.header.SSORedirectTemplateVO;
import org.zstack.sso.header.SSORedirectTemplateVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;
import static org.zstack.identity.imports.AccountImportsManager.accountSourceQueueSyncSignature;

public abstract class SSOAccountSource extends AbstractAccountSourceBase {
    private static final CLogger logger = Utils.getLogger(SSOAccountSource.class);

    @Autowired
    protected DatabaseFacade databaseFacade;

    protected SSOAccountSource(ThirdPartyAccountSourceVO self) {
        super(self);
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIUpdateSSORedirectTemplateMsg) {
            handle((APIUpdateSSORedirectTemplateMsg) msg);
        } else if (msg instanceof APICreateSSORedirectTemplateMsg) {
            handle((APICreateSSORedirectTemplateMsg) msg);
        } else if (msg instanceof APIDeleteSSORedirectTemplateMsg) {
            handle((APIDeleteSSORedirectTemplateMsg) msg);
        } else {
            super.handleMessage(msg);
        }
    }

    private void handle(APIUpdateSSORedirectTemplateMsg message) {
        APIUpdateSSORedirectTemplateEvent event = new APIUpdateSSORedirectTemplateEvent(message.getId());
        final String sourceUuid = message.getSourceUuid();

        threadFacade.chainSubmit(new ChainTask(message) {
            @Override
            public void run(SyncTaskChain chain) {
                final SSORedirectTemplateInventory inventory = updateRedirectTemplate(message);
                chain.next();
                event.setInventory(inventory);
                bus.publish(event);
            }

            @Override
            public String getSyncSignature() {
                return accountSourceQueueSyncSignature(sourceUuid);
            }

            @Override
            public String getName() {
                return "update-template-for-oauth2-client-" + sourceUuid;
            }
        });
    }

    protected SSORedirectTemplateInventory updateRedirectTemplate(APIUpdateSSORedirectTemplateMsg message) {
        if (message.getRedirectTemplate() != null) {
            SQL.New(SSORedirectTemplateVO.class)
                    .eq(SSORedirectTemplateVO_.uuid, message.getUuid())
                    .set(SSORedirectTemplateVO_.redirectTemplate, message.getRedirectTemplate())
                    .update();
        }

        SSORedirectTemplateVO template = Q.New(SSORedirectTemplateVO.class)
                .eq(SSORedirectTemplateVO_.uuid, message.getUuid())
                .find();
        return SSORedirectTemplateInventory.valueOf(template);
    }

    private void handle(APICreateSSORedirectTemplateMsg message) {
        APICreateSSORedirectTemplateEvent event = new APICreateSSORedirectTemplateEvent(message.getId());
        final String sourceUuid = message.getSourceUuid();

        threadFacade.chainSubmit(new ChainTask(message) {
            @Override
            public void run(SyncTaskChain chain) {
                SSORedirectTemplateVO template = new SSORedirectTemplateVO();
                template.setUuid(message.getResourceUuid() == null ? Platform.getUuid() : message.getResourceUuid());
                template.setName(message.getName());
                template.setDescription(message.getDescription());
                template.setClientUuid(message.getClientUuid());
                template.setRedirectTemplate(message.getRedirectTemplate());
                databaseFacade.persistAndRefresh(template);
                chain.next();

                final SSORedirectTemplateInventory inventory = SSORedirectTemplateInventory.valueOf(template);
                event.setInventory(inventory);
                bus.publish(event);
            }

            @Override
            public String getSyncSignature() {
                return accountSourceQueueSyncSignature(sourceUuid);
            }

            @Override
            public String getName() {
                return "create-template-for-oauth2-client-" + sourceUuid;
            }
        });
    }

    private void handle(APIDeleteSSORedirectTemplateMsg message) {
        APIDeleteSSORedirectTemplateEvent event = new APIDeleteSSORedirectTemplateEvent(message.getId());
        final String sourceUuid = message.getSourceUuid();

        threadFacade.chainSubmit(new ChainTask(message) {
            @Override
            public void run(SyncTaskChain chain) {
                SQL.New(SSORedirectTemplateVO.class)
                        .eq(SSORedirectTemplateVO_.clientUuid, sourceUuid)
                        .eq(SSORedirectTemplateVO_.uuid, message.getUuid())
                        .delete();

                chain.next();
                bus.publish(event);
            }

            @Override
            public String getSyncSignature() {
                return accountSourceQueueSyncSignature(sourceUuid);
            }

            @Override
            public String getName() {
                return "delete-template-for-oauth2-client-" + sourceUuid;
            }
        });
    }

    @Override
    protected void syncAccountsFromSource(SyncTaskSpec spec, ReturnValueCompletion<SyncTaskResult> completion) {
        completion.fail(operr("not support"));
    }

    public void createSSORedirectTemplate(String urlTemplate) {
        SSORedirectTemplateVO templateVO = new SSORedirectTemplateVO();
        templateVO.setUuid(Platform.getUuid());
        templateVO.setClientUuid(self.getUuid());
        templateVO.setRedirectTemplate(urlTemplate);
        templateVO.setName(SSOConstants.URL_TEMPLATE);
        databaseFacade.persist(templateVO);
    }
}