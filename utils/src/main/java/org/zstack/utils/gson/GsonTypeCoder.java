package org.zstack.utils.gson;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

public interface GsonTypeCoder<T> extends JsonDeserializer<T>, JsonSerializer<T> {

}
