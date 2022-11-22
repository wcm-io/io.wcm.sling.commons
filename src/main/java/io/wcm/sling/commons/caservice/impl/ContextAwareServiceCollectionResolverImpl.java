/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2022 wcm.io
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

import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.sling.api.adapter.Adaptable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.ServiceObjects;

import io.wcm.sling.commons.caservice.ContextAwareService;
import io.wcm.sling.commons.caservice.ContextAwareServiceCollectionResolver;
import io.wcm.sling.commons.caservice.ContextAwareServiceResolver.ResolveAllResult;

class ContextAwareServiceCollectionResolverImpl<T extends ContextAwareService>
    implements ContextAwareServiceCollectionResolver<T> {

  private final Collection<ServiceInfo<T>> services;
  private final ResolverHelper resolverHelper;
  private final long timestamp;

  @SuppressWarnings("null")
  ContextAwareServiceCollectionResolverImpl(@NotNull Collection<ServiceObjects<T>> serviceObjectsCollection,
      @NotNull ResolverHelper resolverHelper) {
    this.services = serviceObjectsCollection.stream()
        .map(serviceObjects -> new ServiceInfo<T>(serviceObjects.getServiceReference(), serviceObjects.getService()))
        .filter(ServiceInfo::isValid)
        .collect(Collectors.toList());
    this.resolverHelper = resolverHelper;
    this.timestamp = System.currentTimeMillis();
  }

  @Override
  @SuppressWarnings("null")
  public @Nullable T resolve(@Nullable Adaptable adaptable) {
    return resolverHelper.getValidServices(getMatchingServiceInfos(adaptable))
        .findFirst().orElse(null);
  }

  @Override
  public @NotNull ResolveAllResult<T> resolveAll(@Nullable Adaptable adaptable) {
    Stream<T> matchingServices = resolverHelper.getValidServices(getMatchingServiceInfos(adaptable));
    Supplier<String> combinedKey = resolverHelper.buildCombinedKey(timestamp, getMatchingServiceInfos(adaptable));
    return new ResolveAllResultImpl<>(matchingServices, combinedKey);
  }

  private Stream<ServiceInfo<T>> getMatchingServiceInfos(@Nullable Adaptable adaptable) {
    String resourcePath = resolverHelper.getResourcePath(adaptable);
    return services.stream()
        .filter(service -> service.matches(resourcePath));
  }

}
