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

package com.wei.c.im.test.msg.data.send;

import com.google.gson.annotations.Expose;
import com.wei.c.im.test.msg.data.abs.AbsSendable;

/**
<pre>
{
	"type": "recevied",
	"sendid": "1000000367",
	"no": [
			546154315456,
			546154315456
		]
}
</pre>
 */
public class StateBack extends AbsSendable {
	public static final String TYPE_RECEIVED			= "received";
	public static final String TYPE_READ				= "readed";

	@Expose
	public final String type;
	@Expose
	public final long sendid;
	@Expose
	public final long[] no;

	public StateBack(String type, long sendid, long... no) {
		this.type = type;
		this.sendid = sendid;
		this.no = no;
	}
}
