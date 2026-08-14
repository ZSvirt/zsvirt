package org.zstack.utils.gson;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * author:kaicai.hu
 * Date:2020/5/6
 */
public class OffsetDateTimeGsonTypeCoder implements GsonTypeCoder<OffsetDateTime> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public JsonElement serialize(OffsetDateTime offsetDateTime, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(FORMATTER.format(offsetDateTime));
    }

    @Override
    public OffsetDateTime deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        return FORMATTER.parse(jsonElement.getAsString(), OffsetDateTime::from);
    }
}
