package org.zstack.zsv;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.zsv.storage.api.APICheckCephPluginMsg;
import org.zstack.zsv.storage.api.HostSshParameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.CollectionUtils.isEmpty;

@InterceptorForService({"ZsvStorage", "zsv"})
public class ZsvApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICheckCephPluginMsg) {
            validate((APICheckCephPluginMsg) msg);
        }
        return msg;
    }

    private void validate(APICheckCephPluginMsg msg) {
        if (!isEmpty(msg.getExternalHosts())) {
            for (HostSshParameter parameter : msg.getExternalHosts()) {
                if (NetworkUtils.isIpv4Address(parameter.getIp())) {
                    continue;
                }
                if (IPv6NetworkUtils.isIpv6Address(parameter.getIp())) {
                    continue;
                }
                throw new ApiMessageInterceptionException(argerr("invalid ip format [%s]", parameter.getIp()));
            }
        }

        if (msg.getExternalHosts() == null) {
            msg.setExternalHosts(new ArrayList<>());
        }

        if (!isEmpty(msg.getIpList())) {
            for (String ip : msg.getIpList()) {
                msg.getExternalHosts().add(new HostSshParameter(ip));
                if (NetworkUtils.isIpv4Address(ip)) {
                    continue;
                }
                if (IPv6NetworkUtils.isIpv6Address(ip)) {
                    continue;
                }
                throw new ApiMessageInterceptionException(argerr("invalid ip format [%s]", ip));
            }
        }
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(APICheckCephPluginMsg.class);
    }
}
