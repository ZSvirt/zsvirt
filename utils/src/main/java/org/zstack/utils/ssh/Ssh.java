package org.zstack.utils.ssh;

import com.jcraft.jsch.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.StringDSL.ln;
import static org.zstack.utils.StringDSL.s;
import static org.zstack.utils.path.PathUtil.setFilePosixPermissions;
import static org.zstack.utils.path.PathUtil.writeFile;

public class Ssh {
    private static final CLogger logger = Utils.getLogger(Ssh.class);

    private String hostname;
    private String username;
    private String privateKey;
    private String password;
    private int port = 22;
    /**
     * used for ssh channel connection
     *
     * jsch uses this value as timeout waiting for
     * ssh connection message which contains
     * SSH_MSG_CHANNEL_OPEN_CONFIRMATION.
     *
     * try to extend timeout if fails to open the channel
     */
    private int timeout = 5;
    private int execTimeout = 604800;
    private int socketTimeout = 300;
    private Session session;
    private File privateKeyFile;
    private boolean closed = false;
    private boolean suppressException = false;
    private static final String LANGUAGE = "LANG=\"en_US.UTF-8\"; ";

    private boolean init = false;

    private class ScriptRunner implements SshRunner {
        String rawScript;
        SshRunner scriptCommand;

        ScriptRunner(CommandScript script) {
            this.rawScript = script.cmd;
            String remoteScript = ln(
                    "cat << EOF1 > {remotePath}",
                    "PATH=/usr/local/bin:/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/sbin",
                    "{scriptContent}",
                    "EOF1",
                    "timeout {execTimeout} /bin/bash {remotePath} 1>{stdout} 2>{stderr}",
                    "ret=$?",
                    "test -f {stdout} && cat {stdout}",
                    "test -f {stderr} && cat {stderr} 1>&2",
                    "rm -f {remotePath}",
                    "rm -f {stdout}",
                    "rm -f {stderr}",
                    "exit $ret"
            ).formatByMap(map(e("remotePath", String.format("/tmp/%s", UUID.randomUUID().toString())),
                    e("scriptContent", script),
                    e("execTimeout", execTimeout),
                    e("stdout", String.format("/tmp/%s", UUID.randomUUID().toString())),
                    e("stderr", String.format("/tmp/%s", UUID.randomUUID().toString()))
            ));

            CommandScript become = new CommandScript(remoteScript);
            if (script.sudo) {
                become.enableSudo();
            }
            scriptCommand = buildSshRunner(become);
        }

        @Override
        public SshResult run() {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("run script remotely[ip: %s, port: %s]:\n%s\n", hostname, port, rawScript));
            }
            return scriptCommand.run();
        }

        @Override
        public String getCommand() {
            return rawScript;
        }
    }

    public static class CommandScript {
        public final String cmd;
        private boolean sudo;
        private String mode = MODE_SSH;
        private final Map<String, String> parameters = new HashMap<>();

        public static final String MODE_SSH = "ssh";
        public static final String MODE_BATCH_SCRIPTS = "batchScripts";
        public static final String MODE_UPLOAD = "upload";
        public static final String MODE_DOWNLOAD = "download";

        public CommandScript(String cmd) {
            this.cmd = cmd;
        }
        public CommandScript enableSudo() {
            this.sudo = true;
            return this;
        }
        public CommandScript withMode(String mode) {
            this.mode = mode;
            return this;
        }
        public CommandScript withParameter(String key, String value) {
            parameters.put(key, value);
            return this;
        }
        public String getParameter(String key) {
            return parameters.get(key);
        }
        @Override
        public String toString() {
            return removeSensitiveInfoFromCmd(cmd);
        }
    }
    private List<CommandScript> commandScripts = new ArrayList<>();

    private SshRunner buildSshRunner(CommandScript commandScript) {
        switch (commandScript.mode) {
        case CommandScript.MODE_SSH: {
            if (!commandScript.sudo || "root".equals(username)) {
                return createCommand(commandScript.cmd);
            } else if (password == null) {
                return createCommand("sudo " + commandScript.cmd);
            } else {
                String quotePassword = shellQuote(password);
                return createCommand(String.format("echo %s | sudo -S %s 2>/dev/null", quotePassword, commandScript.cmd));
            }
        }

        case CommandScript.MODE_BATCH_SCRIPTS:
            return new ScriptRunner(commandScript);

        case CommandScript.MODE_UPLOAD:
            return createScpCommand(commandScript.getParameter("from"), commandScript.getParameter("to"), false);

        case CommandScript.MODE_DOWNLOAD:
            return createScpCommand(commandScript.getParameter("from"), commandScript.getParameter("to"), true);

        default:
            throw new IllegalArgumentException(String.format("unsupported mode[%s]", commandScript.mode));
        }
    }

    public int getExecTimeout() {
        return execTimeout;
    }

    public Ssh setExecTimeout(int execTimeout) {
        this.execTimeout = execTimeout;
        return this;
    }

    public String getHostname() {
        return hostname;
    }

    public Ssh setHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public Ssh setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public Ssh setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public Ssh setPassword(String password) {
        this.password = password;
        return this;
    }

    public int getPort() {
        return port;
    }

    public Ssh setPort(int port) {
        this.port = port;
        return this;
    }

    public boolean isSuppressException() {
        return suppressException;
    }

    public Ssh setSuppressException(boolean suppressException) {
        this.suppressException = suppressException;
        return this;
    }

    public Ssh command(String...cmds) {
        for (String cmd : cmds) {
            commandScripts.add(new CommandScript(cmd));
        }
        return this;
    }

    public Ssh sudoCommand(String...cmds) {
        for (String cmd : cmds) {
            commandScripts.add(new CommandScript(cmd).enableSudo());
        }
        return this;
    }

    private SshRunner createCommand(final String cmdWithoutPrefix) {
       final String cmd = LANGUAGE + cmdWithoutPrefix;
       return new SshRunner() {
           @Override
           public SshResult run() {
               SshResult ret = new SshResult();
               String cmdWithoutPassword = removeSensitiveInfoFromCmd(getCommand());
               ret.setDesensitizeCmd(cmdWithoutPassword);

               try {
                   ChannelExec channel = null;
                   try {
                       channel = (ChannelExec) session.openChannel("exec");
                       channel.setPty(true);
                       channel.setCommand(cmd);
                       if (logger.isTraceEnabled()) {
                           logger.trace(String.format("[start SSH] %s", cmdWithoutPassword));
                       }

                       try (InputStream ins = channel.getInputStream();
                            InputStream errs = channel.getErrStream()) {
                           channel.connect(getTimeoutInMilli(timeout));

                           String output = IOUtils.toString(ins, StandardCharsets.UTF_8);
                           String stderr = IOUtils.toString(errs, StandardCharsets.UTF_8);
                           ret.setReturnCode(channel.getExitStatus());
                           ret.setStderr(stderr);
                           ret.setStdout(output);
                           if (logger.isTraceEnabled()) {
                               logger.trace(String.format("[end SSH] %s, return code: %d", cmdWithoutPassword, ret.getReturnCode()));
                           }
                       }
                   } finally {
                       if (channel != null) {
                           channel.disconnect();
                       }
                   }
               } catch (Exception e) {
                   if (e instanceof IOException) {
                       ret.setSshFailure(true);
                   }

                   StringBuilder sb = new StringBuilder(String.format("exec ssh command: %s, exception\n", cmdWithoutPassword));
                   sb.append(String.format("[host:%s, port:%s, user:%s, timeout:%s]\n", hostname, port, username, timeout));
                   if (!suppressException) {
                       logger.warn(sb.toString(), e);
                   }
                   ret.setExitErrorMessage(e.getMessage());
                   ret.setReturnCode(1);
               }

               return ret;
           }

           @Override
           public String getCommand() {
               return cmd;
           }
       };
    }

    public Ssh scpUpload(final String local, final String remote) {
        commandScripts.add(new CommandScript("scpUpload")
                .withMode(CommandScript.MODE_UPLOAD)
                .withParameter("from", local)
                .withParameter("to", remote));
        return this;
    }

    public Ssh scpDownload(final String remote, final String local) {
        commandScripts.add(new CommandScript("scpDownload")
                .withMode(CommandScript.MODE_DOWNLOAD)
                .withParameter("to", local)
                .withParameter("from", remote));
        return this;
    }

    private SshRunner createScpCommand(final String src, final String dst, boolean download) {
        return new SshRunner() {
            @Override
            public SshResult run() {
                SshResult ret = new SshResult();
                String cmd = getCommand();
                ret.setDesensitizeCmd(cmd);

                try {
                    ChannelSftp channel = null;
                    try {
                        channel = (ChannelSftp) session.openChannel("sftp");
                        channel.connect(getTimeoutInMilli(timeout));

                        if (download) {
                            channel.get(src, dst);
                        } else {
                            channel.put(src, dst);
                        }
                        ret.setReturnCode(0);
                    } finally {
                        if (channel != null) {
                            channel.disconnect();
                        }
                    }
                } catch (JSchException | SftpException e) {
                    if (!suppressException) {
                        logger.warn(String.format("[SCP failed]: %s", cmd), e);
                    }
                    ret.setSshFailure(true);
                    ret.setReturnCode(1);
                    ret.setExitErrorMessage(e.getMessage());
                }

                return ret;
            }

            @Override
            public String getCommand() {
                if (download) {
                    return String.format("scp -P %d %s@%s:%s %s", port, username, hostname, src, dst);
                } else {
                    return String.format("scp -P %d %s %s@%s:%s", port, src, username, hostname, dst);
                }
            }
        };
    }

    public Ssh checkTool(String...toolNames) {
        String tool = StringUtils.join(Arrays.asList(toolNames), " ");
        String cmdstr = s("EXIT (){ echo \"$1\"; exit 1;}; cmds=\"{0}\"; for cmd in $cmds; do which $cmd >/dev/null 2>&1 || EXIT \"Not find command: $cmd\";  done").format(tool);
        return command(cmdstr);
    }

    public Ssh shell(String script) {
        boolean hasScript = false;
        for (CommandScript cs : commandScripts) {
            if (CommandScript.MODE_BATCH_SCRIPTS.equals(cs.mode)) {
                hasScript = true;
                break;
            }
        }
        DebugUtils.Assert(!hasScript, "every Ssh object can only specify one script");
        commandScripts.add(new CommandScript(script)
                .withMode(CommandScript.MODE_BATCH_SCRIPTS));
        return this;
    }

    private static int getTimeoutInMilli(int seconds) {
        if (seconds <= 0) {
            return 0;
        }

        long timeo = TimeUnit.SECONDS.toMillis(seconds);
        if (timeo < Integer.MAX_VALUE) {
            return (int)timeo;
        }

        return Integer.MAX_VALUE;
    }

    private void build() throws IOException {
        if (init) {
            return;
        }

        try {
            JSch jSch = new JSch();
            if (privateKey != null) {
                privateKeyFile = File.createTempFile("zstack", "tmp");
                writeSecretFile(privateKeyFile, privateKey);
                jSch.addIdentity(privateKeyFile.getAbsolutePath());
            }

            session = jSch.getSession(username, hostname, port);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(password);
            session.setConfig("PreferredAuthentications", privateKey == null ? "password" : "publickey,password");
            session.setConfig("server_host_key", session.getConfig("server_host_key") + ",ssh-rsa,ssh-dss");
            session.setConfig("PubkeyAcceptedKeyTypes", session.getConfig("PubkeyAcceptedKeyTypes") + ",ssh-rsa,ssh-dss");
            session.setTimeout(getTimeoutInMilli(socketTimeout)/2);
            session.connect(getTimeoutInMilli(timeout));
        } catch (JSchException ex) {
            throw new IOException(ex);
        }

        init = true;
    }

    public void close() {
        if (closed) {
            return;
        }

        closed = true;

        if (privateKeyFile != null) {
            if (!privateKeyFile.delete()) {
                logger.warn("delete file failed: " + privateKeyFile.getAbsolutePath());
            }
        }

        if (session != null) {
            session.disconnect();
        }
    }

    public SshResult run() {
        if (closed) {
            throw new SshException("this Ssh instance has been closed, you can not call run() after close()");
        }

        StopWatch watch = new StopWatch();
        watch.start();
        try {
            build();
            if (commandScripts.isEmpty()) {
                throw new IllegalArgumentException("no command or scp command or script specified");
            }

            if (privateKey == null && password == null) {
                throw new IllegalArgumentException("no password and private key specified");
            }

            if (username == null) {
                throw new IllegalArgumentException("no username specified");
            }

            if (hostname == null) {
                throw new IllegalArgumentException("no hostname specified");
            }

            SshResult ret = null;
            for (CommandScript commandScript : commandScripts) {
                SshRunner runner = buildSshRunner(commandScript);
                ret = runner.run();
                if (ret.getReturnCode() != 0) {
                    return ret;
                }
            }

            return ret;

        } catch (IOException e) {
            StringBuilder sb = new StringBuilder("ssh exception\n");
            sb.append(String.format("[host:%s, port:%s, user:%s, timeout:%s]\n", hostname, port, username, timeout));
            if (!suppressException) {
                logger.warn(sb.toString(), e);
            }
            SshResult ret = new SshResult();
            ret.setSshFailure(true);
            ret.setExitErrorMessage(e.getMessage());
            ret.setReturnCode(1);
            return ret;
        } finally {
            watch.stop();
            if (logger.isTraceEnabled()) {
                String cmd = StringUtils.join(commandScripts, ",");
                String info = s(
                        "\nssh execution[host: {0}, port:{1}]\n",
                        "command: {2}\n",
                        "cost time: {3}ms\n"
                ).format(hostname, port, cmd, watch.getTime());
                logger.trace(info);
            }
        }
    }

    public int getTimeout() {
        return timeout;
    }

    public Ssh setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public Ssh setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Ssh reset() {
        commandScripts.clear();
        return this;
    }

    public SshResult runAndClose() {
        SshResult ret = run();
        close();
        return ret;
    }

    public void runErrorByExceptionAndClose() {
        SshResult ret = run();
        close();
        ret.raiseExceptionIfFailed();
    }

    private static void writeSecretFile(File file, String content) throws IOException {
        writeFile(file, new byte[0]);
        setFilePosixPermissions(file, "rw-------");
        writeFile(file, content);
    }

    public static String removeSensitiveInfoFromCmd(String cmd) {
        return cmd.replaceAll("echo .*?\\s*\\|\\s*sudo -S", "echo ****** | sudo -S");
    }

    public static String shellQuote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }
}
