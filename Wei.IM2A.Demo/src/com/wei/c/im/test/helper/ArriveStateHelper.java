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

package com.wei.c.im.test.helper;

import android.widget.TextView;

import com.wei.c.im.test.R;

public class ArriveStateHelper {

	public static String getArriveState(int arriveState) {
		switch (arriveState) {
		case 0:
			return "...";
		case 1:
			return "未发送";
		case 2:
			return "已发送";
		case 3:
			return "已发送";
		case 4:
			return "送达";
		case 5:
			return "已读";
		default:
			return null;
		}
	}

	public static void updateArriveState(TextView textView, int arriveState) {
		switch (arriveState) {
		case 0:
			textView.setBackgroundResource(R.drawable.bg_i_m_message_list_item_arrive_state_sending_s);
			textView.setText("...");
			break;
		case 1:
			textView.setBackgroundResource(R.drawable.bg_i_m_message_list_item_arrive_state_arrived_s);
			textView.setText("未发送");
			break;
		case 2:
			textView.setBackgroundResource(R.drawable.bg_i_m_message_list_item_arrive_state_arrived_s);
			textView.setText("已发送");
			break;
		case 3:
			textView.setBackgroundResource(R.drawable.bg_i_m_message_list_item_arrive_state_arrived_s);
			textView.setText("已发送");
			break;
		case 4:
			textView.setBackgroundResource(R.drawable.bg_i_m_message_list_item_arrive_state_arrived_s);
			textView.setText("送达");
			break;
		case 5:
			textView.setBackgroundResource(R.drawable.bg_i_m_message_list_item_arrive_state_read_s);
			textView.setText("已读");
			break;
		}
	}
}
