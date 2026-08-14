package org.zstack.testlib

import org.zstack.core.Platform
import org.zstack.utils.FieldUtils
import org.zstack.utils.ShellUtils

import java.lang.reflect.Method
import java.lang.reflect.Parameter

/**
 * Created by xing5 on 2017/2/24.
 */
class EnvDSLDocGenerator {
    def reflections = Platform.reflections

    class MethodSpec {
        Class delegateClass
        Method method

        MethodSpec(Class delegateClass, Method method) {
            this.delegateClass = delegateClass
            this.method = method
        }
    }

    class SpecNode {
        Class specClass
        Set<MethodSpec> delegatingMethods = []
    }

    Map<Class, SpecNode> specNodes = [:]

    EnvDSLDocGenerator() {
        reflections.getSubTypesOf(Spec.class).each {
            def snode = new SpecNode()
            snode.specClass = it
            specNodes[it] = snode
        }

        reflections.getMethodsWithAnyParamAnnotated(DelegatesTo.class).each { method ->
            SpecNode n = specNodes[method.declaringClass]
            if (n == null) {
                println("cannot find the SpecNode for the class[${method.declaringClass}], skip it")
                return
            }

            method.parameters.each { param ->
                DelegatesTo at = param.getAnnotation(DelegatesTo.class)
                if (at != null) {
                    n.delegatingMethods.add(new MethodSpec(at.value(), method))
                }
            }
        }
    }

    private void resolveDependency(SpecNode node, Set<SpecNode> resolved) {
        if (resolved.contains(node)) {
            return
        }

        for (MethodSpec ms : node.delegatingMethods) {
            SpecNode dnode = specNodes[ms.delegateClass]
            assert dnode != null: "cannot find the spec for the class[${ms.delegateClass}]"

            resolveDependency(dnode, resolved)
        }

        resolved.add(node)
    }

    void generate() {
        String tmpDir = System.getProperty("java.io.tmpdir")
        def root = new File([tmpDir, "zstack", "env"].join("/"))
        root.deleteDir()
        root.mkdirs()

        try {
            EnvSpec.class.methods.each { m ->
                Parameter parameter = m.parameters.find { it.isAnnotationPresent(DelegatesTo.class) }
                if (parameter != null) {
                    DelegatesTo at = parameter.getAnnotation(DelegatesTo.class)

                    SpecNode node = specNodes[at.value()]
                    assert node != null: "cannot find SpecNode for the class[${at.value()}]"

                    drawNodes(node, new HashSet<SpecNode>(), [root.absolutePath, m.name])
                }
            }

            def tree = ShellUtils.run("sync; sleep 0.5; find . -type f -exec touch {} +; tree -c", root.parentFile.absolutePath)
            tree = tree.replaceAll("\\\\", "")
            new File("envDSLTree").write(tree)
        } finally {
            root.deleteDir()
        }
    }

    private void drawNodes(SpecNode node, Set<SpecNode> resolved, List<String> paths) {
        if (resolved.contains(node)) {
            return
        }

        resolved.add(node)

        String currentPath = paths.join("/")
        new File(currentPath).mkdirs()

        FieldUtils.getAnnotatedFields(SpecParam.class, node.specClass).each { field ->
            SpecParam at = field.getAnnotation(SpecParam.class)
            String name = "(field ${at.required() ? "required" : "optional"}) ${field.name}"
            new File([currentPath, name].join("/")).createNewFile()
        }

        node.specClass.methods.each { m ->
            SpecMethod at = m.getAnnotation(SpecMethod.class)
            if (at == null) {
                return
            }

            String name = "(method) ${m.name}"
            new File([currentPath, name].join("/")).createNewFile()
        }

        node.delegatingMethods.each { sm ->
            paths.push(sm.method.name)

            File nodeFile = new File(paths.join("/"))
            nodeFile.mkdirs()

            SpecNode cnode = specNodes[sm.delegateClass]
            assert cnode != null: "cannot find spec node for the class[${sm.delegateClass}]"
            drawNodes(cnode, resolved, paths)

            paths.pop()
        }
    }
}
