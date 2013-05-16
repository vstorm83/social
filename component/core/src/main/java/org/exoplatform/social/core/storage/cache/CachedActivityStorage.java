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

  private final FutureExoCache<ActivityKey, ActivityData, ServiceContext<ActivityData>> activityCache;
  private final FutureExoCache<ActivityCountKey, IntegerData, ServiceContext<IntegerData>> activitiesCountCache;
  private final FutureExoCache<ListActivitiesKey, ListActivitiesData, ServiceContext<ListActivitiesData>> activitiesCache;

  private final ActivityStorageImpl storage;

  public void clearCache() {

    try {
      exoActivitiesCache.select(new ScopeCacheSelector<ListActivitiesKey, ListActivitiesData>());
      exoActivitiesCountCache.select(new ScopeCacheSelector<ActivityCountKey, IntegerData>());
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
   * @return ids
   */
  private ListActivitiesData buildIds(List<ExoSocialActivity> activities) {

    List<ActivityKey> data = new ArrayList<ActivityKey>();
    for (ExoSocialActivity a : activities) {
      ActivityKey k = new ActivityKey(a.getId());
      exoActivityCache.put(k, new ActivityData(a));
      data.add(k);
    }
    return new ListActivitiesData(data);

  }

  public CachedActivityStorage(final ActivityStorageImpl storage, final SocialStorageCacheService cacheService) {

    //
    this.storage = storage;
    this.storage.setStorage(this);

    //
    this.exoActivityCache = cacheService.getActivityCache();
    this.exoActivitiesCountCache = cacheService.getActivitiesCountCache();
    this.exoActivitiesCache = cacheService.getActivitiesCache();

    //
    this.activityCache = CacheType.ACTIVITY.createFutureCache(exoActivityCache);
    this.activitiesCountCache = CacheType.ACTIVITIES_COUNT.createFutureCache(exoActivitiesCountCache);
    this.activitiesCache = CacheType.ACTIVITIES.createFutureCache(exoActivitiesCache);

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
              ExoSocialActivity got = storage.getActivity(activityId);
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
    return storage.getUserActivities(owner);
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
            List<ExoSocialActivity> got = storage.getUserActivities(owner, offset, limit);
            return buildIds(got);
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
    storage.saveComment(activity, comment);

    //
    exoActivityCache.put(new ActivityKey(comment.getId()), new ActivityData(getActivity(comment.getId())));
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
    ExoSocialActivity a = storage.saveActivity(owner, activity);

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
    return storage.getParentActivity(comment);
  }

  /**
   * {@inheritDoc}
   */
  public void deleteActivity(final String activityId) throws ActivityStorageException {

    //
    ExoSocialActivity a = storage.getActivity(activityId);
    storage.deleteActivity(activityId);

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
    storage.deleteComment(activityId, commentId);

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
            List<ExoSocialActivity> got = storage.getActivitiesOfIdentities(connectionList, offset, limit);
            return buildIds(got);
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
    return storage.getActivitiesOfIdentities(connectionList, type, offset, limit);
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
            return new IntegerData(storage.getNumberOfUserActivities(owner));
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
            return new IntegerData(storage.getNumberOfNewerOnUserActivities(ownerIdentity, baseActivity));
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
            List<ExoSocialActivity> got = storage.getNewerOnUserActivities(ownerIdentity, baseActivity, limit);
            return buildIds(got);
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
            return new IntegerData(storage.getNumberOfOlderOnUserActivities(ownerIdentity, baseActivity));
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
            List<ExoSocialActivity> got = storage.getOlderOnUserActivities(ownerIdentity, baseActivity, limit);
            return buildIds(got);
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
            List<ExoSocialActivity> got = storage.getActivityFeed(ownerIdentity, offset, limit);
            return buildIds(got);
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
            return new IntegerData(storage.getNumberOfActivitesOnActivityFeed(ownerIdentity));
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
            return new IntegerData(storage.getNumberOfNewerOnActivityFeed(ownerIdentity, baseActivity));
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
            List<ExoSocialActivity> got = storage.getNewerOnActivityFeed(ownerIdentity, baseActivity, limit);
            return buildIds(got);
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
            return new IntegerData(storage.getNumberOfOlderOnActivityFeed(ownerIdentity, baseActivity));
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
            List<ExoSocialActivity> got = storage.getOlderOnActivityFeed(ownerIdentity, baseActivity, limit);
            return buildIds(got);
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
            List<ExoSocialActivity> got = storage.getActivitiesOfConnections(ownerIdentity, offset, limit);
            return buildIds(got);
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
            return new IntegerData(storage.getNumberOfActivitiesOfConnections(ownerIdentity));
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
    return storage.getActivitiesOfIdentity(ownerIdentity, offset, limit);
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
            return new IntegerData(storage.getNumberOfNewerOnActivitiesOfConnections(ownerIdentity, baseActivity));
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
            List<ExoSocialActivity> got = storage.getNewerOnActivitiesOfConnections(ownerIdentity, baseActivity, limit);
            return buildIds(got);
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
            return new IntegerData(storage.getNumberOfOlderOnActivitiesOfConnections(ownerIdentity, baseActivity));
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
            List<ExoSocialActivity> got = storage.getOlderOnActivitiesOfConnections(ownerIdentity, baseActivity, limit);
            return buildIds(got);
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
            List<ExoSocialActivity> got = storage.getUserSpacesActivities(ownerIdentity, offset, limit);
            return buildIds(got);
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
            return new IntegerData(storage.getNumberOfUserSpacesActivities(ownerIdentity));
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
            return new IntegerData(storage.getNumberOfNewerOnUserSpacesActivities(ownerIdentity, baseActivity));
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
            List<ExoSocialActivity> got = storage.getNewerOnUserSpacesActivities(ownerIdentity, baseActivity, limit);
            return buildIds(got);
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
            return new IntegerData(storage.getNumberOfOlderOnUserSpacesActivities(ownerIdentity, baseActivity));
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
            List<ExoSocialActivity> got = storage.getOlderOnUserSpacesActivities(ownerIdentity, baseActivity, limit);
            return buildIds(got);
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
    return storage.getComments(existingActivity, offset, limit);
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfComments(final ExoSocialActivity existingActivity) {
    return storage.getNumberOfComments(existingActivity);
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfNewerComments(final ExoSocialActivity existingActivity, final ExoSocialActivity baseComment) {
    return storage.getNumberOfNewerComments(existingActivity, baseComment);
  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getNewerComments(final ExoSocialActivity existingActivity, final ExoSocialActivity baseComment,
                                                  final int limit) {
    return storage.getNewerComments(existingActivity, baseComment, limit);
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfOlderComments(final ExoSocialActivity existingActivity, final ExoSocialActivity baseComment) {
    return storage.getNumberOfOlderComments(existingActivity, baseComment);
  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getOlderComments(final ExoSocialActivity existingActivity, final ExoSocialActivity baseComment,
                                                  final int limit) {
    return storage.getOlderComments(existingActivity, baseComment, limit);
  }

  /**
   * {@inheritDoc}
   */
  public SortedSet<ActivityProcessor> getActivityProcessors() {
    return storage.getActivityProcessors();
  }

  /**
   * {@inheritDoc}
   */
  public void updateActivity(final ExoSocialActivity existingActivity) throws ActivityStorageException {

    //
    storage.updateActivity(existingActivity);
    
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
        return new IntegerData(storage.getNumberOfNewerOnActivityFeed(ownerIdentity, sinceTime));
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
        return new IntegerData(storage.getNumberOfNewerOnUserActivities(ownerIdentity, sinceTime));
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
        return new IntegerData(storage.getNumberOfNewerOnActivitiesOfConnections(ownerIdentity, sinceTime));
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
        return new IntegerData(storage.getNumberOfNewerOnUserSpacesActivities(ownerIdentity,
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
            List<ExoSocialActivity> got = storage.getActivitiesOfIdentities(connectionList, offset, limit);
            return buildIds(got);
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
            return new IntegerData(storage.getNumberOfSpaceActivities(spaceIdentity));
          }
        },
        key)
        .build();
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
            List<ExoSocialActivity> got = storage.getSpaceActivities(ownerIdentity, offset, limit);
            return buildIds(got);
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
            List<ExoSocialActivity> got = storage.getActivitiesByPoster(posterIdentity, offset, limit);
            return buildIds(got);
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
            return new IntegerData(storage.getNumberOfActivitiesByPoster(posterIdentity));
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
            List<ExoSocialActivity> got = storage.getNewerOnSpaceActivities(ownerIdentity, baseActivity, limit);
            return buildIds(got);
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
            return new IntegerData(storage.getNumberOfNewerOnSpaceActivities(ownerIdentity, baseActivity));
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
            List<ExoSocialActivity> got = storage.getOlderOnSpaceActivities(ownerIdentity, baseActivity, limit);
            return buildIds(got);
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
            return new IntegerData(storage.getNumberOfOlderOnUserSpacesActivities(ownerIdentity, baseActivity));
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
        return new IntegerData(storage.getNumberOfNewerOnUserSpacesActivities(ownerIdentity,
                                                                              sinceTime));
      }
    }, key).build();
  }

  @Override
  public int getNumberOfUpdatedOnActivityFeed(Identity owner, ActivityUpdateFilter filter) {
    return storage.getNumberOfUpdatedOnActivityFeed(owner, filter);
  }

  @Override
  public int getNumberOfMultiUpdated(Identity owner, Map<String, Long> sinceTimes) {
    return storage.getNumberOfMultiUpdated(owner, sinceTimes);
  }
  
  public List<ExoSocialActivity> getFeedActivities(Identity owner, Long sinceTime) {
    return storage.getFeedActivities(owner, sinceTime);
  }
  
  public List<ExoSocialActivity> getSpaceActivities(Identity owner, Long sinceTime) {
    return storage.getSpaceActivities(owner, sinceTime);
  }

  @Override
  public List<ExoSocialActivity> getUserActivities(Identity owner, Long sinceTime) {
    return storage.getUserActivities(owner, sinceTime);
  }

  @Override
  public List<ExoSocialActivity> getUserSpacesActivities(Identity owner, Long sinceTime) {
    return storage.getUserSpacesActivities(owner, sinceTime);
  }
  
  @Override
  public List<ExoSocialActivity> getActivitiesOfConnections(Identity owner, Long sinceTime) {
    return storage.getActivitiesOfConnections(owner, sinceTime);
  }
  
  @Override
  public int getNumberOfUpdatedOnUserActivities(Identity owner, ActivityUpdateFilter filter) {
    return storage.getNumberOfUpdatedOnUserActivities(owner, filter);
  }

  @Override
  public int getNumberOfUpdatedOnActivitiesOfConnections(Identity owner, ActivityUpdateFilter filter) {
    return storage.getNumberOfUpdatedOnActivitiesOfConnections(owner, filter);
  }

  @Override
  public int getNumberOfUpdatedOnUserSpacesActivities(Identity owner, ActivityUpdateFilter filter) {
    return storage.getNumberOfUpdatedOnUserSpacesActivities(owner, filter);
  }

  @Override
  public int getNumberOfUpdatedOnSpaceActivities(Identity owner, ActivityUpdateFilter filter) {
    return storage.getNumberOfUpdatedOnSpaceActivities(owner, filter);
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
            List<ExoSocialActivity> got = storage.getActivities(owner, viewer, offset, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);
    
  }
  
}
