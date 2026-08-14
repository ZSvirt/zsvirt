package org.zstack.pluginpremium.externalapiadapter

import org.apache.commons.lang.StringUtils
import org.zstack.core.CoreGlobalProperty
import org.zstack.sdk.LogInByAccountAction
import org.zstack.sdk.ValidateSessionAction
import org.zstack.sdk.ValidateSessionResult

import java.util.concurrent.TimeUnit

/**
 * Created by lining on 2018/4/24.
 */
class ZStackSessionFactory {
    private static Map<String, ZStackAccountSession> zstackAccountMap = [:]

    static String getZStackSessionId(String accountName) {
        if (zstackAccountMap.isEmpty()) {
            loadZStackAccount()
        }

        ZStackAccountSession accountSession = zstackAccountMap.get(accountName)

        // not login
        if (accountSession.sessionId == null || accountSession.sessionExpiredDate == null) {
            return loginZStack(accountName, accountSession.password)
        }

        long currentTime = new Date().getTime()

        // session expired
        if (accountSession.sessionExpiredDate <= currentTime) {
            return loginZStack(accountName, accountSession.password)
        }

        // session is about to expire
        if (accountSession.sessionExpiredDate - currentTime < TimeUnit.MINUTES.toMillis(30)) {
            return loginZStack(accountName, accountSession.password)
        }

        if (CoreGlobalProperty.UNIT_TEST_ON) {
            ValidateSessionAction validateSessionAction = new ValidateSessionAction(
                    sessionUuid : accountSession.sessionId
            )
            ValidateSessionAction.Result validateSessionResult = validateSessionAction.call()
            if (validateSessionResult.error != null || !validateSessionResult.value.valid) {
                return loginZStack(accountName, accountSession.password)
            }
        }

        return accountSession.sessionId
    }

    private static String loginZStack(String accountName, String password) {
        synchronized (accountName) {
            LogInByAccountAction loginAction = new LogInByAccountAction()
            loginAction.accountName = accountName
            loginAction.password = password

            LogInByAccountAction.Result result = loginAction.call()
            result.throwExceptionIfError()

            String sessionId = result.value.inventory.uuid
            Long dateLong = result.value.inventory.expiredDate.getTime()

            ZStackAccountSession accountSession = zstackAccountMap.get(accountName)
            accountSession.sessionId = sessionId
            accountSession.sessionExpiredDate = dateLong

            return sessionId
        }
    }

    private synchronized static void loadZStackAccount() {
        if (StringUtils.isEmpty(ExternalAPIAdapterGlobalProperty.ZSTACK_ACCOUNT_PASSWORD_ACCESSKEY_ACCESSSECRET)) {
            return
        }

        String[] configs = ExternalAPIAdapterGlobalProperty.ZSTACK_ACCOUNT_PASSWORD_ACCESSKEY_ACCESSSECRET.split(",")

        for (String config : configs) {
            String[] accountAccessConfigs = ExternalAPIAdapterUtils.splitAccountAccessConfig(config)
            String accountName = accountAccessConfigs[0]
            String password = accountAccessConfigs[1]

            ZStackAccountSession accountSession = new ZStackAccountSession()
            accountSession.accountName = accountName
            accountSession.password = password

            zstackAccountMap.put(accountName, accountSession)
        }

    }

    private static class ZStackAccountSession {
        String accountName
        String password
        String sessionId
        Long sessionExpiredDate
    }

}
