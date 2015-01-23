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

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;

import com.wei.c.L;
import com.wei.c.im.AbsStringEntityParser;
import com.wei.c.im.test.UID;
import com.wei.c.im.test.msg.data.ContentHelper;
import com.wei.c.im.test.msg.data.receive.ArriveState;
import com.wei.c.im.test.msg.data.receive.Authority;
import com.wei.c.im.test.msg.data.receive.MsgReceived;
import com.wei.c.im.test.msg.data.receive.SayHi;

public class StringMsgParser extends AbsStringEntityParser {
	private MsgService mService;
	private ContentResolver mContentResolver;
	private long mID;

	public StringMsgParser(MsgService service) {
		super("utf-8");
		mService = service;
		mContentResolver = mService.getContentResolver();
	}

	/**只要连接没断开，则收到的就是上一个ID的消息，那么不改变mMsgParser的ID即可，
	 * 而mMsgAdapter的ID没有改变，发送的也是该ID的消息，因此不会在切换用户时造成混乱。
	 */
	public synchronized void setUid(long uid) {
		mID = uid;
	}

	@Override
	public void onStartWork() {
		mID = -1;
		super.onStartWork();
	}

	protected void handleString(String json) {
		L.i(this, "++++++++++++++++handleString:" + json);
		if (json != null) {
			json = json.trim();
			try {
				JSONObject object = new JSONObject(json);
				if (new Authority().isBelongToMe(object)) {
					Authority authority = Authority.fromJsonWithAllFields(json, Authority.class);
					mService.setAuthority(authority);
				} else if (UID.isValid(mID)) {
					if (new SayHi().isBelongToMe(object)) {
						SayHi sayhi = SayHi.fromJsonWithAllFields(json, SayHi.class);
						ContentHelper.SAY_HI.receive(mService, mContentResolver, mID, sayhi);
					} else if (new MsgReceived().isBelongToMe(object)) {
						MsgReceived msgReceived = MsgReceived.fromJsonWithAllFields(json, MsgReceived.class);
						ContentHelper.MSG_RECEIVED.receive(mContentResolver, mID, msgReceived);
					} else if (new ArriveState().isBelongToMe(object)) {
						ArriveState arriveState = new ArriveState().fromJson(json);
						ContentHelper.ARRIVE_STATE.receive(mContentResolver, mID, arriveState);
					}
					//TODO ...


				} else {
					//throw new RuntimeException("mID无效:" + mID);
					L.e(this, "--------------------mID无效:" + mID);
				}
			} catch (JSONException e) {
				L.e(this, "++++++++++++++++handleString----JSONException:" + json);
			}
		}
	}
}
