package org.zstack.sso.oauth2.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventFacadeImpl;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.rest.TimeoutRestTemplate;
import org.zstack.identity.Session;
import org.zstack.identity.LogoutExtensionPoint;
import org.zstack.identity.imports.AccountImportsConstant;
import org.zstack.identity.imports.entity.SyncCreatedAccountStrategy;
import org.zstack.identity.imports.header.ImportAccountResult;
import org.zstack.identity.imports.message.ImportThirdPartyAccountMsg;
import org.zstack.identity.imports.message.ImportThirdPartyAccountReply;
import org.zstack.sso.SSOConstants;
import org.zstack.sso.header.*;
import org.zstack.sso.oauth2.AuthCache;
import org.zstack.sso.oauth2.AuthGolabalProperty;
import org.zstack.sso.oauth2.OAuth2Constants;
import org.zstack.sso.oauth2.header.OAuth2UserToken;
import org.zstack.sso.service.SSOManager;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.sso.header.SSOErrors.*;
import static org.zstack.sso.oauth2.OAuth2Constants.KEY_SID;
import static org.zstack.sso.oauth2.OAuth2Constants.KEY_SUB;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_RESPONSE_OUTPUT;

/**
 * @Author: DaoDao
 * @Date: 2022/8/24
 */
public class OAuth2ManagerImpl implements RenewSessionPreAuthExtensionPoint, LogoutExtensionPoint {

    private static final CLogger logger = Utils.getLogger(OAuth2ManagerImpl.class);

    private Cache<String, AuthCache> authStateCache = CacheBuilder.newBuilder().
            maximumSize(1000).expireAfterAccess(10, TimeUnit.MINUTES).build();

    @Autowired
    private PluginRegistry pluginRegistry;
    @Autowired
    private EventFacadeImpl evtf;
    @Autowired
    private SSOManager sso;
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private OAuth2Login loginBackend;
    
    private final static TimeoutRestTemplate http = RESTFacade.createRestTemplate(CoreGlobalProperty.REST_FACADE_READ_TIMEOUT,
            CoreGlobalProperty.REST_FACADE_CONNECT_TIMEOUT);

    public void oauth2Redirect(HttpServletRequest request, HttpServletResponse response) {
        try {
            String clientUuid = request.getParameter(SSOConstants.CLIENT_UUID_NAME);
            if (clientUuid == null) {
                sendResponse(OAuth2Constants.AUTH_RESPONSE_SUCCESS, "url is wrong: clientUuid is required", response);
                return;
            }

            OAuth2ClientVO auth2ClientVO = dbf.findByUuid(clientUuid, OAuth2ClientVO.class);
            if (auth2ClientVO == null) {
                sendResponse(OAuth2Constants.AUTH_RESPONSE_SUCCESS,
                        String.format("client[uuid=%s] not found", clientUuid),
                        response);
                return;
            }

            String code = request.getParameter(OAuth2Constants.OAUTH2_CODE);
            String state = request.getParameter(OAuth2Constants.AUTH2_STATE);
            String accessToken = request.getParameter(OAuth2Constants.ACCESS_TOKEN_VALUE);
            String idToken = request.getParameter(OAuth2Constants.ID_TOKEN_VALUE);
            String refreshToken = request.getParameter(OAuth2Constants.REFRESH_TOKEN_VALUE);

            AuthCache cache = null;
            if (!StringUtils.isEmpty(state)) {
                cache = authStateCache.getIfPresent(state);
            }

            if (cache == null) {
                cache = new AuthCache();
                cache.setClientUuid(request.getParameter(SSOConstants.CLIENT_UUID_NAME));
                cache.setRedirectUrl(request.getParameter(SSOConstants.REDIRECT_NAME));
                cache.setProjectUuid(request.getParameter(SSOConstants.PROJECTUUID_NAME));
                cache.setZoneUuid(request.getParameter(SSOConstants.ZONEUUID_NAME));
            }

            StringBuilder redirectUrlBuffer = new StringBuilder(request.getRequestURL().toString());
            if (request.getQueryString() != null) {
                redirectUrlBuffer.append("?").append(request.getQueryString());
            }

            if (!StringUtils.isEmpty(accessToken) || !StringUtils.isEmpty(idToken) || !StringUtils.isEmpty(refreshToken)) {
                tokenIntrospect(accessToken, idToken, refreshToken, auth2ClientVO, cache, request, response);
                return;
            }

            if (StringUtils.isEmpty(code)) {
                String thirdPartyRedirectUrl = StringUtils.isEmpty(auth2ClientVO.getRedirectUrl()) ? redirectUrlBuffer.toString() : auth2ClientVO.getRedirectUrl();
                authorize(request, response, thirdPartyRedirectUrl);
                if (logger.isTraceEnabled()) {
                    logger.trace("authorize redirect_uri:" + thirdPartyRedirectUrl);
                }
                return;
            }

            MultiValueMap<String, String> httpMap = generateRequestCode(code, cache.getThirdPartyRedirectUrl(),
                    auth2ClientVO.getClientId(), auth2ClientVO.getClientSecret());

            OAuth2Response oauth2Response = getOAuth2Response(httpMap, auth2ClientVO.getTokenUrl(), null, HttpMethod.POST);
            if (oauth2Response.getErrorCode() != null) {
                throw new OperationFailureException(oauth2Response.getErrorCode());
            }

            Map<String, String> tokenResponse = oauth2Response.getResponse().entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().toString()));
            if (tokenResponse.isEmpty()) {
                throw new OperationFailureException(operr("there was an error, reason:  token response is null"));
            }

            if (tokenResponse.get(AuthGolabalProperty.OAUTH2_GET_TOKEN_USERINFO) == null) {
                throw new OperationFailureException(operr("there was an error, reason:  %s is null", AuthGolabalProperty.OAUTH2_GET_TOKEN_USERINFO));
            }


            loginZStack(tokenResponse, auth2ClientVO, cache, state, request, response);

        } catch (IOException e) {
            throw new OperationFailureException(operr("response has error : %s", e));
        }
    }

    private void loginZStack(Map<String, String> tokenResponse, OAuth2ClientVO client, AuthCache cache, String state, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("loginZStack, tokenResponse=%s, client.uuid=%s", tokenResponse, client.getUuid()));
        }

        final ErrorableValue<SessionInventory> session;
        if (StringUtils.isEmpty(client.getUserinfoUrl())) {
            session = loginByToken(tokenResponse, client.getUuid());
            if (!session.isSuccess()) {
                if (logger.isTraceEnabled()) {
                    logger.warn("failed to login oauth2-user by token: " + session.error);
                }
                throw new OperationFailureException(session.error);
            }
        } else {
            session = getUserInfo(tokenResponse, client, response);
            if (!session.isSuccess()) {
                if (logger.isTraceEnabled()) {
                    logger.warn("failed to get user-info: " + session.error);
                }
                throw new OperationFailureException(session.error);
            }
        }

        saveOAuth2Token(tokenResponse, session.result, client.getUuid());
        if (!StringUtils.isEmpty(state)) {
            authStateCache.invalidate(state);
        }

        sso.redirect(request, response, session.result, client.getUuid(), cache);
    }

    public ErrorableValue<SessionInventory> getUserInfo(Map<String, String> tokenResponse,
                                        OAuth2ClientVO client,
                                        HttpServletResponse response) {
        if (logger.isTraceEnabled()) {
            logger.trace("token response: " + JSONObjectUtil.toJsonString(tokenResponse));
        }

        if (!tokenResponse.containsKey(AuthGolabalProperty.OAUTH2_GET_TOKEN_USERINFO)) {
            try {
                sendResponse(OAuth2Constants.AUTH_RESPONSE_SUCCESS, "access_token is not null", response);
            } catch (IOException e) {
                return ErrorableValue.ofErrorCode(operr("failed to send response to oauth server"));
            }
        }

        String accessToken = tokenResponse.get(AuthGolabalProperty.OAUTH2_GET_TOKEN_USERINFO);

        MultiValueMap<String, String> httpMap = new LinkedMultiValueMap<>();
        HttpHeaders headers = new HttpHeaders();
        headers.put(OAuth2Constants.TOKEN_Authorization, Arrays.asList(String.format("Bearer %s", accessToken)));

        OAuth2Response oauth2Response = getOAuth2Response(httpMap, client.getUserinfoUrl(), client.getClientId(),
                HttpMethod.POST, headers);

        if (oauth2Response.getErrorCode() != null) {
            return ErrorableValue.ofErrorCode(oauth2Response.getErrorCode());
        }

        Map<String, Object> oauth2Res = oauth2Response.getResponse();

        final OAuth2UserToken oAuth2UserToken = generateTokenFromUserInfo(oauth2Res, client.getUuid());
        return loginByToken(oAuth2UserToken);
    }

    private OAuth2UserToken generateTokenFromUserInfo(Map<String, Object> tokens, String clientUuid) {
        OAuth2UserToken token = new OAuth2UserToken();
        token.clientUuid = clientUuid;

        String usernameProperty = Q.New(OAuth2ClientVO.class)
                .eq(OAuth2ClientVO_.uuid, clientUuid)
                .select(OAuth2ClientVO_.usernameProperty)
                .findValue();

        token.usernameProperty = usernameProperty;
        final Object username = tokens.get(usernameProperty);
        token.username = username == null ? null : username.toString();

        final Object sid = tokens.get(KEY_SID);
        token.sid = sid == null ? null : sid.toString();

        final Object sub = tokens.get(KEY_SUB);
        token.sub = sub == null ? null : sub.toString();

        return token;
    }

    private void tokenIntrospect(String accessToken, String idToken, String refreshToken, OAuth2ClientVO client,
                                 AuthCache cache, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (StringUtils.isEmpty(refreshToken)) {
            sendResponse(OAuth2Constants.AUTH_RESPONSE_SUCCESS, "refresh_token is not null", response);
        }

        String token = StringUtils.isEmpty(accessToken) ? accessToken : idToken;
        String authorization = Base64.encodeBase64String(String.format("%s:%s", client.getClientId(), client.getClientSecret()).getBytes());

        MultiValueMap<String, String> httpMap = generateTokenIntrospect(token);
        HttpHeaders headers = new HttpHeaders();
        headers.put(OAuth2Constants.TOKEN_Authorization, Arrays.asList(String.format("Basic %s", authorization)));

        OAuth2Response oauth2Response = getOAuth2Response(httpMap, client.getTokenUrl() + OAuth2Constants.TOKEN_INTROSPECT_PATH,
                client.getClientId(), HttpMethod.POST, headers);

        if (oauth2Response.getErrorCode() != null) {
            throw new OperationFailureException(oauth2Response.getErrorCode());
        }
        Map<String, Object> oauth2Res = oauth2Response.getResponse();

        if (!(Boolean)oauth2Res.get(OAuth2Constants.TOKEN_INTROSPECT_ACTIVE)) {
            sendResponse(OAuth2Constants.AUTH_RESPONSE_SUCCESS, "accecc_token or id_token active is false", response);
            return;
        }
        Map<String, String> tokenResponse = new HashMap<>();
        tokenResponse.put(OAuth2Constants.ACCESS_TOKEN_VALUE, accessToken);
        tokenResponse.put(OAuth2Constants.ID_TOKEN_VALUE, idToken);
        tokenResponse.put(OAuth2Constants.REFRESH_TOKEN_VALUE, refreshToken);

        loginZStack(tokenResponse, client, cache, null, request, response);
    }

    public ErrorableValue<SessionInventory> loginByToken(Map<String, String> tokenResponse, String clientUuid) {
        if (logger.isTraceEnabled()) {
            logger.trace("token response: " + JSONObjectUtil.toJsonString(tokenResponse));
        }

        DecodedJWT jwt = JWT.decode(tokenResponse.get(AuthGolabalProperty.OAUTH2_GET_TOKEN_USERINFO));
        Map<String, Claim> map = jwt.getClaims();

        final OAuth2UserToken oAuth2UserToken = generateToken(map, clientUuid);
        return loginByToken(oAuth2UserToken);
    }

    /**
     * This method does NOT allow the use of Completion:
     * The outer caller is within @RequestMapping, and the web.xml (version 2.5) does not support this feature:
     *
     * {@code <async-supported>true</async-supported>}
     */
    protected ErrorableValue<SessionInventory> loginByToken(OAuth2UserToken oAuth2UserToken) {
        final ErrorableValue<SessionInventory> session = loginBackend.createSessionWithToken(oAuth2UserToken);
        if (session.isSuccess()) {
            return session;
        } else if (!session.error.isError(SSO_ACCOUNT_NOT_FOUND)) {
            return session;
        }

        if (!needCreateAccountWhenUserFirstLogIn(oAuth2UserToken.clientUuid)) {
            return session;
        }

        ImportThirdPartyAccountMsg innerMsg = new ImportThirdPartyAccountMsg();
        innerMsg.setSpec(loginBackend.generateImportAccountSpec(oAuth2UserToken));
        innerMsg.setTimeout(TimeUnit.SECONDS.toMillis(3));

        bus.makeTargetServiceIdByResourceUuid(innerMsg, AccountImportsConstant.SERVICE_ID, innerMsg.getSourceUuid());
        final MessageReply reply = bus.call(innerMsg);
        if (reply.isSuccess()) {
            ImportThirdPartyAccountReply castReply = reply.castReply();
            final ImportAccountResult result = castReply.getResults().get(0);
            if (result.getError() == null) {
                return ErrorableValue.of(loginBackend.createSession(result.getRef().getAccountUuid()));
            } else {
                return ErrorableValue.ofErrorCode(result.getError());
            }
        } else {
            return ErrorableValue.ofErrorCode(reply.getError());
        }
    }

    private boolean needCreateAccountWhenUserFirstLogIn(String accountSourceUuid) {
        final SyncCreatedAccountStrategy strategy = Q.New(OAuth2ClientVO.class)
                .eq(OAuth2ClientVO_.uuid, accountSourceUuid)
                .select(OAuth2ClientVO_.createAccountStrategy)
                .findValue();
        return strategy == SyncCreatedAccountStrategy.CreateAccount;
    }

    public OAuth2UserToken generateToken(Map<String, Claim> tokens, String clientUuid) {
        OAuth2UserToken token = new OAuth2UserToken();
        token.clientUuid = clientUuid;

        String usernameProperty = Q.New(OAuth2ClientVO.class)
                .eq(OAuth2ClientVO_.uuid, clientUuid)
                .select(OAuth2ClientVO_.usernameProperty)
                .findValue();

        token.usernameProperty = usernameProperty;
        final Claim usernameClaim = tokens.get(usernameProperty);
        token.username = usernameClaim == null ? null : usernameClaim.asString();

        final Claim sidClaim = tokens.get(KEY_SID);
        token.sid = sidClaim == null ? null : sidClaim.asString();

        final Claim subClaim = tokens.get(KEY_SUB);
        token.sub = subClaim == null ? null : subClaim.asString();

        return token;
    }

    public void authorize(HttpServletRequest request, HttpServletResponse response, String thirdPartyRedirectUrl) {
        try {
            String scope = request.getParameter(OAuth2Constants.AUTH_SCOPE_NAME);
            String state = Platform.getUuid();

            if (scope == null) {
                scope = OAuth2Constants.AUTH_SCOPE_VALUE;
            }

            AuthCache cache = new AuthCache();
            cache.setState(state);
            cache.setScope(scope);
            cache.setClientUuid(request.getParameter(SSOConstants.CLIENT_UUID_NAME));
            cache.setRedirectUrl(request.getParameter(SSOConstants.REDIRECT_NAME));
            cache.setProjectUuid(request.getParameter(SSOConstants.PROJECTUUID_NAME));
            cache.setZoneUuid(request.getParameter(SSOConstants.ZONEUUID_NAME));
            authStateCache.put(state, cache);

            cache.setThirdPartyRedirectUrl(thirdPartyRedirectUrl);

            OAuth2ClientVO oauth2Client = Q.New(OAuth2ClientVO.class)
                    .eq(OAuth2ClientVO_.uuid, cache.getClientUuid())
                    .find();

            String url = UriComponentsBuilder.fromHttpUrl(oauth2Client.getAuthorizationUrl())
                    .queryParam(OAuth2Constants.RESPONSE_TYPE_NAME, OAuth2Constants.OAUTH2_CODE)
                    .queryParam(OAuth2Constants.CLIENT_ID_NAME, oauth2Client.getClientId())
                    .queryParam(OAuth2Constants.AUTH_REDIRECT_URL_NAME, thirdPartyRedirectUrl)
                    .queryParam(OAuth2Constants.AUTH_SCOPE_NAME, scope)
                    .queryParam(OAuth2Constants.AUTH2_STATE, state)
                    .toUriString();

            if (logger.isTraceEnabled()) {
                logger.trace("response code url: " + url);
            }

            response.sendRedirect(url);
        } catch (IOException e) {
            throw new OperationFailureException(operr("get code response has error : %s", e));
        }
    }

    public MultiValueMap<String, String> generateTokenIntrospect(String token) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(OAuth2Constants.OAUTH2_TOKEN, token);
        return map;
    }

    public MultiValueMap<String, String> generateRequestPassword(String username, String password, String clientId) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(OAuth2Constants.GRANT_TYPE_NAME, OAuth2Constants.GRANT_TYPE_PASSWORD_VALUE);
        map.add(OAuth2Constants.OAUTH2_USER_NAME, username);
        map.add(OAuth2Constants.OAUTH2_PASSWORD, password);
        map.add(OAuth2Constants.CLIENT_ID_NAME, clientId);
        return map;
    }

    public MultiValueMap<String, String> generateRequestCode(String code, String thirdPartyRedirectUrl, String clientId, String clientSecret) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(OAuth2Constants.GRANT_TYPE_NAME, OAuth2Constants.GRANT_TYPE_OCDE_VALUE);
        map.add(OAuth2Constants.AUTH_REDIRECT_URL_NAME, thirdPartyRedirectUrl);
        map.add(OAuth2Constants.CLIENT_ID_NAME, clientId);
        map.add(OAuth2Constants.CLIENT_SECRET_NAME, clientSecret);
        map.add(OAuth2Constants.OAUTH2_CODE, code);
        map.add(OAuth2Constants.AUTH_SCOPE_NAME, OAuth2Constants.AUTH_SCOPE_VALUE);
        return map;
    }

    public MultiValueMap<String, String> generateRefreshToken(String clientId, String clientSecret, String refreshToken) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(OAuth2Constants.GRANT_TYPE_NAME, OAuth2Constants.REFRESH_TOKEN_VALUE);
        map.add(OAuth2Constants.CLIENT_ID_NAME, clientId);
        map.add(OAuth2Constants.CLIENT_SECRET_NAME, clientSecret);
        map.add(OAuth2Constants.REFRESH_TOKEN_VALUE, refreshToken);
        return map;
    }

    public MultiValueMap<String, String> generateLogout(String clientId, String clientSecret, String refreshToken) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(OAuth2Constants.CLIENT_ID_NAME, clientId);
        map.add(OAuth2Constants.CLIENT_SECRET_NAME, clientSecret);
        map.add(OAuth2Constants.REFRESH_TOKEN_VALUE, refreshToken);
        return map;
    }

    OAuth2Response getOAuth2Response(MultiValueMap<String, String> map, String requestUrl, String clientUuid, HttpMethod method) {
        return getOAuth2Response(map, requestUrl, clientUuid, method, new HttpHeaders());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public OAuth2Response getOAuth2Response(MultiValueMap<String, String> map, String requestUrl, String clientUuid, HttpMethod method, HttpHeaders headers) {
        OAuth2Response response = new OAuth2Response();

        String url = UriComponentsBuilder.fromHttpUrl(requestUrl).toUriString();

        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(map, headers);
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("oauth2 json %s[%s], %s",  HttpMethod.POST, url, req));
        }
        try {
            ResponseEntity<Map> rsp = new Retry<ResponseEntity<Map>>() {
                {
                    interval = 2;
                }

                @Override
                @RetryCondition(onExceptions = {IOException.class, RestClientException.class, HttpClientErrorException.class})
                protected ResponseEntity<Map> call() {
                    return http.exchange(url, method, req, Map.class);
                }
            }.run();

            if (rsp.getStatusCode().is4xxClientError() && rsp.getBody() != null && rsp.getBody().containsValue("Token is not active")) {
                response.setErrorCode(err(SSOErrors.INVALID_SSO_TOKEN,
                        "error requesting token in clientUuid[%s], reason: %s", clientUuid, rsp.getBody()));
                return response;
            }

            if (!rsp.getStatusCode().is2xxSuccessful()) {
                response.setErrorCode(operr("HTTP ERROR, status code: %s, body: %s", rsp.getStatusCode(), rsp.getBody()));
                return response;
            }
            response.setResponse(rsp.getBody());
            return response;

        } catch (HttpStatusCodeException e) {
            response.setErrorCode(operr("failed to post %s with unexpected status code %s", url, e.getStatusCode())
                    .withOpaque(OPAQUE_KEY_RESPONSE_OUTPUT, e.getResponseBodyAsString()));
            return response;
        } catch (ResourceAccessException e) {
            response.setErrorCode(operr("failed to post %s with IO error", url)
                    .withException(e.getMessage()));
            return response;
        }
    }

    public ErrorCode refreshToken(SessionInventory session) {
        OAuth2TokenVO tokenVO = Q.New(OAuth2TokenVO.class)
                .eq(OAuth2TokenVO_.userUuid, session.getAccountUuid())
                .find();

        Query query = dbf.getEntityManager().createNativeQuery("select current_timestamp()");
        Timestamp ts = (Timestamp) query.getSingleResult();

        DecodedJWT jwt = JWT.decode(tokenVO.getAccessToken());
        Map<String, Claim> map = jwt.getClaims();
        if (!map.containsKey(OAuth2Constants.TOKEN_EXPIRE_TIME_NAME)) {
            return null;
        }

        Date expireTime = map.get(OAuth2Constants.TOKEN_EXPIRE_TIME_NAME).asDate();

        if (new Timestamp(expireTime.getTime()).after(ts)){
            return null;
        }
        
        OAuth2ClientVO clientVO = Q.New(OAuth2ClientVO.class)
                .eq(OAuth2ClientVO_.uuid, tokenVO.getClientUuid())
                .find();

        MultiValueMap<String, String> httpMap = generateRefreshToken(clientVO.getClientId(),
                clientVO.getClientSecret(), tokenVO.getRefreshToken());
        OAuth2Response response = getOAuth2Response(httpMap, clientVO.getTokenUrl(), clientVO.getUuid(), HttpMethod.POST);
        if (response.getErrorCode() != null) {
            cleanOAuth2Token(session);
            return response.getErrorCode();
        }

        Map<String, String> tokenResponse = response.getResponse().entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().toString()));

        updateOAuth2Token(tokenResponse, session);
        return null;
    }

    private void saveOAuth2Token(Map<String, String> tokenResponse, SessionInventory session, String clientUuid) {
        if (Q.New(OAuth2TokenVO.class)
                .eq(OAuth2TokenVO_.userUuid, session.getAccountUuid())
                .isExists()) {
            updateOAuth2Token(tokenResponse, session);
            return;
        }

        OAuth2TokenVO vo = new OAuth2TokenVO();
        vo.setUuid(Platform.getUuid());
        vo.setClientUuid(clientUuid);
        vo.setUserUuid(session.getAccountUuid());
        vo.setIdToken(tokenResponse.get(OAuth2Constants.ID_TOKEN_VALUE));
        vo.setAccessToken(tokenResponse.get(OAuth2Constants.ACCESS_TOKEN_VALUE));
        vo.setRefreshToken(tokenResponse.get(OAuth2Constants.REFRESH_TOKEN_VALUE));
        dbf.persist(vo);

    }

    private void updateOAuth2Token(Map<String, String> tokenResponse, SessionInventory session) {
        UpdateQuery.New(OAuth2TokenVO.class)
                .eq(OAuth2TokenVO_.userUuid, session.getAccountUuid())
                .set(OAuth2TokenVO_.accessToken, tokenResponse.get(OAuth2Constants.ACCESS_TOKEN_VALUE))
                .set(OAuth2TokenVO_.idToken, tokenResponse.get(OAuth2Constants.ID_TOKEN_VALUE))
                .set(OAuth2TokenVO_.refreshToken, tokenResponse.get(OAuth2Constants.REFRESH_TOKEN_VALUE))
                .update();

    }

    private void cleanOAuth2Token(SessionInventory session) {
        if (Q.New(SessionVO.class)
                .eq(SessionVO_.accountUuid, session.getAccountUuid())
                .count() > 1) {
            Session.logout(session.getUuid());
            return;
        }

        UpdateQuery.New(OAuth2TokenVO.class)
                .eq(OAuth2TokenVO_.userUuid, session.getAccountUuid())
                .delete();

    }


    private void sendResponse(int statusCode, String body, HttpServletResponse rsp) throws IOException {
        rsp.setStatus(statusCode);
        rsp.getWriter().write(body == null ? "" : body);
    }

    @Override
    public ErrorCode checkAuth(SessionInventory session) {
        if (!Q.New(OAuth2TokenVO.class)
                .eq(OAuth2TokenVO_.userUuid, session.getAccountUuid())
                .isExists()) {
            return null;
        }

        AccountVO account = Q.New(AccountVO.class)
                .eq(AccountVO_.uuid, session.getAccountUuid())
                .find();
        if (account != null && account.getType() == AccountType.ThirdParty) {
            return refreshToken(session);
        }

        return null;
    }

    @Override
    public void beforeLogout(SessionInventory session) {
        if (session == null) {
            return;
        }
        
        OAuth2TokenVO tokenVO = Q.New(OAuth2TokenVO.class)
                .eq(OAuth2TokenVO_.userUuid, session.getAccountUuid())
                .find();
        if (tokenVO == null) {
            return;
        }

        OAuth2ClientVO clientVO = Q.New(OAuth2ClientVO.class)
                .eq(OAuth2ClientVO_.uuid, tokenVO.getClientUuid())
                .find();

        if (StringUtils.isEmpty(clientVO.getLogoutUrl())) {
            return;
        }

        MultiValueMap<String, String> httpMap = generateLogout(clientVO.getClientId(),
                clientVO.getClientSecret(), tokenVO.getRefreshToken());
        getOAuth2Response(httpMap, clientVO.getLogoutUrl(), clientVO.getUuid(), HttpMethod.POST);
    }
}
