package org.zstack.expon.sdk;


import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/login",
        method = HttpMethod.POST,
        responseClass = LoginExponResponse.class,
        version = "v1",
        sync = false
)
public class LoginExponRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private String name;
    @Param
    private String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        parameterMap.remove("sessionId");
        return parameterMap;
    }
}
