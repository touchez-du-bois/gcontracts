/**
 * Copyright (c) 2011, Andre Steingress
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1.) Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 * 2.) Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * 3.) Neither the name of Andre Steingress nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.gcontracts.generation;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.gcontracts.ast.visitor.AnnotationClosureVisitor;

/**
 * <p>
 * Functions in this class are used to determine whether a certain AST node fulfills certain assertion
 * requirements. E.g. whether a class node is a class invariant candidate or not.
 * </p>
 *
 * @author ast
 */
public class CandidateChecks {

    /**
     * Checks whether the given {@link org.codehaus.groovy.ast.ClassNode} is a candidate
     * for applying contracts. <p/>
     *
     * If the given class node has already been processed in this compilation run, this
     * method will return <tt>false</tt>.
     *
     * @param type the {@link org.codehaus.groovy.ast.ClassNode} to be checked
     * @return whether the given <tt>type</tt> is a candidate for applying contract assertions
     */
    public static boolean isContractsCandidate(final ClassNode type)  {
        return
               type != null && !type.isSynthetic() && !type.isInterface() && !type.isEnum() && !type.isGenericsPlaceHolder() && !type.isScript() && !type.isScriptBody() && !isRuntimeClass(type);
    }

    /**
     * Checks whether the given {@link org.codehaus.groovy.ast.ClassNode} is a candidate
     * for applying interface contracts.
     *
     * @param type the {@link org.codehaus.groovy.ast.ClassNode} to be checked
     * @return whether the given <tt>type</tt> is a candidate for applying interface contract assertions
     */
    public static boolean isInterfaceContractsCandidate(final ClassNode type)  {
        return type != null && type.isInterface() && !type.isSynthetic() && !type.isEnum() && !type.isGenericsPlaceHolder() && !type.isScript() && !type.isScriptBody() && !isRuntimeClass(type);
    }

    /**
     * Decides whether the given <tt>propertyNode</tt> is a candidate for class invariant injection.
     *
     * @param propertyNode the {@link org.codehaus.groovy.ast.PropertyNode} to check
     * @return whether the <tt>propertyNode</tt> is a candidate for injecting the class invariant or not
     */
    public static boolean isClassInvariantCandidate(final PropertyNode propertyNode)  {
        return propertyNode != null &&
                propertyNode.isPublic() && !propertyNode.isStatic() && !propertyNode.isInStaticContext() && !propertyNode.isClosureSharedVariable();
    }

    /**
     * Decides whether the given <tt>method</tt> is a candidate for a pre- or postcondition.
     *
     * @param type the current {@link org.codehaus.groovy.ast.ClassNode}
     * @param method the {@link org.codehaus.groovy.ast.MethodNode} to check for pre- or postcondition compliance
     * @return whether the given {@link org.codehaus.groovy.ast.MethodNode} is a candidate for pre- or postconditions 
     */
    public static boolean isPreOrPostconditionCandidate(final ClassNode type, final MethodNode method)  {
        if (!isPreconditionCandidate(type, method) && !isPostconditionCandidate(type, method)) return false;

        return true;
    }

    /**
     * Decides whether the given <tt>method</tt> is a candidate for class invariants.
     *
     * @param type the current {@link org.codehaus.groovy.ast.ClassNode}
     * @param method the {@link org.codehaus.groovy.ast.MethodNode} to check for class invariant compliance
     * @return whether the given {@link org.codehaus.groovy.ast.MethodNode} is a candidate for class invariants
     */
    public static boolean isClassInvariantCandidate(final ClassNode type, final MethodNode method)  {
        if (method.isSynthetic() || method.isAbstract() || method.isStatic() || !method.isPublic()) return false;
        if (method.getDeclaringClass() != type) return false;

        return true;
    }

    /**
     * Decides whether the given <tt>method</tt> is a candidate for a pre-condition.
     *
     * @param type the current {@link org.codehaus.groovy.ast.ClassNode}
     * @param method the {@link org.codehaus.groovy.ast.MethodNode} to check for pre-condition compliance
     * @return whether the given {@link org.codehaus.groovy.ast.MethodNode} is a candidate for pre-conditions
     */
    public static boolean isPreconditionCandidate(final ClassNode type, final MethodNode method)  {
        if (method.isSynthetic() || method.isAbstract()) return false;
        if (method.getDeclaringClass() != type) return false;

        return true;
    }

    /**
     * Decides whether the given <tt>method</tt> is a candidate for a post-condition.
     *
     * @param type the current {@link org.codehaus.groovy.ast.ClassNode}
     * @param method the {@link org.codehaus.groovy.ast.MethodNode} to check for post-condition compliance
     * @return whether the given {@link org.codehaus.groovy.ast.MethodNode} is a candidate for post-conditions
     */
    public static boolean isPostconditionCandidate(final ClassNode type, final MethodNode method)  {
        if (!isPreconditionCandidate(type, method)) return false;
        if (method.isStatic()) return false;

        return true;
    }

    /**
     * Checks whether the given {@link MethodNode} could be a candidate for an arbitrary {@link org.gcontracts.annotations.meta.ContractElement}
     * annotation.
     *
     * @param type the current {@link org.codehaus.groovy.ast.ClassNode}
     * @param method the {@link org.codehaus.groovy.ast.MethodNode} to check for {@link org.gcontracts.annotations.meta.ContractElement} compliance
     * @return whether the given method node could be a candidate or not
     */
    public static boolean couldBeContractElementMethodNode(final ClassNode type, final MethodNode method)  {
        if (method.isSynthetic() || !method.isPublic()) return false;
        if (method.getDeclaringClass() != null && !method.getDeclaringClass().getName().equals(type.getName())) return false;

        return true;
    }

    /**
     * Checks whether the given {@link ClassNode} is part of the Groovy/Java runtime.
     *
     * @param type the current {@link org.codehaus.groovy.ast.ClassNode}
     * @return <tt>true</tt> whether the current {@link org.codehaus.groovy.ast.ClassNode} is a Groovy/Java system class
     */
    public static boolean isRuntimeClass(final ClassNode type)  {
        return type.getName().startsWith("groovy.") || type.getName().startsWith("java.");
    }
}
