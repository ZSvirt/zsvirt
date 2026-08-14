package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.Component;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.exception.CloudConfigureFailException;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2016/10/31.
 */
public class MevocoVmInstanceBaseFactory implements VmInstanceBaseExtensionFactory, Component {
    protected static final CLogger logger = Utils.getLogger(MevocoVmInstanceBaseFactory.class);
    @Override
    public VmInstance getVmInstance(VmInstanceVO vo) {
        return new MevocoVmInstanceBase(vo);
    }

    @Override
    public List<Class> getMessageClasses() {
        return asList(APIChangeVmPasswordMsg.class, APISetNicQosMsg.class,
                APIGetNicQosMsg.class, APIGetVmQgaMsg.class,
                APIGetVmUsbRedirectMsg.class, APISetVmUsbRedirectMsg.class,
                APIGetVmRDPMsg.class, APISetVmRDPMsg.class,
                APIGetVmMonitorNumberMsg.class, APISetVmMonitorNumberMsg.class,
                APISetVmQgaMsg.class, CloneVmSyncQosMsg.class,
                APIChangeVmImageMsg.class, ChangeVmImageMsg.class,
                APIGetImageCandidatesForVmToChangeMsg.class,
                APIUpdateVmNicMacMsg.class,
                GetImageCandidatesForVmToChangeMsg.class,
                CloneVmInstanceMsg.class,
                ChangeVmPasswordMsg.class,
                APISyncVmClockMsg.class, SyncVmClockMsg.class, SetVmQgaSyncClockTaskMsg.class,
                GetVmNicQosMsg.class,
                SetVmNicQosMsg.class,
                APIGetVmEmulatorPinningMsg.class, APISetVmEmulatorPinningMsg.class);
    }

    @Autowired
    private AnsibleFacade asf;

    @Override
    public boolean start() {
        try {
            deploySaltState();
            createVmFlowChainBuilder();
        } catch (Exception e) {
            throw new CloudConfigureFailException(MevocoVmInstanceBaseFactory.class, e.getMessage(), e);
        }return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private FlowChainBuilder changeVmPasswdFlowBuilder;
    private FlowChainBuilder changeVmImageFlowBuilder;

    private List<String> changeVmPasswdFlowElements;
    private List<String> changeVmImageFlowElements;

    private void createVmFlowChainBuilder() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        changeVmPasswdFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(changeVmPasswdFlowElements).construct();
        changeVmImageFlowBuilder  = FlowChainBuilder.newBuilder().setFlowClassNames(changeVmImageFlowElements).construct();
    }

    public FlowChain getChangeVmPasswordWorkFlowChain() {
        return changeVmPasswdFlowBuilder.build();
    }

    public FlowChain getChangeVmImageWorkFlowChain() {
        return changeVmImageFlowBuilder.build();
    }

    public void setChangeVmPasswdFlowElements(List<String> changeVmPasswdFlowElements) {
        this.changeVmPasswdFlowElements = changeVmPasswdFlowElements;
    }

    public void setChangeVmImageFlowElements(List<String> changeVmImageFlowElements) {
        this.changeVmImageFlowElements = changeVmImageFlowElements;
    }

    private void deploySaltState() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

    }
}
