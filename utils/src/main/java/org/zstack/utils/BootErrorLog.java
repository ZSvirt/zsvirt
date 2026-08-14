package org.zstack.utils;

import org.apache.commons.io.FileUtils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;

/**
 * Created by frank on 2/17/2016.
 */
public class BootErrorLog {
    private static final CLogger logger = Utils.getLogger(BootErrorLog.class);
    private String bootErrorPath = PathUtil.join(PathUtil.getZStackHomeFolder(), "bootError.log");

    public void write(String log) {
        try {
            FileUtils.writeStringToFile(new File(bootErrorPath), log);
        } catch (IOException e) {
            logger.warn(String.format("unable to write error to %s", bootErrorPath));
        }
    }
}
