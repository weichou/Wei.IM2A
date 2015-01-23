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
    "type": "sayhi",    //类型 sayhi
    "touid": "1000000025"       //发给谁
}
</pre>
 */
public class SayHi extends AbsSendable {
	@Expose
	public final String type = "sayhi";
	@Expose
	public final long touid;

	public SayHi(long touid) {
		this.touid = touid;
	}
}
