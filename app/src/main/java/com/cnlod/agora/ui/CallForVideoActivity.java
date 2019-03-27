package com.cnlod.agora.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cnlod.agora.AGApplication;
import com.cnlod.agora.Constant;
import com.cnlod.agora.R;
import com.cnlod.agora.util.Ls;

import java.util.Locale;

import io.agora.AgoraAPI;
import io.agora.AgoraAPIOnlySignal;
import io.agora.IAgoraAPI;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

/**
 * Created by beryl on 2017/11/6.
 */

public class CallForVideoActivity extends AppCompatActivity implements AGApplication.OnAgoraEngineInterface {

    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_ID_CAMERA = PERMISSION_REQ_ID_RECORD_AUDIO + 1;
    private static final int PERMISSION_REQ_ID_STORAGE = PERMISSION_REQ_ID_CAMERA + 1;

    private AgoraAPIOnlySignal mAgoraAPI;
    private RtcEngine mRtcEngine;

    private String mSubscriber;

    private CheckBox mCheckMute;
    private TextView mCallTitle;
    private ImageView mCallOutHangupBtn;
    private RelativeLayout mLayoutCallIn;

    private FrameLayout mLayoutBigView;
    private FrameLayout mLayoutSmallView;

    private String channelName = "channelid";
    private int callType = -1;
    private boolean mIsCallInRefuse = false;
    private int mRemoteUid = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_video);
        Ls.e("视频！！！！");

        InitUI();

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)
                && checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA)
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_STORAGE)) {
            initAgoraEngineAndJoinChannel();
        }
    }

    private void InitUI() {
        mCallTitle = findViewById(R.id.meet_title);

        mCheckMute = findViewById(R.id.call_mute_button);
        mCheckMute.setOnCheckedChangeListener(oncheckChangeListerener);

        mCallOutHangupBtn = findViewById(R.id.call_out_hangup);
        mLayoutCallIn = findViewById(R.id.call_layout_callin);

        mLayoutBigView = findViewById(R.id.big_video_view_container);
        mLayoutSmallView = findViewById(R.id.small_video_view_container);
    }

    private void setupData() {
        Intent intent = getIntent();

        mSubscriber = intent.getStringExtra("subscriber");//对方
        channelName = intent.getStringExtra("channelName");
        callType = intent.getIntExtra("type", -1);
        if (callType == Constant.CALL_IN) {//收到视频邀请
            mIsCallInRefuse = true;//todo ????
            mLayoutCallIn.setVisibility(View.VISIBLE);
            mCallOutHangupBtn.setVisibility(View.GONE);
            mCallTitle.setText(String.format(Locale.US, "%s is calling...", mSubscriber));

            setupLocalVideo(); // Tutorial Step 3
        } else if (callType == Constant.CALL_OUT) {//发送视频邀请
            mLayoutCallIn.setVisibility(View.GONE);
            mCallOutHangupBtn.setVisibility(View.VISIBLE);
            mCallTitle.setText(String.format(Locale.US, "%s is be called...", mSubscriber));

            setupLocalVideo(); // Tutorial Step 3
            joinChannel(); // Tutorial Step 4
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Ls.w( "onNewIntent");
        setupData();
    }

    @Override
    public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) { // Tutorial Step 5
        Ls.w( "onFirstRemoteVideoDecoded  uid:" + uid);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRemoteUid != 0) {
                    return;
                }
                mRemoteUid = uid;
                setupRemoteVideo(uid);
            }
        });
    }

    @Override
    public void onUserOffline(final int uid, int reason) { // Tutorial Step 7
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onRemoteUserLeft(uid);
            }
        });
    }

    @Override
    public void onUserMuteVideo(final int uid, final boolean muted) { // Tutorial Step 10
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onRemoteUserVideoMuted(uid, muted);
            }
        });
    }

    private CompoundButton.OnCheckedChangeListener oncheckChangeListerener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mRtcEngine.muteLocalAudioStream(isChecked);
        }
    };

    public void CallClickInit(View v) {
        switch (v.getId()) {

            case R.id.call_in_hangup:
                callInRefuse();
                break;

            case R.id.call_in_pickup:
                mIsCallInRefuse = false;
                joinChannel(); // Tutorial Step 4
                mAgoraAPI.channelInviteAccept(channelName, mSubscriber, 0, null);
                mLayoutCallIn.setVisibility(View.GONE);
                mCallOutHangupBtn.setVisibility(View.VISIBLE);
                mCallTitle.setVisibility(View.GONE);
                break;

            case R.id.call_out_hangup: // call out canceled or call ended

                callOutHangup();
                break;
        }
    }

    private void callOutHangup() {
        if (mAgoraAPI != null)
            mAgoraAPI.channelInviteEnd(channelName, mSubscriber, 0);
    }

    private void callInRefuse() {
        // "status": 0 // Default
        // "status": 1 // Busy
        if (mAgoraAPI != null)
            mAgoraAPI.channelInviteRefuse(channelName, mSubscriber, 0, "{\"status\":0}");

        onEncCallClicked();
    }

    @Override
    public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
        Ls.w( "onJoinChannelSuccess channel: " + channel + " uid: " + uid);
    }

    private void addSignalingCallback() {
        if (mAgoraAPI == null) {
            return;
        }

        mAgoraAPI.callbackSet(new AgoraAPI.CallBack() {

            @Override
            public void onLogout(final int i) {
                Ls.w( "onLogout  i = " + i);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (i == IAgoraAPI.ECODE_LOGOUT_E_KICKED) { // other login the account
                            Toast.makeText(CallForVideoActivity.this, "Other login account ,you are logout.", Toast.LENGTH_SHORT).show();

                        } else if (i == IAgoraAPI.ECODE_LOGOUT_E_NET) { // net
                            Toast.makeText(CallForVideoActivity.this, "Logout for Network can not be.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        Intent intent = new Intent();
                        intent.putExtra("result", "finish");
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                });

            }

            /**
             * call in receiver
             */
            @Override
            public void onInviteReceived(final String channelID, final String account, final int uid, String s2) {
                Ls.w( "onInviteReceived  channelID = " + channelID + "  account = " + account);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                      "status": 0 // Default
//                      "status": 1 // Busy
                        mAgoraAPI.channelInviteRefuse(channelID, account, uid, "{\"status\":1}");

                    }
                });
            }

            /**
             * call out other ,local receiver
             */
            @Override
            public void onInviteReceivedByPeer(final String channelID, String account, int uid) {
                Ls.w( "onInviteReceivedByPeer  channelID = " + channelID + "  account = " + account);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCallOutHangupBtn.setVisibility(View.VISIBLE);

                        mCallTitle.setText(String.format(Locale.US, "%s is being called ...", mSubscriber));
                    }
                });
            }

            /**
             * other receiver call accept callback
             * @param channelID
             * @param account
             * @param uid
             * @param s2
             */
            @Override
            public void onInviteAcceptedByPeer(String channelID, String account, int uid, String s2) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCallTitle.setVisibility(View.GONE);
                    }
                });

            }

            /**
             * other receiver call refuse callback
             * @param channelID
             * @param account
             * @param uid
             * @param s2
             */

            @Override
            public void onInviteRefusedByPeer(String channelID, final String account, int uid, final String s2) {
                Ls.w( "onInviteRefusedByPeer channelID = " + channelID + " account = " + account + " s2 = " + s2);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (s2.contains("status") && s2.contains("1")) {
                            Toast.makeText(CallForVideoActivity.this, account + " reject your call for busy", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(CallForVideoActivity.this, account + " reject your call", Toast.LENGTH_SHORT).show();
                        }

                        onEncCallClicked();
                    }
                });
            }


            /**
             * end call remote receiver callback
             * @param channelID
             * @param account
             * @param uid
             * @param s2
             */
            @Override
            public void onInviteEndByPeer(final String channelID, String account, int uid, String s2) {
                Ls.w( "onInviteEndByPeer channelID = " + channelID + " account = " + account);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (channelID.equals(channelName)) {
                            onEncCallClicked();
                        }

                    }
                });
            }

            /**
             * end call local receiver callback
             * @param channelID
             * @param account
             * @param uid
             */
            @Override
            public void onInviteEndByMyself(String channelID, String account, int uid) {
                Ls.w( "onInviteEndByMyself channelID = " + channelID + "  account = " + account);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onEncCallClicked();
                    }
                });
            }
        });
    }

    public final void showLongToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        addSignalingCallback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Ls.w( "onDestroy");

        if (mRtcEngine != null) {
            mRtcEngine.stopPreview();
            mRtcEngine.leaveChannel();
        }
        mRtcEngine = null;

    }

    //返回键
    @Override
    public void onBackPressed() {
        Ls.w( "onBackPressed callType: " + callType + " mIsCallInRefuse: " + mIsCallInRefuse);
        if (callType == Constant.CALL_IN && mIsCallInRefuse) {
            callInRefuse();
        } else {
            callOutHangup();
        }
        super.onBackPressed();
    }

    // Tutorial Step 8
    public void onSwitchCameraClicked(View view) {
        mRtcEngine.switchCamera();
    }

    // Tutorial Step 6
    public void onEncCallClicked() {
        finish();
    }

    // Tutorial Step 1
    private void initializeAgoraEngine() {
        mAgoraAPI = AGApplication.the().getmAgoraAPI();
        mRtcEngine = AGApplication.the().getmRtcEngine();
        Ls.w( "initializeAgoraEngine mRtcEngine :" + mRtcEngine);
        if (mRtcEngine != null) {
            mRtcEngine.setLogFile("/sdcard/sdklog.txt");
        }
        setupVideoProfile();

    }

    // Tutorial Step 2
    private void setupVideoProfile() {
        mRtcEngine.enableVideo();
//      mRtcEngine.setVideoProfile(Constants.VIDEO_PROFILE_360P, false); // Earlier than 2.3.0

        //设置视频编码配置
        mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(VideoEncoderConfiguration.VD_640x360, VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));
    }

    // Tutorial Step 3
    private void setupLocalVideo() {
        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        mLayoutBigView.addView(surfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
        mLayoutBigView.setVisibility(View.VISIBLE);
        int ret = mRtcEngine.startPreview();
        Ls.w( "setupLocalVideo startPreview enter << ret :" + ret);
    }

    // Tutorial Step 4
    private void joinChannel() {
        int ret = mRtcEngine.joinChannel(null, channelName, "Extra Optional Data", 0); // if you do not specify the uid, we will generate the uid for you
        Ls.w( "joinChannel enter ret :" + ret);
    }

    // Tutorial Step 5
    // 步骤a:都显示自己  （已完成）
    // 步骤b:医生端统一显示患者  （待完成）
    private void setupRemoteVideo(int uid) {
        Ls.w( "setupRemoteVideo uid: " + uid + " " + mLayoutBigView.getChildCount());
        if (mLayoutBigView.getChildCount() >= 1) {
            mLayoutBigView.removeAllViews();
        }

        SurfaceView surfaceViewSmall = RtcEngine.CreateRendererView(getBaseContext());
        surfaceViewSmall.setZOrderMediaOverlay(true);//覆盖在另一个surfaceView上面
        mLayoutSmallView.addView(surfaceViewSmall);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceViewSmall, VideoCanvas.RENDER_MODE_HIDDEN, uid));
        mLayoutSmallView.setVisibility(View.VISIBLE);

        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        mLayoutBigView.addView(surfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
        mLayoutBigView.setVisibility(View.VISIBLE);
    }


    // Tutorial Step 7
    private void onRemoteUserLeft(int uid) {
        if (uid == mRemoteUid) {
            finish();
        }
    }

    // Tutorial Step 10
    private void onRemoteUserVideoMuted(int uid, boolean muted) {
        FrameLayout container = (FrameLayout) findViewById(R.id.remote_video_view_container);

        SurfaceView surfaceView = (SurfaceView) container.getChildAt(0);

        Object tag = surfaceView.getTag();
        if (tag != null && (Integer) tag == uid) {
            surfaceView.setVisibility(muted ? View.GONE : View.VISIBLE);
        }
    }


    public boolean checkSelfPermission(String permission, int requestCode) {
        Ls.w( "checkSelfPermission " + permission + " " + requestCode);
        if (ContextCompat.checkSelfPermission(this,
                permission)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    requestCode);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        Ls.w( "onRequestPermissionsResult " + grantResults[0] + " " + requestCode);

        switch (requestCode) {
            case PERMISSION_REQ_ID_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA);
                } else {
                    showLongToast("No permission for " + Manifest.permission.RECORD_AUDIO);
                    onEncCallClicked();
                }
                break;
            }
            case PERMISSION_REQ_ID_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_STORAGE);
                } else {
                    showLongToast("No permission for " + Manifest.permission.CAMERA);
                    onEncCallClicked();
                }
                break;
            }
            case PERMISSION_REQ_ID_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initAgoraEngineAndJoinChannel();
                } else {
                    showLongToast("No permission for " + Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    onEncCallClicked();
                }
                break;
            }
        }
    }

    private void initAgoraEngineAndJoinChannel() {
        initializeAgoraEngine();
        AGApplication.the().setOnAgoraEngineInterface(this);
        setupData();
    }

}
