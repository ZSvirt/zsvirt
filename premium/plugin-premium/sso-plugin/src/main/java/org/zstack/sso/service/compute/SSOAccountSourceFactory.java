package org.zstack.sso.service.compute;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO_;
import org.zstack.identity.imports.header.AbstractAccountSourceSpec;
import org.zstack.identity.imports.source.AccountSourceFactory;

import java.util.Map;

public abstract class SSOAccountSourceFactory implements AccountSourceFactory {
    @Autowired
    protected DatabaseFacade databaseFacade;

    public static class Context {
        public AbstractAccountSourceSpec spec;
        public ThirdPartyAccountSourceVO vo;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void createAccountSource(AbstractAccountSourceSpec spec, ReturnValueCompletion<ThirdPartyAccountSourceVO> completion) {
        final Context context = new Context();
        context.spec = spec;
        context.vo = null;

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("create-sso-client");
        chain.then(new NoRollbackFlow() {
            String __name__ = "pre-create-check";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                ErrorCode errorCode = checkSameSSOServerExists(spec);
                if (errorCode != null) {
                    trigger.fail(errorCode);
                    return;
                }
                if (spec.getUuid() == null) {
                    spec.setUuid(Platform.getUuid());
                }
                trigger.next();
            }
        }).then(new Flow() {
            String __name__ = "add-VO-in-db";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                ThirdPartyAccountSourceVO clientVO = generateAccountSourceVO(spec);
                context.vo = databaseFacade.persistAndRefresh(clientVO);
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                SQL.New(ThirdPartyAccountSourceVO.class)
                        .eq(ThirdPartyAccountSourceVO_.uuid, spec.getUuid())
                        .delete();
                trigger.rollback();
            }
        });

        Flow flow = buildSSOUrlTemplateFlow(context);
        if (flow != null) {
            chain.then(flow);
        }

        flow = buildPostCreateSourceFlow(context);
        if (flow != null) {
            chain.then(flow);
        }

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success(context.vo);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    protected abstract ThirdPartyAccountSourceVO generateAccountSourceVO(AbstractAccountSourceSpec spec);

    protected abstract ErrorCode checkSameSSOServerExists(AbstractAccountSourceSpec spec);

    protected abstract Flow buildSSOUrlTemplateFlow(Context context);

    protected Flow buildPostCreateSourceFlow(Context context) {
        return null;
    }
}