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

package com.wei.c.im.test.msg;

import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;

import com.wei.c.im.service.IMService;

public class CSHelper {
	public static final String TAG							= CSHelper.class.getSimpleName();

	public static final int NONE							= -1;

	public static final int MSG_ERROR						= 0;
	public static final int MSG_AUTHORITY					= 1;
	public static final int MSG_XXX							= 2;

	public static final int ERROR_UNKNOWN_HOST				= 0;
	public static final int ERROR_IO						= 1;
	public static final int ERROR_SERVER_KICKED				= 2;

	public static final int AUTHORIZE_SUCCESS				= 3;
	public static final int AUTHORIZE_FAILED				= 4;
	public static final int AUTHORIZE_ANOTHER_CLIENT_LOGIN	= 5;

	public static void startService(Context context) {
		IMService.start(context, MsgService.class);
	}

	public static void stopService(Context context) {
		IMService.stop(context, MsgService.class);
	}

	public static void unbindAndStopService(Context context, ServiceConnection conn) {
		unbindService(context, conn);
		IMService.stop(context, MsgService.class);
	}

	public static void bindService(Context context, ServiceConnection conn) {
		IMService.bind(context, conn, MsgService.class);
	}

	public static void unbindService(Context context, ServiceConnection conn) {
		IMService.unbind(context, conn);
	}

	public static boolean replyToClient(Messenger messenger, Handler handler) {
		return IMService.replyToMe(messenger, handler);
	}

	//////////////////////////////////////////////////////////////////////////////////
	/****************************************************************************/

	public static void sendAuthoritySuccessToClient(IMService service) {
		IMService.sendMessageToClient(service, MSG_AUTHORITY, AUTHORIZE_SUCCESS, null);
	}

	public static void sendAuthorityFailedToClient(IMService service) {
		IMService.sendMessageToClient(service, MSG_AUTHORITY, AUTHORIZE_FAILED, null);
	}

	public static void sendAnotherClientLoginToClient(IMService service) {
		IMService.sendMessageToClient(service, MSG_AUTHORITY, AUTHORIZE_ANOTHER_CLIENT_LOGIN, null);
	}

	public static void sendUnknownHostToClient(IMService service) {
		IMService.sendMessageToClient(service, MSG_ERROR, ERROR_UNKNOWN_HOST, null);
	}

	public static void sendErrorIOToClient(IMService service) {
		IMService.sendMessageToClient(service, MSG_ERROR, ERROR_IO, null);
	}

	public static void sendErrorServerKickedToClient(IMService service) {
		IMService.sendMessageToClient(service, MSG_ERROR, ERROR_SERVER_KICKED, null);
	}

	public static void sendErrorToClient(IMService service, int error) {
		IMService.sendMessageToClient(service, MSG_ERROR, error, null);
	}

	/****************************************************************************/
	public static boolean isMessageBelongToAuthorizeSuccess(Message msg) {
		return msg.what == MSG_AUTHORITY && msg.arg1 == AUTHORIZE_SUCCESS;
	}

	public static boolean isMessageBelongToAuthorizeFailed(Message msg) {
		return msg.what == MSG_AUTHORITY && msg.arg1 == AUTHORIZE_FAILED;
	}

	/**用户在另一个客户端登录**/
	public static boolean isMessageBelongToAnotherClientLogin(Message msg) {
		return msg.what == MSG_AUTHORITY && msg.arg1 == AUTHORIZE_ANOTHER_CLIENT_LOGIN;
	}

	public static boolean isMessageBelongToUnknownHost(Message msg) {
		return msg.what == MSG_ERROR && msg.arg1 == ERROR_UNKNOWN_HOST;
	}

	public static boolean isMessageBelongToIOError(Message msg) {
		return msg.what == MSG_ERROR && msg.arg1 == ERROR_IO;
	}

	public static boolean isMessageBelongToServerKickedError(Message msg) {
		return msg.what == MSG_ERROR && msg.arg1 == ERROR_SERVER_KICKED;
	}

	public static boolean isMessageBelongToError(Message msg) {
		return msg.what == MSG_ERROR;
	}
}
