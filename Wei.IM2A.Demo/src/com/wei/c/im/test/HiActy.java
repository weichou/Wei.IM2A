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

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.wei.c.L;
import com.wei.c.anno.ViewId;
import com.wei.c.anno.ViewLayoutId;
import com.wei.c.framework.AbsActivity;
import com.wei.c.im.test.data.MsgBean;
import com.wei.c.im.test.exception.TokenNotValidException;
import com.wei.c.im.test.helper.ArriveStateHelper;
import com.wei.c.im.test.msg.client.MsgClient;

@ViewLayoutId(R.layout.m_hi)
public class HiActy extends AbsActivity {

	public static void startMe(Context context) {
		startMe(context, new Intent(context, HiActy.class));
	}

	@ViewId(R.id.m_hi_text_msgs)
	private TextView mTextMsgs;
	@ViewId(R.id.m_hi_text_edit)
	private EditText mTextEdit;
	@ViewId(R.id.m_hi_btn_send)
	private Button mBtn;

	private long mID, uID;
	private MsgClient mMsgClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mMsgClient = new MsgClient(this);
		try {
			mID = UID.getMyID(this);
			uID = 1234567890;
			mMsgClient.setIDs(mID, uID);
		} catch (TokenNotValidException e) {
			L.d(this, e);
			finish();
		}

		mMsgClient.registerOnChatObserver(new MsgClient.OnChatObserver() {

			@Override
			public void onMsgChange(List<MsgBean> list) {
				StringBuilder sb = new StringBuilder();
				for (MsgBean msg : list) {
					switch (msg.type) {
					case MsgBean.TYPE_ME:
						sb.append("\n");
						sb.append("ME: ");
						sb.append(msg.content);
						break;
					case MsgBean.TYPE_ARRIVE_STATE:
						sb.append("\n");
						sb.append(ArriveStateHelper.getArriveState(msg.arriveState));
						break;
					case MsgBean.TYPE_OTHER:
						sb.append("\n\n");
						sb.append("ta: ");
						sb.append(msg.content);
						break;
					case MsgBean.TYPE_TIME:
						sb.append("\n\n");
						sb.append("---------");
						sb.append(msg.timeSendSerialized);
						sb.append("---------");
						break;
					}
				}
				mTextMsgs.setText(sb.toString());
			}
		});

		mBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mMsgClient.sendMsg(1, mTextEdit.getText().toString())) {
					mTextEdit.setText(null);
				} else {
					Toast.makeText(HiActy.this, "消息发送失败", Toast.LENGTH_LONG);
				}
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		mMsgClient.onActivityStart();
	};

	@Override
	public void onStop() {
		mMsgClient.onActivityStop();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		mMsgClient.onActivityDestroy();
		super.onDestroy();
	}

	@Override
	protected int[] getClickHideInputMethodViewIds() {
		return new int[] {R.id.m_hi_text_msgs};
	}
}
