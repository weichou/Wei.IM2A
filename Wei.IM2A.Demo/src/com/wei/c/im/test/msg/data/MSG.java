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

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;

public class MSG {
	public static final String AUTHORITY				= "com.wei.c.im.test.msg";

	static final String PATH_SAY_HI						= "say_hi";
	static final String PATH_MSG_SEND					= "msg_send";
	static final String PATH_MSG_RECEIVED				= "msg_received";
	static final String PATH_ARRIVE_STATE				= "arrive_state";
	static final String PATH_SEND_ENTITY				= "send_entity";
	static final String PATH_MSG_SEND_ENTITY_ID			= "msg_send_entity_id";

	private static final String SCHEME					= ContentResolver.SCHEME_CONTENT + "://";
	private static final String SEP						= "/";

	public static final Uri URI_SAY_HI					= Uri.parse(SCHEME + AUTHORITY + SEP + PATH_SAY_HI);
	/**发送消息（文字、图片地址、语音地址）**/
	public static final Uri URI_MSG_SEND				= Uri.parse(SCHEME + AUTHORITY + SEP + PATH_MSG_SEND);
	/**接收消息**/
	public static final Uri URI_MSG_RECEIVED			= Uri.parse(SCHEME + AUTHORITY + SEP + PATH_MSG_RECEIVED);
	/**已发送、已读标记，由服务端发回。
	 * 注：只对消息表进行update操作，没有insert、delete，query暂无，待定**/
	public static final Uri URI_ARRIVE_STATE			= Uri.parse(SCHEME + AUTHORITY + SEP + PATH_ARRIVE_STATE);
	/**待发送的所有信息都要转换为json字符串先存储在这里**/
	public static final Uri URI_SEND_ENTITY				= Uri.parse(SCHEME + AUTHORITY + SEP + PATH_SEND_ENTITY);
	/**更新MSG_SEND中的_ID_SEND_ENTITY**/
	public static final Uri URI_MSG_SEND_ENTITY_ID		= Uri.parse(SCHEME + AUTHORITY + SEP + PATH_MSG_SEND_ENTITY_ID);

	//////////////////////////////////////////////////////////////////////////////

	public static final String PERMISSION				= AUTHORITY + ".WEI_IM_MSG";

	public static void checkPermission(Context context) {
		if(!isPermissionAllowed(context)) throw new SecurityException("没有存取权限：" + MSG.PERMISSION);
	}

	public static boolean isPermissionAllowed(Context context) {
		return context.checkCallingOrSelfPermission(MSG.PERMISSION) == PackageManager.PERMISSION_GRANTED;
	}

	//暂不需要支持 MIME type
	//public static final String MSG_TYPE_DIR			= "vnd.android.cursor.dir/" + AUTHORITY;
	//public static final String MSG_TYPE_ITEM			= "vnd.android.cursor.item/" + AUTHORITY;

	public abstract static class DbColumns {
		/**数据库记录id**/
		public static final String _ID					= "_id";
		/**我的id（可能多个账号登录）**/
		public static final String _ID_ME				= "_id_me";
		/**对方的id**/
		public static final String _ID_U				= "_id_u";

		private abstract static class Msgs extends DbColumns {
			/**消息内容的类型：1文字，2图片，3声音**/
			public static final String _TYPE_CONTENT	= "_type_content";
			/**发送对象的类型（对于用户而言，就是用户自己。该字段预留）**/
			public static final String _TYPE_SENDER		= "_type_sender";
			/**消息内容**/
			public static final String _CONTENT			= "_content";
		}

		public static class SayHi extends DbColumns {
			/**用户名**/
			public static final String _U_NAME			= "_u_name";
			/**头像地址**/
			public static final String _FACE_PATH		= "_face_path";
			/**发送的服务端时间**/
			public static final String _TIME_SEND		= "_time_send";
			/**反馈状态：0未处理，1已查看，2已回复**/
			public static final String _FB_STATE		= "_fb_state";
		}

		public static class SendEntity extends DbColumns {
			/**json内容**/
			public static final String _JSON			= "_json";
			/**是不是一个返回状态:received，read，或者sayhi之类不需要关注有没有发送成功的消息**/
			public static final String _STATE_BACK		= "_state_back";
			/**创建记录的时间（过期没有发送的将不发送，并将MsgSend表中的_ARRIVE_STATE字段置为未发送）**/
			public static final String _TIME			= "_time";
			/**数字越小，优先级越高。默认为0，优先级高于0的为负数**/
			public static final String _PRIORITY		= "_priority";
		}

		public static class MsgSend extends Msgs {
			/**消息发送时间（本地时间，用于更加准确的显示发送时间。服务端不使用这个时间，而使用服务端时间）**/
			public static final String _TIME_LOCAL		= "_time_local";
			/**用来与接受到的消息进行时间排序的，但不能根据本地时间或服务端时间**/
			public static final String _ORDER_NUM		= "_order_num";
			/**一个发送标签，用于认领发送/已读标记。可直接使用数据库表主键{@link DbColumns#_ID _ID}**/
			public static final String _NO				= "_no";
			/**0发送中，1未发送（发送遇到错误，则回写该状态），2已发送（Socket发送成功），3发送回执（服务端已处理），4接收回执（对方已接收），5已读回执（对方已阅读）**/
			public static final String _ARRIVE_STATE	= "_arrive_state";
			/**SendEntity表的主键id。为什么在这里加个字段，因为有多个发送表都对应于一个SendEntity表**/
			public static final String _ID_SEND_ENTITY	= "_id_send_entity";
		}

		public static class MsgReceived extends Msgs {
			/**消息发送时间（服务端时间）**/
			public static final String _TIME_SEND		= "_time_send";
			/**需要根据{@link MsgSend#_ORDER_NUM}来生成**/
			public static final String _ORDER_NUM		= "_order_num";
			/**发送已读标记时需要该字段**/
			public static final String _NO				= "_no";
			/**可以阅读的时长**/
			public static final String _TIME_LENGTH		= "_time_length";
			/**阅读次数**/
			public static final String _READ_TIMES		= "_read_times";
		}
	}
}
