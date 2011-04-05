// GenericsNote: Converted.
/*
 *  Copyright 2001-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.collections15;

/**
 * Defines a functor interface implemented by classes that do something.
 * <p/>
 * A <code>Closure</code> represents a block of code which is executed from
 * inside some block, function or iteration. It operates an input object.
 * <p/>
 * Standard implementations of common closures are provided by
 * {@link ClosureUtils}. These include method invokation and for/while loops.
 *
 * @author James Strachan
 * @author Nicola Ken Barozzi
 * @author Stephen Colebourne
 * @version $Revision: 1.1 $ $Date: 2005/10/11 17:05:19 $
 * @since Commons Collections 1.0
 */
public interface Closure <T> {

    /**
     * Performs an action on the specified input object.
     *
     * @param input the input to execute on
     * @throws ClassCastException       (runtime) if the input is the wrong class
     * @throws IllegalArgumentException (runtime) if the input is invalid
     * @throws FunctorException         (runtime) if any other error occurs
     */
    public void execute(T input);

}