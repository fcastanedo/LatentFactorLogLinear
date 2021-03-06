/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.ep;

/**
 * Payloads for evolutionary state must be copyable and updatable.  The copy should be
 * a deep copy unless some aspect of the state is sharable or immutable.
 *
 * During mutation, a copy is first made and then after the parameters in the State
 * structure are suitably modified, update is called with the scaled versions of the
 * parameters.
 *
 * @see State
 * @param <T>
 */
public interface Payload<T> {
  T copy();
  void update(double[] params);
}
