package org.zstack.testlib.collectstrategy

import org.zstack.core.Platform
import org.zstack.testlib.Case
import org.zstack.testlib.Test

import java.lang.reflect.Modifier

/**
 * Created by lining on 2017/7/19.
 */

/*
Example:

 primary storage test package
├── local
│   ├── localStorageCapacityCase
│   └── localStorageStateCase
│
├── nfs
│   ├── NfsCapacityCase
│   ├── NfsCapacityStateCase
│   └── NfsTest
│
├── PrimaryStorageCapacityCase
├── PrimaryStorageStateCase
└── PrimaryStorageTest

 PrimaryStorageTest subCases is [PrimaryStorageCapacityCase, PrimaryStorageStateCase, localStorageCapacityCase, localStorageStateCase, NfsCapacityCase, NfsCapacityStateCase]

 */
class MostCompleteSubCaseCollectionStrategy implements SubCaseCollectionStrategy{

    final static String strategyName = "MostComplete"

    @Override
    List<Class> collectSubCases(Test test) {
        assert null != test : "test is null, can not find subcase"

        def cases = Platform.reflections.getSubTypesOf(Case.class).findAll { !Modifier.isAbstract(it.modifiers) }
        cases = cases.findAll { it.package.name.startsWith(test.class.package.name) }
        cases = cases.sort{ a, b ->
            return a.name.compareTo(b.name)
        }

        return cases
    }

}
