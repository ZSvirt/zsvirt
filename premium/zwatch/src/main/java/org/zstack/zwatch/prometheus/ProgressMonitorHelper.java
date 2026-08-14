package org.zstack.zwatch.prometheus;

import org.apache.commons.io.FileUtils;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.utils.Bash;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

/**
 * Created by mingjian.deng on 2020/4/7.
 */
public class ProgressMonitorHelper {
    private static final CLogger logger = Utils.getLogger(ProgressMonitorHelper.class);
    private static long hertz;

    private static long getConfigHZ() {
        long[] hz = new long[1];
        new Bash() {
            @Override
            @ExceptionSafe
            protected void scripts() {
                setE();
                sudoRun("grep -w CONFIG_HZ /boot/config-`uname -r`");
                String[] s = stdout().trim().split("=");
                if (s.length > 1) {
                    hz[0] = Long.valueOf(s[1]);
                }
            }
        }.execute();
        if (hz[0] > 0) {
            return hz[0];
        } else {
            return 1000;
        }
    }

    public static ProgressStatus getProgressStatus(String pid) {
        if (hertz == 0) {
            hertz = getConfigHZ();
        }
        ProgressStatus status = new ProgressStatus();
        File pidFile = new File(String.format("/proc/%s/stat", pid));

        if (pidFile.exists() && pidFile.isFile()) {
            try {
                BasicFileAttributes attr = Files.readAttributes(pidFile.toPath(), BasicFileAttributes.class);
                status.lasts = attr.creationTime().to(TimeUnit.SECONDS);
            } catch (IOException e) {
                logger.warn(e.getLocalizedMessage());
            }
        } else {
            status.existed = false;
            return status;
        }

        File socketDir = new File(String.format("/proc/%s/fd", pid));
        File[] socketFiles = socketDir.listFiles();
        if (socketDir.exists() && socketFiles != null) {
            double sockets = 0;
            for (File s: socketFiles) {
                try {
                    if (Files.readSymbolicLink(s.toPath()).toString().startsWith("socket:")) {
                        sockets ++;
                    }
                } catch (IOException e) {
                    logger.debug(e.getLocalizedMessage());
                }
            }
            status.sockets = sockets;
        }

        File proc = new File(String.format("/proc/%s/status", pid));
        if (proc.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(proc.toPath())))) {
                String line = null;
                while((line = br.readLine()) != null) {
                    if (line.contains("VmRSS")) {
                        String[] mems = line.trim().split("\\s+");
                        if (mems.length > 2){
                            status.memoryExpends = Double.parseDouble(mems[1]);
                        }
                        break;
                    }
                }
            } catch (IOException | NumberFormatException e) {
                logger.warn(e.getLocalizedMessage());
            }
        }

        try {
            // See: https://stackoverflow.com/questions/16726779/how-do-i-get-the-total-cpu-usage-of-an-application-from-proc-pid-stat
            String uptime = FileUtils.readFileToString(new File("/proc/uptime")).split("\\s+")[0];
            String[] cpuspends = FileUtils.readFileToString(pidFile).split("\\s+");
            logger.debug(JSONObjectUtil.toJsonString(cpuspends));
            double total = Double.parseDouble(cpuspends[13]) + Double.parseDouble(cpuspends[14]) + Double.parseDouble(cpuspends[15]) + Double.parseDouble(cpuspends[16]);
            double seconds = Double.parseDouble(uptime) - (Double.parseDouble(cpuspends[21]) / hertz);
            BigDecimal s = BigDecimal.valueOf(total * 100 / hertz / seconds).setScale(2, BigDecimal.ROUND_DOWN);
            status.cpuExpends = s.doubleValue();
        } catch (IOException | NumberFormatException e) {
            logger.warn(e.getLocalizedMessage());
        }

//        new Bash() {
//            @Override
//            @ExceptionSafe
//            protected void scripts() {
//                setE();
////                run("ps -aux | grep %s|grep -v grep", pid);
//                run("top -b -n 1 -p %s|tail -n 1", pid);
//                String[] cpus = stdout().trim().split("\\s+");
//                if (cpus.length > 9){
//                    logger.debug(JSONObjectUtil.toJsonString(cpus));
//                    status.cpuExpends = Double.valueOf(cpus[8]);
//                }
//            }
//        }.execute();

        return status;
    }

    public static Double getDiskSpaceMB(String path) {
        long sizeMB = FileUtils.sizeOfDirectory(new File(path)) / 1048576;

        return Double.valueOf(String.format("%.1f", (double)sizeMB));
    }
}
