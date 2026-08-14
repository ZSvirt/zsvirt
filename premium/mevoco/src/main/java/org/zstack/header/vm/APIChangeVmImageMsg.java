package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

import java.util.concurrent.TimeUnit;

/**
 * Created by GuoYi on 11/2/17.
 */
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIChangeVmImageEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 24)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "vmInstanceUuid")
public class APIChangeVmImageMsg extends APICreateMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @APIParam(resourceType = ImageVO.class)
    private String imageUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public static APIChangeVmImageMsg __example__() {
        APIChangeVmImageMsg msg = new APIChangeVmImageMsg();
        msg.setVmInstanceUuid(uuid());
        msg.setImageUuid(uuid());
        return msg;
    }
}
