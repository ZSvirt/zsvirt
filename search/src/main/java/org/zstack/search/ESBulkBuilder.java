package org.zstack.search;

import com.google.gson.Gson;
import org.zstack.header.search.InventoryDoc;
import org.zstack.utils.gson.GsonUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ESBulkBuilder {
    private class Bulk {
        private String _index;
        private String _type;
        private String _id;
    }
    private class Index {
        private Bulk index;
    }
    private class Delete {
        private Bulk delete;
    }
    
    private static Gson gson;
    static {
        GsonUtil gutil = new GsonUtil();
        gson = gutil.create();
    }
    
    private Map<Object, String> bulks = new HashMap<Object, String>(20);
    private Set<String> toIndexName = new HashSet<String>(5);
    private Set<String> toDeleteName = new HashSet<String>(5);
    private boolean empty = true;
    
    private Bulk createBulk(String indexName, String typeName, String id) {
        Bulk b = new Bulk();
        b._index = indexName;
        b._type = typeName;
        b._id = id;
        return b;
    }
    
    public ESBulkBuilder addIndexBulk(String indexName, String typeName, InventoryDoc doc) {
        Bulk b = createBulk(indexName, typeName, doc.getIndexId());
        Index i = new Index();
        i.index = b;
        bulks.put(i, doc.getDoc());
        toIndexName.add(typeName);
        empty = false;
        return this;
    }
    
    public ESBulkBuilder addDeleteBulk(String indexName, String typeName, String id) {
        Bulk b = createBulk(indexName, typeName, id);
        Delete d = new Delete();
        d.delete = b;
        bulks.put(d, "");
        toDeleteName.add(typeName);
        empty = false;
        return this;
    }
    
    public String build() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Object, String> e : bulks.entrySet()) {
            Object metaData = e.getKey();
            sb.append(gson.toJson(metaData));
            sb.append("\n");
            if (!"".equals(e.getValue())) {
                sb.append(e.getValue());
                sb.append("\n");
            }
        }
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return build();
    }
    
    public String getAffectedInventoryNames() {
        StringBuilder sb = new StringBuilder();
        sb.append("inventory to index: ").append(toIndexName.toString()).append(", ").append("inventory to delete: ").append(toDeleteName.toString());
        return sb.toString();
    }

    public boolean isEmpty() {
        return empty;
    }
}
