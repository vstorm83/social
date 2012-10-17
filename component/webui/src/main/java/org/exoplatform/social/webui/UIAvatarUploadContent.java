/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.webui;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.image.ImageUtils;
import org.exoplatform.social.core.model.AvatarAttachment;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.webui.space.UISpaceInfo;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Displays uploaded content from UIAvatarUploader.<br>
 *
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 4, 2009
 */
@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "classpath:groovy/social/webui/UIAvatarUploadContent.gtmpl",
  events = {
    @EventConfig(listeners = UIAvatarUploadContent.CropActionListener.class),
    @EventConfig(listeners = UIAvatarUploadContent.SaveActionListener.class, phase = Phase.PROCESS),
    @EventConfig(listeners = UIAvatarUploadContent.CancelActionListener.class)
  }
)
public class UIAvatarUploadContent extends UIForm {

  private static final String CROPPED_INFO = "croppedInfo";
  private static final String X = "X";
  private static final String Y = "Y";
  private static final String WIDTH = "WIDTH";
  private static final String HEIGHT = "HEIGHT";
  
  /** AvatarAttachment instance. */
  private AvatarAttachment avatarAttachment;

  /** Stores information of image storage. */
  private String imageSource;

  /** */
  private InputStream originImg;
  
  /** */
  private byte[] resizedImgInBytes;
  
  private Map<String, String> croppedInfo;
  
  /**
   * Default constructor.<br>
   *
   */
  public UIAvatarUploadContent() {
    UIFormStringInput input = new UIFormStringInput(CROPPED_INFO, CROPPED_INFO, "0");
    input.setId(CROPPED_INFO);
    addUIFormInput(input);
  }

  /**
   * Initializes object at the first run time.<br>
   *
   * @param AvatarAttachment
   *        Information about attachment.
   *
   * @throws Exception
   */
  public UIAvatarUploadContent(AvatarAttachment avatarAttachment) throws Exception {
    this.avatarAttachment = avatarAttachment;
    setImageSource(avatarAttachment.getImageBytes());
  }


  /**
   * Gets information of AvatarAttachment.<br>
   *
   * @return AvatarAttachment
   */
  public AvatarAttachment getAvatarAttachment() {
    return avatarAttachment;
  }

  /**
   * Sets information of AvatarAttachment.<br>
   *
   * @param AvatarAttachment
   *
   * @throws Exception
   */
  public void setAvatarAttachment(AvatarAttachment avatarAttachment) throws Exception {
    this.avatarAttachment = avatarAttachment;
    setImageSource(avatarAttachment.getImageBytes());
  }

  /**
   * Gets the source of image.
   *
   * @return imageSource link
   */
  public String getImageSource() {
    return imageSource;
  }

  /**
   * 
   * @return
   */
  public InputStream getOriginImg() {
    return originImg;
  }

  /**
   * 
   * @param originImg
   */
  public void setOriginImg(InputStream originImg) {
    this.originImg = originImg;
  }


  /*
   * 
   */
  public Map<String, String> getCroppedInfo() {
    return croppedInfo;
  }

  /**
   * 
   * @param croppedInfo
   */
  public void setCroppedInfo(Map<String, String> croppedInfo) {
    this.croppedInfo = croppedInfo;
  }

  /**
   * 
   * @return
   */
  public byte[] getResizedImgInBytes() {
    return resizedImgInBytes;
  }

  /**
   * 
   * @param resizedImgInBytes
   */
  public void setResizedImgInBytes(byte[] resizedImgInBytes) {
    this.resizedImgInBytes = resizedImgInBytes;
  }

  /**
   * Crop image, make preview image.
   *
   */
  public static class CropActionListener extends EventListener<UIAvatarUploadContent> {
    @Override
    public void execute(Event<UIAvatarUploadContent> event) throws Exception {
      UIAvatarUploadContent uiAvatarUploadContent = event.getSource();
      AvatarAttachment att = uiAvatarUploadContent.avatarAttachment;
      
      String croppedInfoVal =  ((UIFormStringInput)uiAvatarUploadContent.getChildById(CROPPED_INFO)).getValue();
      Map<String, String> croppedInfos = getCroppedInfoValues(croppedInfoVal);

      // set cropping information
      uiAvatarUploadContent.setCroppedInfo(croppedInfos);

      // get cropped information
      int x = (int)Double.parseDouble(croppedInfos.get(X));
      int y = (int)Double.parseDouble(croppedInfos.get(Y));
      int w = (int)Double.parseDouble(croppedInfos.get(WIDTH));
      int h = (int)Double.parseDouble(croppedInfos.get(HEIGHT));
      
      InputStream in = new ByteArrayInputStream(att.getImageBytes());
      
      //
      uiAvatarUploadContent.setResizedImgInBytes(att.getImageBytes());
      
      BufferedImage image = ImageIO.read(in);
      
      //
      image = image.getSubimage(x, y, w, h);
      
      // create and re-store attachment info
      MimeTypeResolver mimeTypeResolver = new MimeTypeResolver();
      String extension = mimeTypeResolver.getExtension(att.getMimeType());
      
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      ImageIO.write(image, extension, os);
      os.flush();
      byte[] imageInByte = os.toByteArray();
      os.close();
      att.setImageBytes(imageInByte);
      InputStream input = new ByteArrayInputStream(imageInByte);

      att.setInputStream(input);
      
      uiAvatarUploadContent.setAvatarAttachment(att);
      
      event.getRequestContext().addUIComponentToUpdateByAjax(uiAvatarUploadContent);
    }

    private Map<String, String> getCroppedInfoValues(String input) {
      Map<String, String> croppedInfo = new HashMap<String, String>();
      String[] value = input.split(",");
      
      for (String val : value) {
        String[] info = val.split(":");
        croppedInfo.put(info[0], info[1]);
      }
      
      return croppedInfo;
    }
  }
  
  /**
   * Accepts and saves the uploaded image to profile.
   * Closes the popup window, refreshes UIProfile.
   *
   */
  public static class SaveActionListener extends EventListener<UIAvatarUploadContent> {
    @Override
    public void execute(Event<UIAvatarUploadContent> event) throws Exception {
      UIAvatarUploadContent uiAvatarUploadContent = event.getSource();
      
      // crop image
      uiAvatarUploadContent.crop();
      
      saveAvatar(uiAvatarUploadContent);
      UIPopupWindow uiPopup = uiAvatarUploadContent.getParent();
      uiPopup.setShow(false);
      Utils.updateWorkingWorkSpace();
    }

    private void saveAvatar(UIAvatarUploadContent uiAvatarUploadContent) throws Exception {
      UIComponent parent =uiAvatarUploadContent.getParent();
      while (parent != null) {
         if (UISpaceInfo.class.isInstance(parent)) {
           UISpaceInfo uiSpaceInfo = ((UISpaceInfo)parent);
           SpaceService spaceService = uiSpaceInfo.getSpaceService();
           String id = uiSpaceInfo.getUIStringInput("id").getValue();
           Space space = spaceService.getSpaceById(id);
           if (space != null) {
             uiSpaceInfo.saveAvatar(uiAvatarUploadContent, space);
             return;
           }
         }
         parent = parent.getParent();
      }
      
      // Save user avatar
      uiAvatarUploadContent.saveUserAvatar(uiAvatarUploadContent);
      return;
    }
  }

  /**
   * Saves avatar of users.
   * 
   * @param uiAvatarUploadContent
   * @throws Exception
   * @since 1.2.2
   */
  public void saveUserAvatar(UIAvatarUploadContent uiAvatarUploadContent) throws Exception {
    AvatarAttachment attacthment = uiAvatarUploadContent.getAvatarAttachment();
    
    Profile p = Utils.getOwnerIdentity().getProfile();
    p.setProperty(Profile.AVATAR, attacthment);
    Map<String, Object> props = p.getProperties();

    // Removes avatar url and resized avatar
    for (String key : props.keySet()) {
      if (key.startsWith(Profile.AVATAR + ImageUtils.KEY_SEPARATOR)) {
        p.removeProperty(key);
      }
    }

    Utils.getIdentityManager().updateProfile(p);
  }
  
  /**
   * Cancels, close the popup window.
   *
   */
  public static class CancelActionListener extends EventListener<UIAvatarUploadContent> {
    @Override
    public void execute(Event<UIAvatarUploadContent> event) throws Exception {
      UIAvatarUploadContent uiAvatarUploadContent = event.getSource();
      UIPopupWindow uiPopup = uiAvatarUploadContent.getParent();
      uiPopup.setShow(false);
    }

  }

  /**
   * Sets information of image storage.<br>
   *
   * @param imageBytes
   *        Image information in byte type for storing.
   * @throws Exception
   */
  private void setImageSource(byte[] imageBytes) throws Exception {
    if (imageBytes == null || imageBytes.length == 0) return;
    ByteArrayInputStream byteImage = new ByteArrayInputStream(imageBytes);
    DownloadService downloadService = getApplicationComponent(DownloadService.class);
    InputStreamDownloadResource downloadResource = new InputStreamDownloadResource(byteImage, "image");
    downloadResource.setDownloadName(avatarAttachment.getFileName());
    imageSource = downloadService.getDownloadLink(downloadService.addDownloadResource(downloadResource));
  }
  
  private void crop() throws Exception {
    BufferedImage originImg = ImageIO.read(getOriginImg());
    BufferedImage resizedImg = ImageIO.read(new ByteArrayInputStream(getResizedImgInBytes()));

    // Original image information
    int w_o = originImg.getWidth();
    int h_o = originImg.getHeight();
    
    // Resized image information
    int w_r = resizedImg.getWidth();
    int h_r = resizedImg.getHeight();
    
    // cropped image information on resized image
    int x_cr = (int)Double.parseDouble(getCroppedInfo().get(X));
    int y_cr = (int)Double.parseDouble(getCroppedInfo().get(Y));
    int width_cr = (int)Double.parseDouble(getCroppedInfo().get(WIDTH));
    int height_cr = (int)Double.parseDouble(getCroppedInfo().get(HEIGHT));
    
    // calculate the scale
    double scale_w = (double)w_o/(double)w_r;
    double scale_h = (double)h_o/(double)h_r;
    
    double scale_OR = scale_w > scale_h ? scale_h : scale_w;
    
    // cropped image information on original image
    int x_co = (int)(x_cr*scale_w);
    int y_co = (int)(y_cr*scale_h);
    int width_co = (int) (width_cr*scale_OR); 
    int height_co = (int) (height_cr*scale_OR);
    
    // sub image with new information
    BufferedImage croppedImg = originImg.getSubimage(x_co, y_co, width_co, height_co);
    
    AvatarAttachment att = getAvatarAttachment();
    MimeTypeResolver mimeTypeResolver = new MimeTypeResolver();
    String extension = mimeTypeResolver.getExtension(att.getMimeType());
    
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    ImageIO.write(croppedImg, extension, os);
    os.flush();
    byte[] imageInByte = os.toByteArray();
    os.close();
    att.setImageBytes(imageInByte);
    InputStream input = new ByteArrayInputStream(imageInByte);

    att.setInputStream(input);
    
    setAvatarAttachment(att);
  }
}
