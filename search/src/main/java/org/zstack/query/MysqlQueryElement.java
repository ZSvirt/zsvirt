package org.zstack.query;

import org.zstack.header.search.Inventory;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

public class MysqlQueryElement {
    private Class<?> entityClass;
    private Inventory inventoryAnnotation;
    private CriteriaBuilder criteriaBuilder;
    private List<Predicate> predicates;
    private CriteriaQuery criteriaQuery;
    private Root root;

    public Class<?> getEntityClass() {
        return entityClass;
    }
    public void setEntityClass(Class<?> entityClass) {
        this.entityClass = entityClass;
    }
    public Inventory getInventoryAnnotation() {
        return inventoryAnnotation;
    }
    public void setInventoryAnnotation(Inventory inventoryAnnotation) {
        this.inventoryAnnotation = inventoryAnnotation;
    }
    public CriteriaBuilder getCriteriaBuilder() {
        return criteriaBuilder;
    }
    public void setCriteriaBuilder(CriteriaBuilder criteriaBuilder) {
        this.criteriaBuilder = criteriaBuilder;
    }
    public List<Predicate> getPredicates() {
        return predicates;
    }
    public void setPredicates(List<Predicate> predicates) {
        this.predicates = predicates;
    }
    public CriteriaQuery getCriteriaQuery() {
        return criteriaQuery;
    }
    public void setCriteriaQuery(CriteriaQuery criteriaQuery) {
        this.criteriaQuery = criteriaQuery;
    }
    public Root getRoot() {
        return root;
    }
    public void setRoot(Root root) {
        this.root = root;
    }
}
