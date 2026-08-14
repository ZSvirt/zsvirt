package org.zstack.header.vipQos;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by liangbo.zhou on 17-6-10.
 */
@RestResponse
public class APIDeleteVipQosEvent extends APIEvent{
    public APIDeleteVipQosEvent(){
        super();
    }
    
    public APIDeleteVipQosEvent(String apiId){
        super(apiId);
    }

    public static APIDeleteVipQosEvent __example__(){
        APIDeleteVipQosEvent event = new APIDeleteVipQosEvent();

        return  event;
    }
}
