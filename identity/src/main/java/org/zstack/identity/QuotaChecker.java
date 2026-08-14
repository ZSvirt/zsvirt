package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.Quota.QuotaPair;
import org.zstack.header.message.APIMessage;

import java.util.List;
import java.util.Map;

/**
 * Created by frank on 7/30/2015.
 */
public class QuotaChecker implements GlobalApiMessageInterceptor {
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public List<Class> getMessageClassToIntercept() {
        return null;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        // login, logout
        if (msg.getSession() == null) {
            return msg;
        }

        // skip admin. if needed, another quota check will be issued in AccountManagerImpl
        if (new QuotaUtil().isAdminAccount(msg.getSession().getAccountUuid())) {
            return msg;
        }

        List<Quota> quotas = acntMgr.getMessageQuotaMap().get(msg.getClass());
        if (quotas == null || quotas.size() == 0) {
            return msg;
        }
        for (Quota q : quotas) {
            check(msg, q);
        }
        return msg;
    }

    private void check(APIMessage msg, Quota quota) {
        Map<String, QuotaPair> pairs = new QuotaUtil().makeQuotaPairs(msg.getSession().getAccountUuid());
        quota.getOperator().checkQuota(msg, pairs);
        if (quota.getQuotaValidators() != null) {
            for (Quota.QuotaValidator q : quota.getQuotaValidators()) {
                q.checkQuota(msg, pairs);
            }
        }
    }
}
