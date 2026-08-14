package org.zstack.utils;

import org.apache.commons.io.FileUtils;
import org.zstack.utils.logging.CLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class Bash {
    private static final CLogger logger = Utils.getLogger(Bash.class);

    protected abstract void scripts();
    protected int lastReturnCode;
    protected String lastStdout;
    protected String lastStderr;
    protected String lastCommand;
    protected boolean SET_E;

    protected class BashBuilder {
        private boolean useSudo;
        private String path;

        public BashBuilder sudo() {
            useSudo = true;
            return this;
        }

        public BashBuilder cwd(String v) {
            path = v;
            return this;
        }

        public int run(String cmd, Object...args) {
            if (args != null) {
                cmd = String.format(cmd, args);
            }

            lastCommand = Utils.maskSensitiveInfo(cmd);
            ShellResult res = ShellUtils.runAndReturn(cmd, path, useSudo);
            lastReturnCode = res.getRetCode();
            lastStdout = res.getStdout();
            lastStderr = res.getStderr();

            if (SET_E) {
                errorOnFailure();
            }

            return lastReturnCode;
        }
    }

    public static class Script {
        public boolean escape = true;
        public String cmd;

        @Override
        public String toString() {
            if (cmd == null) {
                return "";
            }
            return escape ? escapeCmd(cmd) : cmd;
        }

        public static String escapeCmd(String cmd) {
            if (cmd.contains("\\")) {
                cmd = cmd.replace("\\", "\\\\");
            }
            if (cmd.contains(" ")) {
                cmd = cmd.replace(" ", "\\ ");
            }
            return cmd;
        }
    }

    protected Script noEscape(String cmd) {
        Script s = new Script();
        s.escape = false;
        s.cmd = cmd;
        return s;
    }

    /**
     * @return return true if @content is equal to @filePath's content
     */
    protected boolean compareWithFile(String filePath, String content) {
        File f = new File(filePath);
        if (!f.exists() || f.isDirectory()) {
            return false;
        }
        try {
            String fileContent = FileUtils.readFileToString(f);
            return fileContent.trim().equals(content.trim());
        } catch (IOException e) {
            logger.warn(e.getLocalizedMessage());
            return false;
        }
    }

    protected void writeFile(String filePath, String content) {
        mkdirs(dirname(filePath));

        try {
            FileUtils.writeStringToFile(new File(filePath), content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void mkdirs(String path) {
        File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
        }
    }

    protected String dirname(String path) {
        return new File(path).getParentFile().getPath();
    }

    protected void copyDir(String src, String dst) {
        try {
            FileUtils.copyDirectory(new File(src), new File(dst));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected BashBuilder bash() {
        return new BashBuilder();
    }

    protected int run(String cmd, Object...args) {
        return runFormat(cmd, args);
    }

    protected int runScripts(Object... cmdScripts) {
        List<String> tos = new ArrayList<>(cmdScripts.length);
        for (Object cmd : cmdScripts) {
            if (cmd instanceof Script) {
                tos.add(cmd.toString());
            } else {
                tos.add(Script.escapeCmd(cmd.toString()));
            }
        }
        return runFormat(String.join(" ", tos));
    }

    protected int runFormat(String cmd, Object... args) {
        if (args != null) {
            cmd = String.format(cmd, args);
        }

        lastCommand = cmd;
        ShellResult res = ShellUtils.runAndReturn(cmd, false);
        lastReturnCode = res.getRetCode();
        lastStdout = res.getStdout();
        lastStderr = res.getStderr();

        if (SET_E) {
            errorOnFailure();
        }

        return lastReturnCode;
    }

    protected int sudoRun(String cmd, Object... args) {
        return sudoRunFormat(cmd, args);
    }

    /**
     * ex: sudoRun("tar", "-xf", path)      => run("tar -xf %s", path)     => tar -xf $your_path
     * ex: sudoRun("cat", "error code.txt") => run("cat error\\ code.txt") => cat error\ code.txt
     */
    protected int sudoRunScripts(Object... cmdScripts) {
        List<String> tos = new ArrayList<>(cmdScripts.length);
        for (Object cmd : cmdScripts) {
            if (cmd instanceof Script) {
                tos.add(cmd.toString());
            } else {
                tos.add(Script.escapeCmd(cmd.toString()));
            }
        }
        return sudoRunFormat(String.join(" ", tos));
    }

    protected int sudoRunFormat(String cmd, Object... args) {
        if (args != null && args.length > 0) {
            cmd = String.format(cmd, args);
        }

        lastCommand = cmd;
        ShellResult res = ShellUtils.runAndReturn(cmd, true);
        lastReturnCode = res.getRetCode();
        lastStdout = res.getStdout();
        lastStderr = res.getStderr();

        if (SET_E) {
            errorOnFailure();
        }

        return lastReturnCode;
    }

    protected void errorOnFailure() {
        if (lastReturnCode != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("\nshell command[%s] failed", lastCommand));
            sb.append(String.format("\nret code: %s", lastReturnCode));
            sb.append(String.format("\nstderr: %s", lastStderr));
            sb.append(String.format("\nstdout: %s", lastStdout));
            throw new RuntimeException(sb.toString());
        }
    }

    // similar to set -e in shell script
    protected void setE() {
        SET_E = true;
    }

    // similar to set +e in shell script
    protected void unsetE() {
        SET_E = false;
    }

    protected String stdout() {
        return lastStdout;
    }

    protected String stderr() {
        return lastStderr;
    }

    public void execute() {
        scripts();
    }
}
