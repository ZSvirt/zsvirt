package org.zstack.console;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.console.APIRequestConsoleAccessMsg;
import org.zstack.header.console.APIUpdateConsoleProxyAgentMsg;
import org.zstack.header.console.ConsoleConstants;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;

import java.util.Arrays;
import java.util.List;

import static org.zstack.core.Platform.operr;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 4:46 PM
 * To change this template use File | Settings | File Templates.
 */
@InterceptorForService("console")
public class ConsoleApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIRequestConsoleAccessMsg) {
            validate((APIRequestConsoleAccessMsg) msg);
        } else if (msg instanceof APIUpdateConsoleProxyAgentMsg) {
            validate((APIUpdateConsoleProxyAgentMsg) msg);
        }

        return msg;
    }

    private List<VmInstanceState> consoleAvailableStates = Arrays.asList(
            VmInstanceState.Running,
            VmInstanceState.Crashed,
            VmInstanceState.VolumeRecovering,
            VmInstanceState.Paused,
            VmInstanceState.NoState
    );

    private void validate(APIRequestConsoleAccessMsg msg) {
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.state);
        q.add(VmInstanceVO_.uuid, Op.EQ, msg.getVmInstanceUuid());
        VmInstanceState state = q.findValue();
        if (!consoleAvailableStates.contains(state)) {
            throw new ApiMessageInterceptionException(operr("vm[uuid:%s] is not in state of %s, current state is %s",
                    msg.getVmInstanceUuid(), consoleAvailableStates, state));
        }
        bus.makeTargetServiceIdByResourceUuid(msg, ConsoleConstants.SERVICE_ID, msg.getVmInstanceUuid());
    }

    private void validate(APIUpdateConsoleProxyAgentMsg msg) {
        // consoleProxyOverriddenIp default value is 0.0.0.0
        if (msg.getConsoleProxyOverriddenIp().trim().equals("")) {
            msg.setConsoleProxyOverriddenIp("0.0.0.0");
        }
    }
}
