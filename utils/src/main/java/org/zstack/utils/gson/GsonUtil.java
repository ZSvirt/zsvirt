package org.zstack.utils.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;

public class GsonUtil {
    GsonBuilder _gsonBuilder;
    
    public GsonUtil() {
        _gsonBuilder = new GsonBuilder();
    }
    
    public GsonUtil setCoder(Class<?> clazz, GsonTypeCoder<?> coder) {
        _gsonBuilder.registerTypeAdapter(clazz, coder);
        return this;
    }

    public GsonUtil setExclusionStrategies(ExclusionStrategy[] excludeStrateges) {
         _gsonBuilder.setExclusionStrategies(excludeStrateges);
         return this; 
    }

    public GsonUtil setSerializationExclusionStrategy(ExclusionStrategy excludeStratege) {
        _gsonBuilder.addSerializationExclusionStrategy(excludeStratege);
        return this;
    }
    
    public GsonUtil setInstanceCreator(Class<?> clazz, Object creator) {
        _gsonBuilder.registerTypeAdapter(clazz, creator);
        return this;
    }

    public GsonUtil enableComplexMapKeySerialization() {
        _gsonBuilder.enableComplexMapKeySerialization();
        return this;
    }
    
    public GsonUtil enableNullDecoder() {
        _gsonBuilder.serializeNulls();
        return this;
    }

    public GsonUtil setDateFormat(String dateFormat) {
        _gsonBuilder.setDateFormat(dateFormat);
        return this;
    }
    
    public Gson create() {
        //TODO: configuration database
        _gsonBuilder.setVersion(1.7);
        return _gsonBuilder.create();
    }
}