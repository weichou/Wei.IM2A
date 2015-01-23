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

package com.wei.c.im.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import com.wei.c.L;
import com.wei.c.im.core.Receiver.EntityParser;
import com.wei.c.im.core.Sender.SenderAdapter;

public class ConnectionManager implements IServer {
	private static final String TAG = ConnectionManager.class.getSimpleName();

	private Socket mSocket;
	private Receiver mReceiver;
	private Sender mSender;
	private InputStream mIn;
	private OutputStream mOut;
	private ConnectionConfig mConnConfig;
	private OnConnectionListener mOnConnectionListener;
	//private ConnectionObservable mObservable = new ConnectionObservable();

	private boolean mReceiverStarted = false, mSenderStarted = false;
	private AtomicBoolean mAlive = new AtomicBoolean(false);

	/**无法在{@link ConnectionConfig}相同的情况下共用同一个Socket连接，因此这里总是直接创建一个新的。
	 * 在具体应用时应避免创建多个连接，而只使用一个。*/
	public static Connection getConnection(ConnectionConfig connConfig) {
		return new Connection(connConfig);
	}

	private ConnectionManager(ConnectionConfig connConfig) {
		mConnConfig = connConfig;
	}

	private void setOnConnectionListener(OnConnectionListener l) {
		mOnConnectionListener = l;
	}

	@Override
	public synchronized void start() {
		L.i(TAG, "--start");
		if(!mAlive.getAndSet(true)) {
			mReceiverStarted = mSenderStarted = false;
			try {
				InetAddress inetAddress = ByteStreamMatcher.isValid(mConnConfig.ipAddressBytes) ? mConnConfig.ipAddress != null ?
						InetAddress.getByAddress(mConnConfig.ipAddress, mConnConfig.ipAddressBytes) :
							InetAddress.getByAddress(mConnConfig.ipAddressBytes) : mConnConfig.ipAddress != null ?
									InetAddress.getByName(mConnConfig.ipAddress) : null;
									if(inetAddress == null) throw new IllegalArgumentException("没有有效的ip地址");
									mSocket = new Socket(inetAddress, mConnConfig.port);
									mSocket.setKeepAlive(true);
									startReceiver();
									startSender();
									L.i(TAG, "--started--");
			} catch(IOException e) {
				stop();
				reportError(e);
				L.e(TAG, "--start--error", e);
			}
		}
	}

	private void startReceiver() throws IOException {
		mIn = mSocket.getInputStream();
		if (mReceiver == null) {
			mReceiver = new Receiver(mIn, mConnConfig.rPreLoadingSequences, mConnConfig.rEndingSequences, mConnConfig.rHeartBeatSequences);
			mReceiver.setEntityParser(mConnConfig.entityParser);
			mReceiver.setOnTransferListener(mOnTransferListener);
		} else {
			mReceiver.setInputStream(mIn);
		}
		mReceiver.start();
	}

	private void startSender() throws IOException {
		mOut = mSocket.getOutputStream();
		if (mSender == null) {
			mSender = new Sender(mOut, mConnConfig.sPreLoadingSequences, mConnConfig.sEndingSequences, mConnConfig.sHeartBeatSequences,
					mConnConfig.heartBeatTimeInterval, mConnConfig.authorizeNeeded);
			mSender.setAdapter(mConnConfig.senderAdapter);
			mSender.setOnTransferListener(mOnTransferListener);
		} else {
			mSender.setOutputStream(mOut);
		}
		mSender.start();
	}

	@Override
	public synchronized void stop() {
		L.i(TAG, "--stop");
		if(mAlive.getAndSet(false)) {
			if (mSender != null) mSender.stop();
			if (mReceiver != null) mReceiver.stop();
			mSocket = null;
			decideIfReportStopped();
			L.i(TAG, "--stopped--");
		}
	}

	/**暴力停止，用于在切换账号登录时使用**/
	public void forceStop() {
		L.i(TAG, "--forceStop");
		if(mAlive.getAndSet(false)) {
			stopInputStream();
			stopOutputStream();
			if (mSender != null) mSender.stop();
			if (mReceiver != null) mReceiver.stop();
			mSocket = null;
			L.i(TAG, "--forceStop--");
		}
	}

	private void stopInputStream() {
		try {
			if(mIn != null) mIn.close();
		} catch (IOException e) {}
		mIn = null;
		L.i(TAG, "--stopInputStream--");
	}

	private void stopOutputStream() {
		try {
			if(mOut != null) mOut.close();
		} catch (IOException e) {}
		mOut = null;
		L.i(TAG, "--stopOutputStream--");
	}

	@Override
	public boolean isAlive() {
		return mAlive.get();
	}

	@Override
	public boolean canRestart() {
		return true;
	}

	private void reportStarted() {
		mOnConnectionListener.onStarted();
	}

	private void reportError(Exception e) {
		mOnConnectionListener.onError(e);
	}

	private void reportServerKicked() {
		mOnConnectionListener.onServerKicked();
	}

	private void reportStopped() {
		mOnConnectionListener.onStopped();
	}
	
	private void decideIfReportStopped() {
		if (mIn == null && mOut == null) {
			reportStopped();
		}
	}

	private ITransfer.OnTransferListener mOnTransferListener = new ITransfer.OnTransferListener() {

		@Override
		public void onStopWork(ITransfer transfer) {
			synchronized(ConnectionManager.this) {
				if (transfer == mReceiver) {
					stopInputStream();
				} else if (transfer == mSender) {
					stopOutputStream();
				}
				decideIfReportStopped();
			}
		}

		@Override
		public void onStartWork(ITransfer transfer) {
			synchronized(ConnectionManager.this) {
				if (transfer == mReceiver) {
					mReceiverStarted = true;
				} else if (transfer == mSender) {
					mSenderStarted = true;
				}
				if (mReceiverStarted && mSenderStarted) {
					reportStarted();
				}
			}
		}

		@Override
		public void onError(ITransfer transfer, Exception e) {
			synchronized(ConnectionManager.this) {
				L.e(TAG, e);
				if (e instanceof RuntimeException) {
					stop();
					reportError(e);
					throw (RuntimeException)e;
				} else {
					if (!isAlive() && e instanceof IOException) {
						//由关闭导致的，吞掉
					}
					reportError(e);
					stop();
					//关于网络连接状况的检查、重试、以及监听广播等操作都由外部执行，这里只报告错误
				}
			}
		}

		@Override
		public void onServerKicked(ITransfer transfer) {
			synchronized(ConnectionManager.this) {
				reportServerKicked();
				stop();
			}
		}
	};

	public final static class Connection {
		private ConnectionManager mConnectionManager;

		private Connection(ConnectionConfig connConfig) {
			/* 无法在ConnectionConfig相同的情况下共用同一个ConnectionManager，因此还是改为直接创建一个新的。
			 * 在具体应用时应避免创建多个连接，而只使用一个。*/
			mConnectionManager = new ConnectionManager(connConfig);
		}

		public void connect() {
			mConnectionManager.start();
		}

		public void disconnect() {
			mConnectionManager.stop();
		}

		public void forceStop() {
			mConnectionManager.forceStop();
		}

		public boolean isAlive() {
			return mConnectionManager.isAlive();
		}

		public void setAuthorizeSuccess() {
			if(mConnectionManager.isAlive()) mConnectionManager.mSender.setAuthorizeSuccess();
		}

		public void setOnConnectionListener(OnConnectionListener l) {
			mConnectionManager.setOnConnectionListener(l);
		}
	}

	public static class ConnectionConfig {
		public static byte[] END_SEQUENCES = new byte[]{ITransfer.CR, ITransfer.LF, ITransfer.CR, ITransfer.LF};
		public static byte[] HEART_BEAT_SEQUENCES = new byte[]{ITransfer.CR, ITransfer.LF};	//ITransfer.CRLF.getBytes(charset);

		public final String ipAddress;
		public final byte[] ipAddressBytes;
		public final int port;

		public final byte[] rPreLoadingSequences, rEndingSequences, rHeartBeatSequences;
		public final byte[] sPreLoadingSequences, sEndingSequences, sHeartBeatSequences;

		public final int heartBeatTimeInterval;
		public final boolean authorizeNeeded;
		public final EntityParser entityParser;
		public final SenderAdapter senderAdapter;

		public ConnectionConfig(byte[] ipAddressBytes, int port, EntityParser entityParser, SenderAdapter senderAdapter) {
			this(null, ipAddressBytes, port, null, END_SEQUENCES, HEART_BEAT_SEQUENCES,
					null, END_SEQUENCES, HEART_BEAT_SEQUENCES, 60 * 5 * 1000, true,
					entityParser, senderAdapter);
		}

		public ConnectionConfig(String ipAddress, int port, EntityParser entityParser, SenderAdapter senderAdapter) {
			this(ipAddress, null, port, null, END_SEQUENCES, HEART_BEAT_SEQUENCES,
					null, END_SEQUENCES, HEART_BEAT_SEQUENCES, 60 * 5 * 1000, true,
					entityParser, senderAdapter);
		}

		public ConnectionConfig(String ipAddress, byte[] ipAddressBytes, int port,
				byte[] rPreLoadingSeqs, byte[] rEndingSeqs, byte[] rHeartBeatSeqs,
				byte[] sPreLoadingSeqs, byte[] sEndingSeqs, byte[] sHeartBeatSeqs,
				int heartBeatTimeInterval, boolean authorizeNeeded,
				EntityParser entityParser, SenderAdapter senderAdapter) {
			this.ipAddress = ipAddress;
			this.ipAddressBytes = ipAddressBytes;
			this.port = port;
			this.rPreLoadingSequences = rPreLoadingSeqs;
			this.rEndingSequences = rEndingSeqs;
			this.rHeartBeatSequences = rHeartBeatSeqs;
			this.sPreLoadingSequences = sPreLoadingSeqs;
			this.sEndingSequences = sEndingSeqs;
			this.sHeartBeatSequences = sHeartBeatSeqs;
			this.heartBeatTimeInterval = heartBeatTimeInterval;
			this.authorizeNeeded = authorizeNeeded;
			this.entityParser = entityParser;
			this.senderAdapter = senderAdapter;
		}

		@Override
		public int hashCode() {
			if (ipAddress != null) {
				return ipAddress.hashCode() + port;
			} else if (ipAddressBytes != null && ipAddressBytes.length >= 4) {
				int sum = port;
				for(byte b : ipAddressBytes) {
					sum += 0xff & b;
				}
				return sum;
			}
			return super.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) return true;
			ConnectionConfig cc = (ConnectionConfig)o;
			if (cc.hashCode() == this.hashCode()) {
				if (cc.port == this.port) {
					if (cc.ipAddress != null && this.ipAddress != null) {
						return cc.ipAddress.equals(this.ipAddress);
					} else if(cc.ipAddressBytes != null && this.ipAddressBytes != null
							&& cc.ipAddressBytes.length == this.ipAddressBytes.length) {
						for(int i = 0; i < cc.ipAddressBytes.length; i++) {
							if ((0xff & cc.ipAddressBytes[i]) != (0xff & this.ipAddressBytes[i])) return false;
						}
						return true;
					} else {
						return false;
					}
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
	}

	/*private static class ConnectionObservable extends Observable<OnConnectionListener> {
		public void onStarted() {
			synchronized(mObservers) {
				for (int i = mObservers.size() - 1; i >= 0; i--) {
					mObservers.get(i).onStarted();
				}
			}
		}

		public void onError(Exception e) {
			synchronized(mObservers) {
				for (int i = mObservers.size() - 1; i >= 0; i--) {
					mObservers.get(i).onError(e);
				}
			}
		}

		public void onServerKicked() {
			synchronized(mObservers) {
				for (int i = mObservers.size() - 1; i >= 0; i--) {
					mObservers.get(i).onServerKicked();
				}
			}
		}

		public void onStopped() {
			synchronized(mObservers) {
				for (int i = mObservers.size() - 1; i >= 0; i--) {
					mObservers.get(i).onStopped();
				}
			}
		}
	}*/

	public static interface OnConnectionListener {
		void onStarted();
		void onServerKicked();
		void onError(Exception e);
		void onStopped();
	}
}
