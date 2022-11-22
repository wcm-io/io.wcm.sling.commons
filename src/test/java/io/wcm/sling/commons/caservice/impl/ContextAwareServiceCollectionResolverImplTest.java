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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.osgi.framework.Constants.SERVICE_RANKING;

import java.util.Collection;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.testing.mock.osgi.MapUtil;
import org.apache.sling.testing.mock.osgi.MockBundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceRegistration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.wcm.sling.commons.caservice.ContextAwareServiceCollectionResolver;
import io.wcm.sling.commons.caservice.ContextAwareServiceResolver;
import io.wcm.sling.commons.caservice.ContextAwareServiceResolver.ResolveAllResult;
import io.wcm.sling.commons.caservice.PathPreprocessor;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class ContextAwareServiceCollectionResolverImplTest {

  private final AemContext context = new AemContext();

  private DummySpi contentImpl;
  private DummySpi contentDamImpl;
  private DummySpi contentSampleImpl;

  private ContextAwareServiceResolver contextAwareServiceResolver;
  private Collection<ServiceObjects<DummySpi>> services = new TreeSet<>(new Comparator<ServiceObjects<DummySpi>>() {
    @Override
    public int compare(ServiceObjects<DummySpi> o1, ServiceObjects<DummySpi> o2) {
      return o2.getServiceReference().compareTo(o1.getServiceReference());
    }
  });

  @BeforeEach
  void setUp() {
    contentImpl = registerDummySpi(new DummySpiImpl("/content/*"),
        PROPERTY_CONTEXT_PATH_PATTERN, "^/content(/.*)?$",
        SERVICE_RANKING, 100);
    contentDamImpl = registerDummySpi(new DummySpiImpl("/content/dam/*"),
        PROPERTY_CONTEXT_PATH_PATTERN, "^/content/dam(/.*)?$",
        SERVICE_RANKING, 200);
    contentSampleImpl = registerDummySpi(new DummySpiImpl("/content/sample/*[!=exclude]"),
        PROPERTY_CONTEXT_PATH_PATTERN, "^/content/sample(/.*)?$",
        PROPERTY_CONTEXT_PATH_BLACKLIST_PATTERN, "^/content/sample/exclude(/.*)?$",
        SERVICE_RANKING, 300);

    // add some more services with high ranking but invalid properties - they should never be returned
    registerDummySpi(new DummySpiImpl("invalid1"),
        PROPERTY_CONTEXT_PATH_PATTERN, "(",
        SERVICE_RANKING, 10000);
    registerDummySpi(new DummySpiImpl("invalid2"),
        PROPERTY_CONTEXT_PATH_PATTERN, "^/content(/.*)?$",
        PROPERTY_CONTEXT_PATH_BLACKLIST_PATTERN, ")",
        SERVICE_RANKING, 20000);

    contextAwareServiceResolver = context.registerInjectActivateService(new ContextAwareServiceResolverImpl());
  }

  @Test
  void testWithDefaultImpl() {
    DummySpi defaultImpl = registerDummySpi(new DummySpiImpl("default"),
        SERVICE_RANKING, Integer.MIN_VALUE,
        PROPERTY_ACCEPTS_CONTEXT_PATH_EMPTY, true);
    ContextAwareServiceCollectionResolver<DummySpi> underTest = contextAwareServiceResolver.getCollectionResolver(services);

    assertSame(contentImpl, underTest.resolve(context.create().resource("/content/test1")));
    assertSame(contentSampleImpl, underTest.resolve(context.create().resource("/content/sample/test1")));
    assertSame(contentImpl, underTest.resolve(context.create().resource("/content/sample/exclude/test1")));
    assertSame(contentDamImpl, underTest.resolve(context.create().resource("/content/dam/test1")));
    assertSame(defaultImpl, underTest.resolve(context.create().resource("/etc/test1")));

    assertEquals(ImmutableList.of(contentDamImpl, contentImpl, defaultImpl),
        underTest.resolveAll(context.create().resource("/content/dam/test2")).getServices().collect(Collectors.toList()));
  }

  @Test
  void testWithoutDefaultImpl() {
    ContextAwareServiceCollectionResolver<DummySpi> underTest = contextAwareServiceResolver.getCollectionResolver(services);

    assertSame(contentImpl, underTest.resolve(context.create().resource("/content/test1")));
    assertSame(contentSampleImpl, underTest.resolve(context.create().resource("/content/sample/test1")));
    assertSame(contentImpl, underTest.resolve(context.create().resource("/content/sample/exclude/test1")));
    assertSame(contentDamImpl, underTest.resolve(context.create().resource("/content/dam/test1")));
    assertNull(underTest.resolve(context.create().resource("/etc/test1")));

    assertEquals(ImmutableList.of(contentDamImpl, contentImpl),
        underTest.resolveAll(context.create().resource("/content/dam/test2")).getServices().collect(Collectors.toList()));
    assertEquals(ImmutableList.of(contentSampleImpl, contentImpl),
        underTest.resolveAll(context.create().resource("/content/sample/test2")).getServices().collect(Collectors.toList()));
  }

  @Test
  void testWithSlingHttpServletRequest() {
    DummySpi defaultImpl = registerDummySpi(new DummySpiImpl("default"),
        SERVICE_RANKING, Integer.MIN_VALUE,
        PROPERTY_ACCEPTS_CONTEXT_PATH_EMPTY, true);
    ContextAwareServiceCollectionResolver<DummySpi> underTest = contextAwareServiceResolver.getCollectionResolver(services);

    context.currentResource(context.create().resource("/content/sample/test1"));
    assertSame(contentSampleImpl, underTest.resolve(context.request()));

    assertEquals(ImmutableList.of(contentSampleImpl, contentImpl, defaultImpl),
        underTest.resolveAll(context.request()).getServices().collect(Collectors.toList()));
  }

  /**
   * Simulate an experience fragment resource included in a page.
   * Context-aware service resolver should take the current page as context to resolve, not the current resource.
   */
  @Test
  void testWithSlingHttpServletRequest_ResourceOtherContext() {
    DummySpi defaultImpl = registerDummySpi(new DummySpiImpl("default"),
        SERVICE_RANKING, Integer.MIN_VALUE,
        PROPERTY_ACCEPTS_CONTEXT_PATH_EMPTY, true);
    ContextAwareServiceCollectionResolver<DummySpi> underTest = contextAwareServiceResolver.getCollectionResolver(services);

    context.currentPage(context.create().page("/content/sample/test1"));
    context.currentResource(context.create().resource("/content/experience-fragments/test1"));
    assertSame(contentSampleImpl, underTest.resolve(context.request()));

    assertEquals(ImmutableList.of(contentSampleImpl, contentImpl, defaultImpl),
        underTest.resolveAll(context.request()).getServices().collect(Collectors.toList()));
  }

  @Test
  void testWithNull() {
    DummySpi defaultImpl = registerDummySpi(new DummySpiImpl("default"),
        SERVICE_RANKING, Integer.MIN_VALUE,
        PROPERTY_ACCEPTS_CONTEXT_PATH_EMPTY, true);
    ContextAwareServiceCollectionResolver<DummySpi> underTest = contextAwareServiceResolver.getCollectionResolver(services);

    assertSame(defaultImpl, underTest.resolve(null));

    assertEquals(ImmutableList.of(defaultImpl), underTest.resolveAll(null).getServices().collect(Collectors.toList()));
  }

  @Test
  void testResolveAllCombindedKey() {
    ContextAwareServiceCollectionResolver<DummySpi> underTest = contextAwareServiceResolver.getCollectionResolver(services);

    ResolveAllResult result1 = underTest.resolveAll(context.create().resource("/content/dam/test1"));
    ResolveAllResult result2 = underTest.resolveAll(context.create().resource("/content/dam/test2"));
    ResolveAllResult result3 = underTest.resolveAll(context.create().resource("/content/sample/test3"));

    assertEquals(result1.getCombinedKey(), result2.getCombinedKey());
    assertNotEquals(result1.getCombinedKey(), result3.getCombinedKey());
  }

  @Test
  @SuppressWarnings("null")
  void testWithBundleHeader() {
    // service gets path pattern from bundle header instead of service property
    ((MockBundle)context.bundleContext().getBundle()).setHeaders(ImmutableMap.of(
        PROPERTY_CONTEXT_PATH_PATTERN, "^/content/dam(/.*)?$"));
    DummySpi contentDamImplWithBundleHeader = registerDummySpi(new DummySpiImpl("/content/dam (bundle header)"),
        SERVICE_RANKING, 1000);
    ContextAwareServiceCollectionResolver<DummySpi> underTest = contextAwareServiceResolver.getCollectionResolver(services);

    assertSame(contentImpl, underTest.resolve(context.create().resource("/content/test1")));
    assertSame(contentSampleImpl, underTest.resolve(context.create().resource("/content/sample/test1")));
    assertSame(contentImpl, underTest.resolve(context.create().resource("/content/sample/exclude/test1")));
    assertSame(contentDamImplWithBundleHeader, underTest.resolve(context.create().resource("/content/dam/test1")));
    assertNull(underTest.resolve(context.create().resource("/etc/test1")));

    assertEquals(ImmutableList.of(contentDamImplWithBundleHeader, contentDamImpl, contentImpl),
        underTest.resolveAll(context.create().resource("/content/dam/test2")).getServices().collect(Collectors.toList()));
  }

  @Test
  void testWithPathPreProcessor() {
    context.registerService(PathPreprocessor.class, (path, resourceResolver) -> StringUtils.removeStart(path, "/pathprefix"));
    contextAwareServiceResolver = context.registerInjectActivateService(new ContextAwareServiceResolverImpl());
    ContextAwareServiceCollectionResolver<DummySpi> underTest = contextAwareServiceResolver.getCollectionResolver(services);

    assertSame(contentImpl, underTest.resolve(context.create().resource("/pathprefix/content/test1")));
    assertSame(contentSampleImpl, underTest.resolve(context.create().resource("/pathprefix/content/sample/test1")));
    assertSame(contentImpl, underTest.resolve(context.create().resource("/pathprefix/content/sample/exclude/test1")));
    assertSame(contentDamImpl, underTest.resolve(context.create().resource("/pathprefix/content/dam/test1")));
    assertNull(underTest.resolve(context.create().resource("/pathprefix/etc/test1")));

    assertEquals(ImmutableList.of(contentDamImpl, contentImpl),
        underTest.resolveAll(context.create().resource("/pathprefix/content/dam/test2")).getServices().collect(Collectors.toList()));
  }

  @SuppressWarnings("null")
  private DummySpi registerDummySpi(DummySpi service, Object... properties) {
    Dictionary<String, Object> serviceProperties = MapUtil.toDictionary(properties);
    ServiceRegistration<DummySpi> serviceRegistration = context.bundleContext().registerService(DummySpi.class, service, serviceProperties);
    services.add(new MockServiceObjects<DummySpi>(serviceRegistration.getReference(), service));
    return service;
  }

}
