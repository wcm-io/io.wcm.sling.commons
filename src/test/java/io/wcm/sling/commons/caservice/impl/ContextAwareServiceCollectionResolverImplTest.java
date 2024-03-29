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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.sling.commons.caservice.ContextAwareServiceCollectionResolver;
import io.wcm.sling.commons.caservice.ContextAwareServiceResolver;
import io.wcm.sling.commons.caservice.PathPreprocessor;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

/**
 * Test resolving context-aware services using service collection against the {@link TestServices}.
 */
@ExtendWith(AemContextExtension.class)
class ContextAwareServiceCollectionResolverImplTest {

  private final AemContext context = new AemContext();

  private TestServices testServices;
  private DummySpi contentImpl;
  private DummySpi contentDamImpl;
  private DummySpi contentSampleImpl;

  private ContextAwareServiceResolver contextAwareServiceResolver;

  @BeforeEach
  void setUp() {
    testServices = new TestServices(context);
    contentImpl = testServices.getContentService();
    contentDamImpl = testServices.getContentDamService();
    contentSampleImpl = testServices.getContentSampleService();

    contextAwareServiceResolver = context.registerInjectActivateService(new ContextAwareServiceResolverImpl());
  }

  @Test
  void testWithDefaultImpl() {
    DummySpi defaultImpl = testServices.addDefaultService();
    try (ContextAwareServiceCollectionResolver<DummySpi, Void> underTest = contextAwareServiceResolver
        .getCollectionResolver(testServices.getServices())) {

      assertSame(contentImpl, underTest.resolve(context.create().resource("/content/test1")));
      assertSame(contentSampleImpl, underTest.resolve(context.create().resource("/content/sample/test1")));
      assertSame(contentImpl, underTest.resolve(context.create().resource("/content/sample/exclude/test1")));
      assertSame(contentDamImpl, underTest.resolve(context.create().resource("/content/dam/test1")));
      assertSame(defaultImpl, underTest.resolve(context.create().resource("/etc/test1")));

      assertEquals(List.of(contentDamImpl, contentImpl, defaultImpl),
          underTest.resolveAll(context.create().resource("/content/dam/test2")).collect(Collectors.toList()));
    }
  }

  @Test
  @SuppressWarnings("null")
  void testWithDefaultImpl_Decorated() {
    DummySpi defaultImpl = testServices.addDefaultService();
    try (ContextAwareServiceCollectionResolver<DummySpi, DummySpiDecorator> underTest = contextAwareServiceResolver
        .getCollectionResolver(testServices.getServices(), (ref, service) -> new DummySpiDecorator(service))) {

      assertSame(contentImpl, underTest.resolveDecorated(context.create().resource("/content/test1")).getService());
      assertSame(contentSampleImpl, underTest.resolveDecorated(context.create().resource("/content/sample/test1")).getService());
      assertSame(contentImpl, underTest.resolveDecorated(context.create().resource("/content/sample/exclude/test1")).getService());
      assertSame(contentDamImpl, underTest.resolveDecorated(context.create().resource("/content/dam/test1")).getService());
      assertSame(defaultImpl, underTest.resolveDecorated(context.create().resource("/etc/test1")).getService());

      assertEquals(List.of(contentDamImpl, contentImpl, defaultImpl),
          underTest.resolveAllDecorated(context.create().resource("/content/dam/test2"))
              .map(DummySpiDecorator::getService).collect(Collectors.toList()));
    }
  }

  @Test
  void testWithDefaultImpl_DynamicListChange() {
    try (ContextAwareServiceCollectionResolver<DummySpi, Void> underTest = contextAwareServiceResolver
        .getCollectionResolver(testServices.getServices())) {

      assertSame(contentImpl, underTest.resolve(context.create().resource("/content/test1")));
      assertSame(contentSampleImpl, underTest.resolve(context.create().resource("/content/sample/test1")));
      assertSame(contentImpl, underTest.resolve(context.create().resource("/content/sample/exclude/test1")));
      assertSame(contentDamImpl, underTest.resolve(context.create().resource("/content/dam/test1")));
      assertNull(underTest.resolve(context.create().resource("/etc/test1")));
      assertEquals(List.of(contentDamImpl, contentImpl),
          underTest.resolveAll(context.create().resource("/content/dam/test2")).collect(Collectors.toList()));

      DummySpi defaultImpl = testServices.addDefaultService();
      assertSame(defaultImpl, underTest.resolve(context.create().resource("/etc/test2")));

      assertEquals(List.of(contentDamImpl, contentImpl, defaultImpl),
          underTest.resolveAll(context.create().resource("/content/dam/test3")).collect(Collectors.toList()));
    }
  }

  @Test
  void testWithoutDefaultImpl() {
    try (ContextAwareServiceCollectionResolver<DummySpi, Void> underTest = contextAwareServiceResolver
        .getCollectionResolver(testServices.getServices())) {

      assertSame(contentImpl, underTest.resolve(context.create().resource("/content/test1")));
      assertSame(contentSampleImpl, underTest.resolve(context.create().resource("/content/sample/test1")));
      assertSame(contentImpl, underTest.resolve(context.create().resource("/content/sample/exclude/test1")));
      assertSame(contentDamImpl, underTest.resolve(context.create().resource("/content/dam/test1")));
      assertNull(underTest.resolve(context.create().resource("/etc/test1")));

      assertEquals(List.of(contentDamImpl, contentImpl),
          underTest.resolveAll(context.create().resource("/content/dam/test2")).collect(Collectors.toList()));
      assertEquals(List.of(contentSampleImpl, contentImpl),
          underTest.resolveAll(context.create().resource("/content/sample/test2")).collect(Collectors.toList()));
    }
  }

  @Test
  void testWithSlingHttpServletRequest() {
    DummySpi defaultImpl = testServices.addDefaultService();
    try (ContextAwareServiceCollectionResolver<DummySpi, Void> underTest = contextAwareServiceResolver
        .getCollectionResolver(testServices.getServices())) {

      context.currentResource(context.create().resource("/content/sample/test1"));
      assertSame(contentSampleImpl, underTest.resolve(context.request()));

      assertEquals(List.of(contentSampleImpl, contentImpl, defaultImpl),
          underTest.resolveAll(context.request()).collect(Collectors.toList()));
    }
  }

  /**
   * Simulate an experience fragment resource included in a page.
   * Context-aware service resolver should take the current page as context to resolve, not the current resource.
   */
  @Test
  void testWithSlingHttpServletRequest_ResourceOtherContext() {
    DummySpi defaultImpl = testServices.addDefaultService();
    try (ContextAwareServiceCollectionResolver<DummySpi, Void> underTest = contextAwareServiceResolver
        .getCollectionResolver(testServices.getServices())) {

      context.currentPage(context.create().page("/content/sample/test1"));
      context.currentResource(context.create().resource("/content/experience-fragments/test1"));
      assertSame(contentSampleImpl, underTest.resolve(context.request()));

      assertEquals(List.of(contentSampleImpl, contentImpl, defaultImpl),
          underTest.resolveAll(context.request()).collect(Collectors.toList()));
    }
  }

  @Test
  void testWithNull() {
    DummySpi defaultImpl = testServices.addDefaultService();

    try (ContextAwareServiceCollectionResolver<DummySpi, Void> underTest = contextAwareServiceResolver
        .getCollectionResolver(testServices.getServices())) {

      assertSame(defaultImpl, underTest.resolve(null));

      assertEquals(List.of(defaultImpl), underTest.resolveAll(null).collect(Collectors.toList()));
    }
  }

  @Test
  void testWithBundleHeader() {
    DummySpi contentDamImplWithBundleHeader = testServices.addContentDamImplWithBundleHeader();
    try (ContextAwareServiceCollectionResolver<DummySpi, Void> underTest = contextAwareServiceResolver
        .getCollectionResolver(testServices.getServices())) {

      assertSame(contentImpl, underTest.resolve(context.create().resource("/content/test1")));
      assertSame(contentSampleImpl, underTest.resolve(context.create().resource("/content/sample/test1")));
      assertSame(contentImpl, underTest.resolve(context.create().resource("/content/sample/exclude/test1")));
      assertSame(contentDamImplWithBundleHeader, underTest.resolve(context.create().resource("/content/dam/test1")));
      assertNull(underTest.resolve(context.create().resource("/etc/test1")));

      assertEquals(List.of(contentDamImplWithBundleHeader, contentDamImpl, contentImpl),
          underTest.resolveAll(context.create().resource("/content/dam/test2")).collect(Collectors.toList()));
    }
  }

  @Test
  void testWithPathPreProcessor() {
    context.registerService(PathPreprocessor.class, (path, resourceResolver) -> StringUtils.removeStart(path, "/pathprefix"));
    contextAwareServiceResolver = context.registerInjectActivateService(new ContextAwareServiceResolverImpl());
    try (ContextAwareServiceCollectionResolver<DummySpi, Void> underTest = contextAwareServiceResolver
        .getCollectionResolver(testServices.getServices())) {

      assertSame(contentImpl, underTest.resolve(context.create().resource("/pathprefix/content/test1")));
      assertSame(contentSampleImpl, underTest.resolve(context.create().resource("/pathprefix/content/sample/test1")));
      assertSame(contentImpl, underTest.resolve(context.create().resource("/pathprefix/content/sample/exclude/test1")));
      assertSame(contentDamImpl, underTest.resolve(context.create().resource("/pathprefix/content/dam/test1")));
      assertNull(underTest.resolve(context.create().resource("/pathprefix/etc/test1")));

      assertEquals(List.of(contentDamImpl, contentImpl),
          underTest.resolveAll(context.create().resource("/pathprefix/content/dam/test2")).collect(Collectors.toList()));
    }
  }

}
