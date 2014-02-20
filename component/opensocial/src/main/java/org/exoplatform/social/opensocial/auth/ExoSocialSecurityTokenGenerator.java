/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.social.opensocial.auth;


import java.util.HashMap;
import java.util.Map;

import org.apache.shindig.auth.AbstractSecurityToken.Keys;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.gadget.core.ExoDefaultSecurityTokenGenerator;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.RequestContext;

/**
 * The Social Security Token Generator.
 *
 * @author <a href="http://hoatle.net">hoatle (hoatlevan at gmail dot com)</a>
 */
public class ExoSocialSecurityTokenGenerator extends ExoDefaultSecurityTokenGenerator {

  /**
   * Logger
   */
  private static Log LOG = ExoLogger.getLogger(ExoSocialSecurityTokenGenerator.class);

  /**
   * Default Constructor.
   *
   * @throws Exception
   */
  public ExoSocialSecurityTokenGenerator() throws Exception {
    super();
  }

  /**
   * {@inheritDoc}
   * Creates a security token with some more data from Social instead of default implementation one.
   */
  protected String createToken(String gadgetURL, String owner, String viewer, Long moduleId, String container) {
    try {
        Map<String, String> values = new HashMap<String, String>();
        values.put(Keys.APP_URL.getKey(), gadgetURL);
        values.put(Keys.MODULE_ID.getKey(), Long.toString(moduleId));
        values.put(Keys.OWNER.getKey(), owner);
        values.put(Keys.VIEWER.getKey(), viewer);
        values.put(Keys.TRUSTED_JSON.getKey(), "trusted");
        
      ExoBlobCrypterSecurityToken t = new ExoBlobCrypterSecurityToken(container, null, null, values);
      String portalContainer = PortalContainer.getCurrentPortalContainerName();
      PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
      String url = portalRequestContext.getRequest().getRequestURL().toString();
      String hostName = url.substring(0, url.indexOf(portalRequestContext.getRequestContextPath()));
      t.setPortalContainer(portalContainer);
      t.setHostName(hostName);
      t.setPortalOwner(portalRequestContext.getPortalOwner());
      
      BlobCrypter crypter = getBlobCrypter();

      return t.getContainer() + ":" + crypter.wrap(t.toMap());
    } catch (Exception e) {
      LOG.error("Failed to generate token for gadget " + gadgetURL + " for owner " + owner, e);
    }
    return null;
  }

  /**
   * Creates a token by gadgetURL and moduleId.
   *
   * @param gadgetURL
   * @param moduleId
   * @return
   */
  public String createToken(String gadgetURL, Long moduleId) {
    RequestContext context = RequestContext.getCurrentInstance();
    String rUserId = context.getRemoteUser();
    String ownerId = rUserId;
    return createToken(gadgetURL, ownerId, rUserId, moduleId, "default");
  }

}

