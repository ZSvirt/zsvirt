package org.zstack.guesttools;

import org.yaml.snakeyaml.Yaml;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.guesttools.pvpanic.PVPanicConstant.FEATURE_VERSION_FILE_PATH;

/**
 * Created by Wenhao.Zhang on 22/08/24
 */
public class GuestToolsFeatureHelper {
    private volatile static List<FeatureStruct> guestToolsFeatureVersionMap;

    public static List<FeatureStruct> getFeatureVersionMap() {
        if (guestToolsFeatureVersionMap == null) {
            synchronized (GuestToolsFeatureHelper.class) {
                if (guestToolsFeatureVersionMap == null) {
                    guestToolsFeatureVersionMap = fetchFeatureVersionMap(FEATURE_VERSION_FILE_PATH);
                }
            }
        }
        return guestToolsFeatureVersionMap;
    }

    private static List<FeatureStruct> fetchFeatureVersionMap(String filePath) {
        File mapperFile = PathUtil.findFileOnClassPath(filePath);
        if (mapperFile == null) {
            throw new CloudRuntimeException(
            String.format("failed to find mapper file: %s", filePath));
        }
        Map<String, Map<String, String>> map;
        try {
            map = new Yaml().load(Files.newInputStream(mapperFile.toPath()));
        } catch (IOException e) {
            throw new CloudRuntimeException(
            String.format("failed to load mapper file: %s", filePath), e);
        }

        return map.entrySet().stream()
                .map(entry -> new FeatureStruct(entry.getKey(), entry.getValue().get("version")))
                .collect(Collectors.toList());
    }

    public static class FeatureStruct {
        public FeatureStruct(String name, String version) {
            this.name = name;
            this.version = version;
        }
        public String name;
        public String version;
    }
}
