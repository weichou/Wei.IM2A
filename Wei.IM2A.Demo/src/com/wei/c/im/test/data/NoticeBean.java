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

package com.wei.c.im.test.data;

import com.wei.c.utils.TimeUtils;

/**简单信息**/
public class NoticeBean implements Comparable<NoticeBean> {
	public static final int TYPE_STRANGER			= 0;
	public static final int TYPE_SYS_NOTICE			= 1;
	public static final int TYPE_FRIENDS_NEWS		= 2;
	public static final int TYPE_MSG				= 3;

	public static int dayNoon;

	public final int type;

	public final long uid;
	public String nickname;
	public String faceUrl;
	public boolean online;
	public int arriveState;
	public int unreadCount;
	public String lastSendMsg;
	public long timeLastMsg;
	public String timeLastMsgSerialized;

	public NoticeBean(int type) {
		this(type, -1);
	}

	public NoticeBean(int type, long uid) {
		if (type < TYPE_STRANGER || type > TYPE_MSG) throw new IllegalArgumentException("参数type不正确，见常量");
		this.type = type;
		this.uid = uid;
		dayNoon = TimeUtils.getDayNoon();
	}

	@Override
	public int compareTo(NoticeBean another) {
		//前三个type在消息列表中只可能各存在一个
		return type < another.type ? -1 : type > another.type ? 1 :
			/*时间较大的排在前面*/
			timeLastMsg > another.timeLastMsg ? -1 : timeLastMsg == another.timeLastMsg ? 0 : 1;
	}
}
