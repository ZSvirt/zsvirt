package org.zstack.header.identity;

import org.zstack.header.message.NeedJsonSchema;

import java.util.Date;

/**
 * Created by xing5 on 2016/3/21.
 */
public class IdentityCanonicalEvents {
    public static final String ACCOUNT_DELETED_PATH = "/account/delete";
    public static final String ACCOUNT_LOGIN_PATH = "/account/login";
    public static final String SESSION_FORCE_LOGOUT_PATH = "/session/logout";

    public static final String SESSION_FORCE_LOGOUT = "SessionForceLogout";

    @NeedJsonSchema
    public static class SessionForceLogoutData {
        private String sessionUuid;
        private String accountUuid;
        private String name = SESSION_FORCE_LOGOUT;
        private Date date = new Date();

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getSessionUuid() {
            return sessionUuid;
        }

        public void setSessionUuid(String sessionUuid) {
            this.sessionUuid = sessionUuid;
        }

        public String getAccountUuid() {
            return accountUuid;
        }

        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @NeedJsonSchema
    public static class AccountDeletedData {
        private String accountUuid;
        private AccountInventory inventory;
        private Date date = new Date();

        public String getAccountUuid() {
            return accountUuid;
        }

        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }

        public AccountInventory getInventory() {
            return inventory;
        }

        public void setInventory(AccountInventory inventory) {
            this.inventory = inventory;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }

    @NeedJsonSchema
    public static class AccountLoginData {
        private String accountUuid;

        public String getAccountUuid() {
            return accountUuid;
        }

        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }
    }
}
