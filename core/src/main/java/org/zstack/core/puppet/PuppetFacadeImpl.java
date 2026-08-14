package org.zstack.core.puppet;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.job.JobQueueFacade;
import org.zstack.core.puppet.PuppetConstant.PuppetGlobalConfig;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;

public class PuppetFacadeImpl extends AbstractService implements PuppetFacade {
    private static final CLogger logger = Utils.getLogger(PuppetFacadeImpl.class);

    private String puppetModulesHome = "/etc/puppet/modules";
    private String puppetNodesHome = "/etc/puppet/manifests/nodes";
    private String puppetSitepp = "/etc/puppet/manifests/site.pp";
    private String puppetAutoSign = "/etc/puppet/autosign.conf";
    private String puppetMasterCertName;
    private String COMMON_MODULE_PATH = String.format("%s/puppet/commonModules", Platform.COMPONENT_CLASSPATH_HOME);
    private String hostname;

    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CloudBus bus;
    @Autowired
    protected JobQueueFacade jobf;
    @Autowired
    protected GlobalConfigFacade gcf;
    
    ////////////////////////////// For unit test //////////////////////////////////
    private boolean createPuppetFolder = true;
    ///////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("rawtypes")
    public class ModuleWalker extends DirectoryWalker {
        @Override
        protected void handleFile(File file, int depth, Collection results) {
            results.add(file);
        }

        public void doWalk(File start, Collection result) throws IOException {
            walk(start, result);
        }
    }

    private boolean isNeedToDeploy(String moduleName, String modulePath) throws IOException {
        String destModulePath = PathUtil.join(puppetModulesHome, moduleName);
        File dest = new File(destModulePath);
        if (!dest.exists()) {
            logger.debug(String.format("%s is not existing, need to deploy puppet module[%s]", destModulePath, moduleName));
            return true;
        }
        List<File> destFiles = new ArrayList<File>(20);
        ModuleWalker walker = new ModuleWalker();
        walker.doWalk(dest, destFiles);

        List<File> srcFiles = new ArrayList<File>(20);
        walker = new ModuleWalker();
        File src = new File(modulePath);
        walker.doWalk(src, srcFiles);
        if (srcFiles.size() != destFiles.size()) {
            logger.debug(String.format("%s has %s files, %s has %s files, need to deploy puppet module[%s]", destModulePath, destFiles.size(), modulePath,
                    srcFiles.size(), moduleName));
            return true;
        }

        Map<String, String> srcMd5sum = new HashMap<String, String>(srcFiles.size());
        for (File f : srcFiles) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(f);
                String md5 = DigestUtils.md5Hex(fis);
                srcMd5sum.put(f.getName(), md5);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        logger.warn(String.format("FileInputStream close IOException：%s", e.getMessage()));
                    }
                }
            }
        }
        Map<String, String> destMd5sum = new HashMap<String, String>(destFiles.size());
        for (File f : destFiles) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(f);
                String md5 = DigestUtils.md5Hex(fis);
                destMd5sum.put(f.getName(), md5);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        logger.warn(String.format("FileInputStream close IOException：%s", e.getMessage()));
                    }
                }
            }
        }
        for (Map.Entry<String, String> srcEntry : srcMd5sum.entrySet()) {
            String name = srcEntry.getKey();
            String destMd5 = destMd5sum.get(name);
            if (destMd5 == null) {
                logger.debug(String.format("%s is not existing in %s, need to deploy puppet module[%s]", name, destModulePath, moduleName));
                return true;
            }
            if (!destMd5.equals(srcEntry.getValue())) {
                logger.debug(String.format("%s's md5 changed[{%s} in %s, {%s} in %s], need to deploy puppet module[%s]", name, destMd5, destModulePath,
                        srcEntry.getValue(), modulePath, moduleName));
                return true;
            }
        }
        logger.debug(String.format("no file changed in puppet module[%s], no need to deploy", moduleName));
        return false;
    }

    public void setPuppetModulesHome(String puppetModulesHome) {
        this.puppetModulesHome = puppetModulesHome;
    }

    public void setPuppetMasterCertName(String puppetMasterCertName) {
        this.puppetMasterCertName = puppetMasterCertName;
    }
    

    private void deployModule(String modulePath) {
        File src = new File(modulePath);
        if (!src.isDirectory()) {
            throw new PuppetException(String.format("Cannot find puppet module[%s], it's either not existing or not a directory", modulePath));
        }

        String moduleName = src.getName();
        if (moduleName == null) {
            throw new PuppetException(String.format("Cannot get puppet module name from path[%s]", modulePath));
        }

        try {
            if (!isNeedToDeploy(moduleName, modulePath)) {
                return;
            }

            String destModulePath = PathUtil.join(puppetModulesHome, moduleName);
            File dest = new File(destModulePath);
            if (dest.exists()) {
                FileUtils.forceDelete(dest);
                FileUtils.forceMkdir(dest);
            }
            FileUtils.copyDirectory(src, dest);
        } catch (Exception e) {
            String err = String.format("Unable to deploy puppet module[%s] from %s", moduleName, modulePath);
            throw new PuppetException(err, e);
        }
    }

    @Override
    public void deployModule(String nodeFileName, String nodeExpression, String modulePath) {
        deployModule(modulePath);
        File nodeFile = new File(PathUtil.join(puppetNodesHome, nodeFileName));
        try {
            FileUtils.writeStringToFile(nodeFile, nodeExpression);
        } catch (IOException e) {
            String err = String.format("Unable to deploy puppet module from %s", modulePath);
            throw new PuppetException(err, e);
        }
    }

    public void pokePuppetAgent(final PuppetPokeAgentMsg msg) throws UnknownHostException {
        final String targetIp = InetAddress.getByName(msg.getHostname()).getHostAddress();

        thdf.syncSubmit(new SyncTask<Void>() {
            @Override
            public String getName() {
                return "poke-puppet-agent";
            }

            @Override
            public Void call() throws Exception {
                /*
                PuppetPokeAgentReply reply = new PuppetPokeAgentReply();
                try {
                    int port = 22;
                    if (msg.getSshPort() != null) {
                    	port = msg.getSshPort();
                    }
                    // get management server ip from SSH_CLIENT env variable after ssh log into target system, then mapping this ip to hostname 'puppet' in /etc/hosts
                    String cmd = String.mediaType("ip=`env | grep SSH_CLIENT | cut -d '=' -f 2 | cut -d ' ' -f 1`; sed -i \"/%s/d\" /etc/hosts; echo \"$ip %s\" >> /etc/hosts", hostname, hostname);
                    Ssh.run(msg.getHostname(), port, msg.getUsername(), msg.getPassword(), cmd);
                    cmd = String.mediaType("puppet agent --certname %s --no-daemonize --onetime --waitforcert 60 --server %s --verbose --detailed-exitcodes", msg.getNodeName(), hostname);
                    SshResult ret = Ssh.run2(msg.getHostname(), msg.getUsername(), msg.getPassword(), port, cmd);
                    StringBuilder sb = new StringBuilder(ret.getCommandToExecute()).append("\n");
                    sb.append(String.mediaType("return code: %s", ret.getReturnCode()));
                    sb.append(String.mediaType("stdout: %s\n", ret.getStdout()));
                    sb.append(String.mediaType("stderr: %s\n", ret.getStderr()));
                    sb.append(String.mediaType("exitErrorMessage: %s\n", ret.getExitErrorMessage()));
                    if (ret.getReturnCode() == 4 || ret.getReturnCode() == 6 || ret.getReturnCode() == 1) {
                    	//ErrorCodeFacade.setErrorToMessageReply(PuppetErrorCodes.FAILS_TO_RUN_PUPPET_AGENT.toString(), sb.toString(), reply);
                    } else {
                    	logger.debug(sb.toString());
                    }
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                    //ErrorCodeFacade.setErrorToMessageReply(PuppetErrorCodes.FAILS_TO_RUN_PUPPET_AGENT.toString(), e.getMessage(), reply);
                }

                bus.reply(msg, reply);
                */
                return null;
            }

            @Override
            public String getSyncSignature() {
                return targetIp;
            }

            @Override
            public int getSyncLevel() {
                return 1;
            }

        });
    }

    public void setPuppetNodesHome(String puppetNodesHome) {
        this.puppetNodesHome = puppetNodesHome;
    }

    private void deployCommonModules() {
        URL commonModulesUrl = this.getClass().getClassLoader().getResource(COMMON_MODULE_PATH);
        if (commonModulesUrl == null) {
            logger.warn(String.format("Cannot find %s in classpath, it should be at least an empty directory", COMMON_MODULE_PATH));
            return;
        }

        File modules = new File(commonModulesUrl.getPath());
        for (File f : modules.listFiles()) {
            if (f.isDirectory()) {
                deployModule(f.getAbsolutePath());
            }
        }
    }
    
	private void preparePuppet() {
		File nodeHome = new File(puppetNodesHome);
		if (!nodeHome.exists()) {
			nodeHome.mkdirs();
		}

		try {
			File sitepp = new File(puppetSitepp);
			if (!sitepp.exists()) {
				FileUtils.writeStringToFile(sitepp, "import 'nodes/*.pp'");
			}
			
			File autoSign = new File(puppetAutoSign);
			if (!autoSign.exists()) {
				FileUtils.writeStringToFile(autoSign, "*");
			}
		} catch (Exception e) {
			throw new CloudRuntimeException(e);
		}
	}

    @Override
    public boolean start() {
        try {
            hostname = java.net.InetAddress.getLocalHost().getHostName();
            logger.debug(String.format("get hostname as %s", hostname));
        } catch (Exception e) {
    		throw new CloudRuntimeException("unable to get hostname of management server", e);
        }
        
        if (createPuppetFolder) {
            preparePuppet();
        }

    	bus.registerService(this);
    	deployCommonModules();
    	return true;
    }

    @Override
    public boolean stop() {
        bus.unregisterService(this);
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg instanceof PuppetPokeAgentMsg) {
                handle((PuppetPokeAgentMsg) msg);
            } else {
                bus.dealWithUnknownMessage(msg);
            }
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg, e);
        }
    }

    private void handle(PuppetPokeAgentMsg msg) throws UnknownHostException {


        if (true) {
            pokePuppetAgent(msg);
        } else {
            PokePuppetAgentJob job = new PokePuppetAgentJob(msg, puppetMasterCertName);
            jobf.execute("puppet-" + msg.getHostname(), Platform.getManagementServerId(), job);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(PuppetConstant.SERVICE_ID);
    }

    public void setCreatePuppetFolder(boolean createPuppetFolder) {
        this.createPuppetFolder = createPuppetFolder;
    }
}
