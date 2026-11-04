package org.zstack.sso.service.redirect;

import org.zstack.utils.gson.JSONObjectUtil;

import java.util.Map;

/**
 * @Author: DaoDao
 * @Date: 2022/8/23
 */
public class RedirectParamsBuilder {
    private RedirectParams params = new RedirectParams();

    public RedirectParamsBuilder setUserName(String userName) {
        params.setUsername(userName);
        return this;
    }

    public RedirectParamsBuilder setSessionId(String sessionId) {
        params.setSessionId(sessionId);
        return this;
    }

    public RedirectParamsBuilder setAccountUuid(String accountUuid) {
        params.setAccountUuid(accountUuid);
        return this;
    }

    public RedirectParamsBuilder setRedirect(String redirect) {
        params.setRedirect(redirect);
        return this;
    }

    public RedirectParamsBuilder setZoneUUid(String zoneUuid) {
        params.setZoneUuid(zoneUuid);
        return this;
    }

    public RedirectParamsBuilder setUserUuid(String userUuid) {
        params.setUserUuid(userUuid);
        return this;
    }

    public RedirectParamsBuilder setProjectUuid(String projectUuid) {
        params.setProjectUuid(projectUuid);
        return this;
    }

    public Map<String, String> build() {
        return JSONObjectUtil.rehashObject(params, Map.class);
    }

    public static class RedirectParams extends SessionParams {
        private String redirect;
        private String zoneUuid;
        private String projectUuid;

        // only for compatibility (ZSphere 4.0.0 ~ ZSphere 4.10.3)
        private String userType = "iam1";
        private String loginType = "iam1";

        public String getRedirect() {
            return redirect;
        }

        public void setRedirect(String redirect) {
            this.redirect = redirect;
        }

        public String getZoneUuid() {
            return zoneUuid;
        }

        public void setZoneUuid(String zoneUuid) {
            this.zoneUuid = zoneUuid;
        }

        public String getProjectUuid() {
            return projectUuid;
        }

        public void setProjectUuid(String projectUuid) {
            this.projectUuid = projectUuid;
        }

        public String getUserType() {
            return userType;
        }

        public void setUserType(String userType) {
            this.userType = userType;
        }

        public String getLoginType() {
            return loginType;
        }

        public void setLoginType(String loginType) {
            this.loginType = loginType;
        }
    }
}
