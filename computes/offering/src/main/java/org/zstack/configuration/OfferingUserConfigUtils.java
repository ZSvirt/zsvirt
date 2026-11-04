package org.zstack.configuration;

import com.google.gson.*;
import org.springframework.util.StringUtils;
import org.zstack.header.configuration.userconfig.DiskOfferingUserConfig;
import org.zstack.header.configuration.userconfig.InstanceOfferingUserConfig;
import org.zstack.header.storage.primary.PrimaryStorageAllocateConfig;
import org.zstack.header.storage.primary.PrimaryStorageAllocateConfigType;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import java.lang.reflect.Type;

import static org.zstack.configuration.DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG;

/**
 * Created by lining on 2019/4/17.
 */
public class OfferingUserConfigUtils {
    private static final Gson gson;

    static {
        gson = new GsonBuilder().registerTypeAdapter(PrimaryStorageAllocateConfig.class, new JsonDeserializer<PrimaryStorageAllocateConfig>() {
            @Override
            public PrimaryStorageAllocateConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                String type = json.getAsJsonObject().get("type").getAsString();
                PrimaryStorageAllocateConfig config = context.deserialize(json, PrimaryStorageAllocateConfigType.getByProductCategory(type).getType());
                return config;
            }
        }).create();
    }

    public final static InstanceOfferingUserConfig getInstanceOfferingConfig(String instanceOfferingUuid) {
        String configStr = InstanceOfferingSystemTags.INSTANCE_OFFERING_USER_CONFIG
                .getTokenByResourceUuid(instanceOfferingUuid, InstanceOfferingSystemTags.INSTANCE_OFFERING_USER_CONFIG_TOKEN);
        DebugUtils.Assert(!StringUtils.isEmpty(configStr), "instanceOffering userConfig is null");

        InstanceOfferingUserConfig config = gson.fromJson(configStr, InstanceOfferingUserConfig.class);
        return config;
    }

    public static <T extends InstanceOfferingUserConfig> T getInstanceOfferingConfig(String instanceOfferingUuid, Class<T> clazz){
        String configStr = InstanceOfferingSystemTags.INSTANCE_OFFERING_USER_CONFIG
                .getTokenByResourceUuid(instanceOfferingUuid, InstanceOfferingSystemTags.INSTANCE_OFFERING_USER_CONFIG_TOKEN);
        DebugUtils.Assert(!StringUtils.isEmpty(configStr), "instanceOffering userConfig is null");

        T config = gson.fromJson(configStr, clazz);
        return config;
    }

    public final static DiskOfferingUserConfig getDiskOfferingConfig(String diskOfferingUuid) {
        String configStr = DISK_OFFERING_USER_CONFIG.getTokenByResourceUuid(diskOfferingUuid, DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG_TOKEN);
        DebugUtils.Assert(!StringUtils.isEmpty(configStr), "diskOffering userConfig is null");

        DiskOfferingUserConfig config = gson.fromJson(configStr, DiskOfferingUserConfig.class);
        return config;
    }

    public static <T extends DiskOfferingUserConfig> T getDiskOfferingConfig(String diskOfferingUuid, Class<T> clazz){
        String configStr = DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG
                .getTokenByResourceUuid(diskOfferingUuid, DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG_TOKEN);
        DebugUtils.Assert(!StringUtils.isEmpty(configStr), "diskOffering userConfig is null");

        T config = gson.fromJson(configStr, clazz);
        return config;
    }

    public static <T> T toObject(String content, Class<T> clazz){
        return gson.fromJson(content, clazz);
    }
}
