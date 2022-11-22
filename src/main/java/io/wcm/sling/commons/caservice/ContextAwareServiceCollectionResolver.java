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
package io.wcm.sling.commons.caservice;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

import io.wcm.sling.commons.caservice.ContextAwareServiceResolver.ResolveAllResult;

/**
 * Resolves the best-matching context-aware service implementation.
 * This is based on a pre-populated collection of service objects in the correct order, so this collection
 * resolver only filters the list by respecting the resource context and configured service/bundle properties.
 * @param <T> Service interface or class
 */
@ProviderType
public interface ContextAwareServiceCollectionResolver<T extends ContextAwareService> {

  /**
   * Resolves the best-matching service implementation for the given resource context.
   * Only implementations which accept the given context resource path (via the service properties defined in
   * {@link ContextAwareService}) are considered as candidates.
   * If multiple candidates exist the implementation with the highest service ranking is returned.
   * @param adaptable Adaptable which is either a {@link Resource} or {@link SlingHttpServletRequest}.
   *          A resource instance is used directly for matching, in case of request the associated resource is used.
   *          May be null if no context is available.
   * @return Service implementation or null if no match found.
   */
  @Nullable
  T resolve(@Nullable Adaptable adaptable);

  /**
   * Resolves all matching service implementations for the given resource context.
   * Only implementations which accept the given context resource path (via the service properties defined in
   * {@link ContextAwareService}) are considered as candidates.
   * The candidates are returned ordered descending by their service ranking.
   * @param adaptable Adaptable which is either a {@link Resource} or {@link SlingHttpServletRequest}.
   *          A resource instance is used directly for matching, in case of request the associated resource is used.
   *          May be null if no context is available.
   * @return Collection of all matching services
   */
  @NotNull
  ResolveAllResult<T> resolveAll(@Nullable Adaptable adaptable);

}
