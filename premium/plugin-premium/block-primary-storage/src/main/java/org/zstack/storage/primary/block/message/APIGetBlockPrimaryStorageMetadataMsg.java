package org.zstack.storage.primary.block.message;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/10 23:51
 */
@RestRequest(
        path = "/primary-storage/block/metadata",
        method = HttpMethod.POST,
        responseClass = APIQueryBlockPrimaryStorageReply.class,
        parameterName = "param"
)
public class APIGetBlockPrimaryStorageMetadataMsg extends APISyncCallMessage {
    @APIParam()
    private String vendorName;

    @APIParam()
    private String metadata;

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getMetadata() {
        return metadata;
    }

    public static APIGetBlockPrimaryStorageMetadataMsg __example__() {
        APIGetBlockPrimaryStorageMetadataMsg msg = new APIGetBlockPrimaryStorageMetadataMsg();
        msg.setVendorName("test-vendor");
        msg.setMetadata("{\"vendor\":\"test-vendor\",\"version\":\"1.0\"}");
        return msg;
    }
}
