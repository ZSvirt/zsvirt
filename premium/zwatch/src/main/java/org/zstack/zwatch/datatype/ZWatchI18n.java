package org.zstack.zwatch.datatype;

import org.apache.commons.io.FileUtils;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by mingjian.deng on 2020/5/6.
 */
public class ZWatchI18n {
    private static final File zwatchMetricsCNFile = PathUtil.findFileOnClassPath("zwatch/zwatch_i18n_cn");
    private static final File zwatchMetricsENFile = PathUtil.findFileOnClassPath("zwatch/zwatch_i18n_en");
    private static List<String> zwatchMetricsCN;
    private static List<String> zwatchMetricsEN;

    static {
        try {
            zwatchMetricsCN = FileUtils.readLines(zwatchMetricsCNFile);
        } catch (IOException e) {
            throw new RuntimeException("cannot find zwatch_i18n_cn in classpath");
        }

        try {
            zwatchMetricsEN = FileUtils.readLines(zwatchMetricsENFile);
        } catch (IOException e) {
            throw new RuntimeException("cannot find zwatch_i18n_en in classpath");
        }
    }

    public static String generateDescriptionFromName(String namespace, String name) {
        String en = "";
        String cn = "";
        String key = namespace + "/" + name;
        boolean lack = true;
        for (String line: zwatchMetricsEN) {
            if (line.contains(key)) {
                en = line.split(":")[1].trim();
            }
        }
        if (en.isEmpty()) {
            try {
                synchronized (zwatchMetricsENFile) {
                    FileUtils.writeStringToFile(zwatchMetricsENFile, String.format("\n%s: %s", key, name), true);
                    en = name;
                }
            } catch (IOException e) {
                throw new CloudRuntimeException(String.format("cannot write %s to file: %s", key, zwatchMetricsENFile.getAbsolutePath()));
            }
        }


        for (String line: zwatchMetricsCN) {
            if (line.contains(key)) {
                cn = line.split(":")[1].trim();
                if (!cn.isEmpty()) {
                    lack = false;
                }
            }
        }

        if (lack) {
            try {
                synchronized (zwatchMetricsCNFile) {
                    FileUtils.writeStringToFile(zwatchMetricsCNFile, String.format("\n%s: ", key), true);
                }
            } catch (IOException e) {
                throw new CloudRuntimeException(String.format("cannot write %s to file: %s", key, zwatchMetricsCNFile.getAbsolutePath()));
            }
        }

        switch (CoreGlobalProperty.LOCALE) {
            case "en_US" : {
                return en;
            }
            case "zh_CN" : case "zh_HANT" : {
                if (lack) {
                    return en;
                }
                return cn;
            }
            default: return en;
        }
    }
}
