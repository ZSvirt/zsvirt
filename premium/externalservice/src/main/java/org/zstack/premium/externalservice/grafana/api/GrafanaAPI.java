package org.zstack.premium.externalservice.grafana.api;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.rest.RESTFacade;
import org.zstack.premium.externalservice.grafana.GrafanaGlobalProperty;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2019/8/21.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class GrafanaAPI {
    protected static final CLogger logger = Utils.getLogger(GrafanaAPI.class);

    @Autowired
    private RESTFacade restf;

    public static final String POST = "POST";
    public static final String GET = "GET";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";

    private static final String UTF8 = "utf-8";

    private String host = Platform.getManagementServerIp();
    private int port = GrafanaGlobalProperty.GRAFANA_SERVER_PORT;
    private String username = "admin";
    private String password = "password";

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public GrafanaAPI() {
    }

    public GrafanaAPI(String password) {
        this.password = password;
    }

    private void appendBuffer(StringBuffer buffer, String key, String value) {
        if (buffer.length() == 0) {
            buffer.append("?");
        } else {
            buffer.append("&");
        }
        buffer.append(String.format("%s=%s", key, value));
    }

    private <T> T basicCall(String path, String method, Map<String, Object> apiParams, Class<T> returnClass) {
        String rpath = String.format("http://%s:%d/api", host, port) + path;

        StringEntity entity = new StringEntity(JSONObjectUtil.toJsonString(apiParams), UTF8);
        entity.setContentEncoding(UTF8);
        entity.setContentType("application/json");


        Map<String, String> headers = new HashMap<>();
        StringBuffer buffer = new StringBuffer();
        if (apiParams != null) {
            for (Map.Entry<String, Object> entry : apiParams.entrySet()) {
                if (entry.getValue() != null) {
                    headers.put(entry.getKey(), entry.getValue().toString());
                    appendBuffer(buffer, entry.getKey(), entry.getValue().toString());
                }
            }
        }

        HttpRequestBase request;
        switch (method) {
            case POST: {
                request = new HttpPost(rpath);
                ((HttpPost) request).setEntity(entity);
                break;
            }
            case GET: {
                request = new HttpGet(buffer.length() > 0 ? rpath + buffer.toString() : rpath);
                break;
            }
            case DELETE: {
                request = new HttpDelete(rpath);
                break;
            }
            case PUT: {
                request = new HttpPut(rpath);
                ((HttpPut) request).setEntity(entity);
                break;
            }
            default:
                throw new OperationFailureException(operr("non support method: %s", method));
        }

        String auth = String.format("%s:%s", username, password);
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes());
        request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(encodedAuth));

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpResponse response = client.execute(request);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new OperationFailureException(operr("http request error! status_code: %s, error: %s", statusCode, response.getStatusLine().getReasonPhrase()));
            }
            return JSONObjectUtil.toObject(EntityUtils.toString(response.getEntity(), UTF8), returnClass);
        } catch (IOException e) {
            throw new OperationFailureException(operr(e.getMessage()));
        }
    }

    public <T> T call(String path, String method, Map<String, Object> apiParams, Class<T> returnClass) {
        try {
            return basicCall(path, method, apiParams, returnClass);
        } catch (OperationFailureException e) {
            GrafanaAPIResult result = new GrafanaAPIResult();
            result.error = e.getErrorCode();
            result.success = false;
            return JSONObjectUtil.rehashObject(result, returnClass);
        }
    }
}
