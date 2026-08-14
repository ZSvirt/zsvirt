package org.zstack.core.ansible;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshException;
import org.zstack.utils.ssh.SshResult;
import org.zstack.utils.ssh.SshShell;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.StringDSL.ln;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AnsibleRunner {
    private static final CLogger logger = Utils.getLogger(AnsibleRunner.class);

    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private RESTFacade restf;

    private static String privKeyFile;
    private List<AnsibleChecker> checkers = new ArrayList<>();

    static {
        privKeyFile = PathUtil.findFileOnClassPath(AnsibleConstant.RSA_PRIVATE_KEY).getAbsolutePath();
    }

    {
        fullDeploy = AnsibleGlobalProperty.FULL_DEPLOY;
    }

    private String targetIp;
    /** targetUuid is an unique resource for cache (see /usr/local/zstack/ansible/.ansible.cache/$targetUuid/),
     *  it should be a phsical resource,
     *  it it option.
    */
    private String targetUuid;
    private String username;
    private String password;
    private String privateKey;
    private int sshPort = 22;
    private String playBookName;
    private String playBookPath;
    private Map<String, Object> arguments = new HashMap<String, Object>();
    private int agentPort;
    private boolean fullDeploy;
    private boolean localPublicKey;
    private boolean runOnLocal;
    private AnsibleNeedRun ansibleNeedRun;
    private String ansibleExecutable;
    private boolean forceRun;

    private AnsibleBasicArguments deployArguments;

    public String getAnsibleExecutable() {
        return ansibleExecutable;
    }

    public void setAnsibleExecutable(String ansibleExecutable) {
        this.ansibleExecutable = ansibleExecutable;
    }

    public String getPlayBookPath() {
        return playBookPath;
    }

    public void setPlayBookPath(String playBookPath) {
        this.playBookPath = playBookPath;
    }

    public AnsibleNeedRun getAnsibleNeedRun() {
        return ansibleNeedRun;
    }

    public void setAnsibleNeedRun(AnsibleNeedRun ansibleNeedRun) {
        this.ansibleNeedRun = ansibleNeedRun;
    }

    public boolean isRunOnLocal() {
        return runOnLocal;
    }

    public void setRunOnLocal(boolean runOnLocal) {
        this.runOnLocal = runOnLocal;
    }

    public boolean isLocalPublicKey() {
        return localPublicKey;
    }

    public void setLocalPublicKey(boolean localPublicKey) {
        this.localPublicKey = localPublicKey;
    }

    public void putArgument(String key, Object value) {
        arguments.put(key, value);
    }

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public String getTargetUuid() {
        return targetUuid;
    }

    public void setTargetUuid(String targetUuid) {
        this.targetUuid = targetUuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public void useDefaultPrivateKey() {
        setPrivateKey(asf.getPrivateKey());
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    public String getPlayBookName() {
        return playBookName;
    }

    public void setPlayBookName(String playBookName) {
        this.playBookName = playBookName;
    }

    public AnsibleBasicArguments getDeployArguments() {
        return deployArguments;
    }

    public void setDeployArguments(AnsibleBasicArguments deployArguments) {
        this.deployArguments = deployArguments;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    public int getAgentPort() {
        return agentPort;
    }

    public void setAgentPort(int agentPort) {
        this.agentPort = agentPort;
    }

    public boolean isFullDeploy() {
        return fullDeploy;
    }

    public void setFullDeploy(boolean fullDeploy) {
        this.fullDeploy = fullDeploy;
    }

    public boolean isForceRun() {
        return forceRun;
    }

    public void setForceRun(boolean forceRun) {
        this.forceRun = forceRun;
    }

    private void setupPublicKey() throws IOException {
        File pubKeyFile = PathUtil.findFileOnClassPath(AnsibleConstant.RSA_PUBLIC_KEY);
        String script = PathUtil.findFileOnClassPath(AnsibleConstant.IMPORT_PUBLIC_KEY_SCRIPT_PATH, true).getAbsolutePath();
        if (localPublicKey) {
            ShellUtils.run(String.format("sh %s %s", script, pubKeyFile.getAbsolutePath()));
        } else {
            setupPublicKeyOnRemote();
        }
    }

    private void setupPublicKeyOnRemote() {
        String script = ln(
                "#!/bin/sh",
                "if [ ! -d ~/.ssh ]; then",
                "mkdir -p ~/.ssh",
                "chmod 700 ~/.ssh",
                "fi",
                "if [ ! -f ~/.ssh/authorized_keys ]; then",
                "touch ~/.ssh/authorized_keys",
                "chmod 600 ~/.ssh/authorized_keys",
                "fi",
                "pub_key='{pubkey}'",
                "grep \"$pub_key\" ~/.ssh/authorized_keys > /dev/null",
                "if [ $? -eq 1 ]; then",
                "echo \"$pub_key\" >> ~/.ssh/authorized_keys",
                "fi",
                "if [ -x /sbin/restorecon ]; then",
                "/sbin/restorecon ~/.ssh ~/.ssh/authorized_keys",
                "fi",
                "exit 0"
        ).formatByMap(map(
                e("pubkey", asf.getPublicKey())
        ));

        SshShell ssh = new SshShell();
        ssh.setHostname(targetIp);
        ssh.setPassword(password);
        ssh.setPort(sshPort);
        ssh.setUsername(username);

        if (privateKey != null) {
            ssh.setPrivateKey(privateKey);
        }

        SshResult res = ssh.runScript(script);
        res.raiseExceptionIfFailed();
    }

    private void callAnsible(final ReturnValueCompletion<Boolean> completion) {
        RunAnsibleMsg msg = new RunAnsibleMsg();
        msg.setTargetIp(targetIp);
        msg.setTargetUuid(targetUuid);
        msg.setPrivateKeyFile(privKeyFile);
        msg.setArguments(arguments);
        msg.setAnsibleExecutable(ansibleExecutable);
        msg.setDeployArguments(deployArguments);

        if (playBookPath != null) {
            msg.setPlayBookPath(playBookPath);
        } else {
            msg.setPlayBookPath(PathUtil.join(AnsibleConstant.ROOT_DIR, playBookName));
        }

        if (runOnLocal) {
            bus.makeLocalServiceId(msg, AnsibleConstant.SERVICE_ID);
        } else {
            bus.makeTargetServiceIdByResourceUuid(msg, AnsibleConstant.SERVICE_ID, targetIp);
        }

        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success(true);
                } else {
                    cleanup();
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private boolean runChecker() {
        for (AnsibleChecker checker : checkers) {
            ErrorCode err = checker.stopAnsible();
            if (err != null) {
                throw new OperationFailureException(err);
            }

            if (checker.needDeploy()) {
                logger.debug(String.format("checker[%s] reports deploy is needed", checker.getClass()));
                return true;
            }
        }

        return false;
    }

    private boolean isNeedRun() {
        List<String> ignoreAgentPortModule = new ArrayList<String>();
        ignoreAgentPortModule.add("imagestorebackupstorage.py");
        if (isFullDeploy()) {
            logger.debug("Ansible.fullDeploy is set, run ansible anyway");
            return true;
        }

        if (ansibleNeedRun != null) {
            return ansibleNeedRun.isRunNeed();
        }

        boolean changed = asf.isModuleChanged(playBookName);
        if (changed) {
            logger.debug(String.format("ansible module[%s] changed, run ansible", playBookName));
            return true;
        }

        if (agentPort != 0) {
            boolean opened = NetworkUtils.isRemotePortOpen(targetIp, agentPort, (int) TimeUnit.SECONDS.toMillis(5));
            if (!opened) {
                logger.debug(String.format("agent port[%s] on target ip[%s] is not opened, run ansible[%s]", agentPort, targetIp, playBookName));
                return true;
            }

            if (runChecker()) {
                return true;
            }

            logger.debug(String.format("agent port[%s] on target ip[%s] is opened, ansible module[%s] is not changed, skip to run ansible", agentPort, targetIp, playBookName));
            return false;
        } else if ( ignoreAgentPortModule.contains(playBookName) ) {
            logger.debug(String.format("module %s will not check agent port, only check md5sum", playBookName));
            if (runChecker()) {
                logger.debug(String.format("module %s md5sum changed, run ansible", playBookName));
                return true;
            } else {
                logger.debug(String.format("module %s md5sum not change, skip to run ansible", playBookName));
                return false;
            }
        }

        logger.debug("agent port is not set, run ansible anyway");
        return true;
    }

    private void cleanup() {
        // deleting source files. Then next time ansible is called, AnsibleChecker returns false that lets ansible run
        for (AnsibleChecker checker : checkers) {
            checker.deleteDestFile();
        }
    }

    public void run(ReturnValueCompletion<Boolean> completion) {
        try {
            if (!forceRun && !isNeedRun()) {
                completion.success(false);
                return;
            }

            int port = new URI(restf.getBaseUrl()).getPort();

            if (deployArguments == null) {
                deployArguments = new AnsibleBasicArguments();
            }

            deployArguments.setPipUrl(String.format("http://%s:%d/zstack/static/pypi/simple", restf.getHostName(), port));
            deployArguments.setTrustedHost(restf.getHostName());
            deployArguments.setYumServer(String.format("%s:%d", restf.getHostName(), port));
            deployArguments.setRemoteUser(username);
            if (password != null && !password.isEmpty()) {
                deployArguments.setRemotePass(password);
            }
            deployArguments.setRemotePort(Integer.toString(sshPort));

            logger.debug(String.format("starts to run ansible[%s]", playBookPath == null ? playBookName : playBookPath));
            new PrepareAnsible().setTargetIp(targetIp).prepare();

            if (!CoreGlobalProperty.UNIT_TEST_ON) {
                setupPublicKey();
            }

            callAnsible(completion);
        } catch (SshException e) {
            throw new OperationFailureException(operr("User name or password or port number may be problematic"));
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }

    }


    public void restartAgent(String agentName, Completion completion) {
        String script = String.format("sudo bash /etc/init.d/%s restart\n", agentName);

        if (CoreGlobalProperty.UNIT_TEST_ON) {
            logger.debug("skip restartAgent as it's unittest");
        } else {
            new Ssh().shell(script).setTimeout(60).setPrivateKey(asf.getPrivateKey()).setUsername(username).setHostname(targetIp)
                    .setPort(sshPort).setPassword(password).runErrorByExceptionAndClose();
        }
        completion.success();
    }

    public List<AnsibleChecker> getCheckers() {
        return checkers;
    }

    public void installChecker(AnsibleChecker checker) {
        checkers.add(checker);
    }
}
