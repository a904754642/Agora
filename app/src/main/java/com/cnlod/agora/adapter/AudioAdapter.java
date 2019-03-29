package com.cnlod.agora.adapter;

import android.content.Context;
import android.support.annotation.Nullable;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.cnlod.agora.Constant;
import com.cnlod.agora.R;

import java.util.List;

public class AudioAdapter extends BaseQuickAdapter<String, BaseViewHolder> {
    private Context context;

    public AudioAdapter(Context context, @Nullable List<String> data) {
        super(R.layout.item_grid_audio, data);
        this.context = context;
    }

    @Override
    protected void convert(BaseViewHolder helper, final String item) {
        ImageView iv = helper.getView(R.id.iv_audio);
        TextView tv = helper.getView(R.id.tv_audio);

        if (item.equals(Constant.userId1)) {
            iv.setImageResource(R.drawable.patient);
            tv.setText("患者");
        } else if (item.equals(Constant.userId2)) {
            iv.setImageResource(R.drawable.kf);
            tv.setText("客服");
        } else if (item.equals(Constant.userId3)) {
            iv.setImageResource(R.drawable.doctor);
            tv.setText("医生");
        }
    }
}
