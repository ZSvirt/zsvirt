package org.zstack.zql.ast.visitors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.query.dsl.EntityContext;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.DBGraph;
import org.zstack.header.core.StaticInit;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.search.SearchConstant;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;
import org.zstack.search.SearchFacade;
import org.zstack.search.schema.IndexType;
import org.zstack.search.schema.Indexes;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.zql.ast.ZQLMetadata;
import org.zstack.zql.ast.visitors.result.SearchResult;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ Author : yh.w
 * @ Date   : Created in 16:41 2020/10/27
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SearchVisitor implements ASTVisitor<SearchResult, ASTNode.Search> {
    private static final CLogger logger = Utils.getLogger(SearchVisitor.class);

    @Autowired
    SearchFacade sf;

    private static Map<Class, String[]> indexFieldsMap = Maps.newConcurrentMap();
    private static Set<Class> indexedEntities;
    private static final String DocumentId = "uuid";

    SearchResult ret = new SearchResult();

    @StaticInit
    static void staticInit() {
        try {
            JAXBContext context = JAXBContext.newInstance("org.zstack.search.schema");
            File cfg = PathUtil.findFileOnClassPath(SearchConstant.INDEX_CONFIG_PATH, true);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            Indexes schema = (Indexes) unmarshaller.unmarshal(cfg);
            for (IndexType index : schema.getIndex()) {
                if (index.isBaseClass()) {
                    continue;
                }
                Class entityClass;
                try {
                    entityClass = Class.forName(index.getName());
                } catch (ClassNotFoundException e) {
                    if (CoreGlobalProperty.UNIT_TEST_ON) {
                        logger.warn(String.format("skip missing search index entity[%s] in unit test: %s",
                                index.getName(), e.getMessage()));
                        continue;
                    }
                    throw new CloudRuntimeException(String.format("build search mapping failed, %s", e));
                }
                List<String> props = index.getProp().stream().map(IndexType.Prop::getName).collect(Collectors.toList());
                props.add(DocumentId);
                String[] fieldArray = props.toArray(new String[0]);
                indexFieldsMap.put(entityClass, fieldArray);
            }

            indexedEntities = indexFieldsMap.keySet();
        } catch (JAXBException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResult visit(ASTNode.Search node) {
        String keyword = node.getKeyword().getValue()
                .replaceAll("^'|'$", "")
                .replaceAll("''", "'");
        FullTextEntityManager manager = sf.getFullTextEntityManager();
        Set<Class> indexs;
        if (node.getIndex() == null) {
            indexs = indexedEntities;
        } else {
            indexs = node.getIndex()
                    .getIndexs()
                    .stream()
                    .map(v -> {
                        ZQLMetadata.InventoryMetadata inventory = ZQLMetadata.findInventoryMetadata(v);
                        Class entity = inventory.inventoryAnnotation.mappingVOClass();
                        Optional<Class> opt = indexedEntities
                                .stream()
                                .filter(e -> e.getSimpleName().equalsIgnoreCase(entity.getSimpleName()))
                                .findAny();
                        DebugUtils.Assert(opt.isPresent(), String.format("cannot find index with name[%s]", entity.getSimpleName()));
                        return entity;
                    })
                    .collect(Collectors.toSet());
        }

        List<SearchResult.Search> searches = Lists.newArrayList();
        indexs.forEach(v -> {
            SearchResult.Search search = new SearchResult.Search();

            EntityContext context = manager.getSearchFactory()
                    .buildQueryBuilder()
                    .forEntity(v);

            for (String fieldName : indexFieldsMap.get(v)) {
                context.overridesForField(fieldName, "Keyword_analyzer");
            }

            QueryBuilder queryBuilder = context.get();
            Query luceneQuery = queryBuilder
                    .keyword()
                    .onFields(indexFieldsMap.get(v))
                    .matching(keyword)
                    .createQuery();

            FullTextQuery fullTextQuery = manager.createFullTextQuery(luceneQuery, v);
            fullTextQuery.setProjection("uuid");

            search.setQuery(fullTextQuery);
            search.setRestrictSql(node.getRestrictBy() == null ? null : getRestrictSql(node ,v));
            searches.add(search);
        });

        ret.searchs = searches;
        return ret;
    }

    private String getRestrictSql(ASTNode.Search node, Class v) {
        if (node.getRestrictBy().getExprs().size() > 1) {
            throw new ZQLError("zql search not support multiple restrict expressions");
        }

        ASTNode.RestrictExpr restrictExpr = node.getRestrictBy().getExprs().get(0);

        ZQLMetadata.InventoryMetadata dst = ZQLMetadata.findInventoryMetadata(restrictExpr.getEntity());
        Optional<DBGraph.EntityVertex> vertex = Optional.ofNullable(DBGraph.findVerticesWithSmallestWeight(v, dst.inventoryAnnotation.mappingVOClass()));
        return vertex.map(entityVertex -> entityVertex.makeSQLForSearch(restrictExpr.getField(), restrictExpr.getOperator(),
                String.valueOf(((ASTNode) restrictExpr.getValue()).accept(new ValueVisitor())))).orElse(null);

    }
}
