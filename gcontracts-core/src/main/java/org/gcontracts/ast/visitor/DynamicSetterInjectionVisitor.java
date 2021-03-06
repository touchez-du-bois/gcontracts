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
package org.gcontracts.ast.visitor;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.io.ReaderSource;
import org.codehaus.groovy.runtime.MetaClassHelper;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.gcontracts.generation.BaseGenerator;
import org.gcontracts.generation.CandidateChecks;
import org.gcontracts.util.AnnotationUtils;
import org.objectweb.asm.Opcodes;

import java.util.List;

/**
 * <p>
 * Implements contract support for setter methods and default constructors of POGOs.
 * </p>
 *
 * @see BaseVisitor
 *
 * @author ast
 */
public class DynamicSetterInjectionVisitor extends BaseVisitor {

    private static final String SPRING_STEREOTYPE_PACKAGE = "org.springframework.stereotype";

    private BlockStatement invariantAssertionBlockStatement;

    public DynamicSetterInjectionVisitor(final SourceUnit sourceUnit, final ReaderSource source) {
        super(sourceUnit, source);
    }

    protected Statement createSetterBlock(final ClassNode classNode, final FieldNode field, final Parameter parameter) {
        final BlockStatement setterMethodBlock = new BlockStatement();

        // check invariant before assignment
        setterMethodBlock.addStatement(invariantAssertionBlockStatement);

        // do assignment
        BinaryExpression fieldAssignment = new BinaryExpression(new FieldExpression(field), Token.newSymbol(Types.ASSIGN, -1, -1), new VariableExpression(parameter));
        setterMethodBlock.addStatement(new org.codehaus.groovy.ast.stmt.ExpressionStatement(fieldAssignment));


        // check invariant after assignment
        setterMethodBlock.addStatement(invariantAssertionBlockStatement);

        return setterMethodBlock;
    }

    @Override
    public void visitProperty(PropertyNode node) {
        final ClassNode classNode = node.getDeclaringClass();
        final String setterName = "set" + MetaClassHelper.capitalize(node.getName());

        final Statement setterBlock = node.getSetterBlock();
        final Parameter parameter = new Parameter(node.getType(), "value");

        if (CandidateChecks.isClassInvariantCandidate(node) && (setterBlock == null && classNode.getMethod(setterName, new Parameter[]{ parameter } ) == null)) {
            final Statement setterBlockStatement = createSetterBlock(classNode, node.getField(), parameter);
            node.setSetterBlock(setterBlockStatement);
        }
    }

    @Override
    public void visitClass(ClassNode classNode) {
        // if a class invariant is available visit all property nodes else skip this class
        final MethodNode invariantMethodNode = BaseGenerator.getInvariantMethodNode(classNode);
        if (invariantMethodNode == null || AnnotationUtils.hasAnnotationOfType(classNode, SPRING_STEREOTYPE_PACKAGE)) return;

        invariantAssertionBlockStatement = new BlockStatement();
        invariantAssertionBlockStatement.addStatement(new ExpressionStatement(
                new MethodCallExpression(VariableExpression.THIS_EXPRESSION, invariantMethodNode.getName(), ArgumentListExpression.EMPTY_ARGUMENTS)
        ));

        List<ConstructorNode> declaredConstructors = classNode.getDeclaredConstructors();
        if (declaredConstructors == null || declaredConstructors.isEmpty())  {
            // create default constructor with class invariant check
            ConstructorNode constructor = new ConstructorNode(Opcodes.ACC_PUBLIC, invariantAssertionBlockStatement);
            constructor.setSynthetic(true);
            classNode.addConstructor(constructor);
        }

        super.visitClass(classNode);
    }
}
