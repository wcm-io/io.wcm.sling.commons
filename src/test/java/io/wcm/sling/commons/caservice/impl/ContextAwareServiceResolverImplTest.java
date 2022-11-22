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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.InvalidSyntaxException;

import com.google.common.collect.ImmutableList;

import io.wcm.sling.commons.caservice.ContextAwareServiceResolver;
import io.wcm.sling.commons.caservice.ContextAwareServiceResolver.ResolveAllResult;
import io.wcm.sling.commons.caservice.PathPreprocessor;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

/**
 * Test resolving context-aware services using service tracker against the {@link TestServices}.
 */
@ExtendWith(AemContextExtension.class)
class ContextAwareServiceResolverImplTest {

  private static final String CUSTOM_PROPERTY = "myprop1";

  private final AemContext context = new AemContext();

  private TestServices testServices;
  private DummySpi contentImpl;
  private DummySpi contentDamImpl;
  private DummySpi contentSampleImpl;

  private ContextAwareServiceResolver underTest;

  @BeforeEach
  void setUp() {
    testServices = new TestServices(context);
    contentImpl = testServices.getContentService();
    contentDamImpl = testServices.getContentDamService();
    contentSampleImpl = testServices.getContentSampleService();

    underTest = context.registerInjectActivateService(new ContextAwareServiceResolverImpl());
  }

  @Test
  void testWithDefaultImpl() {
    DummySpi defaultImpl = testServices.addDefaultService();

    assertSame(contentImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/test1")));
    assertSame(contentSampleImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/sample/test1")));
    assertSame(contentImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/sample/exclude/test1")));
    assertSame(contentDamImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/dam/test1")));
    assertSame(defaultImpl, underTest.resolve(DummySpi.class, context.create().resource("/etc/test1")));

    assertEquals(ImmutableList.of(contentDamImpl, contentImpl, defaultImpl),
        underTest.resolveAll(DummySpi.class, context.create().resource("/content/dam/test2")).getServices().collect(Collectors.toList()));
  }

  @Test
  void testWithoutDefaultImpl() {
    assertSame(contentImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/test1")));
    assertSame(contentSampleImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/sample/test1")));
    assertSame(contentImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/sample/exclude/test1")));
    assertSame(contentDamImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/dam/test1")));
    assertNull(underTest.resolve(DummySpi.class, context.create().resource("/etc/test1")));

    assertEquals(ImmutableList.of(contentDamImpl, contentImpl),
        underTest.resolveAll(DummySpi.class, context.create().resource("/content/dam/test2")).getServices().collect(Collectors.toList()));
    assertEquals(ImmutableList.of(contentSampleImpl, contentImpl),
        underTest.resolveAll(DummySpi.class, context.create().resource("/content/sample/test2")).getServices().collect(Collectors.toList()));
  }

  @Test
  void testWithSlingHttpServletRequest() {
    DummySpi defaultImpl = testServices.addDefaultService();

    context.currentResource(context.create().resource("/content/sample/test1"));
    assertSame(contentSampleImpl, underTest.resolve(DummySpi.class, context.request()));

    assertEquals(ImmutableList.of(contentSampleImpl, contentImpl, defaultImpl),
        underTest.resolveAll(DummySpi.class, context.request()).getServices().collect(Collectors.toList()));
  }

  /**
   * Simulate an experience fragment resource included in a page.
   * Context-aware service resolver should take the current page as context to resolve, not the current resource.
   */
  @Test
  void testWithSlingHttpServletRequest_ResourceOtherContext() {
    DummySpi defaultImpl = testServices.addDefaultService();

    context.currentPage(context.create().page("/content/sample/test1"));
    context.currentResource(context.create().resource("/content/experience-fragments/test1"));
    assertSame(contentSampleImpl, underTest.resolve(DummySpi.class, context.request()));

    assertEquals(ImmutableList.of(contentSampleImpl, contentImpl, defaultImpl),
        underTest.resolveAll(DummySpi.class, context.request()).getServices().collect(Collectors.toList()));
  }

  @Test
  void testWithNull() {
    DummySpi defaultImpl = testServices.addDefaultService();

    assertSame(defaultImpl, underTest.resolve(DummySpi.class, null));

    assertEquals(ImmutableList.of(defaultImpl), underTest.resolveAll(DummySpi.class, null).getServices().collect(Collectors.toList()));
  }

  @Test
  void testResolveAllCombindedKey() {
    ResolveAllResult result1 = underTest.resolveAll(DummySpi.class, context.create().resource("/content/dam/test1"));
    ResolveAllResult result2 = underTest.resolveAll(DummySpi.class, context.create().resource("/content/dam/test2"));
    ResolveAllResult result3 = underTest.resolveAll(DummySpi.class, context.create().resource("/content/sample/test3"));

    assertEquals(result1.getCombinedKey(), result2.getCombinedKey());
    assertNotEquals(result1.getCombinedKey(), result3.getCombinedKey());
  }

  @Test
  void testWithBundleHeader() {
    DummySpi contentDamImplWithBundleHeader = testServices.addContentDamImplWithBundleHeader();

    assertSame(contentImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/test1")));
    assertSame(contentSampleImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/sample/test1")));
    assertSame(contentImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/sample/exclude/test1")));
    assertSame(contentDamImplWithBundleHeader, underTest.resolve(DummySpi.class, context.create().resource("/content/dam/test1")));
    assertNull(underTest.resolve(DummySpi.class, context.create().resource("/etc/test1")));

    assertEquals(ImmutableList.of(contentDamImplWithBundleHeader, contentDamImpl, contentImpl),
        underTest.resolveAll(DummySpi.class, context.create().resource("/content/dam/test2")).getServices().collect(Collectors.toList()));
  }

  @Test
  void testWithPathPreProcessor() {
    context.registerService(PathPreprocessor.class, (path, resourceResolver) -> StringUtils.removeStart(path, "/pathprefix"));
    underTest = context.registerInjectActivateService(new ContextAwareServiceResolverImpl());

    assertSame(contentImpl, underTest.resolve(DummySpi.class, context.create().resource("/pathprefix/content/test1")));
    assertSame(contentSampleImpl, underTest.resolve(DummySpi.class, context.create().resource("/pathprefix/content/sample/test1")));
    assertSame(contentImpl, underTest.resolve(DummySpi.class, context.create().resource("/pathprefix/content/sample/exclude/test1")));
    assertSame(contentDamImpl, underTest.resolve(DummySpi.class, context.create().resource("/pathprefix/content/dam/test1")));
    assertNull(underTest.resolve(DummySpi.class, context.create().resource("/pathprefix/etc/test1")));

    assertEquals(ImmutableList.of(contentDamImpl, contentImpl),
        underTest.resolveAll(DummySpi.class, context.create().resource("/pathprefix/content/dam/test2")).getServices().collect(Collectors.toList()));
  }

  @Test
  void testWithFilter() throws InvalidSyntaxException {
    String filter = "(!(" + CUSTOM_PROPERTY + "=s3))";
    assertSame(contentImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/test1"), filter));
    assertSame(contentImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/sample/test1"), filter));
    assertSame(contentImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/sample/exclude/test1"), filter));
    assertSame(contentDamImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/dam/test1"), filter));
    assertNull(underTest.resolve(DummySpi.class, context.create().resource("/etc/test1"), filter));

    assertEquals(ImmutableList.of(contentDamImpl, contentImpl),
        underTest.resolveAll(DummySpi.class, context.create().resource("/content/dam/test2"), filter).getServices().collect(Collectors.toList()));
    assertEquals(ImmutableList.of(contentImpl),
        underTest.resolveAll(DummySpi.class, context.create().resource("/content/sample/test2"), filter).getServices().collect(Collectors.toList()));
  }

}
