package org.zstack.core.jsonlabel;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Created by xing5 on 2016/9/13.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class JsonLabel {
    @Autowired
    private DatabaseFacade dbf;

    private static String LOCK = JsonLabel.class.getName();

    public JsonLabelInventory create(String key, Object obj, String resourceUuid) {
        JsonLabelVO vo = new JsonLabelVO();
        vo.setLabelKey(key);

        if (obj instanceof String) {
            vo.setLabelValue(obj.toString());
        } else {
            vo.setLabelValue(JSONObjectUtil.toJsonString(obj));
        }

        vo.setResourceUuid(resourceUuid);
        vo = dbf.persistAndRefresh(vo);
        return JsonLabelInventory.valueOf(vo);
    }

    public JsonLabelInventory create(String key, Object obj) {
        return create(key, obj, null);
    }

    @Deferred
    public JsonLabelInventory createIfAbsent(String key, Object obj, String resourceUuid) {
        GLock lock = new GLock(LOCK, TimeUnit.MINUTES.toSeconds(2));
        lock.lock();
        Defer.defer(lock::unlock);

        SimpleQuery<JsonLabelVO> q = dbf.createQuery(JsonLabelVO.class);
        q.add(JsonLabelVO_.labelKey, Op.EQ, key);
        JsonLabelVO vo = q.find();
        return vo == null ? create(key, obj, resourceUuid) : JsonLabelInventory.valueOf(vo);
    }

    public JsonLabelInventory createIfAbsent(String key, Object obj) {
        return createIfAbsent(key, obj, null);
    }

    public JsonLabelInventory get(String key) {
        JsonLabelVO vo = Q.New(JsonLabelVO.class).eq(JsonLabelVO_.labelKey, key).find();
        return vo == null ? null : JsonLabelInventory.valueOf(vo);
    }

    public <T> T get(String key, Class<T> clazz) {
        JsonLabelInventory inv = get(key);
        if (inv == null) {
            return null;
        }

        if (String.class.isAssignableFrom(clazz)) {
            return (T) inv.getLabelValue();
        } else {
            return JSONObjectUtil.toObject(inv.getLabelValue(), clazz);
        }
    }

    public <T, K extends Collection> Collection getAsCollection(String key, Class<K> collectionClass, Class<T> clazz) {
        JsonLabelInventory inv = get(key);
        if (inv == null) {
            return null;
        }

        return JSONObjectUtil.toCollection(inv.getLabelValue(), collectionClass, clazz);
    }

    public void delete(String key) {
        SQL.New(JsonLabelVO.class).eq(JsonLabelVO_.labelKey, key).hardDelete();
    }

    public boolean exists(String key) {
        return Q.New(JsonLabelVO.class).eq(JsonLabelVO_.labelKey, key).isExists();
    }

    @Deferred
    public boolean atomicExists(String key) {
        GLock lock = new GLock(LOCK, TimeUnit.MINUTES.toSeconds(2));
        lock.lock();
        Defer.defer(lock::unlock);

        return exists(key);
    }

    @Deferred
    public void atomicDelete(String key) {
        GLock lock = new GLock(LOCK, TimeUnit.MINUTES.toSeconds(2));
        lock.lock();
        Defer.defer(lock::unlock);

        delete(key);
    }
}
