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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.sling.api.adapter.Adaptable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.ServiceObjects;

import io.wcm.sling.commons.caservice.ContextAwareService;
import io.wcm.sling.commons.caservice.ContextAwareServiceCollectionResolver;

class ContextAwareServiceCollectionResolverImpl<S extends ContextAwareService, D>
    implements ContextAwareServiceCollectionResolver<S, D> {

  private final Collection<ServiceWrapper<S, D>> services;
  private final ResourcePathResolver resourcePathResolver;

  ContextAwareServiceCollectionResolverImpl(@NotNull Collection<ServiceObjects<S>> serviceObjectsCollection,
      Function<ServiceObjects<S>, D> decorator, @NotNull ResourcePathResolver resourcePathResolver) {
    this.services = serviceObjectsCollection.stream()
        .map(serviceObjects -> new ServiceWrapper<>(serviceObjects, decorator))
        .filter(ServiceWrapper::isValid)
        .collect(Collectors.toList());
    this.resourcePathResolver = resourcePathResolver;
  }

  @Override
  @SuppressWarnings("null")
  public @Nullable S resolve(@Nullable Adaptable adaptable) {
    return getMatching(adaptable)
        .map(ServiceWrapper::getService)
        .findFirst().orElse(null);
  }

  @Override
  @SuppressWarnings("null")
  public @NotNull Collection<S> resolveAll(@Nullable Adaptable adaptable) {
    return getMatching(adaptable)
        .map(ServiceWrapper::getService)
        .collect(Collectors.toList());
  }

  @Override
  @SuppressWarnings("null")
  public @Nullable D resolveDecorated(@Nullable Adaptable adaptable) {
    return getMatching(adaptable)
        .map(ServiceWrapper::getDecoration)
        .findFirst().orElse(null);
  }

  @Override
  @SuppressWarnings("null")
  public @NotNull Collection<D> resolveAllDecorated(@Nullable Adaptable adaptable) {
    return getMatching(adaptable)
        .map(ServiceWrapper::getDecoration)
        .collect(Collectors.toList());
  }

  private Stream<ServiceWrapper<S, D>> getMatching(@Nullable Adaptable adaptable) {
    String resourcePath = resourcePathResolver.get(adaptable);
    return services.stream()
        .filter(wrapper -> wrapper.matches(resourcePath));
  }

  private static class ServiceWrapper<S extends ContextAwareService, D> {

    private final S service;
    private final D decoration;
    private final ServiceInfo<S> serviceInfo;

    ServiceWrapper(ServiceObjects<S> serviceObjects, Function<ServiceObjects<S>, D> decorator) {
      this.service = serviceObjects.getService();
      this.decoration = decorator.apply(serviceObjects);
      this.serviceInfo = new ServiceInfo<>(serviceObjects.getServiceReference(), this.service);
    }

    boolean isValid() {
      return this.serviceInfo.isValid();
    }

    boolean matches(@Nullable String resourcePath) {
      return this.serviceInfo.matches(resourcePath);
    }

    S getService() {
      return this.service;
    }

    D getDecoration() {
      return this.decoration;
    }

  }

}
