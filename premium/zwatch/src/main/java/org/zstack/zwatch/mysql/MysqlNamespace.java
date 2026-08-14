package org.zstack.zwatch.mysql;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.LabelValueQueryObject;
import org.zstack.zwatch.datatype.MetricQueryObject;
import org.zstack.zwatch.datatype.Namespace;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface MysqlNamespace {
    List<Datapoint> query(MetricQueryObject queryObject);

    List<Map> queryLabelValues(LabelValueQueryObject qo);

    Map<Class, Class> namespacesClasses = new HashMap<>();

    static MysqlNamespace getMysqlNamespace(Namespace ns) {
        Class nsClz = namespacesClasses.get(ns.getClass());
        if (nsClz == null) {
            throw new CloudRuntimeException(String.format("cannot find MysqlNamespace for the Namespace class[%s]", ns.getClass()));
        }

        try {
            return (MysqlNamespace) nsClz.getConstructor(Namespace.class).newInstance(ns);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
