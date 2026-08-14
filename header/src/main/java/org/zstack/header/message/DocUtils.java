package org.zstack.header.message;

import org.zstack.utils.StringDSL;
import org.zstack.utils.string.Uuid;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by lining on 2017/11/14.
 */
public class DocUtils {
    private static Map<String, Integer> apiUuidMap = new HashMap<>();

    public static final long date = 1510669257141L;

    public static String uuidForAPIDoc(){
        String apiName = new Throwable().getStackTrace()[2].getClassName();

        if(!apiUuidMap.containsKey(apiName)){
            apiUuidMap.put(apiName, 0);
        }

        Integer index = apiUuidMap.get(apiName);
        String uuidKey = apiName + index;
        String uuid = Uuid.generate(uuidKey.getBytes());

        apiUuidMap.put(apiName, ++index);
        return uuid;
    }

    public static void removeApiUuidMap(String apiName){
        apiUuidMap.remove(apiName);
    }

    public static String createFixedUuid(Class<?> voClass) {
        return StringDSL.createFixedUuid(voClass);
    }

    public static Timestamp timestamp() {
        return new Timestamp(date);
    }

    public static long dateInSeconds() {
        return date / 1000;
    }

    public static long dateInSecondsAndAddDays(int days) {
        return dateInSeconds() + TimeUnit.DAYS.toSeconds(days);
    }

    public static long dateInSecondsAndAddSeconds(int seconds) {
        return dateInSeconds() + TimeUnit.SECONDS.toSeconds(seconds);
    }
}
