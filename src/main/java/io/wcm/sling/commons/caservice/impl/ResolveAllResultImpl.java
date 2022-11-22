/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2017 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.sling.commons.caservice.impl;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import io.wcm.sling.commons.caservice.ContextAwareService;
import io.wcm.sling.commons.caservice.ContextAwareServiceResolver.ResolveAllResult;

class ResolveAllResultImpl<T extends ContextAwareService> implements ResolveAllResult<T> {

  private final Stream<T> services;
  private final Supplier<String> combinedKeySupplier;
  private String combinedKey;

  ResolveAllResultImpl(Stream<T> services, Supplier<String> combinedKeySupplier) {
    this.services = services;
    this.combinedKeySupplier = combinedKeySupplier;
  }

  @Override
  public @NotNull Stream<T> getServices() {
    return services;
  }

  @Override
  public @NotNull String getCombinedKey() {
    if (combinedKey == null) {
      combinedKey = combinedKeySupplier.get();
    }
    return combinedKey;
  }

}
