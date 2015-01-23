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

import org.json.JSONObject;

import com.google.gson.reflect.TypeToken;
import com.wei.c.im.test.msg.data.abs.AbsReceived;

/**
<pre>
{
	//"type": "messages_online", //消息的类型在线消息
	"ctype": "1", //信息的类型这里说明是文字
	"cvalue": "吃了咩？", //信息的值
	//"sendid": "1000000025", //发给谁也就是文件的接收者
	//"sendtype": "user", //发送类型目前只有用户这一项
	"sendtime": 1403862958, //消息的发送时间
	"fromuid": 1000000024 //消息来自哪个用户
	“no”:546154315456
}
</pre>

 * @author Wei.Chou
 */
public class MsgReceived extends AbsReceived<MsgReceived> {

	public long fromuid;
	public int ctype;
	public String cvalue;
	public String sendtype = "user";
	public long sendtime;
	public long no;
	public long _order_num;

	@Override
	public boolean isBelongToMe(JSONObject json) {
		return !json.has(KEY_TYPE);		//根本就没有type。有且仅有本类型消息才没有type
	}

	@Override
	public MsgReceived fromJson(String json) {
		return fromJsonWithAllFields(json, MsgReceived.class);
	}

	@Override
	protected String[] typeValues() {
		return new String[]{};
	}

	@Override
	protected TypeToken<MsgReceived> getTypeToken() {
		return null;
	}
}
