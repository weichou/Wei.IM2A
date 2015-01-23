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

import java.io.InputStream;

import com.wei.c.L;

public class Receiver extends AbsTransfer {
	public static final int MODE_HEART_BEAT         = 0;
	public static final int MODE_PRE_LOADING        = 1;
	public static final int MODE_ENTITY             = 2;

	private final byte[] BUFFER;
	private final ByteStreamMatcher mPreLoadingSeqs, mEndingSeqs, mHeartBeatSeqs;
	private EntityParser mEntityParser;

	private InputStream mIn;
	private boolean mEntityHasBegan = false;

	private int mStreamMode;
	private int mAfterEndSeqsOffset;

	public Receiver(InputStream in, byte[] preLoadingSequences, byte[] endingSequences, byte[] heartBeatSequences) {
		this(in, preLoadingSequences, endingSequences, heartBeatSequences, 4096);
	}

	public Receiver(InputStream in, byte[] preLoadingSequences, byte[] endingSequences, byte[] heartBeatSequences, int bufferSize) {
		mIn = in;
		mPreLoadingSeqs = ByteStreamMatcher.isValid(preLoadingSequences) ? new ByteStreamMatcher(preLoadingSequences) : null;
		if(!ByteStreamMatcher.isValid(endingSequences)) throw new IllegalArgumentException("endSequences 不能为空");
		mEndingSeqs = new ByteStreamMatcher(endingSequences);
		mHeartBeatSeqs = ByteStreamMatcher.isValid(heartBeatSequences) ? new ByteStreamMatcher(heartBeatSequences) : null;
		if(bufferSize < 1024) bufferSize = 1024;
		BUFFER = new byte[bufferSize];
	}

	public synchronized void setInputStream(InputStream in) {
		mIn = in;
	}

	@Override
	public void doWork() throws Exception {
		while (isAlive()) {
			int count = mIn.read(BUFFER);
			if(count > 0) {
				handleStream(BUFFER, 0, count);
			}else if(count == 0) {
				L.w(this, "输入流读取空数据: count = 0");
			}else if(count == -1) {	//正常断开连接的方式就是读取长度未-1，异常断开才会抛异常
				reportServerKicked();
				L.w(this, "输入流读取到结尾，连接正常断开: count = -1");
				break;
			}
		}
	}

	protected void handleStream(byte[] buffer, int offset, int count) {
		int start, end;
		switch (mStreamMode) {
		case MODE_HEART_BEAT:
			if (ByteStreamMatcher.isValid(mHeartBeatSeqs)) {
				start = mHeartBeatSeqs.parseHeartBeat(buffer, offset, count, mNoHeartBeatHandler);
				end = offset + count;
				if (start == -1) {
					return;		//分析完了，但没有结束心跳序列，继续等待下一次数据来临
				} else if (start >= end) {	//断言，未能匹配的位置只能小于end
					throw new IllegalStateException("状态不正确：MODE_HEART_BEAT 未能匹配的位置到达了 end");
				} else {	//未能匹配
					//注意在走到这里前，mOnHeartBeatListener已接收了回调数据并改变了状态，所以这里不需要改变状态
					handleStream(buffer, start, end - start);
				}
				return;
			} else {
				if (ByteStreamMatcher.isValid(mPreLoadingSeqs)) {
					mStreamMode = MODE_PRE_LOADING;
					mPreLoadingSeqs.reset();
				} else {
					mStreamMode = MODE_ENTITY;
					mEndingSeqs.reset();
				}
				handleStream(buffer, offset, count);
			}
			return;
		case MODE_PRE_LOADING:
			if (ByteStreamMatcher.isValid(mPreLoadingSeqs)) {
				start = mPreLoadingSeqs.parsePreLoading(buffer, offset, count);
				end = offset + count;
				if (start == -1) {
					return;		//分析完了，但没有结束开始序列，继续等待下一次数据来临
				} else if (start > end) {	//断言，逻辑上不会出现的
					throw new IllegalStateException("状态不正确：MODE_START dataStart > offset + count");
				} else {
					mStreamMode = MODE_ENTITY;
					mEndingSeqs.reset();
					if(start == end) {	//下一个buffer的开头就是数据
						return;
					}
					handleStream(buffer, start, end - start);
				}
			} else {
				mStreamMode = MODE_ENTITY;
				mEndingSeqs.reset();
				handleStream(buffer, offset, count);
			}
			return;
		case MODE_ENTITY:
			if (mEndingSeqs.parseEntity(buffer, offset, count, mEntityHandler)) {
				if (ByteStreamMatcher.isValid(mHeartBeatSeqs)) {
					mStreamMode = MODE_HEART_BEAT;
					mHeartBeatSeqs.reset();
				} else if (ByteStreamMatcher.isValid(mPreLoadingSeqs)) {
					mStreamMode = MODE_PRE_LOADING;
					mPreLoadingSeqs.reset();
				} else {	//还是自己
					mStreamMode = MODE_ENTITY;
					mEndingSeqs.reset();
				}
				end = offset + count;
				if (mAfterEndSeqsOffset > end) {
					throw new IllegalStateException("状态不正确：MODE_DATA mAfterEndSeqsOffset > offset + count");
				} else if (mAfterEndSeqsOffset == end) {
					return;
				}
				handleStream(buffer, mAfterEndSeqsOffset, end - mAfterEndSeqsOffset);
				return;
			} else {	//数据区未结束，继续等待下一次数据来临
				return;
			}
		}
	}

	private ByteStreamMatcher.NoHeartBeatHandler mNoHeartBeatHandler = new ByteStreamMatcher.NoHeartBeatHandler() {

		@Override
		public void noHeartBeat(byte[] buffer, int offset, int count) {
			if (ByteStreamMatcher.isValid(mPreLoadingSeqs)) {
				mStreamMode = MODE_PRE_LOADING;
				mPreLoadingSeqs.reset();
			} else {
				mStreamMode = MODE_ENTITY;
				mEndingSeqs.reset();
			}
			handleStream(buffer, offset, count);
		}
	};

	private ByteStreamMatcher.EntityHandler mEntityHandler = new ByteStreamMatcher.EntityHandler() {

		@Override
		public void fill(byte[] buffer, int offset, int count) {
			entityFill(buffer, offset, count);
		}

		@Override
		public void end(byte[] buffer, int afterEndSeqsOffset, int count) {
			mAfterEndSeqsOffset = afterEndSeqsOffset;
			entityFinish();
		};
	};

	@Override
	protected void onStartWork() {
		mEntityHasBegan = false;
		mStreamMode = MODE_HEART_BEAT;
		if (mEntityParser != null) mEntityParser.onStartWork();
	}

	@Override
	public boolean canRestart() {
		return true;
	}

	@Override
	protected void onError() {
		mEntityHasBegan = false;
		if (mEntityParser != null) mEntityParser.onAbort();
	}

	@Override
	protected void onStopWork() {
		mEntityHasBegan = false;
		if (mEntityParser != null) mEntityParser.onStopWork();
	}

	private void entityBegin() {
		onEntityBegin();
		mEntityHasBegan = true;
	}

	private void entityFill(byte[] buffer, int offset, int count) {
		if (count > 0) {
			if(!mEntityHasBegan) {
				entityBegin();
			}
			onEntityFill(buffer, offset, count);
		}
	}

	private void entityFinish() {
		onEntityFinish();
		mEntityHasBegan = false;
	}

	private void onEntityBegin() {
		if (mEntityParser != null) mEntityParser.onBegin();
	}

	private void onEntityFill(byte[] buffer, int offset, int count) {
		if (mEntityParser != null) mEntityParser.onFill(buffer, offset, count);
	}

	private void onEntityFinish() {
		if (mEntityParser != null) mEntityParser.onFinish();
	}

	public void setEntityParser(EntityParser parser) {
		mEntityParser = parser;
	}

	public static interface EntityParser {
		void onStartWork();
		void onBegin();
		void onFill(byte[] buffer, int offset, int count);
		void onFinish();
		void onAbort();
		void onStopWork();
	}
}