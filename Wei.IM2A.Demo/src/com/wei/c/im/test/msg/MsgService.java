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

import java.io.IOException;
import java.net.UnknownHostException;

import android.os.Message;

import com.wei.c.L;
import com.wei.c.framework.UserHelper;
import com.wei.c.im.core.ConnectionManager.ConnectionConfig;
import com.wei.c.im.service.IMService;
import com.wei.c.im.test.UID;
import com.wei.c.im.test.exception.TokenNotValidException;
import com.wei.c.im.test.msg.data.MSG;
import com.wei.c.im.test.msg.data.receive.Authority;

public class MsgService extends IMService {
	private static final byte[] sIpAddressBytes = new byte[] {(byte)192, (byte)168, 1, 100};
	private static final int sPort = 8282;

	private StringMsgAdapter mMsgAdapter;
	private StringMsgParser mMsgParser;
	private boolean mServerKicked = false;
	//private boolean mReadyForStop = false;
	private boolean mAuthorizeSuccess = false;

	@Override
	protected boolean isPermissionAllowed() {
		return MSG.isPermissionAllowed(this);
	}

	@Override
	public void onCreate() {
		L.i(this, "++++++++++++++++onCreate++++++++++++++++++++");
		super.onCreate();
		mMsgParser = new StringMsgParser(this);
		mMsgAdapter = new StringMsgAdapter(this);
	}

	@Override
	public void onDestroy() {
		L.i(this, "++++++++++++++++onDestroy++++++++++++++++++++");
		mMsgParser = null;
		mMsgAdapter = null;
		super.onDestroy();
	}

	@Override
	protected ConnectionConfig getConnectionConfig() {
		L.i(this, "++++++++++++++++getConnectionConfig++++++++++++++++++++");
		return new ConnectionConfig(sIpAddressBytes, sPort, mMsgParser, mMsgAdapter);
	}

	@Override
	protected synchronized boolean isReadyForStop() {
		L.i(this, "++++++++++++++++isReadyForStop++++++++++++++++++++");
		return true;
	}

	@Override
	protected void onPreparedToConnect() {
		L.i(this, "++++++++++++++++onPreparedToConnect++++++++++++++++++++");
		if (!isAuthorityTokenValid()) {
			L.i(this, "++++++++++++++++onPreparedToConnect++++++++++stopForAuthorityTokenNotValid++++++++++");
			stopForAuthorityFailed();
		}
	}

	protected boolean isAuthorityTokenValid() {
		L.i(this, "++++++++++++++++isAuthorityTokenValid++++++++++++++++++++");
		try {
			UID.getMyID(this);
			UID.nextToken(this);
			return true;
		} catch (TokenNotValidException e) {}
		return false;
	}

	@Override
	protected void onConnectionServerKicked() {
		L.i(this, "++++++++++++++++onConnectionServerKicked++++++++++++++++++++");
		mServerKicked = true;
		CSHelper.sendErrorServerKickedToClient(this);
	}

	@Override
	protected void onConnectionStopped() {
		L.i(this, "++++++++++++++++onConnectionStopped++++++++++++++++++++");
		if (mServerKicked && mAuthorizeSuccess && isAuthorityTokenValid()) retryAuthorize();
		mServerKicked = false;
		mAuthorizeSuccess = false;
	}

	@Override
	protected void onConnectionError(Exception e) {
		L.i(this, "++++++++++++++++onConnectionError++++++++++++++++++++");
		if (e instanceof UnknownHostException) {
			CSHelper.sendUnknownHostToClient(this);
		} else if (e instanceof IOException) {
			CSHelper.sendErrorIOToClient(this);
		}
	}

	@Override
	protected void handleClientMessage(Message msg) {
		/* 只要连接没断开，则收到的就是上一个ID的消息，那么不改变mMsgParser的ID即可，
		 * 而mMsgAdapter的ID没有改变，发送的也是该ID的消息，因此不会在切换用户时造成混乱。
		 */
	}

	public synchronized void setAuthority(Authority authority) {
		mAuthorizeSuccess = authority.isSuccess();
		//写入鉴权失败标记，因为有可能client还没有bind或没有启动，本服务独自在后台运行，无法接收反馈信息，因此不应发给client去进行保存处理
		UserHelper.saveAuthorityFlag(this, mAuthorizeSuccess);
		if (mAuthorizeSuccess) {
			mMsgParser.setUid(authority.uid);
			mMsgAdapter.setUid(authority.uid);
			setAuthorizeSuccess();
			CSHelper.sendAuthoritySuccessToClient(this);
		} else {
			setAuthorizeFailed();
			stopForAuthorityFailed();
			if (authority.isInvalid()) {
				CSHelper.sendAuthorityFailedToClient(this);
			} else if (authority.isAnotherClientLogin()) {
				CSHelper.sendAnotherClientLoginToClient(this);
			}
		}
	}
}
