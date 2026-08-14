package org.zstack.network.service.virtualrouter;

import org.springframework.http.HttpMethod;
import org.zstack.header.configuration.APIUpdateInstanceOfferingEvent;
import org.zstack.header.configuration.APIUpdateInstanceOfferingMsg;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 7/31/2015.
 */
@RestRequest(
        path = "/instance-offerings/virtual-routers/{uuid}/actions",
        responseClass = APIUpdateInstanceOfferingEvent.class,
        method = HttpMethod.PUT,
        isAction = true
)
public class APIUpdateVirtualRouterOfferingMsg extends APIUpdateInstanceOfferingMsg {
    private Boolean isDefault;
    @APIParam(resourceType = ImageVO.class, required = false)
    private String imageUuid;

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
 
    public static APIUpdateVirtualRouterOfferingMsg __example__() {
        APIUpdateVirtualRouterOfferingMsg msg = new APIUpdateVirtualRouterOfferingMsg();
        msg.setUuid(uuid());
        msg.setIsDefault(false);
        return msg;
    }

}
