package org.zstack.accessKey;

import org.zstack.header.rest.RestAuthenticationType;

public interface AccessKeyConstant {
    String SERVICE_ID = "accessKey";
    String ACTION_CATEGORY = "accessKey";

    String ACCESS_KEY_ALGORITHM = "HmacSHA1";

    int ACCESSKEY_ID_LEN = 20;
    int ACCESSKEY_ID_MIN_LEN = 10;
    int ACCESSKEY_SECRET_LEN = 40;
    int ACCESSKEY_SECRET_MIN_LEN = 10;
    int ACCESSKEY_QUOTA = 2;

    String REST_AUTH_TYPE = "ZStack";
    RestAuthenticationType ACCESSKEY_REST_AUTHENTICATION_TYPE = new RestAuthenticationType(REST_AUTH_TYPE);
}
