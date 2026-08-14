package org.zstack.core.salt;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.job.Job;
import org.zstack.core.job.JobContext;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.utils.Utils;
import org.zstack.utils.data.StringTemplate;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshException;
import org.zstack.utils.ssh.SshResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.zstack.core.Platform.inerr;
import static org.zstack.core.Platform.operr;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SaltSetupMinionJob implements Job {
    private static final CLogger logger = Utils.getLogger(SaltSetupMinionJob.class);

    @JobContext
    private String targetIp;
    @JobContext
    private String username;
    @JobContext
    private String privateKey;
    @JobContext
    private String password;
    @JobContext
    private int port;
    @JobContext
    private String saltBootstrapScriptPath;
    @JobContext
    private String saltMinionConfPath;
    @JobContext
    private String minionId;
    @JobContext
    private boolean cleanMasterKey;

    @Autowired
    private ErrorFacade errf;

    private static final String SALT_BOOTSTRAP = "salt/salt-bootstrap.sh";

    private File rewriteMinionConfFile(String minionId) throws IOException {
        File minionConfTmpt = new File(saltMinionConfPath);

        Map<String, String> map = new HashMap<String, String>();
        map.put("managementNodeIp", Platform.getManagementServerIp());
        map.put("minionId", minionId);

        String srcConf = FileUtils.readFileToString(minionConfTmpt);
        String conf = StringTemplate.substitute(srcConf, map);
        File minionConf = File.createTempFile("zstack-salt", "minion");
        FileUtils.write(minionConf, conf);
        return minionConf;
    }

    @Override
    public void run(ReturnValueCompletion<Object> completion) {
        File tmpt = null;
        Ssh ssh = null;
        FileInputStream fis = null;
        try {
            ssh = new Ssh().setHostname(targetIp).setPassword(password).setPrivateKey(privateKey)
                    .setUsername(username).setPort(port);
            SshResult ret = ssh.checkTool("scp").run();
            if (ret.getReturnCode() != 0) {
                completion.fail(operr("scp is not found on system[%s], unable to setup salt", targetIp));
                return;
            }

            ret = ssh.reset().checkTool("salt-minion").run();
            boolean hasMinion = ret.getReturnCode() == 0;

            if (!hasMinion) {
                String dstPath = String.format("/tmp/%s.sh", Platform.getUuid());
                String srcPath = PathUtil.findFileOnClassPath(SALT_BOOTSTRAP, true).getAbsolutePath();
                logger.debug(String.format("salt-minion is not found on system[%s], about to install a new one", targetIp));
                ret = ssh.reset().scpUpload(srcPath, dstPath).command(String.format("sh %s ; ret=$?; rm -f %s; exit $ret", dstPath, dstPath)).run();
                ret.raiseExceptionIfFailed();
                logger.debug(String.format("successfully installed salt-minion on system[%s]", targetIp));
            } else {
                logger.debug(String.format("salt-minion is found on system[%s], no need to install new one", targetIp));
            }

            tmpt = rewriteMinionConfFile(minionId);
            String minionConfPath = PathUtil.join(SaltConstant.SALT_CONF_HOME, SaltConstant.MINION_CONF_NAME);
            boolean deployMinion = false;
            ret = ssh.reset().command(String.format("md5sum %s", minionConfPath)).run();

            if (ret.getReturnCode() != 0) {
                deployMinion = true;
                logger.debug(String.format("cannot get md5 of minion configuration file, need to re-setup minion"));
            } else {
                String dstMd5 = ret.getStdout().split(" ")[0].trim();
                fis = new FileInputStream(tmpt);
                String srcMd5 = DigestUtils.md5Hex(fis);
                deployMinion = !srcMd5.equals(dstMd5);
                if (deployMinion) {
                    logger.debug(String.format("MD5 of minion configuration file changed[%s --> %s], need to re-setup minion", dstMd5, srcMd5));
                } else {
                    logger.debug(String.format("MD5 of minion configuration file not changed, no needs to re-setup minion"));
                }
            }

            ssh.reset();
            if (deployMinion) {
                if (cleanMasterKey) {
                    ssh.command("rm -f /etc/salt/pki/minion/minion_master.pub");
                }
                ret = ssh.scpUpload(tmpt.getAbsolutePath(), minionConfPath)
                        .command("service salt-minion restart").run();
            } else {
                ret = ssh.command("service salt-minion status | grep -- 'running' || service salt-minion start").run();
            }
            ret.raiseExceptionIfFailed();

            logger.debug(String.format("successfully setup salt-minion on target system[%s]", targetIp));
            completion.success(minionId);
        } catch (SshException e) {
            String err = String.format("failed to setup minion on target system[%s], because %s", targetIp, e.getMessage());
            logger.warn(err, e);
            completion.fail(operr(e.getMessage()));
        } catch (IOException ie) {
            String err = String.format("failed to setup minion on target system[%s], because %s", targetIp, ie.getMessage());
            logger.warn(err, ie);
            completion.fail(inerr(ie.getMessage()));
        } finally {
            if (tmpt != null && !tmpt.delete()) {
                logger.warn(String.format("failed to delete file[%s]", tmpt));
            }
            if (ssh != null) {
                ssh.close();
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    logger.warn(String.format("FileInputStream close IOException：%s", e.getMessage()));
                }
            }
        }
    }

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }


    public String getSaltBootstrapScriptPath() {
        return saltBootstrapScriptPath;
    }

    public void setSaltBootstrapScriptPath(String saltBootstrapScriptPath) {
        this.saltBootstrapScriptPath = saltBootstrapScriptPath;
    }

    public String getSaltMinionConfPath() {
        return saltMinionConfPath;
    }

    public void setSaltMinionConfPath(String saltMinionConfPath) {
        this.saltMinionConfPath = saltMinionConfPath;
    }

    public String getMinionId() {
        return minionId;
    }

    public void setMinionId(String minionId) {
        this.minionId = minionId;
    }

    public boolean isCleanMasterKey() {
        return cleanMasterKey;
    }

    public void setCleanMasterKey(boolean cleanMasterKey) {
        this.cleanMasterKey = cleanMasterKey;
    }
}
