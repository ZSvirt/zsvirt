package org.zstack.sso.cas.compute;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.EventFacadeImpl;
import org.zstack.core.db.Q;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;
import org.zstack.identity.imports.header.AbstractAccountSourceSpec;
import org.zstack.sso.cas.header.CasAccountSourceSpec;
import org.zstack.sso.header.CasClientInventory;
import org.zstack.sso.header.CasClientVO;
import org.zstack.sso.header.CasClientVO_;
import org.zstack.sso.header.CasState;
import org.zstack.sso.service.SSOCanonicalEvents;
import org.zstack.sso.service.compute.SSOAccountSourceFactory;

import java.util.Map;

import static org.zstack.core.Platform.operr;
import static org.zstack.sso.SSOConstants.CAS_CLIENT_TYPE;

public class CasAccountSourceFactory extends SSOAccountSourceFactory {
    @Autowired
    private EventFacadeImpl eventFacade;

    @Override
    public String type() {
        return CAS_CLIENT_TYPE;
    }

    @Override
    public CasAccountSource createBase(ThirdPartyAccountSourceVO vo) {
        final CasClientVO casClientVO = (vo instanceof CasClientVO) ?
                (CasClientVO) vo :
                databaseFacade.findByUuid(vo.getUuid(), CasClientVO.class);
        if (casClientVO == null) {
            throw new OperationFailureException(operr("unable to find CAS client[uuid=%s]", vo.getUuid()));
        }
        return new CasAccountSource(casClientVO);
    }

    @Override
    protected CasClientVO generateAccountSourceVO(AbstractAccountSourceSpec rawSpec) {
        CasAccountSourceSpec spec = (CasAccountSourceSpec) rawSpec;

        CasClientVO clientVO = new CasClientVO();
        clientVO.setUuid(spec.getUuid());
        clientVO.setResourceName(spec.getName());
        clientVO.setDescription(spec.getDescription());
        clientVO.setType(CAS_CLIENT_TYPE);
        clientVO.setCreateAccountStrategy(spec.getCreatedAccountStrategy());
        clientVO.setDeleteAccountStrategy(spec.getDeleteAccountStrategy());

        clientVO.setLoginMNUrl(spec.getLoginMNUrl());
        clientVO.setRedirectUrl(spec.getRedirectUrl());
        clientVO.setCasServerLoginUrl(spec.getCasServerLoginUrl());
        clientVO.setCasServerUrlPrefix(spec.getCasServerUrlPrefix());
        clientVO.setServerName(spec.getServerName());
        clientVO.setState(CasState.Disabled);
        clientVO.setUsernameProperty(spec.getUsernameProperty());
        return clientVO;
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected Flow buildSSOUrlTemplateFlow(Context context) {
        CasAccountSourceSpec spec = (CasAccountSourceSpec) context.spec;

        return new NoRollbackFlow() {
            String __name__ = "add-sso-url-template";

            @Override
            public boolean skip(Map data) {
                return spec.getUrlTemplate() == null;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                CasClientVO client = (CasClientVO) context.vo;
                createBase(client).createSSORedirectTemplate(spec.getUrlTemplate());
                trigger.next();
            }
        };
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected Flow buildPostCreateSourceFlow(Context context) {
        CasAccountSourceSpec spec = (CasAccountSourceSpec) context.spec;

        return new NoRollbackFlow() {
            String __name__ = "fire-cas-modification-event";

            @Override
            public boolean skip(Map data) {
                return spec.getUrlTemplate() == null;
            }

            @Override
            public void run(FlowTrigger trigger, Map map) {
                CasClientVO client = databaseFacade.findByUuid(spec.getUuid(), CasClientVO.class);
                CasClientInventory inventory = CasClientInventory.valueOf(client);
                SSOCanonicalEvents.CasModifyConfigureData data = new SSOCanonicalEvents.CasModifyConfigureData();
                data.setInventory(inventory);
                eventFacade.fire(SSOCanonicalEvents.CAS_MODIFY_CONFIGURE_PATH, data);
                trigger.next();
            }
        };
    }

    @Override
    protected ErrorCode checkSameSSOServerExists(AbstractAccountSourceSpec rawSpec) {
        CasAccountSourceSpec spec = (CasAccountSourceSpec) rawSpec;
        boolean duplicate = Q.New(CasClientVO.class)
                .eq(CasClientVO_.serverName, spec.getServerName())
                .isExists();
        return duplicate ?
                operr("duplicate CAS server[serverName=%s]", spec.getServerName()) :
                null;
    }
}