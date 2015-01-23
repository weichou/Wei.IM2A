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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import android.os.Process;

import com.wei.c.L;

public abstract class AbsTransfer implements ITransfer {
	private AtomicBoolean mAlive = new AtomicBoolean(false);
	private boolean mDone = false;
	private ExecutorService mExecutor;
	protected OnTransferListener mOnTransferListener;

	public void setOnTransferListener(OnTransferListener l) {
		mOnTransferListener = l;
	}

	private ExecutorService getExecutor() {
		if(mExecutor == null) mExecutor = Executors.newFixedThreadPool(1);
		return mExecutor;
	}

	@Override
	public synchronized void start() {
		if(!mAlive.getAndSet(true)) {
			if(mDone && !canRestart()) throw new IllegalStateException("不可以再次启动");
			getExecutor().execute(this);
		}
	}

	@Override
	public synchronized void stop() {
		if(mAlive.getAndSet(false) && mExecutor != null) {
			mExecutor.shutdown();
			mExecutor = null;
		}
	}

	@Override
	public synchronized boolean isAlive() {
		return mAlive.get();
	}

	@Override
	public void run() {
		if(!isAlive()) return;
		synchronized(this) {
			mDone = true;
		}
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		reportStartWork();
		onStartWork();
		try {
			doWork();
		} catch (Exception e) {
			//说明连接已断开
			onError();
			reportError(e);
			L.e(this, e);
		}
		onStopWork();
		reportStopWork();
		stop();
	}

	protected void reportStartWork() {
		if(mOnTransferListener != null) mOnTransferListener.onStartWork(this);
	}

	protected void reportStopWork() {
		if(mOnTransferListener != null) mOnTransferListener.onStopWork(this);
	}

	protected void reportError(Exception e) {
		if(mOnTransferListener != null) mOnTransferListener.onError(this, e);
	}

	protected void reportServerKicked() {
		if(mOnTransferListener != null) mOnTransferListener.onServerKicked(this);
	}

	protected abstract void doWork() throws Exception;

	protected abstract void onStartWork();
	/**出错了，应该丢弃当前正在处理而不完整（没有读取到结尾）的数据包**/
	protected abstract void onError();
	protected abstract void onStopWork();
}
