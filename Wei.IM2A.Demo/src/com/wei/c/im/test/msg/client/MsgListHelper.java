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

package com.wei.c.im.test.msg.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.util.LongSparseArray;

import com.wei.c.L;
import com.wei.c.im.test.UID;
import com.wei.c.im.test.data.MsgBean;
import com.wei.c.im.test.data.NoticeBean;
import com.wei.c.im.test.msg.data.ContentHelper;
import com.wei.c.im.test.msg.data.MSG;
import com.wei.c.im.test.msg.data.MsgProvider;
import com.wei.c.utils.TimeUtils;

public class MsgListHelper {

	public static List<NoticeBean> loadSysPushs(ContentResolver resolver, long mid) {
		List<NoticeBean> list = new ArrayList<NoticeBean>();
		int count = ContentHelper.SAY_HI.getCount(resolver, mid);
		if (count > 0) {
			NoticeBean bean = new NoticeBean(NoticeBean.TYPE_SYS_NOTICE);
			bean.unreadCount = count;	//TODO 改为读取有未读标记的，策略待定
			list.add(bean);
		}
		count = ContentHelper.MSG_RECEIVED.getStrangerCount(resolver, mid);
		if (count > 0) {
			NoticeBean bean = new NoticeBean(NoticeBean.TYPE_STRANGER);
			bean.unreadCount = count;	//TODO 改为读取有未读标记的，策略待定
			list.add(bean);
		}
		return list;
	}

	public static synchronized void loadMsgBeansToMaps(ContentResolver resolver, long mid, long uid, LongSparseArray<MsgBean> mapSend, LongSparseArray<MsgBean> mapReceived, int count) {
		L.d(MsgClient.class, "loadMsgBeansToMap---------");

		long targetOrderNum = ContentHelper.getMaxOrderNumSendOrReceived(resolver, mid, uid) - count;
		loadSendMsgBeansToMap(resolver, mid, uid, mapSend, targetOrderNum, count);
		loadReceivedMsgBeansToMap(resolver, mid, uid, mapReceived, targetOrderNum, count);
	}

	public static synchronized void loadSendMsgBeansToMap(ContentResolver resolver, long mid, long uid, LongSparseArray<MsgBean> mapSend, long targetOrderNum, int count) {
		checkUIDs(mid, uid);
		if (targetOrderNum < 0) targetOrderNum = ContentHelper.getMaxOrderNumSendOrReceived(resolver, mid, uid) - count;

		Cursor cursorSend = ContentHelper.MSG_SEND.getCursorOrderNumDesc(resolver, mid, uid);
		if (cursorSend != null) {
			while (cursorSend.moveToNext()) {
				long orderNum = cursorSend.getLong(cursorSend.getColumnIndex(MSG.DbColumns.MsgSend._ORDER_NUM));
				if (orderNum <= targetOrderNum) {
					break;
				}
				long id = cursorSend.getLong(cursorSend.getColumnIndex(MSG.DbColumns.MsgSend._ID));
				int ctype = cursorSend.getInt(cursorSend.getColumnIndex(MSG.DbColumns.MsgSend._TYPE_CONTENT));
				String content = cursorSend.getString(cursorSend.getColumnIndex(MSG.DbColumns.MsgSend._CONTENT));
				int arriveState = cursorSend.getInt(cursorSend.getColumnIndex(MSG.DbColumns.MsgSend._ARRIVE_STATE));
				MsgBean bean = new MsgBean(id, MsgBean.TYPE_ME, ctype, content, orderNum, 0, arriveState);
				bean.timeSend = cursorSend.getLong(cursorSend.getColumnIndex(MSG.DbColumns.MsgSend._TIME_LOCAL));
				mapSend.put(bean.id, bean);
			}
			cursorSend.close();
		}
	}

	public static synchronized void loadReceivedMsgBeansToMap(ContentResolver resolver, long mid, long uid, LongSparseArray<MsgBean> mapReceived, long targetOrderNum, int count) {
		checkUIDs(mid, uid);
		if (targetOrderNum < 0) targetOrderNum = ContentHelper.getMaxOrderNumSendOrReceived(resolver, mid, uid) - count;

		Cursor cursorReceived = ContentHelper.MSG_RECEIVED.getCursorOrderNumDesc(resolver, mid, uid);
		if (cursorReceived != null) {
			while (cursorReceived.moveToNext()) {
				long orderNum = cursorReceived.getLong(cursorReceived.getColumnIndex(MSG.DbColumns.MsgReceived._ORDER_NUM));
				if (orderNum <= targetOrderNum) {
					break;
				}
				long id = cursorReceived.getLong(cursorReceived.getColumnIndex(MSG.DbColumns.MsgReceived._ID));
				int ctype = cursorReceived.getInt(cursorReceived.getColumnIndex(MSG.DbColumns.MsgReceived._TYPE_CONTENT));
				String content = cursorReceived.getString(cursorReceived.getColumnIndex(MSG.DbColumns.MsgReceived._CONTENT));
				int timeLength = cursorReceived.getInt(cursorReceived.getColumnIndex(MSG.DbColumns.MsgReceived._TIME_LENGTH));
				int readTimes = cursorReceived.getInt(cursorReceived.getColumnIndex(MSG.DbColumns.MsgReceived._READ_TIMES));
				MsgBean bean = new MsgBean(id, MsgBean.TYPE_OTHER, ctype, content, orderNum, timeLength);
				bean.timeSend = cursorReceived.getLong(cursorReceived.getColumnIndex(MSG.DbColumns.MsgReceived._TIME_SEND));
				bean.readTimes = readTimes;
				mapReceived.put(bean.id, bean);
			}
			cursorReceived.close();
		}
	}

	public static synchronized void parseAndUpdateMaps(ContentResolver resolver, Uri uri, long mid, long uid, LongSparseArray<MsgBean> mapSend, LongSparseArray<MsgBean> mapReceived, int count) {
		checkUIDs(mid, uid);
		L.d(MsgClient.class, "parseAndUpdateMaps---------");

		if (uri != null) {
			L.e(MsgClient.class, "parseAndUpdateMaps---------uri != null");
			MsgBean bean = null;
			switch(MsgProvider.match(uri)) {
			case MsgProvider.MSG_SEND:
				L.e(MsgClient.class, "parseAndUpdateMaps-------MSG_SEND--");
				loadSendMsgBeansToMap(resolver, mid, uid, mapSend, -1, count);
				break;
			case MsgProvider.MSG_SEND_ID:
				L.e(MsgClient.class, "parseAndUpdateMaps-------MSG_SEND_ID--");
			case MsgProvider.ARRIVE_STATE_ID:
				L.e(MsgClient.class, "parseAndUpdateMaps-------ARRIVE_STATE_ID--");
				/*long targetOrderNum = ContentHelper.getMaxOrderNumSendOrReceived(resolver, mid, uid) - count;
				bean = ContentHelper.MSG_SEND.getSendMsgBeanWithIdUri(resolver, uri, targetOrderNum);
				if (bean != null) mapSend.put(bean.id, bean);*/
				bean = ContentHelper.MSG_SEND.getSendMsgBeanWithIdUri(resolver, uri, mid, uid, 0);
				if (bean != null) {
					if (mapSend.get(bean.id) != null) {
						mapSend.put(bean.id, bean);
					} else {
						long targetOrderNum = ContentHelper.getMaxOrderNumSendOrReceived(resolver, mid, uid) - count;
						//最新发出去的，列表里面还没有
						if (bean.orderNum > targetOrderNum) mapSend.put(bean.id, bean);
					}
				}
				break;
			case MsgProvider.MSG_RECEIVED:
				L.e(MsgClient.class, "parseAndUpdateMaps-------MSG_RECEIVED--");
				loadReceivedMsgBeansToMap(resolver, mid, uid, mapReceived, -1, count);
				break;
			case MsgProvider.MSG_RECEIVED_ID:
				L.e(MsgClient.class, "parseAndUpdateMaps-------MSG_RECEIVED_ID--");
				/*targetOrderNum = ContentHelper.getMaxOrderNumSendOrReceived(resolver, mid, uid) - count;
				bean = ContentHelper.MSG_RECEIVED.getReceivedMsgBeanWithIdUri(resolver, uri, targetOrderNum);
				if (bean != null) mapReceived.put(bean.id, bean);*/
				bean = ContentHelper.MSG_RECEIVED.getReceivedMsgBeanWithIdUri(resolver, uri, mid, uid, 0);
				if (bean != null) {
					if (mapReceived.get(bean.id) != null) {
						mapReceived.put(bean.id, bean);
					} else {
						long targetOrderNum = ContentHelper.getMaxOrderNumSendOrReceived(resolver, mid, uid) - count;
						//最新收到的，列表里面还没有
						if (bean.orderNum > targetOrderNum) mapReceived.put(bean.id, bean);
					}
				}
				break;
			case MsgProvider.ARRIVE_STATE:
				L.e(MsgClient.class, "parseAndUpdateMaps-------ARRIVE_STATE--");
				loadSendMsgBeansToMap(resolver, mid, uid, mapSend, -1, count);
				break;
			}
		}
	}

	public static synchronized List<MsgBean> makeMsgList(LongSparseArray<MsgBean> mapSend, LongSparseArray<MsgBean> mapReceived) {
		L.d(MsgClient.class, "makeMsgList---------");
		List<MsgBean> list = new LinkedList<MsgBean>();
		addAll(list, mapSend);
		addAll(list, mapReceived);
		Collections.sort(list, sMsgBeanComparator);

		int arriveState = -1, endArriveState = 0, lastIndex = -1;
		long timeSend = 0, timeReceived = 0;
		MsgBean bean;
		for (int i = 0; i < list.size(); i++) {
			bean = list.get(i);
			if (bean.type == MsgBean.TYPE_ME) {
				endArriveState = bean.arriveState;
				if (arriveState < 0) {
					arriveState = bean.arriveState;
				} else {
					if(bean.arriveState != arriveState) {
						list.add(lastIndex + 1, new MsgBean(arriveState));
						arriveState = bean.arriveState;
						i++;
					}
				}
				//以本地时间为准
				if (bean.timeSend - timeSend > ContentHelper.TIME_INTERVAL) {
					list.add(i++, new MsgBean(TimeUtils.format(bean.timeSend, "yyyy-MM-dd hh:mm:ss a", false)));
				}
				timeSend = bean.timeSend;
				lastIndex = i;
			} else if (bean.type == MsgBean.TYPE_OTHER) {
				if (ContentHelper.convertServerTimeToLocal(bean.timeSend) - timeReceived > ContentHelper.TIME_INTERVAL) {
					list.add(i++, new MsgBean(TimeUtils.format(ContentHelper.convertServerTimeToLocal(bean.timeSend), "yyyy-MM-dd hh:mm:ss a", false)));
				}
				timeReceived = ContentHelper.convertServerTimeToLocal(bean.timeSend);
			}
		}
		//lastIndex >= 1 表示至少有一个item是发出去的；0位置是时间
		if (list.size() > 0 && lastIndex >= 1) list.add(lastIndex + 1, new MsgBean(endArriveState));
		return list;
	}

	private static <T> void addAll(List<T> list, LongSparseArray<T> map) {
		for (int i = 0; i < map.size(); i++) {
			list.add(map.valueAt(i));
		}
	}

	private static void checkUIDs(long mid, long uid) {
		if (!UID.isValid(mid) || !UID.isValid(uid)) throw new IllegalArgumentException("mID或uID无效");
	}

	public static final Comparator<MsgBean> sMsgBeanComparator = new Comparator<MsgBean>() {
		@Override
		public int compare(MsgBean lhs, MsgBean rhs) {
			return lhs.compareTo(rhs);
		}
	};

	public static final Comparator<NoticeBean> sNoticeBeanComparator = new Comparator<NoticeBean>() {
		@Override
		public int compare(NoticeBean lhs, NoticeBean rhs) {
			return lhs.compareTo(rhs);
		}
	};
}
