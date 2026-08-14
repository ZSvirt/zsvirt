package org.zstack.configuration;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by mingjian.deng on 2020/1/19.
 */
public class BusinessProperties {
    private static final CLogger logger = Utils.getLogger(BusinessProperties.class);

    private static Properties prop = new Properties();

    private static void loadBusinessProperties(String filename) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(filename))) {
            logger.debug(String.format("start load business properties: %s", filename));
            prop.load(inputStream);
        } catch (Exception e) {
            logger.debug(e.getMessage());
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    static {
        List<String> files = PathUtil.scanFolderOnClassPath("businessConfig");
        files.forEach(BusinessProperties::loadBusinessProperties);
    }

    public static String getProperties(String key) {
        return prop.getProperty(key);
    }

    public static List<String> getPropertiesAsList(String key) {
        List<String> res = new ArrayList<>();
        prop.forEach((k, v) -> {
            if (k.toString().contains(key)) {
                res.add(v.toString());
            }
        });
        return res;
    }
}
