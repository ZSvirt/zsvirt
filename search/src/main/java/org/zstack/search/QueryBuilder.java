package org.zstack.search;

import org.json.JSONException;
import org.json.JSONObject;
import org.zstack.header.exception.CloudRuntimeException;

import java.util.*;

public class QueryBuilder {
    private JSONObject obj = new JSONObject();
    private List<JSONObject> andList = new ArrayList<JSONObject>();
    private List<JSONObject> orList = new ArrayList<JSONObject>();
    private int from;
    private long size;
    private Set<String> fields = new HashSet<String>();
    
    private JSONObject nj() {
        return new JSONObject();
    }
    
    public QueryBuilder() {
    }
    
    private JSONObject term(String name, String val) {
        try {
            return nj().put("term", nj().put(name, val));
        } catch (JSONException e) {
            throw new CloudRuntimeException(e);
        }
    }
        
    public QueryBuilder andEQ(String name, String val) {
        andList.add(term(name, val));
        return this;
    }
    
    private JSONObject notTerm(String name, String val) {
        try {
            return nj().put("not", nj().put("term", nj().put(name, val)));
        } catch (JSONException e) {
            throw new CloudRuntimeException(e);
        }
    }
    
    public QueryBuilder andNotEQ(String name, String val) {
        andList.add(notTerm(name, val));
        return this;
    }
    
    private JSONObject gt(String name, String val) {
        try {
            return nj().put("range", nj().put(name, nj().put("gt", val)));
        } catch (JSONException e) {
            throw new CloudRuntimeException(e);
        }
    }
    
    public QueryBuilder andGT(String name, String val) {
        andList.add(gt(name, val));
        return this;
    }
    
    private JSONObject gte(String name, String val) {
        try {
            return nj().put("range", nj().put(name, nj().put("gte", val)));
        } catch (JSONException e) {
            throw new CloudRuntimeException(e);
        }
    }
    
    public QueryBuilder andGTE(String name, String val) {
        andList.add(gte(name, val));
        return this;
    }
    
    private JSONObject lt(String name, String val) {
        try {
            return nj().put("range", nj().put(name, nj().put("lt", val)));
        } catch (JSONException e) {
            throw new CloudRuntimeException(e);
        }
    }
    
    public QueryBuilder andLT(String name, String val) {
        andList.add(lt(name, val));
        return this;
    }
    
    private JSONObject lte(String name, String val) {
        try {
            return nj().put("range", nj().put(name, nj().put("lte", val)));
        } catch (JSONException e) {
            throw new CloudRuntimeException(e);
        }
    }
    
    public QueryBuilder andLTE(String name, String val) {
        andList.add(lte(name, val));
        return this;
    }
    
    private JSONObject in(String name, List<String> vals) {
        try {
            return nj().put("terms", nj().put(name, vals));
        } catch (JSONException e) {
            throw new CloudRuntimeException(e);
        }
    }
    
    public QueryBuilder andIn(String name, List<String> vals) {
        andList.add(in(name, vals));
        return this;
    }
    
    public JSONObject notIn(String name, List<String> vals) {
        try {
            return nj().put("not", nj().put("terms", nj().put(name, vals)));
        } catch (JSONException e) {
            throw new CloudRuntimeException(e);
        }
    }
    
    public QueryBuilder andNotIn(String name, List<String> vals) {
        andList.add(notIn(name, vals));
        return this;
    }
    
    public QueryBuilder orEQ(String name, String val) {
        orList.add(term(name, val));
        return this;
    }
    
    public QueryBuilder orNotEQ(String name, String val) {
        orList.add(notTerm(name, val));
        return this;
    }
    
    public QueryBuilder orGT(String name, String val) {
        orList.add(gt(name, val));
        return this;
    }
    
    public QueryBuilder orGTE(String name, String val) {
        orList.add(gte(name, val));
        return this;
    }
    
    public QueryBuilder orLT(String name, String val) {
        orList.add(lt(name, val));
        return this;
    }
    
    public QueryBuilder orLTE(String name, String val) {
        orList.add(lte(name, val));
        return this;
    }
    
    public QueryBuilder orIn(String name, List<String> vals) {
        orList.add(in(name, vals));
        return this;
    }
    
    public QueryBuilder orNotIn(String name, List<String> vals) {
        orList.add(notIn(name, vals));
        return this;
    }
    
    private void buildQueryObject() {
        try {
            obj.put("query", nj().put("filtered", nj().put("query", nj().put("match_all", nj()))));
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
    public String build() {
        try {
            buildQueryObject();
            List<JSONObject> globalOr = new ArrayList<JSONObject>(2);
            boolean isFilter = false;
            if (!andList.isEmpty()) {
                //filterObj.put("and", andList);
                globalOr.add(nj().put("and", andList));
                isFilter = true;
            }
            if (!orList.isEmpty()) {
                //filterObj.put("or", orList);
                globalOr.add(nj().put("or", orList));
                isFilter = true;
            }
            if (isFilter) {
                JSONObject filterObj = nj().put("or", globalOr);
                obj.put("filter", filterObj);
            }
            if (from != 0) {
                obj.put("from", from);
            }
            if (size != 0) {
                obj.put("size", size);
            }
            if (!fields.isEmpty()) {
                obj.put("fields", fields);
            }
            return obj.toString();
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
    
    @Override
    public String toString() {
        return build();
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
    
    public void select(String...fields) {
        Collections.addAll(this.fields, fields);
    }
}
