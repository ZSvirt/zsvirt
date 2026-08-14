package org.zstack.core.ansible;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.ini4j.Wini;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.jsonlabel.JsonLabel;
import org.zstack.core.jsonlabel.JsonLabelInventory;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.ShellUtils.ShellException;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_BASH_CMD;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_EXCEPTION;

/**
 */
public class AnsibleFacadeImpl extends AbstractService implements AnsibleFacade {
    private static final CLogger logger = Utils.getLogger(AnsibleFacadeImpl.class);

    private int maxForks = 100;
    private String filesDir = PathUtil.join(AnsibleConstant.ROOT_DIR, "files");
    private Map<String, Boolean> moduleChanges = new HashMap<String, Boolean>();
    private Map<String, String> variables = new HashMap<String, String>();

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ThreadFacade dbf;

    private String publicKey;
    private String privateKey;

    private void placePip703() {
        File pip = PathUtil.findFileOnClassPath("tools/pip-7.0.3.tar.gz");
        if (pip == null) {
            throw new CloudRuntimeException("cannot find tools/pip-7.0.3.tar.gz on classpath");
        }

        File root = new File(filesDir);
        if (!root.exists()) {
            root.mkdirs();
        }

        ShellUtils.run(String.format("yes | cp %s %s", pip.getAbsolutePath(), filesDir));
    }

    private void placeAnsible4100() {
        File ansible = PathUtil.findFileOnClassPath("tools/ansible-4.10.0-py2.py3-none-any.whl");
        if (ansible == null) {
            throw new CloudRuntimeException("cannot find tools/ansible-4.10.0-py2.py3-none-any.whl on classpath");
        }

        File root = new File(filesDir);
        if (!root.exists()) {
            root.mkdirs();
        }

        ShellUtils.run(String.format("yes | cp %s %s", ansible.getAbsolutePath(), filesDir));
    }

    void init() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            logger.debug("skip AnsibleFacade init as it's unittest");
            return;
        }

        try {
            File invFile = new File(AnsibleConstant.CONFIGURATION_FILE);
            File invDir = new File(invFile.getParent());
            if (!invDir.exists()) {
                invDir.mkdirs();
            }

            if (!invFile.exists() && !invFile.createNewFile()) {
                throw new OperationFailureException(operr("fail to create new File[%s]", invFile));
            }
            Wini ini = new Wini(invFile);
            Map<String, String> cfgs = Platform.getGlobalPropertiesStartWith("Ansible.cfg.");
            ini.put("defaults", "forks", maxForks);
            ini.put("defaults", "inventory", AnsibleConstant.INVENTORY_FILE);
            for (Map.Entry<String, String> e : cfgs.entrySet()) {
                String key = StringDSL.stripStart(e.getKey(), "Ansible.cfg.");
                if (!key.contains(".")) {
                    ini.put("defaults", key, e.getValue());
                } else {
                    String[] pair = key.split("\\.", 2);
                    ini.put(pair[0], pair[1], e.getValue());
                }
                logger.debug(String.format("added ansible cfg[%s=%s] to %s", key, e.getValue(), AnsibleConstant.CONFIGURATION_FILE));
            }
            ini.store();

            Map<String, String> vars = Platform.getGlobalPropertiesStartWith("Ansible.var.");
            for (Map.Entry<String, String> e : vars.entrySet()) {
                String key = StringDSL.stripStart(e.getKey(), "Ansible.var.");
                variables.put(key, e.getValue());
                logger.debug(String.format("discovered ansible variable[%s=%s]", key, e.getValue()));
            }

            placePip703();
            placeAnsible4100();

            ShellUtils.run(String.format(
                    "NEED_INSTALL=false; " +
                    "if [ -d /var/lib/zstack/virtualenv/zstacksys ]; then " +
                    ". /var/lib/zstack/virtualenv/zstacksys/bin/activate; " +
                    "if ! ansible --version | grep -q 'core 2.11.12'; then " +
                    "deactivate; " +
                    "NEED_INSTALL=true; " +
                    "fi; " +
                    "else " +
                    "NEED_INSTALL=true; "+
                    "fi; " +
                    "if $NEED_INSTALL; then " +
                    "sudo bash -c 'rm -rf /var/lib/zstack/virtualenv/zstacksys && virtualenv /var/lib/zstack/virtualenv/zstacksys --python=python2.7; "+
                    ". /var/lib/zstack/virtualenv/zstacksys/bin/activate; "+
                    "TMPDIR=/usr/local/zstack/ pip install -i file://%s --trusted-host localhost -I setuptools==39.2.0; "+
                    "TMPDIR=/usr/local/zstack/ pip install -i file://%s --trusted-host localhost -I ansible==4.10.0'; "+
                    "fi" , AnsibleConstant.PYPI_REPO, AnsibleConstant.PYPI_REPO), false);


            deployModule("ansible/zstacklib", "zstacklib.py");
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof RunAnsibleMsg) {
            handle((RunAnsibleMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final RunAnsibleMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "run-ansible-sync-control";
            }

            @Override
            public void run(SyncTaskChain chain) {
                doHandle(msg, chain);
            }

            @Override
            protected int getSyncLevel() {
                return maxForks;
            }

            @Override
            public String getName() {
                return String.format("run-ansible-for-%s", msg.getTargetIp());
            }
        });
    }

    private Map<String, Object> collectArguments(RunAnsibleMsg msg) {
        Map<String, Object> arguments = new HashMap<String, Object>();
        if (msg.getArguments() != null) {
            arguments.putAll(msg.getArguments());
        }

        if (msg.getDeployArguments() != null) {
            arguments.putAll(msg.getDeployArguments().toArgumentMap());
        }

        arguments.put("host", msg.getTargetIp());
        arguments.put("mn_ip", Platform.getCanonicalServerIp());
        if (AnsibleGlobalConfig.ENABLE_ANSIBLE_CACHE_SYSTEM_INFO.value(Boolean.class)) {
            arguments.put("host_uuid", msg.getTargetUuid());
        }

        arguments.put("zstack_root", AnsibleGlobalProperty.ZSTACK_ROOT);
        arguments.put("pkg_zstacklib", AnsibleGlobalProperty.ZSTACKLIB_PACKAGE_NAME);
        getVariables().forEach(arguments::putIfAbsent);
        String playBookPath = msg.getPlayBookPath();
        if (!playBookPath.contains("py")) {
            arguments.put("ansible_ssh_user", arguments.get("remote_user"));
            arguments.put("ansible_ssh_port", arguments.get("remote_port"));
            arguments.put("ansible_ssh_pass", arguments.get("remote_pass"));
            arguments.remove("remote_user");
            arguments.remove("remote_pass");
            arguments.remove("remote_port");
            if (!arguments.get("ansible_ssh_user").equals("root")) {
                arguments.put("ansible_become", "yes");
                arguments.put("become_user", "root");
                arguments.put("ansible_become_pass", arguments.get("ansible_ssh_pass"));
            }
        }

        return arguments;
    }

    private void doHandle(final RunAnsibleMsg msg, SyncTaskChain outter) {
        thdf.syncSubmit(new SyncTask<Object>() {
            @Override
            public String getSyncSignature() {
                return String.format("run-anisble-for-host-%s", msg.getTargetIp());
            }

            @Override
            public int getSyncLevel() {
                return 1;
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            private void run(Completion completion) {
                new PrepareAnsible().setTargetIp(msg.getTargetIp()).prepare();

                String playBookPath = msg.getPlayBookPath();
                Map<String, Object> arguments = collectArguments(msg);
                logger.debug(String.format("start running ansible for playbook[%s]", msg.getPlayBookPath()));
                String executable = msg.getAnsibleExecutable() == null ? AnsibleGlobalProperty.EXECUTABLE : msg.getAnsibleExecutable();
                long timeout = TimeUnit.MILLISECONDS.toSeconds(msg.getTimeout());
                try {
                    String output;
                    if (AnsibleGlobalProperty.DEBUG_MODE2) {
                        output = ShellUtils.run(String.format("bash -c '. /var/lib/zstack/virtualenv/zstacksys/bin/activate; PYTHONPATH=%s timeout %d %s %s -i %s -vvvv --private-key %s -e '\\''%s'\\' | tee -a %s",
                                AnsibleConstant.ZSTACKLIB_ROOT, timeout, executable, playBookPath, AnsibleConstant.INVENTORY_FILE, msg.getPrivateKeyFile(), JSONObjectUtil.dumpPretty(arguments), AnsibleConstant.LOG_PATH),
                                AnsibleConstant.ROOT_DIR);
                    } else if (AnsibleGlobalProperty.DEBUG_MODE) {
                        output = ShellUtils.run(String.format("bash -c '. /var/lib/zstack/virtualenv/zstacksys/bin/activate; PYTHONPATH=%s timeout %d %s %s -i %s -vvvv --private-key %s -e '\\''%s'\\'",
                                AnsibleConstant.ZSTACKLIB_ROOT, timeout, executable, playBookPath, AnsibleConstant.INVENTORY_FILE, msg.getPrivateKeyFile(), JSONObjectUtil.dumpPretty(arguments)),
                                AnsibleConstant.ROOT_DIR);
                    } else {
                        output = ShellUtils.run(String.format("bash -c '. /var/lib/zstack/virtualenv/zstacksys/bin/activate; PYTHONPATH=%s timeout %d %s %s -i %s --private-key %s -e '\\''%s'\\'",
                                AnsibleConstant.ZSTACKLIB_ROOT, timeout, executable, playBookPath, AnsibleConstant.INVENTORY_FILE, msg.getPrivateKeyFile(), JSONObjectUtil.dumpPretty(arguments)),
                                AnsibleConstant.ROOT_DIR);
                    }

                    if (output.contains("skipping: no hosts matched")) {
                        throw new OperationFailureException(operr("failed to run ansible: failed to find target host")
                                .withOpaque(OPAQUE_KEY_EXCEPTION, output));
                    }

                } catch (ShellException se) {
                    logger.warn("failed to run ansible", se);
                    throw new OperationFailureException(buildErrorCode(se));
                }

                completion.success();
            }
            private ErrorCode buildErrorCode(ShellException exception) {
                ErrorCode error = operr("failed to run ansible").withOpaque(exception);
                if (error.getFromOpaque(OPAQUE_KEY_BASH_CMD) != null) {
                    error.withOpaque(OPAQUE_KEY_BASH_CMD, hidePassword((String) error.getFromOpaque(OPAQUE_KEY_BASH_CMD)));
                }
                return error;
            }

            private String hidePassword(String message) {
                String errMsg = message;
                if (errMsg.contains("remote_pass")) {
                    String[] splitted = errMsg.trim().split(",");
                    if (splitted.length > 1) {
                        List<String> replaces = new ArrayList<>();
                        boolean simple = false;
                        for (String s : splitted) {
                            if (s.startsWith("\\\"remote_pass\\\":")) {
                                replaces.add(s);
                            } else if (s.startsWith("\"remote_pass\":")) {
                                replaces.add(s);
                                simple = true;
                            }
                        }
                        if (!replaces.isEmpty()) {
                            for (String replace: replaces) {
                                if (simple) {
                                    errMsg = errMsg.replaceFirst(replace, "\"remote_pass\":\"******\"");
                                } else {
                                    errMsg = errMsg.replaceFirst(replace, "\\\"remote_pass\\\":\\\"******\\\"");
                                }
                            }
                        }
                    }
                }
                return errMsg;
            }

            @Override
            public Object call() {
                final RunAnsibleReply reply = new RunAnsibleReply();
                run(new Completion(msg, outter) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        outter.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        outter.next();
                    }
                });

                return null;
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(AnsibleConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return true;
        }

        try {
            File privKeyFile = PathUtil.findFileOnClassPath(AnsibleConstant.RSA_PRIVATE_KEY, true);
            PathUtil.setFilePosixPermissions(privKeyFile.getAbsolutePath(), "rw-------");
            File pubKeyFile = PathUtil.findFileOnClassPath(AnsibleConstant.RSA_PUBLIC_KEY);

            if (pubKeyFile == null) {
                publicKey = new JsonLabel().get("ansiblePublicKey", String.class);
            } else {
                publicKey = FileUtils.readFileToString(pubKeyFile);
                publicKey = publicKey.trim();
                publicKey = StringDSL.stripEnd(publicKey, "\n");
            }

            if (privKeyFile.length() == 0) {
                privateKey = new JsonLabel().get("ansiblePrivateKey", String.class);
            } else {
                privateKey = FileUtils.readFileToString(privKeyFile);
                privateKey = privateKey.trim();
                privateKey = StringDSL.stripEnd(privateKey, "\n");
            }

            if (StringUtils.isEmpty(privateKey)) {
                String info = String.format("Private key [%s] for Ansible is corrupted.\n"
                                + "Please re-generate it with:\n"
                                + "# ssh-keygen -f %s -N \"\"\n"
                                + "# chown -R zstack.zstack %s",
                        privKeyFile.getName(), privKeyFile.getAbsolutePath(), privKeyFile.getParent());
                throw new CloudRuntimeException(info);
            }

            JsonLabelInventory ansiblePublicKey = new JsonLabel().createIfAbsent("ansiblePublicKey", publicKey);
            JsonLabelInventory ansiblePrivateKey = new JsonLabel().createIfAbsent("ansiblePrivateKey", privateKey);

            publicKey = ansiblePublicKey.getLabelValue();
            if (pubKeyFile != null) {
                FileUtils.writeStringToFile(pubKeyFile, publicKey);
            }

            privateKey = ansiblePrivateKey.getLabelValue();
            FileUtils.writeStringToFile(privKeyFile, privateKey);
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private boolean isNeedToDeploy(String moduleName, String modulePath) throws IOException {
        boolean needed = false;

        try {
            String destModulePath = PathUtil.join(filesDir, moduleName);
            File dest = new File(destModulePath);
            if (!dest.exists()) {
                logger.debug(String.format("%s is not existing, need to deploy ansible module[%s]", destModulePath, moduleName));
                needed = true;
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
                logger.debug(String.format("%s has %s files, %s has %s files, need to deploy ansible module[%s]", destModulePath, destFiles.size(), modulePath,
                        srcFiles.size(), moduleName));
                needed = true;
                return true;
            }

            Map<String, String> srcMd5sum = new HashMap<String, String>(srcFiles.size());
            for (File f : srcFiles) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(f);
                    String md5 = DigestUtils.md5Hex(fis);
                    if (!srcMd5sum.containsKey(f.getName())) {
                        srcMd5sum.put(f.getName(), md5);
                    } else {
                        srcMd5sum.put(String.format("%s:%s", f.getName(), f.length()), md5);
                    }
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
                    if (!destMd5sum.containsKey(f.getName())) {
                        destMd5sum.put(f.getName(), md5);
                    } else {
                        destMd5sum.put(String.format("%s:%s", f.getName(), f.length()), md5);
                    }
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
                if (srcEntry.getKey().contains(":")) {
                    logger.debug(String.format("duplicate file name detected, combine name and length as a unique key: %s", srcEntry.getKey()));
                }
                String name = srcEntry.getKey();
                String destMd5 = destMd5sum.get(name);
                if (destMd5 == null) {
                    logger.debug(String.format("%s is not existing in %s, need to deploy ansible module[%s]", name, destModulePath, moduleName));
                    needed = true;
                    return true;
                }
                if (!destMd5.equals(srcEntry.getValue())) {
                    logger.debug(String.format("%s's md5 changed[{%s} in %s, {%s} in %s], need to deploy ansible module[%s]", name, destMd5, destModulePath,
                            srcEntry.getValue(), modulePath, moduleName));
                    needed = true;
                    return true;
                }
            }
            logger.debug(String.format("no file changed in ansible module[%s], no need to deploy", moduleName));
            needed = false;
            return false;
        } finally {
            moduleChanges.put(moduleName, needed);
        }
    }

    @Override
    public void deployModule(String modulePath, String playBookName) {
        File src = PathUtil.findFolderOnClassPath(modulePath, true);
        if (!src.isDirectory()) {
            throw new CloudRuntimeException(String.format("Cannot find ansible module[%s], it's either not existing or not a directory", modulePath));
        }

        String moduleName = src.getName();

        File root = new File(AnsibleConstant.ROOT_DIR);
        if (!root.exists()) {
            root.mkdirs();
        }

        File filesRoot = new File(filesDir);
        if (!filesRoot.exists()) {
            filesRoot.mkdirs();
        }

        try {
            if (!isNeedToDeploy(moduleName, src.getAbsolutePath())) {
                return;
            }

            String destModulePath = PathUtil.join(filesRoot.getAbsolutePath(), moduleName);
            File dest = new File(destModulePath);
            if (dest.exists()) {
                FileUtils.forceDelete(dest);
                FileUtils.forceMkdir(dest);
            }
            FileUtils.copyDirectory(src, dest);


            boolean isPlaybookLinked = false;
            for (File f : dest.listFiles()) {
                if (f.getName().equals(playBookName)) {
                    String lnPath = PathUtil.join(AnsibleConstant.ROOT_DIR, playBookName);
                    File lnFile = new File(lnPath);
                    if (lnFile.exists() && !lnFile.delete()) {
                        logger.warn(String.format("failed to delete file[%s]", lnFile));
                    }
                    Files.createSymbolicLink(Paths.get(lnPath), Paths.get(f.getAbsolutePath()));
                    isPlaybookLinked = true;
                }
            }

            // if playBookName=null, skip the deploy steps because the deploy is not independent
            if (playBookName != null && !isPlaybookLinked) {
                throw new IllegalArgumentException(String.format("cannot find playbook[%s] in module[%s], module files are%s", playBookName, modulePath, Arrays.asList(src.list())));
            }

            logger.debug(String.format("successfully deployed ansible module[%s]", modulePath));
        } catch (Exception e) {
            String err = String.format("Unable to deploy ansible module[%s] from %s", moduleName, modulePath);
            throw new CloudRuntimeException(err, e);
        }
    }

    @Override
    public boolean isModuleChanged(String playbookName) {
        String moduleName = StringDSL.stripEnd(playbookName, ".py");
        Boolean ret = moduleChanges.get(moduleName);
        DebugUtils.Assert(ret != null, String.format("cannot find ansible module name[%s]", moduleName));
        if (ret) {
            // we only need to deploy once
            moduleChanges.put(moduleName, false);
        }
        return ret;
    }

    @Override
    public Map<String, String> getVariables() {
        return variables;
    }

    @Override
    public String getPublicKey() {
        return publicKey;
    }

    @Override
    public String getPrivateKey() {
        return privateKey;
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
}
