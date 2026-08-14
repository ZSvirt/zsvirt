package org.zstack.search;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.message.APIMessage;
import org.zstack.header.search.APISearchMessage;
import org.zstack.header.search.SearchOp;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.GsonUtil;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SearchQuery<T> {
    private static final CLogger logger = Utils.getLogger(SearchQuery.class);
    private static final Gson gson;

    static {
        gson = new GsonUtil().create();
    }

    @Autowired
    private InventoryIndexManager mgr;
    @Autowired
    private GlobalConfigFacade gcf;

    private URI uri;
    private QueryBuilder qb;
    private Class<?> inventoryClass;
    private int from;
    private long size;
    private String[] fields = new String[0];
    
    private static int USE_DEFAULT_SIZE = -99999;

    public SearchQuery(Class<?> invClass) {
        try {
            inventoryClass = invClass;
            uri = new URI(String.format("%s/%s/%s/_search", mgr.getElasticSearchBaseUrl(), invClass.getSimpleName().toLowerCase(), invClass.getSimpleName())
                    .replaceAll("(?<!:)//", "/"));
            qb = new QueryBuilder();
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException(e);
        }
    }

    public SearchQuery<T> add(String name, SearchOp op, String val) {
        if (op == SearchOp.AND_EQ) {
            qb.andEQ(name, val);
        } else if (op == SearchOp.AND_GT) {
            qb.andGT(name, val);
        } else if (op == SearchOp.AND_GTE) {
            qb.andGTE(name, val);
        } else if (op == SearchOp.AND_LT) {
            qb.andLT(name, val);
        } else if (op == SearchOp.AND_LTE) {
            qb.andLTE(name, val);
        } else if (op == SearchOp.AND_NOT_EQ) {
            qb.andNotEQ(name, val);
        } else if (op == SearchOp.OR_EQ) {
            qb.orEQ(name, val);
        } else if (op == SearchOp.OR_GT) {
            qb.orGT(name, val);
        } else if (op == SearchOp.OR_GTE) {
            qb.orGTE(name, val);
        } else if (op == SearchOp.OR_LT) {
            qb.orLT(name, val);
        } else if (op == SearchOp.OR_LTE) {
            qb.orLTE(name, val);
        } else if (op == SearchOp.OR_NOT_EQ) {
            qb.orNotEQ(name, val);
        } else {
            throw new IllegalArgumentException(String.format("%s is not vaild operator for this function, try another add() ???", op));
        }

        return this;
    }

    public SearchQuery<T> add(String name, SearchOp op, List<String> vals) {
        if (op == SearchOp.AND_IN) {
            qb.andIn(name, vals);
        } else if (op == SearchOp.AND_NOT_IN) {
            qb.andNotIn(name, vals);
        } else if (op == SearchOp.OR_IN) {
            qb.orIn(name, vals);
        } else if (op == SearchOp.OR_NOT_IN) {
            qb.orNotIn(name, vals);
        } else {
            throw new IllegalArgumentException(String.format("%s is not vaild operator for this function, try another add() ???", op));
        }

        return this;
    }

    public T find() {
        if (fields.length > 0) {
            throw new IllegalArgumentException(String.format("You have called SearchQuery.find(), call SearchQuery.findTuple() instead of SearchQuery.find()"));
        }

        List<T> lst = list();
        if (lst.size() > 1) {
            throw new IllegalArgumentException(String.format("more than one result found"));
        } else if (lst.size() == 0) {
            return null;
        }

        return lst.get(0);
    }

    private void build() {
        if (from != 0) {
            qb.setFrom(from);
        }
        if (size == USE_DEFAULT_SIZE) {
            //int defaultSize = Integer.valueOf(gcf.getConfigValue(SearchConstant.SearchGlobalConfig.DefaultSearchSize.getCategory(), SearchGlobalConfig.DefaultSearchSize.toString()));
            qb.setSize(10);
        } else if (size != 0) {
            qb.setSize(size);
        }
        if (fields.length > 0) {
            qb.select(fields);
        }
    }

    private String callElasticSearch() {
        try {
            build();
            HttpPost post = new HttpPost(uri);
            final String requestBody = qb.build();
            logger.trace(String.format("executing elasticsearch query as:\n%s", requestBody));
            StringEntity body = new StringEntity(requestBody);
            body.setChunked(false);
            post.setEntity(body);
            ResponseHandler<String> rspHandler = new ResponseHandler<String>() {
                @Override
                public String handleResponse(HttpResponse rsp) throws ClientProtocolException, IOException {
                    String res;
                    if (rsp.getStatusLine().getStatusCode() != HttpStatus.SC_OK && rsp.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                        logger.warn(String.format("Failed to search index[%s] , because: \nstatus line: %s\nbody: %s\nrequest body: %s",
                                inventoryClass.getSimpleName(), rsp.getStatusLine(), EntityUtils.toString(rsp.getEntity()), requestBody));
                        throw new IOException(String.format("Failed to search index[%s] because %s", inventoryClass.getSimpleName(), rsp.getStatusLine()));
                    } else {
                        res = EntityUtils.toString(rsp.getEntity());
                        logger.trace(String.format("Successfully search index[%s], %s", inventoryClass.getSimpleName(), res));
                    }

                    return res;
                }
            };

            String res = mgr.getHttpClient().execute(post, rspHandler);
            return res;
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage(), e);
        }
    }

    public List<T> list() {
        if (fields.length > 0) {
            throw new IllegalArgumentException(
                    String.format("You have called SearchQuery.select(), call SearchQuery.listTuple() instead of SearchQuery.list()"));
        }

        try {
            String res = callElasticSearch();
            JSONArray jarr = new JSONObject(res).getJSONObject("hits").getJSONArray("hits");
            List<T> rlst = new ArrayList<T>(jarr.length());
            for (int i = 0; i < jarr.length(); i++) {
                String source = jarr.getJSONObject(i).getString("_source");
                rlst.add((T) gson.fromJson(source, inventoryClass));
            }
            return rlst;
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage(), e);
        }

    }
    
    public String listAsString() {
        if (fields.length == 0) {
            return gson.toJson(list());
        } else {
            List<ESTuple> tuples = listTuple();
            List<Map<String, String>> lst = new ArrayList<Map<String, String>>(tuples.size());
            for (ESTuple t : tuples) {
                lst.add(t.getKeyValuePairs());
            }
            return gson.toJson(lst);
        }
    }

    public List<ESTuple> listTuple() {
        if (fields.length == 0) {
            throw new IllegalArgumentException(
                    String.format("You have not called SearchQuery.select(), call SearchQuery.list() instead of SearchQuery.listTuple()"));
        }

        try {
            String res = callElasticSearch();
            JSONArray jarr = new JSONObject(res).getJSONObject("hits").getJSONArray("hits");
            List<ESTuple> rlst = new ArrayList<ESTuple>(jarr.length());
            for (int i = 0; i < jarr.length(); i++) {
                JSONObject fs = jarr.getJSONObject(i).getJSONObject("fields");
                ESTuple tuple = new ESTuple(fields);
                for (String name : fields) {
                    tuple.put(name, fs.optString(name, null));
                }
                rlst.add(tuple);
            }
            return rlst;
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage(), e);
        }
    }

    public ESTuple findTuple() {
        if (fields.length == 0) {
            throw new IllegalArgumentException(
                    String.format("You have not called SearchQuery.select(), call SearchQuery.find() instead of SearchQuery.findTuple()"));
        }

        List<ESTuple> lst = listTuple();
        if (lst.size() > 1) {
            throw new IllegalArgumentException(String.format("more than one result found"));
        } else if (lst.size() == 0) {
            return null;
        }

        return lst.get(0);
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

    public void select(String... fields) {
        this.fields = fields;
    }
    
    public static <K> SearchQuery<K> create(APISearchMessage msg, Class<K> invClass) {
        SearchQuery<K> query = new SearchQuery<K>(invClass);
        if (!msg.getFields().isEmpty()) {
            query.select(msg.getFields().toArray(new String[msg.getFields().size()]));
        }
        for (APISearchMessage.NOVTriple t : msg.getNameOpValueTriples()) {
            query.add(t.getName(), SearchOp.valueOf(t.getOp()), t.getVal());
        }
        for (APISearchMessage.NOLTriple tl : msg.getNameOpListTriples()) {
            query.add(tl.getName(), SearchOp.valueOf(tl.getOp()), tl.getVals());
        }
        if (msg.getStart() > 0) {
            query.setFrom(msg.getStart());
        }
        if (msg.getSize() > 0) {
            query.setSize(msg.getSize());
        } else {
            query.setSize(USE_DEFAULT_SIZE);
        }
        return query;
    }
    
    public void addAccountAsAnd(APIMessage msg) {
        if (!msg.getSession().getAccountUuid().equals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)) {
            this.add("accountUuid", SearchOp.AND_EQ, msg.getSession().getAccountUuid());
        }
    }
}
