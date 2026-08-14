package org.zstack.sso.service.redirect


import org.zstack.core.db.Q
import org.zstack.sso.header.SSORedirectTemplateVO
import org.zstack.sso.header.SSORedirectTemplateVO_
import org.zstack.utils.StringTemplateUtils

/**
 * @Author: DaoDao
 * @Date: 2022/9/6
 */
class UIRedirectUrl extends AbstractRedirectUrl implements UrlTemplate {
    public static final String REDIRECT_USERNAME = "username"
    public static final String REDIRECT_SESSIONID = "sessionId"
    public static final String REDIRECT_USERUUID = "userUuid"
    public static final String REDIRECT_ACCOUNTUUID = "accountUuid"
    public static final String REDIRECT_LOGINTYPE = "loginType"
    public static final String REDIRECT_REDIRECT = "redirect"
    public static final String REDIRECT_USERTYPE = "userType"
    public static final String REDIRECT_PROJECTUUID = "projectUuid"
    public static final String REDIRECT_ZONEUUID = "zoneUuid"

    private Map<String, String> makeTemplateBindings(Map<String, String> params) {
        Map<String, String> bindings = new HashMap<>()
        bindings.put(REDIRECT_USERNAME, params.get(REDIRECT_USERNAME))
        bindings.put(REDIRECT_SESSIONID, params.get(REDIRECT_SESSIONID))
        bindings.put(REDIRECT_USERUUID,  params.get(REDIRECT_USERUUID))
        bindings.put(REDIRECT_ACCOUNTUUID,  params.get(REDIRECT_ACCOUNTUUID))
        bindings.put(REDIRECT_LOGINTYPE,  params.get(REDIRECT_LOGINTYPE))
        bindings.put(REDIRECT_USERTYPE,  params.get(REDIRECT_USERTYPE))
        bindings.put(REDIRECT_REDIRECT,  params.get(REDIRECT_REDIRECT))
        bindings.put(REDIRECT_PROJECTUUID, params.get(REDIRECT_PROJECTUUID))
        bindings.put(REDIRECT_ZONEUUID, params.get(REDIRECT_ZONEUUID))

        return bindings
    }

    String addRedirectIfNeed(Map<String, String> params, String uiTempalte) {
        if (params.get(REDIRECT_REDIRECT) != null) {
            uiTempalte += '''&redirect=${redirect}'''
        }

        if (params.get(REDIRECT_PROJECTUUID) != null) {
            uiTempalte += '''&projectUuid=${projectUuid}'''
        }

        if (params.get(REDIRECT_ZONEUUID) != null) {
            uiTempalte += '''&zoneUuid=${zoneUuid}'''
        }

        return uiTempalte
    }

    @Override
    String getRedirectUrl(Map<String, String> params, String templateUuid) {
        String tempalte =  Q.New(SSORedirectTemplateVO.class)
                .eq(SSORedirectTemplateVO_.uuid, templateUuid)
                .select(SSORedirectTemplateVO_.redirectTemplate)
                .findValue()

        tempalte = addRedirectIfNeed(params, tempalte)

        def bindings = makeTemplateBindings(params)
        return StringTemplateUtils.createStringFromTemplate(tempalte, bindings)
    }
}
