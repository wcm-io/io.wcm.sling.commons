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
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.sling.api.adapter.Adaptable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
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

  private ResolverHelper resolverHelper;

  private static final Logger log = LoggerFactory.getLogger(ContextAwareServiceResolverImpl.class);

  // cache of service trackers for each SPI interface
  private LoadingCache<String, ContextAwareServiceTracker<ContextAwareService>> serviceTrackerCache;

  @Activate
  private void activate(BundleContext bundleContext) {
    this.resolverHelper = new ResolverHelper(pathPreprocessor);
    this.serviceTrackerCache = buildServiceTrackerCache(bundleContext);
  }

  private static <T extends ContextAwareService> LoadingCache<String, ContextAwareServiceTracker<T>> buildServiceTrackerCache(
      BundleContext bundleContext) {
    return CacheBuilder.newBuilder()
        .removalListener(new RemovalListener<String, ContextAwareServiceTracker<T>>() {
          @SuppressWarnings("null")
          @Override
          public void onRemoval(RemovalNotification<String, ContextAwareServiceTracker<T>> notification) {
            notification.getValue().dispose();
          }
        })
        .build(new CacheLoader<String, ContextAwareServiceTracker<T>>() {
          @Override
          public ContextAwareServiceTracker load(String className) {
            return new ContextAwareServiceTracker<>(className, bundleContext);
          }
        });
  }

  @Deactivate
  private void deactivate() {
    this.serviceTrackerCache.invalidateAll();
  }

  @Override
  @SuppressWarnings("null")
  public <T extends ContextAwareService> T resolve(@NotNull Class<T> serviceClass, @Nullable Adaptable adaptable) {
    ContextAwareServiceTracker<T> serviceTracker = getServiceTracker(serviceClass);
    return resolverHelper.getValidServices(getMatchingServiceInfos(serviceTracker, adaptable))
        .findFirst().orElse(null);
  }

  @Override
  public <T extends ContextAwareService> @NotNull ResolveAllResult<T> resolveAll(@NotNull Class<T> serviceClass,
      @Nullable Adaptable adaptable) {
    ContextAwareServiceTracker<T> serviceTracker = getServiceTracker(serviceClass);
    Stream<T> services = resolverHelper.getValidServices(getMatchingServiceInfos(serviceTracker, adaptable));
    Supplier<String> combinedKey = resolverHelper.buildCombinedKey(serviceTracker.getLastServiceChangeTimestamp(),
        getMatchingServiceInfos(serviceTracker, adaptable));
    return new ResolveAllResultImpl<>(services, combinedKey);
  }

  @Override
  public <T extends ContextAwareService> @NotNull ContextAwareServiceCollectionResolver<T> getCollectionResolver(
      @NotNull Collection<ServiceObjects<T>> serviceObjectsCollection) {
    return new ContextAwareServiceCollectionResolverImpl<>(serviceObjectsCollection, resolverHelper);
  }

  private <T extends ContextAwareService> Stream<ServiceInfo<T>> getMatchingServiceInfos(
      @NotNull ContextAwareServiceTracker<T> serviceTracker, @Nullable Adaptable adaptable) {
    String resourcePath = resolverHelper.getResourcePath(adaptable);
    if (log.isTraceEnabled()) {
      log.trace("Resolve {} for resource {}", serviceTracker.getServiceClassName(), resourcePath);
    }
    return serviceTracker.resolve(resourcePath);
  }

  @SuppressWarnings({
      "java:S112", // allow generic exception
      "unchecked"
  })
  private <T extends ContextAwareService> ContextAwareServiceTracker<T> getServiceTracker(Class<T> serviceClass) {
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

}
