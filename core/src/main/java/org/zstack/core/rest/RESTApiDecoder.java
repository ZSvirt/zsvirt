package org.zstack.core.rest;

import com.google.gson.*;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.header.rest.APINoSee;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.GsonTypeCoder;
import org.zstack.utils.gson.GsonUtil;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Type;
import java.util.Map;

import static org.zstack.core.Platform.operr;

public class RESTApiDecoder {
    private static CLogger logger = Utils.getLogger(RESTApiDecoder.class);
    
    private final Gson gsonEncoder;
    private final Gson gsonDecoder;
    private static final RESTApiDecoder self;
    
    private class Encoder implements GsonTypeCoder<Message>, ExclusionStrategy {
        private Gson gson;
        
        void setGson(Gson gson) {
            this.gson = gson;
        }
        
        @Override
        public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jObj = json.getAsJsonObject();
            Map.Entry<String, JsonElement> entry = jObj.entrySet().iterator().next();
            String className = entry.getKey();
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new JsonParseException("Unable to deserialize class " + className, e);
            }
            if (!Message.class.isAssignableFrom(clazz)) {
                throw new CloudRuntimeException(String.format("Unable to deserialize class %s", className));
            }
            return (Message) this.gson.fromJson(entry.getValue(), clazz);
        }

        @Override
        public JsonElement serialize(Message msg, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jObj = new JsonObject();
            jObj.add(msg.getClass().getName(), this.gson.toJsonTree(msg));
            return jObj;
        }

        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return f.getAnnotation(APINoSee.class) != null;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }
    
    private class Decoder implements GsonTypeCoder<Message> {
        private Gson gson;
        
        void setGson(Gson gson) {
            this.gson = gson;
        }
        
        @Override
        public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jObj = json.getAsJsonObject();
            Map.Entry<String, JsonElement> entry = jObj.entrySet().iterator().next();
            String className = entry.getKey();
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new JsonParseException("Unable to deserialize class " + className, e);
            }
            if (!Message.class.isAssignableFrom(clazz)) {
                throw new CloudRuntimeException(String.format("Unable to deserialize class %s", className));
            }
            Message msg = (Message) this.gson.fromJson(entry.getValue(), clazz);
            return msg;
        }

        @Override
        public JsonElement serialize(Message msg, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jObj = new JsonObject();
            jObj.add(msg.getClass().getName(), this.gson.toJsonTree(msg));
            return jObj;
        }
        
    }
    
    static {
        self = new RESTApiDecoder();
    }
    
    private RESTApiDecoder() {
        Encoder encoder = new Encoder();
        gsonEncoder = new GsonUtil().setCoder(Message.class, encoder)
                .setExclusionStrategies(new ExclusionStrategy[]{encoder})
                .enableComplexMapKeySerialization()
                .create();
        encoder.setGson(gsonEncoder);
        
        Decoder decoder = new Decoder();
        gsonDecoder = new GsonUtil().setCoder(Message.class, decoder).create();
        decoder.setGson(gsonDecoder);
    }
    
    public static Message loads(String jsonStr) {
        Message msg = self.gsonDecoder.fromJson(jsonStr, Message.class);
        return msg;
    }
    
    public static String dump(Message msg) {
        return self.gsonEncoder.toJson(msg, Message.class);
    }
}
