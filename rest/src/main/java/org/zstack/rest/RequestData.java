package org.zstack.rest;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by xing5 on 2017/1/2.
 */
public class RequestData {
    String webHook;
    APIMessage apiMessage;
    RestServer.RequestInfo requestInfo;
    private String apiClassName;

    static RequestData fromJson(String jsonstr) {
        Map m = JSONObjectUtil.toObject(jsonstr, LinkedHashMap.class);

        RequestData d = new RequestData();
        d.webHook = (String) m.get("webHook");
        d.apiClassName = (String) m.get("apiClassName");
        d.requestInfo = JSONObjectUtil.rehashObject(m.get("requestInfo"), RestServer.RequestInfo.class);

        if (d.apiClassName != null) {
            try {
                Class<? extends APIMessage> clz = (Class<? extends APIMessage>) Class.forName(d.apiClassName);
                d.apiMessage = JSONObjectUtil.rehashObject(m.get("apiMessage"), clz);
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        }

        return d;
    }

    String toJson() {
        if (apiMessage != null) {
            apiClassName = apiMessage.getClass().getName();
        }

        return RequestDataJSONObjectUtil.toJsonString(this);
    }
}
