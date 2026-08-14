package org.zstack.network.hostNetworkInterface.lldp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.message.APIMessage;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.network.hostNetworkInterface.lldp.api.APIChangeHostNetworkInterfaceLldpModeMsg;
import org.zstack.network.hostNetworkInterface.lldp.api.APIGetHostNetworkInterfaceLldpMsg;
import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpVO;
import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpVO_;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.argerr;

@InterceptorForService("hostNetwork.lldp")
public class LldpApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    private static final CLogger logger = CLoggerImpl.getLogger(LldpApiInterceptor.class);

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIChangeHostNetworkInterfaceLldpModeMsg) {
            validate((APIChangeHostNetworkInterfaceLldpModeMsg)msg);
        } else if (msg instanceof APIGetHostNetworkInterfaceLldpMsg) {
            validate((APIGetHostNetworkInterfaceLldpMsg)msg);
        }

        return msg;
    }

    private void validate(APIChangeHostNetworkInterfaceLldpModeMsg msg) {
        List<String> hostUuids = Q.New(HostNetworkInterfaceVO.class).select(HostNetworkInterfaceVO_.hostUuid)
                .in(HostNetworkInterfaceVO_.uuid, msg.getInterfaceUuids())
                .listValues();
        Set<String> set = new HashSet<>(hostUuids);
        if (set.size() > 1) {
            throw new ApiMessageInterceptionException((argerr("could not change lldp mode for the interfaces of different hosts")));
        }
    }

    private void validate(APIGetHostNetworkInterfaceLldpMsg msg) {
        String mode = Q.New(HostNetworkInterfaceLldpVO.class).select(HostNetworkInterfaceLldpVO_.mode).eq(HostNetworkInterfaceLldpVO_.interfaceUuid, msg.getInterfaceUuid()).findValue();
        if (mode != null && !mode.contains("rx")) {
            throw new ApiMessageInterceptionException((argerr("could not get interface lldp info which is not in receive mode")));
        }
    }
}
