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

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.control.io.ReaderSource;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;

import java.util.List;

/**
 * <p>
 * Code generator for postconditions.
 * </p>
 *
 * @author ast
 */
public class PostconditionGenerator extends BaseGenerator {

    public PostconditionGenerator(final ReaderSource source) {
        super(source);
    }

    /**
     * Adds a synthetic method to the given <tt>classNode</tt> which can be used
     * to create a map of most instance variables found in this class. Used for the <tt>old</tt> variable
     * mechanism.
     *
     * @param classNode the {@link org.codehaus.groovy.ast.ClassNode} to add the synthetic method to
     */
    public void addOldVariablesMethod(final ClassNode classNode)  {
        VariableGenerationUtility.addOldVariableMethodNode(classNode);
    }

    /**
     * Injects a postcondition assertion statement in the given <tt>method</tt>, based on the given <tt>annotation</tt> of
     * type {@link org.gcontracts.annotations.Ensures}.
     *
     * @param method the {@link org.codehaus.groovy.ast.MethodNode} for assertion injection
     * @param closureExpression the {@link org.codehaus.groovy.ast.expr.ClosureExpression} containing the assertion expression
     */
    @Deprecated
    public void generatePostconditionAssertionStatement(MethodNode method, ClosureExpression closureExpression)  {

        final BlockStatement methodBlock = (BlockStatement) method.getCode();

        // if return type is not void, than a "result" variable is provided in the postcondition expression
        final List<Statement> statements = methodBlock.getStatements();
        if (statements.size() > 0)  {
            final BlockStatement postconditionCheck = new BlockStatement();

            if (method.getReturnType() != ClassHelper.VOID_TYPE)  {
                Statement lastStatement = statements.get(statements.size() - 1);

                ReturnStatement returnStatement = AssertStatementCreationUtility.getReturnStatement(method.getDeclaringClass(), method, lastStatement);
                if (returnStatement != null) statements.remove(statements.size() - 1);

                final IfStatement assertionIfStatement = AssertStatementCreationUtility.getAssertionStatement("postcondition", method, closureExpression);
                final AssertStatement assertionStatement = AssertStatementCreationUtility.getAssertStatement(assertionIfStatement);

                // backup the current assertion in a synthetic method
                AssertStatementCreationUtility.addAssertionMethodNode("postcondition", method, assertionStatement, true, returnStatement != null);

                final MethodCallExpression methodCallToSuperPostcondition = AssertStatementCreationUtility.getMethodCallExpressionToSuperClassPostcondition(method, assertionIfStatement.getLineNumber(), true, returnStatement != null);
                if (methodCallToSuperPostcondition != null) AssertStatementCreationUtility.addToAssertStatement(assertionStatement, methodCallToSuperPostcondition, Token.newSymbol(Types.LOGICAL_AND, -1, -1));

                postconditionCheck.addStatement(assertionIfStatement);

                VariableExpression resultVariable = null;

                if (returnStatement != null)  {
                    // Assign the return statement expression to a local variable of type Object
                    resultVariable = new VariableExpression("result");
                    ExpressionStatement resultVariableStatement = new ExpressionStatement(
                    new DeclarationExpression(resultVariable,
                            Token.newSymbol(Types.ASSIGN, -1, -1),
                            returnStatement.getExpression()));

                    postconditionCheck.getStatements().add(0, resultVariableStatement);
                }

                // Assign the return statement expression to a local variable of type Object
                VariableExpression oldVariable = new VariableExpression("old");
                ExpressionStatement oldVariabeStatement = new ExpressionStatement(
                new DeclarationExpression(oldVariable,
                        Token.newSymbol(Types.ASSIGN, -1, -1),
                        new MethodCallExpression(VariableExpression.THIS_EXPRESSION, VariableGenerationUtility.OLD_VARIABLES_METHOD, ArgumentListExpression.EMPTY_ARGUMENTS)));

                methodBlock.getStatements().add(0, oldVariabeStatement);
                methodBlock.addStatements(postconditionCheck.getStatements());
                if (returnStatement != null) methodBlock.addStatement(new ReturnStatement(resultVariable));
            } else if (method instanceof ConstructorNode) {

                final IfStatement assertionIfStatement = AssertStatementCreationUtility.getAssertionStatement("postcondition", method, closureExpression);
                final AssertStatement assertionStatement = AssertStatementCreationUtility.getAssertStatement(assertionIfStatement);

                // backup the current assertion in a synthetic method
                AssertStatementCreationUtility.addAssertionMethodNode("postcondition", method, assertionStatement, false, false);

                final MethodCallExpression methodCallToSuperPostcondition = AssertStatementCreationUtility.getMethodCallExpressionToSuperClassPostcondition(method, assertionIfStatement.getLineNumber(), false, false);

                if (methodCallToSuperPostcondition != null) AssertStatementCreationUtility.addToAssertStatement(assertionStatement, methodCallToSuperPostcondition, Token.newSymbol(Types.LOGICAL_AND, -1, -1));

                postconditionCheck.addStatement(assertionIfStatement);
                methodBlock.addStatements(postconditionCheck.getStatements());

            } else {

                final IfStatement assertionIfStatement = AssertStatementCreationUtility.getAssertionStatement("postcondition", method, closureExpression);
                final AssertStatement assertionStatement = AssertStatementCreationUtility.getAssertStatement(assertionIfStatement);

                // backup the current assertion in a synthetic method
                AssertStatementCreationUtility.addAssertionMethodNode("postcondition", method, assertionStatement, true, false);

                final MethodCallExpression methodCallToSuperPostcondition = AssertStatementCreationUtility.getMethodCallExpressionToSuperClassPostcondition(method, assertionIfStatement.getLineNumber(), true, false);

                if (methodCallToSuperPostcondition != null) AssertStatementCreationUtility.addToAssertStatement(assertionStatement, methodCallToSuperPostcondition, Token.newSymbol(Types.LOGICAL_AND, -1, -1));

                postconditionCheck.addStatement(assertionIfStatement);

                // Assign the return statement expression to a local variable of type Object
                VariableExpression oldVariable = new VariableExpression("old");
                ExpressionStatement oldVariabeStatement = new ExpressionStatement(
                new DeclarationExpression(oldVariable,
                        Token.newSymbol(Types.ASSIGN, -1, -1),
                        new MethodCallExpression(VariableExpression.THIS_EXPRESSION, VariableGenerationUtility.OLD_VARIABLES_METHOD, ArgumentListExpression.EMPTY_ARGUMENTS)));

                methodBlock.getStatements().add(0, oldVariabeStatement);
                methodBlock.addStatements(postconditionCheck.getStatements());
            }
        }
    }

    public void generatePostconditionAssertionStatement(MethodNode method, BooleanExpression closureExpression)  {

        final BlockStatement methodBlock = (BlockStatement) method.getCode();

        // if return type is not void, than a "result" variable is provided in the postcondition expression
        final List<Statement> statements = methodBlock.getStatements();
        if (statements.size() > 0)  {
            final BlockStatement postconditionCheck = new BlockStatement();

            if (method.getReturnType() != ClassHelper.VOID_TYPE)  {
                Statement lastStatement = statements.get(statements.size() - 1);

                ReturnStatement returnStatement = AssertStatementCreationUtility.getReturnStatement(method.getDeclaringClass(), method, lastStatement);
                if (returnStatement != null) statements.remove(statements.size() - 1);

                final IfStatement assertionIfStatement = AssertStatementCreationUtility.getAssertionStatement("postcondition", method, closureExpression);
                final AssertStatement assertionStatement = AssertStatementCreationUtility.getAssertStatement(assertionIfStatement);

                // backup the current assertion in a synthetic method
                AssertStatementCreationUtility.addAssertionMethodNode("postcondition", method, assertionStatement, true, returnStatement != null);

                final MethodCallExpression methodCallToSuperPostcondition = AssertStatementCreationUtility.getMethodCallExpressionToSuperClassPostcondition(method, assertionIfStatement.getLineNumber(), true, returnStatement != null);
                if (methodCallToSuperPostcondition != null) AssertStatementCreationUtility.addToAssertStatement(assertionStatement, methodCallToSuperPostcondition, Token.newSymbol(Types.LOGICAL_AND, -1, -1));

                postconditionCheck.addStatement(assertionIfStatement);

                VariableExpression resultVariable = null;

                if (returnStatement != null)  {
                    // Assign the return statement expression to a local variable of type Object
                    resultVariable = new VariableExpression("result");
                    ExpressionStatement resultVariableStatement = new ExpressionStatement(
                    new DeclarationExpression(resultVariable,
                            Token.newSymbol(Types.ASSIGN, -1, -1),
                            returnStatement.getExpression()));

                    postconditionCheck.getStatements().add(0, resultVariableStatement);
                }

                // Assign the return statement expression to a local variable of type Object
                VariableExpression oldVariable = new VariableExpression("old");
                ExpressionStatement oldVariabeStatement = new ExpressionStatement(
                new DeclarationExpression(oldVariable,
                        Token.newSymbol(Types.ASSIGN, -1, -1),
                        new MethodCallExpression(VariableExpression.THIS_EXPRESSION, VariableGenerationUtility.OLD_VARIABLES_METHOD, ArgumentListExpression.EMPTY_ARGUMENTS)));

                methodBlock.getStatements().add(0, oldVariabeStatement);
                methodBlock.addStatements(postconditionCheck.getStatements());
                if (returnStatement != null) methodBlock.addStatement(new ReturnStatement(resultVariable));
            } else if (method instanceof ConstructorNode) {

                final IfStatement assertionIfStatement = AssertStatementCreationUtility.getAssertionStatement("postcondition", method, closureExpression);
                final AssertStatement assertionStatement = AssertStatementCreationUtility.getAssertStatement(assertionIfStatement);

                // backup the current assertion in a synthetic method
                AssertStatementCreationUtility.addAssertionMethodNode("postcondition", method, assertionStatement, false, false);

                final MethodCallExpression methodCallToSuperPostcondition = AssertStatementCreationUtility.getMethodCallExpressionToSuperClassPostcondition(method, assertionIfStatement.getLineNumber(), false, false);

                if (methodCallToSuperPostcondition != null) AssertStatementCreationUtility.addToAssertStatement(assertionStatement, methodCallToSuperPostcondition, Token.newSymbol(Types.LOGICAL_AND, -1, -1));

                postconditionCheck.addStatement(assertionIfStatement);
                methodBlock.addStatements(postconditionCheck.getStatements());

            } else {

                final IfStatement assertionIfStatement = AssertStatementCreationUtility.getAssertionStatement("postcondition", method, closureExpression);
                final AssertStatement assertionStatement = AssertStatementCreationUtility.getAssertStatement(assertionIfStatement);

                // backup the current assertion in a synthetic method
                AssertStatementCreationUtility.addAssertionMethodNode("postcondition", method, assertionStatement, true, false);

                final MethodCallExpression methodCallToSuperPostcondition = AssertStatementCreationUtility.getMethodCallExpressionToSuperClassPostcondition(method, assertionIfStatement.getLineNumber(), true, false);

                if (methodCallToSuperPostcondition != null) AssertStatementCreationUtility.addToAssertStatement(assertionStatement, methodCallToSuperPostcondition, Token.newSymbol(Types.LOGICAL_AND, -1, -1));

                postconditionCheck.addStatement(assertionIfStatement);

                // Assign the return statement expression to a local variable of type Object
                VariableExpression oldVariable = new VariableExpression("old");
                ExpressionStatement oldVariabeStatement = new ExpressionStatement(
                new DeclarationExpression(oldVariable,
                        Token.newSymbol(Types.ASSIGN, -1, -1),
                        new MethodCallExpression(VariableExpression.THIS_EXPRESSION, VariableGenerationUtility.OLD_VARIABLES_METHOD, ArgumentListExpression.EMPTY_ARGUMENTS)));

                methodBlock.getStatements().add(0, oldVariabeStatement);
                methodBlock.addStatements(postconditionCheck.getStatements());
            }
        }
    }

    /**
     * Adds a default postcondition if a postcondition has already been defined for this {@link org.codehaus.groovy.ast.MethodNode}
     * in a super-class.
     *
     * @param type the current {@link org.codehaus.groovy.ast.ClassNode} of the given <tt>methodNode</tt>
     * @param methodNode the {@link org.codehaus.groovy.ast.MethodNode} to create the default postcondition for
     */
    public void generateDefaultPostconditionStatement(final ClassNode type, final MethodNode methodNode)  {

        final BlockStatement methodBlock = (BlockStatement) methodNode.getCode();

        // if return type is not void, than a "result" variable is provided in the postcondition expression
        final List<Statement> statements = methodBlock.getStatements();
        if (statements.size() > 0)  {
            final BlockStatement postconditionCheck = new BlockStatement();

            if (methodNode.getReturnType() != ClassHelper.VOID_TYPE)  {
                Statement lastStatement = statements.get(statements.size() - 1);
                ReturnStatement returnStatement = AssertStatementCreationUtility.getReturnStatement(type, methodNode, lastStatement);

                final MethodCallExpression methodCallToSuperPostcondition = AssertStatementCreationUtility.getMethodCallExpressionToSuperClassPostcondition(methodNode, methodNode.getLineNumber(), true, returnStatement != null);
                if (methodCallToSuperPostcondition == null) return;

                if (returnStatement != null) statements.remove(statements.size() - 1);

                // Assign the return statement expression to a local variable of type Object
                VariableExpression resultVariable = null;

                if (returnStatement != null)  {
                    resultVariable = new VariableExpression("result");
                    ExpressionStatement resultVariableStatement = new ExpressionStatement(
                        new DeclarationExpression(resultVariable,
                            Token.newSymbol(Types.ASSIGN, -1, -1),
                            returnStatement.getExpression()));

                    postconditionCheck.addStatement(resultVariableStatement);
                }

                // Assign the return statement expression to a local variable of type Object
                VariableExpression oldVariable = new VariableExpression("old");
                ExpressionStatement oldVariabeStatement = new ExpressionStatement(
                new DeclarationExpression(oldVariable,
                        Token.newSymbol(Types.ASSIGN, -1, -1),
                        new MethodCallExpression(VariableExpression.THIS_EXPRESSION, VariableGenerationUtility.OLD_VARIABLES_METHOD, ArgumentListExpression.EMPTY_ARGUMENTS)));


                postconditionCheck.addStatement(new ExpressionStatement(methodCallToSuperPostcondition));

                methodBlock.getStatements().add(0, oldVariabeStatement);
                methodBlock.addStatements(postconditionCheck.getStatements());
                if (returnStatement != null) methodBlock.addStatement(new ReturnStatement(resultVariable));
                
            } else {

                final MethodCallExpression methodCallToSuperPostcondition = AssertStatementCreationUtility.getMethodCallExpressionToSuperClassPostcondition(methodNode, methodNode.getLineNumber(), true, false);
                if (methodCallToSuperPostcondition == null) return;

                // Assign the return statement expression to a local variable of type Object
                VariableExpression oldVariable = new VariableExpression("old");
                ExpressionStatement oldVariabeStatement = new ExpressionStatement(
                new DeclarationExpression(oldVariable,
                        Token.newSymbol(Types.ASSIGN, -1, -1),
                        new MethodCallExpression(VariableExpression.THIS_EXPRESSION, VariableGenerationUtility.OLD_VARIABLES_METHOD, ArgumentListExpression.EMPTY_ARGUMENTS)));

                postconditionCheck.addStatement(new ExpressionStatement(methodCallToSuperPostcondition));

                methodBlock.getStatements().add(0, oldVariabeStatement);
                methodBlock.addStatements(postconditionCheck.getStatements());
            }
        }
    }
}
