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


/**
 * Created by The eXo Platform SAS
 * Author : thanh_vucong
 *          thanh_vucong@exoplatform.com
 * Sep 18, 2012  
 */
public class ApplicationFilter {

  private String portletName = null;
  
  public static ApplicationFilter with(String portletName) {
    return new ApplicationFilter(portletName);
  }
  /**
   * Provides for IOC to invoke.
   */
  public ApplicationFilter() {
    
  }
  
  public ApplicationFilter(String portletName) {
    this.portletName = portletName;
  }

  public String getPortletName() {
    return portletName;
  }

  public void setPortletName(String portletName) {
    this.portletName = portletName;
  }
  
  
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ApplicationFilter)) {
      return false;
    }

    ApplicationFilter filter = (ApplicationFilter) o;

    if (portletName != null ? !portletName.equals(filter.portletName) : filter.portletName != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return portletName != null ? portletName.hashCode() : 0;
  }
  
}
