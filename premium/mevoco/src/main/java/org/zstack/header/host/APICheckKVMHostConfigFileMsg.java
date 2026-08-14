package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.rest.RestRequest;
import org.zstack.kvm.KVMConstant;

@RestRequest(
        path = "/hosts/kvm/from-file/check",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICheckHostConfigFileReply.class
)
public class APICheckKVMHostConfigFileMsg extends APICheckHostConfigFileMsg {
    @Override
    public String getHypervisorType() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }

    public static APICheckKVMHostConfigFileMsg __example__() {
        APICheckKVMHostConfigFileMsg msg = new APICheckKVMHostConfigFileMsg();
        msg.setHostInfo("FILE CONTENT ENCODE BY BASE64");
        return msg;
    }
}
