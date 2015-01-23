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

import android.content.Context;
import android.content.pm.PackageManager;

import com.wei.c.im.test.exception.TokenNotValidException;
import com.wei.c.utils.RsaUtils;

public class UID {
	public static final long MIN_VALUE		= 1000000000;
	public static final long MAX_VALUE		= Long.MAX_VALUE;

	private static long mId	= 0;

	public static final boolean isValid(long uid) {
		return uid >= MIN_VALUE && uid <= MAX_VALUE;
	}

	public static final boolean isUsing(Context context, long uid) {
		try {
			return uid == getMyID(context);
		} catch (TokenNotValidException e) {}
		return false;
	}

	public static synchronized long getMyID(Context context) throws TokenNotValidException {
		//TODO 读取mid
		mId = MIN_VALUE + 1;
		return mId;
	}

	public static synchronized void updateToken(Context context, String token) {
		checkPermission(context);
		mId = 0;
		//TODO 保存token
	}

	public static synchronized String nextToken(Context context) throws TokenNotValidException {
		return "token-xxxxxx";
	}

	public static String encrypt(String s) throws Exception {
		return RsaUtils.encrypt(true, RSA_PUBLIC, s, CHARSET);
	}

	private static void checkPermission(Context context) {
		if(!isPermissionAllowed(context)) throw new SecurityException("没有存取权限");
	}

	private static boolean isPermissionAllowed(Context context) {
		return context.checkCallingOrSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED;
	}

	private static final String CHARSET		= "UTF-8";
	private static final String PERMISSION	= "com.wei.c.im.test.UID";
	private static final String RSA_PUBLIC	= "xxxxxx==";
}
