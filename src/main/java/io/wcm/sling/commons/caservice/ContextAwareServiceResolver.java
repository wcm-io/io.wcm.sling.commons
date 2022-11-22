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
package io.wcm.sling.commons.caservice;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.ServiceObjects;

/**
 * Resolves the best-matching context-aware service implementation.
 */
@ProviderType
public interface ContextAwareServiceResolver {

  /**
   * Resolves the best-matching service implementation for the given resource context.
   * Only implementations which accept the given context resource path (via the service properties defined in
   * {@link ContextAwareService}) are considered as candidates.
   * If multiple candidates exist the implementation with the highest service ranking is returned.
   * @param serviceClass Service interface or class
   * @param adaptable Adaptable which is either a {@link Resource} or {@link SlingHttpServletRequest}.
   *          A resource instance is used directly for matching, in case of request the associated resource is used.
   *          May be null if no context is available.
   * @param <S> Service interface or class
   * @return Service implementation or null if no match found.
   */
  <S extends ContextAwareService> @Nullable S resolve(@NotNull Class<S> serviceClass, @Nullable Adaptable adaptable);

  /**
   * Resolves all matching service implementations for the given resource context.
   * Only implementations which accept the given context resource path (via the service properties defined in
   * {@link ContextAwareService}) are considered as candidates.
   * The candidates are returned ordered descending by their service ranking.
   * @param serviceClass Service interface or class
   * @param adaptable Adaptable which is either a {@link Resource} or {@link SlingHttpServletRequest}.
   *          A resource instance is used directly for matching, in case of request the associated resource is used.
   *          May be null if no context is available.
   * @param <S> Service interface or class
   * @return Collection of all matching services
   */
  <S extends ContextAwareService> @NotNull ResolveAllResult<S> resolveAll(@NotNull Class<S> serviceClass, @Nullable Adaptable adaptable);

  /**
   * Gets a {@link ContextAwareServiceCollectionResolver} which operates on a given collection of service objects
   * of the required service. This collection is usually managed by OSGi Declarative Services and expected
   * to contain all services with the service interface order by service ranking (high to low).
   * The collection resolver helps to get service(s) matching the resource context out of this list.
   * @param <S> Service interface or class
   * @param serviceObjectsCollection Collection of service objects
   * @return Collection resolver
   */
  <S extends ContextAwareService> @NotNull ContextAwareServiceCollectionResolver<S, Void> getCollectionResolver(
      @NotNull Collection<ServiceObjects<S>> serviceObjectsCollection);

  /**
   * Gets a {@link ContextAwareServiceCollectionResolver} which operates on a given collection of service objects
   * of the required service. This collection is usually managed by OSGi Declarative Services and expected
   * to contain all services with the service interface order by service ranking (high to low).
   * The collection resolver helps to get service(s) matching the resource context out of this list.
   * @param <S> Service interface or class
   * @param <D> Decorator class that is calculated once for each item of the service objects collection.
   * @param serviceObjectsCollection Collection of service objects
   * @param decorator Creates decoration for each collection item once.
   * @return Collection resolver
   */
  <S extends ContextAwareService, D> @NotNull ContextAwareServiceCollectionResolver<S, D> getCollectionResolver(
      @NotNull Collection<ServiceObjects<S>> serviceObjectsCollection,
      Function<ServiceObjects<S>, D> decorator);

  /**
   * Result of the {@link ContextAwareServiceResolver#resolveAll(Class, Adaptable)} method.
   * All methods are implemented in a lazy fashion.
   * @param <S> Service interface or class
   */
  interface ResolveAllResult<S extends ContextAwareService> {

    /**
     * Gets all matching services
     * @return Context-Aware services
     */
    @NotNull
    Stream<S> getServices();

    /**
     * Gets a combined key that represents the path filter sets of all affected context-aware services
     * that matched for the given resource and a timestamp of the service registration for this service interface
     * changed last.
     * That means for two different resources that get the same combined key the same list of services is returned, and
     * the result that was calculated from them can be cached with this key. Of course this makes only sense when the
     * services always the same result as long as they are registered.
     * @return Key string
     */
    @NotNull
    String getCombinedKey();

  }

}
