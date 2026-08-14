package org.zstack.utils;

import org.apache.commons.io.IOUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class Linux {
    public static class ShellResult {
        private String stderr;
        private String stdout;
        private int exitCode;

        public String getStderr() {
            return stderr;
        }

        public void setStderr(String stderr) {
            this.stderr = stderr;
        }

        public String getStdout() {
            return stdout;
        }

        public void setStdout(String stdout) {
            this.stdout = stdout;
        }

        public int getExitCode() {
            return exitCode;
        }

        public void setExitCode(int exitCode) {
            this.exitCode = exitCode;
        }
    }

    public static ShellResult shell(String cmdStr) {
        String[] cmds = cmdStr.split(" ");
        List<String> cmdLst = new ArrayList<String>();
        Collections.addAll(cmdLst, cmds);
        return shell(cmdLst);
    }

    public static ShellResult shell(List<String> cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process pro = pb.start();
            int exitCode = pro.waitFor();
            ShellResult ret = new ShellResult();
            ret.setExitCode(exitCode);
            ret.setStderr(IOUtils.toString(pro.getErrorStream()));
            ret.setStdout(IOUtils.toString(pro.getInputStream()));
            return ret;
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
