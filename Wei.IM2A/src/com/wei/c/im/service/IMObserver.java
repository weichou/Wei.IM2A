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

import java.lang.ref.WeakReference;

import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.wei.c.L;

public class IMObserver {
	private Messenger mMessenger;
	private WeakReference<IMObservable> mIMObservable;

	public IMObserver(Messenger messenger, IMObservable observable) {
		mMessenger = messenger;
		mIMObservable = new WeakReference<IMObservable>(observable);
	}

	public void onMessage(Message msg) {
		try {
			mMessenger.send(msg);
		} catch (RemoteException e) {
			L.e(this, e);
			if (!mMessenger.getBinder().pingBinder()) {
				IMObservable observable = mIMObservable.get();
				if(observable != null) observable.unregisterObserver(this);
			}
		}
	}
}
