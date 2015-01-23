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

package com.wei.c.im.test.msg.data.receive;

import com.google.gson.reflect.TypeToken;
import com.wei.c.im.test.UID;
import com.wei.c.im.test.msg.data.abs.AbsReceived;

/**
 * 鉴权成功：{"uid":"1000000024","type":"login"}
 * 鉴权失败：{"uid":"-1","type":"login"}
 * 被挤下线：{"uid":"1000000024","type":"another client login"}
 * 
 * @author Wei.Chou
 */
public class Authority extends AbsReceived<Authority> {
	public static final String TYPE_LOGIN				= "login";
	public static final String TYPE_INVALID			= "invalid user";
	public static final String TYPE_ANOTHER_LOGIN		= "another client login";

	public long uid;

	public boolean isSuccess() {
		return type != null && type.equals(TYPE_LOGIN) && UID.isValid(uid);
	}

	public boolean isInvalid() {
		return type != null && type.equals(TYPE_INVALID);
	}

	public boolean isAnotherClientLogin() {
		return type != null && type.equals(TYPE_ANOTHER_LOGIN);
	}

	@Override
	public Authority fromJson(String json) {
		return fromJsonWithAllFields(json, Authority.class);
	}

	@Override
	protected String[] typeValues() {
		return new String[]{TYPE_LOGIN, TYPE_INVALID, TYPE_ANOTHER_LOGIN};
	}

	@Override
	protected TypeToken<Authority> getTypeToken() {
		return null;
	}
}
