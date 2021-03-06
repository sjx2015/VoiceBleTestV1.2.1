/*
 * Copyright (c) 2015, 张涛.
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
package com.actions.voicebletest.adapter;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actions.voicebletest.R;
import com.actions.voicebletest.bean.Message;
import com.actions.voicebletest.fragment.VoiceTestFragment;
import com.actions.voicebletest.utils.UrlUtils;
import com.actions.voicebletest.utils.Utils;

import org.kymjs.kjframe.KJBitmap;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kymjs (http://www.kymjs.com/) on 6/8/15.
 */
public class ChatAdapter extends BaseAdapter {
    public static final String TAG = ChatAdapter.class.getSimpleName();

    private final Context cxt;
    private List<Message> datas = null;
    private KJBitmap kjb;
    private AnimationDrawable voiceAnimation = null;
    private VoiceTestFragment.OnChatItemClickListener listener;

    public ChatAdapter(Context cxt, List<Message> datas, VoiceTestFragment.OnChatItemClickListener listener) {
        this.cxt = cxt;
        if (datas == null) {
            datas = new ArrayList<>(0);
        }
        this.datas = datas;
        kjb = new KJBitmap();
        this.listener = listener;
    }

    public void refresh(List<Message> datas) {
        if (datas == null) {
            datas = new ArrayList<>(0);
        }
        this.datas = datas;
        notifyDataSetChanged();
    }

    public void addMessage(Message msg){
        if (datas == null) {
            datas = new ArrayList<>(0);
        }
        datas.add(msg);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return datas.size();
    }

    @Override
    public Object getItem(int position) {
        return datas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return datas.get(position).getIsSend() ? 1 : 0;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {
        final ViewHolder holder;
        final Message data = datas.get(position);
        View rootView = null;
        if (v == null) {
            holder = new ViewHolder();
            if (data.getIsSend()) {
                LayoutInflater inflater = LayoutInflater.from(cxt);
                rootView = inflater.inflate(R.layout.chat_item_list_right, parent, false);
            } else {
                LayoutInflater inflater = LayoutInflater.from(cxt);
                rootView = inflater.inflate(R.layout.chat_item_list_left, parent, false);
            }
            holder.layout_content = (RelativeLayout) rootView.findViewById(R.id.chat_item_layout_content);
            holder.img_avatar = (ImageView) rootView.findViewById(R.id.chat_item_avatar);
            holder.img_chatimage = (ImageView) rootView.findViewById(R.id.chat_item_content_image);
            holder.img_sendfail = (ImageView) rootView.findViewById(R.id.chat_item_fail);
            holder.tv_length = (TextView)rootView.findViewById(R.id.tv_length);
            holder.progress = (ProgressBar) rootView.findViewById(R.id.chat_item_progress);
            holder.tv_chatcontent = (TextView) rootView.findViewById(R.id.chat_item_content_text);
            holder.tv_date = (TextView) rootView.findViewById(R.id.chat_item_date);
            holder.layout_image = rootView.findViewById(R.id.image_layout);
            holder.translate_btn = rootView.findViewById(R.id.translate_btn);
            rootView.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
            rootView = v;
        }

        //holder.tv_date.setText(StringUtils.getDataTime("yyyy-MM-dd " + "HH:mm:ss"));
        String date = Utils.timeStampToDate(data.getTime().getTime()).toString();
        holder.tv_date.setText(date);
        holder.tv_date.setVisibility(View.VISIBLE);

        //如果是文本类型，则隐藏图片，如果是图片则隐藏文本
        if (data.getType() == Message.MSG_TYPE_TEXT) {
            holder.img_chatimage.setVisibility(View.GONE);
            holder.tv_chatcontent.setVisibility(View.VISIBLE);
            holder.tv_length.setVisibility(View.GONE);
            holder.layout_image.setVisibility(View.GONE);
            holder.translate_btn.setVisibility(View.GONE);
            if (data.getContent().contains("href")) {
                holder.tv_chatcontent = UrlUtils.handleHtmlText(holder.tv_chatcontent, data
                        .getContent());
            } else {
                holder.tv_chatcontent = UrlUtils.handleText(holder.tv_chatcontent, data.getContent());
            }
        } else if (data.getType() == Message.MSG_TYPE_VOICE){
            holder.tv_chatcontent.setVisibility(View.GONE);
            holder.img_chatimage.setVisibility(View.VISIBLE);
            holder.layout_image.setVisibility(View.VISIBLE);
            holder.translate_btn.setVisibility(View.VISIBLE);
            //更改并显示录音条长度
            RelativeLayout.LayoutParams ps = (RelativeLayout.LayoutParams) holder.layout_image.getLayoutParams();
            ps.width = Utils.getVoiceLineWight2(cxt, data.getSeconds());
            holder.layout_image.setLayoutParams(ps); //更改语音长条长度
            holder.layout_image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onVoiceClick(position, holder.img_chatimage);
                }
            });

            if (data.isPlaying()){
                showAnimation(holder.img_chatimage);
            } else {
                holder.img_chatimage.setImageResource(R.drawable.ease_chatto_voice_playing);
            }

            holder.tv_length.setVisibility(View.VISIBLE);
            if (data.getSeconds() <= 0){
                holder.tv_length.setText(" < 1sec ");
            }
            else {
                holder.tv_length.setText(Utils.formatSecondsDuration(data.getSeconds()));
            }

            holder.translate_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onTranslateClick(position);
                }
            });
        }
        else{
            holder.tv_chatcontent.setVisibility(View.GONE);
            holder.img_chatimage.setVisibility(View.VISIBLE);
        }

        //如果是表情或图片，则不显示气泡，如果是图片则显示气泡
        if (data.getType() != Message.MSG_TYPE_TEXT && data.getType() != Message.MSG_TYPE_VOICE) {
            holder.layout_content.setBackgroundResource(android.R.color.transparent);
        }
        else {
            if (data.getIsSend()) {
                holder.layout_content.setBackgroundResource(R.drawable.chat_to_bg_selector);
            } else {
                holder.layout_content.setBackgroundResource(R.drawable.chat_from_bg_selector);
            }
        }

        //显示头像
        if (data.getIsSend()) {
            kjb.display(holder.img_avatar, data.getFromUserAvatar());
        } else {
            kjb.display(holder.img_avatar, data.getToUserAvatar());
        }

        if (listener != null) {
            holder.tv_chatcontent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onTextClick(position);
                }
            });
            holder.img_chatimage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (data.getType()) {
                        case Message.MSG_TYPE_PHOTO:
                            listener.onPhotoClick(position);
                            break;
                        case Message.MSG_TYPE_FACE:
                            listener.onFaceClick(position);
                            break;
                    }
                }
            });
        }

        //消息发送的状态
        switch (data.getState()) {
            case Message.MSG_STATE_FAIL:
                holder.progress.setVisibility(View.GONE);
                holder.img_sendfail.setVisibility(View.VISIBLE);
                break;
            case Message.MSG_STATE_SUCCESS:
                holder.progress.setVisibility(View.GONE);
                holder.img_sendfail.setVisibility(View.GONE);
                break;
            case Message.MSG_STATE_SENDING:
                holder.progress.setVisibility(View.VISIBLE);
                holder.img_sendfail.setVisibility(View.GONE);
                break;
        }
        return rootView;
    }

    public void stopPlayVoiceAnimation(ImageView imageView) {
        if (voiceAnimation != null && voiceAnimation.isRunning()) {
            voiceAnimation.stop();
            imageView.setImageResource(R.drawable.ease_chatto_voice_playing);
        }
    }

    public void showAnimation(ImageView imageView) {
        // play voice, and start animation
        imageView.setImageResource(R.drawable.voice_to_icon);
        voiceAnimation = (AnimationDrawable) imageView.getDrawable();
        voiceAnimation.start();
    }

    static class ViewHolder {
        TextView tv_date;
        ImageView img_avatar;
        TextView tv_chatcontent;
        ImageView img_chatimage;
        ImageView img_sendfail;
        TextView tv_length;
        ProgressBar progress;
        RelativeLayout layout_content;
        RelativeLayout layout_image;
        Button translate_btn;
    }
}
