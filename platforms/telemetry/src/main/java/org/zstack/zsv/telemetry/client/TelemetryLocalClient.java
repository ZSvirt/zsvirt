package org.zstack.zsv.telemetry.client;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.zsv.telemetry.TelemetryConstant;
import org.zstack.zsv.telemetry.collect.TelemetryDailyReport;
import org.zstack.zsv.telemetry.header.TelemetryGlobalConfig;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class TelemetryLocalClient {
    private static final CLogger logger = Utils.getLogger(TelemetryLocalClient.class);

    public TelemetryLocalClient ensureDirectories() {
        mkDir(rootDir());
        mkDir(reportsDir());
        mkDir(pendingDir());
        return this;
    }

    public String saveReport(TelemetryDailyReport report) {
        ensureDirectories();
        String reportJson = JSONObjectUtil.toJsonString(report);
        String fileName = reportFileName(report.snapshotDate);
        File reportFile = new File(PathUtil.join(reportsDir(), fileName));

        deleteExistingReportIfPresent(reportFile);
        writeFile(reportFile, reportJson);
        keepOnlyLatestReport(reportFile);
        copyToPending(reportFile);
        enforceCapacityLimit();
        return reportJson;
    }

    public void writeSyncedReport(String snapshotDate, String fileName, String reportJson) {
        ensureDirectories();
        File reportFile = new File(PathUtil.join(reportsDir(), fileName));
        deleteExistingReportIfPresent(reportFile);
        writeFile(reportFile, reportJson);
        keepOnlyLatestReport(reportFile);
        copyToPending(reportFile);
        enforceCapacityLimit();
        logger.debug(String.format("wrote synced report for snapshotDate=%s", snapshotDate));
    }

    public List<File> listPendingReports() {
        ensureDirectories();
        File pending = new File(pendingDir());
        File[] files = pending.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return new ArrayList<>();
        }
        return Arrays.stream(files)
                .sorted(Comparator.comparingLong(File::lastModified))
                .collect(Collectors.toList());
    }

    public String readFile(File file) throws IOException {
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    public void deletePending(File pendingFile) {
        if (pendingFile != null && pendingFile.exists()) {
            FileUtils.deleteQuietly(pendingFile);
        }
    }

    public void deletePendingByFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return;
        }
        ensureDirectories();
        deletePending(new File(PathUtil.join(pendingDir(), fileName)));
    }

    public String reportFileName(String snapshotDate) {
        return String.format("daily-report-%s.json", snapshotDate);
    }

    public String rootDir() {
        return TelemetryConstant.LOCAL_ROOT_DIR;
    }

    public String reportsDir() {
        return PathUtil.join(rootDir(), TelemetryConstant.LOCAL_REPORTS_DIR);
    }

    public String pendingDir() {
        return PathUtil.join(rootDir(), TelemetryConstant.LOCAL_PENDING_DIR);
    }

    private void copyToPending(File reportFile) {
        if (reportFile == null || !reportFile.exists()) {
            return;
        }
        File dest = new File(PathUtil.join(pendingDir(), reportFile.getName()));
        try {
            FileUtils.copyFile(reportFile, dest);
            PathUtil.setFilePosixPermissions(dest, TelemetryConstant.LOCAL_DIR_PERMISSION);
        } catch (IOException e) {
            logger.warn(String.format("failed to copy report to pending: %s", e.getMessage()), e);
        }
    }

    private void deleteExistingReportIfPresent(File reportFile) {
        if (reportFile != null && reportFile.exists()) {
            FileUtils.deleteQuietly(reportFile);
        }
    }

    /**
     * reports/ keeps only the latest daily report (one file). Older files are deleted,
     * not moved to pending/ — pending/ is the sole upload-state source via copyToPending.
     */
    private void keepOnlyLatestReport(File currentReport) {
        if (currentReport == null) {
            return;
        }
        String keepName = currentReport.getName();
        for (File other : listFiles(reportsDir())) {
            if (keepName.equals(other.getName())) {
                continue;
            }
            if (FileUtils.deleteQuietly(other)) {
                logger.debug(String.format("deleted archived report (not re-queued): %s", other.getName()));
            }
        }
    }

    /**
     * Trim under {@code telemetry.local.max.bytes}. Evict oldest pending first, then oldest
     * reports except the newest one (reports/ must keep one latest daily report).
     */
    private void enforceCapacityLimit() {
        long limit = TelemetryGlobalConfig.LOCAL_MAX_BYTES.value(Long.class);
        long size = sizeOf(new File(rootDir()));
        if (size <= limit) {
            return;
        }

        List<File> candidates = new ArrayList<>(listFiles(pendingDir()));
        candidates.sort(Comparator.comparingLong(File::lastModified));

        List<File> reports = listFiles(reportsDir());
        File newestReport = reports.stream()
                .max(Comparator.comparingLong(File::lastModified))
                .orElse(null);
        List<File> oldReports = reports.stream()
                .filter(f -> newestReport == null || !f.equals(newestReport))
                .sorted(Comparator.comparingLong(File::lastModified))
                .collect(Collectors.toList());
        candidates.addAll(oldReports);

        for (File f : candidates) {
            if (size <= limit) {
                break;
            }
            long fileSize = f.length();
            if (FileUtils.deleteQuietly(f)) {
                size -= fileSize;
                logger.debug(String.format("deleted old file for capacity: %s", f.getAbsolutePath()));
            }
        }

        if (size > limit) {
            logger.warn(String.format(
                    "telemetry local size %d still exceeds limit %d after eviction (kept latest report)",
                    size, limit));
        }
    }

    private List<File> listFiles(String dirPath) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles(File::isFile);
        if (files == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(files));
    }

    private void writeFile(File file, String content) {
        try {
            FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8, false);
            PathUtil.setFilePosixPermissions(file, TelemetryConstant.LOCAL_DIR_PERMISSION);
        } catch (IOException e) {
            throw new CloudRuntimeException(String.format("failed to write telemetry file %s: %s",
                    file.getAbsolutePath(), e.getMessage()), e);
        }
    }

    private void mkDir(String path) {
        File dir = new File(path);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new CloudRuntimeException(String.format("failed to create telemetry dir: %s", path));
        }
        PathUtil.setFilePosixPermissions(dir, TelemetryConstant.LOCAL_DIR_PERMISSION);
    }

    private long sizeOf(File file) {
        if (file == null || !file.exists()) {
            return 0L;
        }
        if (file.isFile()) {
            return file.length();
        }
        File[] children = file.listFiles();
        if (children == null) {
            return 0L;
        }
        long total = 0L;
        for (File child : children) {
            total += sizeOf(child);
        }
        return total;
    }
}
