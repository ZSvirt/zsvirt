package org.zstack.core.salt;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.header.AbstractService;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.IptablesUtils;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.StringTemplate;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 */
public class SaltFacadeImpl extends AbstractService implements SaltFacade {
    private static final CLogger logger = Utils.getLogger(SaltFacadeImpl.class);

    @Autowired
    private CloudBus bus;

    private int masterMaxOpenFiles = 1024;
    private int masterWorkerThreads = 10;
    private String masterFileRoots = "/srv/salt";

    private String saltBootstrapScriptPath;
    private String saltMinionConfPath;
    private Map<String, Boolean> moduleChanges = new HashMap<String, Boolean>();

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SaltConstant.SERVICE_ID);
    }

    private boolean deployFile(File src, File dst) throws IOException {
        if (dst.exists() && PathUtil.compareFileByMd5(src, dst)) {
            logger.debug(String.format("MD5 of src file[%s] and dst file[%s] are the same, no need to deploy", src.getAbsolutePath(), dst.getAbsolutePath()));
            return false;
        }

        FileUtils.copyFile(src, dst);
        logger.debug(String.format("deployed src file[%s] to dst file[%s]", src.getAbsolutePath(), dst.getAbsolutePath()));
        return true;
    }

    private File rewriteMasterConfFile() throws IOException {
        File masterConfTmpt = PathUtil.findFileOnClassPath(PathUtil.join("salt", SaltConstant.MASTER_CONF_NAME), true);

        Map<String, String> map = new HashMap<String, String>();
        map.put("maxOpenFiles", String.valueOf(masterMaxOpenFiles));
        map.put("workerThreads", String.valueOf(masterWorkerThreads));

        String srcConf = FileUtils.readFileToString(masterConfTmpt);
        String conf = StringTemplate.substitute(srcConf, map);
        File masterConf = File.createTempFile("zstack-salt", "master");
        FileUtils.write(masterConf, conf);

        return masterConf;
    }



    private void prepareSaltMaster() throws IOException {
        File srcMasterConf = rewriteMasterConfFile();
        try {
            IptablesUtils.insertRuleToFilterTable("-A INPUT -p tcp -m state --state NEW -m tcp --dport 4505 -j ACCEPT");
            IptablesUtils.insertRuleToFilterTable("-A INPUT -p tcp -m state --state NEW -m tcp --dport 4506 -j ACCEPT");

            File dstMasterConf = new File(PathUtil.join(SaltConstant.SALT_CONF_HOME, SaltConstant.MASTER_CONF_NAME));
            if (deployFile(srcMasterConf, dstMasterConf)) {
                ShellUtils.run("service salt-master restart");
            }
        } finally {
            if (!srcMasterConf.delete()) {
                logger.warn(String.format("failed to delete file[%s]", srcMasterConf));
            }
        }
    }

    private void deployCommonStates() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        deployModule("salt/zstacklib");
    }

    private void prepareJinjaVariables() throws IOException {
        Properties props = System.getProperties();
        Map<String, String> envVars = new HashMap<String, String>();
        Map<String, String> vars = new HashMap<String, String>();
        for (String key : props.stringPropertyNames()) {
            String val = props.getProperty(key);
            if (key.startsWith("salt.env.")) {
                String[] tuples = key.split("\\.");
                if (tuples.length != 3) {
                    throw new IllegalArgumentException(String.format("invalid salt environment variable[%s], it must be in form of 'salt.env.variableName'", key));
                }
                String envName = tuples[2];
                envVars.put(envName, val);
            } else if (key.startsWith("salt.var.")) {
                String[] tuples = key.split("\\.");
                if (tuples.length != 3) {
                    throw new IllegalArgumentException(String.format("invalid salt variable[%s], it must be in form of 'salt.var.variableName'", key));
                }
                String varName = tuples[2];
                vars.put(varName, val);
            }
        }

        String varModulePath = PathUtil.join(masterFileRoots, "variables");
        File varModule = new File(varModulePath);
        if (!varModule.exists()) {
            varModule.mkdirs();
        }

        StringBuilder sb = new StringBuilder();
        if (!envVars.isEmpty()) {
            List<String> dicts = new ArrayList<String>();
            for (Map.Entry<String, String> e : envVars.entrySet()) {
                dicts.add(String.format("'%s':'%s'", e.getKey(), e.getValue()));
            }
            sb.append(String.format("{%% set cmd_env = {%s} %%}\n", StringUtils.join(dicts, ",")));
        }

        if (!vars.isEmpty()) {
            for (Map.Entry<String, String> e : vars.entrySet()) {
                sb.append(String.format("{%% set %s = '%s' %%}\n", e.getKey(), e.getValue()));
            }
        }

        File varFile = new File(PathUtil.join(varModulePath, "var.sls"));
        FileUtils.writeStringToFile(varFile, sb.toString());
    }

    @Override
    public boolean start() {
        try {
            prepareSaltMaster();
            prepareJinjaVariables();
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }

        saltBootstrapScriptPath = PathUtil.findFileOnClassPath("salt/salt-bootstrap.sh", true).getAbsolutePath();
        saltMinionConfPath = PathUtil.findFileOnClassPath(String.format("salt/%s", SaltConstant.MINION_CONF_NAME), true).getAbsolutePath();

        deployCommonStates();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public void setMasterMaxOpenFiles(int masterMaxOpenFiles) {
        this.masterMaxOpenFiles = masterMaxOpenFiles;
    }

    public void setMasterWorkerThreads(int masterWorkerThreads) {
        this.masterWorkerThreads = masterWorkerThreads;
    }

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
        boolean needed = false;

        try {
            String destModulePath = PathUtil.join(this.masterFileRoots, moduleName);
            File dest = new File(destModulePath);
            if (!dest.exists()) {
                logger.debug(String.format("%s is not existing, need to deploy salt module[%s]", destModulePath, moduleName));
                needed = true;
                return needed;
            }
            List<File> destFiles = new ArrayList<File>(20);
            ModuleWalker walker = new ModuleWalker();
            walker.doWalk(dest, destFiles);

            List<File> srcFiles = new ArrayList<File>(20);
            walker = new ModuleWalker();
            File src = new File(modulePath);
            walker.doWalk(src, srcFiles);
            if (srcFiles.size() != destFiles.size()) {
                logger.debug(String.format("%s has %s files, %s has %s files, need to deploy salt module[%s]", destModulePath, destFiles.size(), modulePath,
                        srcFiles.size(), moduleName));
                needed = true;
                return needed;
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
                    logger.debug(String.format("%s is not existing in %s, need to deploy salt module[%s]", name, destModulePath, moduleName));
                    needed = true;
                    return needed;
                }
                if (!destMd5.equals(srcEntry.getValue())) {
                    logger.debug(String.format("%s's md5 changed[{%s} in %s, {%s} in %s], need to deploy salt module[%s]", name, destMd5, destModulePath,
                            srcEntry.getValue(), modulePath, moduleName));
                    needed = true;
                    return needed;
                }
            }
            logger.debug(String.format("no file changed in puppet module[%s], no need to deploy", moduleName));
            needed = false;
            return needed;
        }  finally {
            moduleChanges.put(moduleName, needed);
        }
    }

    @Override
    public void deployModule(String modulePath) {
        File src = PathUtil.findFolderOnClassPath(modulePath, true);
        if (!src.isDirectory()) {
            throw new CloudRuntimeException(String.format("Cannot find salt module[%s], it's either not existing or not a directory", modulePath));
        }

        String moduleName = src.getName();
        if (moduleName == null) {
            throw new CloudRuntimeException(String.format("Cannot get salt module name from path[%s]", modulePath));
        }

        try {
            if (!isNeedToDeploy(moduleName, src.getAbsolutePath())) {
                return;
            }

            String destModulePath = PathUtil.join(masterFileRoots, moduleName);
            File dest = new File(destModulePath);
            if (dest.exists()) {
                FileUtils.forceDelete(dest);
                FileUtils.forceMkdir(dest);
            }
            FileUtils.copyDirectory(src, dest);
        } catch (Exception e) {
            String err = String.format("Unable to deploy salt module[%s] from %s", moduleName, modulePath);
            throw new CloudRuntimeException(err, e);
        }
    }

    @Override
    public SaltRunner createSaltRunner() {
        return new SaltRunner(saltBootstrapScriptPath, saltMinionConfPath);
    }

    @Override
    public boolean isModuleChanged(String moduleName) {
        Boolean ret = moduleChanges.get(moduleName);
        DebugUtils.Assert(ret != null, String.format("cannot find salt module name[%s]", moduleName));
        return ret;
    }

    public void setMasterFileRoots(String masterFileRoots) {
        this.masterFileRoots = masterFileRoots;
    }
}

