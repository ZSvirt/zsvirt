package org.zstack.premium.externalservice.fluentbit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.zstack.log.server.LogServerStruct;

/**
 * @author shenjin
 * @date 2024/12/2
 */
public class FluentBitConfigurationStruct extends LogServerStruct {
	public static FluentBitConfigurationStruct fromJson(String content) {
		JsonObject object = new JsonParser().parse(content).getAsJsonObject();
		FluentBitConfigurationStruct struct = new FluentBitConfigurationStruct();
		struct.setType(getString(object, "type"));

		JsonElement configuration = object.get("configuration");
		if (configuration != null && !configuration.isJsonNull()) {
			struct.setConfiguration(configuration.isJsonPrimitive() ? configuration.getAsString() : configuration.toString());
		}

		return struct;
	}

	private static String getString(JsonObject object, String name) {
		JsonElement element = object.get(name);
		return element == null || element.isJsonNull() ? null : element.getAsString();
	}
}
