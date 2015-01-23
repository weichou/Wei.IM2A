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
    "type": "sayhi",           //类型 sayhi
    "fromuid": 1000000024,     //来自谁
    "uname": "用户名",
    "facepath": "头像路径",
	"time": 8213824432	//注意时间是10位数，精确到秒
}
</pre>
 */
public class SayHi extends AbsReceived<SayHi> {
	public static final String TYPE_SAYHI	= "sayhi";

	public long fromuid;
	public String uname;
	public String facepath;
	public long time;

	@Override
	public SayHi fromJson(String json) {
		return fromJsonWithAllFields(json, SayHi.class);
	}

	@Override
	protected String[] typeValues() {
		return new String[]{TYPE_SAYHI};
	}

	@Override
	protected TypeToken<SayHi> getTypeToken() {
		return null;
	}
}
