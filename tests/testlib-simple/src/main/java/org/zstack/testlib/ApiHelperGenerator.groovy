package org.zstack.testlib

import org.zstack.core.Platform
import org.zstack.sdk.AbstractAction
import org.zstack.sdk.QueryAction

import java.lang.reflect.Modifier

/**
 * Created by xing5 on 2017/2/14.
 */
class ApiHelperGenerator {
    Set<Class> actions
    List<String> groovyActions = []

    ApiHelperGenerator() {
        def reflections = Platform.getReflections()
        actions = reflections.getSubTypesOf(AbstractAction.class).sort { a1, a2 -> (a1.name <=> a2.name) }
    }

    String generate(String outputFilePath) {
        actions.each { actionClass ->
            if (Modifier.isAbstract(actionClass.modifiers)) {
                return
            }

            String queryConditionManipulate = {
                if (!QueryAction.isAssignableFrom(actionClass)) {
                    return ""
                }

                return """
        a.conditions = a.conditions.collect { it.toString() }
"""
            }()

            String funcName = actionClass.simpleName - "Action"
            funcName = "${Character.toLowerCase(funcName.charAt(0))}${funcName.substring(1)}"
            groovyActions.add("""\
    def $funcName(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = ${actionClass.typeName}.class) Closure c) {
        def a = new ${actionClass.typeName}()
        ${actionClass.fields.find {it.name == "sessionId"} != null ? "a.sessionId = Test.currentEnvSpec?.session?.uuid" : ""}
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        $queryConditionManipulate

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }

""")
        }

        def fileContent = """package org.zstack.testlib

import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.core.Platform

abstract class ApiHelper {
    def errorOut(res) {
        assert res.error == null : "API failure: \${JSONObjectUtil.toJsonString(res.error)}"
        if (res.value.hasProperty("inventory")) {
            return res.value.inventory
        } else if (res.value.hasProperty("inventories")) {
            return res.value.inventories
        } else {
            return res.value
        }
    }
    
    ${groovyActions.join("\n")}
}
"""
        def dir = new File(outputFilePath).parentFile
        if (!dir.exists()) {
            dir.mkdirs()
        }

        new File(outputFilePath).write(fileContent)
    }
}
