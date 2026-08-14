package org.zstack.core.db;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.persistence.metamodel.SingularAttribute;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configurable(preConstruction=true,autowire=Autowire.BY_TYPE,dependencyCheck=true)
public class SimpleQueryImpl<T> implements SimpleQuery<T> {
    private static final CLogger _logger = CLoggerImpl.getLogger(SimpleQueryImpl.class);
    private final Class<T> _entityClass;
    private Root<T> _root;
    private List<AttrInfo> _selects = new ArrayList<AttrInfo>();
    private List<Condition> _conditions = new ArrayList<Condition>();
    private List<OrderInfo> orderInfos = new ArrayList<OrderInfo>();
    private SingularAttribute groupByInfo = null;
    private List<Path> _paths = new ArrayList<Path>();
    private CriteriaQuery _query;
    private final CriteriaBuilder _builder;
    private Integer limit;
    private Integer start;

    @Autowired
    private DatabaseFacade _dbf;
    
    class Condition {
        private final SingularAttribute _attr;
        private final Op _op;
        private final Object[] _val;
        
        Condition (SingularAttribute attr, Op op, Object... val) {
            this._attr = attr; 
            this._op = op;
            this._val = val;
        }
    }
    
    class OrderInfo {
         private final SingularAttribute attr;
         private final Od od;
         
         OrderInfo(SingularAttribute attr, Od od) {
            this.attr = attr; 
            this.od = od;
         }
    }
    
    class AttrInfo {
        private final SingularAttribute _attr;
        private final Class<?> _javaType;
        
        AttrInfo(SingularAttribute attr, Class<?> type) {
            this._attr = attr;
            this._javaType = type;
        }
        
    }

    SimpleQueryImpl(Class<T> vo) {
        _entityClass = vo;
        _builder = _dbf.getCriteriaBuilder();
    }

    @Override
    public SimpleQuery<T> select(SingularAttribute... attrs) {
        for (int i=0; i<attrs.length; i++) {
            _selects.add(new AttrInfo(attrs[i], attrs[i].getJavaType()));
        }
        return this;
    }

    @Override
    public SimpleQuery<T> add(SingularAttribute attr, Op op, Collection vals) {
        _conditions.add(new Condition(attr, op, vals.toArray(new Object[vals.size()])));
        return this;
    }

    @Override
    public SimpleQuery<T> add(SingularAttribute attr, Op op, Object... val) {
        _conditions.add(new Condition(attr, op, val));
        return this;
    }
    
    private Order[] orderClause() {
        ArrayList<Order> orders = new ArrayList<Order>(orderInfos.size());
        Order[] orderArr = (Order[]) Array.newInstance(Order.class, orderInfos.size());
        
        for (OrderInfo info : orderInfos) {
            if (info.od == Od.ASC) {
                orders.add(_builder.asc(_root.get(info.attr)));
            } else if (info.od == Od.DESC) {
                orders.add(_builder.desc(_root.get(info.attr)));
            }
        }
        
        return orders.toArray(orderArr);
    }
    
    private CriteriaQuery groupByClause(CriteriaQuery q) {
        assert _root != null : "You just set root before call groupby clause";
        if (groupByInfo != null) {
            q.groupBy(_root.get(groupByInfo));
        }
        return q;
    }
    
    private Predicate[] whereClause() {
        List<Predicate> preds = new ArrayList<Predicate>(_conditions.size());
        for (Condition con : _conditions) {
            Op op = con._op;
            Path p = _root.get(con._attr);
            Object[] vals = con._val;
            if (op == Op.EQ) {
                assert vals.length == 1 : String.format("Op.EQ needs one value, but %s given", vals.length);
                preds.add(_builder.equal(p, vals[0]));
            } else if (op == Op.NOT_EQ) {
                assert vals.length == 1 : String.format("Op.NOT_EQ needs one value, but %s given", vals.length);
                preds.add(_builder.notEqual(p, vals[0]));
            } else if (op == Op.NOT_NULL) {
                preds.add(_builder.isNotNull(p));
            } else if (op == Op.IN) {
                //preds.add(_builder.in(p.in(vals)));
                assert vals.length !=0 : String.format("Op.IN needs more than one value, but %s given", vals.length);
                preds.add(p.in(vals));
            } else if (op == Op.NOT_IN) {
                assert vals.length !=0 : String.format("Op.NOT_IN needs more than one value, but %s given", vals.length);
                preds.add(_builder.not(p.in(vals)));
            } else if (op == Op.NULL) {
                preds.add(_builder.isNull(p)); 
            } else if (op == Op.LIKE) {
                assert vals.length == 1 : String.format("Op.LIKE needs one value, but %s given", vals.length);
                preds.add(_builder.like(p, (String)vals[0]));
            } else if (op == Op.NOT_LIKE) {
                assert vals.length == 1 : String.format("Op.NOTLIKE needs one value, but %s given", vals.length);
                preds.add(_builder.notLike(p, (String)vals[0]));
            } else if (op == Op.GT) {
                assert vals.length == 1 : String.format("Op.GT needs one value, but %s given", vals.length);
                preds.add(_builder.greaterThan(p, (Comparable)vals[0]));
            } else if (op == Op.LT) {
                assert vals.length == 1 : String.format("Op.LT needs one value, but %s given", vals.length);
                preds.add(_builder.lessThan(p, (Comparable)vals[0]));
            } else if (op == Op.GTE) {
                assert vals.length == 1 : String.format("Op.GT_EQ needs one value, but %s given", vals.length);
                preds.add(_builder.greaterThanOrEqualTo(p, (Comparable)vals[0]));
            } else if (op == Op.LTE) {
                assert vals.length == 1 : String.format("Op.LT_EQ needs one value, but %s given", vals.length);
                preds.add(_builder.lessThanOrEqualTo(p, (Comparable)vals[0]));
            } else {
                assert(false) : op.toString() + " has not been supported";
            }
        }
        
        Predicate[] predArray = (Predicate[]) Array.newInstance(Predicate.class, preds.size());
        return preds.toArray(predArray);
    }

    private void done() {
        if (_selects.size() == 0) {
            _query = _builder.createQuery(_entityClass);
        } else if (_selects.size() == 1) {
           Class<?> selectType = _selects.get(0)._javaType;
           _query = _builder.createQuery(selectType);
        } else {
           _query = _builder.createTupleQuery(); 
        }
        
        _root = _query.from(_entityClass);
        
        if (_selects.size() == 0) {
        } else if (_selects.size() == 1) {
            Path p = _root.get(_selects.get(0)._attr);
            _query.select(p);
        } else {
            for (AttrInfo info : _selects) {
               _paths.add(_root.get(info._attr)); 
            }
            _query.multiselect(_paths);
        }

        _query.where(whereClause());
        _query.orderBy(orderClause());
        groupByClause(_query);
    }

    @Override
    @Transactional(readOnly=true, propagation=Propagation.REQUIRES_NEW)
    public T find() {
        return _find();
    }

    @Transactional
    T _find() {
        assert _selects.size() == 0 : "find() for entity doesn't need any parameter in Query.Select(), you have put some parameter in Query.select(..), either removing these parameters or using findValue() or findTuple()";
        done();

        T vo = null;
        try {
            Query q = _dbf.getEntityManager().createQuery(_query);
            if (limit != null) {
                q.setMaxResults(limit);
            }
            vo = (T)q.getSingleResult();
        } catch (NoResultException e) {
        } catch (EmptyResultDataAccessException e) {
        }
        if (vo != null) {
            return vo;
        } else {
            return null;
        }
    }

    @Transactional(readOnly=true, propagation=Propagation.REQUIRES_NEW)
    @Override
    public <K> List<K> list() {
        return _list();
    }

    @Transactional
    <K> List<K> _list() {
        assert _selects.size() == 0 : "list() for entities doesn't need any parameter in Query.Select(), you have put some parameter in Query.select(..), either removing these parameters or using listValue() or listTuple()";
        done();
        Query q = _dbf.getEntityManager().createQuery(_query);
        if (limit != null) {
            q.setMaxResults(limit);
        }
        if (start != null) {
            q.setFirstResult(start);
        }
        List<T> vos = q.getResultList();
        List<K> ros = new ArrayList<K>(vos.size());
        for (T vo : vos) {
           ros.add((K) vo);
        }
        return ros;
    }

    @Override
    @Transactional(readOnly=true, propagation=Propagation.REQUIRES_NEW)
    public <K> K findValue() {
        return _findValue();
    }

    @Transactional
    <K> K _findValue() {
        assert _selects.size() == 1 : String.format("findValue() only need one parameter in Query.Select(), you have put %s parameter in Query.select(..), either correcting the parameter or using find() or findTuple()", _selects.size());
        done();
        K value = null;
        try {
            Query q = _dbf.getEntityManager().createQuery(_query);
            if (limit != null) {
                q.setMaxResults(limit);
            }
            if (start != null) {
                q.setFirstResult(start);
            }
            value = (K)q.getSingleResult();
        } catch (NoResultException e) {
        } catch (EmptyResultDataAccessException e) {
        }
        
        return value;
    }

    @Override
    @Transactional(readOnly=true, propagation=Propagation.REQUIRES_NEW)
    public <K> List<K> listValue() {
        return _listValue();
    }

    @Transactional
    <K> List<K> _listValue() {
        assert _selects.size() == 1 : String.format("listValue() only need one parameter in Query.Select(), you have put %s parameter in Query.select(..), either correcting the parameter or using list() or listTuple()", _selects.size());
        done();
        Query q = _dbf.getEntityManager().createQuery(_query);
        if (limit != null) {
            q.setMaxResults(limit);
        }
        if (start != null) {
            q.setFirstResult(start);
        }
        List<K> vals = q.getResultList();
        return vals;
    }

    @Override
    @Transactional(readOnly=true, propagation=Propagation.REQUIRES_NEW)
    public Tuple findTuple() {
        return _findTuple();
    }

    @Transactional
    Tuple _findTuple() {
        assert _selects.size() > 1 : String.format("findTuple() needs more than one parameter in Query.Select(), you have put %s parameter in Query.select(..), either correcting the parameter or using find() or findValue()", _selects.size());
        done();
        Tuple ret = null;
        try {
            Query q = _dbf.getEntityManager().createQuery(_query);
            if (limit != null) {
                q.setMaxResults(limit);
            }
            ret = (Tuple)q.getSingleResult();
        } catch (NoResultException e) {
        } catch (EmptyResultDataAccessException e) {
        }
        return ret;
    }

    @Override
    @Transactional(readOnly=true, propagation=Propagation.REQUIRES_NEW)
    public List<Tuple> listTuple() {
        return _listTuple();
    }

    @Transactional
    List<Tuple> _listTuple() {
        assert _selects.size() > 1 : String.format("listTuple() needs more than one parameter in Query.Select(), you have put %s parameter in Query.select(..), either correcting the parameter or using list() or listValue()", _selects.size());
        done();
        Query q = _dbf.getEntityManager().createQuery(_query);
        if (limit != null) {
            q.setMaxResults(limit);
        }
        if (start != null) {
            q.setFirstResult(start);
        }
        List<Tuple> rets =  q.getResultList();
        return rets;
    }

    @Override
    @Transactional(readOnly=true, propagation=Propagation.REQUIRES_NEW)
    public Long count() {
        return _count();
    }

    @Transactional
    Long _count() {
        assert _selects.size() == 0 : "count() for entity doesn't need any parameter in Query.Select(), you have put some parameter in Query.select(..), either removing these parameters or using findValue() or findTuple()";
        _query = _builder.createQuery(Long.class);
        _root = _query.from(_entityClass);
        _query.select(_builder.count(_root));
        _query.where(whereClause());
        return (Long) _dbf.getEntityManager().createQuery(_query).getSingleResult();
    }

    @Override
    public SimpleQuery<T> orderBy(SingularAttribute attr, org.zstack.core.db.SimpleQuery.Od order) {
        orderInfos.add(new OrderInfo(attr, order));
        return this;
    }

    @Override
    public SimpleQuery<T> groupBy(SingularAttribute attr) {
        this.groupByInfo = attr;
        return this;
    }

	@Override
    public SimpleQuery<T> isSoftDeleted(SingularAttribute attr) {
	    return add(attr, Op.NULL);
    }

    @Override
    @Transactional(readOnly=true, propagation=Propagation.REQUIRES_NEW)
    public boolean isExists() {
        return _isExists();
    }

    @Transactional
    boolean _isExists() {
        assert _selects.size() == 0 : "isExists() for entity doesn't need any parameter in Query.Select(), you have put some parameter in Query.select(..), either removing these parameters or using findValue() or findTuple()";
        _query = _builder.createQuery(Long.class);
        _root = _query.from(_entityClass);
        _query.select(_builder.count(_root));
        _query.where(whereClause());
        TypedQuery<Long> tq = _dbf.getEntityManager().createQuery(_query);
        tq.setMaxResults(1);
        long count = tq.getSingleResult();
        return count >= 1;
    }

    @Override
    public SimpleQuery<T> setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public SimpleQuery<T> setStart(int start) {
        this.start = start;
        return this;
    }

    public QueryMore toQueryMore() {
        QueryMore q = new QueryMore(_entityClass);

        if (start != null) {
            q.start(start);
        }
        if (limit != null) {
            q.limit(limit);
        }

        if (!_selects.isEmpty()) {
            q.select(_selects.stream().map(s -> s._attr).toArray(SingularAttribute<?, ?>[]::new));
        }
        if (!orderInfos.isEmpty()) {
            for (OrderInfo orderInfo : orderInfos) {
                if (orderInfo.od == Od.ASC) {
                    q.orderByAsc(orderInfo.attr);
                } else {
                    q.orderByDesc(orderInfo.attr);
                }
            }
        }
        if (!_conditions.isEmpty()) {
            for (Condition condition : _conditions) {
                q.condition(condition._attr, condition._op, condition._val[0]);
            }
        }
        return q;
    }
}
