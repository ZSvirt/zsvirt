package org.zstack.core.cloudbus;

import com.google.gson.*;
import org.zstack.core.log.LogSafeGson;
import org.zstack.header.message.GsonTransient;
import org.zstack.header.message.Message;
import org.zstack.utils.gson.GsonTypeCoder;
import org.zstack.utils.gson.GsonUtil;
import org.zstack.utils.gson.OffsetDateTimeGsonTypeCoder;

import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.util.*;

public class CloudBusGson {
    private static Gson gson = new GsonUtil().setCoder(Message.class, new GsonTypeCoder<Message>() {

        @Override
        public JsonElement serialize(Message message, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject jObj = new JsonObject();
            jObj.add(message.getClass().getName(), gson.toJsonTree(message));
            return jObj;
        }

        @Override
        public Message deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jObj = jsonElement.getAsJsonObject();
            Map.Entry<String, JsonElement> entry = jObj.entrySet().iterator().next();
            String className = entry.getKey();
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new JsonParseException(String.format("Unable to deserialize class[%s]", className), e);
            }
            return (Message) gson.fromJson(entry.getValue(), clazz);
        }
    }).setCoder(OffsetDateTime.class, new OffsetDateTimeGsonTypeCoder()).setExclusionStrategies(new ExclusionStrategy[]{
            new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                    return fieldAttributes.getAnnotation(GsonTransient.class) != null;
                }

                @Override
                public boolean shouldSkipClass(Class<?> aClass) {
                    return false;
                }
            }
    }).enableComplexMapKeySerialization().create();

    private static Gson logSafeGson = new GsonUtil().setCoder(Message.class, new GsonTypeCoder<Message>() {

        @Override
        public JsonElement serialize(Message message, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject msg = LogSafeGson.toJsonElement(message).getAsJsonObject();
            JsonObject jObj = new JsonObject();
            jObj.add(message.getClass().getName(), msg);
            return jObj;
        }

        @Override
        public Message deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jObj = jsonElement.getAsJsonObject();
            Map.Entry<String, JsonElement> entry = jObj.entrySet().iterator().next();
            String className = entry.getKey();
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new JsonParseException(String.format("Unable to deserialize class[%s]", className), e);
            }
            return (Message) logSafeGson.fromJson(entry.getValue(), clazz);
        }
    }).setCoder(OffsetDateTime.class, new OffsetDateTimeGsonTypeCoder()).setSerializationExclusionStrategy(new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return f.getAnnotation(GsonTransient.class) != null;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }).enableComplexMapKeySerialization().create();

    private static Gson httpGson = new GsonUtil().setCoder(Message.class, new GsonTypeCoder<Message>() {

        @Override
        public JsonElement serialize(Message message, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject jObj = new JsonObject();
            jObj.add(message.getClass().getName(), gson.toJsonTree(message));
            return jObj;
        }

        @Override
        public Message deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jObj = jsonElement.getAsJsonObject();
            Map.Entry<String, JsonElement> entry = jObj.entrySet().iterator().next();
            String className = entry.getKey();
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new JsonParseException(String.format("Unable to deserialize class[%s]", className), e);
            }
            return (Message) gson.fromJson(entry.getValue(), clazz);
        }
    }).setCoder(OffsetDateTime.class, new OffsetDateTimeGsonTypeCoder()).setExclusionStrategies(new ExclusionStrategy[]{
            new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                    GsonTransient g = fieldAttributes.getAnnotation(GsonTransient.class);
                    if (g != null && g.transientInHttpResponse()) {
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean shouldSkipClass(Class<?> aClass) {
                    return false;
                }
            }
    }).enableComplexMapKeySerialization().create();

    public static Message fromJson(String json) {
        return gson.fromJson(json, Message.class);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    public static String toJson(Message msg) {
        return gson.toJson(msg, Message.class);
    }

    public static String toLogSafeJson(Message msg) {
        return logSafeGson.toJson(msg, Message.class);
    }

    public static String toJson(Object obj) {
        if (obj instanceof Message) {
            return gson.toJson(obj, Message.class);
        } else {
            return gson.toJson(obj);
        }
    }

    public static String toJsonForHttpResponse(Object obj) {
        if (obj instanceof Message) {
            return httpGson.toJson(obj, Message.class);
        } else {
            return httpGson.toJson(obj);
        }
    }
}
