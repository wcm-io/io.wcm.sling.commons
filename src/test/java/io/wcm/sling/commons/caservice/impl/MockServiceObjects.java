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

import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;

class MockServiceObjects<S> implements ServiceObjects<S> {

  private final ServiceReference<S> serviceReference;
  private final S service;

  MockServiceObjects(ServiceReference<S> serviceReference, S service) {
    this.serviceReference = serviceReference;
    this.service = service;
  }

  @Override
  public ServiceReference<S> getServiceReference() {
    return serviceReference;
  }

  @Override
  public S getService() {
    return service;
  }

  @Override
  public void ungetService(S svc) {
    // ignore
  }

}
