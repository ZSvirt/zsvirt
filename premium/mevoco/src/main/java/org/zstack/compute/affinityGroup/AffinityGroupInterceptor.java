package org.zstack.compute.affinityGroup;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginDSL;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.affinitygroup.*;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;

import static org.zstack.core.Platform.argerr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
@InterceptorForService("affinityGroup")
public class AffinityGroupInterceptor implements GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(AffinityGroupInterceptor.class);
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acntMgr;

    private void validateVmState(String vmUuid) {
        VmInstanceState state = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).select(VmInstanceVO_.state).findValue();
        if (!(state.toString().equals(VmInstanceState.Running.toString()) || state.toString().equals(VmInstanceState.Stopped.toString()))) {
            throw new ApiMessageInterceptionException(argerr("Vm can change its affinityGroup only in state [%s,%s], but vm is in state [%s]",
                    VmInstanceState.Running.toString(), VmInstanceState.Stopped.toString(), state.toString()));
        }
    }

    private void validate(APIAddVmToAffinityGroupMsg msg) {
        /* a vm can not be added to more that 1 affinityGroup */
        String agUuid = Q.New(AffinityGroupUsageVO.class).eq(AffinityGroupUsageVO_.resourceUuid, msg.getUuid())
                .select(AffinityGroupUsageVO_.affinityGroupUuid).findValue();
        if(agUuid != null) {
            throw new ApiMessageInterceptionException(argerr("Vm [uuid: %s] is already added to affinityGroup [uuid: %s]", msg.getUuid(), agUuid));
        }

        validateVmState(msg.getUuid());
        validateAffinityGroup(msg.getAffinityGroupUuid());
        /* vm can not be added to disabled affinityGroup */
        validateAffinityGroupState(msg.getAffinityGroupUuid());
    }

    private boolean isSystemAffinityGroup(AffinityGroupVO vo) {
        if (AffinityGroupAppliance.valueOf(vo.getAppliance().toUpperCase()) == AffinityGroupAppliance.CUSTOMER) {
            return false;
        } else {
            return true;
        }
    }

    private void validate(APIRemoveVmFromAffinityGroupMsg msg) {
        validateVmState(msg.getUuid());
        validateAffinityGroup(msg.getAffinityGroupUuid());
    }

    private void validateAffinityGroup(String affinityGroupUuid){
        AffinityGroupVO vo = dbf.findByUuid(affinityGroupUuid, AffinityGroupVO.class);
        if (vo == null){
            throw new ApiMessageInterceptionException(argerr("AffinityGroup [uuid: %s] does not existed", affinityGroupUuid));
        }

        if (isSystemAffinityGroup(vo)) {
            throw new ApiMessageInterceptionException(argerr("Can not operate on affinity group created by system"));
        }
    }

    private void validateAffinityGroupState(String affinityGroupUuid){
        AffinityGroupVO vo = dbf.findByUuid(affinityGroupUuid, AffinityGroupVO.class);
        if (vo != null && vo.getState() != AffinityGroupState.Enabled) {
            throw new ApiMessageInterceptionException(argerr("Can not operate on affinityGroup [uuid: %s] which is not enabled",
                    affinityGroupUuid));
        }
    }

    private void validate(APICreateVmInstanceMsg msg) {
        if(msg.getSystemTags() == null || msg.getSystemTags().isEmpty()) {
            return;
        }

        PatternedSystemTag tag =  AffinityGroupSystemTags.AFFINITY_GROUP_UUID;
        String token = AffinityGroupSystemTags.AFFINITY_GROUP_UUID_TOKEN;

        String agUuid = SystemTagUtils.findTagValue(msg.getSystemTags(), tag, token);
        if(StringUtils.isEmpty(agUuid)){
            return;
        }

        validateAffinityGroup(agUuid);
        /* vm can not be added to disabled affinityGroup */
        validateAffinityGroupState(agUuid);
    }

    private void validate(APICloneVmInstanceMsg msg) {
        if(msg.getSystemTags() == null || msg.getSystemTags().isEmpty()) {
            return;
        }

        PatternedSystemTag tag =  AffinityGroupSystemTags.AFFINITY_GROUP_UUID;
        String token = AffinityGroupSystemTags.AFFINITY_GROUP_UUID_TOKEN;

        String agUuid = SystemTagUtils.findTagValue(msg.getSystemTags(), tag, token);
        if(StringUtils.isEmpty(agUuid)){
            return;
        }

        validateAffinityGroup(agUuid);
        /* vm can not be added to disabled affinityGroup */
        validateAffinityGroupState(agUuid);
    }

    private void validate(APICreateAffinityGroupMsg msg) {
        return;
    }

    private void validate(APIDeleteAffinityGroupMsg msg) {
        validateAffinityGroup(msg.getUuid());
    }

    private void validate(APIUpdateAffinityGroupMsg msg) {
        validateAffinityGroup(msg.getUuid());
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(APICreateVmInstanceMsg.class, APICloneVmInstanceMsg.class, APIAddVmToAffinityGroupMsg.class,
                APIRemoveVmFromAffinityGroupMsg.class, APICreateAffinityGroupMsg.class, APIDeleteAffinityGroupMsg.class,
                APIUpdateAffinityGroupMsg.class);
    }

    @Override
    public GlobalApiMessageInterceptor.InterceptorPosition getPosition() {
        return GlobalApiMessageInterceptor.InterceptorPosition.FRONT;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateVmInstanceMsg) {
            validate((APICreateVmInstanceMsg)msg);
        } else if (msg instanceof APICloneVmInstanceMsg) {
            validate((APICloneVmInstanceMsg)msg);
        } else if (msg instanceof APIAddVmToAffinityGroupMsg) {
            validate((APIAddVmToAffinityGroupMsg) msg);
        } else if (msg instanceof APIRemoveVmFromAffinityGroupMsg) {
            validate((APIRemoveVmFromAffinityGroupMsg) msg);
        } else if (msg instanceof APICreateAffinityGroupMsg) {
            validate((APICreateAffinityGroupMsg) msg);
        } else if (msg instanceof APIDeleteAffinityGroupMsg) {
            validate((APIDeleteAffinityGroupMsg) msg);
        } else if (msg instanceof APIUpdateAffinityGroupMsg) {
            validate((APIUpdateAffinityGroupMsg) msg);
        }

        return msg;
    }

    {
        PluginDSL.PluginDefinition definition = new PluginDSL.PluginDefinition(AffinityGroupInterceptor.class);
        definition.newExtension().extensionClass(GlobalApiMessageInterceptor.class);
    }
}
