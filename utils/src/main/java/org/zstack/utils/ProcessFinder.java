package org.zstack.utils;

import org.apache.commons.io.FileUtils;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xing5 on 2016/7/8.
 */
public class ProcessFinder {
    static class Proc {
        File procFolder;
        int pid;
    }

    private List<Proc> getAllProcessFolders() {
        List<Proc> procs = new ArrayList<>();

        File[] files = new File("/proc/").listFiles();
        if (files == null) {
            return procs;
        }

        for (File f : files) {
            if (!f.isDirectory()) {
                continue;
            }

            try {
                Proc p = new Proc();
                p.pid = Integer.parseInt(f.getName());
                p.procFolder = f;
                procs.add(p);
            } catch (NumberFormatException e) {
                // pass
            }
        }

        return procs;
    }

    public boolean processExists(int pid) {
        return new File(String.format("/proc/%s", pid)).exists();
    }

    public Integer findByCommandLineKeywords(String...words) {
        for (Proc p : getAllProcessFolders()) {
            File cmdline = new File(PathUtil.join(p.procFolder.getAbsolutePath(), "cmdline"));
            if (!cmdline.exists()) {
                continue;
            }

            try {
                String cmdlineContent = FileUtils.readFileToString(cmdline);
                int count = 0;
                for (String word : words) {
                    if (cmdlineContent.contains(word)) {
                        count ++;
                    }
                }

                if (count == words.length) {
                    return p.pid;
                }
            } catch (IOException ignore) {}
        }

        return null;
    }
}
