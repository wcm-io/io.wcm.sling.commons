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

import java.util.stream.Stream;

import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.RankedServices;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.wcm.sling.commons.caservice.ContextAwareService;

class ContextAwareServiceTracker<T extends ContextAwareService> implements ServiceTrackerCustomizer<T, ServiceInfo<T>> {

  private final String serviceClassName;
  private final BundleContext bundleContext;
  private final ServiceTracker<T, ServiceInfo<T>> serviceTracker;
  private volatile RankedServices<ServiceInfo<T>> rankedServices;
  private volatile long lastServiceChange;

  private static final Logger log = LoggerFactory.getLogger(ContextAwareServiceTracker.class);

  ContextAwareServiceTracker(@NotNull String serviceClassName, @NotNull BundleContext bundleContext) {
    this.serviceClassName = serviceClassName;
    this.bundleContext = bundleContext;
    this.rankedServices = new RankedServices<>(Order.DESCENDING);
    this.serviceTracker = new ServiceTracker<>(bundleContext, serviceClassName, this);
    this.serviceTracker.open();
  }

  public void dispose() {
    serviceTracker.close();
    rankedServices = null;
  }

  @Override
  public ServiceInfo addingService(ServiceReference<T> reference) {
    ServiceInfo<T> serviceInfo = new ServiceInfo<>(reference, bundleContext);
    logServiceDebugMessage("Add service {}", serviceInfo);
    if (rankedServices != null) {
      rankedServices.bind(serviceInfo, serviceInfo.getServiceProperties());
    }
    lastServiceChange = System.currentTimeMillis();
    return serviceInfo;
  }

  @Override
  public void modifiedService(ServiceReference<T> reference, ServiceInfo<T> serviceInfo) {
    // nothing to do
  }

  @Override
  public void removedService(ServiceReference<T> reference, ServiceInfo<T> serviceInfo) {
    logServiceDebugMessage("Remove service {}", serviceInfo);
    if (rankedServices != null) {
      rankedServices.unbind(serviceInfo, serviceInfo.getServiceProperties());
    }
    lastServiceChange = System.currentTimeMillis();
    bundleContext.ungetService(reference);
  }

  public Stream<ServiceInfo<T>> resolve(@Nullable String resourcePath, @Nullable Filter filter) {
    if (rankedServices == null) {
      return Stream.empty();
    }
    return rankedServices.getList().stream()
        .filter(serviceInfo -> serviceInfo.matchesFilter(filter))
        .filter(serviceInfo -> serviceInfo.matchesPath(resourcePath));
  }

  public String getServiceClassName() {
    return this.serviceClassName;
  }

  public long getLastServiceChangeTimestamp() {
    return this.lastServiceChange;
  }

  public Iterable<ServiceInfo<T>> getServiceInfos() {
    return rankedServices;
  }

  private void logServiceDebugMessage(String message, ServiceInfo<T> serviceInfo) {
    if (!log.isDebugEnabled()) {
      return;
    }
    Object service = serviceInfo.getService();
    if (service != null) {
      log.debug(message, service.getClass().getName());
    }
  }

}
