package org.zstack.search;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.search.APIGetMessage;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class GetQuery {
	private static CLogger logger = Utils.getLogger(CLogger.class);
	
    @Autowired
    private InventoryIndexManager mgr;
    
    private String callElasticSearch(final String url) {
        try {
            HttpGet get = new HttpGet(url);
            ResponseHandler<String> rspHandler = new ResponseHandler<String>() {
                @Override
                public String handleResponse(HttpResponse rsp) throws ClientProtocolException, IOException {
                    String res = null;
                    if (rsp.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                        logger.trace(String.format("failed to call %s, 404 not found, the entity queryed doesn't exist", url));
                    } else if (rsp.getStatusLine().getStatusCode() != HttpStatus.SC_OK && rsp.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                        String err = String.format("Failed to call , because: \nstatus line: %s\nbody: %s\n",
                                 rsp.getStatusLine(), EntityUtils.toString(rsp.getEntity()));
                        logger.warn(err);
                        throw new IOException(err);
                    } else {
                        res = EntityUtils.toString(rsp.getEntity());
                        logger.trace(String.format("Successfully call %s, %s", url, res));
                    }

                    return res;
                }
            };

            String res = mgr.getHttpClient().execute(get, rspHandler);
            return res;
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage(), e);
        }
    }
    
    public <T> String getAsString(APIGetMessage msg, Class<T> retClass) {
        return getAsString(msg.getUuid(), retClass);
    }
    
    public <T> String getAsString(String uuid, Class<T> retClass) {
    	String url = String.format("%s/%s/%s/%s", mgr.getElasticSearchBaseUrl(), retClass.getSimpleName().toLowerCase(), retClass.getSimpleName(), uuid);
    	String res = callElasticSearch(url);
    	if (res == null) {
    	    return res;
    	}
    	
    	JSONObject jo;
        try {
            jo = new JSONObject(res);
            return jo.getString("_source");
        } catch (JSONException e) {
            throw new CloudRuntimeException(e);
        }
    }
    
    public <T> T get(String uuid, Class<T> retClass) {
    	String res = getAsString(uuid, retClass);
    	if (res == null) {
    	    return null;
    	}
    	
    	return JSONObjectUtil.toObject(res, retClass);
    }
}
