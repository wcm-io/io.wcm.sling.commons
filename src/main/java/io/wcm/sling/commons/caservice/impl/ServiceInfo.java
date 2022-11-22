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

import static io.wcm.sling.commons.caservice.ContextAwareService.PROPERTY_ACCEPTS_CONTEXT_PATH_EMPTY;
import static io.wcm.sling.commons.caservice.ContextAwareService.PROPERTY_CONTEXT_PATH_BLACKLIST_PATTERN;
import static io.wcm.sling.commons.caservice.ContextAwareService.PROPERTY_CONTEXT_PATH_PATTERN;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.wcm.sling.commons.caservice.ContextAwareService;

/**
 * Extracts metadata of a context-aware service implementation.
 */
class ServiceInfo<T extends ContextAwareService> {

  private static final Pattern PATTERN_MATCH_ALL = Pattern.compile(".*");

  private final @Nullable T service;
  private final Map<String, Object> servicePropertiesMap;
  private final Dictionary<String, Object> servicePropertiesDictionary;
  private final Pattern contextPathRegex;
  private final Pattern contextPathBlacklistRegex;
  private final boolean acceptsContextPathEmpty;
  private final String key;
  private final boolean valid;

  private static final Logger log = LoggerFactory.getLogger(ServiceInfo.class);

  /**
   * @param serviceReference Service reference
   * @param bundleContext Bundle context
   */
  ServiceInfo(@NotNull ServiceReference<T> serviceReference, @NotNull BundleContext bundleContext) {
    this(serviceReference, validateAndGetService(serviceReference, bundleContext));
  }

  /**
   * @param serviceReference Service reference
   * @param service Service instance
   */
  ServiceInfo(@NotNull ServiceReference<T> serviceReference, @Nullable T service) {
    this.service = service;
    this.servicePropertiesDictionary = serviceReference.getProperties();
    this.servicePropertiesMap = propertiesToMap(serviceReference);
    this.contextPathRegex = validateAndParsePattern(serviceReference, service, PROPERTY_CONTEXT_PATH_PATTERN);
    this.contextPathBlacklistRegex = validateAndParsePattern(serviceReference, service, PROPERTY_CONTEXT_PATH_BLACKLIST_PATTERN);
    this.acceptsContextPathEmpty = validateAndGetBoolan(lookupServicePropertyBundleHeader(serviceReference, PROPERTY_ACCEPTS_CONTEXT_PATH_EMPTY));
    this.key = buildKey();
    this.valid = service != null && contextPathRegex != null && contextPathBlacklistRegex != null;
  }

  @SuppressWarnings("unchecked")
  private static <T extends ContextAwareService> @Nullable T validateAndGetService(
      @NotNull ServiceReference<T> serviceReference, @NotNull BundleContext bundleContext) {
    Object serviceObject = bundleContext.getService(serviceReference);
    if (serviceObject instanceof ContextAwareService) {
      return (T)serviceObject;
    }
    if (log.isWarnEnabled()) {
      log.warn("Service implementation {} does not implement the ContextAwareService interface"
          + " - service will be ignored for context-aware service resolution.", (serviceObject != null ? serviceObject.getClass().getName() : ""));
    }
    return null;
  }

  private static <T extends ContextAwareService> Map<String, Object> propertiesToMap(@NotNull ServiceReference<T> reference) {
    Map<String, Object> props = new HashMap<>();
    for (String propertyName : reference.getPropertyKeys()) {
      props.put(propertyName, reference.getProperty(propertyName));
    }
    return props;
  }

  private static <T extends ContextAwareService> Object lookupServicePropertyBundleHeader(
      @NotNull ServiceReference<T> serviceReference, @NotNull String propertyName) {
    Object value = serviceReference.getProperty(propertyName);
    if (value == null) {
      value = serviceReference.getBundle().getHeaders().get(propertyName);
    }
    return value;
  }

  private static <T extends ContextAwareService> Pattern validateAndParsePattern(
      @NotNull ServiceReference<T> serviceReference, @Nullable T service, @NotNull String patternPropertyName) {
    Object value = lookupServicePropertyBundleHeader(serviceReference, patternPropertyName);
    if (value == null || value instanceof String) {
      String patternString = (String)value;
      if (StringUtils.isEmpty(patternString)) {
        return PATTERN_MATCH_ALL;
      }
      else {
        try {
          return Pattern.compile(patternString);
        }
        catch (PatternSyntaxException ex) {
          // fallback to invalid
        }
      }
    }
    if (log.isWarnEnabled()) {
      log.warn("Invalid {} regex pattern '{}' - service {} from bundle {} will be ignored for context-aware service resolution.",
          patternPropertyName, value, service != null ? service.getClass().getName() : "", serviceReference.getBundle().getSymbolicName());
    }
    return null;
  }

  private static boolean validateAndGetBoolan(Object value) {
    if (value instanceof Boolean) {
      return (Boolean)value;
    }
    if (value instanceof String) {
      return BooleanUtils.toBoolean((String)value);
    }
    return false;
  }

  /**
   * Service implementation.
   * @return Service object.
   */
  public @Nullable T getService() {
    return this.service;
  }

  /**
   * Service properties
   * @return Property map
   */
  public Map<String, Object> getServiceProperties() {
    return this.servicePropertiesMap;
  }

  /**
   * @return Valid service
   */
  public boolean isValid() {
    return this.valid;
  }

  /**
   * Checks if this service implementation accepts the given resource path.
   * @param resourcePath Resource path
   * @return true if the implementation matches and the configuration is not invalid.
   */
  public boolean matchesPath(String resourcePath) {
    if (!valid) {
      return false;
    }
    if (resourcePath == null) {
      return acceptsContextPathEmpty;
    }
    if (contextPathRegex != PATTERN_MATCH_ALL && !contextPathRegex.matcher(resourcePath).matches()) {
      return false;
    }
    if (contextPathBlacklistRegex != PATTERN_MATCH_ALL && contextPathBlacklistRegex.matcher(resourcePath).matches()) {
      return false;
    }
    return true;
  }

  /**
   * Checks if the services matched the given filter.
   * @param filter OSGi filter. If null it matches always.
   * @return true if matching
   */
  public boolean matchesFilter(@Nullable Filter filter) {
    if (filter == null) {
      return true;
    }
    return filter.match(servicePropertiesDictionary);
  }

  private String buildKey() {
    return "[wl]" + contextPathRegex + "\n"
        + "[bl]" + contextPathBlacklistRegex + "\n";
  }

  /**
   * Key from the path matching patterns defined by this service implementation that
   * can be used for caching and faster lookup of matching services.
   * @return Key of all path patterns
   */
  public String getKey() {
    return this.key;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(service, TO_STRING_STYLE);
    if (contextPathRegex != null) {
      builder.append("contextPathRegex", contextPathRegex);
    }
    if (contextPathBlacklistRegex != null) {
      builder.append("contextPathBlacklistRegex", contextPathBlacklistRegex);
    }
    if (acceptsContextPathEmpty) {
      builder.append("acceptsContextPathEmpty", acceptsContextPathEmpty);
    }
    return builder.build();
  }

  @SuppressWarnings("java:S1171")
  private static final ToStringStyle TO_STRING_STYLE = new ToStringStyle() {
    private static final long serialVersionUID = 1L;
    {
      setUseIdentityHashCode(false);
      setContentStart(" [");
    }
  };

}
