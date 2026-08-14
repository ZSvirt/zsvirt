package org.zstack.header.search;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public enum SearchOp {
    AND_EQ,
    AND_NOT_EQ,
    AND_GT,
    AND_GTE,
    AND_LT,
    AND_LTE,
    AND_IN,
    AND_NOT_IN,
    OR_EQ,
    OR_NOT_EQ,
    OR_GT,
    OR_GTE,
    OR_LT,
    OR_LTE,
    OR_IN,
    OR_NOT_IN,
}
