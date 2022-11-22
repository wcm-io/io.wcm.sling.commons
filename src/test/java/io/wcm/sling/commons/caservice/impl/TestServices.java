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

import static io.wcm.sling.commons.caservice.ContextAwareService.PROPERTY_ACCEPTS_CONTEXT_PATH_EMPTY;
import static io.wcm.sling.commons.caservice.ContextAwareService.PROPERTY_CONTEXT_PATH_BLACKLIST_PATTERN;
import static io.wcm.sling.commons.caservice.ContextAwareService.PROPERTY_CONTEXT_PATH_PATTERN;
import static org.osgi.framework.Constants.SERVICE_RANKING;

import java.util.Collection;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.TreeSet;

import org.apache.sling.testing.mock.osgi.MapUtil;
import org.apache.sling.testing.mock.osgi.MockBundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentServiceObjects;

import com.google.common.collect.ImmutableMap;

import io.wcm.testing.mock.aem.junit5.AemContext;

/**
 * Test services for testing context-aware services implementation.
 */
class TestServices {

  private final AemContext context;

  private final DummySpi contentService;
  private final DummySpi contentDamService;
  private final DummySpi contentSampleService;

  private final Collection<ComponentServiceObjects<DummySpi>> services = new TreeSet<>(
      new Comparator<ComponentServiceObjects<DummySpi>>() {
        @Override
        public int compare(ComponentServiceObjects<DummySpi> o1, ComponentServiceObjects<DummySpi> o2) {
          return o2.getServiceReference().compareTo(o1.getServiceReference());
        }
      });

  TestServices(AemContext context) {
    this.context = context;

    this.contentService = register(new DummySpiImpl("/content/*"),
        PROPERTY_CONTEXT_PATH_PATTERN, "^/content(/.*)?$",
        SERVICE_RANKING, 100);
    this.contentDamService = register(new DummySpiImpl("/content/dam/*"),
        PROPERTY_CONTEXT_PATH_PATTERN, "^/content/dam(/.*)?$",
        SERVICE_RANKING, 200);
    this.contentSampleService = register(new DummySpiImpl("/content/sample/*[!=exclude]"),
        PROPERTY_CONTEXT_PATH_PATTERN, "^/content/sample(/.*)?$",
        PROPERTY_CONTEXT_PATH_BLACKLIST_PATTERN, "^/content/sample/exclude(/.*)?$",
        SERVICE_RANKING, 300);

    // add some more services with high ranking but invalid properties - they should never be returned
    register(new DummySpiImpl("invalid1"),
        PROPERTY_CONTEXT_PATH_PATTERN, "(",
        SERVICE_RANKING, 10000);
    register(new DummySpiImpl("invalid2"),
        PROPERTY_CONTEXT_PATH_PATTERN, "^/content(/.*)?$",
        PROPERTY_CONTEXT_PATH_BLACKLIST_PATTERN, ")",
        SERVICE_RANKING, 20000);
  }

  Collection<ComponentServiceObjects<DummySpi>> getServices() {
    return this.services;
  }

  DummySpi getContentService() {
    return this.contentService;
  }

  DummySpi getContentDamService() {
    return this.contentDamService;
  }

  DummySpi getContentSampleService() {
    return this.contentSampleService;
  }

  DummySpi addDefaultService() {
    return register(new DummySpiImpl("default"),
        SERVICE_RANKING, Integer.MIN_VALUE,
        PROPERTY_ACCEPTS_CONTEXT_PATH_EMPTY, true);
  }

  @SuppressWarnings("null")
  DummySpi addContentDamImplWithBundleHeader() {
    // service gets path pattern from bundle header instead of service property
    ((MockBundle)context.bundleContext().getBundle()).setHeaders(ImmutableMap.of(
        PROPERTY_CONTEXT_PATH_PATTERN, "^/content/dam(/.*)?$"));
    return register(new DummySpiImpl("/content/dam (bundle header)"),
        SERVICE_RANKING, 1000);
  }

  @SuppressWarnings("null")
  private DummySpi register(DummySpi service, Object... properties) {
    Dictionary<String, Object> serviceProperties = MapUtil.toDictionary(properties);
    ServiceRegistration<DummySpi> serviceRegistration = context.bundleContext().registerService(DummySpi.class, service, serviceProperties);
    services.add(new MockComponentServiceObjects<DummySpi>(serviceRegistration.getReference(), service));
    return service;
  }

}
