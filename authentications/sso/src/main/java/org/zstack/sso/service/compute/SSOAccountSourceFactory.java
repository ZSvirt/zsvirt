package org.zstack.sso.service.compute;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO_;
import org.zstack.identity.imports.header.AbstractAccountSourceSpec;
import org.zstack.identity.imports.source.AccountSourceFactory;

public abstract class SSOAccountSourceFactory implements AccountSourceFactory {
    @Autowired
    protected DatabaseFacade databaseFacade;

    public static class Context {
        public AbstractAccountSourceSpec spec;
        public ThirdPartyAccountSourceVO vo;
    }

    @Override
    public void createAccountSource(AbstractAccountSourceSpec spec, ReturnValueCompletion<ThirdPartyAccountSourceVO> completion) {
        final Context context = new Context();
        context.spec = spec;
        context.vo = null;

        SimpleFlowChain chain = SimpleFlowChain.of("create-sso-client")

        .then(Flow.of("pre-create-check")
            .handle(() -> checkSameSSOServerExists(spec))
            .build())
        .then(Flow.of("complete-spec")
            .handle(trigger -> {
                if (spec.getUuid() == null) {
                    spec.setUuid(Platform.getUuid());
                }
                trigger.next();
            })
            .build())
        .then(Flow.of("add-VO-in-db")
            .handle(trigger -> {
                ThirdPartyAccountSourceVO clientVO = generateAccountSourceVO(spec);
                context.vo = databaseFacade.persistAndRefresh(clientVO);
                trigger.next();
            })
            .rollback(trigger -> {
                SQL.New(ThirdPartyAccountSourceVO.class)
                        .eq(ThirdPartyAccountSourceVO_.uuid, spec.getUuid())
                        .delete();
                trigger.rollback();
            })
            .build());

        Flow flow = buildSSOUrlTemplateFlow(context);
        if (flow != null) {
            chain.then(flow);
        }

        flow = buildPostCreateSourceFlow(context);
        if (flow != null) {
            chain.then(flow);
        }

        chain.propagateExceptionTo(completion)
            .done(() -> completion.success(context.vo))
            .error(completion::fail)
            .start();
    }

    protected abstract ThirdPartyAccountSourceVO generateAccountSourceVO(AbstractAccountSourceSpec spec);

    protected abstract ErrorCode checkSameSSOServerExists(AbstractAccountSourceSpec spec);

    protected abstract Flow buildSSOUrlTemplateFlow(Context context);

    protected Flow buildPostCreateSourceFlow(Context context) {
        return null;
    }
}