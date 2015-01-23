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
import java.io.OutputStream;

import android.database.Observable;

import com.wei.c.L;

public class Sender extends AbsTransfer {
	private OutputStream mOut;
	private byte[] mStartSequences, mEndSequences, mHeartBeatSequences;
	private int mHeartBeatTimeInterval;
	private SenderAdapter mAdapter;
	private SenderObserver mSenderObserver;
	private byte[] mBuffer;
	private boolean mSendPrev = false;
	private boolean mAuthorizeNeeded = true;
	private boolean mAuthorizeSuccess = false;

	public Sender(OutputStream out, byte[] startSequences, byte[] endSequences, byte[] heartBeatSequences) {
		this(out, startSequences, endSequences, heartBeatSequences, 60 * 5 * 1000, true);
	}

	public Sender(OutputStream out, byte[] startSequences, byte[] endSequences, byte[] heartBeatSequences, int heartBeatTimeInterval, boolean authorizeNeeded) {
		mOut = out;
		mStartSequences = startSequences;
		mEndSequences = endSequences;
		mHeartBeatSequences = heartBeatSequences;
		mHeartBeatTimeInterval = heartBeatTimeInterval < 5000 ? 5000 : heartBeatTimeInterval;
		mAuthorizeNeeded = authorizeNeeded;
	}

	public synchronized void setOutputStream(OutputStream out) {
		mOut = out;
	}

	@Override
	public synchronized void stop() {
		super.stop();
		notifyWork();
	}

	public void notifyWork() {
		synchronized (this) {
			notifyAll();
		}
	}

	@Override
	public void doWork() throws Exception {
		if(mAuthorizeNeeded) {
			sendStartSequences();
			sendAuthorizeSequences();
			sendEndSequences();
			while (!isAuthorizeSuccess()) {
				L.i(this, "+++++++++++++++++isAuthorizeSuccess:" + false);
				if (!sendHeartBeat()) break;
			}
		}
		LOOP:while (isAlive()) {
			while (!hasNext()) {
				recycleBuffer();
				if (!sendHeartBeat()) break LOOP;
			}
			sendStartSequences();
			if (!sendEntity()) break LOOP;
			sendEndSequences();
			mOut.flush();
			mSendPrev = true;
			onSend();
		}
	}

	private void sendAuthorizeSequences() throws IOException {
		if(isAlive()) {
			byte[] seqs = getAuthorizeSequences();
			if (ByteStreamMatcher.isValid(seqs)) {
				synchronized (this) {
					if (mAuthorizeSuccess) {
						throw new RuntimeException("调用顺序错误：未开始发送鉴权信息，但已设置鉴权成功");
					}
					mOut.write(seqs);
					/* 原子操作。防止两个操作中间间隔太长，而此时已经返回了鉴权成功，并调用了setAuthorizeSuccess()，
					 * 而后执行了下面的语句，导致本来鉴权成功，而后面认为没成功的情况。
					 */
					mAuthorizeSuccess = false;
				}
				mSendPrev = true;
			} else {
				throw new RuntimeException("鉴权实体字节序列无效");
			}
		}
	}

	public final synchronized void setAuthorizeSuccess() {
		mAuthorizeSuccess = true;
		notifyWork();
	}

	private synchronized boolean isAuthorizeSuccess() {
		return mAuthorizeSuccess;
	}

	private boolean sendHeartBeat() throws IOException {
		if (mSendPrev) {
			mSendPrev = false;
		} else {
			L.i(this, "-|-|-|-|-|-|-|-|-|-|-|-|-|- heartBeat -|-|-|-|-|-|-|-|-|-|-|-|-|-");
			if (ByteStreamMatcher.isValid(mHeartBeatSequences)) {
				mOut.write(mHeartBeatSequences);
				//L.i(this, "heartBeat:" + ArrayUtils.toNumbers(mHeartBeatSequences, ","));
			}
		}
		synchronized (this) {
			if (!isAlive()) return false;
			try {
				wait(mHeartBeatTimeInterval);
			} catch (InterruptedException e) {}
		}
		return isAlive();
	}

	private void sendStartSequences() throws IOException {
		if (ByteStreamMatcher.isValid(mStartSequences)) mOut.write(mStartSequences);
	}

	private boolean sendEntity() throws IOException {
		int count;
		byte[] buffer = makeBuffer();
		boolean alive;
		while((alive = isAlive()) && (count = fillEntity(buffer)) != -1) {
			if(count > 0) mOut.write(buffer, 0, count);
		}
		mSendPrev = true;
		return alive;
	}

	private void sendEndSequences() throws IOException {
		if (ByteStreamMatcher.isValid(mEndSequences)) mOut.write(mEndSequences);
	}

	private byte[] makeBuffer() {
		if (mBuffer == null) {
			mBuffer = new byte[1024];
		}
		return mBuffer;
	}

	private void recycleBuffer() {
		mBuffer = null;
	}

	private byte[] getAuthorizeSequences() {
		return mAdapter.getAuthorizeSequences();
	}

	private final boolean hasNext() {
		return mAdapter != null ? mAdapter.onNewEntity() : false;
	}

	private final int fillEntity(byte[] buffer) {
		return mAdapter != null ? mAdapter.fillEntity(buffer) : -1;
	}

	private void onSend() {
		if (mAdapter != null) mAdapter.onSend();
	}

	public void setAdapter(SenderAdapter adapter) {
		if(mAdapter != null && mSenderObserver != null) {
			mAdapter.unregisterSenderObserver(mSenderObserver);
		}
		mAdapter = adapter;
		if(mAdapter != null) {
			if(mSenderObserver == null) mSenderObserver = new SenderObserver() {
				@Override
				public void onNotifyWork() {
					notifyWork();
				}
			};
			mAdapter.registerSenderObserver(mSenderObserver);
		}
	}

	@Override
	public boolean canRestart() {
		return true;
	}

	@Override
	protected synchronized void onStartWork() {
		mSendPrev = false;
		mAuthorizeSuccess = false;
		if (mAdapter != null) mAdapter.onStartWork();
	}

	@Override
	protected void onError() {}

	@Override
	protected synchronized void onStopWork() {
		recycleBuffer();
		if (mAdapter != null) mAdapter.onStopWork();
	}


	public static class SenderObservable extends Observable<SenderObserver> {
		public void notifyWork() {
			synchronized(mObservers) {
				for (int i = mObservers.size() - 1; i >= 0; i--) {
					mObservers.get(i).onNotifyWork();
				}
			}
		}
	}

	public static abstract class SenderObserver {
		public abstract void onNotifyWork();
	}

	public static interface SenderAdapter {
		/**
		 * 准备就绪，开始工作
		 */
		void onStartWork();
		/**
		 * 取得用于登录鉴权的信息的字节序列。不可能太长，因此直接返回字节序列，简化操作
		 * @return 返回用于登录鉴权的信息的字节序列
		 */
		byte[] getAuthorizeSequences();
		/**
		 * 构造新的实体，以准备发送
		 * @return 是否构造成功
		 */
		boolean onNewEntity();
		/**
		 * 将实体数据填充入缓冲
		 * @param buffer 要填入实体数据的缓冲区
		 * @return 返回填入数据的长度，如果返回-1，则表示实体结束
		 */
		int fillEntity(byte[] buffer);
		/**
		 * 消息实体发送完毕
		 */
		void onSend();
		/**
		 * 停止传送数据。可以保存状态，回收资源了
		 */
		void onStopWork();

		void registerSenderObserver(SenderObserver observer);
		void unregisterSenderObserver(SenderObserver observer);
	}
}
