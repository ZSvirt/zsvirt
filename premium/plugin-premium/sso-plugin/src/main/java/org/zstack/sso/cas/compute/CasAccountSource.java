package org.zstack.sso.cas.compute;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.EventFacadeImpl;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.Message;
import org.zstack.sso.cas.header.CasAccountSourceSpec;
import org.zstack.sso.header.APIUpdateCasClientEvent;
import org.zstack.sso.header.APIUpdateCasClientMsg;
import org.zstack.sso.header.CasClientInventory;
import org.zstack.sso.header.CasClientVO;
import org.zstack.sso.header.CasState;
import org.zstack.sso.service.SSOCanonicalEvents;
import org.zstack.sso.service.compute.SSOAccountSource;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.operr;
import static org.zstack.identity.imports.AccountImportsManager.accountSourceQueueSyncSignature;
import static org.zstack.sso.SSOConstants.CAS_CLIENT_TYPE;

public class CasAccountSource extends SSOAccountSource {
    @Autowired
    private EventFacadeImpl eventFacade;

    protected CasAccountSource(CasClientVO self) {
        super(self);
    }

    private CasClientVO refreshVO() {
        final CasClientVO clientVO = databaseFacade.findByUuid(self.getUuid(), CasClientVO.class);
        if (clientVO == null) {
            throw new OperationFailureException(operr("casClient[uuid:%s, name:%s] has been deleted",
                    self.getUuid(), self.getResourceName()));
        }
        self = clientVO;
        return clientVO;
    }

    @Override
    public String type() {
        return CAS_CLIENT_TYPE;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIUpdateCasClientMsg) {
            handle((APIUpdateCasClientMsg) msg);
        } else {
            super.handleMessage(msg);
        }
    }

    private void handle(APIUpdateCasClientMsg message) {
        APIUpdateCasClientEvent event = new APIUpdateCasClientEvent(message.getId());
        final String sourceUuid = message.getSourceUuid();

        CasAccountSourceSpec spec = new CasAccountSourceSpec();
        spec.setUuid(message.getUuid());
        spec.setName(message.getName());
        spec.setDescription(message.getDescription());
        spec.setCasServerLoginUrl(message.getCasServerLoginUrl());
        spec.setCasServerUrlPrefix(message.getCasServerUrlPrefix());
        spec.setServerName(message.getServerName());
        spec.setUsernameProperty(message.getUsernameProperty());

        threadFacade.chainSubmit(new ChainTask(message) {
            @Override
            public void run(SyncTaskChain chain) {
                final CasClientInventory inventory = updateCasClient(spec);
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
                return "update-cas-client-" + sourceUuid;
            }
        });
    }

    protected CasClientInventory updateCasClient(CasAccountSourceSpec spec) {
        final CasClientVO vo = refreshVO();
        List<String> disableReasons = new ArrayList<>();

        if (spec.getName() != null) {
            vo.setResourceName(spec.getName());
        }
        if (spec.getDescription() != null) {
            vo.setDescription(spec.getDescription());
        }
        if (spec.getCasServerLoginUrl() != null) {
            vo.setCasServerLoginUrl(spec.getCasServerLoginUrl());
            disableReasons.add("CAS server log-in URL changes");
        }
        if (spec.getCasServerUrlPrefix() != null) {
            vo.setCasServerUrlPrefix(spec.getCasServerUrlPrefix());
            disableReasons.add("CAS server URL prefix changes");
        }
        if (spec.getServerName() != null) {
            vo.setServerName(spec.getServerName());
            disableReasons.add("CAS server IP and port changes");
        }
        if (spec.getRedirectUrl() != null) {
            vo.setRedirectUrl(spec.getRedirectUrl());
        }
        if (spec.getLoginMNUrl() != null) {
            vo.setLoginMNUrl(spec.getLoginMNUrl());
        }
        if (spec.getUsernameProperty() != null) {
            vo.setUsernameProperty(spec.getUsernameProperty());
        }
        if (!disableReasons.isEmpty()) {
            vo.setState(CasState.Disabled);
        }

        CasClientInventory inventory = CasClientInventory.valueOf(databaseFacade.updateAndRefresh(vo));

        if (!disableReasons.isEmpty()) {
            SSOCanonicalEvents.CasModifyConfigureData data = new SSOCanonicalEvents.CasModifyConfigureData();
            data.setInventory(CasClientInventory.valueOf(vo));
            data.setReasons(disableReasons);
            eventFacade.fire(SSOCanonicalEvents.CAS_MODIFY_CONFIGURE_PATH, data);
        }

        return inventory;
    }

    @Override
    protected void destroySource(Completion completion) {
        databaseFacade.removeByPrimaryKey(self.getUuid(), CasClientVO.class);
        completion.success();
    }
}