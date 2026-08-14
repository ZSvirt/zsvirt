package org.zstack.vpc;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.*;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vpc.VpcConstants;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

/**
 * Created by weiwang on 15/11/2017
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VpcVyosDeployZsnAgentFlow extends NoRollbackFlow {
    protected static final CLogger logger = Utils.getLogger(VpcVyosDeployZsnAgentFlow.class);
    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    private boolean isVpcVrouter(VirtualRouterVmInventory vr) {
        if (vr == null) {
            logger.debug("can not get vr inventory, skip to deploy vpc vr");
            return false;
        }

        return vr.getApplianceVmType().equals(VpcConstants.VPC_VROUTER_VM_TYPE);
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            trigger.next();
            return;
        }

        boolean isReconnect = Boolean.parseBoolean((String) data.get(ApplianceVmConstant.Params.isReconnect.toString()));

        Boolean state;
        String vrUuid;

        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        if (spec == null) {
            logger.debug("can not get vm spec, try to use vr inventory");
            VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
            if (!isVpcVrouter(vr)) {
                trigger.next();
                return;
            }

            vrUuid = vr.getUuid();
            state = new VpcDistributedRoutingGetter().getState(vrUuid);
        } else {
            final ApplianceVmSpec aspec = spec.getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec.class);
            if (aspec == null) {
                logger.debug("can not get appliance spec, try to use vr inventory");
                VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
                if (!isVpcVrouter(vr)) {
                    trigger.next();
                    return;
                }
                vrUuid = vr.getUuid();
                state = new VpcDistributedRoutingGetter().getState(vrUuid);
            } else if (!aspec.getApplianceVmType().toString().equals(VpcConstants.VPC_VROUTER_VM_TYPE)) {
                trigger.next();
                return;
            } else {
                vrUuid = spec.getVmInventory().getUuid();
                state = new VpcDistributedRoutingGetter().getState(vrUuid);
            }
        }

        String mgmtNicIp;
        if (!isReconnect && spec != null) {
            VmNicInventory mgmtNic;
            if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.NewCreate) {
                final ApplianceVmSpec aspec = spec.getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec.class);
                mgmtNic = CollectionUtils.findOneOrNull(spec.getDestNics(),
                        arg -> arg.getL3NetworkUuid().equals(aspec.getManagementNic().getL3NetworkUuid()));
            } else {
                ApplianceVmVO avo = dbf.findByUuid(spec.getVmInventory().getUuid(), ApplianceVmVO.class);
                ApplianceVmInventory ainv = ApplianceVmInventory.valueOf(avo);
                mgmtNic = ainv.getManagementNic();
            }
            mgmtNicIp = mgmtNic.getIp();
        } else {
            mgmtNicIp = (String) data.get(ApplianceVmConstant.Params.managementNicIp.toString());
        }

        int timeoutInSeconds = ApplianceVmGlobalConfig.CONNECT_TIMEOUT.value(Integer.class);
        long timeout = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutInSeconds);
        int sshPort = VirtualRouterGlobalConfig.SSH_PORT.value(Integer.class);
        String vrUserTag = VirtualRouterSystemTags.VIRTUAL_ROUTER_LOGIN_USER.getTokenByResourceUuid(
                            vrUuid, VirtualRouterVmVO.class, VirtualRouterSystemTags.VIRTUAL_ROUTER_LOGIN_USER_TOKEN);
        String remoteUser = vrUserTag != null ? vrUserTag : "vyos"; //old vpc vrouter has no tag, that's vyos.
        String remoteZsnPath = String.format("/home/%s/zsn-agent.bin", remoteUser);

        final Long tmout = state ? VpcGlobalConfig.ZSNP_TMOUT.value(Long.class) : -1 * VpcGlobalConfig.ZSNP_TMOUT.value(Long.class);

        if (isReconnect && !NetworkUtils.isRemotePortOpen(mgmtNicIp, sshPort, 2000)) {
            throw new OperationFailureException(operr("unable to ssh in to the vpc router[%s], the ssh port seems not open", mgmtNicIp));
        }
        logger.debug("deploying vpc zsn agent");

        thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            @Override
            public boolean run() {
                try {
                    long now = System.currentTimeMillis();
                    if (now > timeout) {
                        trigger.fail(err(ApplianceVmErrors.UNABLE_TO_START, "the SSH port is not" +
                                " open after %s seconds. Failed to login the vpc router[ip:%s]", timeoutInSeconds, mgmtNicIp));
                        return true;
                    }
                    if (NetworkUtils.isRemotePortOpen(mgmtNicIp, sshPort, 2000)) {
                        logger.debug(String.format("%s in mgmt nic of vpc vrouter %s open",sshPort, mgmtNicIp));
                        deployAgent();
                        logger.debug("deploy succeed");
                        return true;
                    } else {
                        return false;
                    }
                } catch (Throwable t) {
                    logger.warn("unhandled exception happened", t);
                    return false;
                }
            }

            private void deployAgent() {
                File zsnAgentBinary = PathUtil.findFileOnClassPath("ansible/zsnagentansible/zsn-agent.bin", true);
                SshResult ret = new Ssh().setTimeout(timeoutInSeconds).setPrivateKey(asf.getPrivateKey()).setUsername(remoteUser).setHostname(mgmtNicIp)
                        .setPort(sshPort).command("uname -m").setTimeout(60).runAndClose();
                if (ret.getReturnCode() == 0 && !ret.getStdout().trim().equals("x86_64")) {
                    zsnAgentBinary = PathUtil.findFileOnClassPath(String.format("ansible/zsnagentansible/zsn-agent.%s.bin", ret.getStdout().trim()), true);
                }
                logger.debug(String.format("scp %s to vrouter %s", zsnAgentBinary, mgmtNicIp));
                new Ssh().setTimeout(timeoutInSeconds).scpUpload(zsnAgentBinary.getAbsolutePath(), remoteZsnPath)
                        .setPrivateKey(asf.getPrivateKey()).setUsername(remoteUser).setHostname(mgmtNicIp).setPort(sshPort)
                        .runErrorByExceptionAndClose();

                logger.debug(String.format("run zsn-agent.bin in vrouter %s", mgmtNicIp));


                new Ssh().shell(String.format("sudo bash %s\n" +
                        "sudo bash -c 'export ZSNP_TMOUT=%s && /usr/local/zstack/zsn-agent/bin/zstack-network-agent restart'\n",
                        remoteZsnPath, tmout)
                ).setTimeout(timeoutInSeconds).setPrivateKey(asf.getPrivateKey()).setUsername(remoteUser).setHostname(mgmtNicIp).setPort(sshPort).runErrorByExceptionAndClose();

                trigger.next();
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return 1;
            }

            @Override
            public String getName() {
                return VpcVyosDeployZsnAgentFlow.class.getName();
            }
        });
    }
}
