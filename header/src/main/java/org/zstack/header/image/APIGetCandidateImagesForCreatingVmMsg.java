package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageVO;


@RestRequest(
        path = "/images/primaryStorage/{primaryStorageUuid}/candidate-image",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateImagesForCreatingVmReply.class
)
public class APIGetCandidateImagesForCreatingVmMsg extends APISyncCallMessage {
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuid;

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public static APIGetCandidateImagesForCreatingVmMsg __example__() {
        APIGetCandidateImagesForCreatingVmMsg msg = new APIGetCandidateImagesForCreatingVmMsg();
        msg.setPrimaryStorageUuid(uuid());
        return msg;
    }

}
