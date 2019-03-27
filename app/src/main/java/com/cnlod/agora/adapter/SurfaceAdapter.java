package com.cnlod.agora.adapter;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.cnlod.agora.AGApplication;
import com.cnlod.agora.R;
import com.cnlod.agora.util.DisplayUtil;

import java.util.List;

import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;

public class SurfaceAdapter extends BaseQuickAdapter<Integer, BaseViewHolder> {
    private Context context;

    public SurfaceAdapter(Context context, @Nullable List<Integer> data) {
        super(R.layout.item_grid, data);
        this.context = context;
    }

    @Override
    protected void convert(BaseViewHolder helper, final Integer item) {
        FrameLayout frameLayout = helper.getView(R.id.frag);

        int surfaceH = (DisplayUtil.getWindowWidth() - DisplayUtil.dip2px(20)) / 3;
        frameLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, surfaceH));

        SurfaceView surfaceViewSmall = RtcEngine.CreateRendererView(context);
        frameLayout.addView(surfaceViewSmall);
        surfaceViewSmall.setZOrderMediaOverlay(true);//覆盖在另一个surfaceView上面

        AGApplication.the().getmRtcEngine().setupRemoteVideo(new VideoCanvas(surfaceViewSmall, VideoCanvas.RENDER_MODE_HIDDEN, item));
    }
}
