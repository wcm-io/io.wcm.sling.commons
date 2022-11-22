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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.inventory.Format;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.sling.commons.caservice.ContextAwareServiceResolver;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

/**
 * Test inventory printer.
 */
@ExtendWith(AemContextExtension.class)
class ContextAwareServiceInventoryPrinterTest {

  private final AemContext context = new AemContext();
  private ContextAwareServiceResolver contextAwareServiceResolver;
  private ContextAwareServiceInventoryPrinter underTest;

  @BeforeEach
  protected void setUp() {
    // register test services
    new TestServices(context);
    contextAwareServiceResolver = context.registerInjectActivateService(new ContextAwareServiceResolverImpl());
    underTest = context.registerInjectActivateService(ContextAwareServiceInventoryPrinter.class);
  }

  @Test
  void testNoServiceTracker() throws IOException {
    String result = getResultFromInventoryPrinter(Format.TEXT);
    assertTrue(StringUtils.contains(result, "No context-aware services found."));
  }

  @Test
  void testWithServiceTracker() throws IOException {
    // make dummy call to have a service tracker registered
    contextAwareServiceResolver.resolve(DummySpi.class, null);

    String result = getResultFromInventoryPrinter(Format.TEXT);
    assertTrue(StringUtils.contains(result, DummySpi.class.getName()));
  }

  @Test
  void testNonText() throws IOException {
    String result = getResultFromInventoryPrinter(Format.HTML);
    assertTrue(StringUtils.isEmpty(result));
  }

  private String getResultFromInventoryPrinter(Format format) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    PrintWriter pw = new PrintWriter(bos);
    underTest.print(pw, format, false);
    pw.flush();

    return IOUtils.toString(bos.toByteArray(), StandardCharsets.UTF_8.name());
  }

}
