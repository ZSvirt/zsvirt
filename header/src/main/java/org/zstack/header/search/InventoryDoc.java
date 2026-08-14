package org.zstack.header.search;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import org.zstack.header.rest.APINoSee;
import org.zstack.utils.gson.GsonUtil;

public class InventoryDoc {
    private static Gson gson;
    private String inventoryName;
    private String indexId;
    private String doc;

    private static class APINoSeeFilter implements ExclusionStrategy {
        @Override
        public boolean shouldSkipClass(Class<?> arg0) {
            return false;
        }

        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return f.getAnnotation(APINoSee.class) != null;
        }
    }

    static {
        GsonUtil gutil = new GsonUtil();
        gson = gutil.setExclusionStrategies(new ExclusionStrategy[]{new APINoSeeFilter()}).create();
    }

    public String getIndexId() {
        return indexId;
    }

    public void setIndexId(String indexId) {
        this.indexId = indexId;
    }

    public String getDoc() {
        return doc;
    }

    public void setDoc(String doc) {
        this.doc = doc;
    }

    public static InventoryDoc toDoc(String inventoryName, String indexId, Object inventory) {
        InventoryDoc doc = new InventoryDoc();
        doc.setInventoryName(inventoryName);
        doc.setIndexId(indexId);
        doc.setDoc(gson.toJson(inventory));
        return doc;
    }

    public static Gson getGson() {
        return gson;
    }

    public String getInventoryName() {
        return inventoryName;
    }

    public void setInventoryName(String inventoryName) {
        this.inventoryName = inventoryName;
    }
}
