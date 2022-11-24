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

import java.util.function.BiFunction;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import io.wcm.sling.commons.caservice.ContextAwareService;

/**
 * Manages two decorations for each service reference that is managed in the collection by
 * {@link ContextAwareServiceCollectionResolverImpl}: The {@link ServiceInfo} decoration which helps
 * matching the context-aware resources, and and optional custom decoration provided by project code.
 * @param <S> Service interface or class
 * @param <D> Custom decoration
 */
class CollectionItemDecoration<S extends ContextAwareService, D> {

  private final S service;
  private final D decoration;
  private final ServiceInfo<S> serviceInfo;

  CollectionItemDecoration(ServiceReference<S> serviceReference,
      BiFunction<ServiceReference<S>, S, D> decorator, BundleContext bundleContext) {
    this.service = bundleContext.getService(serviceReference);
    this.decoration = decorator.apply(serviceReference, this.service);
    this.serviceInfo = new ServiceInfo<>(serviceReference, this.service);
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

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("service", service)
        .append("serviceInfo", serviceInfo)
        .append("decoration", decoration)
        .toString();
  }

}
