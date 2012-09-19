/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.core.application.filter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Author : thanh_vucong
 *          thanh_vucong@exoplatform.com
 * Sep 18, 2012  
 */
public class FilterConfiguration extends BaseComponentPlugin {

  private static final Log LOG = ExoLogger.getLogger(FilterConfiguration.class);
  
  private List<ApplicationFilter> appFilters = null;
  
  public FilterConfiguration(InitParams initParams) {
    if (initParams == null) {
      LOG.warn("Failed to register this plugin: initParams is null");
      return;
    }
    
    Iterator<ObjectParameter> itr = initParams.getObjectParamIterator();
    
    if (!itr.hasNext()) {
      LOG.warn("Failed to register this route configuration: no <object-param>");
      return;
    }
    
    appFilters = new ArrayList<ApplicationFilter>();
    while (itr.hasNext()) {
      ObjectParameter objectParameter = itr.next();
      ApplicationFilter appFilter = (ApplicationFilter) objectParameter.getObject();
      appFilters.add(appFilter);
    }
  }
  
  public FilterConfiguration() {
    appFilters = new ArrayList<ApplicationFilter>();
  }
  /**
   * 
   * @param filter
   * @return
   */
  public boolean hasConfigured(ApplicationFilter filter) {
    for(ApplicationFilter element: appFilters) {
      if(element.equals(filter)) {
        return true;
      }
    }
    return false;
  }

  
  /**
   * import ApplicationFilter from other Filter to this.
   * @param importConfig
   */
  public void importConfiguration(FilterConfiguration importConfig) {
    appFilters.addAll(importConfig.getAppFilters());
  }

  /**
   * Gets ApplicationFilter list
   * @return
   */
  public List<ApplicationFilter> getAppFilters() {
    return appFilters;
  }
  
  
}
