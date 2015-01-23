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

package com.wei.c.im.test;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.wei.c.im.test.R;

public class Notifications {
	public static final String TAG				= "com.wei.c.im.test";

	public static final int TYPE_SAY_HI			= 0;
	public static final int TYPE_XXX			= 1;
	private static int mYoId;

	@SuppressWarnings("deprecation")
	public static void showNotification(Context context, int type, String title, String content) {

		Notification notice = new Notification();

		notice.flags |= Notification.FLAG_AUTO_CANCEL;
		notice.flags |= Notification.FLAG_SHOW_LIGHTS;

		switch (type) {
		case TYPE_SAY_HI:
			mCurrentType = TYPE_SAY_HI;
			notice.defaults = Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE;
			notice.ledARGB = context.getResources().getColor(R.color.blue);
			notice.ledOnMS = 800;
			notice.ledOffMS = 300;
			notice.icon = R.drawable.exp_a;
			notice.tickerText = content;

			notice.setLatestEventInfo(context, title, content, PendingIntent.getBroadcast(context, TYPE_SAY_HI,
					new Intent(Const.Broadcast.SAY_HI_CLICK_NOTIFY), PendingIntent.FLAG_UPDATE_CURRENT));
			//太多不清理的话，后面的会不显示
			getNotificationManager(context).cancel(TAG, mYoId - 5);
			getNotificationManager(context).notify(TAG, mYoId++, notice);
			break;
		case TYPE_XXX:
			//xxx
			break;
		}
	}

	public static void removeNotification(Context context, int type) {
		if(type == mCurrentType) {
			getNotificationManager(context).cancel(TAG, 0);
			mCurrentType = -1;
		}
	}

	public static void removeAllNotification(Context context) {
		getNotificationManager(context).cancel(TAG, 0);
		mCurrentType = -1;
	}

	private static NotificationManager getNotificationManager(Context context) {
		return (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	private static int mCurrentType = -1;
}
