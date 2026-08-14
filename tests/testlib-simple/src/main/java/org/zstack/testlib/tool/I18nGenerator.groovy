package org.zstack.testlib.tool

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.zstack.utils.path.PathUtil

class I18nGenerator {
    static void generateI18nFile() {
        String home = System.getProperty("user.dir")

        JsonElement element = JsonParser.parseReader(new FileReader(PathUtil.join(home, "../../conf/i18n.json")))
        // for each key in the json file
        for (Object o : element.getAsJsonArray()) {
            JsonObject obj = (JsonObject) o

            String key = obj.get("raw")
            String value = obj.get("zh_CN")
            System.out.println(key + " => " + value)
        }

        // replace the key with the value in the json file

        // write the new json file
    }
}
