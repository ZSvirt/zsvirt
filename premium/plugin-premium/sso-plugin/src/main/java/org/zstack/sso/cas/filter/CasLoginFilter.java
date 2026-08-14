package org.zstack.sso.cas.filter;

import org.jasig.cas.client.authentication.AttributePrincipal;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.identity.imports.AccountImportsConstant;
import org.zstack.identity.imports.entity.SyncCreatedAccountStrategy;
import org.zstack.identity.imports.header.ImportAccountResult;
import org.zstack.identity.imports.message.ImportThirdPartyAccountMsg;
import org.zstack.identity.imports.message.ImportThirdPartyAccountReply;
import org.zstack.sso.SSOConstants;
import org.zstack.sso.cas.header.CasUserToken;
import org.zstack.sso.cas.service.CasLogin;
import org.zstack.sso.header.CasClientVO;
import org.zstack.sso.header.CasClientVO_;
import org.zstack.sso.oauth2.AuthCache;
import org.zstack.sso.service.SSOManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.sso.header.SSOErrors.*;


/**
 * @Author: DaoDao
 * @Date: 2022/8/26
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CasLoginFilter implements Filter {
    private static final CLogger logger = Utils.getLogger(CasLoginFilter.class);
    @Autowired
    private CasLogin casLogin;
    @Autowired
    private SSOManager sso;
    @Autowired
    private CloudBus bus;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String clientUuid = request.getParameter(SSOConstants.CLIENT_UUID_NAME);
        if (clientUuid == null) {
            throw new OperationFailureException(operr("url is error, clientUuid is miss"));
        }

        CasClientVO vo = Q.New(CasClientVO.class)
                .eq(CasClientVO_.uuid, clientUuid)
                .find();
        if (vo == null) {
            throw new OperationFailureException(operr(" missing cas client, please create cas client before sso"));
        }

        AttributePrincipal principal = (AttributePrincipal) ((HttpServletRequest)request).getUserPrincipal();
        if (principal == null) {
            chain.doFilter(request, response);
            return;
        }

        CasUserToken token = new CasUserToken();
        token.clientUuid = vo.getUuid();
        token.attributes.putAll(principal.getAttributes()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (String) e.getValue())));
        token.usernameProperty = vo.getUsernameProperty();
        token.username = principal.getName();
        ErrorableValue<SessionInventory> session = loginByToken(token);

        if (session.isSuccess()) {
            AuthCache cache = new AuthCache();
            cache.setZoneUuid(request.getParameter(SSOConstants.ZONEUUID_NAME));
            cache.setProjectUuid(request.getParameter(SSOConstants.PROJECTUUID_NAME));
            cache.setRedirectUrl(request.getParameter(SSOConstants.REDIRECT_NAME));

            sso.redirect((HttpServletRequest) request, (HttpServletResponse) response, session.result, vo.getUuid(), cache);
        } else {
            logger.warn(String.format("failed to login CAS token[%s]: %s",
                        token.username, session.error.getDetails()));
        }
    }

    /**
     * This method does NOT allow the use of Completion:
     * The outer caller is within @RequestMapping, and the web.xml (version 2.5) does not support this feature:
     *
     * {@code <async-supported>true</async-supported>}
     */
    protected ErrorableValue<SessionInventory> loginByToken(CasUserToken token) {
        final ErrorableValue<SessionInventory> session = casLogin.createSessionWithToken(token);
        if (session.isSuccess()) {
            return ErrorableValue.of(session.result);
        } else if (!session.error.isError(SSO_ACCOUNT_NOT_FOUND)) {
            return ErrorableValue.ofErrorCode(session.error);
        }

        if (!needCreateAccountWhenUserFirstLogIn(token.clientUuid)) {
            return ErrorableValue.ofErrorCode(session.error);
        }

        ImportThirdPartyAccountMsg innerMsg = new ImportThirdPartyAccountMsg();
        innerMsg.setSpec(casLogin.generateImportAccountSpec(token));
        innerMsg.setTimeout(TimeUnit.SECONDS.toMillis(3));

        bus.makeTargetServiceIdByResourceUuid(innerMsg, AccountImportsConstant.SERVICE_ID, innerMsg.getSourceUuid());
        final MessageReply reply = bus.call(innerMsg);
        if (reply.isSuccess()) {
            ImportThirdPartyAccountReply castReply = reply.castReply();
            final ImportAccountResult result = castReply.getResults().get(0);
            if (result.getError() == null) {
                return ErrorableValue.of(casLogin.createSession(result.getRef().getAccountUuid()));
            } else {
                return ErrorableValue.ofErrorCode(result.getError());
            }
        } else {
            return ErrorableValue.ofErrorCode(reply.getError());
        }
    }

    private boolean needCreateAccountWhenUserFirstLogIn(String accountSourceUuid) {
        final SyncCreatedAccountStrategy strategy = Q.New(CasClientVO.class)
                .eq(CasClientVO_.uuid, accountSourceUuid)
                .select(CasClientVO_.createAccountStrategy)
                .findValue();
        return strategy == SyncCreatedAccountStrategy.CreateAccount;
    }

    @Override
    public void destroy() {

    }
}

