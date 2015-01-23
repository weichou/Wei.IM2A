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
import com.wei.c.im.test.msg.data.abs.AbsReceived;

/**
<pre>
{
	"type": "readed", //类型已读标记
	"sendid": "100000025", //发给对方的uid
	"no": [ //每条消息发送的客户端唯一标识，因为我们可以通过发送人和该标识来确定每条消息
		"1400925276",
		"1400869456",
		"1400777862",
		"1400834227"
	],
	"num":3	//剩下未取的
}
</pre>
 */
public class ArriveState extends AbsReceived<ArriveState> {
	public static final String TYPE_SERVER_HANDLED		= "serverhandled";
	public static final String TYPE_RECEIVED			= "received";
	public static final String TYPE_READ				= "readed";

	public long sendid;
	public long[] no;
	public int unloadnum;

	public boolean isServerHandled() {
		return type.equals(TYPE_SERVER_HANDLED);
	}

	public boolean isUserReceived() {
		return type.equals(TYPE_RECEIVED);
	}

	public boolean isUserRead() {
		return type.equals(TYPE_READ);
	}

	@Override
	public ArriveState fromJson(String json) {
		return fromJsonWithAllFields(json, ArriveState.class);
	}

	@Override
	protected String[] typeValues() {
		return new String[]{TYPE_SERVER_HANDLED, TYPE_RECEIVED, TYPE_READ};
	}

	@Override
	protected TypeToken<ArriveState> getTypeToken() {
		return null;
	}
}
