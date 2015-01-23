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

package com.wei.c.im.test.msg.data.send;

import com.google.gson.annotations.Expose;
import com.wei.c.im.test.msg.data.abs.AbsSendable;

public class MsgSend extends AbsSendable {
	public final long _id_me;

	@Expose
	/**必须有**/
	public final String type = "";
	@Expose
	/**接收者id**/
	public final long sendtoid;
	@Expose
	/**消息内容的类型：1文字，2图片，3声音**/
	public final int ctype;
	@Expose
	/**消息内容**/
	public final String cvalue;
	@Expose
	/**一个发送标签，用于认领发送/已读标记。可直接使用数据库表主键{@link DbColumns#_ID _ID}**/
	public final long no;

	/**发送对象的类型（对于用户而言，就是用户自己，值为'user'。该字段预留）**/
	public String _type_sender = "user";
	/**消息发送时间（本地时间，用于更加准确的显示发送时间。服务端不使用这个时间，而使用服务端时间）**/
	public final long _time_local;
	/**顺序号**/
	public long _order_num;

	public MsgSend(long myid, long sendtoid, int type, String value) {
		this._id_me = myid;
		this.sendtoid = sendtoid;
		this.ctype = type;
		this.cvalue = value;
		this._time_local = System.currentTimeMillis();
		this.no = _time_local;
	}

	public MsgSend(long myid, long sendtoid, int type, String value, long timelocal, long no) {
		this._id_me = myid;
		this.sendtoid = sendtoid;
		this.ctype = type;
		this.cvalue = value;
		this._time_local = timelocal;
		this.no = no;
	}
}
