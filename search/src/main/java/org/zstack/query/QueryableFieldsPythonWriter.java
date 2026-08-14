package org.zstack.query;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 */
public class QueryableFieldsPythonWriter {
    private String outputFolder;
    private Map<String, List<String>> queryables;

    private final static String FILE_NAME = "zstack_queryables.py";

    public QueryableFieldsPythonWriter(String outputFolder, Map<String, List<String>> queryables) {
        this.outputFolder = outputFolder;
        this.queryables = queryables;
        if (this.outputFolder == null) {
            this.outputFolder = PathUtil.join(System.getProperty("user.home"), "zstack-python-template/python/");
        }
    }

    public void write() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> e : queryables.entrySet()) {
            sb.append(String.format("\n# %s", e.getKey()));
            String[] namePairs = e.getKey().split("\\.");
            String name = namePairs[namePairs.length-1];
            sb.append(String.format("\n%s = [", name));
            for (String f : e.getValue()) {
                sb.append(String.format("\n%s'%s',", StringUtils.repeat(" ", 4), f));
            }
            sb.append(String.format("]\n\n"));
        }

        File f = new File(PathUtil.join(outputFolder, FILE_NAME));
        try {
            FileUtils.write(f, sb.toString());
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }
    }
}
