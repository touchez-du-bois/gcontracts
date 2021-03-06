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
package org.gcontracts.annotations;

import org.gcontracts.annotations.meta.AnnotationProcessorImplementation;
import org.gcontracts.annotations.meta.ClassInvariant;
import org.gcontracts.common.impl.ClassInvariantAnnotationProcessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Represents a <b>class-invariant</b>.
 * </p>
 *
 * <p>
 * The class-invariant defines assertions holding during the entire objects life-time.
 * </p>
 * <p>
 * Class-invariants are verified at runtime at the following pointcuts:
 * <ul>
 *  <li>after a constructor call</li>
 *  <li>before a method call</li>
 *  <li>after a method call</li>
 * </ul>
 * </p>
 * <p>
 * Whenever a class has a parent which itself specifies a class-invariant, that class-invariant expression is combined
 * with the actual class's invariant (by using a logical AND).
 * </p>
 *
 * @author ast
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)

@ClassInvariant
@AnnotationProcessorImplementation(ClassInvariantAnnotationProcessor.class)
public @interface Invariant {
    Class value();
}