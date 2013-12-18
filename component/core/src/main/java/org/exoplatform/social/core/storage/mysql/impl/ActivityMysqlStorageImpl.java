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
      //ActivityEntity activityEntity = _findById(ActivityEntity.class, activityId);
	    Connection dbConnection = null;
	    PreparedStatement preparedStatement = null;
	    ResultSet rs = null;
	    
	    String getActivitySQL = "select "
	    +"_id, title, titleId, body, bodyId, postedTime, lastUpdated, posterId, ownerId,"
	    +"permaLink, appId, externalId, priority, hidable, lockable, likers, metadata"
	    +" from activity where _id = ?";
	    
	    ExoSocialActivity activity = new ExoSocialActivityImpl();
	    
	    try {
	      dbConnection = getDBConnection();
	      preparedStatement = dbConnection.prepareStatement(getActivitySQL);
	      preparedStatement.setString(1, activityId);
	      
	      rs = preparedStatement.executeQuery();
	      
	      while(rs.next()){
	        fillActivityFromResultSet(rs, activity);
	      }
	      
	      processActivity(activity);


        LOG.debug("activity found");
        
	      return activity;
	      
	    } catch (SQLException e) {
	 
	      LOG.debug("error in activity look up:", e.getMessage());
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
	        LOG.debug("Cannot close statement or connection:", e.getMessage());
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
	public void saveComment(ExoSocialActivity activity,
			ExoSocialActivity comment) throws ActivityStorageException {
		// TODO Auto-generated method stub

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
 
    String insertTableSQL = "INSERT INTO activity"
    +"(_id, title, titleId, body, bodyId, postedTime, lastUpdated, posterId, ownerId,"
    +"permaLink, appId, externalId, priority, hidable, lockable, likers, metadata)"
    +"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    
    try {
      dbConnection = getDBConnection();
      preparedStatement = dbConnection.prepareStatement(insertTableSQL);
 
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
 
      LOG.debug("error in activity creation:", e.getMessage());
 
    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.debug("Cannot close statement or connection:", e.getMessage());
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
  
  private void poster(Identity poster, ExoSocialActivity activity){
    //insert to mysql stream_item table
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
 
    String insertTableSQL = "INSERT INTO stream_item"
    +"(_id, activityId, ownerId, posterId, viewerId, viewerType,"
    +"hidable, lockable, time)"
    +"VALUES (?,?,?,?,?,?,?,?,?)";
    
    try {
      dbConnection = getDBConnection();
      preparedStatement = dbConnection.prepareStatement(insertTableSQL);
      preparedStatement.setString(1, UUID.randomUUID().toString());
      preparedStatement.setString(2, activity.getId());
      preparedStatement.setString(3, poster.getRemoteId());
      preparedStatement.setString(4, activity.getUserId() != null ? activity.getUserId() : poster.getId());
      preparedStatement.setString(5, poster.getId());
      preparedStatement.setString(6, "POSTER");
      preparedStatement.setBoolean(7, activity.isHidden());
      preparedStatement.setBoolean(8, activity.isLocked());
      preparedStatement.setLong(9, activity.getPostedTime());
      
      preparedStatement.executeUpdate();
 
      LOG.debug("new stream item created");
 
    } catch (SQLException e) {
 
      LOG.debug("error in stream item creation:", e.getMessage());
 
    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.debug("Cannot close statement or connection:", e.getMessage());
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
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteComment(String activityId, String commentId)
			throws ActivityStorageException {
		// TODO Auto-generated method stub

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
       .append(" from activity where _id in (select distinct activityId from stream_item where ")
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
    
    sql.append(" order by time desc)");
    
    List<ExoSocialActivity> result = new LinkedList<ExoSocialActivity>();
    
    try {
      dbConnection = getDBConnection();
      preparedStatement = dbConnection.prepareStatement(sql.toString());
      preparedStatement.setString(1, ownerIdentity.getId());
      
      if (sinceTime > 0) {
        preparedStatement.setLong(2, sinceTime);
      }
      
      rs = preparedStatement.executeQuery();
      
      while(rs.next() && result.size() < limit){
        ExoSocialActivity activity = new ExoSocialActivityImpl();
        fillActivityFromResultSet(rs, activity);
        result.add(activity);
      }
      
      LOG.debug("getActivityFeed size = "+ result.size());
      
      return result;
      
    } catch (SQLException e) {
 
      LOG.debug("error in activity look up:", e.getMessage());
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
        LOG.debug("Cannot close statement or connection:", e.getMessage());
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
    /*DBCollection activityColl = CollectionName.COMMENT_COLLECTION.getCollection(this);
    BasicDBObject query = new BasicDBObject(CommentMongoEntity.activityId.getName(), existingActivity.getId());

    DBCursor cur = activityColl.find(query);*/
    List<ExoSocialActivity> result = new ArrayList<ExoSocialActivity>();
    /*while (cur.hasNext()) {
      BasicDBObject entity = (BasicDBObject) cur.next();
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      fillActivity(activity, entity);
      activity.isComment(true);
      
      processActivity(activity);
      result.add(activity);
    }*/
    LOG.debug("=======>getComments SIZE ="+ result.size());
    
    return result;
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
