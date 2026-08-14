package org.zstack.header.identity;

import org.zstack.header.configuration.PythonClass;
import org.zstack.header.rest.RestAuthenticationType;

@PythonClass
public interface AccountConstant {
    String SERVICE_ID = "identity";
    @PythonClass
    String INITIAL_SYSTEM_ADMIN_UUID = "36c27e8ff05c4780bf6d2fa65700f22e";
    @PythonClass
    String INITIAL_SYSTEM_ADMIN_NAME = "admin";
    // 'password' SHA512 hex coding
    @PythonClass
    String INITIAL_SYSTEM_ADMIN_PASSWORD = "b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86";

    String SYSTEM_ADMIN_ROLE = ".*";

    @Deprecated
    int RESOURCE_PERMISSION_READ = 1;
    @Deprecated
    int RESOURCE_PERMISSION_WRITE = 2;

    String ACTION_CATEGORY = "identity";
    String READ_PERMISSION_POLICY = "default-read-permission";

    String QUOTA_GLOBAL_CONFIG_CATETORY = "quota";

    String PRINCIPAL_USER = "user";
    String PRINCIPAL_ACCOUNT = "account";

    String LOGIN_TYPE = "account";
    String LOGIN_TYPE_AUTHENTICATIONS_KEY = "authentications";
    String POLICY_BASE_PACKAGE = "org.zstack.";

    String NO_EXIST_ACCOUNT ="no-exist-account:::%s";

    // login property accountType
    String ACCOUNT_TYPE = "accountType";

    String OTHER_ROLE_UUID = "80315b1f85314917826b182bf6def552";
    String LEGACY_ROLE_UUID = "85cfac2138494b2db6501881e1e68045";

    /**
     * Allow querying all resources.
     * Querying audit data is not allowed
     */
    String ALL_RESOURCES_READABLE_ROLE_UUID = "8550153cd5474c79850566787fe0f055";

    // for: Separation of Duties
    String SOD_SYSTEM_ADMIN_ROLE_UUID = "8550125df53c54edb33d1b8ae83ded55";
    String SOD_SECURITY_ADMIN_ROLE_UUID = "855013d87cf55944b4a6c6ae729b3f55";
    String SOD_AUDITOR_ROLE_UUID = "855014f1908759aca90f58ca56290955";

    static boolean isAdmin(SessionInventory session) {
        return isAdmin(session.getAccountUuid());
    }

    static boolean isAdmin(String accountUuid) {
        return INITIAL_SYSTEM_ADMIN_UUID.equals(accountUuid);
    }

    String ACCOUNT_REST_AUTH = "OAuth";
    RestAuthenticationType ACCOUNT_REST_AUTHENTICATION_TYPE = new RestAuthenticationType(ACCOUNT_REST_AUTH);
}
