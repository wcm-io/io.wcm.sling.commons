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
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.apache.sling.api.adapter.Adaptable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;

import io.wcm.sling.commons.caservice.ContextAwareService;
import io.wcm.sling.commons.caservice.ContextAwareServiceCollectionResolver;

class ContextAwareServiceCollectionResolverImpl<S extends ContextAwareService, D>
    implements ContextAwareServiceCollectionResolver<S, D> {

  private final Collection<ServiceReference<S>> serviceReferenceCollection;
  private final ResourcePathResolver resourcePathResolver;

  // cache of service trackers for each SPI interface
  private final LoadingCache<ServiceReference<S>, CollectionItemDecoration<S, D>> decorationCache;

  private static final Logger log = LoggerFactory.getLogger(ContextAwareServiceCollectionResolverImpl.class);

  ContextAwareServiceCollectionResolverImpl(@NotNull Collection<ServiceReference<S>> serviceReferenceCollection,
      @NotNull BiFunction<ServiceReference<S>, S, D> decorator, @NotNull ResourcePathResolver resourcePathResolver,
      @NotNull BundleContext bundleContext) {
    this.serviceReferenceCollection = serviceReferenceCollection;
    this.resourcePathResolver = resourcePathResolver;
    this.decorationCache = buildCache(decorator, bundleContext);
  }

  private static <S extends ContextAwareService, D> LoadingCache<ServiceReference<S>, CollectionItemDecoration<S, D>> buildCache(
      @NotNull BiFunction<ServiceReference<S>, S, D> decorator, @NotNull BundleContext bundleContext) {
    return CacheBuilder.newBuilder()
        // expire cached entry after 24h
        .expireAfterAccess(24, TimeUnit.HOURS)
        // unget service on removal
        .removalListener((RemovalNotification<ServiceReference<S>, CollectionItemDecoration<S, D>> notification) -> {
          log.debug("Remove service {}", notification.getValue());
          bundleContext.ungetService(notification.getKey());
        })
        // build cache lazily
        .build(new CacheLoader<ServiceReference<S>, CollectionItemDecoration<S, D>>() {
          @Override
          public CollectionItemDecoration<S, D> load(ServiceReference<S> serviceReference) {
            CollectionItemDecoration<S, D> item = new CollectionItemDecoration<>(serviceReference, decorator, bundleContext);
            log.debug("Add service {}", item);
            return item;
          }
        });
  }

  @Override
  @SuppressWarnings("null")
  public @Nullable S resolve(@Nullable Adaptable adaptable) {
    return getMatching(adaptable)
        .map(CollectionItemDecoration::getService)
        .findFirst().orElse(null);
  }

  @Override
  public @NotNull Stream<S> resolveAll(@Nullable Adaptable adaptable) {
    return getMatching(adaptable)
        .map(CollectionItemDecoration::getService);
  }

  @Override
  @SuppressWarnings("null")
  public @Nullable D resolveDecorated(@Nullable Adaptable adaptable) {
    return getMatching(adaptable)
        .map(CollectionItemDecoration::getDecoration)
        .findFirst().orElse(null);
  }

  @Override
  public @NotNull Stream<D> resolveAllDecorated(@Nullable Adaptable adaptable) {
    return getMatching(adaptable)
        .map(CollectionItemDecoration::getDecoration);
  }

  private Stream<CollectionItemDecoration<S, D>> getMatching(@Nullable Adaptable adaptable) {
    String resourcePath = resourcePathResolver.get(adaptable);
    return serviceReferenceCollection.stream()
        .map(decorationCache::getUnchecked)
        .filter(CollectionItemDecoration::isValid)
        .filter(item -> item.matches(resourcePath));
  }

}
