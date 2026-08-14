package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.kvm.KVMConstant;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/hosts/kvm/from-file",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddHostFromConfigFileEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 24)
public class APIAddKVMHostFromConfigFileMsg extends APIAddHostFromConfigFileMsg {
    @Override
    public String getHypervisorType() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }

    public static APIAddKVMHostFromConfigFileMsg __example__() {
        APIAddKVMHostFromConfigFileMsg msg = new APIAddKVMHostFromConfigFileMsg();
        msg.setHostInfo("FILE CONTENT ENCODE BY BASE64");
        return msg;
    }
}
