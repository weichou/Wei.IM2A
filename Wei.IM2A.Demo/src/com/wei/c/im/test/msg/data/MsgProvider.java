/*
 * Copyright (C) 2014 Wei Chou (weichou2010@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wei.c.im.test.msg.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import com.wei.c.L;
import com.wei.c.im.test.UID;
import com.wei.c.im.test.msg.client.MsgClient;

public class MsgProvider extends ContentProvider {
	private static final String DB_NAME					= "wei_im_msg.db";
	private static final int DB_VERSION					= 52;

	public static final String TABLE_SAY_HI				= MSG.DbColumns.SayHi.class.getSimpleName();
	public static final String TABLE_MSG_SEND			= MSG.DbColumns.MsgSend.class.getSimpleName();
	public static final String TABLE_MSG_RECEIVED		= MSG.DbColumns.MsgReceived.class.getSimpleName();
	public static final String TABLE_SEND_ENTITY		= MSG.DbColumns.SendEntity.class.getSimpleName();

	private static final UriMatcher sURIMatcher			= new UriMatcher(UriMatcher.NO_MATCH);
	private static final String FRAGMENT				= "/#";

	public static final int SAY_HI						= 1;
	public static final int SAY_HI_ID					= 2;
	public static final int MSG_SEND					= 3;
	public static final int MSG_SEND_ID					= 4;
	public static final int MSG_RECEIVED				= 5;
	public static final int MSG_RECEIVED_ID				= 6;
	public static final int ARRIVE_STATE				= 10;
	public static final int ARRIVE_STATE_ID				= 11;
	public static final int SEND_ENTITY					= 13;
	public static final int SEND_ENTITY_ID				= 14;
	public static final int MSG_SEND_ENTITY_ID			= 15;

	static {
		sURIMatcher.addURI(MSG.AUTHORITY, MSG.PATH_SAY_HI, SAY_HI);
		sURIMatcher.addURI(MSG.AUTHORITY, MSG.PATH_SAY_HI + FRAGMENT, SAY_HI_ID);
		sURIMatcher.addURI(MSG.AUTHORITY, MSG.PATH_MSG_SEND, MSG_SEND);
		sURIMatcher.addURI(MSG.AUTHORITY, MSG.PATH_MSG_SEND + FRAGMENT, MSG_SEND_ID);
		sURIMatcher.addURI(MSG.AUTHORITY, MSG.PATH_MSG_RECEIVED, MSG_RECEIVED);
		sURIMatcher.addURI(MSG.AUTHORITY, MSG.PATH_MSG_RECEIVED + FRAGMENT, MSG_RECEIVED_ID);
		sURIMatcher.addURI(MSG.AUTHORITY, MSG.PATH_ARRIVE_STATE, ARRIVE_STATE);
		sURIMatcher.addURI(MSG.AUTHORITY, MSG.PATH_ARRIVE_STATE + FRAGMENT, ARRIVE_STATE_ID);
		sURIMatcher.addURI(MSG.AUTHORITY, MSG.PATH_SEND_ENTITY, SEND_ENTITY);
		sURIMatcher.addURI(MSG.AUTHORITY, MSG.PATH_SEND_ENTITY + FRAGMENT, SEND_ENTITY_ID);
		sURIMatcher.addURI(MSG.AUTHORITY, MSG.PATH_MSG_SEND_ENTITY_ID + FRAGMENT, MSG_SEND_ENTITY_ID);
	}

	public static int match(Uri uri) {
		return sURIMatcher.match(uri);
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public String getType(Uri uri) {
		throw new IllegalArgumentException("不支持 MIME type");
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		MSG.checkPermission(getContext());
		String table;
		long id;
		switch (match(uri)) {
		case SAY_HI:
			table = TABLE_SAY_HI;
			return getReadableDatabase().query(true, table, projection, selection, selectionArgs, null, null, sortOrder, null);
		case SAY_HI_ID:
			table = TABLE_SAY_HI;
			id = ContentUris.parseId(uri);
			return getReadableDatabase().query(true, table, projection, MSG.DbColumns.SayHi._ID + "=" + id, null, null, null, null, null);
		case MSG_SEND:
			table = TABLE_MSG_SEND;
			return getReadableDatabase().query(true, table, projection, selection, selectionArgs, null, null, sortOrder, null);
		case MSG_SEND_ID:
			table = TABLE_MSG_SEND;
			id = ContentUris.parseId(uri);
			return getReadableDatabase().query(true, table, projection, MSG.DbColumns.MsgSend._ID + "=" + id, null, null, null, null, null);
		case MSG_RECEIVED:
			table = TABLE_MSG_RECEIVED;
			return getReadableDatabase().query(true, table, projection, selection, selectionArgs, null, null, sortOrder, null);
		case MSG_RECEIVED_ID:
			table = TABLE_MSG_RECEIVED;
			id = ContentUris.parseId(uri);
			return getReadableDatabase().query(true, table, projection, MSG.DbColumns.MsgReceived._ID + "=" + id, null, null, null, null, null);
		case ARRIVE_STATE:
			table = TABLE_MSG_SEND;
			return getReadableDatabase().query(true, table, projection, selection, selectionArgs, null, null, sortOrder, null);
		case ARRIVE_STATE_ID:
			table = TABLE_MSG_SEND;
			id = ContentUris.parseId(uri);
			return getReadableDatabase().query(true, table, projection, MSG.DbColumns.MsgSend._ID + "=" + id, null, null, null, null, null);
		case SEND_ENTITY:
			table = TABLE_SEND_ENTITY;
			return getReadableDatabase().query(true, table, projection, selection, selectionArgs, null, null, sortOrder, null);
		case SEND_ENTITY_ID:
			table = TABLE_SEND_ENTITY;
			id = ContentUris.parseId(uri);
			return getReadableDatabase().query(true, table, projection, MSG.DbColumns.SendEntity._ID + "=" + id, null, null, null, null, null);
		default:
			throw new IllegalArgumentException("不支持该 Uri 的查询操作：" + uri.toString());
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		MSG.checkPermission(getContext());

		Long id_me = values.getAsLong(MSG.DbColumns._ID_ME);
		Long id_u = values.getAsLong(MSG.DbColumns.MsgSend._ID_U);

		if (id_me == null || !UID.isValid(id_me)
				|| id_u == null || !UID.isValid(id_u)) throw new IllegalArgumentException("必须包含有效的 _ID_ME 和 _ID_U 字段值" +
						"-----_ID_ME:" + id_me + ", _ID_U:" + id_u);

		String table;
		switch (sURIMatcher.match(uri)) {
		case SAY_HI:
			table = TABLE_SAY_HI;
			break;
		case MSG_SEND:
			table = TABLE_MSG_SEND;
			break;
		case MSG_RECEIVED:
			table = TABLE_MSG_RECEIVED;
			break;
		case SEND_ENTITY:
			table = TABLE_SEND_ENTITY;
			break;
		default:
			throw new IllegalArgumentException("不支持该 Uri 的插入操作：" + uri.toString());
		}
		L.d(MsgClient.class, "mContentObserver---MsgProvider insert------uri:" + uri);
		Uri iduri = ContentUris.withAppendedId(uri, getWritableDatabase().insert(table, null, values));
		//if(table.equals(TABLE_SEND_ENTITY)) {
		//全部通知
		getContext().getContentResolver().notifyChange(iduri, null);
		//}
		return iduri;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		MSG.checkPermission(getContext());
		L.d(MsgClient.class, "mContentObserver---MsgProvider delete------uri:" + uri);
		String table;
		long id;
		int rows;
		switch (sURIMatcher.match(uri)) {
		case SAY_HI:
			table = TABLE_SAY_HI;
			rows = getWritableDatabase().delete(table, selection, selectionArgs);
			getContext().getContentResolver().notifyChange(uri, null);
			return rows;
		case SAY_HI_ID:
			table = TABLE_SAY_HI;
			id = ContentUris.parseId(uri);
			rows = getWritableDatabase().delete(table, MSG.DbColumns.SayHi._ID + "=" + id, null);
			getContext().getContentResolver().notifyChange(uri, null);
			return rows;
		case MSG_SEND:
			table = TABLE_MSG_SEND;
			rows = getWritableDatabase().delete(table, selection, selectionArgs);
			getContext().getContentResolver().notifyChange(uri, null);
			return rows;
		case MSG_SEND_ID:
			table = TABLE_MSG_SEND;
			id = ContentUris.parseId(uri);
			rows = getWritableDatabase().delete(table, MSG.DbColumns.MsgSend._ID + "=" + id, null);
			getContext().getContentResolver().notifyChange(uri, null);
			return rows;
		case MSG_RECEIVED:
			table = TABLE_MSG_RECEIVED;
			rows = getWritableDatabase().delete(table, selection, selectionArgs);
			getContext().getContentResolver().notifyChange(uri, null);
			return rows;
		case MSG_RECEIVED_ID:
			table = TABLE_MSG_RECEIVED;
			id = ContentUris.parseId(uri);
			rows = getWritableDatabase().delete(table, MSG.DbColumns.MsgReceived._ID + "=" + id, null);
			getContext().getContentResolver().notifyChange(uri, null);
			return rows;
			/*case ARRIVE_STATE:	//由于在MSG_SEND表里，不可以删除
			table = TABLE_MSG_SEND;*/
		case SEND_ENTITY_ID:
			table = TABLE_SEND_ENTITY;
			id = ContentUris.parseId(uri);
			rows = getWritableDatabase().delete(table, MSG.DbColumns.SendEntity._ID + "=" + id, null);
			getContext().getContentResolver().notifyChange(uri, null);
			return rows;
		default:
			throw new IllegalArgumentException("不支持该 Uri 的删除操作：" + uri.toString());
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		MSG.checkPermission(getContext());

		Long _id = values.getAsLong(MSG.DbColumns._ID);
		Long id_me = values.getAsLong(MSG.DbColumns._ID_ME);
		Long id_u = values.getAsLong(MSG.DbColumns._ID_U);
		if (_id != null || id_me != null || id_u != null) throw new IllegalArgumentException("不应该更新字段 _ID_ME 或 _ID_U");
		L.d(MsgClient.class, "mContentObserver---MsgProvider update------uri:" + uri);

		String table;
		long id = -1;
		int rows = 0;
		switch (sURIMatcher.match(uri)) {
		case SAY_HI:
			table = TABLE_SAY_HI;
			rows = getWritableDatabase().update(table, values, selection, selectionArgs);
			break;
		case SAY_HI_ID:
			table = TABLE_SAY_HI;
			id = ContentUris.parseId(uri);
			rows = getWritableDatabase().update(table, values, MSG.DbColumns.SayHi._ID + "=" + id, null);
			break;
		case MSG_SEND:
			table = TABLE_MSG_SEND;
			rows = getWritableDatabase().update(table, values, selection, selectionArgs);
			break;
		case MSG_SEND_ID:
			table = TABLE_MSG_SEND;
			id = ContentUris.parseId(uri);
			rows = getWritableDatabase().update(table, values, MSG.DbColumns.MsgSend._ID + "=" + id, null);
			break;
		case MSG_RECEIVED:
			table = TABLE_MSG_RECEIVED;
			rows = getWritableDatabase().update(table, values, selection, selectionArgs);
			break;
		case MSG_RECEIVED_ID:
			table = TABLE_MSG_RECEIVED;
			id = ContentUris.parseId(uri);
			rows = getWritableDatabase().update(table, values, MSG.DbColumns.MsgReceived._ID + "=" + id, null);
			break;
		case ARRIVE_STATE:
			table = TABLE_MSG_SEND;
			checkAndAdjustArriveStateValues(uri, values);
			rows = getWritableDatabase().update(table, values, selection, selectionArgs);
			break;
		case ARRIVE_STATE_ID:
			table = TABLE_MSG_SEND;
			checkAndAdjustArriveStateValues(uri, values);
			id = ContentUris.parseId(uri);
			rows = getWritableDatabase().update(table, values, MSG.DbColumns.MsgSend._ID + "=" + id, null);
			break;
		case SEND_ENTITY:
			table = TABLE_SEND_ENTITY;
			checkAndAdjustSendJsonValues(uri, values);
			rows = getWritableDatabase().update(table, values, selection, selectionArgs);
			break;
		case SEND_ENTITY_ID:
			table = TABLE_SEND_ENTITY;
			checkAndAdjustSendJsonValues(uri, values);
			id = ContentUris.parseId(uri);
			rows = getWritableDatabase().update(table, values, MSG.DbColumns.SendEntity._ID + "=" + id, null);
			break;
		case MSG_SEND_ENTITY_ID:	//这个比较特殊，只是更新一个外键id
			table = TABLE_MSG_SEND;
			id = ContentUris.parseId(uri);
			rows = getWritableDatabase().update(table, values, MSG.DbColumns.SendEntity._ID + "=" + id, null);
			return rows;
		default:
			throw new IllegalArgumentException("不支持该 Uri 的更新操作：" + uri.toString());
		}
		if (rows > 0) {
			if (id < 0 && rows == 1) {	//id == 0也是解析出来的，说明带了id，就不用再查了
				Cursor cursor = query(uri, new String[]{MSG.DbColumns._ID}, selection, selectionArgs, null);
				if (cursor != null) {
					if (cursor.moveToNext()) {
						id = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns._ID));
					}
					if (id >= 0 && !cursor.moveToNext()) {	//只查到了一行，有可能更新了selection
						uri = ContentUris.withAppendedId(uri, id);
					}
					cursor.close();
				}
			}
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return rows;
	}

	private void checkAndAdjustArriveStateValues(Uri uri, ContentValues values) {
		Integer state = values.getAsInteger(MSG.DbColumns.MsgSend._ARRIVE_STATE);
		if (state != null) {
			values.clear();
			values.put(MSG.DbColumns.MsgSend._ARRIVE_STATE, state);
		} else {
			throw new IllegalArgumentException("参数有误，该 Uri 的更新操作只支持列名：" + MSG.DbColumns.MsgSend._ARRIVE_STATE + ", Uri：" + uri.toString());
		}
	}

	private void checkAndAdjustSendJsonValues(Uri uri, ContentValues values) {
		Integer priority = values.getAsInteger(MSG.DbColumns.SendEntity._PRIORITY);
		Long time = values.getAsLong(MSG.DbColumns.SendEntity._TIME);
		if (priority != null || time != null) {
			values.clear();
			if (time != null) {
				values.put(MSG.DbColumns.SendEntity._TIME, time);
			}
			if (priority != null) {
				values.put(MSG.DbColumns.SendEntity._PRIORITY, priority);
			}
		} else {
			throw new IllegalArgumentException("参数有误，该 Uri 的更新操作只支持列名：" + MSG.DbColumns.SendEntity._TIME +
					" 和 " + MSG.DbColumns.SendEntity._PRIORITY + ", Uri：" + uri.toString());
		}
	}

	private final class DatabaseHelper extends SQLiteOpenHelper {
		public DatabaseHelper(final Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {
			onUpgrade(db, 50, DB_VERSION);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
			//借鉴DownloadProvider的写法，但是不能在第一次创建的时候将DB_VERSION设置为非1
			if (oldV == 31) {
				//nothing...
			} else if (oldV < 100) {
				oldV = 99;
				newV = 100;
			} else if (oldV > newV) {
				oldV = 99;
			}

			for (int version = oldV + 1; version <= newV; version++) {
				upgradeTo(db, version);
			}
		}

		private void upgradeTo(SQLiteDatabase db, int version) {
			switch (version) {
			case 100:
				createTables(db);
				break;
				/*case 105:
                    fillNullValues(db);
                    break;
                case 106:
                    addColumn(db, DB_TABLE, Downloads.Impl.COLUMN_MEDIAPROVIDER_URI, "TEXT");
                    addColumn(db, DB_TABLE, Downloads.Impl.COLUMN_DELETED, "BOOLEAN NOT NULL DEFAULT 0");
                    break;*/
			default:
				throw new IllegalStateException("Don't know how to upgrade to " + version);
			}
		}

		private void createTables(SQLiteDatabase db) {
			try {
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_SAY_HI);
				db.execSQL("CREATE TABLE " + TABLE_SAY_HI + "(" +
						MSG.DbColumns.SayHi._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
						MSG.DbColumns.SayHi._ID_ME + " BIGINT NOT NULL," +
						MSG.DbColumns.SayHi._ID_U + " BIGINT NOT NULL," +
						MSG.DbColumns.SayHi._U_NAME + " TEXT," +
						MSG.DbColumns.SayHi._FACE_PATH + " TEXT," +
						MSG.DbColumns.SayHi._TIME_SEND + " BIGINT," +
						MSG.DbColumns.SayHi._FB_STATE + " INTEGER NOT NULL DEFAULT 0);");

				db.execSQL("DROP TABLE IF EXISTS " + TABLE_MSG_SEND);
				db.execSQL("CREATE TABLE " + TABLE_MSG_SEND + "(" +
						MSG.DbColumns.MsgSend._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
						MSG.DbColumns.MsgSend._ID_ME + " BIGINT NOT NULL," +
						MSG.DbColumns.MsgSend._ID_U + " BIGINT NOT NULL," +
						MSG.DbColumns.MsgSend._TYPE_SENDER + " TEXT NOT NULL DEFAULT `user`," +
						MSG.DbColumns.MsgSend._TYPE_CONTENT + " INTEGER NOT NULL," +
						MSG.DbColumns.MsgSend._CONTENT + " TEXT NOT NULL," +
						MSG.DbColumns.MsgSend._TIME_LOCAL + " BIGINT NOT NULL," +
						MSG.DbColumns.MsgSend._ORDER_NUM + " BIGINT NOT NULL," +	//不能自增（只有PRIMARY KEY才可以），也不可以用时间，可能被修改到以前，所以进行二次插入
						MSG.DbColumns.MsgSend._NO + " BIGINT NOT NULL," +
						MSG.DbColumns.MsgSend._ARRIVE_STATE + " INTEGER NOT NULL DEFAULT 0," +
						MSG.DbColumns.MsgSend._ID_SEND_ENTITY + " INTEGER);");

				db.execSQL("DROP TABLE IF EXISTS " + TABLE_MSG_RECEIVED);
				db.execSQL("CREATE TABLE " + TABLE_MSG_RECEIVED + "(" +
						MSG.DbColumns.MsgReceived._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
						MSG.DbColumns.MsgReceived._ID_ME + " BIGINT NOT NULL," +
						MSG.DbColumns.MsgReceived._ID_U + " BIGINT NOT NULL," +
						MSG.DbColumns.MsgReceived._TYPE_SENDER + " TEXT NOT NULL DEFAULT `user`," +
						MSG.DbColumns.MsgReceived._TYPE_CONTENT + " INTEGER NOT NULL," +
						MSG.DbColumns.MsgReceived._CONTENT + " TEXT NOT NULL," +
						MSG.DbColumns.MsgReceived._TIME_SEND + " BIGINT NOT NULL," +
						MSG.DbColumns.MsgReceived._ORDER_NUM + " BIGINT NOT NULL," +	//以MsgSend._ORDER_NUM和本字段的最大值为准，并在其基础上增加
						MSG.DbColumns.MsgReceived._NO + " BIGINT NOT NULL," +
						MSG.DbColumns.MsgReceived._TIME_LENGTH + " INTEGER NOT NULL DEFAULT 7," +
						MSG.DbColumns.MsgReceived._READ_TIMES + " INTEGER NOT NULL DEFAULT 0);");

				db.execSQL("DROP TABLE IF EXISTS " + TABLE_SEND_ENTITY);
				db.execSQL("CREATE TABLE " + TABLE_SEND_ENTITY + "(" +
						MSG.DbColumns.SendEntity._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
						MSG.DbColumns.SendEntity._ID_ME + " BIGINT NOT NULL," +
						MSG.DbColumns.SendEntity._ID_U + " BIGINT NOT NULL," +
						MSG.DbColumns.SendEntity._JSON + " TEXT NOT NULL," +
						MSG.DbColumns.SendEntity._STATE_BACK + " INTEGER NOT NULL DEFAULT 0," +
						MSG.DbColumns.SendEntity._TIME + " BIGINT NOT NULL," +
						MSG.DbColumns.SendEntity._PRIORITY + " INTEGER NOT NULL DEFAULT 0);");
			} catch (SQLException ex) {
				throw ex;
			}
		}

		private void addColumn(SQLiteDatabase db, String dbTable, String columnName, String columnDefinition) {
			db.execSQL("ALTER TABLE " + dbTable + " ADD COLUMN " + columnName + " " + columnDefinition);
		}

		private void fillNullValues(SQLiteDatabase db) {
			/*ContentValues values = new ContentValues();
            values.put(xxx, 0);
            fillNullValuesForColumn(db, values);
            values.put(xxx, -1);
            fillNullValuesForColumn(db, values);*/
		}

		private void fillNullValuesForColumn(SQLiteDatabase db, ContentValues values) {
			/*String column = values.valueSet().iterator().next().getKey();
            db.update(DB_TABLE, values, column + " is null", null);
            values.clear();*/
		}
	}

	private SQLiteDatabase getReadableDatabase() {
		return getDatabaseHelper().getReadableDatabase();
	}

	private SQLiteDatabase getWritableDatabase() {
		return getDatabaseHelper().getWritableDatabase();
	}

	private DatabaseHelper getDatabaseHelper() {
		if(mDB == null) mDB = new DatabaseHelper(getContext());
		return mDB;
	}

	private DatabaseHelper mDB;
	public static char DOT = '`';
}
