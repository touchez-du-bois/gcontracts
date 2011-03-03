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
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.stmt.AssertStatement;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.io.ReaderSource;
import org.gcontracts.annotations.meta.ContractElement;
import org.gcontracts.classgen.asm.ClosureWriter;
import org.gcontracts.generation.AssertStatementCreationUtility;
import org.gcontracts.generation.CandidateChecks;
import org.gcontracts.util.AnnotationUtils;
import org.gcontracts.util.ExpressionUtils;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Visits interfaces and looks for <tt>@Requires</tt> or <tt>@Ensures</tt> and creates {@link groovy.lang.Closure}
 * classes for the annotation closures.<p/>
 *
 * The annotation closure classes are used later on to check interface contract pre- and post-conditions in
 * implementation classes.
 *
 * @see org.gcontracts.annotations.Requires
 * @see org.gcontracts.annotations.Ensures
 *
 * @see org.gcontracts.ast.visitor.BaseVisitor
 *
 * @author ast
 */
public class InterfaceContractVisitor extends BaseVisitor {

    private ClassNode classNode;
    private final ClosureWriter closureWriter = new ClosureWriter();

    public InterfaceContractVisitor(final SourceUnit sourceUnit, final ReaderSource source) {
        super(sourceUnit, source);
    }

    @Override
    public void visitClass(ClassNode node) {
        if (!CandidateChecks.isInterfaceContractsCandidate(node)) return;

        classNode = node;

        super.visitClass(node);
    }

    @Override
    public void visitAnnotations(AnnotatedNode node) {
        if (!(node instanceof MethodNode)) return;

        final MethodNode methodNode = (MethodNode) node;
        if (!methodNode.isAbstract()) return;

        final List<AnnotationNode> annotationNodes = AnnotationUtils.hasMetaAnnotations(methodNode, ContractElement.class.getName());
        if (annotationNodes.size() > 0)  {
            for (AnnotationNode annotationNode : annotationNodes)  {
                boolean isPostcondition = AnnotationUtils.hasAnnotationOfType(annotationNode.getClassNode(), org.gcontracts.annotations.meta.Postcondition.class.getName());

                ClosureExpression closureExpression = (ClosureExpression) annotationNode.getMember(CLOSURE_ATTRIBUTE_NAME);
                if (closureExpression == null) continue;

                List<Parameter> parameters = new ArrayList<Parameter>(Arrays.asList(closureExpression.getParameters()));

                parameters.addAll(new ArrayList<Parameter>(Arrays.asList(methodNode.getParameters())));

                final BooleanExpression booleanExpression = ExpressionUtils.getBooleanExpression(closureExpression);
                if (booleanExpression == null) continue;

                final AssertStatement assertStatement = AssertStatementCreationUtility.getAssertionStatement(booleanExpression);
                final String assertionType = isPostcondition ? "postcondition" : "precondition";

                BlockStatement blockStatement = (BlockStatement) closureExpression.getCode();
                blockStatement.getStatements().clear();
                blockStatement.addStatement(AssertStatementCreationUtility.createInheritedAssertStatement(assertionType, methodNode, assertStatement));
                blockStatement.addStatement(new ReturnStatement(ConstantExpression.TRUE));

                ClosureExpression rewrittenClosureExpression = new ClosureExpression(parameters.toArray(new Parameter[parameters.size()]), blockStatement);
                rewrittenClosureExpression.setSourcePosition(closureExpression);
                rewrittenClosureExpression.setDeclaringClass(closureExpression.getDeclaringClass());
                rewrittenClosureExpression.setSynthetic(true);
                rewrittenClosureExpression.setVariableScope(closureExpression.getVariableScope());
                rewrittenClosureExpression.setType(closureExpression.getType());

                ClassNode closureClassNode = closureWriter.createClosureClass(classNode, methodNode, rewrittenClosureExpression, isPostcondition, Opcodes.ACC_PUBLIC);
                classNode.getModule().addClass(closureClassNode);

                annotationNode.setMember(CLOSURE_ATTRIBUTE_NAME, new ClassExpression(closureClassNode));
            }
        }
        super.visitAnnotations(node);
    }
}