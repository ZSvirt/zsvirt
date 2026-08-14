package org.zstack.crypto.keyprovider.api;

import org.springframework.http.HttpMethod;
import org.zstack.crypto.keyprovider.KeyProviderMessage;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.header.message.APIBatchRequest;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

import java.util.List;

@RestRequest(
        path = "/key-providers/{providerUuid}/rekey",
        method = HttpMethod.PUT,
        responseClass = APIRekeyKeyProviderRefsEvent.class,
        isAction = true
)
public class APIRekeyKeyProviderRefsMsg extends APIMessage implements KeyProviderMessage, APIAuditor, APIBatchRequest {
    @APIParam(required = false, nonempty = true, emptyString = false)
    private List<Long> refIds;

    @APIParam(required = false, nonempty = true, emptyString = false)
    private List<String> resourceUuids;

    @APIParam(required = false, emptyString = false)
    private String resourceType;

    @APIParam(resourceType = KeyProviderVO.class, emptyString = false)
    private String providerUuid;

    @APIParam(required = false)
    private boolean rekeyAll;

    public List<Long> getRefIds() {
        return refIds;
    }

    public void setRefIds(List<Long> refIds) {
        this.refIds = refIds;
    }

    public List<String> getResourceUuids() {
        return resourceUuids;
    }

    public void setResourceUuids(List<String> resourceUuids) {
        this.resourceUuids = resourceUuids;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getProviderUuid() {
        return providerUuid;
    }

    public void setProviderUuid(String providerUuid) {
        this.providerUuid = providerUuid;
    }

    public boolean isRekeyAll() {
        return rekeyAll;
    }

    public void setRekeyAll(boolean rekeyAll) {
        this.rekeyAll = rekeyAll;
    }

    @Override
    public String getKeyProviderUuid() {
        return providerUuid;
    }

    @Override
    public APIAuditor.Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIRekeyKeyProviderRefsMsg) msg).getProviderUuid(), KeyProviderVO.class);
    }

    @Override
    public APIBatchRequest.Result collectResult(APIMessage message, APIEvent rsp) {
        APIRekeyKeyProviderRefsEvent evt = (APIRekeyKeyProviderRefsEvent) rsp;
        if (evt.getTotalCount() == 0) {
            // Keep event counters truthful (0/0) while mapping x-job-batch to SUCCESS for rekey no-op.
            return new APIBatchRequest.Result(1, 1);
        }
        return new APIBatchRequest.Result(evt.getTotalCount(), evt.getSuccessCount());
    }

    public static APIRekeyKeyProviderRefsMsg __example__() {
        APIRekeyKeyProviderRefsMsg msg = new APIRekeyKeyProviderRefsMsg();
        msg.setProviderUuid(uuid(KeyProviderVO.class));
        msg.setRefIds(java.util.Arrays.asList(1L, 2L));
        msg.setRekeyAll(false);
        return msg;
    }
}
