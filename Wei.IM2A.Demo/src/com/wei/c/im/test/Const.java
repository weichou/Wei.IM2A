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

package com.wei.c.im.test;

public class Const {
	public static class Url {
		//public static String loginByQQOpenId				= "http://x.xxx.com/1/account/tencent";
	}

	public static class Broadcast {
		public static String SAY_HI_CLICK_NOTIFY			= "com.wei.c.im.test.intent.action.SAY_HI_CLICK_NOTIFY";
	}

	public static class ThirdAccount {
		public static String sSharedPrefName				= "third.account";
		public static String sKey_qq_login_json				= "qq.login.json";
		public static String sKey_qq_user_info_json			= "qq.user.info.json";

		//QQ_key QQ第三方登录
		//public static String QQ_APP_ID_XXX				= "110xxxxxxx";

		public static String QQ_SCOPE						= "get_user_info, get_simple_userinfo";	//"get_user_info";	//"get_info(腾讯微博的), get_user_info, get_simple_userinfo";

		//public static String BAIDU_MAP_API_KEY			= "xxxxxxx";
	}
}
