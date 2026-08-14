package org.zstack.test.unittest.utils;

import org.junit.Test;
import org.zstack.configuration.OfferingUserConfigUtils;
import org.zstack.header.configuration.userconfig.InstanceOfferingUserConfig;

/**
 * Created by lining on 2019/5/8.
 */
public class OfferingUserConfigUtilsCase {
    @Test
    public void testConfigStrToObj() {
        String userConfig = "{\n" +
                "    \"allocate\": {\n" +
                "        \"primaryStorage\": {\n" +
                "            \"type\": \"localstorage\", \n" +
                "            \"uuid\": \"c56d0ff8d24f4f119837742d4658aa83\"\n" +
                "        },\n" +
                "        \"clusterUuid\": \"a54d0ff8d24f4f119837742d4658aa83\"" +
                "    }, \n" +
                "    \"displayAttribute\": {\n" +
                "        \"rootVolume\": {\n" +
                "            \"云盘类型\": \"高速云盘\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        InstanceOfferingUserConfig config = OfferingUserConfigUtils.toObject(userConfig, InstanceOfferingUserConfig.class);

        userConfig = "{\n" +
                "    \"allocate\": {\n" +
                "        \"primaryStorage\": {\n" +
                "            \"type\": \"ceph\", \n" +
                "            \"uuid\": \"c56d0ff8d24f4f119837742d4658aa83\"\n" +
                "        }\n" +
                "    }, \n" +
                "    \"displayAttribute\": {\n" +
                "        \"rootVolume\": {\n" +
                "            \"云盘类型\": \"高速云盘\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        config = OfferingUserConfigUtils.toObject(userConfig, InstanceOfferingUserConfig.class);

        userConfig = "{\n" +
                "    \"allocate\": {\n" +
                "        \"primaryStorage\": {\n" +
                "            \"type\": \"sharedblock\", \n" +
                "            \"uuid\": \"c56d0ff8d24f4f119837742d4658aa83\"\n" +
                "        }\n" +
                "    }, \n" +
                "    \"displayAttribute\": {\n" +
                "        \"rootVolume\": {\n" +
                "            \"云盘类型\": \"高速云盘\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        config = OfferingUserConfigUtils.toObject(userConfig, InstanceOfferingUserConfig.class);
    }
}
