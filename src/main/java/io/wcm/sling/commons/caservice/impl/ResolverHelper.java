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

import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.day.cq.wcm.api.components.ComponentContext;
import com.day.cq.wcm.commons.WCMUtils;

import io.wcm.sling.commons.caservice.ContextAwareService;
import io.wcm.sling.commons.caservice.PathPreprocessor;

/**
 * Helps mapping adaptable to actual resource, and getting resource path respecting a path preprocessor.
 */
class ResolverHelper {

  private final PathPreprocessor pathPreprocessor;

  ResolverHelper(@Nullable PathPreprocessor pathPreprocessor) {
    this.pathPreprocessor = pathPreprocessor;
  }

  /**
   * Get resource path from resource represented by Adaptable.
   * @param adaptable Either a {@link Resource} or a {@link SlingHttpServletRequest} instance.
   * @return Resource path or null
   */
  public @Nullable String getResourcePath(@Nullable Adaptable adaptable) {
    Resource resource = getResourceFromAdaptable(adaptable);
    String path = null;
    if (resource != null) {
      path = resource.getPath();
      if (pathPreprocessor != null) {
        // apply path preprocessor
        path = pathPreprocessor.apply(path, resource.getResourceResolver());
      }
    }
    return path;
  }

  private @Nullable Resource getResourceFromAdaptable(@Nullable Adaptable adaptable) {
    if (adaptable instanceof Resource) {
      return (Resource)adaptable;
    }
    else if (adaptable instanceof SlingHttpServletRequest) {
      // if request has a current page prefer the page content resource as context resource
      // because otherwise included resource e.g. from experience fragments lead to wrong contexts
      SlingHttpServletRequest request = (SlingHttpServletRequest)adaptable;
      ComponentContext wcmComponentContext = WCMUtils.getComponentContext(request);
      if (wcmComponentContext != null && wcmComponentContext.getPage() != null) {
        return wcmComponentContext.getPage().getContentResource();
      }
      else {
        return request.getResource();
      }
    }
    return null;
  }

  /**
   * Maps a list of service infos to a list of services, filtering out invalid ones.
   * @param <T> Service class or interface
   * @param serviceInfos Service infos
   * @return Valid services
   */
  @SuppressWarnings("null")
  public <T extends ContextAwareService> Stream<T> getValidServices(Stream<ServiceInfo<T>> serviceInfos) {
    return serviceInfos
        .filter(ServiceInfo::isValid)
        .map(ServiceInfo::getService);
  }

  /**
   * Build a timestamp for {@link ResolveAllResultImpl} based on timestamp of service tracker/collection
   * and the stream of returned services.
   * @param timestamp Timestamp from service tracker or collection.
   * @param serviceInfos Service infos
   * @return Supplier which builds timestamp on demand.
   */
  public <T extends ContextAwareService> @NotNull Supplier<String> buildCombinedKey(long timestamp,
      @NotNull Stream<ServiceInfo<T>> serviceInfos) {
    return () -> timestamp + "\n"
        + serviceInfos.map(ServiceInfo::getKey).collect(Collectors.joining("\n~\n"));
  }

}
