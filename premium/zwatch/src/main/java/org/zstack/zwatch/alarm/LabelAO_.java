package org.zstack.zwatch.alarm;

import org.zstack.zwatch.datatype.Label;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(LabelAO.class)
public class LabelAO_ {
    public static volatile SingularAttribute<LabelAO, String> uuid;
    public static volatile SingularAttribute<LabelAO, String> key;
    public static volatile SingularAttribute<LabelAO, Label.Operator> operator;
    public static volatile SingularAttribute<LabelAO, String> value;
}
