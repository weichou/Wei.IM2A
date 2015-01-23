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

import com.google.gson.annotations.Expose;
import com.wei.c.im.test.data.abs.AbsData;

public abstract class AbsReceived<T extends AbsReceived<T>> extends AbsData<T> {
	public static final String KEY_TYPE		= "type";

	@Expose	//TODO 父类中的该属性无法序列化，即使是public的，但是没有注解的，直接用new Gson().toJson(obj)可以序列化
	public String type;

	@Override
	protected String typeKey() {
		return KEY_TYPE;
	}
}
