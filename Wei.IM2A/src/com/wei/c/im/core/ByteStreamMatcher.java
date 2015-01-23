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

/**
 * 可匹配字节流中的某一段字节序列{@link #mSequences}，分为三种匹配模式：<br/>
 * 
 * 1、引导模式。该模式下{@link #mSequences}表示有效数据之前的引导序列，当完全匹配时即表示紧随其后的便是有效数据了，
 * 否则忽略所有流过的数据，见{@link #parsePreLoading(byte[], int, int)}，使用递归后移匹配；<br/>
 * 
 * 2、实体模式。该模式下{@link #mSequences}表示有效数据之后的结束序列，当匹配完成时，表示该序列的开头是有效数据的结束，
 * 见{@link #parseEntity(byte[], int, int, EntityHandler)}，使用递归后移匹配；<br/>
 * 
 * 3、心跳模式。该模式下{@link #mSequences}表示心跳数据序列，连续匹配但不递归，一旦匹配失败，则回传已匹配成功的部分序列
 * 并返回buffer缓冲中未能匹配的位置，见{@link #parseHeartBeat(byte[], int, int, EntityHandler)}。
 * 
 * @author Wei.Chou
 */
public class ByteStreamMatcher {
	public final byte[] mSequences;
	private int[] mSelfMatches;
	/**取值范围 >= 0**/
	private int mOffset = 0;

	public ByteStreamMatcher(byte[] sequences) {
		this.mSequences = sequences;
	}

	/**
	 * 将字节{@link b}与{@link #mSequences}的{@link #mOffset}位置字节进行匹配，若匹配成功，则将mOffset后移，
	 * 否则：<br/>
	 * 若参数{@link forwardMatch}为true，则将整个{@link #mSequences}后移并查找能够与前面已匹配部分再次匹配的长度，
	 * 并将{@link b}重新与该位置进行匹配，以此递归；<br/>
	 * 若参数{@link forwardMatch}为false，则直接将{@link #mOffset}置为0，并返回。
	 * 
	 * @param b 要匹配的字节
	 * @param forwardMatch 是否进行递归后移匹配
	 * @return true 表示成功匹配到结尾，否则为 false.
	 */
	public boolean match(byte b, boolean forwardMatch) {
		if (b == mSequences[mOffset]) {
			if (mOffset == mSequences.length - 1) {	//匹配完了，后面跟随的就是Data数据了
				mOffset = 0;
				return true;
			}
			mOffset++;
		} else {
			/*依次往后移动一位再次匹配，注意：如果不判断 > 0，递归会出现死循环。
			 为0时，递归本身就没有意义，分隔串的第一个字节都不匹配，应该直接返回让源向后走*/
			if (forwardMatch && mOffset > 0) {
				mOffset = getSelfMatchLength();
				if (match(b, true)) return true;
			} else {
				mOffset = 0;
			}
		}
		return false;
	}

	/**
	 * 把{@link #mSequences}作为有效数据之前的引导序列，当完全匹配时即表示紧随其后的便是有效数据了，否则忽略所有流过的数据。
	 * 
	 * @param buffer 缓冲数据的字节序列
	 * @param offset 缓冲数据的起始位置
	 * @param count 缓冲数据从起始位置开始的长度
	 * 
	 * @return 返回完全匹配了字节序列{@link #mSequences}之后，
	 * 位于该序列之后的offset，即有效数据的起始字节位置。
	 * -1表示还没有结束匹配，offset + count表示下一个buffer的开头。
	 **/
	public int parsePreLoading(byte[] buffer, int offset, int count) {
		if(!isValid(mSequences)) return offset;
		int end = offset + count;
		for (int i = offset; i < end; i++) {
			if(match(buffer[i], true)) {
				return i + 1;
			}
		}
		return -1;
	}

	/**
	 * 把{@link #mSequences}作为有效数据之后的结束序列，当匹配完成时，表示该序列的开头是有效数据的结束。
	 * 
	 * @param buffer 缓冲数据的字节序列
	 * @param offset 缓冲数据的起始位置
	 * @param count 缓冲数据从起始位置开始的长度
	 * @param entityHandler 反馈有效数据或结束状态的回调对象
	 * 
	 * @return true 表示数据区结束，false 未结束
	 */
	public boolean parseEntity(byte[] buffer, int offset, int count, EntityHandler entityHandler) {
		if (!isValid(mSequences)) throw new IllegalArgumentException("结束标识序列 endSequences 无效");
		int end = offset + count;
		int prevOffset, failStart = offset;
		for (int i = offset; i < end; i++) {
			prevOffset = mOffset;
			if (match(buffer[i], true)) {
				if(entityHandler != null) {
					entityHandler.end(buffer, i + 1, end - (i + 1));
				}
				return true;
			}
			int length = prevOffset - mOffset;
			//=0的情况是向后走了一位又匹配成功了，或者本来上一次就没有匹配上而这次有又没有匹配上
			if (length >= 0) {
				if (mOffset == 0) {	//没有匹配成功
					if (prevOffset > 0) failStart = i;
					if (entityHandler != null) {
						if (prevOffset > 0) entityHandler.fill(mSequences, 0, prevOffset);
						//使用了failStart，避免了总是写一个字节对性能的损耗
						//entityHandler.fill(buffer, i, 1);	//写入当前字节
						if (i == end - 1) entityHandler.fill(buffer, failStart, end - failStart);
					}
				} else if(mOffset > 0) {	//匹配失败但向后走又匹配成功了
					//+1是因为匹配成功后向后走了1，注意一种情况：只匹配成功了当前字节(mOffset == 1)，这里也是有效的
					if (entityHandler != null) entityHandler.fill(mSequences, 0, length + 1);
				} else {
					throw new IllegalStateException("算法有误，mOffset不应该小于0"); 
				}
			}else if(length == -1) {	//正常匹配成功该字节，那么继续向后走
				//但先要把没有匹配成功的这一串处理掉
				if (prevOffset == 0) {
					if(entityHandler != null) entityHandler.fill(buffer, failStart, i - failStart);
				}
			}else {	//匹配成功，最多只走一个字节,不可能更多
				throw new IllegalStateException("算法有误，一次只可能向后走一个字节，即 mSequences 只可能增加1");
			}
		}
		return false;
	}

	/**
	 * 把{@link #mSequences}作为心跳数据序列，连续匹配但不递归，一旦匹配失败，则回传已匹配成功的部分序列并返回buffer缓冲中未能匹配的位置。
	 * 
	 * @param buffer 缓冲数据的字节序列
	 * @param offset 缓冲数据的起始位置
	 * @param count 缓冲数据从起始位置开始的长度
	 * @param noHBeatHandler 在匹配失败时，反馈已匹配成功的部分序列的回调对象
	 * 
	 * @return buffer缓冲中未能匹配的位置。
	 * -1表示还没有结束匹配，必然小于offset + count
	 */
	public int parseHeartBeat(byte[] buffer, int offset, int count, NoHeartBeatHandler noHBeatHandler) {
		if(!isValid(mSequences)) return offset;
		int end = offset + count;
		for (int i = offset; i < end; i++) {
			int seqOffset = mOffset;
			if(match(buffer[i], false)) {	//不后移递归，否则移动之后前面的都丢了，若向后一直匹配不上则前面的也无法找回
				//continue;	//成功匹配到结尾，继续循环从头开始匹配
			}else {
				if(mOffset != seqOffset + 1) {	//匹配失败，若成功则继续
					if(noHBeatHandler != null) noHBeatHandler.noHeartBeat(mSequences, 0, seqOffset);
					return i;
				}
			}
		}
		return -1;
	}

	public int getOffset() {
		return mOffset;
	}

	public void reset() {
		mOffset = 0;
	}

	private int getSelfMatchLength() {
		if (mSelfMatches == null) mSelfMatches = computeSequencesSelfMatch();
		return mSelfMatches[mOffset];
	}

	/**
	 * 算出mSequences序列在相对于自身的某一部分（以自身开头为起点）进行后移后，还能够完全匹配的长度。
	 * 用于在对输入字节流进行匹配时，若只匹配了前面某一部分而后面部分（mOffset之后，包括mOffset）未能匹配，
	 * 则需要后移并找到能够完全匹配的长度（找到offset位置即可），然后接着这个位置（offset）再对输入流进行匹配。
	 * e.g:
	 * <pre>
	 * 完整的分隔序列:
	 * mSequences:      |abcdabcabcdefcdacef|
	 * 
	 * 自身的一部分:
	 * mOffset = 1      |a|
	 *                   |abcdabcabcdefcdacef|                --> 0        后移并匹配，能够匹配到结尾的长度
	 * mOffset = 6      |abcdab|
	 *                      |abcdabcabcdefcdacef|             --> 2        能匹配ab
	 * mOffset = 7      |abcdabc|
	 *                      |abcdabcabcdefcdacef|             --> 3        能匹配abc
	 * mOffset = 12     |abcdabcabcde|
	 *                              |abcdabcabcdefcdacef|     --> 0
	 * mOffset = 16     |abcdabcabcdefcda|
	 *                                 |abcdabcabcdefcdacef|  --> 1
	 *          ...
	 * </pre>
	 **/
	private int[] computeSequencesSelfMatch() {
		//有一个最大的下标是count，范围 <= sequences.length，虽然在本类中无意义，还是保留完整性覆盖整个范围
		int[] matchOffset = new int[mSequences.length + 1];
		int seqOffset = 0;
		int offset = 1;
		for (int count = 0; count <= mSequences.length; count++) {
			if (offset >= count) offset = count - 1;	//上一次向后移动过了，最多只能移动到上一次的末尾count位置
			if (offset < 1) offset = 1;
			for (int k = offset + seqOffset; k < count; k++) {
				if (mSequences[k] == mSequences[seqOffset]) {
					seqOffset++;
				} else {
					seqOffset = 0;
					offset++;
					k = offset;
					k--;
				}
			}
			matchOffset[count] = seqOffset;
		}
		return matchOffset;
	}

	public static boolean isValid(ByteStreamMatcher byteSepsMatcher) {
		return byteSepsMatcher != null && isValid(byteSepsMatcher.mSequences);
	}

	public static boolean isValid(byte[] sequences) {
		return sequences != null && sequences.length > 0;
	}

	public static interface EntityHandler {
		/**
		 * 引导序列之后，结束序列之前的有效数据。
		 * 
		 * @param buffer 缓冲数据的字节序列
		 * @param offset 缓冲数据的起始位置
		 * @param count 缓冲数据从起始位置开始的长度
		 */
		void fill(byte[] buffer, int offset, int count);
		/**
		 * 数据区结束，写入完成。参数表示剩下未处理的数据。
		 * 
		 * @param buffer 未处理数据的缓冲区
		 * @param afterEndSeqsOffset 结束序列之后的位置，即缓冲区中未处理数据的起始位置
		 * @param count 未处理数据从起始位置开始的长度
		 */
		void end(byte[] buffer, int afterEndSeqsOffset, int count);
	}

	public static interface NoHeartBeatHandler {
		/**
		 * 在后知后觉发现不是心跳包时回调，以反悔。由于此时，可能成功匹配的序列包括前一个缓冲，而它已经被丢弃，
		 * 但幸运的是，既然是成功匹配的部分，那么必然包含在{@link ByteStreamMatcher#mSequences mSequences}中，
		 * 因此取{@link ByteStreamMatcher#mSequences mSequences}中的这部分数据回写以作为开始序列或实体序列并再次处理吧。
		 * 
		 * @param buffer 成功匹配部分数据的缓冲
		 * @param offset 成功匹配部分数据在缓冲中的起始位置
		 * @param count 成功匹配部分数据在缓冲中从起始位置开始的长度
		 */
		void noHeartBeat(byte[] buffer, int offset, int count);
	}
}
