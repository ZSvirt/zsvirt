package org.zstack.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.opaque.OpaqueScripts;
import org.zstack.utils.path.PathUtil;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_EXCEPTION;

public class ShellUtils {
    private static final CLogger logger = Utils.getSafeLogger(ShellUtils.class);

    public static class ShellException extends RuntimeException implements OpaqueScripts {
        private ShellResult result;

        public ShellException(String msg, Throwable t) {
            super(msg, t);
        }

        public ShellException(String msg) {
            super(msg);
        }

        public ShellException(Throwable t) {
            super(t);
        }

        public ShellResult getResult() {
            return result;
        }

        public ShellException withResult(ShellResult result) {
            this.result = result;
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map<String, Object> opaqueScripts() {
            if (result != null) {
                return result.opaqueScripts();
            }
            return map(e(OPAQUE_KEY_EXCEPTION, getMessage()));
        }
    }

    private static class StreamConsumer extends Thread {
        final InputStream in;
        final PrintWriter out;
        final boolean flush;

        StreamConsumer(InputStream in, PrintWriter out, boolean flushEveryWrite) {
            this.in = in;
            this.out = out;
            flush = flushEveryWrite;
        }

        @Override
        public void run() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ( (line = br.readLine()) != null) {
                    out.println(line);
                    if (flush) {
                        out.flush();
                    }
                }
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    public static String runVerbose(String cmdstr) {
        return runVerbose(cmdstr, null);
    }

    public static String runVerbose(String cmdstr, String baseDir) {
        return doRun(cmdstr, baseDir, true);
    }

    public static String runVerbose(String cmdstr, String baseDir, boolean withSudo) {
        return doRun(cmdstr, baseDir, withSudo, true);
    }

    public static String run(String cmdstr, String baseDir) {
        return doRun(cmdstr, baseDir, false);
    }

    public static String run(String cmdstr, String baseDir, boolean withSudo) {
        return doRun(cmdstr, baseDir, withSudo, false);
    }

    public static String run(String cmdsr) {
        return run(cmdsr, null);
    }

    public static String run(String cmdsr, boolean withSudo) {
        return run(cmdsr, null, withSudo);
    }

    public static class ShellRunner {
        private String command;
        private String baseDir;
        private boolean verbose;
        private boolean suppressTraceLog;
        private String stderrFile;
        private String stdoutFile;
        private Process process;
        private boolean withSudo = true;

        public void terminate() {
            DebugUtils.Assert(process!=null, String.format("you can only can call terminate() after calling run()"));
            process.destroy();
        }

        public boolean isWithSudo() {
            return withSudo;
        }

        public void setWithSudo(boolean withSudo) {
            this.withSudo = withSudo;
        }

        public String getStderrFile() {
            return stderrFile;
        }

        public void setStderrFile(String stderrFile) {
            this.stderrFile = stderrFile;
        }

        public String getStdoutFile() {
            return stdoutFile;
        }

        public void setStdoutFile(String stdoutFile) {
            this.stdoutFile = stdoutFile;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public String getBaseDir() {
            return baseDir;
        }

        public void setBaseDir(String baseDir) {
            this.baseDir = baseDir;
        }

        public boolean isVerbose() {
            return verbose;
        }

        public void setVerbose(boolean verbose) {
            this.verbose = verbose;
        }

        public boolean isSuppressTraceLog() {
            return suppressTraceLog;
        }

        public void setSuppressTraceLog(boolean suppressTraceLog) {
            this.suppressTraceLog = suppressTraceLog;
        }

        private static final int LOG_TO_FILE = 0;
        private static final int LOG_TO_SCREEN = 1;
        private static final int LOG_TO_STRING = 2;

        private int logStrategy(String fileToCheck) {
            if (fileToCheck != null) {
                return LOG_TO_FILE;
            } else if (verbose) {
                return LOG_TO_SCREEN;
            } else {
                return LOG_TO_STRING;
            }
        }

        private int stdoutLogStrategy() {
            return logStrategy(stdoutFile);
        }

        private int stderrLogStrategy() {
            return logStrategy(stderrFile);
        }

        public Integer obtainUnixPid() {
            try {
                Class clz = process.getClass();
                if (!clz.getName().equals("java.lang.UNIXProcess")) {
                    return null;
                }

                Field pidField = clz.getDeclaredField("pid");
                pidField.setAccessible(true);
                Object value = pidField.get(process);
                return (Integer) value;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public ShellResult run() {
            StopWatch watch = new StopWatch();
            watch.start();
            try {
                if (withSudo) {
                    command = String.format("sudo %s", command);
                }

                ProcessBuilder pb = new ProcessBuilder(Arrays.asList("/bin/bash", "-c", command));
                if (baseDir == null) {
                    baseDir = System.getProperty("user.home");
                }
                pb.directory(new File(baseDir));

                process = pb.start();
                command = Utils.maskSensitiveInfo(command);
                if (!suppressTraceLog && logger.isTraceEnabled()) {
                    logger.debug(String.format("exec shell command[%s]", command));
                }

                Writer stdout;
                int stdoutLog = stdoutLogStrategy();
                if (stdoutLog == LOG_TO_FILE) {
                    stdout = new BufferedWriter(new FileWriter(stdoutFile));
                } else if (stdoutLog == LOG_TO_SCREEN) {
                    stdout = new BufferedWriter(new OutputStreamWriter(System.out));
                } else {
                    stdout = new StringWriter();
                }

                Writer stderr;
                int stderrLog = stderrLogStrategy();
                if (stderrLog == LOG_TO_FILE) {
                    stderr = new BufferedWriter(new FileWriter(stderrFile));
                } else if (stderrLog == LOG_TO_SCREEN) {
                    stderr = new BufferedWriter(new OutputStreamWriter(System.err));
                } else {
                    stderr = new StringWriter();
                }

                StreamConsumer stdoutConsumer = new StreamConsumer(process.getInputStream(), new PrintWriter(stdout, true), stdoutLog != LOG_TO_FILE);
                StreamConsumer stderrConsumer = new StreamConsumer(process.getErrorStream(), new PrintWriter(stderr, true), stderrLog != LOG_TO_FILE);

                stderrConsumer.start();
                stdoutConsumer.start();
                process.waitFor();
                stderrConsumer.join(TimeUnit.SECONDS.toMillis(30));
                stdoutConsumer.join(TimeUnit.SECONDS.toMillis(30));

                ShellResult ret = new ShellResult();
                ret.setDesensitizeCmd(command);
                ret.setRetCode(process.exitValue());
                if (stderrLog == LOG_TO_STRING) {
                    ret.setStderr(stderr.toString());
                } else if (stderrLog == LOG_TO_FILE) {
                    stderr.close();
                }
                if (stdoutLog == LOG_TO_STRING) {
                    ret.setStdout(stdout.toString());
                } else if (stdoutLog == LOG_TO_FILE) {
                    stdout.close();
                }

                return ret;
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                throw new ShellException("shell command inner exception: " + command, e);
            } finally {
                if (process != null) {
                    process.destroy();
                }
                watch.stop();
                if (!suppressTraceLog && logger.isTraceEnabled()) {
                    logger.trace(String.format("shell command[%s] costs %sms to finish", command, watch.getTime()));
                }
            }
        }
    }

    private static String doRun(String cmdstr, String baseDir, boolean isVerbose) {
        return doRun(cmdstr, baseDir, true, isVerbose);
    }

    private static String doRun(String cmdstr, String baseDir, boolean withRoot, boolean isVerbose) {
        ShellRunner runner = new ShellRunner();
        runner.command = cmdstr;
        runner.baseDir = baseDir;
        runner.verbose = isVerbose;
        runner.withSudo = withRoot;
        ShellResult ret = runner.run();

        ret.raiseExceptionIfFail();

        StringBuilder sb = new StringBuilder(String.format("exec shell: %s\n", cmdstr));

        // if isVerbose is set, there is nothing to read as stream has been closed
        if (!isVerbose) {
            sb.append(String.format("stdout: %s\n", ret.getStdout()));
            sb.append(String.format("stderr: %s\n", ret.getStderr()));
        }

        return sb.toString();
    }

    public static ShellResult runAndReturn(String cmdstr, String baseDir, boolean withSudo) {
        return doRunAndReturn(cmdstr, baseDir, withSudo);
    }

    public static ShellResult runAndReturn(String cmdstr, String baseDir) {
        return doRunAndReturn(cmdstr, baseDir, true);
    }

    public static ShellResult runAndReturn(String cmdsr) {
        return runAndReturn(cmdsr, null);
    }

    public static ShellResult runAndReturn(String cmdsr, boolean withSudo) {
        return doRunAndReturn(cmdsr, null, withSudo);
    }

    private static ShellResult doRunAndReturn(String cmdstr, String baseDir, boolean withSudo) {
        ShellRunner runner = new ShellRunner();
        runner.command = cmdstr;
        runner.baseDir = baseDir;
        runner.withSudo = withSudo;
        return runner.run();
    }

    public static String getScriptsPath(String scriptName) {
        return PathUtil.findFileOnClassPath(scriptName, true).getAbsolutePath();
    }

    public static final String[] ESCAPE_TEXT_FORM;
    public static final String[] ESCAPE_TEXT_TO;

    static {
        Map<String, String> escapeMap = new HashMap<>();
        escapeMap.put("\\", "\\\\");
        escapeMap.put(" ", "\\ ");
        escapeMap.put("`", "\\`");
        escapeMap.put("~", "\\~");
        escapeMap.put("!", "\\!");
        escapeMap.put("#", "\\#");
        escapeMap.put("$", "\\$");
        escapeMap.put("&", "\\&");
        escapeMap.put("*", "\\*");
        escapeMap.put("(", "\\(");
        escapeMap.put(")", "\\)");
        escapeMap.put("-", "\\-");
        escapeMap.put("+", "\\+");
        escapeMap.put("=", "\\=");
        escapeMap.put("[", "\\[");
        escapeMap.put("]", "\\]");
        escapeMap.put("{", "\\{");
        escapeMap.put("}", "\\}");
        escapeMap.put("|", "\\|");
        escapeMap.put(":", "\\:");
        escapeMap.put(";", "\\;");
        escapeMap.put("\"", "\\\"");
        escapeMap.put("'", "\\'");
        escapeMap.put(".", "\\.");
        escapeMap.put("<", "\\<");
        escapeMap.put(">", "\\>");
        escapeMap.put("/", "\\/");
        escapeMap.put("?", "\\?");

        List<String> escapeFromList = new ArrayList<>();
        List<String> escapeToList = new ArrayList<>();

        escapeMap.forEach((k, v) -> {
            escapeFromList.add(k);
            escapeToList.add(v);
        });

        ESCAPE_TEXT_FORM = escapeFromList.toArray(new String[0]);
        ESCAPE_TEXT_TO = escapeToList.toArray(new String[0]);
    }

    /**
     * Escape text for shell script
     *
     * <pre>
     *  escapeShellText(null)        = null
     *  escapeShellText("")          = ""
     *  escapeShellText("     ")     = "\\ \\ \\ \\ \\ "
     *  escapeShellText("abc")       = "abc"
     *  escapeShellText("#123")      = "\\#123"
     *  escapeShellText("!@#$")      = "\\!@\\#\\$"
     *  escapeShellText(";init 0;")  = "\\;init\\ 0\\;"
     * </pre>
     */
    public static String escapeShellText(String text) {
        return StringUtils.replaceEach(text, ESCAPE_TEXT_FORM, ESCAPE_TEXT_TO);
    }
}
