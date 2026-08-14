package org.zstack.testlib.collectstrategy

import org.zstack.testlib.Test

/**
 * Created by lining on 2017/7/19.
 */
interface SubCaseCollectionStrategy {
    String strategyName

    List<Class> collectSubCases(Test test)
}