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

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.sling.api.adapter.Adaptable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import io.wcm.sling.commons.caservice.ContextAwareService;
import io.wcm.sling.commons.caservice.ContextAwareServiceCollectionResolver;
import io.wcm.sling.commons.caservice.ContextAwareServiceResolver;
import io.wcm.sling.commons.caservice.PathPreprocessor;

/**
 * {@link ContextAwareServiceResolver} implementation.
 */
@Component(service = ContextAwareServiceResolver.class, immediate = true)
public class ContextAwareServiceResolverImpl implements ContextAwareServiceResolver {

  @Reference(policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
  private PathPreprocessor pathPreprocessor;

  private BundleContext bundleContext;
  private ResourcePathResolver resourcePathResolver;

  // cache of service trackers for each SPI interface
  private LoadingCache<String, ContextAwareServiceTracker<ContextAwareService>> serviceTrackerCache;

  private static final Logger log = LoggerFactory.getLogger(ContextAwareServiceResolverImpl.class);

  @Activate
  private void activate(BundleContext context) {
    this.bundleContext = context;
    this.resourcePathResolver = new ResourcePathResolver(pathPreprocessor);
    this.serviceTrackerCache = buildServiceTrackerCache(context);
  }

  @Deactivate
  private void deactivate() {
    this.serviceTrackerCache.invalidateAll();
  }

  private static <S extends ContextAwareService> LoadingCache<String, ContextAwareServiceTracker<S>> buildServiceTrackerCache(
      BundleContext bundleContext) {
    return CacheBuilder.newBuilder()
        .removalListener(new RemovalListener<String, ContextAwareServiceTracker<S>>() {
          @SuppressWarnings("null")
          @Override
          public void onRemoval(RemovalNotification<String, ContextAwareServiceTracker<S>> notification) {
            notification.getValue().dispose();
          }
        })
        .build(new CacheLoader<String, ContextAwareServiceTracker<S>>() {
          @Override
          public ContextAwareServiceTracker load(String className) {
            return new ContextAwareServiceTracker<>(className, bundleContext);
          }
        });
  }

  @Override
  @SuppressWarnings("null")
  public <S extends ContextAwareService> S resolve(@NotNull Class<S> serviceClass, @Nullable Adaptable adaptable) {
    ContextAwareServiceTracker<S> serviceTracker = getServiceTracker(serviceClass);
    return getValidServices(getMatchingServiceInfos(serviceTracker, adaptable))
        .findFirst().orElse(null);
  }

  @Override
  public <S extends ContextAwareService> @NotNull ResolveAllResult<S> resolveAll(@NotNull Class<S> serviceClass,
      @Nullable Adaptable adaptable) {
    ContextAwareServiceTracker<S> serviceTracker = getServiceTracker(serviceClass);
    Stream<S> services = getValidServices(getMatchingServiceInfos(serviceTracker, adaptable));
    Supplier<String> combinedKey = buildCombinedKey(serviceTracker.getLastServiceChangeTimestamp(),
        getMatchingServiceInfos(serviceTracker, adaptable));
    return new ResolveAllResultImpl<>(services, combinedKey);
  }

  @Override
  public <S extends ContextAwareService> @NotNull ContextAwareServiceCollectionResolver<S, Void> getCollectionResolver(
      @NotNull Collection<ServiceReference<S>> serviceReferenceCollection) {
    return getCollectionResolver(serviceReferenceCollection, (ref, service) -> null);
  }

  @Override
  public <S extends ContextAwareService, D> @NotNull ContextAwareServiceCollectionResolver<S, D> getCollectionResolver(
      @NotNull Collection<ServiceReference<S>> serviceReferenceCollection,
      @NotNull BiFunction<ServiceReference<S>, S, D> decorator) {
    return new ContextAwareServiceCollectionResolverImpl<>(serviceReferenceCollection, decorator,
        resourcePathResolver, bundleContext);
  }

  private <S extends ContextAwareService> Stream<ServiceInfo<S>> getMatchingServiceInfos(
      @NotNull ContextAwareServiceTracker<S> serviceTracker, @Nullable Adaptable adaptable) {
    String resourcePath = resourcePathResolver.get(adaptable);
    if (log.isTraceEnabled()) {
      log.trace("Resolve {} for resource {}", serviceTracker.getServiceClassName(), resourcePath);
    }
    return serviceTracker.resolve(resourcePath);
  }

  @SuppressWarnings({
      "java:S112", // allow generic exception
      "unchecked"
  })
  private <S extends ContextAwareService> ContextAwareServiceTracker<S> getServiceTracker(Class<S> serviceClass) {
    try {
      return (ContextAwareServiceTracker)serviceTrackerCache.get(serviceClass.getName());
    }
    catch (ExecutionException ex) {
      throw new RuntimeException("Error getting service tracker for " + serviceClass.getName() + " from cache.", ex);
    }
  }

  ConcurrentMap<String, ContextAwareServiceTracker<ContextAwareService>> getContextAwareServiceTrackerMap() {
    return serviceTrackerCache.asMap();
  }

  @SuppressWarnings("null")
  private static <S extends ContextAwareService> Stream<S> getValidServices(Stream<ServiceInfo<S>> serviceInfos) {
    return serviceInfos
        .filter(ServiceInfo::isValid)
        .map(ServiceInfo::getService);
  }

  private static <S extends ContextAwareService> @NotNull Supplier<String> buildCombinedKey(long timestamp,
      @NotNull Stream<ServiceInfo<S>> serviceInfos) {
    return () -> timestamp + "\n"
        + serviceInfos.map(ServiceInfo::getKey).collect(Collectors.joining("\n~\n"));
  }

}
