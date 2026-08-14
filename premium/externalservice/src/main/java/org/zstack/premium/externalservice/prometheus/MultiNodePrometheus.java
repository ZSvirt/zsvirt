package org.zstack.premium.externalservice.prometheus;

import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.externalservice.ExternalServiceCapabilitiesBuilder;
import org.zstack.header.core.external.service.ExternalServiceCapabilities;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.HTTP;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class MultiNodePrometheus implements Prometheus {
    private static final CLogger logger = Utils.getLogger(MultiNodePrometheus.class);

    private Prometheus localNode;
    private List<String> remoteNodeIPs;
    @Autowired
    private PluginRegistry pluginRgty;
    private List<PrometheusDriverExtensionPoint> driverExts = new ArrayList<>();

    ExternalServiceCapabilities capabilities = ExternalServiceCapabilitiesBuilder
            .build()
            .reloadConfig(true);

    public MultiNodePrometheus(Prometheus localNode, Collection<String> remoteNodeIPs) {
        this.localNode = localNode;
        this.remoteNodeIPs =  remoteNodeIPs == null ? new ArrayList<>() : new ArrayList<>(remoteNodeIPs);

        this.remoteNodeIPs.add(Platform.getManagementServerIp());

        // WARN: sorting is very important for dedup,
        // otherwise different management nodes will get different dedup results
        // DO NOT remove this line
        Collections.sort(this.remoteNodeIPs);
    }

    @Override
    public PrometheusConfig getConfig() {
        return localNode.getConfig();
    }

    @Override
    public PrometheusParam getParams() {
        return localNode.getParams();
    }

    @Override
    public void rewriteConfigAndReload() {
        localNode.rewriteConfigAndReload();
    }

    @Override
    public void reload() {
        localNode.reload();
    }

    @Override
    public String getName() {
        return localNode.getName();
    }

    @Override
    public void start() {
        driverExts = pluginRgty.getExtensionList(PrometheusDriverExtensionPoint.class);
        localNode.start();
    }

    @Override
    public void stop() {
        localNode.stop();
    }

    @Override
    public void restart() {
        localNode.restart();
    }

    @Override
    public boolean isAlive() {
        return localNode.isAlive();
    }

    @Override
    public ExternalServiceCapabilities getExternalServiceCapabilities() {
        return capabilities;
    }

    @Override
    public void tolerateFailureStart() {
        localNode.tolerateFailureStart();
    }

    private void addBasicAuthIfEnabled(HTTP.Builder hb) {
        if (PrometheusGlobalConfig.ENABLE_BASIC_AUTH.value(Boolean.class)) {
            String credentials = PrometheusGlobalConfig.BASIC_AUTH_USERNAME.value() + ":" + PrometheusGlobalConfig.BASIC_AUTH_PASSWORD.value();
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            hb.header("Authorization", "Basic " + encoded);
        }
    }

    private <T> T doApiCall(boolean instant, final String path, Map<String, Object> params, Class<T> returnType) {
        List<HTTP.Builder> hbs= new ArrayList<>();

        remoteNodeIPs.forEach(ip -> {
            HTTP.Builder hb = HTTP.get();
            String p = path;
            if (p == null) {
                p = instant ? HTTP_API_PATH : HTTP_RANGE_API_PATH;
            }

            String url = String.format("http://%s:%s%s", ip, PrometheusGlobalProperty.getPrometheusPort(), p);
            hb.url(url);
            addBasicAuthIfEnabled(hb);
            if (params != null) {
                params.forEach((k, v) -> {
                    if (v instanceof String) {
                        hb.queryParameter(k, (String) v);
                    } else if (v instanceof Collection) {
                        for (Object vv : (Collection) v) {
                            hb.queryParameter(k, (String) vv);
                        }
                    } else {
                        throw new OperationFailureException(operr("unknown value type %s, key = %s", v.getClass().getSimpleName(), k));
                    }
                });
            }
            driverExts.forEach(ext -> {
                ext.beforePrometheusApicall(ip, url);
            });
            hbs.add(hb);
        });

        List<APIReturnStruct> rsps = new ArrayList<>();
        hbs.forEach(hb -> {
            try (Response rsp = hb.callWithException()) {
                if (!rsp.isSuccessful()) {
                    logger.warn(String.format("failed to call prometheus[%s, status code: %s], %s", hb.getParam().getUrl(), rsp.code(), rsp.body().string()));
                } else {
                    APIReturnStruct r = (APIReturnStruct) JSONObjectUtil.toObject(rsp.body().string(), returnType);
                    if (r.error != null) {
                        logger.warn(String.format("failed to call prometheus[%s], %s", hb.getParam().getUrl(), r.error));
                    } else {
                        rsps.add(r);
                    }
                }
            } catch (IOException e) {
                logger.warn(String.format("failed to call prometheus[%s], %s", hb.getParam().getUrl(), e.getMessage()));
            }
        });

        if (rsps.isEmpty()) {
            throw new OperationFailureException(operr("failed to HTTP call all prometheus instances"));
        }

        if (APIRangeQueryStruct.class.isAssignableFrom(returnType)) {
            APIRangeQueryStruct ret = new APIRangeQueryStruct();
            ret.data = new APIRangeQueryStruct.Data();

            // WARN: use LinkedHashMap is very important for dedup
            // otherwise different management nodes will get different dedup results
            Map<Map<String, String>, APIRangeQueryStruct.Value> values = new LinkedHashMap<>();
            rsps.forEach(r -> {
                APIRangeQueryStruct rs = (APIRangeQueryStruct) r;
                if (rs.data.resultType != null) {
                    ret.data.resultType = rs.data.resultType;
                }
                rs.data.result.forEach(v-> {
                    APIRangeQueryStruct.Value cv = values.computeIfAbsent(v.metric, x->v);
                    if (cv != v) {
                        cv.values.addAll(v.values);
                    }
                });
            });

            ret.data.result = values.values().stream().map(this::normalizeValue).collect(Collectors.toList());
            return (T) ret;
        } else if (APIQueryMetadataStruct.class.isAssignableFrom(returnType)) {
            APIQueryMetadataStruct ret = new APIQueryMetadataStruct();
            Set<Map<String, String>> s = new HashSet<>();
            rsps.forEach(rsp -> {
                APIQueryMetadataStruct r = (APIQueryMetadataStruct) rsp;
                s.addAll(r.data);
            });
            ret.data = new ArrayList<>(s);
            return (T) ret;
        } else if (APIQueryLabelValueStruct.class.isAssignableFrom(returnType)) {
            APIQueryLabelValueStruct ret = new APIQueryLabelValueStruct();
            Set<String> s = new HashSet<>();
            rsps.forEach(rsp -> {
                APIQueryLabelValueStruct r = (APIQueryLabelValueStruct) rsp;
                s.addAll(r.data);
            });
            ret.data = new ArrayList<>(s);
            return (T) ret;
        } else {
            throw new CloudRuntimeException(String.format("unknown return type[%s]", returnType));
        }
    }

    private APIRangeQueryStruct.Value normalizeValue(APIRangeQueryStruct.Value v) {
        LinkedHashMap<Long, List> values = new LinkedHashMap<>();
        v.values.forEach(vv -> {
            // dedup
            values.put((Long) vv.get(0), vv);
        });

        APIRangeQueryStruct.Value ret = new APIRangeQueryStruct.Value();
        ret.metric = v.metric;
        LinkedHashMap<Long, List> sorted = new LinkedHashMap<>(values.size());
        values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(x -> sorted.put(x.getKey(), x.getValue()));
        ret.values = new ArrayList<>(values.values());
        return ret;
    }

    @Override
    public <T> T apiCall(boolean instant, Map params, Class<T> returnType) {
        return doApiCall(instant, null, (Map<String, Object>) params, returnType);
    }

    @Override
    public <T> T apiCall(String path, Map params, Class<T> returnType) {
        return doApiCall(false, path, (Map<String, Object>) params, returnType);
    }

    @Override
    public boolean apiDelete(List<String> matches) {
        List<HTTP.Builder> hbs = new ArrayList<>();

        StringBuilder param = new StringBuilder();
        matches.forEach(match -> {
            param.append(param.length() == 0 ? "?" :"&");
            param.append("match[]=").append(match);
        });

        remoteNodeIPs.forEach(ip -> {
            HTTP.Builder hb = HTTP.post();
            hb.url(String.format("http://%s:%s%s%s", ip, PrometheusGlobalProperty.PORT, HTTP_SERIES2_PATH, param.toString()));
            addBasicAuthIfEnabled(hb);
            hbs.add(hb);
        });

        boolean result = true;
        for (HTTP.Builder hb : hbs) {
            try (Response rsp = hb.callWithException()) {
                if (!rsp.isSuccessful()) {
                    logger.warn(String.format("failed to call prometheus[%s, status code: %s], %s", hb.getParam().getUrl(), rsp.code(), rsp.body().string()));
                    result = false;
                }
            } catch (IOException e) {
                logger.warn(String.format("failed to call prometheus[%s], %s", hb.getParam().getUrl(), e.getMessage()));
                result = false;
            }
        }

        return result;
    }

    @Override
    public void ctlstop() {
        localNode.ctlstop();
    }
}
