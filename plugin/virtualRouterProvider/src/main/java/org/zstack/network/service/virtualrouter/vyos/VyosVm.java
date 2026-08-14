package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmAsyncHttpCallMsg;
import org.zstack.appliancevm.ApplianceVmRefreshFirewallMsg;
import org.zstack.appliancevm.ApplianceVmRefreshFirewallReply;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HypervisorType;
import org.zstack.network.service.virtualrouter.VirtualRouter;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xing5 on 2016/10/31.
 */
public class VyosVm extends VirtualRouter {
    @Autowired
    private VyosVmFactory vyosf;

    public VyosVm(VirtualRouterVmVO vo) {
        super(vo);
    }

    protected List<Flow> createBootstrapFlows(HypervisorType hvType) {
        List<Flow> flows = new ArrayList<>();

        flows.add(apvmf.createBootstrapFlow(hvType));
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            flows.add(new VyosGetVersionFlow());
            flows.add(new VyosWaitAgentStartFlow());
            flows.add(new VyosDeployAgentFlow());
        }

        return flows;
    }

    @Override
    protected List<Flow> getPostCreateFlows() {
        return vyosf.getPostCreateFlows();
    }

    @Override
    protected List<Flow> getPostStartFlows() {
        return vyosf.getPostStartFlows();
    }

    @Override
    protected List<Flow> getPostStopFlows() {
        return vyosf.getPostStopFlows();
    }

    @Override
    protected List<Flow> getPostRebootFlows() {
        return vyosf.getPostRebootFlows();
    }

    @Override
    protected List<Flow> getPostDestroyFlows() {
        return vyosf.getPostDestroyFlows();
    }

    @Override
    protected List<Flow> getPostMigrateFlows() {
        return vyosf.getPostMigrateFlows();
    }

    @Override
    protected FlowChain getReconnectChain() {
        return vyosf.getReconnectFlowChain();
    }

    @Override
    protected FlowChain getProvisionConfigChain() {
        return vyosf.getFlushConfigChain();
    }

    @Override
    protected void handle(final ApplianceVmRefreshFirewallMsg msg) {
        // vyos doesn't need appliance vm
        ApplianceVmRefreshFirewallReply reply = new ApplianceVmRefreshFirewallReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(final ApplianceVmAsyncHttpCallMsg msg) {
        // vyos doesn't need appliance vm
        throw new CloudRuntimeException("should not be called");
    }
}
