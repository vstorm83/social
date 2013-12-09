package org.exoplatform.social.core.storage.mongodb;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.Validate;
import org.bson.types.BasicBSONList;
import org.bson.types.ObjectId;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.ActivityProcessor;
import org.exoplatform.social.core.activity.filter.ActivityFilter;
import org.exoplatform.social.core.activity.filter.ActivityUpdateFilter;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.chromattic.entity.ActivityEntity;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.mongo.entity.ActivityMongoEntity;
import org.exoplatform.social.core.mongo.entity.ActivityRefMongoEntity;
import org.exoplatform.social.core.mongo.entity.ActivityRefMongoEntity.ViewerType;
import org.exoplatform.social.core.mongo.entity.CommentMongoEntity;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.relationship.model.Relationship.Type;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.ActivityStorageException;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.api.RelationshipStorage;
import org.exoplatform.social.core.storage.api.SpaceStorage;
import org.exoplatform.social.core.storage.cache.model.key.ActivityType;
import org.exoplatform.social.core.storage.exception.NodeNotFoundException;
import org.exoplatform.social.core.storage.impl.ActivityBuilderWhere;
import org.exoplatform.social.core.storage.impl.StorageUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

public class ActivityMongoStorageImpl extends AbstractMongoStorage implements ActivityStorage {
  
  /**
   * Defines the collection name with tenant name
   * @author thanhvc
   *
   */
  enum CollectionName {
    
    ACTIVITY_COLLECTION("activity") {
      @Override
      protected void ensureIndex(AbstractMongoStorage mongoStorage, DBCollection got) {
        got.ensureIndex(new BasicDBObject(ActivityMongoEntity.postedTime.getName(), -1).append(ActivityRefMongoEntity.activityId.getName(), 1)
                        .append(ActivityMongoEntity.poster.getName(), 1));
      }
    },
    COMMENT_COLLECTION("comment") {
      @Override
      protected void ensureIndex(AbstractMongoStorage mongoStorage, DBCollection got) {
        got.ensureIndex(new BasicDBObject(CommentMongoEntity.postedTime.getName(), -1).append(ActivityRefMongoEntity.activityId.getName(), 1));
      }
    },
    STREAM_ITEM_COLLECTION("streamItem") {
      @Override
      protected void ensureIndex(AbstractMongoStorage mongoStorage, DBCollection got) {
        got.ensureIndex(new BasicDBObject(ActivityRefMongoEntity.time.getName(), -1).append(ActivityRefMongoEntity.viewerId.getName(), 1));
      }
    };
    
    private final String collectionName;
    
    private CollectionName(String name) {
      this.collectionName = name;
    }
    
    private static String getRepositoryName() throws RepositoryException, RepositoryConfigurationException {
      RepositoryService service = (RepositoryService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(RepositoryService.class);
      
      String repositoryName = service.getCurrentRepository().getConfiguration().getName();
      if (repositoryName == null || repositoryName.length() <= 0) {
        repositoryName = service.getDefaultRepository().getConfiguration().getName();
      }
      return repositoryName;
    }
    
    public DBCollection getCollection(AbstractMongoStorage mongoStorage) {
      String name = collectionName();
      Set<String> names = mongoStorage.getCollections();
      boolean isExistingCollection = names.contains(name);
      DBCollection got = mongoStorage.getCollection(name);
      //
      if (isExistingCollection == false) {
        ensureIndex(mongoStorage, got);
      }
      return got;
    }
    
    protected abstract void ensureIndex(AbstractMongoStorage mongoStorage, DBCollection got);
    
    
    /**
     * Gets the collection name with tenant name
     * @return
     */
    private String collectionName() {
      try {
        return getRepositoryName() + "." + this.collectionName;
      } catch(RepositoryException e) {
        throw new RuntimeException();
      } catch(RepositoryConfigurationException e) {
        throw new RuntimeException();
      }
    }
  }
  /**
   * Defines Stream Type for each item
   * @author thanhvc
   *
   */
  enum StreamViewType {
    LIKER() {
      @Override
      protected BasicDBObject append(BasicDBObject entity) {
        entity.append(ActivityRefMongoEntity.viewerTypes.getName(), ViewerType.LIKER.name());
        return entity;
      }
    },
    CONNECTION() {
      @Override
      protected BasicDBObject append(BasicDBObject entity) {
        entity.append(ActivityRefMongoEntity.viewerTypes.getName(), ViewerType.CONNECTION.name());
        return entity;
      }
    },
    COMMENTER() {
      @Override
      protected BasicDBObject append(BasicDBObject entity) {
        entity.append(ActivityRefMongoEntity.viewerTypes.getName(), ViewerType.COMMENTER.name());
        return entity;
      }
    },
    MENTIONER() {
      @Override
      protected BasicDBObject append(BasicDBObject entity) {
        entity.append(ActivityRefMongoEntity.viewerTypes.getName(), ViewerType.MENTIONER.name());
        return entity;
      }
    },
    POSTER() {
      @Override
      protected BasicDBObject append(BasicDBObject entity) {
        entity.append(ActivityRefMongoEntity.viewerTypes.getName(), ViewerType.POSTER.name());
        return entity;
      }
    };
    
    private StreamViewType() {
    }
    
    protected abstract BasicDBObject append(BasicDBObject entity);
  }
  
  /** .. */
  private static final Log LOG = ExoLogger.getLogger(ActivityMongoStorageImpl.class);
  /** .. */
  private static final Pattern MENTION_PATTERN = Pattern.compile("@([^\\s]+)|@([^\\s]+)$");
  /** .. */
  public static final Pattern USER_NAME_VALIDATOR_REGEX = Pattern.compile("^[\\p{L}][\\p{L}._\\-\\d]+$");
  /** .. */
  private ActivityStorage activityStorage;

  private final SortedSet<ActivityProcessor> activityProcessors;

  private final RelationshipStorage relationshipStorage;
  private final IdentityStorage identityStorage;
  private final SpaceStorage spaceStorage;
  //sets value to tell this storage to inject Streams or not

  public ActivityMongoStorageImpl(
      final RelationshipStorage relationshipStorage,
      final IdentityStorage identityStorage,
      final SpaceStorage spaceStorage,
      final MongoStorage mongoStorage) {
    
    super(mongoStorage);
    this.relationshipStorage = relationshipStorage;
    this.identityStorage = identityStorage;
    this.spaceStorage = spaceStorage;
    this.activityProcessors = new TreeSet<ActivityProcessor>(processorComparator());
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

	@Override
	public void setInjectStreams(boolean mustInject) {
		
	}

	@Override
  public ExoSocialActivity getActivity(String activityId) throws ActivityStorageException {
	  //
    DBCollection collection = CollectionName.ACTIVITY_COLLECTION.getCollection(this);
    BasicDBObject query = new BasicDBObject();
    query.append("_id", new ObjectId(activityId));
    
    BasicDBObject entity = (BasicDBObject) collection.findOne(query);
    
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    
    fillActivity(activity, entity);
    processActivity(activity);
    
    return activity;
  }

  @Override
  public List<ExoSocialActivity> getUserActivities(Identity owner) throws ActivityStorageException {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getUserActivities(Identity owner, long offset, long limit) throws ActivityStorageException {
    return getUserActivitiesForUpgrade(owner, offset, limit);
  }

  @Override
  public List<ExoSocialActivity> getUserActivitiesForUpgrade(Identity owner, long offset, long limit) throws ActivityStorageException {
    //
    DBCollection streamCol = CollectionName.STREAM_ITEM_COLLECTION.getCollection(this);
    
    BasicDBObject query = new BasicDBObject();
    
    //look for by view types
    String[] viewTypes = new String[]{ViewerType.COMMENTER.name(), ViewerType.LIKER.name(), ViewerType.MENTIONER.name(), ViewerType.POSTER.name()};
    BasicDBObject viewer = new BasicDBObject(ActivityRefMongoEntity.viewerId.getName(), owner.getId());
    viewer.append(ActivityRefMongoEntity.viewerTypes.getName(), new BasicDBObject("$in", viewTypes));
    
    //case user post in space
    BasicDBObject poster = new BasicDBObject(ActivityRefMongoEntity.poster.getName(), owner.getId());
    poster.append(ActivityRefMongoEntity.viewerTypes.getName(), new BasicDBObject("$exists", false));
    
    //
    query.append("$or", new BasicDBObject[]{poster, viewer});
    
    //get since time
    long sinceTime = getStorage().getSinceTime(owner, offset, ActivityType.USER);
    if (sinceTime > 0) {
      query.append(ActivityRefMongoEntity.time.getName(), new BasicDBObject("$lt", sinceTime));
    }
    //order
    BasicDBObject sortObj = new BasicDBObject(ActivityRefMongoEntity.time.getName(), -1);

    DBCursor cur = streamCol.find(query).sort(sortObj).limit((int)limit);
    List<ExoSocialActivity> result = new LinkedList<ExoSocialActivity>();
    while (cur.hasNext()) {
      BasicDBObject row = (BasicDBObject) cur.next();
      String activityId = row.getString(ActivityRefMongoEntity.activityId.getName());
      ExoSocialActivity activity = getStorage().getActivity(activityId);
      result.add(activity);
    }
    LOG.debug("getUserActivities size = "+ result.size());
    
    return result;
  }

  @Override
  public List<ExoSocialActivity> getActivities(Identity owner,
                                               Identity viewer,
                                               long offset,
                                               long limit) throws ActivityStorageException {
    
    //
    DBCollection connectionColl = CollectionName.STREAM_ITEM_COLLECTION.getCollection(this);
    
    String[] identityIds = getIdentities(owner, viewer);
    
    String[] viewTypes = new String[]{ViewerType.COMMENTER.name(), ViewerType.LIKER.name(), ViewerType.MENTIONER.name(), ViewerType.POSTER.name()};
    BasicDBObject query = new BasicDBObject(ActivityRefMongoEntity.viewerId.getName(), new BasicDBObject("$in", identityIds));
    query.append(ActivityRefMongoEntity.viewerTypes.getName(), new BasicDBObject("$in", viewTypes));
    
    //get since time
    long sinceTime = getStorage().getSinceTime(owner, offset, ActivityType.VIEWER);
    if (sinceTime > 0) {
      query.append(ActivityRefMongoEntity.time.getName(), new BasicDBObject("$lt", sinceTime));
    }
    
    BasicDBObject sortObj = new BasicDBObject("time", -1);

    DBCursor cur = connectionColl.find(query).sort(sortObj).limit((int)limit);
    List<ExoSocialActivity> result = new LinkedList<ExoSocialActivity>();
    while (cur.hasNext()) {
      BasicDBObject row = (BasicDBObject) cur.next();
      String activityId = row.getString(ActivityRefMongoEntity.activityId.getName());
      ExoSocialActivity activity = getStorage().getActivity(activityId);
      result.add(activity);
    }
    LOG.debug("getActivities size = "+ result.size());
    
    return result;
  }
  
  private String[] getIdentities(Identity owner, Identity viewer) {
    List<String> posterIdentities = new ArrayList<String>();
    posterIdentities.add(owner.getId());
    
    //
    if (viewer != null && owner.getId().equals(viewer.getId()) == false) {
      //
      Relationship rel = relationshipStorage.getRelationship(owner, viewer);
      
      //
      boolean hasRelationship = false;
      if (rel != null && rel.getStatus() == Type.CONFIRMED) {
        hasRelationship = true;
      }
      
      //
      if (hasRelationship) {
        posterIdentities.add(viewer.getId());
      }
    }
    
    //
    return posterIdentities.toArray(new String[0]);
  }


	@Override
  public void saveComment(ExoSocialActivity activity, ExoSocialActivity comment) throws ActivityStorageException {
		
	  try {
	    //
	    DBCollection commentColl = CollectionName.COMMENT_COLLECTION.getCollection(this);
	    DBCollection activityCol = CollectionName.ACTIVITY_COLLECTION.getCollection(this);
	    
      //
      long currentMillis = System.currentTimeMillis();
      long commentMillis = (comment.getPostedTime() != null ? comment.getPostedTime() : currentMillis);
      //
      BasicDBObject commentEntity = new BasicDBObject();
      commentEntity.append(CommentMongoEntity.activityId.getName(), activity.getId());
      commentEntity.append(CommentMongoEntity.title.getName(), comment.getTitle());
      commentEntity.append(CommentMongoEntity.titleId.getName(), comment.getTitleId());
      commentEntity.append(CommentMongoEntity.body.getName(), comment.getBody());
      commentEntity.append(CommentMongoEntity.bodyId.getName(), comment.getBodyId());
      commentEntity.append(CommentMongoEntity.poster.getName(), comment.getUserId());
      commentEntity.append(CommentMongoEntity.postedTime.getName(), commentMillis);
      commentEntity.append(CommentMongoEntity.lastUpdated.getName(), commentMillis);
      commentEntity.append(CommentMongoEntity.hidable.getName(), comment.isHidden());
      commentEntity.append(CommentMongoEntity.lockable.getName(), comment.isLocked());
      
      commentColl.insert(commentEntity);
      
      comment.setId(commentEntity.getString("_id") != null ? commentEntity.getString("_id") : null);
      comment.setUpdated(commentMillis);
      
      //update activity
      BasicDBObject update = new BasicDBObject("$set", new BasicDBObject(ActivityMongoEntity.lastUpdated.getName(), commentMillis));
      activityCol.update(new BasicDBObject("_id", new ObjectId(activity.getId())), update);
      
      //TODO: Remove this when apply process mention
      updateActivityRef(activity.getId(), commentMillis);
      
      //Identity poster = new Identity(activity.getPosterId());
      //poster.setRemoteId(activity.getStreamOwner());
      //
      //List<String> mentionIds = new ArrayList<String>();
      //processMentions(activity.getMentionedIds(), comment.getTitle(), mentionIds, true);
      
      //
      //mention(poster, activity, mentionIds.toArray(new String[0]));
      
    } catch (MongoException ex) {
      throw new ActivityStorageException(ActivityStorageException.Type.FAILED_TO_SAVE_COMMENT, ex.getMessage());
    }
    //
    LOG.debug(String.format(
        "Comment %s by %s (%s) created",
        comment.getTitle(),
        comment.getUserId(),
        comment.getId()
    ));
    
	}
	
	private void updateMentionerOnStreamIteam(Identity poster, ExoSocialActivity activity, ExoSocialActivity comment) {
	  
	  try {
      //
      List<String> mentionIds = new ArrayList<String>();
      processMentions(activity.getMentionedIds(), comment.getTitle(), mentionIds, true);
      
      DBCollection streamCol = CollectionName.STREAM_ITEM_COLLECTION.getCollection(this);
      //
      for (String mentioner : mentionIds) {
        //skip by poster
        if (mentioner.equals(activity.getUserId())) continue;
        
        BasicDBObject query = new BasicDBObject(ActivityRefMongoEntity.activityId.getName(), activity.getId());
        
        BasicDBObject[] viewerOrPoster = new BasicDBObject[] {
            new BasicDBObject(ActivityRefMongoEntity.viewerId.getName(), mentioner),
            new BasicDBObject(ActivityRefMongoEntity.poster.getName(), mentioner) };
        
        query.append("$or", viewerOrPoster);
        
        BasicDBObject entity = (BasicDBObject) streamCol.findOne(query);
        //
        if (entity == null) {
          //create new stream item
          entity = StreamViewType.MENTIONER.append(new BasicDBObject());
          //
          fillStreamItem(poster, activity, entity);
          entity.append(ActivityRefMongoEntity.viewerId.getName(), mentioner);
          entity.append(ActivityRefMongoEntity.time.getName(), comment.getUpdated().getTime());
          streamCol.insert(entity);
        } else {
          //update
          BasicBSONList viewTypes = (BasicBSONList) entity.get(ActivityRefMongoEntity.viewerTypes.getName());
          if (viewTypes == null) {
            ArrayUtils.contains(viewTypes.toArray(new String[0]), ActivityRefMongoEntity.ViewerType.MENTIONER.name());
          }
          
        }
        
        fillStreamItem(poster, activity, entity);
        entity = StreamViewType.MENTIONER.append(entity);
        //mentioner = identityId@{n}
        entity.append(ActivityRefMongoEntity.viewerId.getName(), mentioner);
        entity.append(ActivityRefMongoEntity.time.getName(), activity.getPostedTime());
        streamCol.insert(entity);
      }
      
    } catch (MongoException e) {
      LOG.warn("Update mentioner on StreamItem failed. ", e);
    }
    
	}
	
	private void updateActivityRef(String activityId, long time) {
	  DBCollection activityColl = CollectionName.ACTIVITY_COLLECTION.getCollection(this);
	  //
    BasicDBObject update = new BasicDBObject();
    BasicDBObject set = new BasicDBObject(ActivityMongoEntity.lastUpdated.getName(), time);
    //
    update.append("$set", set);
    BasicDBObject query = new BasicDBObject(ActivityMongoEntity.id.getName(), new ObjectId(activityId));
    
    WriteResult result = activityColl.update(query, update);
    LOG.debug("UPDATED TIME ACTIVITY: " + result.toString());
    //update refs
    DBCollection streamCol = CollectionName.STREAM_ITEM_COLLECTION.getCollection(this);
    query = new BasicDBObject(ActivityRefMongoEntity.activityId.getName(), activityId);
    set = new BasicDBObject(ActivityRefMongoEntity.time.getName(), time);
    update = new BasicDBObject("$set", set);
    result = streamCol.updateMulti(query, update);
    LOG.debug("UPDATED ACTIVITY Reference: " + result.toString());
	}
	
	@Override
	public ExoSocialActivity saveActivity(Identity owner, ExoSocialActivity activity) throws ActivityStorageException {
	  try {
      Validate.notNull(owner, "owner must not be null.");
      Validate.notNull(activity, "activity must not be null.");
      Validate.notNull(activity.getUpdated(), "Activity.getUpdated() must not be null.");
      Validate.notNull(activity.getPostedTime(), "Activity.getPostedTime() must not be null.");
      Validate.notNull(activity.getTitle(), "Activity.getTitle() must not be null.");
    } catch (IllegalArgumentException e) {
      throw new ActivityStorageException(ActivityStorageException.Type.ILLEGAL_ARGUMENTS, e.getMessage(), e);
    }
	  //
	  try {
      _createActivity(owner, activity);
    } catch (MongoException e) {
      LOG.warn("Insert activity failed.", e);
    } catch (NodeNotFoundException e) {
      LOG.warn("Insert activity failed.", e);
    }
	  
    return activity;
	}
	
	protected void _saveActivity(ExoSocialActivity activity) throws NodeNotFoundException {

    ActivityEntity activityEntity = _findById(ActivityEntity.class, activity.getId());
    
    //
    String[] removedLikes = StorageUtils.sub(activityEntity.getLikes(), activity.getLikeIdentityIds());
    String[] addedLikes = StorageUtils.sub(activity.getLikeIdentityIds(), activityEntity.getLikes());
    
    //TODO LIKER case
    if (removedLikes.length > 0 || addedLikes.length > 0) {
      //manageActivityLikes(addedLikes, removedLikes, activity);
    }
    //
    //fillActivityEntityFromActivity(activity, activityEntity);
  }
	
	/*
   * Private
   */
  private void fillActivityEntityFromActivity(Identity owner, ExoSocialActivity activity, BasicDBObject activityEntity, boolean isNew) {
    
    long currentMillis = System.currentTimeMillis();
    long activityMillis = (activity.getPostedTime() != null ? activity.getPostedTime() : currentMillis);

    if (isNew) {
      // Create activity
      activityEntity.append(ActivityMongoEntity.postedTime.getName(), activityMillis);
      activityEntity.append(ActivityMongoEntity.lastUpdated.getName(), activityMillis);
      //
      activityEntity.append(ActivityMongoEntity.titleId.getName(), activity.getTitleId());
      activityEntity.append(ActivityMongoEntity.bodyId.getName(), activity.getBodyId());

      // Fill activity model
      activity.setStreamOwner(owner.getRemoteId());
      activity.setPostedTime(activityMillis);
      activity.setReplyToId(new String[]{});
      //poster
      String posterId = activity.getUserId() != null ? activity.getUserId() : owner.getId();
      activityEntity.append(ActivityMongoEntity.poster.getName(), posterId);
      activityEntity.append(ActivityMongoEntity.owner.getName(), owner.getRemoteId());
      activity.setPosterId(posterId);
    }

    if (activity.getTitle() != null) {
      activityEntity.append(ActivityMongoEntity.title.getName(), activity.getTitle());
    }

    if (activity.getBody() != null) {
      activityEntity.append(ActivityMongoEntity.body.getName() , activity.getBody());
    }

    activityEntity.append(ActivityMongoEntity.permaLink.getName(),  activity.getPermaLink());
    //mentioners
    List<String> mentioners = new ArrayList<String>();
    activity.setMentionedIds(processMentions(activity.getMentionedIds(), activity.getTitle(), mentioners, true));
    activityEntity.append(ActivityMongoEntity.mentioners.getName(), activity.getMentionedIds());

    activityEntity.append(ActivityMongoEntity.likers.getName(), activity.getLikeIdentityIds());

    activityEntity.append(ActivityMongoEntity.hidable.getName(),  activity.isHidden());
    activityEntity.append(ActivityMongoEntity.lockable.getName(),  activity.isLocked());

    activityEntity.append(ActivityMongoEntity.appId.getName(), activity.getAppId());
    activityEntity.append(ActivityMongoEntity.externalId.getName(), activity.getExternalId());

    activity.setUpdated(activityMillis);
      
  }
  
  /*
   * Private
   */
  private void fillActivity(ExoSocialActivity activity, BasicDBObject activityEntity) {

    activity.setId(activityEntity.getString(ActivityMongoEntity.id.getName()));
    activity.setTitle(activityEntity.getString(ActivityMongoEntity.title.getName()));
    activity.setTitleId(activityEntity.getString(ActivityMongoEntity.titleId.getName()));
    activity.setBody(activityEntity.getString(ActivityMongoEntity.body.getName()));
    activity.setBodyId(activityEntity.getString(ActivityMongoEntity.bodyId.getName()));
    
    activity.setPosterId(activityEntity.getString(ActivityMongoEntity.poster.getName()));
    activity.setUserId(activityEntity.getString(ActivityMongoEntity.poster.getName()));
    activity.setStreamOwner(activityEntity.getString(ActivityMongoEntity.owner.getName()));
    activity.setPermanLink(activityEntity.getString(ActivityMongoEntity.permaLink.getName()));
    BasicBSONList likers = (BasicBSONList) activityEntity.get(ActivityMongoEntity.likers.getName());
    activity.setLikeIdentityIds(likers != null ? likers.toArray(new String[0]) : new String[0]);
    
    BasicBSONList mentions = (BasicBSONList) activityEntity.get(ActivityMongoEntity.mentioners.getName());
    activity.setMentionedIds(mentions != null ? mentions.toArray(new String[0]) : new String[0]);

    activity.isHidden(activityEntity.getBoolean(ActivityMongoEntity.hidable.getName()));
    activity.isLocked(activityEntity.getBoolean(ActivityMongoEntity.lockable.getName()));
    
    activity.setPostedTime(activityEntity.getLong(ActivityMongoEntity.postedTime.getName()));
    activity.setUpdated(activityEntity.getLong(ActivityMongoEntity.lastUpdated.getName()));
    
    activity.setAppId(activityEntity.getString(ActivityMongoEntity.appId.getName()));
    activity.setExternalId(activityEntity.getString(ActivityMongoEntity.externalId.getName()));
    
  }
  
	/*
   * Internal
   */
  protected String[] _createActivity(Identity poster, ExoSocialActivity activity) throws MongoException, NodeNotFoundException {
    //
    DBCollection collection = CollectionName.ACTIVITY_COLLECTION.getCollection(this);
    
    BasicDBObject activityEntity = new BasicDBObject();
    fillActivityEntityFromActivity(poster, activity, activityEntity, true);
    //
    collection.insert(activityEntity);
    activity.setId(activityEntity.getString("_id") != null ? activityEntity.getString("_id") : null);
    
    LOG.debug("ACTIVITY ID: " + activity.getId());
    
    //fill streams
    newStreamItemForNewActivity(poster, activity);
    
    return activity.getMentionedIds();
  }
  
  private void newStreamItemForNewActivity(Identity poster, ExoSocialActivity activity) {
    //create StreamItem
    if (OrganizationIdentityProvider.NAME.equals(poster.getProviderId())) {
      //poster
      poster(poster, activity);
      //connection
      connection(poster, activity);
      //mention
      mention(poster, activity, activity.getMentionedIds());
    } else {
      //for SPACE
      spaceMembers(poster, activity);
    }
  }
  
  private void poster(Identity poster, ExoSocialActivity activity) throws MongoException {
    DBCollection streamCol = CollectionName.STREAM_ITEM_COLLECTION.getCollection(this);
    //poster
    BasicDBObject entity = new BasicDBObject();
    fillStreamItem(poster, activity, entity);
    entity = StreamViewType.POSTER.append(entity);
    entity.append(ActivityRefMongoEntity.viewerId.getName(), poster.getId());
    entity.append(ActivityRefMongoEntity.time.getName(), activity.getPostedTime());
    streamCol.insert(entity);
  }
  /**
   * Creates StreamItem for each user who has mentioned on the activity
   * @param poster
   * @param activity
   * @throws MongoException
   */
  private void mention(Identity poster, ExoSocialActivity activity, String[] mentionIds) throws MongoException {
    DBCollection streamCol = CollectionName.STREAM_ITEM_COLLECTION.getCollection(this);
    //
    for (String mentioner : mentionIds) {
      //skip poster
      if (mentioner.equals(poster.getId())) {
        continue;
      }
      BasicDBObject entity = new BasicDBObject();
      fillStreamItem(poster, activity, entity);
      entity = StreamViewType.MENTIONER.append(entity);
      //mentioner = identityId@{n}
      entity.append(ActivityRefMongoEntity.viewerId.getName(), mentioner.split(MENTION_CHAR)[0]);
      entity.append(ActivityRefMongoEntity.time.getName(), activity.getPostedTime());
      streamCol.insert(entity);
    }
  }
  /**
   * Creates StreamItem for each user who has connected to poster
   * @param poster
   * @param activity
   * @throws MongoException
   */
  private void connection(Identity poster, ExoSocialActivity activity) throws MongoException {
    List<Identity> relationships = relationshipStorage.getConnections(poster);
    DBCollection streamCol = CollectionName.STREAM_ITEM_COLLECTION.getCollection(this);
    //
    for (Identity identity : relationships) {
      BasicDBObject entity = new BasicDBObject();
      fillStreamItem(poster, activity, entity);
      //
      StreamViewType.CONNECTION.append(entity);
      //
      entity.append(ActivityRefMongoEntity.viewerId.getName(), identity.getId());
      entity.append(ActivityRefMongoEntity.time.getName(), activity.getPostedTime());
      //
      streamCol.insert(entity);
    }
  }
  
  private void fillStreamItem(Identity poster, ExoSocialActivity activity, BasicDBObject streamItemEntity) {
    //
    streamItemEntity.append(ActivityRefMongoEntity.activityId.getName(), activity.getId());
    streamItemEntity.append(ActivityRefMongoEntity.owner.getName(), poster.getRemoteId());
    streamItemEntity.append(ActivityRefMongoEntity.poster.getName(), activity.getUserId() != null ? activity.getUserId() : poster.getId());
    streamItemEntity.append(ActivityRefMongoEntity.hiable.getName(), activity.isHidden());
    streamItemEntity.append(ActivityRefMongoEntity.lockable.getName(), activity.isLocked());
  }
  
  private void spaceMembers(Identity poster, ExoSocialActivity activity) throws MongoException {
    Space space = spaceStorage.getSpaceByPrettyName(poster.getRemoteId());
    
    if (space == null) return;
    //
    DBCollection streamColl = CollectionName.STREAM_ITEM_COLLECTION.getCollection(this);
    BasicDBObject streamItemEntity = new BasicDBObject();
    fillStreamItem(poster, activity, streamItemEntity);
    streamItemEntity.append(ActivityRefMongoEntity.time.getName(), activity.getPostedTime());
    //
    streamColl.insert(streamItemEntity);
  }
  
  /**
   * Processes Mentioners who has been mentioned via the Activity.
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
  public ExoSocialActivity getParentActivity(ExoSocialActivity comment) throws ActivityStorageException {
    return null;
  }

  @Override
  public void deleteActivity(String activityId) throws ActivityStorageException {
    //
    DBCollection collection = CollectionName.ACTIVITY_COLLECTION.getCollection(this);
    BasicDBObject query = new BasicDBObject();
    query.append("_id", new ObjectId(activityId));
    
    WriteResult result = collection.remove(query);
    LOG.debug("DELETED: " + result);
    deleteActivityRef(activityId);
  }
  
  private void deleteActivityRef(String activityId) {
    DBCollection streamCol = CollectionName.STREAM_ITEM_COLLECTION.getCollection(this);
    BasicDBObject query = new BasicDBObject();
    query.append(ActivityRefMongoEntity.activityId.getName(), activityId);
    
    WriteResult result = streamCol.remove(query);
    LOG.debug("REF DELETED: " + result);
  }

  @Override
  public void deleteComment(String activityId, String commentId) throws ActivityStorageException {
    //
    DBCollection commentCol = CollectionName.COMMENT_COLLECTION.getCollection(this);
    DBCollection activityCol = CollectionName.ACTIVITY_COLLECTION.getCollection(this);
    BasicDBObject query = new BasicDBObject();
    query.append("_id", new ObjectId(commentId));
    
    BasicDBObject comment = (BasicDBObject) commentCol.findOne(query);
    
    WriteResult result = commentCol.remove(query);
    LOG.debug("DELETE COMMENT: " + result);
    
    //update again activity
    query = new BasicDBObject("_id", new ObjectId(activityId));
    BasicDBObject activity = (BasicDBObject) activityCol.findOne(query);
    
    List<String> mentionIds = new ArrayList<String>();
    BasicBSONList mentions = (BasicBSONList) activity.get(ActivityMongoEntity.mentioners.getName());
    String[] new_mentions = processMentions(mentions.toArray(new String[0]), 
                                            comment.getString(ActivityMongoEntity.title.getName()), mentionIds, false);
    
    BasicDBObject update = new BasicDBObject();
    update.append("$set", new BasicDBObject(ActivityMongoEntity.mentioners.getName(), new_mentions));
    //
    activityCol.update(query, update);
    
    //update activities refs for mentioner
    removeMentionerOnStreamItem(activityId, mentionIds.toArray(new String[0]));
	}
  
  private void removeMentionerOnStreamItem(String activityId, String... mentionIds) {
    //
    DBCollection streamCol = CollectionName.STREAM_ITEM_COLLECTION.getCollection(this);
    BasicDBObject query = new BasicDBObject(ActivityRefMongoEntity.activityId.getName(), activityId);
    
    BasicDBObject byViewer = new BasicDBObject(ActivityRefMongoEntity.viewerId.getName(), new BasicDBObject("$in", mentionIds));
    BasicDBObject byPoster = new BasicDBObject(ActivityRefMongoEntity.poster.getName(), new BasicDBObject("$in", mentionIds));
    query.append("$or", new BasicDBObject[] {byViewer, byPoster} );
    
    DBCursor cur = streamCol.find(query);
    while (cur.hasNext()) {
      BasicDBObject it = (BasicDBObject) cur.next();
      //update
      BasicBSONList viewTypes = (BasicBSONList) it.get(ActivityRefMongoEntity.viewerTypes.getName());
      if (viewTypes != null) {
        String[] oldViewTypes = viewTypes.toArray(new String[0]);
        //
        if (oldViewTypes.length == 0) continue;
        
        String[] newViewTypes = (String[]) ArrayUtils.removeElement(oldViewTypes, ActivityRefMongoEntity.ViewerType.MENTIONER.name());
        if (newViewTypes.length == 0) {
          streamCol.remove(it);
        }
        //update ViewTypes field if has changed
        if (newViewTypes.length > 0 && oldViewTypes.length > newViewTypes.length) {
          BasicDBObject update = new BasicDBObject("$set", new BasicDBObject(ActivityRefMongoEntity.viewerTypes.getName(), newViewTypes));
          streamCol.update(it, update);
        }
      }
    }
    
  }

	@Override
	public List<ExoSocialActivity> getActivitiesOfIdentities(
			List<Identity> connectionList, long offset, long limit)
			throws ActivityStorageException {
		return null;
	}

	@Override
	public List<ExoSocialActivity> getActivitiesOfIdentities(
			List<Identity> connectionList, TimestampType type, long offset,
			long limit) throws ActivityStorageException {
		return null;
	}

	@Override
	public int getNumberOfUserActivities(Identity owner) throws ActivityStorageException {
		return getNumberOfUserActivitiesForUpgrade(owner);
	}

	@Override
	public int getNumberOfUserActivitiesForUpgrade(Identity owner) throws ActivityStorageException {
	  //
    DBCollection streamCol = CollectionName.STREAM_ITEM_COLLECTION.getCollection(this);
    
    BasicDBObject query = new BasicDBObject();
    //look for by view types
    String[] viewTypes = new String[]{ViewerType.COMMENTER.name(), ViewerType.LIKER.name(), ViewerType.MENTIONER.name(), ViewerType.POSTER.name()};
    BasicDBObject viewer = new BasicDBObject(ActivityRefMongoEntity.viewerId.getName(), owner.getId());
    viewer.append(ActivityRefMongoEntity.viewerTypes.getName(), new BasicDBObject("$in", viewTypes));
    
    //case user post in space
    BasicDBObject poster = new BasicDBObject(ActivityRefMongoEntity.poster.getName(), owner.getId());
    poster.append(ActivityRefMongoEntity.viewerTypes.getName(), new BasicDBObject("$exists", false));
    //
    query.append("$or", new BasicDBObject[]{poster, viewer});

    return streamCol.find(query).size();
	}

	@Override
	public int getNumberOfNewerOnUserActivities(Identity ownerIdentity,
			ExoSocialActivity baseActivity) {
		return 0;
	}

	@Override
	public List<ExoSocialActivity> getNewerOnUserActivities(
			Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
		return null;
	}

  @Override
  public int getNumberOfOlderOnUserActivities(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getOlderOnUserActivities(Identity ownerIdentity,
                                                          ExoSocialActivity baseActivity,
                                                          int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivityFeed(Identity ownerIdentity, int offset, int limit) {
    return getActivityFeedForUpgrade(ownerIdentity, offset, limit);
  }

  @Override
  public List<ExoSocialActivity> getActivityFeedForUpgrade(Identity ownerIdentity,
                                                           int offset,
                                                           int limit) {
    //
    DBCollection streamCol = CollectionName.STREAM_ITEM_COLLECTION.getCollection(this);
    //DBCollection activityCol = CollectionName.ACTIVITY_COLLECTION.getCollection(this);
    BasicDBObject query = new BasicDBObject();
    BasicDBObject byViewer = new BasicDBObject(ActivityRefMongoEntity.viewerId.getName(), ownerIdentity.getId());
    
    //get spaces where user is member
    List<Space> spaces = spaceStorage.getMemberSpaces(ownerIdentity.getRemoteId());
    String[] spaceIds = new String[0];
    for (Space space : spaces) {
      spaceIds = (String[]) ArrayUtils.add(spaceIds, space.getPrettyName());
    }
    BasicDBObject bySpaces = new BasicDBObject(ActivityRefMongoEntity.owner.getName(), new BasicDBObject("$in", spaceIds));
    query.append("$or", new BasicDBObject[]{ byViewer, bySpaces });
    
    //get since time
    long sinceTime = getStorage().getSinceTime(ownerIdentity, offset, ActivityType.FEED);
    if (sinceTime > 0) {
      query.append(ActivityRefMongoEntity.time.getName(), new BasicDBObject("$lt", sinceTime));
    }
    
    BasicDBObject sortObj = new BasicDBObject(ActivityRefMongoEntity.time.getName(), -1);

    DBCursor cur = streamCol.find(query).sort(sortObj).limit(limit);
    List<ExoSocialActivity> result = new LinkedList<ExoSocialActivity>();
    while (cur.hasNext()) {
      BasicDBObject row = (BasicDBObject) cur.next();
      String activityId = row.getString(ActivityRefMongoEntity.activityId.getName());
      ExoSocialActivity activity = getStorage().getActivity(activityId);
      result.add(activity);
    }
    LOG.debug("getActivityFeed size = "+ result.size());
    
    return result;
  }

  @Override
  public int getNumberOfActivitesOnActivityFeed(Identity ownerIdentity) {
    return getNumberOfActivitesOnActivityFeedForUpgrade(ownerIdentity);
  }

  @Override
  public int getNumberOfActivitesOnActivityFeedForUpgrade(Identity ownerIdentity) {
    DBCollection streamCol = CollectionName.STREAM_ITEM_COLLECTION.getCollection(this);
    //get SpaceIds
    BasicDBObject query = new BasicDBObject();
    BasicDBObject byViewer = new BasicDBObject(ActivityRefMongoEntity.viewerId.getName(), ownerIdentity.getId());
    
    //get spaces where user is member
    List<Space> spaces = spaceStorage.getMemberSpaces(ownerIdentity.getRemoteId());
    String[] spaceIds = new String[0];
    for (Space space : spaces) {
      spaceIds = (String[]) ArrayUtils.add(spaceIds, space.getPrettyName());
    }
    BasicDBObject bySpaces = new BasicDBObject(ActivityRefMongoEntity.owner.getName(), new BasicDBObject("$in", spaceIds));
    query.append("$or", new BasicDBObject[]{ byViewer, bySpaces });

    return streamCol.find(query).size();
  }

  @Override
  public int getNumberOfNewerOnActivityFeed(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerOnActivityFeed(Identity ownerIdentity,
                                                        ExoSocialActivity baseActivity,
                                                        int limit) {
    return null;
  }

  @Override
  public int getNumberOfOlderOnActivityFeed(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getOlderOnActivityFeed(Identity ownerIdentity,
                                                        ExoSocialActivity baseActivity,
                                                        int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfConnections(Identity ownerIdentity,
                                                            int offset,
                                                            int limit) {
   return getActivitiesOfConnectionsForUpgrade(ownerIdentity, offset, limit);
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfConnectionsForUpgrade(Identity ownerIdentity,
                                                                      int offset,
                                                                      int limit) {
    //
    DBCollection streamCol = CollectionName.STREAM_ITEM_COLLECTION.getCollection(this);
    //DBCollection activityColl = CollectionName.ACTIVITY_COLLECTION.getCollection(this);
    BasicDBObject query = new BasicDBObject(ActivityRefMongoEntity.viewerId.getName(), ownerIdentity.getId());
    //query.append(ActivityRefMongoEntity.viewerTypes.getName(), ActivityRefMongoEntity.ViewerType.CONNECTION.name());
    query.append(ActivityRefMongoEntity.viewerTypes.getName(), new BasicDBObject("$in", new String[] {ActivityRefMongoEntity.ViewerType.CONNECTION.name()}));
    
    //get since time
    long sinceTime = getStorage().getSinceTime(ownerIdentity, offset, ActivityType.CONNECTION);
    if (sinceTime > 0) {
      query.append(ActivityRefMongoEntity.time.getName(), new BasicDBObject("$lt", sinceTime));
    }
    BasicDBObject sortObj = new BasicDBObject("time", -1);
    
    DBCursor cur = streamCol.find(query).sort(sortObj).limit(limit);
    List<ExoSocialActivity> result = new LinkedList<ExoSocialActivity>();
    while (cur.hasNext()) {
      BasicDBObject row = (BasicDBObject) cur.next();
      String activityId = row.getString(ActivityRefMongoEntity.activityId.getName());
      ExoSocialActivity activity = getStorage().getActivity(activityId);
      result.add(activity);
    }
    LOG.debug("getActivitiesOfConnections size = "+ result.size());
    
    return result;
  }

  @Override
  public int getNumberOfActivitiesOfConnections(Identity ownerIdentity) {
   return getNumberOfActivitiesOfConnectionsForUpgrade(ownerIdentity); 
  }

  @Override
  public int getNumberOfActivitiesOfConnectionsForUpgrade(Identity ownerIdentity) {
    //
    DBCollection streamCol = CollectionName.STREAM_ITEM_COLLECTION.getCollection(this);
    BasicDBObject query = new BasicDBObject(ActivityRefMongoEntity.viewerId.getName(), ownerIdentity.getId());
    query.append(ActivityRefMongoEntity.viewerTypes.getName(), ActivityRefMongoEntity.ViewerType.CONNECTION.name());
    return streamCol.find(query).size();
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfIdentity(Identity ownerIdentity,
                                                         long offset,
                                                         long limit) {
    return null;
  }

  @Override
  public int getNumberOfNewerOnActivitiesOfConnections(Identity ownerIdentity,
                                                       ExoSocialActivity baseActivity) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerOnActivitiesOfConnections(Identity ownerIdentity,
                                                                   ExoSocialActivity baseActivity,
                                                                   long limit) {
    return null;
  }

  @Override
  public int getNumberOfOlderOnActivitiesOfConnections(Identity ownerIdentity,
                                                       ExoSocialActivity baseActivity) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getOlderOnActivitiesOfConnections(Identity ownerIdentity,
                                                                   ExoSocialActivity baseActivity,
                                                                   int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getUserSpacesActivities(Identity ownerIdentity,
                                                         int offset,
                                                         int limit) {
    //
    DBCollection streamCol = getCollection(CollectionName.STREAM_ITEM_COLLECTION.collectionName());
    
    List<Space> spaces = spaceStorage.getMemberSpaces(ownerIdentity.getRemoteId());
    String[] spaceIds = new String[0];
    for (Space space : spaces) {
      spaceIds = (String[]) ArrayUtils.add(spaceIds, space.getPrettyName());
    }
    BasicDBObject query = new BasicDBObject();
    query.append(ActivityRefMongoEntity.owner.getName(), new BasicDBObject("$in", spaceIds));
    
    //get since time
    long sinceTime = getStorage().getSinceTime(ownerIdentity, offset, ActivityType.SPACES);
    if (sinceTime > 0) {
      query.append(ActivityRefMongoEntity.time.getName(), new BasicDBObject("$lt", sinceTime));
    }
    
    //sort by time DESC
    BasicDBObject sortObj = new BasicDBObject(ActivityRefMongoEntity.time.getName(), -1);

    DBCursor cur = streamCol.find(query).sort(sortObj).limit(limit);
    List<ExoSocialActivity> result = new LinkedList<ExoSocialActivity>();
    while (cur.hasNext()) {
      BasicDBObject row = (BasicDBObject) cur.next();
      String activityId = row.getString(ActivityRefMongoEntity.activityId.getName());
      ExoSocialActivity activity = getStorage().getActivity(activityId);
      result.add(activity);
    }
    LOG.debug("getUserSpacesActivities size = "+ result.size());
    
    return result;
  }

  @Override
  public List<ExoSocialActivity> getUserSpacesActivitiesForUpgrade(Identity ownerIdentity,
                                                                   int offset,
                                                                   int limit) {
    return null;
  }

  @Override
  public int getNumberOfUserSpacesActivities(Identity ownerIdentity) {
    return 0;
  }

  @Override
  public int getNumberOfUserSpacesActivitiesForUpgrade(Identity ownerIdentity) {
    return 0;
  }

  @Override
  public int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity,
                                                    ExoSocialActivity baseActivity) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerOnUserSpacesActivities(Identity ownerIdentity,
                                                                ExoSocialActivity baseActivity,
                                                                int limit) {
    return null;
  }

  @Override
  public int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity,
                                                    ExoSocialActivity baseActivity) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getOlderOnUserSpacesActivities(Identity ownerIdentity,
                                                                ExoSocialActivity baseActivity,
                                                                int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getComments(ExoSocialActivity existingActivity,
                                             int offset,
                                             int limit) {
    DBCollection activityColl = CollectionName.COMMENT_COLLECTION.getCollection(this);
    BasicDBObject query = new BasicDBObject(CommentMongoEntity.activityId.getName(), existingActivity.getId());

    DBCursor cur = activityColl.find(query);
    List<ExoSocialActivity> result = new ArrayList<ExoSocialActivity>();
    while (cur.hasNext()) {
      BasicDBObject entity = (BasicDBObject) cur.next();
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      fillActivity(activity, entity);
      activity.isComment(true);
      
      processActivity(activity);
      result.add(activity);
    }
    LOG.debug("=======>getComments SIZE ="+ result.size());
    
    return result;
  }

  @Override
  public int getNumberOfComments(ExoSocialActivity existingActivity) {
    DBCollection activityColl = CollectionName.COMMENT_COLLECTION.getCollection(this);
    BasicDBObject query = new BasicDBObject(CommentMongoEntity.activityId.getName(), existingActivity.getId());
    return activityColl.find(query).size();
  }

  @Override
  public int getNumberOfNewerComments(ExoSocialActivity existingActivity,
                                      ExoSocialActivity baseComment) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerComments(ExoSocialActivity existingActivity,
                                                  ExoSocialActivity baseComment,
                                                  int limit) {
    return null;
  }

  @Override
  public int getNumberOfOlderComments(ExoSocialActivity existingActivity,
                                      ExoSocialActivity baseComment) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getOlderComments(ExoSocialActivity existingActivity,
                                                  ExoSocialActivity baseComment,
                                                  int limit) {
    return null;
  }

	@Override
	public SortedSet<ActivityProcessor> getActivityProcessors() {
		return activityProcessors;
	}

	@Override
  public void updateActivity(ExoSocialActivity existingActivity) throws ActivityStorageException {
	  //
    DBCollection streamCol = CollectionName.STREAM_ITEM_COLLECTION.getCollection(this);
    
    BasicDBObject update = new BasicDBObject();
    
    BasicDBObject set = new BasicDBObject();
    //
    fillActivityEntityFromActivity(null, existingActivity, set, false);
    update.append("$set", set);
    
    BasicDBObject query = new BasicDBObject();
    query.append(ActivityMongoEntity.id.getName(), new ObjectId(existingActivity.getId()));
    
    WriteResult result = streamCol.update(query, update);
    LOG.debug("==============>UPDATED ACTIVITY: " + result.toString());
    
    //
    long currentMillis = System.currentTimeMillis();
    LOG.debug("==============>UPDATED ACTIVITY REF [TIME]: " + currentMillis);
    updateActivityRef(existingActivity.getId(), currentMillis);
    
	}

	@Override
  public int getNumberOfNewerOnActivityFeed(Identity ownerIdentity, Long sinceTime) {
		return 0;
	}

	@Override
	public int getNumberOfNewerOnUserActivities(Identity ownerIdentity,
			Long sinceTime) {
		return 0;
	}

	@Override
	public int getNumberOfNewerOnActivitiesOfConnections(
			Identity ownerIdentity, Long sinceTime) {
		return 0;
	}

	@Override
	public int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity,
			Long sinceTime) {
		return 0;
	}

  @Override
  public List<ExoSocialActivity> getActivitiesOfIdentities(ActivityBuilderWhere where,
                                                           ActivityFilter filter,
                                                           long offset,
                                                           long limit) throws ActivityStorageException {
    return null;
  }

  @Override
  public int getNumberOfSpaceActivities(Identity spaceIdentity) {
    return getNumberOfSpaceActivitiesForUpgrade(spaceIdentity);
    
  }

  @Override
  public int getNumberOfSpaceActivitiesForUpgrade(Identity spaceIdentity) {
    //
    DBCollection connectionColl = CollectionName.COMMENT_COLLECTION.getCollection(this);
    BasicDBObject query = new BasicDBObject(ActivityRefMongoEntity.owner.getName(), spaceIdentity.getId());
    DBCursor cur = connectionColl.find(query);
    LOG.debug("getNumberOfSpaceActivities size = "+ cur.size());
    return cur.size();
  }

  @Override
  public List<ExoSocialActivity> getSpaceActivities(Identity spaceIdentity, int index, int limit) {
    return getSpaceActivitiesForUpgrade(spaceIdentity, index, limit);
  }

  @Override
  public List<ExoSocialActivity> getSpaceActivitiesForUpgrade(Identity spaceIdentity,
                                                              int index,
                                                              int limit) {
    //
    DBCollection connectionColl = CollectionName.STREAM_ITEM_COLLECTION.getCollection(this);
    
    BasicDBObject query = new BasicDBObject(ActivityRefMongoEntity.owner.getName(), spaceIdentity.getRemoteId());

    //get since time
    long sinceTime = getStorage().getSinceTime(spaceIdentity, index, ActivityType.SPACE);
    if (sinceTime > 0) {
      query.append(ActivityRefMongoEntity.time.getName(), new BasicDBObject("$lt", sinceTime));
    }
    //sort by time DESC
    BasicDBObject sortObj = new BasicDBObject(ActivityRefMongoEntity.time.getName(), -1);

    DBCursor cur = connectionColl.find(query).sort(sortObj).limit(limit);
    List<ExoSocialActivity> result = new LinkedList<ExoSocialActivity>();
    while (cur.hasNext()) {
      BasicDBObject row = (BasicDBObject) cur.next();
      String activityId = row.getString(ActivityRefMongoEntity.activityId.getName());
      ExoSocialActivity activity = getStorage().getActivity(activityId);
      result.add(activity);
    }
    LOG.debug("getSpaceActivities size = "+ result.size());
    
    return result;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesByPoster(Identity posterIdentity,
                                                       int offset,
                                                       int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesByPoster(Identity posterIdentity,
                                                       int offset,
                                                       int limit,
                                                       String... activityTypes) {
    return null;
  }

  @Override
  public int getNumberOfActivitiesByPoster(Identity posterIdentity) {
    return 0;
  }

  @Override
  public int getNumberOfActivitiesByPoster(Identity ownerIdentity, Identity viewerIdentity) {
    //
    DBCollection connectionColl = CollectionName.STREAM_ITEM_COLLECTION.getCollection(this);
    String[] identityIds = getIdentities(ownerIdentity, viewerIdentity);
    BasicDBObject query = new BasicDBObject(ActivityRefMongoEntity.viewerId.getName(), new BasicDBObject("$in", identityIds));
    return connectionColl.find(query).size();
  }

  @Override
  public List<ExoSocialActivity> getNewerOnSpaceActivities(Identity spaceIdentity,
                                                           ExoSocialActivity baseActivity,
                                                           int limit) {
    return null;
  }

  @Override
  public int getNumberOfNewerOnSpaceActivities(Identity spaceIdentity,
                                               ExoSocialActivity baseActivity) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getOlderOnSpaceActivities(Identity spaceIdentity,
                                                           ExoSocialActivity baseActivity,
                                                           int limit) {
    return null;
  }

  @Override
  public int getNumberOfOlderOnSpaceActivities(Identity spaceIdentity,
                                               ExoSocialActivity baseActivity) {
    return 0;
  }

  @Override
  public int getNumberOfNewerOnSpaceActivities(Identity spaceIdentity, Long sinceTime) {
    return 0;
  }

  @Override
  public int getNumberOfUpdatedOnActivityFeed(Identity owner, ActivityUpdateFilter filter) {
    return 0;
  }

  @Override
  public int getNumberOfUpdatedOnUserActivities(Identity owner, ActivityUpdateFilter filter) {
    return 0;
  }

  @Override
  public int getNumberOfUpdatedOnActivitiesOfConnections(Identity owner, ActivityUpdateFilter filter) {
    return 0;
  }

  @Override
  public int getNumberOfUpdatedOnUserSpacesActivities(Identity owner, ActivityUpdateFilter filter) {
    return 0;
  }

  @Override
  public int getNumberOfUpdatedOnSpaceActivities(Identity owner, ActivityUpdateFilter filter) {
    return 0;
  }

  @Override
  public int getNumberOfMultiUpdated(Identity owner, Map<String, Long> sinceTimes) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerFeedActivities(Identity owner, Long sinceTime, int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getNewerUserActivities(Identity owner, Long sinceTime, int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getNewerUserSpacesActivities(Identity owner,
                                                              Long sinceTime,
                                                              int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getNewerActivitiesOfConnections(Identity owner,
                                                                 Long sinceTime,
                                                                 int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getNewerSpaceActivities(Identity owner, Long sinceTime, int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getOlderFeedActivities(Identity owner, Long sinceTime, int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getOlderUserActivities(Identity owner, Long sinceTime, int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getOlderUserSpacesActivities(Identity owner,
                                                              Long sinceTime,
                                                              int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getOlderActivitiesOfConnections(Identity owner,
                                                                 Long sinceTime,
                                                                 int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getOlderSpaceActivities(Identity owner, Long sinceTime, int limit) {
    return null;
  }

  @Override
  public int getNumberOfOlderOnActivityFeed(Identity ownerIdentity, Long sinceTime) {
    return 0;
  }

  @Override
  public int getNumberOfOlderOnUserActivities(Identity ownerIdentity, Long sinceTime) {
    return 0;
  }

  @Override
  public int getNumberOfOlderOnActivitiesOfConnections(Identity ownerIdentity, Long sinceTime) {
    return 0;
  }

  @Override
  public int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity, Long sinceTime) {
    return 0;
  }

  @Override
  public int getNumberOfOlderOnSpaceActivities(Identity ownerIdentity, Long sinceTime) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerComments(ExoSocialActivity existingActivity,
                                                  Long sinceTime,
                                                  int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getOlderComments(ExoSocialActivity existingActivity,
                                                  Long sinceTime,
                                                  int limit) {
    return null;
  }

  @Override
  public int getNumberOfNewerComments(ExoSocialActivity existingActivity, Long sinceTime) {
    return 0;
  }

  @Override
  public int getNumberOfOlderComments(ExoSocialActivity existingActivity, Long sinceTime) {
    return 0;
  }

  public ExoSocialActivity getComment(String commentId) throws ActivityStorageException {
    //
    DBCollection collection = CollectionName.COMMENT_COLLECTION.getCollection(this);
    BasicDBObject query = new BasicDBObject();
    query.append("_id", new ObjectId(commentId));
    
    BasicDBObject entity = (BasicDBObject) collection.findOne(query);
    
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    
    fillActivity(activity, entity);
    activity.isComment(true);
    
    processActivity(activity);
    
    return activity;
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
  
  private ActivityStorage getStorage() {
    if (activityStorage == null) {
      activityStorage = (ActivityStorage) PortalContainer.getInstance().getComponentInstanceOfType(ActivityStorage.class);
    }
    
    return activityStorage;
  }


  @Override
  public long getSinceTime(Identity owner, long offset, ActivityType type) throws ActivityStorageException {
    // TODO Auto-generated method stub
    return 0;
  }

}
