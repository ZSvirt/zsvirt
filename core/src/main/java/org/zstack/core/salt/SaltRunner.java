package org.zstack.core.salt;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.job.JobQueueFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.DebugUtils.Assert;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SaltRunner {
    private static final CLogger logger = Utils.getLogger(SaltRunner.class);
    private static Map<String, String> minionIds = new HashMap<String, String>();

    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private GlobalConfigFacade gcf;
    @Autowired
    private JobQueueFacade jobf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private SaltFacade saltf;

    private String saltBootstrapScriptPath;
    private String saltMinionConfPath;

    private String minionId;
    private String targetIp;
    private String privateKey;
    private String password;
    private String username;
    private int sshPort = 22;
    private boolean cleanupMasterPubKey = true;
    private String stateName;
    private boolean fullDeploy;
    private int agentPort;
    private String moduleName;


    public SaltRunner(String saltBootstrapScriptPath, String saltMinionConfPath) {
        this.saltBootstrapScriptPath = saltBootstrapScriptPath;
        this.saltMinionConfPath = saltMinionConfPath;
    }

    public String getModuleName() {
        return moduleName;
    }

    public SaltRunner setModuleName(String moduleName) {
        this.moduleName = moduleName;
        return this;
    }

    public String getMinionId() {
        return minionId;
    }

    public SaltRunner setMinionId(String minionId) {
        this.minionId = minionId;
        return this;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public SaltRunner setTargetIp(String targetIp) {
        this.targetIp = targetIp;
        return this;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public SaltRunner setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public SaltRunner setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public SaltRunner setUsername(String username) {
        this.username = username;
        return this;
    }

    public boolean isCleanupMasterPubKey() {
        return cleanupMasterPubKey;
    }

    public SaltRunner setCleanupMasterPubKey(boolean cleanupMasterPubKey) {
        this.cleanupMasterPubKey = cleanupMasterPubKey;
        return this;
    }

    private void generateMinionId() {
        minionId = minionIds.get(targetIp);
        if (minionId != null) {
            return;
        }

        SshResult machineIdRes = new Ssh().setHostname(targetIp).setUsername(username).setPrivateKey(privateKey)
                .setPassword(password).setPort(sshPort).command("cat /sys/class/dmi/id/product_uuid").runAndClose();
        machineIdRes.raiseExceptionIfFailed();
        minionId = machineIdRes.getStdout().trim();
        minionIds.put(targetIp, minionId);
    }

    private boolean needSetupMinion() {
        if (agentPort != 0) {
            boolean opened = NetworkUtils.isRemotePortOpen(targetIp, agentPort, (int)TimeUnit.SECONDS.toMillis(5));
            if (!opened) {
                logger.debug(String.format("agent port[%s] on target ip[%s] is not opened, run salt[%s]", agentPort, targetIp, moduleName));
                return true;
            }

            boolean changed = saltf.isModuleChanged(moduleName);
            if (changed) {
                logger.debug(String.format("salt module[%s] changed, run salt", moduleName));
                return true;
            }

            logger.debug(String.format("agent port[%s] on target ip[%s] is opened, salt module[%s] is not changed, skip to run salt", agentPort, targetIp, moduleName));
            return false;
        }

        return true;
    }

    private void setupMinion(final Completion completion) {
        final SaltSetupMinionJob job = new SaltSetupMinionJob();
        if (minionId == null)  {
            generateMinionId();
        }

        Assert(minionId != null, "minionId can not be null");

        job.setPrivateKey(privateKey);
        job.setPassword(password);
        job.setPort(sshPort);
        job.setTargetIp(targetIp);
        job.setUsername(username);
        job.setSaltBootstrapScriptPath(saltBootstrapScriptPath);
        job.setSaltMinionConfPath(saltMinionConfPath);
        job.setMinionId(minionId);
        job.setCleanMasterKey(cleanupMasterPubKey);

        boolean useJob = SaltGlobalConfig.SETUP_MINION_IN_JOB.value(Boolean.class);
        if (useJob) {
            jobf.execute(String.format("setup-minion-%s", minionId), Platform.getManagementServerId(), job, completion);
        } else {
            thdf.chainSubmit(new ChainTask(completion) {
                @Override
                public String getSyncSignature() {
                    return minionId;
                }

                @Override
                public void run(final SyncTaskChain chain) {
                    job.run(new ReturnValueCompletion<Object>(chain) {
                        @Override
                        public void success(Object returnValue) {
                            completion.success();
                            chain.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
                            chain.next();
                        }
                    });
                }

                @Override
                public String getName() {
                    return String.format("setup-minion-for-%s", targetIp);
                }
            });
        }
    }

    private boolean doRunState() {
        StringBuilder sb = new StringBuilder(String.format("/usr/bin/salt --out=json '%s' state.sls %s queue=True", minionId, stateName));
        boolean alwaysFullDeploy = Boolean.valueOf(System.getProperty("SaltFacade.alwaysFullDeploy"));
        if (alwaysFullDeploy ? alwaysFullDeploy : fullDeploy) {
            sb.append(String.format(" pillar=\"{'pkg':True}\""));
        }
        String cmd = sb.toString();
        ShellResult res = ShellUtils.runAndReturn(cmd);

        if ("".equals(res.getStdout()) || res.isReturnCode(2)) {
            sb = new StringBuilder(String.format("\nfailed to apply salt state[minion id:%s, state name:%s]", minionId, stateName));
            sb.append(String.format("\ncommand: %s", cmd));
            sb.append(String.format("\nno json output from the command, it's probably caused by the minion hasn't connected to master, will retry"));
            logger.debug(sb.toString());
            return false;
        }

        res.raiseExceptionIfFail();

        HashMap output = JSONObjectUtil.toObject(res.getStdout(), HashMap.class);
        for (Object val : output.values()) {
            if (val instanceof List) {
                sb = new StringBuilder(String.format("\nfailed to apply salt state[minion id:%s, state name:%s]", minionId, stateName));
                sb.append(String.format("\ncommand: %s", cmd));
                sb.append(String.format("\n%s", res.getStdout()));
                throw new SaltException(sb.toString());
            }

            Map m = (Map)val;
            for (Object v2 : m.values()) {
                Map m2 = (Map)v2;
                Boolean r = (Boolean) m2.get("result");
                if (!r) {
                    sb = new StringBuilder(String.format("\nfailed to apply salt state[minion id:%s, state name:%s]", minionId, stateName));
                    sb.append(String.format("\ncommand: %s", cmd));
                    sb.append(String.format("\n%s", res.getStdout()));
                    throw new SaltException(sb.toString());
                }
            }
        }

        return true;
    }

    public void run(final Completion completion) {
        if (!needSetupMinion()) {
            completion.success();
            return;
        }

        setupMinion(new Completion(completion) {
            @Override
            public void success() {
                try {
                    // a close runState after setting up minion likely fails because the minion hasn't connected to master
                    // we retry to work around this problem
                    int retry = SaltGlobalConfig.SETUP_MINION_RETRY.value(Integer.class);
                    int interval = SaltGlobalConfig.SETUP_MINION_RETRY_INTERVAL.value(Integer.class);
                    boolean ret = false;
                    for (int i=0; i<retry; i++) {
                        ret = doRunState();
                        if (ret) {
                            break;
                        }

                        try {
                            TimeUnit.SECONDS.sleep(interval);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new CloudRuntimeException(e);
                        }
                    }

                    if (!ret) {
                        completion.fail(operr("failed to run salt state[%s] on system[%s], failed after %s retries", stateName, targetIp, retry));
                        return;
                    }

                    logger.debug(String.format("successfully run salt state[%s] on system[%s]", stateName, targetIp));
                    completion.success();
                } catch (Exception e) {
                    logger.warn(String.format("failed to run salt state[%s] on system[%s], %s", stateName, targetIp, e.getMessage()));
                    completion.fail(operr(e.getMessage()));
                    Thread.currentThread().interrupt();
                }
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to setup salt minion for state[%s] on system[%s], %s", stateName, targetIp, errorCode));
                completion.fail(errorCode);
            }
        });
    }

    public int getAgentPort() {
        return agentPort;
    }

    public SaltRunner setAgentPort(int agentPort) {
        this.agentPort = agentPort;
        return this;
    }

    public int getSshPort() {
        return sshPort;
    }

    public SaltRunner setSshPort(int sshPort) {
        this.sshPort = sshPort;
        return this;
    }

    public String getStateName() {
        return stateName;
    }

    public SaltRunner setStateName(String stateName) {
        this.stateName = stateName;
        return this;
    }

    public boolean isFullDeploy() {
        return fullDeploy;
    }

    public SaltRunner setFullDeploy(boolean fullDeploy) {
        this.fullDeploy = fullDeploy;
        return this;
    }
}
