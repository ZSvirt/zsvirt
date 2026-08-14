package org.zstack.zops.utils;

import org.zstack.core.CoreGlobalProperty;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.zops.ZOpsConstants;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Client {
    private String hostname = ZOpsConstants.UNKNOWN;
    private SshInfo sshInfo;
    private String command = "";
    private Boolean withSudo = false;
    private int timeout = 5;
    public static Map<String, List<MockCommand>> mockMap = new HashMap<>();

    public CommandResult run(){
        if (CoreGlobalProperty.UNIT_TEST_ON ) {
            return getMockResults();
        }

        if (sshInfo == null) {
            return doRunCommandLocal();
        } else {
            return doRunCommandRemote();
        }
    }

    public CommandResult runLocalCommand(String command, Boolean withSudo) {
        this.withSudo = withSudo;
        this.hostname = "127.0.0.1";
        this.command = command;
        return run();
    }

    public Client command(String command) {
        return command(command, false);
    }

    public Client command(String command, Boolean withSudo) {
        this.command = command;
        this.withSudo = withSudo;
        return this;
    }

    public Client timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    private CommandResult doRunCommandLocal() {
        ShellResult res = ShellUtils.runAndReturn(command, withSudo);
        CommandResult cr = new CommandResult();
        cr.setDesensitizeCmd(res.getDesensitizeCmd());
        cr.setStderr(res.getStderr());
        cr.setStdout(res.getStdout());
        cr.setRetCode(res.getRetCode());
        return cr;
    }

    private CommandResult doRunCommandRemote() {
        Ssh ssh = new Ssh();
        if (withSudo) {
            command = String.format("sudo %s", command);
        }

        try {
            ssh.setHostname(sshInfo.getHostname());
            ssh.setTimeout(timeout);
            ssh.setPort(sshInfo.getPort());
            ssh.setPrivateKey(sshInfo.getPrivateKey());
            ssh.setUsername(sshInfo.getUsername());


            SshResult res = ssh.command(command).run();
            CommandResult cr = new CommandResult();
            cr.setDesensitizeCmd(res.getDesensitizeCmd());
            cr.setRetCode(res.getReturnCode());
            cr.setStdout(res.getStdout());
            if (res.isSshFailure()) {
                cr.setStderr(String.format("failed to build ssh connection with %s", hostname));
            } else {
                cr.setStderr(res.getStderr());
            }
            return cr;
        } finally {
            ssh.close();
        }
    }

    public void setSshInfo(SshInfo sshInfo) {
        this.sshInfo = sshInfo;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    private CommandResult getMockResults() {
        if (mockMap == null) {
            throw new CloudRuntimeException("please mock results before run test.");
        }

        List<MockCommand> mocks = mockMap.get(hostname);
        if (mocks == null) {
            throw new CloudRuntimeException(String.format("please mock host %s before run test.", hostname));
        }

        for (MockCommand mock: mocks) {
            if (mock.match(command)) {
                mocks.remove(mock);
                return mock.getResult();
            }
        }

        throw new CloudRuntimeException(String.format("please mock command [%s] in host %s before run test.", command, hostname));
    }

    public static void mockSuccessCommands(String hostname, String command, String result) {
        mockMap.computeIfAbsent(hostname, k -> new ArrayList<>()).add(
                new MockCommand(command, result)
        );
    }

    public static Boolean mockCommandsIsEmpty() {
        for (List<MockCommand> value : mockMap.values()) {
            if (!value.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
