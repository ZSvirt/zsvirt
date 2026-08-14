package org.zstack.simulator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.rest.RESTApiDecoder;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.rest.RestAPIResponse;
import org.zstack.header.rest.RestAPIState;
import org.zstack.utils.URLBuilder;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SyncRESTCaller {
    private static final CLogger logger = Utils.getLogger(SyncRESTCaller.class);
    @Autowired
    private RESTFacade restf;
    
    private String baseUrl;
    
    public SyncRESTCaller(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public SyncRESTCaller() {
        this("http://localhost:8080/");
    }
    
    private RestAPIResponse queryResponse(String uuid) {
        String url =  URLBuilder.buildUrlFromBase(baseUrl, RESTConstant.REST_API_RESULT, uuid);
        String res = restf.getRESTTemplate().getForObject(url, String.class);
        return JSONObjectUtil.toObject(res, RestAPIResponse.class);
    }
    
    public RestAPIResponse syncPost(String path, APIMessage msg, long interval, long timeout) throws InterruptedException {
        String msgStr = RESTApiDecoder.dump(msg);
        String url = URLBuilder.buildUrlFromBase(baseUrl, path);
        RestAPIResponse rsp = restf.syncJsonPost(url, msgStr, RestAPIResponse.class);
        long curr = 0;
        while (!rsp.getState().equals(RestAPIState.Done.toString()) && curr < timeout) {
            Thread.sleep(interval);
            rsp = queryResponse(rsp.getUuid());
            curr += interval;
        }
        
        if (curr >= timeout) {
            throw new CloudRuntimeException(String.format("timeout after %s ms, result uuid:%s", curr, rsp.getUuid()));
        }
        
        return rsp;
    }
    
    public RestAPIResponse syncPost(String url, APIMessage msg) throws InterruptedException {
        return syncPost(url, msg, 500, TimeUnit.SECONDS.toMillis(15));
    }
}
