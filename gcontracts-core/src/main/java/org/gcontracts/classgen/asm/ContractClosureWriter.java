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
package org.gcontracts.classgen.asm;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.classgen.Verifier;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.transform.stc.StaticTypesMarker;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * <p>Replaces annotation closures with closure implementation classes.</p>
 *
 * <p>Attention: large parts of this class have been backported from Groovy 1.8 and customized
 * for usage in GContracts.</p>
 *
 * @author ast
 */
public class ContractClosureWriter {

    private int closureCount = 1;

    public ClassNode createClosureClass(ClassNode classNode, MethodNode methodNode, ClosureExpression expression, boolean addOldVariable, boolean addResultVariable, int mods) {
        ClassNode outerClass = getOutermostClass(classNode);
        String name = outerClass.getName() + "$" + getClosureInnerName(outerClass, classNode);

        // fetch all method parameters, and possibly add 'old' and 'result'
        ArrayList<Parameter> parametersTemp = new ArrayList<Parameter>(Arrays.asList(expression.getParameters()));
        removeParameter("old", parametersTemp);
        removeParameter("result", parametersTemp);

        if (methodNode != null && addResultVariable && methodNode.getReturnType() != ClassHelper.VOID_TYPE)  {
            parametersTemp.add(new Parameter(methodNode.getReturnType(), "result"));
        }

        if (addOldVariable)  {
            parametersTemp.add(new Parameter(new ClassNode(Map.class), "old"));
        }

        // contains all params of the original method
        Parameter[] parameters = parametersTemp.toArray(new Parameter[parametersTemp.size()]);

        Parameter[] localVariableParams = getClosureSharedVariables(expression);
        removeInitialValues(localVariableParams);

        ClassNode answer = new ClassNode(name, mods, ClassHelper.CLOSURE_TYPE.getPlainNodeReference());
        answer.setSynthetic(true);
        answer.setSourcePosition(expression);

        MethodNode method =
                answer.addMethod("doCall", ACC_PUBLIC, ClassHelper.Boolean_TYPE, parameters, ClassNode.EMPTY_ARRAY, expression.getCode());
        method.setSourcePosition(expression);

        VariableScope varScope = expression.getVariableScope();
        if (varScope == null) {
            throw new RuntimeException(
                    "Must have a VariableScope by now! for expression: " + expression + " class: " + name);
        } else {
            method.setVariableScope(varScope.copy());
        }

        // let's add a typesafe call method
        ArgumentListExpression arguments = new ArgumentListExpression();
        for (Parameter parameter : parameters)  {
            arguments.addExpression(new VariableExpression(parameter));
        }

        MethodNode call = answer.addMethod(
                "call",
                ACC_PUBLIC,
                ClassHelper.Boolean_TYPE,
                parameters,
                ClassNode.EMPTY_ARRAY,
                new ReturnStatement(
                        new MethodCallExpression(
                                VariableExpression.THIS_EXPRESSION,
                                "doCall",
                                arguments)));

        call.setSourcePosition(expression);
        call.setSynthetic(true);

        // let's make the constructor
        BlockStatement block = new BlockStatement();
        // this block does not get a source position, because we don't
        // want this synthetic constructor to show up in corbertura reports
        VariableExpression outer = new VariableExpression("_outerInstance");
        outer.setSourcePosition(expression);
        block.getVariableScope().putReferencedLocalVariable(outer);
        VariableExpression thisObject = new VariableExpression("_thisObject");
        thisObject.setSourcePosition(expression);
        block.getVariableScope().putReferencedLocalVariable(thisObject);
        TupleExpression conArgs = new TupleExpression(outer, thisObject);
        block.addStatement(
                new ExpressionStatement(
                        new ConstructorCallExpression(
                                ClassNode.SUPER,
                                conArgs)));

        // let's assign all the parameter fields from the outer context
        for (Parameter param : localVariableParams) {
            String paramName = param.getName();
            ClassNode type = param.getType();
            if (true) {
                VariableExpression initialValue = new VariableExpression(paramName);
                initialValue.setAccessedVariable(param);
                initialValue.setUseReferenceDirectly(true);
                ClassNode realType = type;
                type = ClassHelper.makeReference();
                param.setType(ClassHelper.makeReference());
                FieldNode paramField = answer.addField(paramName, ACC_PRIVATE | ACC_SYNTHETIC, type, initialValue);
                paramField.setDeclaringClass(ClassHelper.getWrapper(param.getOriginType()));
                paramField.setHolder(true);
                String methodName = Verifier.capitalize(paramName);

                // let's add a getter & setter
                Expression fieldExp = new FieldExpression(paramField);
                answer.addMethod(
                        "get" + methodName,
                        ACC_PUBLIC,
                        realType,
                        Parameter.EMPTY_ARRAY,
                        ClassNode.EMPTY_ARRAY,
                        new ReturnStatement(fieldExp));
            }
        }

        Parameter[] params = new Parameter[2];
        params[0] = new Parameter(ClassHelper.OBJECT_TYPE, "_outerInstance");
        params[1] = new Parameter(ClassHelper.OBJECT_TYPE, "_thisObject");

        ASTNode sn = answer.addConstructor(ACC_PUBLIC, params, ClassNode.EMPTY_ARRAY, block);
        sn.setSourcePosition(expression);

        correctAccessedVariable(method, expression);

        return answer;
    }

    private void removeParameter(String name, List<Parameter> parameters)  {
        for (Iterator<Parameter> it = parameters.iterator(); it.hasNext();)  {
            if (it.next().getName().equals(name)) it.remove();
        }
    }

    private ClassNode getOutermostClass(ClassNode outermostClass) {
        while (outermostClass instanceof InnerClassNode) {
            outermostClass = outermostClass.getOuterClass();
        }

        return outermostClass;
    }

    private Parameter[] getClosureSharedVariables(ClosureExpression ce) {
        VariableScope scope = ce.getVariableScope();
        Parameter[] ret = new Parameter[scope.getReferencedLocalVariablesCount()];
        int index = 0;
        for (Iterator iter = scope.getReferencedLocalVariablesIterator(); iter.hasNext();) {
            Variable element = (org.codehaus.groovy.ast.Variable) iter.next();
            Parameter p = new Parameter(element.getType(), element.getName());
            p.setDeclaringClass(element.getOriginType());
            p.setClosureSharedVariable(element.isClosureSharedVariable());
            ret[index] = p;
            index++;
        }
        return ret;
    }

    /*
     * this method is called for local variables shared between scopes.
     * These variables must not have init values because these would
     * then in later steps be used to create multiple versions of the
     * same method, in this case the constructor. A closure should not
     * have more than one constructor!
     */
    private void removeInitialValues(Parameter[] params) {
        for (int i = 0; i < params.length; i++) {
            if (params[i].hasInitialExpression()) {
                Parameter p = new Parameter(params[i].getType(), params[i].getName());
                p.setDeclaringClass(p.getOriginType());
                params[i] = p;
            }
        }
    }

    private void correctAccessedVariable(final MethodNode methodNode, ClosureExpression ce) {

        CodeVisitorSupport visitor = new CodeVisitorSupport() {
            @Override
            public void visitVariableExpression(VariableExpression expression) {
                Variable v = expression.getAccessedVariable();
                if (v==null) return;
                String name = expression.getName();
                if (v instanceof DynamicVariable)  {
                    for (Parameter param : methodNode.getParameters())  {
                        if (name.equals(param.getName()))  {
                            expression.setAccessedVariable(param);
                        }
                    }

                }
            }
        };
        visitor.visitClosureExpression(ce);
    }

    private String getClosureInnerName(ClassNode owner, ClassNode enclosingClass) {
        String ownerShortName = owner.getNameWithoutPackage();
        String classShortName = enclosingClass.getNameWithoutPackage();
        if (classShortName.equals(ownerShortName)) {
            classShortName = "";
        }
        else {
            classShortName += "_";
        }
        // remove $
        int dp = classShortName.lastIndexOf("$");
        if (dp >= 0) {
            classShortName = classShortName.substring(++dp);
        }
        // remove leading _
        if (classShortName.startsWith("_")) {
            classShortName = classShortName.substring(1);
        }

        return "_gc_" + classShortName + "closure" + closureCount++;
    }
}
