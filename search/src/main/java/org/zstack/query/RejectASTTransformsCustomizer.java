/*
 * The MIT License
 *
 * Copyright (c) 2018, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.zstack.query;

import com.google.common.collect.ImmutableList;
import groovy.lang.Grab;
import groovy.lang.GrabConfig;
import groovy.lang.GrabExclude;
import groovy.lang.GrabResolver;
import groovy.lang.Grapes;
import groovy.transform.ASTTest;
import groovy.transform.AnnotationCollector;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

import java.util.ArrayList;
import java.util.List;

/**
 * fork from https://github.com/jenkinsci/script-security-plugin
 *
 * @author  jenkinsci/script-security-plugin
 * @see     https://github.com/jenkinsci/script-security-plugin/blob/script-security-1.76/src/main/java/org/jenkinsci/plugins/scriptsecurity/sandbox/groovy/RejectASTTransformsCustomizer.java
 * @since   1.50
 */
public class RejectASTTransformsCustomizer extends CompilationCustomizer {
    private static final List<String> BLOCKED_TRANSFORMS = ImmutableList.of(ASTTest.class.getCanonicalName(), Grab.class.getCanonicalName(),
            GrabConfig.class.getCanonicalName(), GrabExclude.class.getCanonicalName(), GrabResolver.class.getCanonicalName(),
            Grapes.class.getCanonicalName(), AnnotationCollector.class.getCanonicalName());

    public RejectASTTransformsCustomizer() {
        super(CompilePhase.CONVERSION);
    }

    @Override
    public void call(final SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
        new RejectASTTransformsVisitor(source).visitClass(classNode);
    }

    // Note: Methods in this visitor that override methods from the superclass should call the implementation from the
    // superclass to ensure that any nested AST nodes are traversed.
    private static class RejectASTTransformsVisitor extends ClassCodeVisitorSupport {
        private SourceUnit source;

        public RejectASTTransformsVisitor(SourceUnit source) {
            this.source = source;
        }

        @Override
        protected SourceUnit getSourceUnit() {
            return source;
        }

        @Override
        public void visitImports(ModuleNode node) {
            if (node != null) {
                for (ImportNode importNode : node.getImports()) {
                    checkImportForBlockedAnnotation(importNode);
                }
                for (ImportNode importStaticNode : node.getStaticImports().values()) {
                    checkImportForBlockedAnnotation(importStaticNode);
                }
            }
            super.visitImports(node);
        }

        private void checkImportForBlockedAnnotation(ImportNode node) {
            if (node != null && node.getType() != null) {
                for (String blockedAnnotation : getBlockedTransforms()) {
                    if (blockedAnnotation.equals(node.getType().getName()) || blockedAnnotation.endsWith("." + node.getType().getName())) {
                        throw new SecurityException("Annotation " + node.getType().getName() + " cannot be used in the sandbox.");
                    }
                }
            }
        }

        /**
         * If the node is annotated with one of the blocked transform annotations, throw a security exception.
         *
         * @param node the node to process
         */
        @Override
        public void visitAnnotations(AnnotatedNode node) {
            for (AnnotationNode an : node.getAnnotations()) {
                for (String blockedAnnotation : getBlockedTransforms()) {
                    if (blockedAnnotation.equals(an.getClassNode().getName()) || blockedAnnotation.endsWith("." + an.getClassNode().getName())) {
                        throw new SecurityException("Annotation " + an.getClassNode().getName() + " cannot be used in the sandbox.");
                    }
                }
            }
            super.visitAnnotations(node);
        }
    }

    private static List<String> getBlockedTransforms() {
        List<String> blocked = new ArrayList<>(BLOCKED_TRANSFORMS);

        String additionalBlocked = System.getProperty(RejectASTTransformsCustomizer.class.getName() + ".ADDITIONAL_BLOCKED_TRANSFORMS");

        if (additionalBlocked != null) {
            for (String b : additionalBlocked.split(",")) {
                blocked.add(b.trim());
            }
        }

        return blocked;
    }
}
