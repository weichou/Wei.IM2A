/*
 * 版权所有 (C) 2014 周伟 (weichou2010@gmail.com)
 * 
 * 本程序为自由软件：您可依据自由软件基金会所发表的 GNU 通用公共授权条款，对本程序再次发布和/或修改；
 * 无论您依据的是本授权的第三版，或（您可选的）任一日后发行的版本。
 * 
 * 本程序是基于使用目的而加以发布，然而不负任何担保责任；亦无对适售性或特定目的适用性所为的默示性担保。
 * 详情请参照 GNU 通用公共授权。
 * 
 * 您应已收到附随于本程序的 GNU 通用公共授权的副本；如果没有，请参照
 * 
 *      http://www.gnu.org/licenses/
 * 
 * 
 * Copyright (C) 2014 Wei Chou (weichou2010@gmail.com)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see
 * 
 *      http://www.gnu.org/licenses/
 */

package com.wei.c.im.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;

import com.wei.c.L;
import com.wei.c.im.core.ConnectionManager;
import com.wei.c.im.core.ConnectionManager.Connection;
import com.wei.c.im.core.ConnectionManager.ConnectionConfig;
import com.wei.c.im.core.ConnectionManager.OnConnectionListener;
import com.wei.c.phone.Network;
import com.wei.c.phone.Network.State;
import com.wei.c.phone.Network.Type;
import com.wei.c.receiver.net.NetConnectionReceiver;
import com.wei.c.receiver.net.NetObserver;
import com.wei.c.utils.SPref;

public abstract class IMService extends Service {
	public static final String TAG                      = IMService.class.getSimpleName();

	public static final String KEY_SOCKET_CONNECTED     = "socket.connected";
	public static final String KEY_AUTHORIZE_SUCCESS    = "authorize.success";

	public static final String EXTRA_STOP               = "com.wei.c.im.service.EXTRA_STOP";

	public static final int MSG_REPLY_TO                = 999999999;
	public static final int MSG_AUTHORITY_RETRY         = MSG_REPLY_TO - 1;
	public static final int MSG_CONNECTION_RETRY        = MSG_REPLY_TO - 2;

	public static void start(Context context, Class<? extends IMService> clazz) {
		context.startService(new Intent(context, clazz));
	}

	public static void stop(Context context, Class<? extends IMService> clazz) {
		Intent intent = new Intent(context, clazz);
		intent.putExtra(EXTRA_STOP, true);
		context.startService(intent);	//发送一个请求让其自己关闭，而不是直接stopService()

		/*
		 * 几种stopService()的异同：
		 * 
		 * Context.stopService()
		 * 不论之前调用过多少次startService()，都会在调用一次本语句后关闭Service.
		 * 但是如果有还没断开的bind连接，则会一直等到全部断开后自动关闭Service.
		 * 
		 * Service.stopSelf()完全等同于Context.stopService().
		 * 
		 * stopSelfResult(startId)
		 * 只有startId是最后一次onStartCommand()所传来的时，才会返回true并执行与stopSelf()相同的操作.
		 * 
		 * stopSelf(startId)等同于stopSelfResult(startId)，只是没有返回值.
		 */
	}

	public static void bind(Context context, ServiceConnection conn, Class<? extends IMService> clazz) {
		start(context, clazz);
		L.d(IMService.class, "static----bind-----conn----clazz---");
		context.bindService(new Intent(context, clazz), conn, Context.BIND_AUTO_CREATE);
	}

	public static void unbind(Context context, ServiceConnection conn) {
		try{	//如果Service已经被系统销毁，则这里会出现异常
			context.unbindService(conn);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Client调用本方法以使Service端可以向Client发送Message
	 * @param messenger Client取得的面向Service的信使对象
	 * @param handler Client用来处理Service发来的Message的Handler
	 * @return 是否建立信使成功
	 */
	public static boolean replyToMe(Messenger messenger, Handler handler) {
		Message msg = Message.obtain();
		msg.what = MSG_REPLY_TO;
		msg.replyTo = new Messenger(handler);
		try {
			messenger.send(msg);
			return true;
		} catch (RemoteException e) {
			L.e(IMService.class, e);
			return false;
		}
	}

	public static boolean sendRetryConnectToService(Messenger messenger) {
		return sendMessageToService(messenger, MSG_CONNECTION_RETRY, 0, null);
	}

	public static boolean sendRetryAuthorizeToService(Messenger messenger) {
		return sendMessageToService(messenger, MSG_AUTHORITY_RETRY, 0, null);
	}

	public static boolean isMessageBelongsToRetryConnect(Message msg) {
		return msg.what == MSG_CONNECTION_RETRY;
	}

	public static boolean isMessageBelongsToRetryAuthorize(Message msg) {
		return msg.what == MSG_AUTHORITY_RETRY;
	}

	public static boolean isSocketConnected(Context context) {
		return SPref.getBooleanFromFile(context, IMService.class, KEY_SOCKET_CONNECTED);
	}

	private static void setSocketConnected(Context context, boolean connected) {
		SPref.saveBooleanAsFile(context, IMService.class, KEY_SOCKET_CONNECTED, connected);
	}

	public static boolean isAuthorizeSuccess(Context context) {
		return SPref.getBooleanFromFile(context, IMService.class, KEY_AUTHORIZE_SUCCESS);
	}

	private static void setAuthorityFlag(Context context, boolean success) {
		SPref.saveBooleanAsFile(context, IMService.class, KEY_AUTHORIZE_SUCCESS, success);
	}

	public static boolean sendMessageToService(Messenger messenger, int what, int state, Bundle data) {
		try {
			Message msg = Message.obtain();
			msg.what = what;
			msg.arg1 = state;
			msg.setData(data);
			messenger.send(msg);
			return true;
		} catch (RemoteException e) {
			L.e(TAG, e);
			return false;
		}
	}

	public static void sendMessageToClient(IMService service, int what, int state, Bundle data) {
		Message msg = Message.obtain();
		msg.what = what;
		msg.arg1 = state;
		msg.setData(data);
		service.sendMessageToClient(msg);
	}

	/*************************************************************************************/

	private boolean mAllClientDisconnected = true, mStopRequested = false, mConnStopped = true;
	private HandlerThread mHandlerThread;
	private Handler mMainHandler, mClientHandler;
	private Messenger mMessenger;
	private IMObservable mIMObservable = new IMObservable();
	private volatile Connection mConnection;
	private volatile boolean mKeepConnectionAlive = true, mServerKicked = false;
	private Type mAllowedNetType = Type.G2;
	private PowerManager.WakeLock mWakeLock = null;
	private long mRetryCount, mRetryTime;

	private Handler getClientHandler() {
		if(mClientHandler == null) {
			//只允许一个实例，所以一个不变的name
			mHandlerThread = new HandlerThread(getClass().getName(), Process.THREAD_PRIORITY_BACKGROUND);
			mHandlerThread.start();
			mClientHandler = new Handler(mHandlerThread.getLooper()) {
				@Override
				public void handleMessage(Message msg) {
					if (msg.what == MSG_REPLY_TO) {
						if(msg.replyTo != null) mIMObservable.registerObserver(new IMObserver(msg.replyTo, mIMObservable));
					} else if (isMessageBelongsToRetryConnect(msg)) {
						startWork();
					} else if (isMessageBelongsToRetryAuthorize(msg)) {
						retryAuthorize();
					} else {
						mStopRequested = false;
						handleClientMessage(msg);
					}
				}
			};
		}
		return mClientHandler;
	}

	private Messenger getMessenger() {
		if(mMessenger == null) mMessenger = new Messenger(getClientHandler());
		return mMessenger;
	}

	private final NetObserver mNetObserver = new NetObserver() {
		@Override
		public void onChanged(Type type, State state) {
			if (isReadyForConnect(type, state)) {
				decideIfKeepConnectionAlive();
			}
		}
	};

	@Override
	public void onCreate() {
		try {	//需要权限 android.permission.WAKE_LOCK
			mWakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
			mWakeLock.acquire();
		} catch (Exception e) {}

		mMainHandler = new Handler();
		NetConnectionReceiver.registerObserver(mNetObserver);
		super.onCreate();
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (!isPermissionAllowed()) {
			L.e(TAG, "App鉴权没有通过---------");
			return null;
		}
		L.e(IMService.class, "bind------------");
		mAllClientDisconnected = false;
		return getMessenger().getBinder();
	}

	@Override
	public boolean onUnbind(Intent intent) {	//当所有的bind连接都断开之后会回调
		mAllClientDisconnected = true;
		mIMObservable.unregisterAll();
		decideIfStop();
		return super.onUnbind(intent);	//默认返回false. 当返回true时，下次的的bind操作将执行onRebind()
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//startForeground(id, notification);
		if(!decideIfStop(intent)) {
			startWork();	//本服务就是要时刻保持连接畅通的
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		NetConnectionReceiver.unregisterObserver(mNetObserver);
		mClientHandler = null;
		if(mHandlerThread != null) {
			mHandlerThread.quit();
			mHandlerThread = null;
			mConnection = null;
		}
		if (mWakeLock != null) {
			mWakeLock.release();
			mWakeLock = null;
		}
		super.onDestroy();
	}

	private boolean decideIfStop() {
		return decideIfStop(null);
	}

	private boolean decideIfStop(Intent intent) {
		if(intent != null) mStopRequested = intent.getBooleanExtra(EXTRA_STOP, false);
		if(mStopRequested && mAllClientDisconnected) {
			stopWorkGraceful();
		}
		return mStopRequested;
	}

	private void stopWorkGraceful() {
		stopWork();
		getClientHandler().post(new Runnable() {
			@Override
			public void run() {
				//让消息跟onBind()等排队执行
				postStopSelf(0);
			}
		});
	}

	private void postStopSelf(int delay) {
		mMainHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if(isReadyForStopInner()) {
					stopSelf();	//完全准备好了，该保存的都保存了，那就关闭吧
				}else if(mStopRequested && mAllClientDisconnected) {
					postStopSelf(1000);	//子类还没准备好关闭，数据还没保存完整，推迟关闭
				}else {
					//可能又重新bind()了
				}
			}
		}, delay);
	}

	private boolean isReadyForStopInner() {
		return mStopRequested && mAllClientDisconnected && mConnStopped && isReadyForStop();
	}

	private void startWork() {
		L.i(TAG, "startWork");
		setKeepConnectionAlive(true);
		mRetryCount = 0;
		mServerKicked = false;
		startConnection(0);
	}

	private void stopWork() {
		L.i(TAG, "stopWork");
		setKeepConnectionAlive(false);
		closeConnection();
	}

	private void decideIfKeepConnectionAlive() {
		if (mKeepConnectionAlive) startConnection(0);
	}

	private void closeConnection() {
		getClientHandler().post(new Runnable() {
			@Override
			public void run() {
				if (mConnection != null) mConnection.disconnect();
			}
		});
	}

	protected void startConnection(int delay) {
		if (!isConnected() && isReadyForConnect(null, null)) {
			getClientHandler().postDelayed(new Runnable() {
				@Override
				public void run() {
					if (mConnection == null) {
						ConnectionConfig connConf = getConnectionConfig();
						mConnection = ConnectionManager.getConnection(connConf);
						mConnection.setOnConnectionListener(new OnConnectionListener() {

							@Override
							public synchronized void onStarted() {
								mConnStopped = false;
								mServerKicked = false;
								setSocketConnected(IMService.this, true);
								onConnectionStarted();
							}

							@Override
							public void onServerKicked() {
								mServerKicked = true;
								setKeepConnectionAlive(false);
								setSocketConnected(IMService.this, false);
								onConnectionServerKicked();
							}

							@Override
							public void onError(Exception e) {
								if (e instanceof RuntimeException) {
									stopWork();
									requestStopService();
								}
								onConnectionError(e);
							}

							@Override
							public synchronized void onStopped() {
								mConnStopped = true;
								setSocketConnected(IMService.this, false);
								onConnectionStopped();
								decideIfRetryConnection();
							}
						});
					}
					onPreparedToConnect();
					if (!isConnected() && isReadyForConnect(null, null)) mConnection.connect();
				}
			}, delay);
		}
	}

	private void decideIfRetryConnection() {
		if (!isConnected() && isReadyForConnect(null, null)) {
			if (++mRetryCount > 5 && System.currentTimeMillis() - mRetryTime < 20000) {
				requestStopService();
			} else {
				mRetryTime = System.currentTimeMillis();
				int delay = getRetryDelayTime();
				if (delay < 5000) delay = 5000;
				startConnection(delay);
			}
		}
	}

	private boolean isReadyForConnect(Type type, State state) {
		if (!mServerKicked && mKeepConnectionAlive) {
			if (type == null) type = Network.getConnectedNetworkType(this);
			if (state == null) state = Network.getNetworkState(this);
			return type.ordinal() >= mAllowedNetType.ordinal() && state == State.CONNECTED;
		}
		return false;
	}

	private boolean isConnected() {
		return mConnection != null && mConnection.isAlive();
	}

	public void setKeepConnectionAlive(boolean b) {
		mKeepConnectionAlive = b;
	}

	public boolean getKeepConnectionAlive() {
		return mKeepConnectionAlive;
	}

	public void setAllowedNetType(Type type) {
		mAllowedNetType = type;
	}

	public Type getAllowedNetType() {
		return mAllowedNetType;
	}

	public void setAuthorizeSuccess() {
		mConnection.setAuthorizeSuccess();
		setAuthorityFlag(this, true);
	}

	public void setAuthorizeFailed() {
		setAuthorityFlag(this, false);
	}

	protected abstract boolean isPermissionAllowed();
	protected abstract ConnectionConfig getConnectionConfig();
	protected abstract void handleClientMessage(Message msg);
	/**是否准备好关闭本Service。关闭Service常用于用户选择退出当前账号或者设置为不在后台收到消息。
	 * 该方法回调时，连接中的各个线程均已结束，而对应在SenderAdapter和EntityParser中的全部回调事件均已执行完毕，
	 * 而此时通常该保存的数据都已保存完毕。
	 * 因此若无其他线程操作，可直接返回true**/
	protected abstract boolean isReadyForStop();

	protected void onPreparedToConnect() {}
	protected void onConnectionStarted() {}
	protected void onConnectionServerKicked() {}
	protected void onConnectionError(Exception e) {}
	protected void onConnectionStopped() {}

	protected int getRetryDelayTime() {
		return 5000;
	}

	public void sendMessageToClient(final Message msg) {
		getClientHandler().post(new Runnable() {
			@Override
			public void run() {
				mIMObservable.sendMessage(msg);
			}
		});
	}

	public void retryAuthorize() {
		mMainHandler.post(new Runnable() {
			@Override
			public void run() {
				closeConnection();
				startWork();
			}
		});
	}

	public void stopForAuthorityFailed() {
		setKeepConnectionAlive(false);
		mMainHandler.post(new Runnable() {
			@Override
			public void run() {
				stopWork();
				requestStopService();
			}
		});
	}

	public void stopConnection() {
		mMainHandler.post(new Runnable() {
			@Override
			public void run() {
				stopWork();
			}
		});
	}

	/**在没有client bind的情况下，会停止Service，否则等待最后一个client取消bind的时候会自动断开**/
	public void requestStopService() {
		mMainHandler.post(new Runnable() {
			@Override
			public void run() {
				mStopRequested = true;
				decideIfStop();
			}
		});
	}
}