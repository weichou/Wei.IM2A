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

package com.wei.c.im;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.wei.c.L;
import com.wei.c.im.core.Receiver.EntityParser;

public abstract class AbsStringEntityParser implements EntityParser {

	private String mCharset;
	private ByteArrayOutputStream mDataPackage;

	public AbsStringEntityParser() {
		this("utf-8");
	}

	public AbsStringEntityParser(String charset) {
		if (charset == null || charset.length() == 0) throw new IllegalArgumentException("参数charset不能为空");
		mCharset = charset;
	}

	@Override
	public void onBegin() {
		throwAway();
		mDataPackage = new ByteArrayOutputStream();
	}

	@Override
	public void onFill(byte[] buffer, int offset, int count) {
		mDataPackage.write(buffer, offset, count);
	}

	@Override
	public void onFinish() {
		if (mDataPackage != null) {
			try {
				handleString(mDataPackage.toString(mCharset));
			} catch (UnsupportedEncodingException e) {
				L.e(this, e);
				throw new RuntimeException(e.getMessage(), e);
			}
			throwAway();
		}
	}

	protected abstract void handleString(String json);

	@Override
	public void onAbort() {
		throwAway();
	}

	@Override
	public void onStartWork() {}

	@Override
	public void onStopWork() {
		throwAway();
	}

	private void throwAway() {
		if (mDataPackage != null) {
			try {
				mDataPackage.close();
			} catch (IOException e) {}
			mDataPackage = null;
		}
	}
}
