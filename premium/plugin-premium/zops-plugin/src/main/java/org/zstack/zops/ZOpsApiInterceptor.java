package org.zstack.zops;

import org.apache.commons.validator.routines.DomainValidator;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.message.APIMessage;
import org.zstack.zops.api.APICheckNetworkReachableMsg;
import org.zstack.zops.api.APIUpdateChronyServersMsg;
import org.zstack.utils.network.NetworkUtils;

import java.util.List;

import static org.zstack.core.Platform.argerr;

@InterceptorForService("zops")
public class ZOpsApiInterceptor implements ApiMessageInterceptor {
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICheckNetworkReachableMsg) {
            validate((APICheckNetworkReachableMsg) msg);
        } else if (msg instanceof APIUpdateChronyServersMsg) {
            validate((APIUpdateChronyServersMsg) msg);
        }

        return msg;
    }

    private void validate(APICheckNetworkReachableMsg msg) {
        DomainValidator validator = DomainValidator.getInstance();
        for (String targetHostname : msg.getTargetHostnames()) {
            if (!NetworkUtils.isValidIPAddress(targetHostname) && !validator.isValid(targetHostname)) {
                throw new ApiMessageInterceptionException(argerr("%s is not a valid ip address or domain name", targetHostname));
            }
        }

        if (msg.getSourceHostnames() == null) return;

        for (String sourceHostname : msg.getSourceHostnames()) {
            if (!NetworkUtils.isValidIPAddress(sourceHostname)) {
                throw new ApiMessageInterceptionException(argerr("%s is not a valid ip address", sourceHostname));
            }
        }
    }

    private void validate(APIUpdateChronyServersMsg msg) {
        if (msg.getInternalHostnames() == null && msg.getExternalServers() == null) {
            throw new ApiMessageInterceptionException(argerr("internal and external chrony servers cannot be null at the same time"));
        }

        validateHostnames(msg.getInternalHostnames());
        validateHostnames(msg.getExternalServers());
    }

    private void validateHostnames(List<String> hostnames) {
        DomainValidator validator = DomainValidator.getInstance();
        if (hostnames == null) return;
        for (String targetHostname : hostnames) {
            if (!NetworkUtils.isValidIPAddress(targetHostname) && !validator.isValid(targetHostname)) {
                throw new ApiMessageInterceptionException(argerr("%s is not a valid ip address or domain name", targetHostname));
            }
        }
    }
}
