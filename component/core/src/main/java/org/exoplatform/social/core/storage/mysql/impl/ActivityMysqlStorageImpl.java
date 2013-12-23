/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Affero General Public License
* as published by the Free Software Foundation; either version 3
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.core.storage.mysql.impl;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.chromattic.api.ChromatticException;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.ActivityProcessor;
import org.exoplatform.social.core.activity.filter.ActivityFilter;
import org.exoplatform.social.core.activity.filter.ActivityUpdateFilter;
import org.exoplatform.social.core.activity.model.ActivityStream;
import org.exoplatform.social.core.activity.model.ActivityStreamImpl;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.activity.mysql.model.StreamItem;
import org.exoplatform.social.core.activity.mysql.model.StreamItemImpl;
import org.exoplatform.social.core.chromattic.entity.ActivityEntity;
import org.exoplatform.social.core.chromattic.entity.HidableEntity;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.chromattic.entity.LockableEntity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.ActivityStorageException;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.ActivityStreamStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.api.RelationshipStorage;
import org.exoplatform.social.core.storage.api.SpaceStorage;
import org.exoplatform.social.core.storage.cache.model.key.ActivityType;
import org.exoplatform.social.core.storage.exception.NodeNotFoundException;
import org.exoplatform.social.core.storage.impl.ActivityBuilderWhere;
import org.exoplatform.social.core.storage.impl.StorageUtils;
import org.exoplatform.social.core.storage.streams.StreamInvocationHelper;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Huy Quang
 *          quangnh2@exoplatform.com
 * Dec 12, 2013  
 */
public class ActivityMysqlStorageImpl extends AbstractMysqlStorage implements
		ActivityStorage {

  private static final Pattern MENTION_PATTERN = Pattern.compile("@([^\\s]+)|@([^\\s]+)$");
  public static final Pattern USER_NAME_VALIDATOR_REGEX = Pattern.compile("^[\\p{L}][\\p{L}._\\-\\d]+$");
  
  public enum ViewerType {
    SPACE("SPACE"), POSTER("POSTER"), LIKE("LIKE"), COMMENTER("COMMENTER"), MENTIONER("MENTIONER"), SPACE_MEMBER(
        "SPACE_MEMBER");

    private final String type;

    public String getType() {
      return type;
    }

    ViewerType(String type) {
      this.type = type;
    }
  }
  
  private final SortedSet<ActivityProcessor> activityProcessors;
  private final RelationshipStorage relationshipStorage;
  private final IdentityStorage identityStorage;
  private final SpaceStorage spaceStorage;
  private final ActivityStreamStorage streamStorage;
  private boolean mustInjectStreams = true;
  private ActivityStorage activityStorage;
  
  private static final Log LOG = ExoLogger.getLogger(ActivityMysqlStorageImpl.class);
  
  public ActivityMysqlStorageImpl(final RelationshipStorage relationshipStorage,
                                  final IdentityStorage identityStorage,
                                  final SpaceStorage spaceStorage,
                                  final ActivityStreamStorage streamStorage) {

    this.relationshipStorage = relationshipStorage;
    this.identityStorage = identityStorage;
    this.spaceStorage = spaceStorage;
    this.streamStorage = streamStorage;
    this.activityProcessors = new TreeSet<ActivityProcessor>(processorComparator());
  }
  
	@Override
	public void setInjectStreams(boolean mustInject) {
		// TODO Auto-generated method stub

	}

	@Override
	public ExoSocialActivity getActivity(String activityId)
 throws ActivityStorageException {

    //
    // ActivityEntity activityEntity = _findById(ActivityEntity.class,
    // activityId);
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;

    StringBuilder getActivitySQL = new StringBuilder();
    getActivitySQL.append("select ")
                  .append("_id, title, titleId, body, bodyId, postedTime, lastUpdated, posterId, ownerId,")
                  .append("permaLink, appId, externalId, priority, hidable, lockable, likers, metadata")
                  .append(" from activity where _id = ?");

    ExoSocialActivity activity = new ExoSocialActivityImpl();

    try {
      dbConnection = getJNDIConnection();
      preparedStatement = dbConnection.prepareStatement(getActivitySQL.toString());
      preparedStatement.setString(1, activityId);

      rs = preparedStatement.executeQuery();

      while (rs.next()) {
        fillActivityFromResultSet(rs, activity);
      }

      processActivity(activity);

      LOG.debug("activity found");

      return activity;

    } catch (SQLException e) {

      LOG.error("error in activity look up:", e.getMessage());
      return null;

    } finally {
      try {
        if (rs != null) {
          rs.close();
        }

        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
  }

  private void fillActivityFromResultSet(ResultSet rs, ExoSocialActivity activity) throws SQLException{

    activity.setId(rs.getString("_id"));
    activity.setTitle(rs.getString("title"));
    activity.setTitleId(rs.getString("titleId"));
    activity.setBody(rs.getString("body"));
    activity.setBodyId(rs.getString("bodyId"));
    activity.setUserId(rs.getString("posterId"));
    activity.setPostedTime(rs.getLong("postedTime"));
    activity.setUpdated(rs.getLong("lastUpdated"));
    //activity.setType(activityEntity.getType());
    activity.setAppId(rs.getString("appId"));
    activity.setExternalId(rs.getString("externalId"));
    //activity.setUrl(activityEntity.getUrl());
    activity.setPriority(rs.getFloat("priority"));
    if (rs.wasNull()) {
      activity.setPriority(null);
    }
    activity.setPosterId(rs.getString("posterId"));

    /*
    List<String> computeCommentid = new ArrayList<String>();
    for (ActivityEntity commentEntity : activityEntity.getComments()) {
      computeCommentid.add(commentEntity.getId());
    }

    activity.setReplyToId(computeCommentid.toArray(new String[]{}));*/
    
    String lks = rs.getString("likers");
    String[] likes = StringUtils.split(lks, ",");
    if (likes != null) {
      activity.setLikeIdentityIds(likes);
    }
    
    //mentioners and commenters are moved to StreamItem
    
    //
    activity.isLocked(rs.getBoolean("lockable"));
    activity.isHidden(rs.getBoolean("hidable"));

    activity.setStreamOwner(rs.getString("ownerId"));
    //TODO
    //fillStream(null, activity);
  }
  
  private void processActivity(ExoSocialActivity existingActivity) {
    Iterator<ActivityProcessor> it = activityProcessors.iterator();
    while (it.hasNext()) {
      try {
        it.next().processActivity(existingActivity);
      } catch (Exception e) {
        LOG.warn("activity processing failed " + e.getMessage());
      }
    }
  }
  
	@Override
	public List<ExoSocialActivity> getUserActivities(Identity owner)
			throws ActivityStorageException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getUserActivities(Identity owner,
			long offset, long limit) throws ActivityStorageException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getUserActivitiesForUpgrade(Identity owner,
			long offset, long limit) throws ActivityStorageException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getActivities(Identity owner,
			Identity viewer, long offset, long limit)
			throws ActivityStorageException {
		// TODO Auto-generated method stub
		return null;
	}

  @Override
  public void saveComment(ExoSocialActivity activity, ExoSocialActivity comment) throws ActivityStorageException {

    LOG.debug("begin to create comment");

    // insert to mysql comment table
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;

    StringBuilder insertTableSQL = new StringBuilder();
    insertTableSQL.append("INSERT INTO comment")
                  .append("(_id, activityId, title, titleId, body, bodyId, postedTime, lastUpdated, posterId, ")
                  .append("hidable, lockable)")
                  .append("VALUES (?,?,?,?,?,?,?,?,?,?,?)");

    StringBuilder updateActivitySQL = new StringBuilder();
    updateActivitySQL.append("update activity set lastUpdated = ? where _id = ?");

    long currentMillis = System.currentTimeMillis();
    long commentMillis = (comment.getPostedTime() != null ? comment.getPostedTime() : currentMillis);

    try {
      dbConnection = getJNDIConnection();

      // insert comment
      preparedStatement = dbConnection.prepareStatement(insertTableSQL.toString());

      comment.setId(UUID.randomUUID().toString());
      preparedStatement.setString(1, comment.getId());
      preparedStatement.setString(2, activity.getId());
      preparedStatement.setString(3, comment.getTitle());
      preparedStatement.setString(4, comment.getTitleId());
      preparedStatement.setString(5, comment.getBody());
      preparedStatement.setString(6, comment.getBodyId());
      preparedStatement.setLong(7, commentMillis);
      preparedStatement.setLong(8, commentMillis);
      preparedStatement.setString(9, comment.getUserId());
      preparedStatement.setBoolean(10, activity.isHidden());
      preparedStatement.setBoolean(11, activity.isLocked());

      preparedStatement.executeUpdate();

      LOG.debug("new comment created");

      // update activity
      preparedStatement = dbConnection.prepareStatement(updateActivitySQL.toString());
      preparedStatement.setLong(1, commentMillis);
      preparedStatement.setString(2, activity.getId());

      preparedStatement.executeUpdate();

      LOG.debug("activity updated");

    } catch (SQLException e) {

      LOG.error("error in comment creation:", e.getMessage());

    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }

    comment.setUpdated(commentMillis);

    Identity poster = new Identity(activity.getPosterId());
    poster.setRemoteId(activity.getStreamOwner());

    commenter(poster, activity, comment);

    // TODO update mentionner
    updateMentioner(poster, activity, comment);

  }

  /**
   * Creates StreamItem for each user who commented on the activity
   * 
   * @param poster poster of activity
   * @param activity
   * @param comment
   * @throws MongoException
   */
  private void commenter(Identity poster, ExoSocialActivity activity, ExoSocialActivity comment) {
    StreamItem o = getStreamItem(activity.getId(), comment.getUserId());
    
    if (o == null) {
      // create new stream item for COMMENTER
      createStreamItem(activity.getId(),
                       poster.getRemoteId(),
                       activity.getUserId() != null ? activity.getUserId() : poster.getId(),
                       comment.getUserId(),
                       ViewerType.COMMENTER.getType(),
                       activity.isHidden(),
                       activity.isLocked(),
                       comment.getUpdated().getTime());
      
    } else {
      //update COMMENTER
      if (StringUtils.isBlank(o.getViewerType())) {
        //add new commenter on this stream item
        updateStreamItemWithComment(o.getId(), ViewerType.COMMENTER.getType(), 1, comment.getUpdated().getTime());
      } else {
        String[] viewTypes = o.getViewerType().split(",");
        
        if(ArrayUtils.contains(viewTypes, ViewerType.COMMENTER.getType())){
          //increment only number of commenter
          updateStreamItemWithComment(o.getId(), o.getViewerType(), o.getCommenter() + 1, comment.getUpdated().getTime());
        } else {
          //add new COMMENTER element to viewerTypes field
          updateStreamItemWithComment(o.getId(), o.getViewerType() + "," + ViewerType.COMMENTER.getType(), 1, comment.getUpdated().getTime());
        }
      }
    }
    
  }
  
  private void updateMentioner(Identity poster,
                               ExoSocialActivity activity,
                               ExoSocialActivity comment) {

    String[] mentionIds = processMentions(comment.getTitle());

    for (String mentioner : mentionIds) {
      //
      StreamItem entity = getStreamItem(activity.getId(), mentioner);
      if (entity == null) {
        createStreamItem(activity.getId(),
                         poster.getRemoteId(),
                         activity.getUserId() != null ? activity.getUserId() : poster.getId(),
                         poster.getId(),
                         ViewerType.MENTIONER.getType(),
                         activity.isHidden(),
                         activity.isLocked(),
                         activity.getPostedTime());
      } else {
        // update mention
        updateMention(entity, mentioner, comment);
      }
    }

  }
  
  private void updateMention(StreamItem entity, String mentionId, ExoSocialActivity comment) {
    //
    String mentionType = ViewerType.MENTIONER.getType();
    int actionNum = 0;
    String viewerTypes = null;
    String viewerId = null;
    if (StringUtils.isBlank(entity.getViewerType())) {
      //viewerType = MENTIONER + commenter = 1 + viewerId = mentionId
      actionNum = 1;
      viewerTypes = ViewerType.MENTIONER.getType();
      viewerId = mentionId;
    } else {
      String[] arrViewTypes = entity.getViewerType().split(",");

      if (ArrayUtils.contains(arrViewTypes, mentionType)) {
        // increase number by 1
        actionNum = entity.getMentioner() + 1;
        viewerTypes = entity.getViewerType();
      } else {
        // add new type MENTIONER to arrViewTypes
        arrViewTypes = (String[]) ArrayUtils.add(arrViewTypes, mentionType);
        viewerTypes = StringUtils.join(arrViewTypes, ",");
      }

      // update mentionner
    }
    
    //update time comment.getUpdated().getTime()
    updateStreamItemWithComment(entity.getId(), viewerTypes, viewerId, actionNum, comment.getUpdated().getTime());
  }
  
  /**
   * update stream item's comment info
   */
  private void updateStreamItemWithComment(String id, String viewerTypes, Integer commenterNum, Long time) {
    //insert to mysql stream_item table
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
 
    StringBuilder insertTableSQL = new StringBuilder();
    insertTableSQL.append("update stream_item")
                  .append(" set viewerType = ?, commenter =?, time = ?")
                  .append(" where _id = ?");
    
    try {
      dbConnection = getJNDIConnection();
      preparedStatement = dbConnection.prepareStatement(insertTableSQL.toString());
      preparedStatement.setString(1, viewerTypes);
      preparedStatement.setInt(2, commenterNum);
      preparedStatement.setLong(3, time);
      preparedStatement.setString(4, id);
      
      preparedStatement.executeUpdate();
 
      LOG.debug("stream item updated");
 
    } catch (SQLException e) {
 
      LOG.error("error in stream item update:", e.getMessage());
 
    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
  }
  
  /**
   * update stream item's comment info
   */
  private void updateStreamItemWithComment(String id, String viewerTypes, String viewerId, Integer commenterNum, Long time) {
    //insert to mysql stream_item table
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
 
    StringBuilder insertTableSQL = new StringBuilder();
    insertTableSQL.append("update stream_item")
                  .append(" set viewerType = ?, viewerId = ?, commenter =?, time = ?")
                  .append(" where _id = ?");
    
    try {
      dbConnection = getJNDIConnection();
      preparedStatement = dbConnection.prepareStatement(insertTableSQL.toString());
      preparedStatement.setString(1, viewerTypes);
      preparedStatement.setString(2, viewerId);
      preparedStatement.setInt(3, commenterNum);
      preparedStatement.setLong(4, time);
      preparedStatement.setString(5, id);
      
      preparedStatement.executeUpdate();
 
      LOG.debug("stream item updated");
 
    } catch (SQLException e) {
 
      LOG.error("error in stream item update:", e.getMessage());
 
    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
  }
  
  /**
   * get a stream item by activity, viewer and type
   */
  private StreamItem getStreamItem(String activityId, String viewerId) {
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;

    StringBuilder getActivitySQL = new StringBuilder();
    getActivitySQL.append("select ")
                  .append("_id, activityId, ownerId, posterId, viewerId, viewerType, hidable, lockable, time, mentioner, commenter")
                  .append(" from stream_item where activityId = ? and viewerId = ?");

    StreamItem item = null;

    try {
      dbConnection = getJNDIConnection();
      preparedStatement = dbConnection.prepareStatement(getActivitySQL.toString());
      preparedStatement.setString(1, activityId);
      preparedStatement.setString(2, viewerId);

      rs = preparedStatement.executeQuery();

      while (rs.next()) {
        item = fillStreamItemFromResultSet(rs);
      }

      LOG.debug("stream item found");

      return item;

    } catch (SQLException e) {

      LOG.error("error in stream item look up:", e.getMessage());
      return null;

    } finally {
      try {
        if (rs != null) {
          rs.close();
        }

        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
  }
  
  /**
   * fill in StreamItem object from ResultSet
   */
  private StreamItem fillStreamItemFromResultSet(ResultSet rs) throws SQLException{
    StreamItem item = new StreamItemImpl();
    item.setId(rs.getString("_id"));
    item.setActivityId(rs.getString("activityId"));
    item.setOwnerId(rs.getString("ownerId"));
    item.setPosterId(rs.getString("posterId"));
    item.setViewerId(rs.getString("viewerId"));
    item.setViewerType(rs.getString("viewerType"));
    item.setHidable(rs.getBoolean("hidable"));
    item.setLockable(rs.getBoolean("lockable"));
    item.setTime(rs.getLong("time"));
    item.setMentioner(rs.getInt("mentioner"));
    item.setCommenter(rs.getInt("commenter"));
    return item;
  }
  
	@Override
	public ExoSocialActivity saveActivity(Identity owner,
			ExoSocialActivity activity) throws ActivityStorageException {

    try {
      Validate.notNull(owner, "owner must not be null.");
      Validate.notNull(activity, "activity must not be null.");
      Validate.notNull(activity.getUpdated(), "Activity.getUpdated() must not be null.");
      Validate.notNull(activity.getPostedTime(), "Activity.getPostedTime() must not be null.");
      Validate.notNull(activity.getTitle(), "Activity.getTitle() must not be null.");
    } catch (IllegalArgumentException e) {
      throw new ActivityStorageException(ActivityStorageException.Type.ILLEGAL_ARGUMENTS, e.getMessage(), e);
    }

    try {

      if (activity.getId() == null) {

        String[] mentioners = _createActivity(owner, activity);
        //if (RelationshipPublisher.USER_ACTIVITIES_FOR_RELATIONSHIP.equals(activity.getType()))
        //  identityStorage.updateProfileActivityId(owner, activity.getId(), Profile.AttachedActivityType.RELATIONSHIP);

        //StorageUtils.persist();
        //create refs
        //streamStorage.save(owner, activity);
        /*if (mustInjectStreams) {
          //run synchronous
          StreamInvocationHelper.savePoster(owner, activity);
          //run asynchronous
          StreamInvocationHelper.save(owner, activity, mentioners);
        }*/
      }
      else {
        //TODO to be implemented
        //_saveActivity(activity);
      }

      LOG.debug(String.format(
          "Activity %s by %s (%s) saved",
          activity.getTitle(),
          activity.getUserId(),
          activity.getId()
      ));

      return activity;

    }
    catch (NodeNotFoundException e) {
      throw new ActivityStorageException(ActivityStorageException.Type.FAILED_TO_SAVE_ACTIVITY, e.getMessage(), e);
    } catch (ChromatticException ex) {
      Throwable throwable = ex.getCause();
      if (throwable instanceof ItemExistsException || 
          throwable instanceof InvalidItemStateException) {
        LOG.warn("Probably was inserted activity by another session");
        LOG.debug(ex.getMessage(), ex);
        return activity;
      } else {
        throw new ActivityStorageException(ActivityStorageException.Type.FAILED_TO_SAVE_ACTIVITY, ex.getMessage());
      }
    }
	}

  protected String[] _createActivity(Identity owner, ExoSocialActivity activity) throws NodeNotFoundException {

    LOG.debug("begin to create activity");
    
    IdentityEntity identityEntity = _findById(IdentityEntity.class, owner.getId());

    IdentityEntity posterIdentityEntity = null;
    if (activity.getUserId() != null) {
      posterIdentityEntity = _findById(IdentityEntity.class, activity.getUserId());
    }
    else {
      posterIdentityEntity = identityEntity;
    }

    //insert to mysql activity table
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
 
    StringBuilder insertTableSQL = new StringBuilder();
    insertTableSQL.append("INSERT INTO activity")
                  .append("(_id, title, titleId, body, bodyId, postedTime, lastUpdated, posterId, ownerId,")
                  .append("permaLink, appId, externalId, priority, hidable, lockable, likers, metadata)")
                  .append("VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
    
    try {
      dbConnection = getJNDIConnection();
      preparedStatement = dbConnection.prepareStatement(insertTableSQL.toString());
 
      activity.setId(UUID.randomUUID().toString());
      preparedStatement.setString(1, activity.getId());
      preparedStatement.setString(2, activity.getTitle());
      preparedStatement.setString(3, activity.getTitleId());
      preparedStatement.setString(4, activity.getBody());
      preparedStatement.setString(5, activity.getBodyId());
      preparedStatement.setLong(6, activity.getPostedTime());
      preparedStatement.setLong(7, activity.getUpdated().getTime());
      preparedStatement.setString(8, activity.getUserId() != null ? activity.getUserId() : owner.getId());
      preparedStatement.setString(9, owner.getRemoteId());
      preparedStatement.setString(10, activity.getPermaLink());
      preparedStatement.setString(11, activity.getAppId());
      preparedStatement.setString(12, activity.getExternalId());
      if(activity.getPriority() == null){
        preparedStatement.setNull(13, Types.FLOAT);
      }else{
        preparedStatement.setFloat(13, activity.getPriority());
      }
      preparedStatement.setBoolean(14, activity.isHidden());
      preparedStatement.setBoolean(15, activity.isLocked());
      preparedStatement.setString(16, StringUtils.join(activity.getLikeIdentityIds(),","));
      //TODO add metadata
      preparedStatement.setString(17, null);
      
      preparedStatement.executeUpdate();
 
      LOG.debug("new activity created");
 
    } catch (SQLException e) {
 
      LOG.error("error in activity creation:", e.getMessage());
 
    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
    //end of insertion
    
    // Create activity
    long currentMillis = System.currentTimeMillis();
    long activityMillis = (activity.getPostedTime() != null ? activity.getPostedTime() : currentMillis);

    // Fill activity model
    activity.setStreamOwner(identityEntity.getRemoteId());
    activity.setPostedTime(activityMillis);
    activity.setReplyToId(new String[]{});
    activity.setUpdated(activityMillis);
    
    //records activity for mention case.
    List<String> mentioners = new ArrayList<String>();
    activity.setMentionedIds(processMentions(activity.getMentionedIds(), activity.getTitle(), mentioners, true));
    
    //
    activity.setPosterId(activity.getUserId() != null ? activity.getUserId() : owner.getId());
      
    //
    //fillStream(null, activity);
    newStreamItemForNewActivity(owner, activity);
    
    return null;
  }
  
  private void newStreamItemForNewActivity(Identity poster, ExoSocialActivity activity) {
    //create StreamItem
    if (OrganizationIdentityProvider.NAME.equals(poster.getProviderId())) {
      //poster
      poster(poster, activity);
      //connection
      //connection(poster, activity);
      //mention
      //mention(poster, activity, activity.getMentionedIds());
    } else {
      //for SPACE
      //spaceMembers(poster, activity);
    }
  }
  
  private void poster(Identity poster, ExoSocialActivity activity) {
    createStreamItem(activity.getId(),
                     poster.getRemoteId(),
                     activity.getUserId() != null ? activity.getUserId() : poster.getId(),
                     poster.getId(),
                     ViewerType.POSTER.getType(),
                     activity.isHidden(),
                     activity.isLocked(),
                     activity.getPostedTime());
  }
                                
  private void createStreamItem(String activityId, String ownerId, String posterId, String viewerId, 
                                String viewerType, Boolean hidable, Boolean lockable, Long time){
    //insert to mysql stream_item table
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
 
    StringBuilder insertTableSQL = new StringBuilder();
    insertTableSQL.append("INSERT INTO stream_item")
                  .append("(_id, activityId, ownerId, posterId, viewerId, viewerType,")
                  .append("hidable, lockable, time)")
                  .append("VALUES (?,?,?,?,?,?,?,?,?)");
    
    try {
      dbConnection = getJNDIConnection();
      preparedStatement = dbConnection.prepareStatement(insertTableSQL.toString());
      preparedStatement.setString(1, UUID.randomUUID().toString());
      preparedStatement.setString(2, activityId);
      preparedStatement.setString(3, ownerId);
      preparedStatement.setString(4, posterId);
      preparedStatement.setString(5, viewerId);
      preparedStatement.setString(6, viewerType);
      preparedStatement.setBoolean(7, hidable);
      preparedStatement.setBoolean(8, lockable);
      preparedStatement.setLong(9, time);
      
      preparedStatement.executeUpdate();
 
      LOG.debug("new stream item created");
 
    } catch (SQLException e) {
 
      LOG.error("error in stream item creation:", e.getMessage());
 
    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
    
  }
  
  protected void _saveActivity(ExoSocialActivity activity) throws NodeNotFoundException {

    ActivityEntity activityEntity = _findById(ActivityEntity.class, activity.getId());
    
    //
    //long oldUpdated = activityEntity.getLastUpdated();
    String[] removedLikes = StorageUtils.sub(activityEntity.getLikes(), activity.getLikeIdentityIds());
    String[] addedLikes = StorageUtils.sub(activity.getLikeIdentityIds(), activityEntity.getLikes());
    
    if (removedLikes.length > 0 || addedLikes.length > 0) {
      //process likes activity
      manageActivityLikes(addedLikes, removedLikes, activity);
    }
    //
    fillActivityEntityFromActivity(activity, activityEntity);
  }

  private void fillActivityEntityFromActivity(ExoSocialActivity activity, ActivityEntity activityEntity) {

    activityEntity.setTitle(activity.getTitle());
    activityEntity.setTitleId(activity.getTitleId());
    activityEntity.setBody(activity.getBody());
    activityEntity.setBodyId(activity.getBodyId());
    activityEntity.setLikes(activity.getLikeIdentityIds());
    activityEntity.setType(activity.getType());
    activityEntity.setAppId(activity.getAppId());
    activityEntity.setExternalId(activity.getExternalId());
    activityEntity.setUrl(activity.getUrl());
    activityEntity.setPriority(activity.getPriority());
    activityEntity.setLastUpdated(activity.getUpdated().getTime());
    //
    HidableEntity hidable = _getMixin(activityEntity, HidableEntity.class, true);
    hidable.setHidden(activity.isHidden());
    LockableEntity lockable = _getMixin(activityEntity, LockableEntity.class, true);
    lockable.setLocked(activity.isLocked());
    activityEntity.setMentioners(activity.getMentionedIds());
    activityEntity.setCommenters(activity.getCommentedIds());

    //
    Map<String, String> params = activity.getTemplateParams();
    if (params != null) {
      activityEntity.putParams(params);
    }

    
    //
    fillStream(activityEntity, activity);
    
  }
  
  private void fillStream(ActivityEntity activityEntity, ExoSocialActivity activity) {
    ActivityStream stream = new ActivityStreamImpl();
    
    IdentityEntity identityEntity = null;

    //update new stream owner
    try {
      Identity streamOwnerIdentity = identityStorage.findIdentity(SpaceIdentityProvider.NAME, activity.getStreamOwner());
      if (streamOwnerIdentity == null) {
        streamOwnerIdentity = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, activity.getStreamOwner());
      }
      IdentityEntity streamOwnerEntity = _findById(IdentityEntity.class, streamOwnerIdentity.getId());
      identityEntity = streamOwnerEntity;
      //activityEntity.setIdentity(streamOwnerEntity);
    } catch (Exception e) {
      LOG.debug("Exception in stream owner identification");
      //identityEntity = activityEntity.getIdentity();
    }
    //
    stream.setId(identityEntity.getId());
    stream.setPrettyId(identityEntity.getRemoteId());
    stream.setType(identityEntity.getProviderId());
    
    //Identity identity = identityStorage.findIdentityById(identityEntity.getId());
    if (identityEntity != null && SpaceIdentityProvider.NAME.equals(identityEntity.getProviderId())) {
      Space space = spaceStorage.getSpaceByPrettyName(identityEntity.getRemoteId());
      //work-around for SOC-2366 when rename space's display name.
      if (space != null) {
        String groupId = space.getGroupId().split("/")[2];
        stream.setPermaLink(LinkProvider.getActivityUriForSpace(identityEntity.getRemoteId(), groupId));
      }
    } else {
      stream.setPermaLink(LinkProvider.getActivityUri(identityEntity.getProviderId(), identityEntity.getRemoteId()));
    }
    //
    activity.setActivityStream(stream);
    activity.setStreamId(stream.getId());
    activity.setStreamOwner(stream.getPrettyId());

  }
  
  private void manageActivityLikes(String[] addedLikes, String[] removedLikes, ExoSocialActivity activity) {

    if (addedLikes != null) {
      for (String id : addedLikes) {
        Identity identity = identityStorage.findIdentityById(id);
        //streamStorage.save(identity, activity);
        if (mustInjectStreams) {
          StreamInvocationHelper.like(identity, activity);
        }
      }
    }

    if (removedLikes != null) {
      for (String id : removedLikes) {
        Identity removedLiker = identityStorage.findIdentityById(id);
        if (mustInjectStreams) {
          StreamInvocationHelper.unLike(removedLiker, activity);
        }
      }
    }
  }
  
  /**
   * Processes Mentioners who mention via the Activity.
   * 
   * @param mentionerIds
   * @param title
   * @param isAdded
   * @return list of added IdentityIds who mentioned
   */
  private String[] processMentions(String[] mentionerIds, String title, List<String> addedOrRemovedIds, boolean isAdded) {
    if (title == null || title.length() == 0) {
      return ArrayUtils.EMPTY_STRING_ARRAY;
    }
    
    Matcher matcher = MENTION_PATTERN.matcher(title);
    while (matcher.find()) {
      String remoteId = matcher.group().substring(1);
      if (!USER_NAME_VALIDATOR_REGEX.matcher(remoteId).matches()) {
        continue;
      }
      Identity identity = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, remoteId);
      // if not the right mention then ignore
      if (identity != null) { 
        String mentionStr = identity.getId() + MENTION_CHAR; // identityId@
        mentionerIds = isAdded ? add(mentionerIds, mentionStr, addedOrRemovedIds) : remove(mentionerIds, mentionStr, addedOrRemovedIds);
      }
    }
    return mentionerIds;
  }
  
  private String[] add(String[] mentionerIds, String mentionStr, List<String> addedOrRemovedIds) {
    if (ArrayUtils.toString(mentionerIds).indexOf(mentionStr) == -1) { // the first mention
      addedOrRemovedIds.add(mentionStr.replace(MENTION_CHAR, ""));
      return (String[]) ArrayUtils.add(mentionerIds, mentionStr + 1);
    }
    
    String storedId = null;
    for (String mentionerId : mentionerIds) {
      if (mentionerId.indexOf(mentionStr) != -1) {
        mentionerIds = (String[]) ArrayUtils.removeElement(mentionerIds, mentionerId);
        storedId = mentionStr + (Integer.parseInt(mentionerId.split(MENTION_CHAR)[1]) + 1);
        break;
      }
    }
    

    addedOrRemovedIds.add(mentionStr.replace(MENTION_CHAR, ""));
    mentionerIds = (String[]) ArrayUtils.add(mentionerIds, storedId);
    return mentionerIds;
  }

  private String[] remove(String[] mentionerIds, String mentionStr, List<String> addedOrRemovedIds) {
    for (String mentionerId : mentionerIds) {
      if (mentionerId.indexOf(mentionStr) != -1) {
        int numStored = Integer.parseInt(mentionerId.split(MENTION_CHAR)[1]) - 1;
        
        if (numStored == 0) {
          addedOrRemovedIds.add(mentionStr.replace(MENTION_CHAR, ""));
          return (String[]) ArrayUtils.removeElement(mentionerIds, mentionerId);
        }

        mentionerIds = (String[]) ArrayUtils.removeElement(mentionerIds, mentionerId);
        mentionerIds = (String[]) ArrayUtils.add(mentionerIds, mentionStr + numStored);
        addedOrRemovedIds.add(mentionStr.replace(MENTION_CHAR, ""));
        break;
      }
    }
    return mentionerIds;
  }
  
	@Override
	public ExoSocialActivity getParentActivity(ExoSocialActivity comment)
			throws ActivityStorageException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteActivity(String activityId)
			throws ActivityStorageException {
	  LOG.debug("begin to delete activity");

    // insert to mysql comment table
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;

    StringBuilder sql = new StringBuilder("delete from activity where _id = ?");

    try {
      dbConnection = getJNDIConnection();

      // insert comment
      preparedStatement = dbConnection.prepareStatement(sql.toString());
      preparedStatement.setString(1, activityId);
      preparedStatement.executeUpdate();

      deleteCommentByActivity(activityId);
      deleteStreamItemByActivity(activityId);
      
      LOG.debug("activity deleted");

    } catch (SQLException e) {

      LOG.error("error in activity deletion:", e.getMessage());

    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }

	}

  private void deleteCommentByActivity(String activityId) throws ActivityStorageException {
    LOG.debug("begin to delete comments");

    // insert to mysql comment table
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;

    StringBuilder sql = new StringBuilder("delete from comment where activityId = ?");

    try {
      dbConnection = getJNDIConnection();

      // insert comment
      preparedStatement = dbConnection.prepareStatement(sql.toString());
      preparedStatement.setString(1, activityId);
      preparedStatement.executeUpdate();

      LOG.debug("comments deleted");

    } catch (SQLException e) {

      LOG.error("error in comment deletion:", e.getMessage());

    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }

  }
	 
  private void deleteStreamItemByActivity(String activityId) throws ActivityStorageException {
    LOG.debug("begin to delete stream items");

    // insert to mysql comment table
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;

    StringBuilder sql = new StringBuilder("delete from stream_item where activityId = ?");

    try {
      dbConnection = getJNDIConnection();

      // insert comment
      preparedStatement = dbConnection.prepareStatement(sql.toString());
      preparedStatement.setString(1, activityId);
      preparedStatement.executeUpdate();

      LOG.debug("stream items deleted");

    } catch (SQLException e) {

      LOG.error("error in stream items deletion:", e.getMessage());

    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }

  }
  
	@Override
	public void deleteComment(String activityId, String commentId)
			throws ActivityStorageException {
	  ExoSocialActivity comment = getComment(commentId);
    
    deleteComment(commentId);
    
    String[] mentionIds = processMentions(comment.getTitle());
    //update activities refs for mentioner
    removeMentioner(activityId, mentionIds);
	}

  private void removeMentioner(String activityId, String[] mentionIds) {
    if(ArrayUtils.isEmpty(mentionIds)){
      return;
    }
    
    List<StreamItem> items = getStreamItem(activityId, mentionIds);
    if(CollectionUtils.isEmpty(items)){
      return;
    }
    
    for(StreamItem it:items){
      //update
      if (StringUtils.isNotBlank(it.getViewerType())) {
        String[] viewTypes = it.getViewerType().split(",");
        
        //if MENTIONER is Poster, don't remove stream item
        boolean removeable = ArrayUtils.contains(mentionIds, it.getPosterId()) ? false : true;
        
        if (it.getMentioner() > 0) {
          int number = it.getMentioner() - 1;
          if (number == 0) {
            //remove Mentioner
            String[] newViewTypes = (String[]) ArrayUtils.removeElement(viewTypes, ViewerType.MENTIONER.name());
            if (newViewTypes.length == 0 && removeable) {
              //delete stream item
              deleteStreamItem(it.getId());
            }else{
              //update number + viewType
              updateStreamItemWithComment(it.getId(), StringUtils.join(newViewTypes,","), number, it.getTime());
            }
          } else {
            //update number
            updateStreamItemWithComment(it.getId(), it.getViewerType(), number, it.getTime());
          }
        }
      }
    }
    
  }
  
  private void deleteStreamItem(String id) throws ActivityStorageException {
    LOG.debug("begin to delete stream item");

    // insert to mysql comment table
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;

    StringBuilder sql = new StringBuilder("delete from stream_item where _id = ?");

    try {
      dbConnection = getJNDIConnection();

      // insert comment
      preparedStatement = dbConnection.prepareStatement(sql.toString());
      preparedStatement.setString(1, id);
      preparedStatement.executeUpdate();

      LOG.debug("stream item deleted");

    } catch (SQLException e) {

      LOG.error("error in stream item deletion:", e.getMessage());

    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }

  }
  
  /**
   * get a stream item by activity, viewer and type
   */
  private List<StreamItem> getStreamItem(String activityId, String[] mentionIds) {
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;

    StringBuilder getActivitySQL = new StringBuilder();
    getActivitySQL.append("select ")
                  .append("_id, activityId, ownerId, posterId, viewerId, viewerType, hidable, lockable, time, mentioner, commenter")
                  .append(" from stream_item where activityId = ? and viewerId in (")
                  .append(StringUtils.join(mentionIds, ",")).append(")");

    List<StreamItem> list = new ArrayList<StreamItem>();
    try {
      dbConnection = getJNDIConnection();
      preparedStatement = dbConnection.prepareStatement(getActivitySQL.toString());
      preparedStatement.setString(1, activityId);

      rs = preparedStatement.executeQuery();

      while (rs.next()) {
        StreamItem item = fillStreamItemFromResultSet(rs);
        list.add(item);
      }

      LOG.debug("stream items found");

      return list;

    } catch (SQLException e) {

      LOG.error("error in stream items look up:", e.getMessage());
      return null;

    } finally {
      try {
        if (rs != null) {
          rs.close();
        }

        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
  }
  
  /**
   * Processes Mentioners who has been mentioned via the Activity.
   * 
   * @param title
   */
  private String[] processMentions(String title) {
    String[] mentionerIds = new String[0];
    if (title == null || title.length() == 0) {
      return ArrayUtils.EMPTY_STRING_ARRAY;
    }

    Matcher matcher = MENTION_PATTERN.matcher(title);
    while (matcher.find()) {
      String remoteId = matcher.group().substring(1);
      if (!USER_NAME_VALIDATOR_REGEX.matcher(remoteId).matches()) {
        continue;
      }
      Identity identity = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, remoteId);
      // if not the right mention then ignore
      if (identity != null) {
        mentionerIds = (String[]) ArrayUtils.add(mentionerIds, identity.getId());
      }
    }
    return mentionerIds;
  }

  
	private ExoSocialActivity getComment(String id){
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;

    StringBuilder sql = new StringBuilder();
    sql.append("select ")
                  .append("_id, activityId, title, titleId, body, bodyId, postedTime,")
                  .append("lastUpdated, posterId, ownerId, permaLink, hidable, lockable")
                  .append(" from comment where _id = ?");

    try {
      dbConnection = getJNDIConnection();
      preparedStatement = dbConnection.prepareStatement(sql.toString());
      preparedStatement.setString(1, id);

      rs = preparedStatement.executeQuery();
      ExoSocialActivity comment = null;
      
      while (rs.next()) {
        comment = fillCommentFromResultSet(rs);
      }

      LOG.debug("comment found");

      return comment;

    } catch (SQLException e) {

      LOG.error("error in comment look up:", e.getMessage());
      return null;

    } finally {
      try {
        if (rs != null) {
          rs.close();
        }

        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    } 
	}
	
	private void deleteComment(String id){
	  LOG.debug("begin to delete comment");

    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;

    StringBuilder sql = new StringBuilder("delete from comment where _id = ?");

    try {
      dbConnection = getJNDIConnection();

      // insert comment
      preparedStatement = dbConnection.prepareStatement(sql.toString());
      preparedStatement.setString(1, id);
      preparedStatement.executeUpdate();

      LOG.debug("comment deleted");

    } catch (SQLException e) {

      LOG.error("error in comment deletion:", e.getMessage());

    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
	}
	
	@Override
	public List<ExoSocialActivity> getActivitiesOfIdentities(
			List<Identity> connectionList, long offset, long limit)
			throws ActivityStorageException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getActivitiesOfIdentities(
			List<Identity> connectionList, TimestampType type, long offset,
			long limit) throws ActivityStorageException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfUserActivities(Identity owner)
			throws ActivityStorageException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfUserActivitiesForUpgrade(Identity owner)
			throws ActivityStorageException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfNewerOnUserActivities(Identity ownerIdentity,
			ExoSocialActivity baseActivity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<ExoSocialActivity> getNewerOnUserActivities(
			Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfOlderOnUserActivities(Identity ownerIdentity,
			ExoSocialActivity baseActivity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<ExoSocialActivity> getOlderOnUserActivities(
			Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getActivityFeed(Identity ownerIdentity,
			int offset, int limit) {
	  return getActivityFeedForUpgrade(ownerIdentity, offset, limit);
	}

	@Override
	public List<ExoSocialActivity> getActivityFeedForUpgrade(
			Identity ownerIdentity, int offset, int limit) {
	  
	  Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;
    
    //get spaces where user is member
    List<Space> spaces = spaceStorage.getMemberSpaces(ownerIdentity.getRemoteId());
    String[] spaceIds = new String[0];
    for (Space space : spaces) {
      spaceIds = (String[]) ArrayUtils.add(spaceIds, space.getPrettyName());
    }
    
    List<Identity> relationships = relationshipStorage.getConnections(ownerIdentity);
    
    Set<String> relationshipIds = new HashSet<String>();
    for (Identity identity : relationships) {
      relationshipIds.add(identity.getId());
    }
    
    StringBuilder sql = new StringBuilder();
    sql.append("select ")
       .append("_id, title, titleId, body, bodyId, postedTime, lastUpdated, posterId, ownerId,")
       .append("permaLink, appId, externalId, priority, hidable, lockable, likers, metadata")
       .append(" from activity as a inner join (select distinct activityId from stream_item where ")
       .append(" (viewerId = ? ");
    
    if(CollectionUtils.isNotEmpty(spaces)){
      sql.append("or ownerId in ( ").append(StringUtils.join(spaceIds, ",")).append(") ");
    }
    
    if(CollectionUtils.isNotEmpty(relationships)){
      sql.append("or posterId in ( ").append(StringUtils.join(relationshipIds, ",")).append(") ");
    }
    
    sql.append(")");
    
    long sinceTime = getStorage().getSinceTime(ownerIdentity, offset, ActivityType.FEED);
    if (sinceTime > 0) {
      sql.append(" and time < ?");
    }
    
    sql.append(" order by time desc limit 0,").append(limit).append(") as si on a._id = si.activityId");
    
    List<ExoSocialActivity> result = new LinkedList<ExoSocialActivity>();
    
    try {
      dbConnection = getJNDIConnection();
      preparedStatement = dbConnection.prepareStatement(sql.toString());
      preparedStatement.setString(1, ownerIdentity.getId());
      
      if (sinceTime > 0) {
        preparedStatement.setLong(2, sinceTime);
      }
      
      rs = preparedStatement.executeQuery();
      
      while(rs.next()){
        ExoSocialActivity activity = new ExoSocialActivityImpl();
        fillActivityFromResultSet(rs, activity);
        result.add(activity);
      }
      
      LOG.debug("getActivityFeed size = "+ result.size());
      
      return result;
      
    } catch (SQLException e) {
 
      LOG.error("error in activity look up:", e.getMessage());
      return null;
 
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
	  
	}

	@Override
	public int getNumberOfActivitesOnActivityFeed(Identity ownerIdentity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfActivitesOnActivityFeedForUpgrade(
			Identity ownerIdentity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfNewerOnActivityFeed(Identity ownerIdentity,
			ExoSocialActivity baseActivity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<ExoSocialActivity> getNewerOnActivityFeed(
			Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfOlderOnActivityFeed(Identity ownerIdentity,
			ExoSocialActivity baseActivity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<ExoSocialActivity> getOlderOnActivityFeed(
			Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getActivitiesOfConnections(
			Identity ownerIdentity, int offset, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getActivitiesOfConnectionsForUpgrade(
			Identity ownerIdentity, int offset, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfActivitiesOfConnections(Identity ownerIdentity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfActivitiesOfConnectionsForUpgrade(
			Identity ownerIdentity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<ExoSocialActivity> getActivitiesOfIdentity(
			Identity ownerIdentity, long offset, long limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfNewerOnActivitiesOfConnections(
			Identity ownerIdentity, ExoSocialActivity baseActivity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<ExoSocialActivity> getNewerOnActivitiesOfConnections(
			Identity ownerIdentity, ExoSocialActivity baseActivity, long limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfOlderOnActivitiesOfConnections(
			Identity ownerIdentity, ExoSocialActivity baseActivity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<ExoSocialActivity> getOlderOnActivitiesOfConnections(
			Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getUserSpacesActivities(
			Identity ownerIdentity, int offset, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getUserSpacesActivitiesForUpgrade(
			Identity ownerIdentity, int offset, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfUserSpacesActivities(Identity ownerIdentity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfUserSpacesActivitiesForUpgrade(Identity ownerIdentity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity,
			ExoSocialActivity baseActivity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<ExoSocialActivity> getNewerOnUserSpacesActivities(
			Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity,
			ExoSocialActivity baseActivity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<ExoSocialActivity> getOlderOnUserSpacesActivities(
			Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getComments(
			ExoSocialActivity existingActivity, int offset, int limit) {
	  
	  Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;

    StringBuilder sql = new StringBuilder();
    sql.append("select ")
                  .append("_id, activityId, title, titleId, body, bodyId, postedTime,")
                  .append("lastUpdated, posterId, ownerId, permaLink, hidable, lockable")
                  .append(" from comment where activityId = ?");

    try {
      dbConnection = getJNDIConnection();
      preparedStatement = dbConnection.prepareStatement(sql.toString());
      preparedStatement.setString(1, existingActivity.getId());

      rs = preparedStatement.executeQuery();

      List<ExoSocialActivity> result = new ArrayList<ExoSocialActivity>();
      while (rs.next()) {
        ExoSocialActivity comment = fillCommentFromResultSet(rs);
        processActivity(comment);
        result.add(comment);
      }

      LOG.debug("comments found");

      return result;

    } catch (SQLException e) {

      LOG.error("error in comments look up:", e.getMessage());
      return null;

    } finally {
      try {
        if (rs != null) {
          rs.close();
        }

        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
    
	}

  private ExoSocialActivity fillCommentFromResultSet(ResultSet rs) throws SQLException{
    ExoSocialActivity comment = new ExoSocialActivityImpl();
    
    comment.setId(rs.getString("_id"));
    comment.setTitle(rs.getString("title"));
    comment.setTitleId(rs.getString("titleId"));
    comment.setBody(rs.getString("body"));
    comment.setBodyId(rs.getString("bodyId"));
    comment.setUserId(rs.getString("posterId"));
    comment.setPostedTime(rs.getLong("postedTime"));
    comment.setUpdated(rs.getLong("lastUpdated"));
    comment.setPosterId(rs.getString("posterId"));

    comment.isLocked(rs.getBoolean("lockable"));
    comment.isHidden(rs.getBoolean("hidable"));

    comment.setStreamOwner(rs.getString("ownerId"));
    comment.isComment(true);
    return comment;
  }
  
	@Override
	public int getNumberOfComments(ExoSocialActivity existingActivity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfNewerComments(ExoSocialActivity existingActivity,
			ExoSocialActivity baseComment) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<ExoSocialActivity> getNewerComments(
			ExoSocialActivity existingActivity, ExoSocialActivity baseComment,
			int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfOlderComments(ExoSocialActivity existingActivity,
			ExoSocialActivity baseComment) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<ExoSocialActivity> getOlderComments(
			ExoSocialActivity existingActivity, ExoSocialActivity baseComment,
			int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SortedSet<ActivityProcessor> getActivityProcessors() {
	  return activityProcessors;
	}

	@Override
	public void updateActivity(ExoSocialActivity existingActivity)
			throws ActivityStorageException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getNumberOfNewerOnActivityFeed(Identity ownerIdentity,
			Long sinceTime) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfNewerOnUserActivities(Identity ownerIdentity,
			Long sinceTime) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfNewerOnActivitiesOfConnections(
			Identity ownerIdentity, Long sinceTime) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity,
			Long sinceTime) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<ExoSocialActivity> getActivitiesOfIdentities(
			ActivityBuilderWhere where, ActivityFilter filter, long offset,
			long limit) throws ActivityStorageException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfSpaceActivities(Identity spaceIdentity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfSpaceActivitiesForUpgrade(Identity spaceIdentity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<ExoSocialActivity> getSpaceActivities(Identity spaceIdentity,
			int index, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getSpaceActivitiesForUpgrade(
			Identity spaceIdentity, int index, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getActivitiesByPoster(
			Identity posterIdentity, int offset, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getActivitiesByPoster(
			Identity posterIdentity, int offset, int limit,
			String... activityTypes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfActivitiesByPoster(Identity posterIdentity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfActivitiesByPoster(Identity ownerIdentity,
			Identity viewerIdentity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<ExoSocialActivity> getNewerOnSpaceActivities(
			Identity spaceIdentity, ExoSocialActivity baseActivity, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfNewerOnSpaceActivities(Identity spaceIdentity,
			ExoSocialActivity baseActivity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<ExoSocialActivity> getOlderOnSpaceActivities(
			Identity spaceIdentity, ExoSocialActivity baseActivity, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfOlderOnSpaceActivities(Identity spaceIdentity,
			ExoSocialActivity baseActivity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfNewerOnSpaceActivities(Identity spaceIdentity,
			Long sinceTime) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfUpdatedOnActivityFeed(Identity owner,
			ActivityUpdateFilter filter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfUpdatedOnUserActivities(Identity owner,
			ActivityUpdateFilter filter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfUpdatedOnActivitiesOfConnections(Identity owner,
			ActivityUpdateFilter filter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfUpdatedOnUserSpacesActivities(Identity owner,
			ActivityUpdateFilter filter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfUpdatedOnSpaceActivities(Identity owner,
			ActivityUpdateFilter filter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfMultiUpdated(Identity owner,
			Map<String, Long> sinceTimes) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<ExoSocialActivity> getNewerFeedActivities(Identity owner,
			Long sinceTime, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getNewerUserActivities(Identity owner,
			Long sinceTime, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getNewerUserSpacesActivities(Identity owner,
			Long sinceTime, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getNewerActivitiesOfConnections(
			Identity owner, Long sinceTime, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getNewerSpaceActivities(Identity owner,
			Long sinceTime, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getOlderFeedActivities(Identity owner,
			Long sinceTime, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getOlderUserActivities(Identity owner,
			Long sinceTime, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getOlderUserSpacesActivities(Identity owner,
			Long sinceTime, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getOlderActivitiesOfConnections(
			Identity owner, Long sinceTime, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getOlderSpaceActivities(Identity owner,
			Long sinceTime, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfOlderOnActivityFeed(Identity ownerIdentity,
			Long sinceTime) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfOlderOnUserActivities(Identity ownerIdentity,
			Long sinceTime) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfOlderOnActivitiesOfConnections(
			Identity ownerIdentity, Long sinceTime) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity,
			Long sinceTime) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfOlderOnSpaceActivities(Identity ownerIdentity,
			Long sinceTime) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<ExoSocialActivity> getNewerComments(
			ExoSocialActivity existingActivity, Long sinceTime, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getOlderComments(
			ExoSocialActivity existingActivity, Long sinceTime, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfNewerComments(ExoSocialActivity existingActivity,
			Long sinceTime) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfOlderComments(ExoSocialActivity existingActivity,
			Long sinceTime) {
		// TODO Auto-generated method stub
		return 0;
	}

  private ActivityStorage getStorage() {
    if (activityStorage == null) {
      activityStorage = (ActivityStorage) PortalContainer.getInstance().getComponentInstanceOfType(ActivityStorage.class);
    }
    
    return activityStorage;
  }
  
  public void setStorage(final ActivityStorage storage) {
    this.activityStorage = storage;
  }
  
  private static Comparator<ActivityProcessor> processorComparator() {
    return new Comparator<ActivityProcessor>() {

      public int compare(ActivityProcessor p1, ActivityProcessor p2) {
        if (p1 == null || p2 == null) {
          throw new IllegalArgumentException("Cannot compare null ActivityProcessor");
        }
        return p1.getPriority() - p2.getPriority();
      }
    };
  }
  
}
