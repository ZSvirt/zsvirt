package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshException;
import org.zstack.utils.ssh.SshResult;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.touterr;
import static org.zstack.core.Platform.err;
import static org.zstack.utils.CollectionUtils.findOneOrNull;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ApplianceVmConnectFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(ApplianceVmConnectFlow.class);

    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private AnsibleFacade asf;

    private static final String ERROR_LOG_PATH = "/var/lib/zstack/error.log";

    @Override
    public void run(final FlowTrigger chain, Map data) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            chain.next();
            return;
        }

        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        VmNicInventory mgmtNic;
        if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.NewCreate) {
            final ApplianceVmSpec aspec = spec.getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec.class);
            mgmtNic = findOneOrNull(spec.getDestNics(), arg -> arg.getL3NetworkUuid().equals(aspec.getManagementNic().getL3NetworkUuid()));
        } else {
            ApplianceVmVO avo = dbf.findByUuid(spec.getVmInventory().getUuid(), ApplianceVmVO.class);
            ApplianceVmInventory ainv = ApplianceVmInventory.valueOf(avo);
            mgmtNic = ainv.getManagementNic();
        }

        final int connectTimeout = ApplianceVmGlobalConfig.CONNECT_TIMEOUT.value(Integer.class);
        final String privKey = asf.getPrivateKey();
        final String username = "root";
        final int sshPort = 22;

        final boolean connectVerbose = ApplianceVmGlobalProperty.CONNECT_VERBOSE;

        final String mgmtIp = mgmtNic.getIp();
        final int interval = 2;

        class Retry {
            int value = connectTimeout / interval;
        }
        final Retry retry = new Retry();

        thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            private boolean countDown(String msg) {
                retry.value--;
                if (retry.value <= 0) {
                    ErrorCode toutErr = touterr("connecting appliance vm[uuid:%s, name:%s, ip:%s] timeout, unable to ssh in[%s]", spec.getVmInventory().getUuid(), spec.getVmInventory().getName(), mgmtIp, msg);
                    logger.warn(toutErr.getDetails());
                    chain.fail(toutErr);
                    return true;
                } else {
                    logger.debug(String.format("appliance vm[uuid:%s, name:%s, ip:%s] is still not ready, unable to ssh in[%s], continue ... will be timeout after %s seconds",
                            spec.getVmInventory().getUuid(), spec.getVmInventory().getName(), mgmtIp, msg, interval*retry.value));
                    return false;
                }
            }

            private void connected() {
                String info = String.format("successfully connected to appliance vm[uuid:%s, name:%s, ip:%s], deploying agent now ...", spec.getVmInventory().getUuid(), spec.getVmInventory().getName(), mgmtIp);
                logger.debug(info);
                chain.next();
            }

            private void sshLogIn() throws InterruptedException {
                long expired = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(ApplianceVmGlobalConfig.SSH_LOGIN_TIMEOUT.value(Long.class));
                SshException se = null;
                while (true) {
                   try {
                       new Ssh().setHostname(mgmtIp).setUsername(username).setPrivateKey(privKey).setPort(sshPort).setSuppressException(!connectVerbose)
                               .command("echo 'hello'").setTimeout(60).runErrorByExceptionAndClose();
                       return;
                   } catch (SshException e) {
                       se = e;

                       if (System.currentTimeMillis() > expired) {
                           break;
                       }

                       TimeUnit.SECONDS.sleep(1);
                   }
                }

                throw se;
            }

            @Override
            public boolean run() {
                try {
                    if (!NetworkUtils.isRemotePortOpen(mgmtIp, sshPort, 5)) {
                        return countDown("");
                    } else {
                        sshLogIn();
                        checkError();
                        connected();
                        return true;
                    }
                } catch (Throwable e1) {
                    logger.warn(e1.getMessage(), e1);
                    ErrorCode err = e1 instanceof OperationFailureException ? ((OperationFailureException)e1).getErrorCode() : err(ApplianceVmErrors.UNABLE_TO_START, e1.getMessage());
                    chain.fail(err);
                    Thread.currentThread().interrupt();
                    return true;
                }
            }

            private void checkError() {
                SshResult ret = new Ssh().setHostname(mgmtIp).setUsername(username).setPrivateKey(privKey).setPort(sshPort)
                        .command(String.format("if [ -f %s ]; then cat %s; exit 1; else exit 0; fi", ERROR_LOG_PATH, ERROR_LOG_PATH)).setTimeout(60).runAndClose();
                if (ret.getReturnCode() != 0) {
                    throw new OperationFailureException(err(ApplianceVmErrors.UNABLE_TO_START, ret.getStdout()));
                }
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return interval;
            }

            @Override
            public String getName() {
                return "connect-appliance-vm-" + spec.getVmInventory().getUuid();
            }
        });
    }
}
