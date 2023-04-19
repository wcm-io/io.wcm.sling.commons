/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
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
package io.wcm.sling.commons.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import io.wcm.sling.commons.util.Escape;

class QueryStringBuilderTest {

  private static final String SPECIAL_CHARS = "a!:$&=";

  @Test
  void testEmpty() {
    assertNull(new QueryStringBuilder().build());
  }

  @Test
  void testSimple() {
    assertEquals("p1=value1&p2=123&p3=true&p4=", new QueryStringBuilder()
        .param("p1", "value1")
        .param("p2", 123)
        .param("p3", true)
        .param("p4", null)
        .build());
  }

  @Test
  void testUrlEncoding() {
    assertEquals("p1=" + Escape.urlEncode(SPECIAL_CHARS) + "&" + Escape.urlEncode(SPECIAL_CHARS) + "=value2", new QueryStringBuilder()
        .param("p1", SPECIAL_CHARS)
        .param(SPECIAL_CHARS, "value2")
        .build());
  }

  @Test
  void testMulti() {
    assertEquals("p1=value1&p1=value2&p1=&p2=1&p2=2&p3=false&p3=true&p4=abc", new QueryStringBuilder()
        .param("p1", new String[] { "value1", "value2", null })
        .param("p2", List.of(1, 2))
        .param("p3", new TreeSet<>(Set.of(false, true)))
        .param("p4", "abc")
        .build());
  }

  @Test
  void testMap() {
    assertEquals("p1=value1&p1=value2&p1=&p2=1&p2=2&p3=false&p3=true&p4=abc", new QueryStringBuilder()
        .params(new TreeMap<>(Map.of(
            "p1", new String[] { "value1", "value2", null },
            "p2", List.of(1, 2),
            "p3", new TreeSet<>(Set.of(false, true)),
            "p4", "abc"
        )))
        .build());
  }

}
