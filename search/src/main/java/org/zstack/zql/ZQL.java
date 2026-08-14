package org.zstack.zql;

import com.google.common.collect.Sets;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.EntityMetadata;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.search.SearchGlobalProperty;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vo.ResourceInventory;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;
import org.zstack.header.zql.*;
import org.zstack.query.QueryGlobalConfig;
import org.zstack.search.SearchErrors;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zql.antlr4.ZQLLexer;
import org.zstack.zql.antlr4.ZQLParser;
import org.zstack.zql.ast.ZQLMetadata;
import org.zstack.zql.ast.parser.visitors.CountVisitor;
import org.zstack.zql.ast.parser.visitors.SearchVisitor;
import org.zstack.zql.ast.parser.visitors.SumVisitor;
import org.zstack.zql.ast.visitors.QueryVisitor;
import org.zstack.zql.ast.visitors.ReturnWithVisitor;
import org.zstack.zql.ast.visitors.result.QueryResult;
import org.zstack.zql.ast.visitors.result.ReturnWithResult;
import org.zstack.zql.ast.visitors.result.SearchResult;

import javax.persistence.Query;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ZQL {
    private static final CLogger logger = Utils.getLogger(ZQL.class);

    private static Set<String> slowZql = new HashSet<>();
    private static ZQLStatistic statistic = new ZQLStatistic();

    private QueryResult astResult;
    private SearchResult searchResult;
    private String text;

    @Autowired
    private PluginRegistry pluginRgty;

    static class ThrowingErrorListener extends BaseErrorListener {
        String text;

        ThrowingErrorListener(String t) {
            text = t;
        }

        private String getTextAround(int lineNum, int pos) {
            String line = text.split("\n")[lineNum-1];
            int start = pos - 10 < 0 ? 0 : pos - 10;
            int end = pos + 10 >= line.length() ? line.length() - 1 : pos + 10;
            return line.substring(start, end);
        }

        @Override
        public void syntaxError(Recognizer recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
                throws ParseCancellationException {
            throw new ParseCancellationException(String.format("error %s at line %s:%s, %s",
                    msg, line, charPositionInLine, getTextAround(line, charPositionInLine)));
        }
    }

    public static String queryTargetNameFromInventoryClass(Class invClass) {
        String name = invClass.getSimpleName().toLowerCase();
        return StringUtils.removeEnd(name, "inventory");
    }

    public static ZQL fromString(String text) {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("created ZQL from text: %s", text));
        }

        ZQL zql = new ZQL();
        zql.text = StringUtils.removeEnd(text.trim(), ";");
        return zql;
    }

    private List entityVOtoInventories(List vos) {
        List ret = new ArrayList();
        if (astResult.targetFieldNames != null && !astResult.targetFieldNames.isEmpty()) {
            vos.forEach(it -> {
                ret.add(entityVOtoInventory(it));
            });
        } else {
            vos.forEach(it -> ret.add(ToInventory.toInventory(it)));
        }

        return ret;
    }

    private Object entityVOtoInventory(Object vo) {
        try {
            ZQLMetadata.InventoryMetadata inventoryMetadata = astResult.inventoryMetadata;
            Object inv = inventoryMetadata.selfInventoryClass.getConstructor().newInstance();
            if (vo instanceof Object[]) {
                Object[] fieldValues = (Object[]) vo;
                for (int i = 0; i < astResult.targetFieldNames.size(); i++) {
                    String fieldName = astResult.targetFieldNames.get(i);
                    BeanUtils.setProperty(inv, fieldName, inventoryMetadata.toInventoryFieldObject(fieldName, fieldValues[i]));
                }
            } else {
                String fieldName =  astResult.targetFieldNames.get(0);
                BeanUtils.setProperty(inv, fieldName, astResult.inventoryMetadata.toInventoryFieldObject(fieldName, vo));
            }
            return inv;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    private static void callExtensions(ASTNode.Query node) {
        Platform.getComponentLoader().getComponent(PluginRegistry.class)
                .getExtensionList(MarshalZQLASTTreeExtensionPoint.class)
                .forEach(it -> it.marshalZQLASTTree(node));
    }

    private Runnable prepareZQLContext(ASTNode.Query node) {
        ZQLMetadata.InventoryMetadata inventory = ZQLMetadata.findInventoryMetadata(node.getTarget().getEntity());
        org.zstack.zql.ZQLContext.setQueryTargetInventoryName(inventory.fullInventoryName());

        List<Runnable> cleanUps = new ArrayList<>();
        pluginRgty.getExtensionList(ZQLCustomizeContextExtensionPoint.class).forEach(e->{
            Runnable r = e.zqlCustomizeContext(node);
            if (r != null) {
                cleanUps.add(r);
            }
        });

        return () -> {
            cleanUps.forEach(Runnable::run);
            ZQLContext.cleanQueryTargetInventoryName();
        };
    }

    class ReturnWithQueryNodeWrapper {
        ASTNode.Query node;
        boolean primaryKeyAdded;
        String voPrimaryKeyName;

        public ReturnWithQueryNodeWrapper(ASTNode.Query node) {
            this.node = node;
        }

        public boolean isReturnWithEnabled() {
            if (node.getReturnWith() == null) {
                return false;
            }

            List<ReturnWithResult> rwr = (List<ReturnWithResult>) node.getReturnWith().accept(new ReturnWithVisitor());
            return rwr.stream().anyMatch(r->!r.name.equals("total"));
        }

        public boolean isFieldsQuery() {
            return node.getTarget().getFields() != null && !node.getTarget().getFields().isEmpty();
        }

        public void addPrimaryKeyFieldToTargetFieldNamesWhenReturnWithEnabledAndIsFieldQuery() {
            if (!isReturnWithEnabled()) {
                return;
            }

            if (!isFieldsQuery()) {
                return;
            }


            ZQLMetadata.InventoryMetadata inventoryMetadata = ZQLMetadata.findInventoryMetadata(node.getTarget().getEntity());
            Field priKey = EntityMetadata.getPrimaryKeyField(inventoryMetadata.inventoryAnnotation.mappingVOClass());
            voPrimaryKeyName = priKey.getName();
            if (!node.getTarget().getFields().contains(voPrimaryKeyName)) {
                primaryKeyAdded = true;
                node.getTarget().getFields().add(voPrimaryKeyName);
            }
        }

        public void removePrimaryKeyFieldFromTargetFieldNamesWhenReturnWithEnabledAndIsFieldQuery(QueryResult astResult) {
            if (primaryKeyAdded) {
                astResult.targetFieldNames.remove(voPrimaryKeyName);
            }
        }

        public Integer primaryKeyFieldIndex() {
            return node.getTarget().getFields().indexOf(voPrimaryKeyName);
        }
    }

    public ZQLQueryReturn getSingleResultWithSession(SessionInventory session) {
        try {
            ZQLContext.putAPISession(session);
            return getSingleResult();
        } finally {
            ZQLContext.cleanAPISession();
        }
    }

    public ZQLQueryReturn getSingleResult() {
        List<ZQLQueryReturn> rs = getResultList();
        return rs.get(0);
    }

    public List<ZQLQueryReturn> getResultList() {
        long before = System.currentTimeMillis();

        List<ZQLQueryReturn> rs = new ArrayList<>();

        ZQLLexer l = new ZQLLexer(CharStreams.fromString(text));
        ZQLParser p = new ZQLParser(new CommonTokenStream(l));
        
        l.addErrorListener(new ThrowingErrorListener(text));
        p.addErrorListener(new ThrowingErrorListener(text));

        Map currentContext = ZQLContext.get();
        p.zqls().zql().forEach(ctx -> {
            ZQLContext.set(currentContext);

            ZQLQueryReturn qr = new ZQLQueryReturn();

            class Ret {
                Long count;
                List vos;
            }

            Ret ret = new Ret();

            if (ctx instanceof ZQLParser.CountGrammarContext) {
                ASTNode.Query query = ((ZQLParser.CountGrammarContext)ctx).count().accept(new CountVisitor());

                Runnable clean = prepareZQLContext(query);

                callExtensions(query);

                astResult = (QueryResult) query.accept(new QueryVisitor(true));

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("ZQL query: %s", astResult.sql));
                }

                beforeExecuteQuery(astResult, ZQLContext.getAPISession());

                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        Query q = astResult.createCountQuery.apply(databaseFacade.getEntityManager());
                        List results = q.getResultList();
                        if (results.size() == 1 && results.get(0) instanceof Long) {
                            ret.count = (Long)results.get(0);
                        } else {
                            if (!results.isEmpty()) {
                                /*
                                    gson which enable ComplexMapKeySerialization will serialize the map to json array
                                    when it is not empty or json object "{}" when it is empty.
                                    null is better than a inconsistent json type.
                                 */
                                qr.inventoryCounts = new LinkedHashMap<>();
                            }
                            for (Object result : results) {
                                Object[] fieldValues = (Object[]) result;
                                int countIndex = fieldValues.length - 1;
                                qr.inventoryCounts.put(entityVOtoInventory(fieldValues), (Long)fieldValues[countIndex]);
                            }

                            Query totalCountQuery = astResult.createSimpleCountQuery.apply(databaseFacade.getEntityManager());
                            ret.count = (Long) totalCountQuery.getSingleResult();
                        }
                    }
                }.execute();

                qr.name = query.getName();

                clean.run();
            } else if (ctx instanceof ZQLParser.QueryGrammarContext) {
                ASTNode.Query query = ((ZQLParser.QueryGrammarContext) ctx).query().accept(new org.zstack.zql.ast.parser.visitors.QueryVisitor());
                ReturnWithQueryNodeWrapper wrapper = new ReturnWithQueryNodeWrapper(query);

                wrapper.addPrimaryKeyFieldToTargetFieldNamesWhenReturnWithEnabledAndIsFieldQuery();

                Runnable clean = prepareZQLContext(query);

                callExtensions(query);
                astResult = (QueryResult) query.accept(new QueryVisitor(false));

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("ZQL query: %s", astResult.sql));
                }

                beforeExecuteQuery(astResult, ZQLContext.getAPISession());

                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        Query q = astResult.createJPAQuery.apply(databaseFacade.getEntityManager());
                        ret.vos = q.getResultList();

                        if (astResult.createSimpleCountQuery != null) {
                            q = astResult.createSimpleCountQuery.apply(databaseFacade.getEntityManager());
                            ret.count = (Long) q.getSingleResult();
                        }
                    }
                }.execute();

                qr.returnWith = callReturnWithExtensions(astResult, wrapper, ret.vos);
                qr.name = query.getName();

                wrapper.removePrimaryKeyFieldFromTargetFieldNamesWhenReturnWithEnabledAndIsFieldQuery(astResult);

                clean.run();

                qr.inventories = ret.vos != null ? entityVOtoInventories(ret.vos) : null;
            } else if (ctx instanceof ZQLParser.SumGrammarContext) {
                ASTNode.Sum sum = ((ZQLParser.SumGrammarContext) ctx).sum().accept(new SumVisitor());

                Runnable clean = prepareZQLContext(sum);
                callExtensions(sum);

                astResult = (QueryResult) sum.accept(new QueryVisitor(false));

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("ZQL query: %s", astResult.sql));
                }

                beforeExecuteQuery(astResult, ZQLContext.getAPISession());

                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        Query q = astResult.createJPAQuery.apply(databaseFacade.getEntityManager());
                        ret.vos = q.getResultList();
                    }
                }.execute();

                qr.inventories = (List) ret.vos.stream().map(vo -> Arrays.asList((Object[]) vo)).collect(Collectors.toList());
                qr.name = sum.getName();

                clean.run();
            } else if (ctx instanceof ZQLParser.SearchGrammarContext) {
                if (!SearchGlobalProperty.SearchAutoRegister) {
                    throw new OperationFailureException(err(SearchErrors.SEARCH_MODULE_DISABLED, "search module disabled"));
                }

                ASTNode.Search search = ((ZQLParser.SearchGrammarContext) ctx).search().accept(new SearchVisitor());
                searchResult = (SearchResult) search.accept(new org.zstack.zql.ast.visitors.SearchVisitor());
                Set<String> resourceUuids = Sets.newConcurrentHashSet();
                searchResult.getSearchs()
                        .parallelStream()
                        .forEach(v -> {
                            List result = v.getQuery().getResultList();
                            Set<String> searchResults = Sets.newConcurrentHashSet();
                            for (Object o : result) {
                                Object[] rarray = (Object[]) o;
                                searchResults.add((String) rarray[0]);
                            }

                            if (!searchResults.isEmpty() && v.getRestrictSql() != null) {
                                new SQLBatch() {
                                    @Override
                                    protected void scripts() {
                                        Query q = databaseFacade
                                            .getEntityManager()
                                            .createQuery(String.format(v.getRestrictSql(),
                                                    String.join(",", searchResults)
                                                            .replaceAll("([^,]+)", "'$1'")));
                                        List res = q.getResultList();
                                        resourceUuids.addAll((Collection<? extends String>) res.stream().collect(Collectors.toList()));
                                    }
                                }.execute();
                            } else {
                                resourceUuids.addAll(searchResults);
                            }
                        });

                qr.inventories = filterNoAccessResources(resourceUuids, ZQLContext.getAPISession().getAccountUuid());
                ret.count = (long) qr.inventories.size();
            } else {
                throw new CloudRuntimeException(String.format("should not be here, %s", ctx));
            }


            qr.total = ret.count;

            rs.add(qr);
        });


        long cost = System.currentTimeMillis() - before;
        boolean statisticsOn = QueryGlobalConfig.ZQL_STATISTICS_ON.value(Boolean.class);
        if (cost > TimeUnit.SECONDS.toMillis(QueryGlobalConfig.SLOW_ZQL_COST_TIME.value(Long.class))) {
            logSlowZql(text, cost);

            if (statisticsOn) {
                statistic.getSlowZQLStatistics().add(new SlowZQLStatistic(text, cost));
            }
        }

        if (statisticsOn) {
            statistic.count();
        }

        return rs;
    }

    @ExceptionSafe
    private void logSlowZql(String zql, long time) {
        if (!slowZql.contains(zql)) {
            slowZql.add(zql);
            logger.warn(String.format("SLOW ZQL cost %d ms: %s", time, zql));
        }
    }
    
    public static ZQLStatistic getZQLStatistic() {
        return statistic;
    }

    @ExceptionSafe
    public static void cleanStatisticData() {
        statistic.getSlowZQLStatistics().clear();
        statistic.resetCount();
    }

    @ExceptionSafe
    public static void cleanSlowZqlCache() {
        slowZql.clear();
    }

    private List<ResourceInventory> filterNoAccessResources(Set<String> resourceUuids, String accountUuid) {
        if (resourceUuids.isEmpty()) {
            return Collections.emptyList();
        }

        return new SQLBatchWithReturn<List<ResourceInventory>>() {
            @Override
            protected List<ResourceInventory> scripts() {
                if (accountUuid.equals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)) {
                    Query q = databaseFacade.getEntityManager().createNativeQuery("select uuid, resourceName, SUBSTRING_INDEX(concreteResourceType,\".\",-1) from ResourceVO where uuid in (:uuids)");
                    q.setParameter("uuids", resourceUuids);
                    List<Object[]> objs = q.getResultList();
                    List<ResourceVO> vos = objs.stream().map(ResourceVO::new).collect(Collectors.toList());
                    return ResourceInventory.valueOf(vos);
                }

                Query q = databaseFacade.getEntityManager().createNativeQuery(
                        "select r.uuid, r.resourceName, SUBSTRING_INDEX(r.concreteResourceType,\".\",-1) from ResourceVO r " +
                        "left join AccountResourceRefVO ar on r.uuid=ar.resourceUuid " +
                        "where ar.accountUuid is Null and r.uuid in :uuids "+
                        "or r.uuid in (select ref.resourceUuid from AccountResourceRefVO ref where" +
                        " ((ref.accountUuid = :accountUuid or ref.type = 'SharePublic') " +
                        "and ref.resourceUuid in (:uuids)))");
                q.setParameter("uuids", resourceUuids);
                q.setParameter("accountUuid", accountUuid);
                List<Object[]> objs = q.getResultList();
                List<ResourceVO> vos = objs.stream().map(ResourceVO::new).collect(Collectors.toList());
                return ResourceInventory.valueOf(vos);
            }
        }.execute();
    }

    private void beforeExecuteQuery(QueryResult astResult, SessionInventory session) {
        List<Class> targetInventories = new ArrayList<>();

        targetInventories.add(astResult.inventoryMetadata.selfInventoryClass);

        for (ZQLQueryExtensionPoint ext : pluginRgty.getExtensionList(ZQLQueryExtensionPoint.class)) {
            ext.beforeQueryExtensionPoint(targetInventories, session);
        }
    }

    private Map callReturnWithExtensions(QueryResult astResult, ReturnWithQueryNodeWrapper wrapper, List vos) {
        if (astResult.returnWith == null || astResult.returnWith.isEmpty()) {
            return null;
        }

        FutureCompletion future = new FutureCompletion(null);
        Map ret = new ConcurrentHashMap();
        List<ErrorCode> errs = new ArrayList<>();
        new While<>(astResult.returnWith).all((r, coml) -> {
            Optional<ReturnWithExtensionPoint> opt = pluginRgty.getExtensionList(ReturnWithExtensionPoint.class)
                    .stream().filter(ext->r.name.equals(ext.getReturnWithName())).findAny();
            if (!opt.isPresent()) {
                throw new CloudRuntimeException(String.format("cannot find any ReturnWithExtensionPoint dealing with %s", r.name));
            }

            ReturnWithExtensionPoint ext = opt.get();

            ReturnWithExtensionPoint.ReturnWithExtensionParam param = new ReturnWithExtensionPoint.ReturnWithExtensionParam();
            param.expression = r.expr;
            param.isFieldsQuery = wrapper.isFieldsQuery();
            if (param.isFieldsQuery) {
                param.primaryKeyIndexInVOs = wrapper.primaryKeyFieldIndex();
            }
            param.vos = vos;
            param.voClass = astResult.inventoryMetadata.inventoryAnnotation.mappingVOClass();

            ext.returnWith(param, new ReturnValueCompletion<Map>(coml) {
                @Override
                public void success(Map result) {
                    Optional.ofNullable(result).ifPresent(ret::putAll);
                    coml.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    errs.add(errorCode);
                    coml.allDone();
                }
            });
        }).run(new WhileDoneCompletion(future) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errs.isEmpty()) {
                    future.success();
                } else {
                    future.fail(errs.get(0));
                }
            }
        });

        future.await();
        if (future.isSuccess()) {
            return ret;
        } else {
            throw new OperationFailureException(future.getErrorCode());
        }
    }

    @Override
    public String toString() {
        return astResult.sql;
    }
}
