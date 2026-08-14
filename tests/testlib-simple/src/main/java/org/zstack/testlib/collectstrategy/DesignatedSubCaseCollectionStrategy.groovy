package org.zstack.testlib.collectstrategy

import org.zstack.core.Platform
import org.zstack.testlib.Case
import org.zstack.testlib.Test

import java.lang.reflect.Modifier

class DesignatedSubCaseCollectionStrategy implements SubCaseCollectionStrategy{

    final static String strategyName = "Designated"

    @Override
    List<Class> collectSubCases(Test test) {
        String caseFilePath = System.getProperty("caseFilePath")
        if (caseFilePath == null){
            return new ArrayList<Class>()
        }
        File file = new File(caseFilePath)
        if (!file.exists()){
            return new ArrayList<Class>()
        }
        List<String> runCases = file.readLines().collect { it.trim() }
        if (runCases.isEmpty()) {
            return new ArrayList<Class>()
        }

        List<Class> caseTypes = new ArrayList<Class>()
        if(runCases.isEmpty()){
            return caseTypes
        }
        String testPackage = test.class.package.name
        for (String acase : runCases){
            if (acase.startsWith(testPackage) && (acase.replace(testPackage, "").startsWith("."))){
                caseTypes.add(Class.forName(acase))
            }
        }

        return caseTypes
    }
}
