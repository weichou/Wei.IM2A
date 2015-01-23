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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;

import com.wei.c.L;
import com.wei.c.im.core.BaseSenderAdapter;
import com.wei.c.im.test.UID;
import com.wei.c.im.test.exception.TokenNotValidException;
import com.wei.c.im.test.msg.data.ContentHelper;
import com.wei.c.im.test.msg.data.MSG;
import com.wei.c.im.test.msg.data.abs.SendEntity;
import com.wei.c.im.test.msg.data.send.Token;

public class StringMsgAdapter extends BaseSenderAdapter {
	private MsgService mContext;
	private ContentResolver mContentResolver;
	private ContentObserver mContentObserver;
	private String mCharset;
	private Cursor mCursor;
	private SendEntity<String> mCurrentSendEntity;
	private ByteArrayInputStream mCurrentMsgBytes;
	private volatile boolean mDataSourceChanged = true;
	private long mID;

	public StringMsgAdapter(MsgService service) {
		this(service, "utf-8");
	}

	public StringMsgAdapter(MsgService service, String charset) {
		if (charset == null || charset.length() == 0) throw new IllegalArgumentException("参数charset不能为空");
		mContext = service;
		mCharset = charset;
	}

	/**只要连接没断开，则收到的就是上一个ID的消息，那么不改变mMsgParser的ID即可，
	 * 而mMsgAdapter的ID没有改变，发送的也是该ID的消息，因此不会在切换用户时造成混乱。
	 */
	public synchronized void setUid(long uid) {
		mID = uid;
		L.i(this, "setUid-----------mID:" + mID);
	}

	@Override
	public void onStartWork() {
		mID = -1;
		mContentObserver = new ContentObserver(null) {
			@Override
			public void onChange(boolean selfChange) {
				notifySendNewEntity();
			}
		};
		mContentResolver = mContext.getContentResolver();
		mContentResolver.registerContentObserver(MSG.URI_SEND_ENTITY, true, mContentObserver);
		mDataSourceChanged = true;
	}

	@Override
	public byte[] getAuthorizeSequences() {
		try {
			String json = new Token(UID.nextToken(mContext)).toJson();
			L.i(this, "getAuthorizeSequences-----------:" + json);
			
			byte[] bytes = new Token(UID.nextToken(mContext)).serialize(mCharset);
			return bytes;
		} catch (UnsupportedEncodingException e) {
			L.e(this, e);
			throw new RuntimeException(e);
		} catch (TokenNotValidException e) {
			mContext.stopForAuthorityFailed();
			L.e(this, e);
		}
		return null;
	}

	@Override
	public boolean onNewEntity() {
		if (!UID.isValid(mID)) throw new RuntimeException("mID无效:" + mID);
		/*上一个消息处理完毕，回写了相关数据之后再重新加载Cursor，以免将正在发送的消息的数据库记录加入了新的Cursor
		 * 不写在onSend()里面是因为此时有可能处于休眠状态
		 */
		if (mDataSourceChanged) {
			L.i(this, "onNewEntity-----------reloadCursor--------------");
			recycleCursor();
			loadCursor();
			mDataSourceChanged = false;
		}
		mCurrentSendEntity = getNextEntity();
		
		if (mCurrentSendEntity == null || mCurrentSendEntity.data == null) {
			L.i(this, "onNewEntity-----------mCurrentSendEntity:" + mCurrentSendEntity);
			recycleCursor();
			return false;
		} else {
			L.i(this, "onNewEntity-----------mCurrentSendEntity:" + mCurrentSendEntity.data);
		}
		try {
			mCurrentMsgBytes = new ByteArrayInputStream(mCurrentSendEntity.data.getBytes(mCharset));
			return true;
		} catch (UnsupportedEncodingException e) {
			L.e(this, e);
			mCurrentMsgBytes = null;
			return false;
		}
	}

	@Override
	public int fillEntity(byte[] buffer) {
		try {
			return mCurrentMsgBytes.read(buffer);
		} catch (IOException e) {
			L.e(this, e);
			return -1;
		}
	}

	@Override
	public void notifySendNewEntity() {
		mDataSourceChanged = true;
		super.notifySendNewEntity();
	}

	private SendEntity<String> getNextEntity() {
		return mCursor == null ? null : ContentHelper.SEND_ENTITY.getSendEntity(mContentResolver, mCursor);
	}

	@Override
	public void onSend() {
		ContentHelper.SEND_ENTITY.updateEntitySended(mContentResolver, mCurrentSendEntity);
		L.i(this, "onSend-----------mCurrentSendEntity:" + mCurrentSendEntity.data);
		mCurrentSendEntity = null;
		mCurrentMsgBytes = null;
	}

	@Override
	public void onStopWork() {
		mContentResolver.unregisterContentObserver(mContentObserver);
		recycleCursor();
	}

	private void loadCursor() {
		mCursor = ContentHelper.SEND_ENTITY.getCursor(mContentResolver, mID);
		if(mCursor == null) throw new RuntimeException("获取Cursor失败");
	}

	private void recycleCursor() {
		if (mCursor != null) {
			mCursor.close();
			mCursor = null;
		}
	}
}
