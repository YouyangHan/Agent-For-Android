package com.agentforandroid.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.agentforandroid.data.local.dao.ConfigDao;
import com.agentforandroid.data.local.dao.ConfigDao_Impl;
import com.agentforandroid.data.local.dao.MessageDao;
import com.agentforandroid.data.local.dao.MessageDao_Impl;
import com.agentforandroid.data.local.dao.SessionDao;
import com.agentforandroid.data.local.dao.SessionDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile ConfigDao _configDao;

  private volatile SessionDao _sessionDao;

  private volatile MessageDao _messageDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `model_configs` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `modelId` TEXT NOT NULL, `apiKey` TEXT NOT NULL, `baseUrl` TEXT NOT NULL, `apiType` TEXT NOT NULL, `isDefault` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `chat_sessions` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `modelConfigId` TEXT NOT NULL, `enabledSkills` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `messages` (`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '13a6826a052bf54a4087c942ed41bd6c')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `model_configs`");
        db.execSQL("DROP TABLE IF EXISTS `chat_sessions`");
        db.execSQL("DROP TABLE IF EXISTS `messages`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsModelConfigs = new HashMap<String, TableInfo.Column>(7);
        _columnsModelConfigs.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsModelConfigs.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsModelConfigs.put("modelId", new TableInfo.Column("modelId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsModelConfigs.put("apiKey", new TableInfo.Column("apiKey", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsModelConfigs.put("baseUrl", new TableInfo.Column("baseUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsModelConfigs.put("apiType", new TableInfo.Column("apiType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsModelConfigs.put("isDefault", new TableInfo.Column("isDefault", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysModelConfigs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesModelConfigs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoModelConfigs = new TableInfo("model_configs", _columnsModelConfigs, _foreignKeysModelConfigs, _indicesModelConfigs);
        final TableInfo _existingModelConfigs = TableInfo.read(db, "model_configs");
        if (!_infoModelConfigs.equals(_existingModelConfigs)) {
          return new RoomOpenHelper.ValidationResult(false, "model_configs(com.agentforandroid.data.local.entity.ConfigEntity).\n"
                  + " Expected:\n" + _infoModelConfigs + "\n"
                  + " Found:\n" + _existingModelConfigs);
        }
        final HashMap<String, TableInfo.Column> _columnsChatSessions = new HashMap<String, TableInfo.Column>(5);
        _columnsChatSessions.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatSessions.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatSessions.put("modelConfigId", new TableInfo.Column("modelConfigId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatSessions.put("enabledSkills", new TableInfo.Column("enabledSkills", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatSessions.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysChatSessions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesChatSessions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoChatSessions = new TableInfo("chat_sessions", _columnsChatSessions, _foreignKeysChatSessions, _indicesChatSessions);
        final TableInfo _existingChatSessions = TableInfo.read(db, "chat_sessions");
        if (!_infoChatSessions.equals(_existingChatSessions)) {
          return new RoomOpenHelper.ValidationResult(false, "chat_sessions(com.agentforandroid.data.local.entity.SessionEntity).\n"
                  + " Expected:\n" + _infoChatSessions + "\n"
                  + " Found:\n" + _existingChatSessions);
        }
        final HashMap<String, TableInfo.Column> _columnsMessages = new HashMap<String, TableInfo.Column>(5);
        _columnsMessages.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("sessionId", new TableInfo.Column("sessionId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("role", new TableInfo.Column("role", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMessages = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMessages = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMessages = new TableInfo("messages", _columnsMessages, _foreignKeysMessages, _indicesMessages);
        final TableInfo _existingMessages = TableInfo.read(db, "messages");
        if (!_infoMessages.equals(_existingMessages)) {
          return new RoomOpenHelper.ValidationResult(false, "messages(com.agentforandroid.data.local.entity.MessageEntity).\n"
                  + " Expected:\n" + _infoMessages + "\n"
                  + " Found:\n" + _existingMessages);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "13a6826a052bf54a4087c942ed41bd6c", "5ce94403aa185cc048c9c7d50bd651ea");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "model_configs","chat_sessions","messages");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `model_configs`");
      _db.execSQL("DELETE FROM `chat_sessions`");
      _db.execSQL("DELETE FROM `messages`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(ConfigDao.class, ConfigDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SessionDao.class, SessionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MessageDao.class, MessageDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public ConfigDao configDao() {
    if (_configDao != null) {
      return _configDao;
    } else {
      synchronized(this) {
        if(_configDao == null) {
          _configDao = new ConfigDao_Impl(this);
        }
        return _configDao;
      }
    }
  }

  @Override
  public SessionDao sessionDao() {
    if (_sessionDao != null) {
      return _sessionDao;
    } else {
      synchronized(this) {
        if(_sessionDao == null) {
          _sessionDao = new SessionDao_Impl(this);
        }
        return _sessionDao;
      }
    }
  }

  @Override
  public MessageDao messageDao() {
    if (_messageDao != null) {
      return _messageDao;
    } else {
      synchronized(this) {
        if(_messageDao == null) {
          _messageDao = new MessageDao_Impl(this);
        }
        return _messageDao;
      }
    }
  }
}
