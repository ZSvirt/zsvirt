package org.zstack.pluginpremium.externalapiadapter.server;

import com.taobao.eagleeye.EagleEye;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterGlobalConfig;
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterGlobalProperty;
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils;
import org.zstack.pluginpremium.externalapiadapter.api.APIAdapter;
import org.zstack.pluginpremium.externalapiadapter.api.APIResult;
import org.zstack.sdk.ZSClient;
import org.zstack.sdk.ZSConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by lining on 2018/4/19.
 */
public class ExternalAPIAdapterServer implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ExternalAPIAdapterServer.class);

    @Autowired
    private APIAdapter adapter;

    void handle(HttpServletRequest request, HttpServletResponse response)
            throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (ExternalAPIAdapterGlobalConfig.ENABLE_EAGLEEYE.value(Boolean.class)) {
            boolean success = false;
            String traceId = null;
            String rpcId = null;
            try {
                traceId = EagleEye.getTraceId();
                if (traceId == null) {
                    traceId = request.getHeader("EagleEye-TraceId");
                }
                if (traceId == null) {
                    traceId = EagleEye.generateTraceId(null);
                }

                rpcId = EagleEye.getRpcId();
                if (rpcId == null) {
                    rpcId = request.getHeader("EagleEye-RpcId");
                }
                if (rpcId == null) {
                    rpcId = EagleEye.MAL_ROOT_RPC_ID;
                }

                String userData = EagleEye.exportUserData();
                if (userData == null) {
                    userData = request.getHeader("EagleEye-UserData");
                }
                Map<String, String> eagleEyeContext = new HashMap<>();
                eagleEyeContext.put(EagleEye.TRACE_ID_KEY, traceId);
                eagleEyeContext.put(EagleEye.RPC_ID_KEY, rpcId);
                eagleEyeContext.put(EagleEye.USER_DATA_KEY, userData);
                EagleEye.setRpcContext(eagleEyeContext);
                EagleEye.rpcServerRecv(request.getRequestURI(), request.getMethod());
            } catch (Exception e) {
                logger.debug("Initiate eagleEye failed.", e);
            } catch (Error error) {
                logger.error("Initiate eagleEye failed.", error);
            }

            try {
                success = handleInner(request, response, traceId, rpcId);
            } finally {
                EagleEye.remoteIp("0.0.0.0");
                EagleEye.responseSize(250);
                EagleEye.rpcServerSend(success ? EagleEye.RPC_RESULT_SUCCESS : EagleEye.RPC_RESULT_FAILED, 251);
            }
        } else {
            handleInner(request, response, null, null);
        }

    }

    private boolean handleInner(HttpServletRequest request, HttpServletResponse response, String traceId, String rpcId) throws IOException {
        request.setCharacterEncoding("utf-8");
        response.setCharacterEncoding("utf-8");
        response.setContentType("application/json");

        Map ecsAPIParamMap = this.getEcsAPIParamMap(request);

        String requestId = ExternalAPIAdapterUtils.randomUUID();
        if (traceId == null || rpcId == null) {
            logger.debug(String.format("[RequestId:%s] Received ECS API request: %s",
                    requestId, JSONObjectUtil.toJsonString(ecsAPIParamMap)));
        } else {
            logger.debug(String.format("[EagleEyeTraceId:%s][RpcId:%s][RequestId:%s] Received ECS API request: %s",
                    traceId, rpcId, requestId, JSONObjectUtil.toJsonString(ecsAPIParamMap)));
        }

        APIResult result = adapter.callZStackAPI(request.getMethod(), ecsAPIParamMap, requestId, traceId, rpcId);
        if (traceId == null || rpcId == null) {
            logger.debug(String.format("Response to %s(path: %s), Result: %s",
                    request.getRemoteHost(), request.getRequestURI(), JSONObjectUtil.toJsonString(result)));
        } else {
            logger.debug(String.format("[EagleEyeTraceId:%s][RpcId:%s] Response to %s(path: %s), Result: %s",
                    traceId, rpcId, request.getRemoteHost(), request.getRequestURI(), JSONObjectUtil.toJsonString(result)));
        }

        if (result.getError() == null) {
            sendResponse(200, result.getValue().toString(), response);
            return true;
        } else {
            sendResponse(400, result.getError().toString(), response);
            return false;
        }
    }

    private Map getEcsAPIParamMap(HttpServletRequest req) {
        Map ecsAPIParamMap = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

        Enumeration<String> requestParameterNames = req.getParameterNames();
        while (requestParameterNames.hasMoreElements()) {
            String requestParameterName = requestParameterNames.nextElement();
            String requestParameterValue = req.getParameter(requestParameterName);
            ecsAPIParamMap.put(requestParameterName, requestParameterValue);
        }

        return ecsAPIParamMap;
    }

    private void sendResponse(int statusCode, String body, HttpServletResponse rsp) throws IOException {
        rsp.setStatus(statusCode);
        rsp.getWriter().write(body == null ? "" : body);
    }

    @Override
    @AsyncThread
    public void managementNodeReady() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        ZSClient.configure(
                new ZSConfig.Builder()
                        .setHostname(ExternalAPIAdapterGlobalProperty.ZSCLIENT_HOSTNAME)
                        .setPort(Platform.getManagementNodeServicePort())
                        .setContextPath(ExternalAPIAdapterGlobalProperty.ZSCLIENT_CONTEXTPATH)
                        .setDefaultPollingInterval(100, TimeUnit.MILLISECONDS)
                        .setDefaultPollingTimeout(10, TimeUnit.MINUTES)
                        .setReadTimeout(1, TimeUnit.MINUTES)
                        .setWriteTimeout(5, TimeUnit.MINUTES)
                        .build()
        );
    }
}
