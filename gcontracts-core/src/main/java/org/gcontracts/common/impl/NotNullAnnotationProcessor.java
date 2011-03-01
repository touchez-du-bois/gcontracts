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
package org.gcontracts.common.impl;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.gcontracts.common.spi.AnnotationProcessor;
import org.gcontracts.common.spi.ProcessingContextInformation;
import org.gcontracts.domain.Contract;
import org.gcontracts.domain.Precondition;

/**
 * Implementation of {@link org.gcontracts.common.spi.AnnotationProcessor} which checks
 * {@link Parameter} instances for null values when {@link org.gcontracts.annotations.common.NotNull} is
 * specified on them.
 *
 * @author ast
 */
public class NotNullAnnotationProcessor extends AnnotationProcessor {

    @Override
    public void process(ProcessingContextInformation processingContextInformation, Contract contract, AnnotatedNode annotatedNode, MethodNode methodNode, BooleanExpression booleanExpression) {
        if (!processingContextInformation.isPreconditionsEnabled()) return;

        booleanExpression = new BooleanExpression(new BinaryExpression(
            new VariableExpression((Parameter) annotatedNode),
            Token.newSymbol(Types.COMPARE_NOT_EQUAL, -1, -1),
            ConstantExpression.NULL
        ));

        contract.addPrecondition(methodNode, new Precondition(booleanExpression));
    }
}
