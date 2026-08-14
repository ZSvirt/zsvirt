package org.zstack.header.expon;

import okhttp3.MediaType;

public interface Constants {
    String HEADER_AUTHORIZATION = "Authorization";
    String BEARER = "Bearer";
    String SESSION_ID = "sessionId";
    MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    String HTTP_ERROR = "sdk.1000";
    String POLLING_TIMEOUT_ERROR = "sdk.1001";
    String INTERNAL_ERROR = "sdk.1002";
}
