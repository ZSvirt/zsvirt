package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * Created by shixin on 03/26/2018.
 */
@RestRequest(
        path = "/load-balancers/listeners/{listenerUuid}/certificate",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveCertificateFromLoadBalancerListenerEvent.class
)
public class APIRemoveCertificateFromLoadBalancerListenerMsg extends APIMessage implements LoadBalancerMessage, APIAuditor {
    @APIParam(resourceType = CertificateVO.class, nonempty = true)
    private String certificateUuid;
    @APIParam(resourceType = LoadBalancerListenerVO.class)
    private String listenerUuid;
    @APINoSee
    private String loadBalancerUuid;

    public String getListenerUuid() {
        return listenerUuid;
    }

    public void setListenerUuid(String listenerUuid) {
        this.listenerUuid = listenerUuid;
    }

    public String getCertificateUuid() {
        return certificateUuid;
    }

    public void setCertificateUuid(String certificateUuid) {
        this.certificateUuid = certificateUuid;
    }

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }
 
    public static APIRemoveCertificateFromLoadBalancerListenerMsg __example__() {
        APIRemoveCertificateFromLoadBalancerListenerMsg msg = new APIRemoveCertificateFromLoadBalancerListenerMsg();

        msg.setCertificateUuid(uuid());
        msg.setListenerUuid(uuid());
        msg.setLoadBalancerUuid(uuid());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIRemoveCertificateFromLoadBalancerListenerMsg)msg).getLoadBalancerUuid(), LoadBalancerVO.class);
    }
}
