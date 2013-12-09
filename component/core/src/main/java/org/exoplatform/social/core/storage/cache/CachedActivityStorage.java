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

package org.exoplatform.social.core.storage.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.ActivityProcessor;
import org.exoplatform.social.core.activity.filter.ActivityFilter;
import org.exoplatform.social.core.activity.filter.ActivityUpdateFilter;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.storage.ActivityStorageException;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.cache.loader.ServiceContext;
import org.exoplatform.social.core.storage.cache.model.data.ActivityData;
import org.exoplatform.social.core.storage.cache.model.data.IntegerData;
import org.exoplatform.social.core.storage.cache.model.data.ListActivitiesData;
import org.exoplatform.social.core.storage.cache.model.data.ListIdentitiesData;
import org.exoplatform.social.core.storage.cache.model.key.ActivityCountKey;
import org.exoplatform.social.core.storage.cache.model.key.ActivityKey;
import org.exoplatform.social.core.storage.cache.model.key.ActivityType;
import org.exoplatform.social.core.storage.cache.model.key.IdentityKey;
import org.exoplatform.social.core.storage.cache.model.key.ListActivitiesKey;
import org.exoplatform.social.core.storage.cache.selector.ActivityOwnerCacheSelector;
import org.exoplatform.social.core.storage.cache.selector.ScopeCacheSelector;
import org.exoplatform.social.core.storage.impl.ActivityBuilderWhere;
import org.exoplatform.social.core.storage.impl.ActivityStorageImpl;
import org.exoplatform.social.core.storage.mongodb.ActivityMongoStorageImpl;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 * @version $Revision$
 */
public class CachedActivityStorage implements ActivityStorage {

  /** Logger */
  private static final Log LOG = ExoLogger.getLogger(CachedActivityStorage.class);

  private final ExoCache<ActivityKey, ActivityData> exoActivityCache;
  private final ExoCache<ActivityCountKey, IntegerData> exoActivitiesCountCache;
  private final ExoCache<ListActivitiesKey, ListActivitiesData> exoActivitiesCache;
  private final ExoCache<ActivityCountKey, Long> exoActivitySinceTimeCache;

  private final FutureExoCache<ActivityKey, ActivityData, ServiceContext<ActivityData>> activityCache;
  private final FutureExoCache<ActivityCountKey, IntegerData, ServiceContext<IntegerData>> activitiesCountCache;
  private final FutureExoCache<ActivityCountKey, Long, ServiceContext<Long>> activitySinceTimeCache;
  private final FutureExoCache<ListActivitiesKey, ListActivitiesData, ServiceContext<ListActivitiesData>> activitiesCache;

  private final ActivityMongoStorageImpl mongoStorage;

  public void clearCache() {

    try {
      exoActivitiesCache.select(new ScopeCacheSelector<ListActivitiesKey, ListActivitiesData>());
      exoActivitiesCountCache.select(new ScopeCacheSelector<ActivityCountKey, IntegerData>());
      exoActivitySinceTimeCache.select(new ScopeCacheSelector<ActivityCountKey, Long>());
    }
    catch (Exception e) {
      LOG.error(e);
    }

  }

  void clearOwnerCache(String ownerId) {

    try {
      exoActivityCache.select(new ActivityOwnerCacheSelector(ownerId));
    }
    catch (Exception e) {
      LOG.error(e);
    }

    clearCache();

  }

  /**
   * Clear activity cached.
   * 
   * @param activityId
   * @since 1.2.8
   */
  public void clearActivityCached(String activityId) {
    ActivityKey key = new ActivityKey(activityId);
    exoActivityCache.remove(key);
    clearCache();
  }
  
  /**
   * Build the activity list from the caches Ids.
   *
   * @param data ids
   * @return activities
   */
  private List<ExoSocialActivity> buildActivities(ListActivitiesData data) {

    List<ExoSocialActivity> activities = new ArrayList<ExoSocialActivity>();
    for (ActivityKey k : data.getIds()) {
      ExoSocialActivity a = getActivity(k.getId());
      activities.add(a);
    }
    return activities;

  }

  /**
   * Build the ids from the activity list.
   *
   * @param activities activities
   * @param sinceTimeKey
   * @return ids
   */
  private ListActivitiesData buildIds(List<ExoSocialActivity> activities, final ActivityCountKey sinceTimeKey) {

    List<ActivityKey> data = new ArrayList<ActivityKey>();
    long lastTime = 0;
    for (ExoSocialActivity a : activities) {
      ActivityKey k = new ActivityKey(a.getId());
      exoActivityCache.put(k, new ActivityData(a));
      data.add(k);
      lastTime = a.getUpdated().getTime();
    }
    
    if (sinceTimeKey != null) {
      exoActivitySinceTimeCache.put(sinceTimeKey, lastTime);
    }
    
    //
    return new ListActivitiesData(data);

  }

  public CachedActivityStorage(final ActivityStorageImpl storage, final ActivityMongoStorageImpl mongoStorage, final SocialStorageCacheService cacheService) {

    //
    this.mongoStorage = mongoStorage;

    //
    this.exoActivityCache = cacheService.getActivityCache();
    this.exoActivitiesCountCache = cacheService.getActivitiesCountCache();
    this.exoActivitiesCache = cacheService.getActivitiesCache();
    this.exoActivitySinceTimeCache = cacheService.getActivitySinceTimeCache();

    //
    this.activityCache = CacheType.ACTIVITY.createFutureCache(exoActivityCache);
    this.activitiesCountCache = CacheType.ACTIVITIES_COUNT.createFutureCache(exoActivitiesCountCache);
    this.activitiesCache = CacheType.ACTIVITIES.createFutureCache(exoActivitiesCache);
    this.activitySinceTimeCache = CacheType.ACTIVITY_SINCE_TIME.createFutureCache(exoActivitySinceTimeCache);

  }

  /**
   * {@inheritDoc}
   */
  public ExoSocialActivity getActivity(final String activityId) throws ActivityStorageException {

    //
    ActivityKey key = new ActivityKey(activityId);

    //
    ActivityData activity = activityCache.get(
        new ServiceContext<ActivityData>() {
          public ActivityData execute() {
            try {
              ExoSocialActivity got = mongoStorage.getActivity(activityId);
              if (got != null) {
                return new ActivityData(got);
              }
              else {
                return ActivityData.NULL;
              }
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        },
        key);

    //
    return activity.build();

  }
  
  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getUserActivities(final Identity owner) throws ActivityStorageException {
    return mongoStorage.getUserActivities(owner);
  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getUserActivities(final Identity owner, final long offset, final long limit)
                                                                                            throws ActivityStorageException {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), ActivityType.USER);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getUserActivities(owner, offset, limit);
            ActivityCountKey sinceTimeKey = new ActivityCountKey(new IdentityKey(owner), ActivityType.USER, offset + limit);
            return buildIds(got, sinceTimeKey);
          }
        },
        listKey);

    //
    return buildActivities(keys);

  }

  /**
   * {@inheritDoc}
   */
  public void saveComment(final ExoSocialActivity activity, final ExoSocialActivity comment) throws ActivityStorageException {

    //
    mongoStorage.saveComment(activity, comment);

    //
    exoActivityCache.put(new ActivityKey(comment.getId()), new ActivityData(getComment(comment.getId())));
    ActivityKey activityKey = new ActivityKey(activity.getId());
    exoActivityCache.remove(activityKey);
    exoActivityCache.put(activityKey, new ActivityData(getActivity(activity.getId())));
    clearCache();
  }

  /**
   * {@inheritDoc}
   */
  public ExoSocialActivity saveActivity(final Identity owner, final ExoSocialActivity activity) throws ActivityStorageException {

    //
    ExoSocialActivity a = mongoStorage.saveActivity(owner, activity);

    //
    ActivityKey key = new ActivityKey(a.getId());
    exoActivityCache.put(key, new ActivityData(getActivity(a.getId())));
    clearCache();

    //
    return a;

  }

  /**
   * {@inheritDoc}
   */
  public ExoSocialActivity getParentActivity(final ExoSocialActivity comment) throws ActivityStorageException {
    return mongoStorage.getParentActivity(comment);
  }

  /**
   * {@inheritDoc}
   */
  public void deleteActivity(final String activityId) throws ActivityStorageException {

    //
    mongoStorage.deleteActivity(activityId);

    //
    ActivityKey key = new ActivityKey(activityId);
    exoActivityCache.remove(key);
    clearCache();

  }

  /**
   * {@inheritDoc}
   */
  public void deleteComment(final String activityId, final String commentId) throws ActivityStorageException {
    
    //
    mongoStorage.deleteComment(activityId, commentId);

    //
    exoActivityCache.remove(new ActivityKey(commentId));
    ActivityKey activityKey = new ActivityKey(activityId);
    exoActivityCache.remove(activityKey);
    exoActivityCache.put(activityKey, new ActivityData(getActivity(activityId)));

    clearActivityCached(activityId);
  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getActivitiesOfIdentities(final List<Identity> connectionList, final long offset,
                                                           final long limit) throws ActivityStorageException {

    //
    List<IdentityKey> keyskeys = new ArrayList<IdentityKey>();
    for (Identity i : connectionList) {
      keyskeys.add(new IdentityKey(i));
    }
    ListActivitiesKey listKey = new ListActivitiesKey(new ListIdentitiesData(keyskeys), 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getActivitiesOfIdentities(connectionList, offset, limit);
            return buildIds(got, null);
          }
        },
        listKey);

    //
    return buildActivities(keys);

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getActivitiesOfIdentities(final List<Identity> connectionList, final TimestampType type,
                                                           final long offset, final long limit) throws ActivityStorageException {
    return mongoStorage.getActivitiesOfIdentities(connectionList, type, offset, limit);
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfUserActivities(final Identity owner) throws ActivityStorageException {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), ActivityType.USER);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfUserActivities(owner));
          }
        },
        key)
        .build();
    
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfNewerOnUserActivities(final Identity ownerIdentity, final ExoSocialActivity baseActivity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.NEWER_USER);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfNewerOnUserActivities(ownerIdentity, baseActivity));
          }
        },
        key)
        .build();
    
  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getNewerOnUserActivities(final Identity ownerIdentity, final ExoSocialActivity baseActivity,
                                                          final int limit) {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.NEWER_USER);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getNewerOnUserActivities(ownerIdentity, baseActivity, limit);
            return buildIds(got, null);
          }
        },
        listKey);

    //
    return buildActivities(keys);

  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfOlderOnUserActivities(final Identity ownerIdentity, final ExoSocialActivity baseActivity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.OLDER_USER);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfOlderOnUserActivities(ownerIdentity, baseActivity));
          }
        },
        key)
        .build();
    
  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getOlderOnUserActivities(final Identity ownerIdentity, final ExoSocialActivity baseActivity,
                                                          final int limit) {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.OLDER_USER);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getOlderOnUserActivities(ownerIdentity, baseActivity, limit);
            return buildIds(got, null);
          }
        },
        listKey);

    //
    return buildActivities(keys);

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getActivityFeed(final Identity ownerIdentity, final int offset, final int limit) {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.FEED);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getActivityFeed(ownerIdentity, offset, limit);
            ActivityCountKey sinceTimeKey = new ActivityCountKey(new IdentityKey(ownerIdentity), 
                                                                 ActivityType.FEED, (long) (offset + limit));
            return buildIds(got, sinceTimeKey);
          }
        },
        listKey);

    //
    return buildActivities(keys);

  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfActivitesOnActivityFeed(final Identity ownerIdentity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.FEED);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfActivitesOnActivityFeed(ownerIdentity));
          }
        },
        key)
        .build();

  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfNewerOnActivityFeed(final Identity ownerIdentity, final ExoSocialActivity baseActivity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.NEWER_FEED);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfNewerOnActivityFeed(ownerIdentity, baseActivity));
          }
        },
        key)
        .build();

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getNewerOnActivityFeed(final Identity ownerIdentity, final ExoSocialActivity baseActivity,
                                                        final int limit) {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.NEWER_FEED);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getNewerOnActivityFeed(ownerIdentity, baseActivity, limit);
            return buildIds(got, null);
          }
        },
        listKey);

    //
    return buildActivities(keys);

  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfOlderOnActivityFeed(final Identity ownerIdentity, final ExoSocialActivity baseActivity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.OLDER_FEED);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfOlderOnActivityFeed(ownerIdentity, baseActivity));
          }
        },
        key)
        .build();

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getOlderOnActivityFeed(final Identity ownerIdentity, final ExoSocialActivity baseActivity,
                                                        final int limit) {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.OLDER_FEED);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getOlderOnActivityFeed(ownerIdentity, baseActivity, limit);
            return buildIds(got, null);
          }
        },
        listKey);

    //
    return buildActivities(keys);
    
  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getActivitiesOfConnections(final Identity ownerIdentity, final int offset, final int limit) {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.CONNECTION);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getActivitiesOfConnections(ownerIdentity, offset, limit);
            ActivityCountKey sinceTimeKey = new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.CONNECTION, (long) (offset + limit));
            return buildIds(got, sinceTimeKey);
          }
        },
        listKey);

    //
    return buildActivities(keys);
    
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfActivitiesOfConnections(final Identity ownerIdentity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.CONNECTION);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfActivitiesOfConnections(ownerIdentity));
          }
        },
        key)
        .build();

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getActivitiesOfIdentity(final Identity ownerIdentity, final long offset, final long limit)
                                                                                       throws ActivityStorageException {
    return mongoStorage.getActivitiesOfIdentity(ownerIdentity, offset, limit);
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfNewerOnActivitiesOfConnections(final Identity ownerIdentity, final ExoSocialActivity baseActivity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.NEWER_CONNECTION);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfNewerOnActivitiesOfConnections(ownerIdentity, baseActivity));
          }
        },
        key)
        .build();

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getNewerOnActivitiesOfConnections(final Identity ownerIdentity,
                                                                   final ExoSocialActivity baseActivity, final long limit) {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(),
                                                ActivityType.NEWER_CONNECTION);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getNewerOnActivitiesOfConnections(ownerIdentity, baseActivity, limit);
            return buildIds(got, null);
          }
        },
        listKey);

    //
    return buildActivities(keys);
    
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfOlderOnActivitiesOfConnections(final Identity ownerIdentity, final ExoSocialActivity baseActivity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.OLDER_CONNECTION);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfOlderOnActivitiesOfConnections(ownerIdentity, baseActivity));
          }
        },
        key)
        .build();

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getOlderOnActivitiesOfConnections(final Identity ownerIdentity,
                                                                   final ExoSocialActivity baseActivity, final int limit) {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(),
                                                ActivityType.OLDER_CONNECTION);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getOlderOnActivitiesOfConnections(ownerIdentity, baseActivity, limit);
            return buildIds(got, null);
          }
        },
        listKey);

    //
    return buildActivities(keys);

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getUserSpacesActivities(final Identity ownerIdentity, final int offset, final int limit) {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.SPACES);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getUserSpacesActivities(ownerIdentity, offset, limit);
            ActivityCountKey sinceTimeKey = new ActivityCountKey(new IdentityKey(ownerIdentity), 
                                                                 ActivityType.SPACES, (long) (offset + limit));
            return buildIds(got, sinceTimeKey);
          }
        },
        listKey);

    //
    return buildActivities(keys);

  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfUserSpacesActivities(final Identity ownerIdentity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.SPACES);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfUserSpacesActivities(ownerIdentity));
          }
        },
        key)
        .build();
    
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfNewerOnUserSpacesActivities(final Identity ownerIdentity, final ExoSocialActivity baseActivity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.NEWER_SPACES);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfNewerOnUserSpacesActivities(ownerIdentity, baseActivity));
          }
        },
        key)
        .build();

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getNewerOnUserSpacesActivities(final Identity ownerIdentity,
                                                                final ExoSocialActivity baseActivity, final int limit) {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.NEWER_SPACES);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getNewerOnUserSpacesActivities(ownerIdentity, baseActivity, limit);
            return buildIds(got, null);
          }
        },
        listKey);

    //
    return buildActivities(keys);

  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfOlderOnUserSpacesActivities(final Identity ownerIdentity, final ExoSocialActivity baseActivity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.OLDER_SPACES);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfOlderOnUserSpacesActivities(ownerIdentity, baseActivity));
          }
        },
        key)
        .build();

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getOlderOnUserSpacesActivities(final Identity ownerIdentity,
                                                                final ExoSocialActivity baseActivity, final int limit) {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.OLDER_SPACES);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getOlderOnUserSpacesActivities(ownerIdentity, baseActivity, limit);
            return buildIds(got, null);
          }
        },
        listKey);

    //
    return buildActivities(keys);

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getComments(final ExoSocialActivity existingActivity, final int offset, final int limit) {
    //
    ActivityCountKey key = new ActivityCountKey(existingActivity.getId(), ActivityType.COMMENTS);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getComments(existingActivity, offset, limit);
            return buildIds(got, null);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfComments(final ExoSocialActivity existingActivity) {
    
    //
    ActivityCountKey key =
        new ActivityCountKey(existingActivity.getId(), ActivityType.COMMENTS);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfComments(existingActivity));
          }
        },
        key)
        .build();
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfNewerComments(final ExoSocialActivity existingActivity, final ExoSocialActivity baseComment) {
    return mongoStorage.getNumberOfNewerComments(existingActivity, baseComment);
  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getNewerComments(final ExoSocialActivity existingActivity, final ExoSocialActivity baseComment,
                                                  final int limit) {
    return mongoStorage.getNewerComments(existingActivity, baseComment, limit);
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfOlderComments(final ExoSocialActivity existingActivity, final ExoSocialActivity baseComment) {
    return mongoStorage.getNumberOfOlderComments(existingActivity, baseComment);
  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getOlderComments(final ExoSocialActivity existingActivity, final ExoSocialActivity baseComment,
                                                  final int limit) {
    return mongoStorage.getOlderComments(existingActivity, baseComment, limit);
  }

  /**
   * {@inheritDoc}
   */
  public SortedSet<ActivityProcessor> getActivityProcessors() {
    return mongoStorage.getActivityProcessors();
  }

  /**
   * {@inheritDoc}
   */
  public void updateActivity(final ExoSocialActivity existingActivity) throws ActivityStorageException {

    //
    mongoStorage.updateActivity(existingActivity);
    
    //
    ActivityKey key = new ActivityKey(existingActivity.getId());
    exoActivityCache.remove(key);
    
    //
    clearCache();
    clearActivityCached(existingActivity.getId());
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfNewerOnActivityFeed(final Identity ownerIdentity, final Long sinceTime) {

    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity),
                                                sinceTime,
                                                ActivityType.NEWER_FEED);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(mongoStorage.getNumberOfNewerOnActivityFeed(ownerIdentity, sinceTime));
      }
    }, key).build();
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfNewerOnUserActivities(final Identity ownerIdentity, final Long sinceTime) {

    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity),
                                                sinceTime,
                                                ActivityType.NEWER_USER);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(mongoStorage.getNumberOfNewerOnUserActivities(ownerIdentity, sinceTime));
      }
    }, key).build();
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfNewerOnActivitiesOfConnections(final Identity ownerIdentity, final Long sinceTime) {

    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), sinceTime, ActivityType.NEWER_CONNECTION);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(mongoStorage.getNumberOfNewerOnActivitiesOfConnections(ownerIdentity, sinceTime));
      }
    }, key).build();
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfNewerOnUserSpacesActivities(final Identity ownerIdentity, final Long sinceTime) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity),
                                                sinceTime,
                                                ActivityType.NEWER_SPACE);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(mongoStorage.getNumberOfNewerOnUserSpacesActivities(ownerIdentity,
                                                                              sinceTime));
      }
    }, key).build();
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfIdentities(ActivityBuilderWhere where,
                                                           ActivityFilter filter,
                                                           final long offset,
                                                           final long limit) throws ActivityStorageException {
    final List<Identity> connectionList = where.getOwners();
    //
    List<IdentityKey> keyskeys = new ArrayList<IdentityKey>();
    for (Identity i : connectionList) {
      keyskeys.add(new IdentityKey(i));
    }
    ListActivitiesKey listKey = new ListActivitiesKey(new ListIdentitiesData(keyskeys), 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getActivitiesOfIdentities(connectionList, offset, limit);
            return buildIds(got ,null);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }

  @Override
  public int getNumberOfSpaceActivities(final Identity spaceIdentity) {
    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(spaceIdentity), ActivityType.SPACE);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfSpaceActivities(spaceIdentity));
          }
        },
        key)
        .build();
  }
  
  @Override
  public int getNumberOfSpaceActivitiesForUpgrade(final Identity spaceIdentity) {
    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(spaceIdentity), ActivityType.SPACE_FOR_UPGRADE);

    //
    IntegerData countData = activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfSpaceActivitiesForUpgrade(spaceIdentity));
          }
        },
        key);
    
    ActivityCountKey keySpace =
        new ActivityCountKey(new IdentityKey(spaceIdentity), ActivityType.SPACE);
    exoActivitiesCountCache.put(keySpace, countData);
    
    return countData.build();
  }

  @Override
  public List<ExoSocialActivity> getSpaceActivities(final Identity ownerIdentity, final int offset, final int limit) {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.SPACE);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getSpaceActivities(ownerIdentity, offset, limit);
            ActivityCountKey sinceTimeKey = new ActivityCountKey(new IdentityKey(ownerIdentity), 
                                                                 ActivityType.SPACE, (long) (offset + limit));
            return buildIds(got, sinceTimeKey);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }
  
  @Override
  public List<ExoSocialActivity> getSpaceActivitiesForUpgrade(final Identity ownerIdentity, final int offset, final int limit) {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.SPACE);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getSpaceActivitiesForUpgrade(ownerIdentity, offset, limit);
            ActivityCountKey sinceTimeKey = new ActivityCountKey(new IdentityKey(ownerIdentity), 
                                                                 ActivityType.SPACE, (long) (offset + limit));
            return buildIds(got, sinceTimeKey);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }

  @Override
  public List<ExoSocialActivity> getActivitiesByPoster(final Identity posterIdentity, 
                                                       final int offset, 
                                                       final int limit) {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(posterIdentity), ActivityType.POSTER);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getActivitiesByPoster(posterIdentity, offset, limit);
            ActivityCountKey sinceTimeKey = new ActivityCountKey(new IdentityKey(posterIdentity), 
                                                                 ActivityType.POSTER, (long) (offset + limit));
            return buildIds(got, sinceTimeKey);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }
  
  @Override
  public List<ExoSocialActivity> getActivitiesByPoster(final Identity posterIdentity, 
                                                       final int offset, 
                                                       final int limit,
                                                       final String...activityTypes) {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(posterIdentity), ActivityType.POSTER, activityTypes);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getActivitiesByPoster(posterIdentity, offset, limit, activityTypes);
            ActivityCountKey sinceTimeKey = new ActivityCountKey(new IdentityKey(posterIdentity), 
                                                                 ActivityType.POSTER, (long) (offset + limit));
            return buildIds(got, sinceTimeKey);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }
  
  @Override
  public int getNumberOfActivitiesByPoster(final Identity posterIdentity) {
    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(posterIdentity), ActivityType.POSTER);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfActivitiesByPoster(posterIdentity));
          }
        },
        key)
        .build();
  }
  
  @Override
  public int getNumberOfActivitiesByPoster(final Identity ownerIdentity, final Identity viewerIdentity) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), new IdentityKey(viewerIdentity), ActivityType.POSTER);
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfActivitiesByPoster(ownerIdentity, viewerIdentity));
          }
        },
        key)
        .build();
  }
  
  @Override
  public List<ExoSocialActivity> getNewerOnSpaceActivities(final Identity ownerIdentity,
                                                           final ExoSocialActivity baseActivity,
                                                           final int limit) {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.NEWER_SPACE);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getNewerOnSpaceActivities(ownerIdentity, baseActivity, limit);
            return buildIds(got, null);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }

  @Override
  public int getNumberOfNewerOnSpaceActivities(final Identity ownerIdentity,
                                               final ExoSocialActivity baseActivity) {
    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.NEWER_SPACE);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfNewerOnSpaceActivities(ownerIdentity, baseActivity));
          }
        },
        key)
        .build();
  }

  @Override
  public List<ExoSocialActivity> getOlderOnSpaceActivities(final Identity ownerIdentity,
                                                            final ExoSocialActivity baseActivity,
                                                            final int limit) {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.OLDER_SPACE);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getOlderOnSpaceActivities(ownerIdentity, baseActivity, limit);
            return buildIds(got, null);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }

  @Override
  public int getNumberOfOlderOnSpaceActivities(final Identity ownerIdentity,
                                               final ExoSocialActivity baseActivity) {
    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.OLDER_SPACE);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfOlderOnUserSpacesActivities(ownerIdentity, baseActivity));
          }
        },
        key)
        .build();
  }

  @Override
  public int getNumberOfNewerOnSpaceActivities(final Identity ownerIdentity, final Long sinceTime) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity),
                                                sinceTime,
                                                ActivityType.NEWER_SPACE);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(mongoStorage.getNumberOfNewerOnUserSpacesActivities(ownerIdentity,
                                                                              sinceTime));
      }
    }, key).build();
  }

  @Override
  public int getNumberOfUpdatedOnActivityFeed(Identity owner, ActivityUpdateFilter filter) {
    return mongoStorage.getNumberOfUpdatedOnActivityFeed(owner, filter);
  }

  @Override
  public int getNumberOfMultiUpdated(Identity owner, Map<String, Long> sinceTimes) {
    return mongoStorage.getNumberOfMultiUpdated(owner, sinceTimes);
  }
  
  public List<ExoSocialActivity> getNewerFeedActivities(final Identity owner, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), sinceTime, ActivityType.NEWER_FEED);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = mongoStorage.getNewerFeedActivities(owner, sinceTime, limit);
        return buildIds(got, null);
      }
    }, listKey);

    return buildActivities(keys);
  }
  
  public List<ExoSocialActivity> getNewerSpaceActivities(final Identity owner, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), sinceTime, ActivityType.NEWER_SPACE);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = mongoStorage.getNewerSpaceActivities(owner, sinceTime, limit);
        return buildIds(got, null);
      }
    }, listKey);

    return buildActivities(keys);
  }

  @Override
  public List<ExoSocialActivity> getNewerUserActivities(final Identity owner, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), sinceTime, ActivityType.NEWER_USER);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = mongoStorage.getNewerUserActivities(owner, sinceTime, limit);
        return buildIds(got, null);
      }
    }, listKey);

    return buildActivities(keys);
  }

  @Override
  public List<ExoSocialActivity> getNewerUserSpacesActivities(final Identity owner, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), sinceTime, ActivityType.NEWER_SPACES);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = mongoStorage.getNewerUserSpacesActivities(owner, sinceTime, limit);
        return buildIds(got, null);
      }
    }, listKey);

    return buildActivities(keys);
  }
  
  @Override
  public List<ExoSocialActivity> getNewerActivitiesOfConnections(final Identity owner, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), sinceTime, ActivityType.NEWER_CONNECTION);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = mongoStorage.getNewerActivitiesOfConnections(owner, sinceTime, limit);
        return buildIds(got, null);
      }
    }, listKey);

    return buildActivities(keys);
  }
  
  @Override
  public int getNumberOfUpdatedOnUserActivities(Identity owner, ActivityUpdateFilter filter) {
    return mongoStorage.getNumberOfUpdatedOnUserActivities(owner, filter);
  }

  @Override
  public int getNumberOfUpdatedOnActivitiesOfConnections(Identity owner, ActivityUpdateFilter filter) {
    return mongoStorage.getNumberOfUpdatedOnActivitiesOfConnections(owner, filter);
  }

  @Override
  public int getNumberOfUpdatedOnUserSpacesActivities(Identity owner, ActivityUpdateFilter filter) {
    return mongoStorage.getNumberOfUpdatedOnUserSpacesActivities(owner, filter);
  }

  @Override
  public int getNumberOfUpdatedOnSpaceActivities(Identity owner, ActivityUpdateFilter filter) {
    return mongoStorage.getNumberOfUpdatedOnSpaceActivities(owner, filter);
  }

  @Override
  public List<ExoSocialActivity> getActivities(final Identity owner,
                                               final Identity viewer,
                                               final long offset,
                                               final long limit) throws ActivityStorageException {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), new IdentityKey(viewer), ActivityType.VIEWER);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getActivities(owner, viewer, offset, limit);
            ActivityCountKey sinceTimeKey = new ActivityCountKey(new IdentityKey(owner), 
                                                                 ActivityType.VIEWER, (long) (offset + limit));
            return buildIds(got, sinceTimeKey);
          }
        },
        listKey);

    //
    return buildActivities(keys);
    
  }

  @Override
  public List<ExoSocialActivity> getOlderFeedActivities(final Identity owner, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), sinceTime, ActivityType.OLDER_FEED);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = mongoStorage.getOlderFeedActivities(owner, sinceTime, limit);
        return buildIds(got, null);
      }
    }, listKey);

    return buildActivities(keys);
  }

  @Override
  public List<ExoSocialActivity> getOlderUserActivities(final Identity ownerIdentity, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), sinceTime, ActivityType.OLDER_USER);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = mongoStorage.getOlderUserActivities(ownerIdentity, sinceTime, limit);
        return buildIds(got, null);
      }
    }, listKey);

    return buildActivities(keys);
  }

  @Override
  public List<ExoSocialActivity> getOlderUserSpacesActivities(final Identity ownerIdentity, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), sinceTime, ActivityType.OLDER_SPACES);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = mongoStorage.getOlderUserSpacesActivities(ownerIdentity, sinceTime, limit);
        return buildIds(got, null);
      }
    }, listKey);
    
    return buildActivities(keys);
  }

  @Override
  public List<ExoSocialActivity> getOlderActivitiesOfConnections(final Identity owner, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), sinceTime, ActivityType.OLDER_CONNECTION);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = mongoStorage.getOlderActivitiesOfConnections(owner, sinceTime, limit);
        return buildIds(got, null);
      }
    }, listKey);
    
    return buildActivities(keys);
  }

  @Override
  public List<ExoSocialActivity> getOlderSpaceActivities(final Identity owner, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), sinceTime, ActivityType.OLDER_SPACE);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = mongoStorage.getOlderSpaceActivities(owner, sinceTime, limit);
        return buildIds(got, null);
      }
    }, listKey);
    
    return buildActivities(keys);
  }

  @Override
  public int getNumberOfOlderOnActivityFeed(final Identity ownerIdentity, final Long sinceTime) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity),
                                                sinceTime,
                                                ActivityType.OLDER_FEED);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(mongoStorage.getNumberOfOlderOnActivityFeed(ownerIdentity, sinceTime));
      }
    }, key).build();
  }

  @Override
  public int getNumberOfOlderOnUserActivities(final Identity ownerIdentity, final Long sinceTime) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity),
                                                sinceTime,
                                                ActivityType.OLDER_USER);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(mongoStorage.getNumberOfOlderOnUserActivities(ownerIdentity, sinceTime));
      }
    }, key).build();
  }

  @Override
  public int getNumberOfOlderOnActivitiesOfConnections(final Identity ownerIdentity, final Long sinceTime) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity),
                                                sinceTime,
                                                ActivityType.OLDER_CONNECTION);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(mongoStorage.getNumberOfOlderOnActivitiesOfConnections(ownerIdentity, sinceTime));
      }
    }, key).build();
  }

  @Override
  public int getNumberOfOlderOnUserSpacesActivities(final Identity ownerIdentity, final Long sinceTime) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity),
                                                sinceTime,
                                                ActivityType.OLDER_SPACES);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(mongoStorage.getNumberOfOlderOnUserSpacesActivities(ownerIdentity, sinceTime));
      }
    }, key).build();
  }

  @Override
  public int getNumberOfOlderOnSpaceActivities(final Identity ownerIdentity, final Long sinceTime) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity),
                                                sinceTime,
                                                ActivityType.OLDER_SPACE);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(mongoStorage.getNumberOfOlderOnSpaceActivities(ownerIdentity, sinceTime));
      }
    }, key).build();
  }

  @Override
  public List<ExoSocialActivity> getNewerComments(final ExoSocialActivity existingActivity, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new ActivityKey(existingActivity.getId()), sinceTime, ActivityType.NEWER_COMMENTS);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = mongoStorage.getNewerComments(existingActivity, sinceTime, limit);
        return buildIds(got, null);
      }
    }, listKey);
    
    return buildActivities(keys);
  }

  @Override
  public List<ExoSocialActivity> getOlderComments(final ExoSocialActivity existingActivity, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new ActivityKey(existingActivity.getId()), sinceTime, ActivityType.OLDER_COMMENTS);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = mongoStorage.getOlderComments(existingActivity, sinceTime, limit);
        return buildIds(got, null);
      }
    }, listKey);
    
    return buildActivities(keys);
  }

  @Override
  public int getNumberOfNewerComments(final ExoSocialActivity existingActivity, final Long sinceTime) {
    ActivityCountKey key = new ActivityCountKey(new ActivityKey(existingActivity.getId()), sinceTime, ActivityType.NEWER_COMMENTS);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(mongoStorage.getNumberOfNewerComments(existingActivity, sinceTime));
      }
    }, key).build();
  }

  @Override
  public int getNumberOfOlderComments(final ExoSocialActivity existingActivity, final Long sinceTime) {
    ActivityCountKey key = new ActivityCountKey(new ActivityKey(existingActivity.getId()), sinceTime, ActivityType.OLDER_COMMENTS);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(mongoStorage.getNumberOfOlderComments(existingActivity, sinceTime));
      }
    }, key).build();
  }

  @Override
  public List<ExoSocialActivity> getUserActivitiesForUpgrade(final Identity owner, final long offset, final long limit) throws ActivityStorageException {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), ActivityType.USER);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getUserActivitiesForUpgrade(owner, offset, limit);
            return buildIds(got, null);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }

  @Override
  public int getNumberOfUserActivitiesForUpgrade(final Identity owner) throws ActivityStorageException {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), ActivityType.USER);

    //
    IntegerData countData =  activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfUserActivitiesForUpgrade(owner));
          }
        },
        key);
    
    //
    ActivityCountKey keyUser =
        new ActivityCountKey(new IdentityKey(owner), ActivityType.USER);
    exoActivitiesCountCache.put(keyUser, countData);
    
    //
    return countData.build();
    
  }

  @Override
  public List<ExoSocialActivity> getActivityFeedForUpgrade(final Identity ownerIdentity,
                                                           final int offset,
                                                           final int limit) {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.FEED);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getActivityFeedForUpgrade(ownerIdentity, offset, limit);
            return buildIds(got, null);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }

  @Override
  public int getNumberOfActivitesOnActivityFeedForUpgrade(final Identity ownerIdentity) {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.FEED);
    
    //
    IntegerData countData = activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfActivitesOnActivityFeed(ownerIdentity));
          }
        },
        key);
        
    //
    ActivityCountKey keyFeed =
        new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.FEED);
    exoActivitiesCountCache.put(keyFeed, countData);
    
    //
    return countData.build();
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfConnectionsForUpgrade(final Identity ownerIdentity,
                                                                      final int offset,
                                                                      final int limit) {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.CONNECTION);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getActivitiesOfConnectionsForUpgrade(ownerIdentity, offset, limit);
            return buildIds(got, null);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }

  @Override
  public int getNumberOfActivitiesOfConnectionsForUpgrade(final Identity ownerIdentity) {
    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.CONNECTION);

    //
    IntegerData countData = activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfActivitiesOfConnectionsForUpgrade(ownerIdentity));
          }
        },
        key);
    
    //
    ActivityCountKey keyConnection =
        new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.CONNECTION);
    exoActivitiesCountCache.put(keyConnection, countData);
    
    //
    return countData.build();
  }

  @Override
  public List<ExoSocialActivity> getUserSpacesActivitiesForUpgrade(final Identity ownerIdentity,
                                                                   final int offset,
                                                                   final int limit) {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.SPACES);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = mongoStorage.getUserSpacesActivitiesForUpgrade(ownerIdentity, offset, limit);
            return buildIds(got, null);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }

  @Override
  public int getNumberOfUserSpacesActivitiesForUpgrade(final Identity ownerIdentity) {
    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.SPACES);

    //
    IntegerData countData = activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(mongoStorage.getNumberOfUserSpacesActivitiesForUpgrade(ownerIdentity));
          }
        },
        key);
    
    ActivityCountKey keySpaces =
        new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.SPACES);
    exoActivitiesCountCache.put(keySpaces, countData);
    
    return countData.build();
  }

  @Override
  public void setInjectStreams(boolean mustInject) {
    mongoStorage.setInjectStreams(mustInject);
    
  }

  public ExoSocialActivity getComment(final String commentId) throws ActivityStorageException {
    //
    ActivityKey key = new ActivityKey(commentId);

    //
    ActivityData activity = activityCache.get(
        new ServiceContext<ActivityData>() {
          public ActivityData execute() {
            try {
              ExoSocialActivity got = mongoStorage.getComment(commentId);
              if (got != null) {
                return new ActivityData(got);
              }
              else {
                return ActivityData.NULL;
              }
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        },
        key);

    //
    return activity.build();
  }

  @Override
  public long getSinceTime(Identity owner, long offset, ActivityType type) throws ActivityStorageException {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), type, offset);
    Long sinceTime = activitySinceTimeCache.get(key);
    return sinceTime == null ? 0 : sinceTime;
  }
  
}
