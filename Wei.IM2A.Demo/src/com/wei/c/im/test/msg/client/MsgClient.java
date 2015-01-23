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

import java.util.List;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.util.LongSparseArray;

import com.wei.c.L;
import com.wei.c.im.service.IMService;
import com.wei.c.im.test.data.MsgBean;
import com.wei.c.im.test.data.NoticeBean;
import com.wei.c.im.test.msg.CSHelper;
import com.wei.c.im.test.msg.data.ContentHelper;
import com.wei.c.im.test.msg.data.MSG;
import com.wei.c.im.test.msg.data.MsgProvider;
import com.wei.c.im.test.msg.data.send.MsgSend;
import com.wei.c.phone.Network;
import com.wei.c.phone.Network.State;
import com.wei.c.phone.Network.Type;
import com.wei.c.receiver.net.NetConnectionReceiver;
import com.wei.c.receiver.net.NetObserver;

/**由于Context的原因，必须一个Activity一个实例**/
@SuppressLint("HandlerLeak")
public class MsgClient {

	/**不能把发送和接收的放在同一个Map，由于Id相同，会覆盖**/
	private LongSparseArray<MsgBean> mMsgMapSend = new LongSparseArray<MsgBean>();
	/**不能把发送和接收的放在同一个Map，由于Id相同，会覆盖**/
	private LongSparseArray<MsgBean> mMsgMapReceived = new LongSparseArray<MsgBean>();
	private int mLoadMsgCount = 20;
	private long mID, uID;
	private OnChatObserver mOnChatObserver;
	private OnSysPushObserver mOnSysPushObserver;

	private Context mContext;
	private ContentResolver mContentResolver;
	private Messenger mSender;
	private Handler mHandler;
	private boolean mServiceBinded = false;
	private boolean mNetConnected = false;

	public MsgClient(Context context) {
		mContext = context;
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (CSHelper.isMessageBelongToAuthorizeSuccess(msg)) {
					L.i(this, "==========handleMessage========MessageBelongToAuthorizeSuccess");
				} else if (CSHelper.isMessageBelongToAuthorizeFailed(msg)) {
					L.i(this, "==========handleMessage========MessageBelongToAuthorizeFailed");
					//TODO 检查用户登录状况，并确定是重试还是重新登录
				} else if (CSHelper.isMessageBelongToServerKickedError(msg)) {
					L.i(this, "==========handleMessage========MessageBelongToServerKickedError");
					//unbindService();
					//TODO 检查用户登录状况，并确定是重试还是重新登录
				} else if (CSHelper.isMessageBelongToUnknownHost(msg)) {
					L.i(this, "==========handleMessage========MessageBelongToUnknownHost");
					//TODO 检查网络连接并提示用户
				} else if (CSHelper.isMessageBelongToIOError(msg)) {
					L.i(this, "==========handleMessage========MessageBelongToIOError");
					//TODO 检查网络连接并提示用户
				} else if (CSHelper.isMessageBelongToError(msg)) {
					L.i(this, "==========handleMessage========MessageBelongToError");

				} else if (CSHelper.isMessageBelongToAnotherClientLogin(msg)) {
					L.i(this, "==========handleMessage========AnotherClientLogin");
					//TODO 踢下线
				} else {
					L.i(this, "==========handleMessage========msg.what:" + msg.what + ", msg.arg1:" + msg.arg1);
				}
			}
		};
	}

	public void onActivityStart() {
		NetConnectionReceiver.registerObserver(mNetObserver);
		bindService();
		mNetConnected = Network.isNetConnected(mContext);
		registerMsgObserver();
	}

	public void onActivityStop() {
		unregisterMsgObserver();
		unbindService();
		NetConnectionReceiver.unregisterObserver(mNetObserver);
	}

	public void onActivityDestroy() {
		mContentResolver = null;
		mSender = null;
		mHandler = null;
	}

	public void setIDs(long id_me, long id_u) {
		mID = id_me;
		uID = id_u;
	}

	/**
	 * 发送消息
	 * @param contentType 消息内容的类型：1文字，2图片，3声音
	 * @param msg
	 * @return true表示发送成功，false表示发送失败。当失败时应检查原因，是网络没有连接，还是服务没有运行
	 */
	public boolean sendMsg(int contentType, String msg) {
		MsgSend msgSend = new MsgSend(mID, uID, contentType, msg);
		ContentHelper.MSG_SEND.insertNewMsg(getContentResolver(), mID, msgSend);
		return checkIsStateOK();
	}

	public boolean resendMsg(long msgId) {
		ContentHelper.MSG_SEND.resendMsg(getContentResolver(), msgId);
		return checkIsStateOK();
	}

	public void sendBackStateRead(long _id) {
		ContentHelper.ARRIVE_STATE.sendBackStateRead(getContentResolver(), mID, uID, _id);
	}

	private boolean checkIsStateOK() {
		if (!isNetConnected()) {
			return false;
		}
		if (!isServiceBinding()) {
			bindService();
			return false;
		}
		if (!isAuthorizeSuccess()) {
			IMService.sendRetryAuthorizeToService(mSender);
		}
		if (!isSocketConnected()) {
			IMService.sendRetryConnectToService(mSender);
		}
		return true;
	}

	private void registerMsgObserver() {
		L.d(MsgClient.this, "registerMsgObserver---------mContentObserver");
		getContentResolver().registerContentObserver(MSG.URI_SAY_HI, true, mContentObserver);
		getContentResolver().registerContentObserver(MSG.URI_MSG_SEND, true, mContentObserver);
		getContentResolver().registerContentObserver(MSG.URI_MSG_RECEIVED, true, mContentObserver);
		getContentResolver().registerContentObserver(MSG.URI_ARRIVE_STATE, true, mContentObserver);
	}

	private void unregisterMsgObserver() {
		L.d(MsgClient.this, "unregisterMsgObserver---------mContentObserver");
		mContentResolver.unregisterContentObserver(mContentObserver);
	}

	public boolean isNetConnected() {
		return mNetConnected;
	}

	public boolean isServiceBinding() {
		if (mServiceBinded) {
			if (!mSender.getBinder().pingBinder()) {
				L.d(MsgClient.this, "isServiceBinding---------pingBinder:false");
				unbindService();
			}
		}
		return mServiceBinded;
	}

	public boolean isAuthorizeSuccess() {
		return IMService.isAuthorizeSuccess(mContext);
	}

	public boolean isSocketConnected() {
		return IMService.isSocketConnected(mContext);
	}

	private ContentResolver getContentResolver() {
		if (mContentResolver == null) mContentResolver = mContext.getContentResolver();
		return mContentResolver;
	}

	private void bindService() {
		CSHelper.bindService(mContext, mServiceConnection);
	}

	private void unbindService() {
		mServiceBinded = false;
		mSender = null;
		CSHelper.unbindService(mContext, mServiceConnection);
	}

	//参数的这个mHandler不起作用，因为在创建本实例的时候，mHandler还没有创建，不过也好，让数据的加载操作在另一个线程
	private ContentObserver mContentObserver = new ContentObserver(mHandler) {
		@Override
		public void onChange(boolean selfChange, Uri uri) {
			switch (MsgProvider.match(uri)) {
			case MsgProvider.SAY_HI:
			case MsgProvider.SAY_HI_ID:
				doLoadAndPostSysPushs();
				break;
			default:
				doLoadAndPostSysPushs();	//陌生人发消息需要显示数量
				doParseAndPostChatMsgs(uri);
				break;
			}
		}
	};

	public void loadSysPushs() {
		doLoadAndPostSysPushs();
	}

	public void loadChatMsgs() {
		decideIfLoadAndPostChatMsgs();
	}

	public void moreChatMsgs() {
		mLoadMsgCount += 10;
		decideIfLoadAndPostChatMsgs();
	}

	private void doLoadAndPostSysPushs() {
		if (mOnSysPushObserver != null) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					mOnSysPushObserver.onSysPush(MsgListHelper.loadSysPushs(getContentResolver(), mID));
				}
			});
		}
	}

	private void decideIfLoadAndPostChatMsgs() {
		if (mOnChatObserver != null) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					MsgListHelper.loadMsgBeansToMaps(getContentResolver(), mID, uID, mMsgMapSend, mMsgMapReceived, mLoadMsgCount);
					mOnChatObserver.onMsgChange(MsgListHelper.makeMsgList(mMsgMapSend, mMsgMapReceived));
				}
			});
		}
	}

	private void doParseAndPostChatMsgs(final Uri uri) {
		if (mOnChatObserver != null) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					MsgListHelper.parseAndUpdateMaps(getContentResolver(), uri, mID, uID, mMsgMapSend, mMsgMapReceived, mLoadMsgCount);
					mOnChatObserver.onMsgChange(MsgListHelper.makeMsgList(mMsgMapSend, mMsgMapReceived));
				}
			});
		}
	}

	private NetObserver mNetObserver = new NetObserver() {
		@Override
		public void onChanged(Type type, State state) {
			mNetConnected = state == State.CONNECTED;
		}
	};

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mSender = new Messenger(service);
			if (CSHelper.replyToClient(mSender, mHandler)) {
				mServiceBinded = true;
			} else {
				mServiceBinded = false;
				unbindService();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			unbindService();
		}
	};

	public void registerOnChatObserver(OnChatObserver observer) {
		mOnChatObserver = observer;
	}

	public void registerOnSysPushObserver(OnSysPushObserver observer) {
		mOnSysPushObserver = observer;
	}

	/**聊天**/
	public static interface OnChatObserver {
		void onMsgChange(List<MsgBean> list);
	}

	/**系统通知**/
	public static interface OnSysPushObserver {
		void onSysPush(List<NoticeBean> list);
	}
}
