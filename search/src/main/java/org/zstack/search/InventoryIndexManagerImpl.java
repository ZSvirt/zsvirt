package org.zstack.search;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.TransactionalCallback;
import org.zstack.header.AbstractService;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.search.*;
import org.zstack.utils.Utils;
import org.zstack.utils.data.Pair;
import org.zstack.utils.logging.CLogger;

import javax.persistence.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

public class InventoryIndexManagerImpl extends AbstractService implements InventoryIndexManager, TransactionalCallback {
    private static final CLogger logger = Utils.getLogger(InventoryIndexManagerImpl.class);
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry rgty;

    private Map<String, IndexerInfo> voClassToIndexerMapping = new HashMap<String, IndexerInfo>();
    private Set<Class<?>> triggerVOs = new HashSet<Class<?>>();
    private Set<String> basePkgNames;
    private String elasticSearchBaseUrl = "http://localhost:9200/";
    private HttpClient httpClient;
    private URI bulkUri;
    private boolean stopIfCreateIndexFailed = false;
    private boolean deleteAllIndexWhenStart = false;
    // key: parent class, value: children classes
    private Map<Class<?>, List<Class<?>>> sqlTriggerInheritance = new HashMap<Class<?>, List<Class<?>>>();

    // key: child class, value: parent class names
    private Map<Class<?>, List<String>> insertVOTriggerClassNames = new HashMap<Class<?>, List<String>>();
    // key: parent class, value: children class names
    private Map<Class<?>, List<String>> deleteVOTriggerClassNames = new HashMap<Class<?>, List<String>>();
    private List<SearchIndexRecreateExtensionPoint> reindexExts = new ArrayList<SearchIndexRecreateExtensionPoint>();

    private class IndexerInfo {
        String url;
        Class<?> inventoryClass;
        Class<?> mappingVOClass;
        Method valueOfMethod;
        String inventoryName;
        Field entityIdField;
    }

    private Method getValueOfMethodOfInventoryClass(Class<?> invClass) throws NoSuchMethodException {
        for (Method m : invClass.getMethods()) {
            if ("valueOf".equals(m.getName())) {
                int mod = m.getModifiers();
                if (!(Modifier.isStatic(mod) && Modifier.isPublic(mod))) {
                    continue;
                }

                Class<?>[] params = m.getParameterTypes();
                if (params.length != 1) {
                    continue;
                }

                Class<?> voClass = params[0];
                if (!voClass.isAnnotationPresent(Entity.class)) {
                    continue;
                }

                return m;
            }
        }

        String err = String
                .format("Class[%s] is annotated by @Inventory, it must have a public static method called 'valueOf' which receives a single parmater, the parameter must have type of entity class that is annotated by @Entity.\n\te.g. public staitc HostInventory.valueOf(HostVO vo)",
                        invClass.getName());
        throw new NoSuchMethodException(err);
    }

    private void popluateTriggerVONamesCascade(Class<?> triggerVOClazz) {
        Class<?> parent = triggerVOClazz.getSuperclass();
        List<Class<?>> parents = new ArrayList<Class<?>>();
        while (parent != Object.class) {
            parents.add(parent);
            parent = parent.getSuperclass();
        }

        List<String> insertVOtriggerNames = new ArrayList<String>();
        if (!parents.isEmpty()) {
            for (Class<?> p : parents) {
                insertVOtriggerNames.add(p.getSimpleName());
            }
        }
        insertVOtriggerNames.add(triggerVOClazz.getSimpleName());
        insertVOTriggerClassNames.put(triggerVOClazz, insertVOtriggerNames);

        if (!parents.isEmpty()) {
            for (Class<?> p : parents) {
                List<String> deleteVOTriggerNames = deleteVOTriggerClassNames.get(p);
                if (deleteVOTriggerNames == null) {
                    deleteVOTriggerNames = new ArrayList<String>();
                    deleteVOTriggerNames.add(p.getSimpleName());
                    deleteVOTriggerClassNames.put(p, deleteVOTriggerNames);
                }
                deleteVOTriggerNames.add(triggerVOClazz.getSimpleName());
            }
        }
    }

    private void populateTriggerVOs() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
        scanner.addIncludeFilter(new AnnotationTypeFilter(TriggerIndex.class));
        scanner.addExcludeFilter(new AnnotationTypeFilter(Controller.class));
        for (String pkg : getBasePkgNames()) {
            for (BeanDefinition bd : scanner.findCandidateComponents(pkg)) {
                Class<?> triggerVO = Class.forName(bd.getBeanClassName());
                if (!triggerVO.isAnnotationPresent(Entity.class)) {
                    throw new IllegalArgumentException(String.format("Class[%s] is annotated by @TriggerIndex, but not annotated by @Entity",
                            triggerVO.getName()));
                }
                triggerVOs.add(triggerVO);
                popluateTriggerVONamesCascade(triggerVO);
            }
        }
    }

    private void populateInventoryIndexer() throws URISyntaxException, ClassNotFoundException, NoSuchMethodException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Inventory.class));
        scanner.addExcludeFilter(new AnnotationTypeFilter(Controller.class));
        for (String pkg : getBasePkgNames()) {
            for (BeanDefinition bd : scanner.findCandidateComponents(pkg)) {
                Class<?> inventoryClass = Class.forName(bd.getBeanClassName());
                Inventory invat = inventoryClass.getAnnotation(Inventory.class);
                if (!triggerVOs.contains(invat.mappingVOClass())) {
                    String err = String.format("Inventory[%s]'s mapping VO class[%s] is not annotated by @TriggerIndex", inventoryClass.getName(), invat
                            .mappingVOClass().getName());
                    throw new IllegalArgumentException(err);
                }

                String mappingClassSimpleName = invat.mappingVOClass().getSimpleName();
                IndexerInfo info = voClassToIndexerMapping.get(mappingClassSimpleName);
                if (info == null) {
                    String invName = inventoryClass.getSimpleName();
                    info = new IndexerInfo();
                    info.url = String.format("%s/%s/%s", elasticSearchBaseUrl, invName.toLowerCase(), invName);
                    info.inventoryClass = inventoryClass;
                    info.inventoryName = invName;
                    info.mappingVOClass = invat.mappingVOClass();
                    info.valueOfMethod = getValueOfMethodOfInventoryClass(inventoryClass);
                    info.entityIdField = getEntityIdFieldFromClass(info.mappingVOClass);
                    info.entityIdField.setAccessible(true);
                    voClassToIndexerMapping.put(mappingClassSimpleName, info);
                }
            }
        }
    }

    private void dumpInventoryIndexer() {
        StringBuilder sb = new StringBuilder("InventoryIndexer dump:");
        for (Entry<String, IndexerInfo> e : voClassToIndexerMapping.entrySet()) {
            sb.append(String.format("\nTriggered VO: %s", e.getKey()));
            IndexerInfo info = e.getValue();
            sb.append(String.format("\n\tInventory class: %s", info.inventoryClass.getName()));
            sb.append(String.format("\n\tMapping VO class: %s", info.mappingVOClass.getName()));
            sb.append(String.format("\n\turl: %s\n", info.url));
        }

        logger.debug(sb.toString());
    }

    public Set<String> getBasePkgNames() {
        if (basePkgNames == null) {
            basePkgNames = new HashSet<String>();
            basePkgNames.add("org.zstack");
        }
        return basePkgNames;
    }

    public void setBasePkgNames(Set<String> basePkgNames) {
        this.basePkgNames = basePkgNames;
    }

    private URI makeURI(String... paths) throws URISyntaxException {
        StringBuilder sb = new StringBuilder();
        for (String p : paths) {
            sb.append(p);
            sb.append("/");
        }
        return new URI(sb.toString().replaceAll("(?<!:)//", "/"));
    }

    private JSONObject createIndexSetting() throws JSONException {
        JSONObject root = new JSONObject();
        root.put(
                "settings",
                new JSONObject().put(
                        "index",
                        new JSONObject().put("analysis",
                                new JSONObject().put("analyzer", new JSONObject().put("default", new JSONObject().put("type", "keyword")))).put(
                                "refresh_interval", "500")));
        return root;
    }

    private void doCreateIndexIfNotExists(String inventoryName) throws URISyntaxException, ClientProtocolException, IOException, JSONException {
        final String indexName = inventoryName.toLowerCase();
        URI uri = makeURI(elasticSearchBaseUrl, indexName);
        HttpHead head = new HttpHead(uri);
        ResponseHandler<Boolean> rspHandler = new ResponseHandler<Boolean>() {
            @Override
            public Boolean handleResponse(HttpResponse rsp) throws ClientProtocolException, IOException {
                if (rsp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    return true;
                } else if (rsp.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                    return false;
                } else {
                    throw new ClientProtocolException(String.format("Failed to check index[%s] existence", indexName));
                }
            }
        };

        boolean ret = httpClient.execute(head, rspHandler);
        if (!ret) {
            HttpPut put = new HttpPut(uri);
            String body = createIndexSetting().toString();
            put.setEntity(new StringEntity(body));
            ResponseHandler<Void> putHandler = new ResponseHandler<Void>() {
                @Override
                public Void handleResponse(HttpResponse rsp) throws ClientProtocolException, IOException {
                    if (rsp.getStatusLine().getStatusCode() != HttpStatus.SC_OK && rsp.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                        throw new ClientProtocolException(String.format("Cannot create index[%s] beacuse %s, %s", indexName, rsp.getStatusLine(),
                                EntityUtils.toString(rsp.getEntity())));
                    } else {
                        logger.debug(String.format("Successfully created index[%s]", indexName));
                    }
                    return null;
                }
            };
            httpClient.execute(put, putHandler);
        }
    }

    private void createIndexIfNotExists() throws Exception {
        for (IndexerInfo info : voClassToIndexerMapping.values()) {
            try {
                doCreateIndexIfNotExists(info.inventoryName);
            } catch (Exception ex) {
                if (stopIfCreateIndexFailed) {
                    throw ex;
                } else {
                    logger.warn(ex.getMessage(), ex);
                }
            }
        }
    }

    private void populateExtensions() {
        reindexExts = rgty.getExtensionList(SearchIndexRecreateExtensionPoint.class);
    }

    @Override
    public boolean start() {
        try {
            httpClient = new DefaultHttpClient(new PoolingClientConnectionManager());
            bulkUri = makeURI(elasticSearchBaseUrl, "_bulk");

            /* only for debugging */
            if (deleteAllIndexWhenStart) {
                try {
                    deleteIndex(null);
                } catch (Exception ex) {
                    logger.warn(String.format("Failed to delete all index"), ex);
                }
            }

            populateExtensions();
            populateTriggerVOs();
            populateInventoryIndexer();
            dumpInventoryIndexer();
            createIndexIfNotExists();
            bus.registerService(this);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        return true;
    }

    @Override
    public boolean stop() {
        if (httpClient != null) {
            httpClient.getConnectionManager().shutdown();
        }
        bus.unregisterService(this);
        return true;
    }

    @Override
    public void suspend(Class<?>... entityClass) {
    }

    @Override
    public void resume(Class<?>... entityClass) {
    }

    @Override
    public void flush(Class<?>... entityClass) {
    }

    @Override
    public void beforeCommit(Operation op, boolean readOnly, Class<?>... entityClass) {

    }

    @Override
    public void beforeCompletion(Operation op, Class<?>... entityClass) {
    }

    private void sendBulk(final String requestBody, final String inventoryName) {
        try {
            HttpPost post = new HttpPost(bulkUri);
            StringEntity body = new StringEntity(requestBody);
            logger.trace(String.format("%s:\n%s", inventoryName, requestBody));
            body.setChunked(false);
            post.setEntity(body);
            ResponseHandler<Void> rspHandler = new ResponseHandler<Void>() {
                @Override
                public Void handleResponse(HttpResponse rsp) throws ClientProtocolException, IOException {
                    if (rsp.getStatusLine().getStatusCode() != HttpStatus.SC_OK && rsp.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                        logger.warn(String.format("Failed to do bulk operation on[%s] , because: \nstatus line: %s\nresponse body: %s\nrequest body: %s",
                                inventoryName, rsp.getStatusLine(), EntityUtils.toString(rsp.getEntity()), requestBody));
                    } else {
                        logger.trace(String.format("Successfully did bulk operation on[%s], %s", inventoryName, EntityUtils.toString(rsp.getEntity())));
                    }
                    return null;
                }
            };
            httpClient.execute(post, rspHandler);
        } catch (Exception e) {
            logger.warn(String.format("Failed to do bulk operation on inventory[%s]", inventoryName), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private List<InsertVO> takeInsertVO(Class<?> triggeredVO) {
        List<String> voNames = insertVOTriggerClassNames.get(triggeredVO);
        TypedQuery<InsertVO> query = null;
        if (voNames.size() == 1) {
            String sql = "select i from InsertVO i where i.voName = :voName";
            query = dbf.getEntityManager().createQuery(sql, InsertVO.class);
            query.setParameter("voName", triggeredVO.getSimpleName());
        } else {
            String sql = "select i from InsertVO i where i.voName in (:voName)";
            query = dbf.getEntityManager().createQuery(sql, InsertVO.class);
            query.setParameter("voName", voNames);
        }
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        List<InsertVO> ret = query.getResultList();
        if (!ret.isEmpty()) {
            List<Long> ids = transformAndRemoveNull(ret, InsertVO::getId);
            String usql = "delete from InsertVO i where i.id in :id";
            Query uquery = dbf.getEntityManager().createQuery(usql);
            uquery.setParameter("id", ids);
            uquery.executeUpdate();
        }
        return ret;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private List<UpdateVO> takeUpdateVO(Class<?> triggeredVO) {
        String sql = "select i from UpdateVO i where i.voName = :voName";
        TypedQuery<UpdateVO> query = dbf.getEntityManager().createQuery(sql, UpdateVO.class);
        query.setParameter("voName", triggeredVO.getSimpleName());
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        List<UpdateVO> ret = query.getResultList();
        if (!ret.isEmpty()) {
            List<Long> ids = transformAndRemoveNull(ret, UpdateVO::getId);
            String usql = "delete from UpdateVO i where i.id in :id";
            Query uquery = dbf.getEntityManager().createQuery(usql);
            uquery.setParameter("id", ids);
            uquery.executeUpdate();
        }
        return ret;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private List<DeleteVO> takeDeleteVO(Class<?> triggeredVO) {
        List<String> voNames = deleteVOTriggerClassNames.get(triggeredVO);
        TypedQuery<DeleteVO> query = null;
        if (voNames == null) {
            String sql = "select i from DeleteVO i where i.voName = :voName";
            query = dbf.getEntityManager().createQuery(sql, DeleteVO.class);
            query.setParameter("voName", triggeredVO.getSimpleName());
        } else {
            String sql = "select i from DeleteVO i where i.voName in (:voName)";
            query = dbf.getEntityManager().createQuery(sql, DeleteVO.class);
            query.setParameter("voName", voNames);
        }
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        List<DeleteVO> ret = query.getResultList();
        if (!ret.isEmpty()) {
            List<Long> ids = transformAndRemoveNull(ret, DeleteVO::getId);
            String usql = "delete from DeleteVO i where i.id in :id";
            Query uquery = dbf.getEntityManager().createQuery(usql);
            uquery.setParameter("id", ids);
            uquery.executeUpdate();
        }
        return ret;
    }

    private Map<String, Set<String>> getUuidsOfVOToIndexFromInsertVOUpdateVO(Class<?> triggeredVO, Operation op) {
        Map<String, Set<String>> ret = new HashMap<String, Set<String>>();
        if (op == Operation.PERSIST) {
            List<InsertVO> ivos = takeInsertVO(triggeredVO);
            for (InsertVO ivo : ivos) {
                Set<String> self = ret.get(ivo.getVoName());
                if (self == null) {
                    self = new HashSet<String>();
                    ret.put(ivo.getVoName(), self);
                }
                self.add(ivo.getUuid());

                if (ivo.getForeignVOName() != null && ivo.getForeignVOUuid() != null) {
                    Set<String> foreign = ret.get(ivo.getForeignVOName());
                    if (foreign == null) {
                        foreign = new HashSet<String>();
                        ret.put(ivo.getForeignVOName(), foreign);
                    }
                    foreign.add(ivo.getForeignVOUuid());
                }
            }
        } else if (op == Operation.UPDATE) {
            List<UpdateVO> uvos = takeUpdateVO(triggeredVO);
            for (UpdateVO uvo : uvos) {
                Set<String> self = ret.get(uvo.getVoName());
                if (self == null) {
                    self = new HashSet<String>();
                    ret.put(uvo.getVoName(), self);
                }
                self.add(uvo.getUuid());

                if (uvo.getForeignVOName() != null) {
                    if (uvo.getForeignVOUuid() == null) {
                        String err = String.format("%s[uuid:%s]'s foreignVOname[%s] is not null but foreignVOUuid is null. This is largely the foreignVOname which foreignVOUuid points to has been deleted. otherwise some bug happened", uvo.getVoName(), uvo.getUuid(),
                                uvo.getForeignVOName());
                        logger.warn(err);
                        continue;
                    }

                    Set<String> foreign = ret.get(uvo.getForeignVOName());
                    if (foreign == null) {
                        foreign = new HashSet<String>();
                        ret.put(uvo.getForeignVOName(), foreign);
                    }
                    foreign.add(uvo.getForeignVOUuid());
                }
            }
        }

        return ret;
    }

    private Pair<Map<String, Set<String>>, Map<String, Set<String>>> getVOUuidsToDeleteOrIndexFromDeleteVO(Class<?> triggeredVO) {
        Pair<Map<String, Set<String>>, Map<String, Set<String>>> pair = new Pair<Map<String, Set<String>>, Map<String, Set<String>>>();
        Map<String, Set<String>> toIndex = new HashMap<String, Set<String>>();
        Map<String, Set<String>> toDelete = new HashMap<String, Set<String>>();

        List<DeleteVO> dvos = takeDeleteVO(triggeredVO);
        for (DeleteVO dvo : dvos) {
            Set<String> self = toDelete.get(dvo.getVoName());
            if (self == null) {
                self = new HashSet<String>();
                toDelete.put(dvo.getVoName(), self);
            }
            self.add(dvo.getUuid());

            if (dvo.getForeignVOToDeleteName() != null) {
                if (dvo.getForeignVOToDeleteUuid() == null) {
                    String err = String.format("%s[uuid:%s]'s foreignVOToDeleteName[%s] is not null but foreignVOToDeleteUuid is null. It's largely because the foreignVO has been deleted. otherwise it's a bug", dvo.getVoName(),
                            dvo.getUuid(), dvo.getForeignVOToDeleteName());
                    logger.debug(err);
                }
                Set<String> duuids = toDelete.get(dvo.getForeignVOToDeleteName());
                if (duuids == null) {
                    duuids = new HashSet<String>();
                    toDelete.put(dvo.getForeignVOToDeleteName(), duuids);
                }
                duuids.add(dvo.getForeignVOToDeleteUuid());
            }

            if (dvo.getForeignVOName() != null) {
                if (dvo.getForeignVOUuid() == null) {
                    String err = String.format("%s[uuid:%s]'s foreignVOname[%s] is not null but foreignVOUuid is null. It's largely because the foreignVO has been deleted. otherwise it's a bug", dvo.getVoName(), dvo.getUuid(),
                            dvo.getForeignVOName());
                    logger.debug(err);
                }

                Set<String> foreign = toIndex.get(dvo.getForeignVOName());
                if (foreign == null) {
                    foreign = new HashSet<String>();
                    toIndex.put(dvo.getForeignVOName(), foreign);
                }
                foreign.add(dvo.getForeignVOUuid());
            }
        }

        pair.set(toDelete, toIndex);
        return pair;
    }

    @SuppressWarnings("rawtypes")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private List getEntityToIndex(IndexerInfo info, Set<String> uuids) {
        String sql = String.format("select e from %s e where e.uuid in :uuids", info.mappingVOClass.getSimpleName());
        TypedQuery query = dbf.getEntityManager().createQuery(sql, info.mappingVOClass);
        query.setParameter("uuids", uuids);
        List res = query.getResultList();
        return res;
    }

    @SuppressWarnings("rawtypes")
    private List<InventoryDoc> buildDoc(IndexerInfo info, Set<String> uuids) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        List res = getEntityToIndex(info, uuids);
        if (res.isEmpty()) {
            logger.warn(String.format("Cannot find entities whose uuid in %s from %s table, index failed", uuids, info.mappingVOClass.getSimpleName()));
            return new ArrayList<InventoryDoc>(0);
        }

        List<InventoryDoc> docs = new ArrayList<InventoryDoc>(res.size());
        for (Object entity : res) {
            String uuid = (String) info.entityIdField.get(entity);
            InventoryDoc doc = InventoryDoc.toDoc(info.inventoryName, uuid, info.valueOfMethod.invoke(info.inventoryClass, entity));
            docs.add(doc);
        }
        return docs;
    }

    private ESBulkBuilder addDocToIndexToESBuilder(ESBulkBuilder bbuilder, Map<String, Set<String>> vmap) {
        try {
            for (Map.Entry<String, Set<String>> e : vmap.entrySet()) {
                IndexerInfo info = voClassToIndexerMapping.get(e.getKey());
                if (info == null) {
                    String err = String
                            .format("%s has an entry in InsertVO/UpdateVO table, but there is no indexer matching it. It can be intended if the entity inherits from another entity but itself is not wanted to be indexed. Otherwise it's most likely a bug",
                                    e.getKey());
                    logger.warn(err);
                    continue;
                }

                List<InventoryDoc> docs = buildDoc(info, e.getValue());
                if (docs.isEmpty()) {
                    continue;
                }

                for (InventoryDoc doc : docs) {
                    bbuilder.addIndexBulk(doc.getInventoryName().toLowerCase(), doc.getInventoryName(), doc);
                }
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }

        return bbuilder;
    }

    private ESBulkBuilder addDocToDeleteToESBuilder(ESBulkBuilder bbuilder, Map<String, Set<String>> vmap) {
        for (Map.Entry<String, Set<String>> et : vmap.entrySet()) {
            String voName = et.getKey();
            IndexerInfo info = voClassToIndexerMapping.get(voName);
            if (info == null) {
                /* it's a RefVO for ManyToMany mapping */
                continue;
            }

            Set<String> uuids = et.getValue();
            if (!uuids.isEmpty()) {
                for (String uuid : uuids) {
                    bbuilder.addDeleteBulk(info.inventoryName.toLowerCase(), info.inventoryName, uuid);
                }
            }
        }
        return bbuilder;
    }

    private IndexerInfo getIndexerInfoByInventoryName(String inventoryName) {
        for (IndexerInfo info : voClassToIndexerMapping.values()) {
            if (info.inventoryName.equals(inventoryName)) {
                return info;
            }
        }

        throw new CloudRuntimeException(String.format("cannot find IndexerInfo for inventory[%s]", inventoryName));
    }

    private void reindexInventory(String inventoryName, Set<String> uuids) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        IndexerInfo info = getIndexerInfoByInventoryName(inventoryName);

        ESBulkBuilder bbuilder = new ESBulkBuilder();
        List<InventoryDoc> docs = buildDoc(info, uuids);
        if (docs.isEmpty()) {
            return;
        }

        for (InventoryDoc doc : docs) {
            bbuilder.addIndexBulk(doc.getInventoryName().toLowerCase(), doc.getInventoryName(), doc);
        }

        if (!bbuilder.isEmpty()) {
            sendBulk(bbuilder.toString(), bbuilder.getAffectedInventoryNames());
            logger.debug(String.format("successfully reindex all data for inventory[%s]", inventoryName));
        }
    }

    private void fireSearchIndexRecreateExtension(String invName) {
        for (SearchIndexRecreateExtensionPoint extp : reindexExts) {
            try {
                List<String> lst = extp.returnUuidToReindex(invName);
                if (lst == null || lst.isEmpty()) {
                    continue;
                }

                Set<String> uuids = new HashSet<String>(lst.size());
                uuids.addAll(lst);
                reindexInventory(invName, uuids);
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    @Override
    public void afterCommit(Operation op, Class<?>... entityClass) {
        ESBulkBuilder bbuilder = new ESBulkBuilder();

        for (Class<?> vo : entityClass) {
            if (!triggerVOs.contains(vo)) {
                logger.trace(String.format("Class[%s] is not annotated by @TriggerIndex, no index operation will be proceeded", vo.getName()));
                continue;
            }

            if (op == Operation.PERSIST || op == Operation.UPDATE) {
                Map<String, Set<String>> vmap = getUuidsOfVOToIndexFromInsertVOUpdateVO(vo, op);
                bbuilder = addDocToIndexToESBuilder(bbuilder, vmap);
            } else if (op == Operation.REMOVE) {
                Pair<Map<String, Set<String>>, Map<String, Set<String>>> pair = getVOUuidsToDeleteOrIndexFromDeleteVO(vo);
                Map<String, Set<String>> toIndex = pair.second();
                bbuilder = addDocToIndexToESBuilder(bbuilder, toIndex);
                Map<String, Set<String>> toDelete = pair.first();
                bbuilder = addDocToDeleteToESBuilder(bbuilder, toDelete);
            }
        }

        if (!bbuilder.isEmpty()) {
            sendBulk(bbuilder.toString(), bbuilder.getAffectedInventoryNames());
        }
    }

    @Override
    public void afterCompletion(Operation op, int status, Class<?>... entityClass) {
    }

    public void setElasticSearchBaseUrl(String elasticSearchBaseUrl) {
        this.elasticSearchBaseUrl = elasticSearchBaseUrl;
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg instanceof APIMessage) {
                handleApiMessage((APIMessage) msg);
            } else {
                handleLocalMessage(msg);
            }
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg, e);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) throws Exception {
        if (msg instanceof APIDeleteSearchIndexMsg) {
            handle((APIDeleteSearchIndexMsg) msg);
        } else if (msg instanceof APICreateSearchIndexMsg) {
            handle((APICreateSearchIndexMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APICreateSearchIndexMsg msg) throws Exception {
        APICreateSearchIndexEvent evt = new APICreateSearchIndexEvent(msg.getId());

        if (!msg.getInventoryNames().isEmpty()) {
            Set<String> validInventoryNames = getAllInventoryNames();

            for (String invname : msg.getInventoryNames()) {
                if (!validInventoryNames.contains(invname)) {
                    //ErrorCodeFacade.setErrorToApiEvent(ErrorCodeFacade.BuiltinErrors.INVALID_ARGRUMENT.toString(), String.mediaType("zstack doesn't have this inventory[%s]", invname), evt);
                    bus.publish(evt);
                    return;
                }
            }

            for (String invname : msg.getInventoryNames()) {
                if (msg.isRecreate()) {
                    deleteIndex(invname);
                }

                doCreateIndexIfNotExists(invname);

                if (msg.isRecreate()) {
                    fireSearchIndexRecreateExtension(invname);
                }
            }
        } else {
            if (msg.isRecreate()) {
                deleteAllIndex();
            }

            createIndexIfNotExists();

            if (msg.isRecreate()) {
                for (String invname : getAllInventoryNames()) {
                    fireSearchIndexRecreateExtension(invname);
                }
            }
        }

        bus.publish(evt);
    }

    private Field getEntityIdFieldFromClass(Class<?> clazz) {
        Class<?> c = clazz;
        do {
            for (Field f : c.getDeclaredFields()) {
                if (f.isAnnotationPresent(Id.class)) {
                    return f;
                }
            }

            c = c.getSuperclass();
        } while (c != null && c != Object.class);
        throw new CloudRuntimeException(String.format("class[%s] doesn't have a field annotated by %s, is it a JPA @Entity class???", clazz.getName(), Id.class.getName()));
    }

    private void generateParentSqlTriggerInheritance(Class<?> clazz) {
        if (clazz.getSuperclass() == Object.class) {
            return;
        }

        Class<?> parent = clazz.getSuperclass();
        do {
            if (!parent.isAnnotationPresent(Inheritance.class)) {
                throw new CloudRuntimeException(String.format("@Entity class %s inherits %s, but %s doesn't have @Inheritance annotation", clazz.getName(), parent.getName(), parent.getName()));
            }

            List<Class<?>> parents = sqlTriggerInheritance.get(parent);
            if (parents == null) {
                parents = new ArrayList<Class<?>>();
                sqlTriggerInheritance.put(parent, parents);
            }
            parents.add(clazz);
            parent = parent.getSuperclass();
        } while (parent != Object.class);
    }

    private List<String> getForeignChildVOName(Class<?> clazz) {
        List<String> childNames = new ArrayList<String>();

        List<Class<?>> children = sqlTriggerInheritance.get(clazz);
        if (children != null) {
            for (Class<?> child : children) {
                if (!child.isAnnotationPresent(SqlTrigger.class)) {
                    continue;
                }

                String childVOName = child.getSimpleName();
                childNames.add(childVOName);
            }
        }
        return childNames;
    }

    private void doGenerateSqlTriggerText(SqlTrigger trigger, Class<?> clazz, StringBuilder sb) {
        String voName = clazz.getSimpleName();
        String idName = getEntityIdFieldFromClass(clazz).getName();

        String foreignVOName = null;
        String foreignVOUuid = null;
        List<String> foreignVONames = null;

        String foreignVOToDeleteName = null;
        String foreignVOToDeleteUuid = null;
        List<String> foreignVOToDeleteNames = null;

        if (trigger.foreignVOClass() != Object.class) {
            foreignVOName = trigger.foreignVOClass().getSimpleName();
            foreignVOUuid = trigger.foreignVOJoinColumn();
            if (foreignVOUuid.equals("")) {
                throw new CloudRuntimeException(String.format("@SqlTrigger of %s has foreignVOClass set to %s, but foreignVOJoinColumn is empty", clazz.getName(), trigger.foreignVOClass().getName()));
            }
            foreignVONames = getForeignChildVOName(trigger.foreignVOClass());
            foreignVONames.add(foreignVOName);
        }

        if (trigger.foreignVOToDeleteClass() != Object.class) {
            foreignVOToDeleteName = trigger.foreignVOToDeleteClass().getSimpleName();
            foreignVOToDeleteUuid = trigger.foreignVOToDeleteJoinColumn();
            if (foreignVOToDeleteUuid.equals("")) {
                throw new CloudRuntimeException(String.format("@SqlTrigger of %s has foreignVOToDeleteClass set to %s, but foreignVOToDeleteJoinColumn is empty", clazz.getName(), trigger.foreignVOToDeleteClass().getName()));
            }
            foreignVOToDeleteNames = getForeignChildVOName(trigger.foreignVOToDeleteClass());
            foreignVOToDeleteNames.add(foreignVOToDeleteName);
        }

        String insertTriggerName = String.format("%sInsertTrigger", voName);
        sb.append(String.format("\nDROP TRIGGER IF EXISTS `zstack`.%s;", insertTriggerName));
        sb.append(String.format("\nCREATE TRIGGER `zstack`.%s AFTER INSERT ON `zstack`.`%s`", insertTriggerName, voName));
        sb.append(String.format("\nFOR EACH ROW BEGIN"));
        if (foreignVONames != null) {
            for (String name : foreignVONames) {
                sb.append(String.format("\n\tINSERT INTO InsertVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('%s', new.%s, '%s', new.%s);", voName, idName, name, foreignVOUuid));
            }
        } else {
            sb.append(String.format("\n\tINSERT INTO InsertVO (voName, uuid) VALUES ('%s', new.%s);", voName, idName));
        }
        sb.append("\nEND|\n");

        String updateTriggerName = String.format("%sUpdateTrigger", voName);
        sb.append(String.format("\nDROP TRIGGER IF EXISTS `zstack`.%s;", updateTriggerName));
        sb.append(String.format("\nCREATE TRIGGER `zstack`.%s AFTER UPDATE ON `zstack`.`%s`", updateTriggerName, voName));
        sb.append(String.format("\nFOR EACH ROW BEGIN"));
        if (foreignVONames != null) {
            for (String name : foreignVONames) {
                sb.append(String.format("\n\tINSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('%s', new.%s, '%s', new.%s);", voName, idName, name, foreignVOUuid));
            }
        } else {
            sb.append(String.format("\n\tINSERT INTO UpdateVO (voName, uuid) VALUES ('%s', new.%s);", voName, idName));
        }

        List<Class<?>> children = sqlTriggerInheritance.get(clazz);
        if (children != null) {
            for (Class<?> child : children) {
                if (!child.isAnnotationPresent(SqlTrigger.class)) {
                    continue;
                }

                String childVOName = child.getSimpleName();
                String childVOId = getEntityIdFieldFromClass(child).getName();
                if (!childVOId.equals(idName)) {
                    throw new CloudRuntimeException(String.format("%s inherits %s, but %s has @Id as %s, %s has @Id as %s", child.getName(), clazz.getName(), child.getName(), childVOId, clazz.getName(), idName));
                }

                sb.append(String.format("\n\tINSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('%s', new.%s, '%s', new.%s);", voName, idName, childVOName, childVOId));
            }
        }
        sb.append("\nEND|\n");

        String deleteTriggerName = String.format("%sDeleteTrigger", voName);
        sb.append(String.format("\nDROP TRIGGER IF EXISTS `zstack`.%s;", deleteTriggerName));
        sb.append(String.format("\nCREATE TRIGGER `zstack`.%s AFTER DELETE ON `zstack`.`%s`", deleteTriggerName, voName));
        sb.append(String.format("\nFOR EACH ROW BEGIN"));
        sb.append(String.format("\n\tINSERT INTO DeleteVO (voName, uuid) VALUES ('%s', old.%s);", voName, idName));
        if (foreignVONames != null) {
            for (String name : foreignVONames) {
                sb.append(String.format("\n\tINSERT INTO DeleteVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('%s', old.%s, '%s', old.%s);", voName, idName, name, foreignVOUuid));
            }
        }
        if (foreignVOToDeleteNames != null) {
            for (String name : foreignVOToDeleteNames) {
                sb.append(String.format("\n\tINSERT INTO DeleteVO (voName, uuid, foreignVOToDeleteName, foreignVOToDeleteUuid) VALUES ('%s', old.%s, '%s', old.%s);", voName, idName, name, foreignVOToDeleteUuid));
            }
        }

    	/*
    	if (foreignVOName != null) {
    		sb.append(", foreignVOName, foreignVOUuid");
    	}
    	if (foreignVOToDeleteName != null) {
    		sb.append(", foreignVOToDeleteName, foreignVOToDeleteUuid");
    	}
    	sb.append(String.mediaType(") VALUES ('%s', old.%s", voName, idName));
    	if (foreignVOName != null) {
    		sb.append(String.mediaType(", '%s',  old.%s", foreignVOName, foreignVOUuid));
    	}
    	if (foreignVOToDeleteName != null) {
    		sb.append(String.mediaType(", '%s',  old.%s", foreignVOToDeleteName, foreignVOToDeleteUuid));
    	}
    	sb.append(");");
    	*/
        sb.append("\nEND|\n");
    }

    private void generateSqlTriggerText(Class<?> clazz, StringBuilder sb) {
        SqlTrigger st = clazz.getAnnotation(SqlTrigger.class);
        if (st != null) {
            doGenerateSqlTriggerText(st, clazz, sb);
        }

        SqlTriggers sts = clazz.getAnnotation(SqlTriggers.class);
        if (sts != null) {
            for (SqlTrigger stt : sts.triggers()) {
                doGenerateSqlTriggerText(stt, clazz, sb);
            }
        }
    }

    private Set<String> getAllInventoryNames() {
        Set<String> names = new HashSet<String>();
        for (IndexerInfo info : voClassToIndexerMapping.values()) {
            names.add(info.inventoryName);
        }
        return names;
    }

    private void deleteIndex(String indexName) throws URISyntaxException, ClientProtocolException, IOException {
        if (indexName == null) {
            indexName = "_all";
        }

        final String name = indexName.toLowerCase();
        URI uri = makeURI(elasticSearchBaseUrl, name);
        final HttpDelete del = new HttpDelete(uri);
        ResponseHandler<Void> rspHandler = new ResponseHandler<Void>() {
            @Override
            public Void handleResponse(HttpResponse rsp) throws ClientProtocolException, IOException {
                if (rsp.getStatusLine().getStatusCode() != HttpStatus.SC_OK && rsp.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                    logger.warn(String.format("Failed to delete index[%s] , because: \nstatus line: %s\nbody: %s", del.getURI().toASCIIString(),
                            rsp.getStatusLine(), EntityUtils.toString(rsp.getEntity())));
                } else {
                    logger.trace(String.format("Successfully delete index[%s]", name));
                }
                return null;
            }
        };
        httpClient.execute(del, rspHandler);
    }

    private void deleteAllIndex() throws ClientProtocolException, URISyntaxException, IOException {
        Set<String> invnames = getAllInventoryNames();
        for (String invname : invnames) {
            deleteIndex(invname);
        }
    }

    private void handle(final APIDeleteSearchIndexMsg msg) throws URISyntaxException, ClientProtocolException, IOException {
        if (msg.getIndexName() != null) {
            deleteIndex(msg.getIndexName());
        } else {
            deleteAllIndex();
        }

        APIDeleteSearchIndexEvent evt = new APIDeleteSearchIndexEvent(msg.getId());
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SearchConstant.INDEX_MANAGER_SERVICE_ID);
    }

    @Override
    public HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public String getElasticSearchBaseUrl() {
        return elasticSearchBaseUrl;
    }

    public void setStopIfCreateIndexFailed(boolean stopIfCreateIndexFailed) {
        this.stopIfCreateIndexFailed = stopIfCreateIndexFailed;
    }

    public void setDeleteAllIndexWhenStart(boolean deleteAllIndexWhenStart) {
        this.deleteAllIndexWhenStart = deleteAllIndexWhenStart;
    }
}
