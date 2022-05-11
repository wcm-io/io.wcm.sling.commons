/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 wcm.io
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
package io.wcm.sling.commons.osgi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.sling.commons.osgi.ServiceUtil;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Helper class that collects all services registered via OSGi bind/unbind methods.
 * The services are ordered by service ranking and can be iterated directly using this object instance.
 * Implementation is thread-safe.
 * @param <T> Service type
 * @deprecated Please use <code>org.apache.sling.commons.osgi.RankedServices</code> instead.
 */
@Deprecated
@ProviderType
public final class RankedServices<T> implements Iterable<T> {

  private final ChangeListener changeListener;
  private final SortedMap<Comparable<Object>, T> serviceMap = new TreeMap<>();
  private volatile Collection<T> sortedServices = Collections.emptyList();

  /**
   * Instantiate without change listener.
   */
  public RankedServices() {
    this(null);
  }

  /**
   * Instantiate without change listener.
   * @param changeListener Change listener
   */
  public RankedServices(ChangeListener changeListener) {
    this.changeListener = changeListener;
  }

  /**
   * Handle bind service event.
   * @param service Service instance
   * @param props Service reference properties
   */
  public void bind(T service, Map<String, Object> props) {
    synchronized (serviceMap) {
      serviceMap.put(ServiceUtil.getComparableForServiceRanking(props), service);
      updateSortedServices();
    }
  }

  /**
   * Handle unbind service event.
   * @param service Service instance
   * @param props Service reference properties
   */
  @SuppressWarnings("java:S1172")
  public void unbind(T service, Map<String, Object> props) {
    synchronized (serviceMap) {
      serviceMap.remove(ServiceUtil.getComparableForServiceRanking(props));
      updateSortedServices();
    }
  }

  /**
   * Update list of sorted services by copying it from the array and making it unmodifiable.
   */
  private void updateSortedServices() {
    List<T> copiedList = new ArrayList<>(serviceMap.values());
    sortedServices = Collections.unmodifiableList(copiedList);
    if (changeListener != null) {
      changeListener.changed();
    }
  }

  /**
   * Lists all services registered in OSGi, sorted by service ranking.
   * @return Collection of service instances
   */
  public Collection<T> get() {
    return sortedServices;
  }

  /**
   * Iterates all services registered in OSGi, sorted by service ranking.
   * @return Iterator with service instances.
   */
  @Override
  public Iterator<T> iterator() {
    return sortedServices.iterator();
  }

  /**
   * Notification for changes on services list.
   */
  public interface ChangeListener {

    /**
     * Is called when the list of ranked services was changed due to bundle bindings/unbindings.
     * This method is called within a synchronized block, so it's code should be kept as efficient as possible.
     */
    void changed();

  }

}
