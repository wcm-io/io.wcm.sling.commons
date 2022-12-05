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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.Nullable;

import com.day.cq.wcm.api.components.ComponentContext;
import com.day.cq.wcm.commons.WCMUtils;

import io.wcm.sling.commons.caservice.PathPreprocessor;

/**
 * Helps mapping adaptable to actual resource, and getting resource path respecting a path preprocessor.
 */
class ResourcePathResolver {

  private final PathPreprocessor pathPreprocessor;

  ResourcePathResolver(@Nullable PathPreprocessor pathPreprocessor) {
    this.pathPreprocessor = pathPreprocessor;
  }

  /**
   * Get resource path from resource represented by Adaptable.
   * @param adaptable Either a {@link Resource} or a {@link SlingHttpServletRequest} instance.
   * @return Resource path or null
   */
  public @Nullable String get(@Nullable Adaptable adaptable) {
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

}
