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

public class MsgBean implements Comparable<MsgBean> {
	public static final int TYPE_ME					= 0;
	public static final int TYPE_OTHER				= 1;
	public static final int TYPE_ARRIVE_STATE			= 2;
	public static final int TYPE_TIME					= 3;

	public final long id;
	public final int type;
	/**消息内容的类型：1文字，2图片，3声音**/
	public final int ctype;
	public final String content;
	/**用于排序的序号**/
	public final long orderNum;
	public int arriveState;
	public long timeSend;
	public String timeSendSerialized;
	/**可阅读时间，单位秒**/
	public final int timeLengthToRead;
	/**阅读次数**/
	public int readTimes;
	/**对方的头像地址**/
	public String facePath;

	public MsgBean(long id, int type, int ctype, String content, long orderNum, int timeLengthToRead) {
		this(id, type, ctype, content, orderNum, timeLengthToRead, 0);
	}

	public MsgBean(int arriveState) {
		this(-1, MsgBean.TYPE_ARRIVE_STATE, 0, null, 0, 0, arriveState);
	}

	public MsgBean(String timeSendSerialized) {
		this(-1, MsgBean.TYPE_TIME, 0, null, 0, 0, 0);
		this.timeSendSerialized = timeSendSerialized;
	}

	public MsgBean(long id, int type, int ctype, String content, long orderNum, int timeLengthToRead, int arriveState) {
		this.id = id;
		if(type < TYPE_ME || type > TYPE_TIME) throw new IllegalArgumentException("type 值不正确，见常量");
		this.type = type;
		this.ctype = ctype;
		this.content = content;
		this.orderNum = orderNum;
		this.timeLengthToRead = timeLengthToRead;
		this.arriveState = arriveState;
	}

	@Override
	public int hashCode() {
		return (int)(id / 10);
	}

	@Override
	public boolean equals(Object o) {
		MsgBean bean = (MsgBean)o;
		return bean.id == id && bean.type == type;
	}

	@Override
	public int compareTo(MsgBean another) {
		long delta = orderNum - another.orderNum;
		return delta > 0 ? 1 : delta == 0 ? 0 : -1;
	}
}
