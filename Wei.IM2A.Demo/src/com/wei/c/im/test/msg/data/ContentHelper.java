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

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.wei.c.data.abs.IJson;
import com.wei.c.im.test.Notifications;
import com.wei.c.im.test.data.MsgBean;
import com.wei.c.im.test.data.SayHiBean;
import com.wei.c.im.test.msg.data.abs.SendEntity;
import com.wei.c.im.test.msg.data.receive.ArriveState;
import com.wei.c.im.test.msg.data.receive.MsgReceived;
import com.wei.c.im.test.msg.data.receive.SayHi;
import com.wei.c.im.test.msg.data.send.MsgSend;
import com.wei.c.im.test.msg.data.send.StateBack;
import com.wei.c.utils.ArrayUtils;

public class ContentHelper {
	public static final String TAG              = ContentHelper.class.getSimpleName();
	public static final int TIME_INTERVAL       = 5*60*1000;
	public static char DOT                      = '`';

	public static long getTimeOutOfDatePoint() {
		return System.currentTimeMillis() - TIME_INTERVAL;
	}

	/**服务端时间是精确到秒的**/
	public static long convertServerTimeToLocal(long time) {
		return time * 1000;
	}

	public static long getMaxOrderNumSendOrReceived(ContentResolver contentResolver, long id_me, long id_u) {
		return Math.max(MSG_RECEIVED.getMaxOrderNum(contentResolver, id_me, id_u),
				MSG_SEND.getMaxOrderNum(contentResolver, id_me, id_u));
	}

	public static long getMaxTimeSendOrReceived(ContentResolver contentResolver, long id_me, long id_u) {
		long[] orderNumAndReceiveTime = MSG_RECEIVED.getMaxOrderNumAndSendTime(contentResolver, id_me, id_u);
		long[] orderNumAndSendTime = MSG_SEND.getMaxOrderNumAndSendTime(contentResolver, id_me, id_u);
		//由于这个时间是以本地为准，那么就选最大的吧
		return Math.max(orderNumAndReceiveTime[1], orderNumAndSendTime[1]);
	}

	public static final class SAY_HI {

		public static void sendSayHi(ContentResolver contentResolver, long id_me, long id_u) {
			SEND_ENTITY.insertNewEntity(contentResolver, id_me, id_u, new com.wei.c.im.test.msg.data.send.SayHi(id_u), true, 0, 0);
		}

		public static long receive(Context context, ContentResolver contentResolver, long id_me, SayHi sayhi) {
			Notifications.showNotification(context, Notifications.TYPE_SAY_HI, "Say Hi", sayhi.uname + "向你打招呼啦！~");

			ContentValues values = new  ContentValues();
			values.put(MSG.DbColumns.SayHi._ID_ME, id_me);
			values.put(MSG.DbColumns.SayHi._ID_U, sayhi.fromuid);
			values.put(MSG.DbColumns.SayHi._U_NAME, sayhi.uname);
			values.put(MSG.DbColumns.SayHi._FACE_PATH, sayhi.facepath);
			values.put(MSG.DbColumns.SayHi._TIME_SEND, sayhi.time);
			return ContentUris.parseId(contentResolver.insert(MSG.URI_SAY_HI, values));
		}

		public static void updateRead(ContentResolver contentResolver, long id) {
			update(contentResolver, id, 1);
		}

		public static void updateFeedBack(ContentResolver contentResolver, long id) {
			update(contentResolver, id, 2);
		}

		public static void update(ContentResolver contentResolver, long id, int fb_state) {
			ContentValues values = new  ContentValues();
			values.put(MSG.DbColumns.SayHi._FB_STATE, fb_state);
			contentResolver.update(ContentUris.withAppendedId(MSG.URI_SAY_HI, id), values, null, null);
		}

		public static int getCount(ContentResolver contentResolver, long id_me) {
			Cursor cursor = contentResolver.query(MSG.URI_SAY_HI, new String[]{"count(*)"}, MSG.DbColumns.SayHi._ID_ME + "=? AND " + MSG.DbColumns.SayHi._FB_STATE + "=?",
					new String[]{String.valueOf(id_me), 0 + ""}, null);
			int count = 0;
			if (cursor != null) {
				if (cursor.moveToNext()) {
					count = cursor.getInt(0);
				}
				cursor.close();
			}
			return count;
		}

		public static List<SayHiBean> getSayHis(ContentResolver contentResolver, long id_me) {
			Cursor cursor = contentResolver.query(MSG.URI_SAY_HI, null, MSG.DbColumns.SayHi._ID_ME + "=? AND " + MSG.DbColumns.SayHi._FB_STATE + "=?",
					new String[]{String.valueOf(id_me), 0 + ""}, null);
			List<SayHiBean> list = new ArrayList<SayHiBean>();
			if (cursor != null) {
				while (cursor.moveToNext()) {
					SayHiBean sayHi = new SayHiBean();
					sayHi._id = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.SayHi._ID));
					sayHi.fromuid = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.SayHi._ID_U));
					sayHi.time = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.SayHi._TIME_SEND));
					sayHi.uname = cursor.getString(cursor.getColumnIndex(MSG.DbColumns.SayHi._U_NAME));
					sayHi.facepath = cursor.getString(cursor.getColumnIndex(MSG.DbColumns.SayHi._FACE_PATH));
					sayHi.fbState = cursor.getInt(cursor.getColumnIndex(MSG.DbColumns.SayHi._FB_STATE));
					list.add(sayHi);
				}
				cursor.close();
			}
			return list;			
		}
	}

	public static final class SEND_ENTITY {

		public static long insertNewEntity(ContentResolver contentResolver, long id_me, long id_u, IJson<?> jsonable, boolean state_back, long time_local, int priority) {
			ContentValues values = new  ContentValues();
			values.put(MSG.DbColumns.SendEntity._ID_ME, id_me);
			values.put(MSG.DbColumns.SendEntity._ID_U, id_u);
			values.put(MSG.DbColumns.SendEntity._JSON, jsonable.toJson());
			values.put(MSG.DbColumns.SendEntity._STATE_BACK, state_back);
			values.put(MSG.DbColumns.SendEntity._TIME, time_local);
			values.put(MSG.DbColumns.SendEntity._PRIORITY, priority);
			return ContentUris.parseId(contentResolver.insert(MSG.URI_SEND_ENTITY, values));
		}

		public static Cursor getCursor(ContentResolver contentResolver, long id_me) {
			return contentResolver.query(MSG.URI_SEND_ENTITY, null, MSG.DbColumns.SendEntity._ID_ME + "=" + id_me,
					null, MSG.DbColumns.SendEntity._PRIORITY);
		}

		public static SendEntity<String> getSendEntity(ContentResolver contentResolver, Cursor cursor) {
			while (cursor.moveToNext()) {
				long _id = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.SendEntity._ID));
				boolean _stateBack = cursor.getInt(cursor.getColumnIndex(MSG.DbColumns.SendEntity._STATE_BACK)) != 0;
				if (!_stateBack) {	//sayhi或反馈状态，不管有没有过期，都要发出去
					long _time = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.SendEntity._TIME));
					if (_time < getTimeOutOfDatePoint()) {	//超过5分钟，则变更为不发送
						ARRIVE_STATE.updateNotSendWithSendEntityId(contentResolver, _id);
						delete(contentResolver, _id);
						continue;
					}
				}
				String _json = cursor.getString(cursor.getColumnIndex(MSG.DbColumns.SendEntity._JSON));
				return new SendEntity<String>(_id, _json, _stateBack);
			}
			return null;
		}

		public static void updateEntitySended(ContentResolver contentResolver, SendEntity<?> sendEntity) {
			if (!sendEntity.stateBack) ARRIVE_STATE.updateSendedWithSendEntityId(contentResolver, sendEntity.id);
			delete(contentResolver, sendEntity.id);
		}

		public static void delete(ContentResolver contentResolver, long id) {
			contentResolver.delete(ContentUris.withAppendedId(MSG.URI_SEND_ENTITY, id), null, null);
		}
	}

	public static final class MSG_SEND {

		public static synchronized long insertNewMsg(ContentResolver contentResolver, long id_me, MsgSend msgSend) {
			ContentValues values = new  ContentValues();
			values.put(MSG.DbColumns.MsgSend._ID_ME, id_me);
			values.put(MSG.DbColumns.MsgSend._ID_U, msgSend.sendtoid);
			values.put(MSG.DbColumns.MsgSend._TYPE_SENDER, msgSend._type_sender);
			values.put(MSG.DbColumns.MsgSend._TYPE_CONTENT, msgSend.ctype);
			values.put(MSG.DbColumns.MsgSend._CONTENT, msgSend.cvalue);
			values.put(MSG.DbColumns.MsgSend._NO, msgSend.no);
			values.put(MSG.DbColumns.MsgSend._TIME_LOCAL, msgSend._time_local);
			long id;
			synchronized (ContentHelper.class) {
				msgSend._order_num = getMaxOrderNumSendOrReceived(contentResolver, id_me, msgSend.sendtoid) + 1;
				values.put(MSG.DbColumns.MsgSend._ORDER_NUM, msgSend._order_num);
				id = ContentUris.parseId(contentResolver.insert(MSG.URI_MSG_SEND, values));
			}
			long sendEntityId = SEND_ENTITY.insertNewEntity(contentResolver, id_me, msgSend.sendtoid, msgSend, false, msgSend._time_local, 0);
			values.clear();
			values.put(MSG.DbColumns.MsgSend._ID_SEND_ENTITY, sendEntityId);
			contentResolver.update(ContentUris.withAppendedId(MSG.URI_MSG_SEND_ENTITY_ID, id), values, null, null);
			return id;
		}

		public static synchronized void delete(ContentResolver contentResolver, long id_me, long id_u) {
			contentResolver.delete(MSG.URI_MSG_SEND, MSG.DbColumns.MsgSend._ID_ME + "=? AND " +
					MSG.DbColumns.MsgSend._ID_U + "=?", new String[]{String.valueOf(id_me), String.valueOf(id_u)});
		}

		public static void resendMsg(ContentResolver contentResolver, long msgId) {
			Cursor cursor = contentResolver.query(ContentUris.withAppendedId(MSG.URI_MSG_SEND, msgId), null, null, null, null);
			MsgSend msgSend = null;
			if (cursor != null) {
				if (cursor.moveToNext()) {
					long myid = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.MsgSend._ID_ME));
					long sendtoid = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.MsgSend._ID_U));
					int type = cursor.getInt(cursor.getColumnIndex(MSG.DbColumns.MsgSend._TYPE_CONTENT));
					String content = cursor.getString(cursor.getColumnIndex(MSG.DbColumns.MsgSend._CONTENT));
					long timelocal = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.MsgSend._TIME_LOCAL));
					long no = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.MsgSend._NO));
					msgSend = new MsgSend(myid, sendtoid, type, content, timelocal, no);
				}
			}
			long sendEntityId = SEND_ENTITY.insertNewEntity(contentResolver, msgSend._id_me, msgSend.sendtoid, msgSend, false, System.currentTimeMillis(), 0);
			ContentValues values = new  ContentValues();
			values.put(MSG.DbColumns.MsgSend._ID_SEND_ENTITY, sendEntityId);
			contentResolver.update(ContentUris.withAppendedId(MSG.URI_MSG_SEND_ENTITY_ID, msgId), values, null, null);
		}

		public static long getMaxOrderNum(ContentResolver contentResolver, long id_me, long id_u) {
			return getMaxOrderNumAndSendTime(contentResolver, id_me, id_u)[0];
		}

		public static long[] getMaxOrderNumAndSendTime(ContentResolver contentResolver, long id_me, long id_u) {
			Cursor cursor = contentResolver.query(MSG.URI_MSG_SEND,
					new String[]{MSG.DbColumns.MsgSend._ORDER_NUM, MSG.DbColumns.MsgSend._TIME_LOCAL},
					MSG.DbColumns.MsgSend._ID_ME + "=? AND " + MSG.DbColumns.MsgSend._ID_U + "=?",
					new String[]{String.valueOf(id_me), String.valueOf(id_u)}, MSG.DbColumns.MsgSend._ORDER_NUM + " DESC");
			long orderNum = 0, sendTime = 0;
			if (cursor != null) {
				if( cursor.moveToFirst()) {
					orderNum = cursor.getLong(0);
					sendTime = cursor.getLong(1);
				}
				cursor.close();
			}
			return new long[]{orderNum, sendTime};
		}

		public static Cursor getCursorOrderNumDesc(ContentResolver contentResolver, long id_me, long id_u) {
			return contentResolver.query(MSG.URI_MSG_SEND, null, MSG.DbColumns.MsgSend._ID_ME + "=? AND " +
					MSG.DbColumns.MsgSend._ID_U + "=?", new String[]{String.valueOf(id_me), String.valueOf(id_u)},
					MSG.DbColumns.MsgSend._ORDER_NUM + " DESC");
		}

		public static MsgBean getSendMsgBeanWithIdUri(ContentResolver contentResolver, Uri uri, long id_me, long id_u, long targetOrderNum) {
			long id = ContentUris.parseId(uri);
			if (id < 0) throw new IllegalArgumentException("参数uri不带id部分:" + uri);
			MsgBean bean = null;
			Cursor cursor = contentResolver.query(ContentUris.withAppendedId(MSG.URI_MSG_SEND, id), null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToNext()) {
					long mid = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.MsgSend._ID_ME));
					long uid = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.MsgSend._ID_U));
					long orderNum = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.MsgSend._ORDER_NUM));
					if (mid == id_me && uid == id_u && orderNum > targetOrderNum) {
						id = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.MsgSend._ID));
						int ctype = cursor.getInt(cursor.getColumnIndex(MSG.DbColumns.MsgSend._TYPE_CONTENT));
						String content = cursor.getString(cursor.getColumnIndex(MSG.DbColumns.MsgSend._CONTENT));
						int arriveState = cursor.getInt(cursor.getColumnIndex(MSG.DbColumns.MsgSend._ARRIVE_STATE));
						bean = new MsgBean(id, MsgBean.TYPE_ME, ctype, content, orderNum, 0, arriveState);
						bean.timeSend = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.MsgSend._TIME_LOCAL));
					}
				}
				cursor.close();
			}
			return bean;
		}

		public static MsgBean getLastSendMsg(ContentResolver contentResolver, long id_me, long id_u) {
			MsgBean bean = null;
			Cursor cursor = getCursorOrderNumDesc(contentResolver, id_me, id_u);
			if (cursor != null) {
				if (cursor.moveToNext()) {
					long id = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.MsgSend._ID));
					long orderNum = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.MsgSend._ORDER_NUM));
					int ctype = cursor.getInt(cursor.getColumnIndex(MSG.DbColumns.MsgSend._TYPE_CONTENT));
					String content = cursor.getString(cursor.getColumnIndex(MSG.DbColumns.MsgSend._CONTENT));
					int arriveState = cursor.getInt(cursor.getColumnIndex(MSG.DbColumns.MsgSend._ARRIVE_STATE));
					bean = new MsgBean(id, MsgBean.TYPE_ME, ctype, content, orderNum, 0, arriveState);
					bean.timeSend = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.MsgSend._TIME_LOCAL));
				}
				cursor.close();
			}
			return bean;
		}
	}

	public static final class MSG_RECEIVED {

		public static synchronized long receive(ContentResolver contentResolver, long id_me, MsgReceived msgReceived) {
			ContentValues values = new  ContentValues();
			values.put(MSG.DbColumns.MsgReceived._ID_ME, id_me);
			values.put(MSG.DbColumns.MsgReceived._ID_U, msgReceived.fromuid);
			values.put(MSG.DbColumns.MsgReceived._TYPE_SENDER, msgReceived.sendtype);
			values.put(MSG.DbColumns.MsgReceived._TYPE_CONTENT, msgReceived.ctype);
			values.put(MSG.DbColumns.MsgReceived._CONTENT, msgReceived.cvalue);
			values.put(MSG.DbColumns.MsgReceived._TIME_SEND, msgReceived.sendtime);
			values.put(MSG.DbColumns.MsgReceived._NO, msgReceived.no);
			long id;
			synchronized (ContentHelper.class) {
				msgReceived._order_num = getMaxOrderNumSendOrReceived(contentResolver, id_me, msgReceived.fromuid) + 1;
				values.put(MSG.DbColumns.MsgReceived._ORDER_NUM, msgReceived._order_num);
				id = ContentUris.parseId(contentResolver.insert(MSG.URI_MSG_RECEIVED, values));
			}
			ARRIVE_STATE.sendBackStateReceived(contentResolver, id_me, msgReceived.fromuid, msgReceived.no);
			return id;
		}

		public static synchronized void delete(ContentResolver contentResolver, long id_me, long id_u) {
			contentResolver.delete(MSG.URI_MSG_RECEIVED, MSG.DbColumns.MsgReceived._ID_ME + "=? AND " +
					MSG.DbColumns.MsgReceived._ID_U + "=?", new String[]{String.valueOf(id_me), String.valueOf(id_u)});
		}

		public static long getMaxOrderNum(ContentResolver contentResolver, long id_me, long id_sender) {
			return getMaxOrderNumAndSendTime(contentResolver, id_me, id_sender)[0];
		}

		public static long[] getMaxOrderNumAndSendTime(ContentResolver contentResolver, long id_me, long id_sender) {
			Cursor cursor = contentResolver.query(MSG.URI_MSG_RECEIVED,
					new String[]{MSG.DbColumns.MsgReceived._ORDER_NUM, MSG.DbColumns.MsgReceived._TIME_SEND},
					MSG.DbColumns.MsgReceived._ID_ME + "=? AND " + MSG.DbColumns.MsgReceived._ID_U + "=?",
					new String[]{String.valueOf(id_me), String.valueOf(id_sender)}, MSG.DbColumns.MsgReceived._ORDER_NUM + " DESC");
			long orderNum = 0, sendTime = 0;
			if (cursor != null) {
				if( cursor.moveToFirst()) {
					orderNum = cursor.getLong(0);
					sendTime = convertServerTimeToLocal(cursor.getLong(1));
				}
				cursor.close();
			}
			return new long[]{orderNum, sendTime};
		}

		public static Cursor getCursorOrderNumDesc(ContentResolver contentResolver, long id_me, long id_u) {
			return contentResolver.query(MSG.URI_MSG_RECEIVED, null, MSG.DbColumns.MsgReceived._ID_ME + "=? AND " +
					MSG.DbColumns.MsgReceived._ID_U + "=?", new String[]{String.valueOf(id_me), String.valueOf(id_u)},
					MSG.DbColumns.MsgReceived._ORDER_NUM + " DESC");
		}

		public static List<Long> getStrangerUIDs(ContentResolver contentResolver, long id_me) {
			Cursor cursor = contentResolver.query(MSG.URI_MSG_RECEIVED, new String[]{MSG.DbColumns.MsgReceived._ID_U},
					MSG.DbColumns.MsgReceived._ID_ME + "=" + id_me + " AND " +
							MSG.DbColumns.MsgReceived._ID_U + " NOT IN(" +
							//ArrayUtils.join(RELATIONSHIP.getChatFriendsUIDs(contentResolver, id_me).toArray(), ",") +
							")", null, null);
			List<Long> list = new ArrayList<Long>();
			if (cursor != null) {
				while (cursor.moveToNext()) {
					list.add(cursor.getLong(0));
				}
				cursor.close();
			}
			return list;
		}

		public static int getStrangerCount(ContentResolver contentResolver, long id_me) {
			Cursor cursor = contentResolver.query(MSG.URI_MSG_RECEIVED, new String[]{"COUNT(DISTINCT " + MSG.DbColumns.MsgReceived._ID_U + ")"},
					MSG.DbColumns.MsgReceived._ID_ME + "=" + id_me + " AND " +
							MSG.DbColumns.MsgReceived._ID_U + " NOT IN(" +
							//ArrayUtils.join(RELATIONSHIP.getChatFriendsUIDs(contentResolver, id_me).toArray(), ",") +
							")", null, null);
			if (cursor != null) {
				if (cursor.moveToNext()) return cursor.getInt(0);
				cursor.close();
			}
			return 0;
		}

		public static int getCountUnread(ContentResolver contentResolver, long id_me, long id_u) {
			Cursor cursor = contentResolver.query(MSG.URI_MSG_RECEIVED, new String[]{"COUNT(*)"},
					MSG.DbColumns.MsgReceived._ID_ME + "=? AND " +
							MSG.DbColumns.MsgReceived._ID_U + "=? AND " + MSG.DbColumns.MsgReceived._READ_TIMES + "=0",
							new String[]{String.valueOf(id_me), String.valueOf(id_u)}, null);
			if (cursor != null) {
				if (cursor.moveToNext()) return cursor.getInt(0);
				cursor.close();
			}
			return 0;
		}

		public static long getNo(ContentResolver contentResolver, long _id) {
			Cursor cursor = contentResolver.query(ContentUris.withAppendedId(MSG.URI_MSG_RECEIVED, _id), new String[]{MSG.DbColumns.MsgReceived._NO}, null, null, null);
			long no = 0;
			if (cursor != null) {
				if (cursor.moveToNext()) {
					no = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.MsgReceived._NO));
				}
				cursor.close();
			}
			return no;
		}

		public static void updateReadTimes(ContentResolver contentResolver, long _id, int num) {
			ContentValues values = new  ContentValues();
			values.put(MSG.DbColumns.MsgReceived._READ_TIMES, num);
			contentResolver.update(ContentUris.withAppendedId(MSG.URI_MSG_RECEIVED, _id), values, null, null);

			//TODO 删除所有已读的
		}

		public static MsgBean getReceivedMsgBeanWithIdUri(ContentResolver contentResolver, Uri uri, long id_me, long id_u, long targetOrderNum) {
			long id = ContentUris.parseId(uri);
			if (id < 0) throw new IllegalArgumentException("参数uri不带id部分:" + uri);
			MsgBean bean = null;
			Cursor cursor = contentResolver.query(ContentUris.withAppendedId(MSG.URI_MSG_RECEIVED, id), null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToNext()) {
					long mid = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.MsgReceived._ID_ME));
					long uid = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.MsgReceived._ID_U));
					long orderNum = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.MsgReceived._ORDER_NUM));
					if (mid == id_me && uid == id_u && orderNum > targetOrderNum) {
						id = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.MsgReceived._ID));
						int ctype = cursor.getInt(cursor.getColumnIndex(MSG.DbColumns.MsgReceived._TYPE_CONTENT));
						String content = cursor.getString(cursor.getColumnIndex(MSG.DbColumns.MsgReceived._CONTENT));
						int timeLength = cursor.getInt(cursor.getColumnIndex(MSG.DbColumns.MsgReceived._TIME_LENGTH));
						int readTimes = cursor.getInt(cursor.getColumnIndex(MSG.DbColumns.MsgReceived._READ_TIMES));
						bean = new MsgBean(id, MsgBean.TYPE_OTHER, ctype, content, orderNum, timeLength);
						bean.timeSend = cursor.getLong(cursor.getColumnIndex(MSG.DbColumns.MsgReceived._TIME_SEND));
						bean.readTimes = readTimes;
					}
				}
				cursor.close();
			}
			return bean;
		}
	}

	public static final class ARRIVE_STATE {

		public static void updateNotSendWithSendEntityId(ContentResolver contentResolver, long sendEntityId) {
			ContentValues values = new ContentValues();
			values.put(MSG.DbColumns.MsgSend._ARRIVE_STATE, 1);
			contentResolver.update(MSG.URI_ARRIVE_STATE, values, MSG.DbColumns.MsgSend._ID_SEND_ENTITY + "=" + sendEntityId, null);
		}

		public static void updateSendedWithSendEntityId(ContentResolver contentResolver, long sendEntityId) {
			ContentValues values = new ContentValues();
			values.put(MSG.DbColumns.MsgSend._ARRIVE_STATE, 2);
			contentResolver.update(MSG.URI_ARRIVE_STATE, values, MSG.DbColumns.MsgSend._ID_SEND_ENTITY + "=" + sendEntityId, null);
		}

		public static void receive(ContentResolver contentResolver, long id_me, ArriveState arriveState) {
			int state = 1;
			if (arriveState.isServerHandled()) {
				state = 3;
			} else if (arriveState.isUserReceived()) {
				state = 4;
			} else if (arriveState.isUserRead()) {
				state = 5;
			}
			ContentValues values = new ContentValues();
			values.put(MSG.DbColumns.MsgSend._ARRIVE_STATE, state);
			if (arriveState.no != null && arriveState.no.length > 0) {
				contentResolver.update(MSG.URI_ARRIVE_STATE, values, MSG.DbColumns.MsgSend._ID_U + "=? AND " + MSG.DbColumns.MsgSend._NO + " IN(?)",
						new String[]{String.valueOf(arriveState.sendid), ArrayUtils.join(ArrayUtils.toObject(arriveState.no), ", ")});
			}
			if (state >= 4) {
				//ARRIVE_STATE_UNLOAD.deleteOrUpdate(contentResolver, id_me, arriveState);
			}
		}

		public static void sendBackStateReceived(ContentResolver contentResolver, long id_me, long id_u, long... no) {
			SEND_ENTITY.insertNewEntity(contentResolver, id_me, id_u, 
					new StateBack(StateBack.TYPE_RECEIVED, id_u, no), true, System.currentTimeMillis(), 0);
		}

		public static void sendBackStateRead(ContentResolver contentResolver, long id_me, long id_u, long _id) {
			long no = ContentHelper.MSG_RECEIVED.getNo(contentResolver, _id);
			if (no > 0) {
				SEND_ENTITY.insertNewEntity(contentResolver, id_me, id_u, 
						new StateBack(StateBack.TYPE_READ, id_u, no), true, System.currentTimeMillis(), 0);
			}
		}
	}
}