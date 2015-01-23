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

package com.wei.c.im.test.msg.data.abs;

public class SendEntity<T> {

	public final long id;
	public final T data;
	/**是否只是一个返回状态**/
	public final boolean stateBack;

	public SendEntity(long id, T data, boolean stateBack) {
		this.id = id;
		this.data = data;
		this.stateBack = stateBack;
	}
}
