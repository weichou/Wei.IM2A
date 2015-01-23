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

import java.io.UnsupportedEncodingException;

import org.json.JSONObject;

import com.wei.c.data.abs.AbsJson;

public abstract class AbsSendable extends AbsJson<AbsSendable> implements ISendable {
	@Override
	public byte[] serialize(String charset) throws UnsupportedEncodingException {
		return toJson().getBytes(charset);
	}

	@Override
	public String toJson() {
		return toJsonWithAllFields(this);
	}

	@Override
	public AbsSendable fromJson(String json) {
		throw new RuntimeException("Sendable 不支持反序列化");
	}

	@Override
	public boolean isBelongToMe(JSONObject json) {
		throw new RuntimeException("Sendable 不支持反序列化");
	}
}
