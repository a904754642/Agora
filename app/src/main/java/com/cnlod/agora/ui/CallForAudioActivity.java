package com.cnlod.agora.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
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
import com.cnlod.agora.adapter.AudioAdapter;
import com.cnlod.agora.adapter.SurfaceAdapter;
import com.cnlod.agora.entity.User;
import com.cnlod.agora.util.GsonUtil;
import com.cnlod.agora.util.Ls;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.agora.AgoraAPI;
import io.agora.AgoraAPIOnlySignal;
import io.agora.IAgoraAPI;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

/**
 * Created by beryl on 2017/11/6.
 */

public class CallForAudioActivity extends AppCompatActivity implements AGApplication.OnAgoraEngineInterface {

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
    private Button inviteDoctorBtn, invitePatientBtn;

    private FrameLayout mLayoutBigView;
    private RecyclerView smallRecyclerView;

    private String channelName = "channelid";
    private int callType = -1;
    private boolean mIsCallInRefuse = false;
    //    private int mRemoteUid = 0;
    private String myself;
    private boolean isAudio;

    private List<Integer> uids = new ArrayList<>();
    private List<String> list = new ArrayList<>();
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_audio);
        Ls.e("音频！！！！");
        InitUI();
        mContext = this;

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
        smallRecyclerView = findViewById(R.id.small_video_recycler);

        inviteDoctorBtn = findViewById(R.id.btn_invited);
        invitePatientBtn = findViewById(R.id.btn_invitep);

        smallRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));

    }

    private void setupData() {
        Intent intent = getIntent();

        isAudio = intent.getBooleanExtra("isAudio", false);
        myself = intent.getStringExtra("account");
        if (myself.equals(Constant.userId2)) {
            inviteDoctorBtn.setVisibility(View.VISIBLE);
            invitePatientBtn.setVisibility(View.VISIBLE);
        } else {
            inviteDoctorBtn.setVisibility(View.INVISIBLE);
            invitePatientBtn.setVisibility(View.INVISIBLE);
        }

        mSubscriber = intent.getStringExtra("subscriber");//对方

        Ls.e("我是" + myself + "  对方是" + mSubscriber);
        channelName = intent.getStringExtra("channelName");
        callType = intent.getIntExtra("type", -1);
        if (callType == Constant.CALL_IN) {//收到音频邀请
            mIsCallInRefuse = true;//todo ????
            mLayoutCallIn.setVisibility(View.VISIBLE);
            mCallOutHangupBtn.setVisibility(View.GONE);
            mCallTitle.setText(String.format(Locale.US, "%s is calling...", mSubscriber));

//            setupLocalVideo(); // Tutorial Step 3
        } else if (callType == Constant.CALL_OUT) {//发送音频邀请
            mLayoutCallIn.setVisibility(View.GONE);
            mCallOutHangupBtn.setVisibility(View.VISIBLE);
            mCallTitle.setText(String.format(Locale.US, "%s is be called...", mSubscriber));

//            setupLocalVideo(); // Tutorial Step 3
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
    public void onFirstLocalAudioFrame(int elapsed) {
        if (isAudio) {
            Ls.w("onFirstRemoteAudioFrame  elapsed:" + elapsed);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setupRemoteAudio();
                }
            });
        }
    }

    @Override
    public void onFirstRemoteAudioFrame(int uid, int elapsed) {
        if (isAudio) {
            Ls.w("onFirstRemoteAudioFrame  elapsed:" + elapsed);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setupRemoteAudio();
                }
            });
        }
    }

    @Override
    public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) { // Tutorial Step 5
        Ls.w( "onFirstRemoteVideoDecoded  uid:" + uid);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                if (mRemoteUid != 0) {
//                    return;
//                }
//                mRemoteUid = uid;
//                setupRemoteVideo(uid);
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
//                onRemoteUserVideoMuted(uid, muted);
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

            case R.id.btn_invited://邀请医生
                Ls.ts("邀请医生");
                mAgoraAPI.queryUserStatus(Constant.userId3);
                break;
            case R.id.btn_invitep://邀请患者
                Ls.ts("邀请患者");
                mAgoraAPI.queryUserStatus(Constant.userId1);
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
            public void onUserAttrResult(String account, String name, String value) {
                super.onUserAttrResult(account, name, value);
                Ls.e("account = " + account + "  name = " + name + "  value = " + value);
            }

            @Override
            public void onInvokeRet(String callID, String err, final String resp) {
                super.onInvokeRet(callID, err, resp);
                Ls.e("onInvokeRet     err=" + err + "  resp=" + resp);
//                {"list":[["34",947090903],["22",186136837],["2",448070539]],"num":3,"result":"ok"}

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        User user = GsonUtil.GsonToBean(resp, User.class);
                        if (user.getResult().equals("ok")) {
                            Map<String, Integer> map = user.getList();
                            list.clear();
                            for (String key : map.keySet()) {
                                if (!key.equals(myself)) {
                                    list.add(key);
                                }
                            }

                            smallRecyclerView.setAdapter(new AudioAdapter(mContext, list));
                            smallRecyclerView.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }

            @Override
            public void onLogout(final int i) {
                Ls.w( "onLogout  i = " + i);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (i == IAgoraAPI.ECODE_LOGOUT_E_KICKED) { // other login the account
                            Ls.ts("Other login account ,you are logout.");

                        } else if (i == IAgoraAPI.ECODE_LOGOUT_E_NET) { // net
                            Ls.ts("Logout for Network can not be.");
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
            public void onInviteReceivedByPeer(final String channelID, final String account, int uid) {
                Ls.e("onInviteReceivedByPeer  channelID = " + channelID + "  account = " + account);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCallOutHangupBtn.setVisibility(View.VISIBLE);
                        mSubscriber = account;
                        mCallTitle.setText(String.format(Locale.US, "%s is being called ...", mSubscriber));
                    }
                });
            }

            /**
             * other receiver call accept callback
             */
            @Override
            public void onInviteAcceptedByPeer(String channelID, final String account, final int uid, String s2) {
                Ls.e("onInviteAcceptedByPeer   channelID = " + channelID + "  account = " + account + "  uid=" + uid + "  s2 =" + s2);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCallTitle.setVisibility(View.GONE);
//                        mRemoteUid = 0;
                    }
                });

            }

            /**
             * other receiver call refuse callback
             */

            @Override
            public void onInviteRefusedByPeer(String channelID, final String account, int uid, final String s2) {
                Ls.w( "onInviteRefusedByPeer channelID = " + channelID + " account = " + account + " s2 = " + s2);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (s2.contains("status") && s2.contains("1")) {
                            Ls.ts(account + " reject your call for busy");
                        } else {
                            Ls.ts(account + " reject your call");
                        }

                        onEncCallClicked();
                    }
                });
            }


            /**
             * end call remote receiver callback
             * 邀请者主动结束
             */
            @Override
            public void onInviteEndByPeer(final String channelID, String account, int uid, String s2) {
                Ls.w("onInviteEndByPeer channelID = " + channelID + " account = " + account + "  uid=" + uid);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (channelID.equals(channelName)) {
//                            onEncCallClicked();
                        }

                    }
                });
            }

            /**
             * end call local receiver callback
             * 自己退出
             */
            @Override
            public void onInviteEndByMyself(String channelID, String account, int uid) {
                Ls.w( "onInviteEndByMyself channelID = " + channelID + "  account = " + account);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (myself.equals(Constant.userId2)) {//发送点对点信息
                            mAgoraAPI.messageInstantSend(Constant.userId1, 0, "logout", "");
                            mAgoraAPI.messageInstantSend(Constant.userId3, 0, "logout", "");
                        }
                        onEncCallClicked();

                    }
                });
            }

            //收到点对点信息
            @Override
            public void onMessageInstantReceive(final String account, int uid, final String msg) {
                super.onMessageInstantReceive(account, uid, msg);
                Ls.e("onMessageInstantReceive     account = " + account + "    uid = " + uid + "  msg = " + msg);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (account.equals(Constant.userId2) && "logout".equals(msg)) {
                            onEncCallClicked();
                        }
                    }
                });
            }

            @Override
            public void onError(final String s, final int i, final String s1) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (s.equals("query_user_status")) {
                            Ls.ts(s1);
                        }

                        if (i == 208) {
                            Ls.ts("用户已登录");
                        } else {
                            Ls.e("onError s = " + s + " i = " + i + " s1 = " + s1);
                        }
                    }
                });
            }

            @Override
            public void onQueryUserStatusResult(final String name, final String status) {
                Ls.w("222-----onQueryUserStatusResult name = " + name + " status = " + status);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (status.equals("1")) {
                            Ls.e("目标在线，准备邀请");

                            JSONObject json = new JSONObject();
                            try {
                                json.put("isAudio", isAudio ? 1 : 0);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
//
                            mAgoraAPI.channelInviteUser2("channel", name, json.toString());//json.toString()
                        } else if (status.equals("0")) {
                            Ls.ts(name + " is offline ，不在线");
                        }
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

//        mAgoraAPI.getAttr("name");
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
        mAgoraAPI.channelLeave(channelName);

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
//        setupVideoProfile();

    }

    // Tutorial Step 2
    private void setupVideoProfile() {
        mRtcEngine.enableVideo();

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
        mAgoraAPI.channelJoin(channelName);
    }

    // Tutorial Step 5
    // 步骤a:都显示自己  （已完成）
    // 步骤b:医生端统一显示患者  （待完成）
    private void setupRemoteVideo(int uid) {
        Ls.e("uids  " + uids.size());
        Ls.w( "setupRemoteVideo uid: " + uid + " " + mLayoutBigView.getChildCount());
        if (mLayoutBigView.getChildCount() >= 1) {
            mLayoutBigView.removeAllViews();
        }

        uids.add(uid);
        smallRecyclerView.setAdapter(new SurfaceAdapter(this, uids));
        smallRecyclerView.setVisibility(View.VISIBLE);

        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        mLayoutBigView.addView(surfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
        mLayoutBigView.setVisibility(View.VISIBLE);
    }

    private void setupRemoteAudio() {//"{\"status\":1}"
        JSONObject json = new JSONObject();
        try {
            json.put("name", channelName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mAgoraAPI.invoke("io.agora.signal.channel_query_userlist", json.toString(), "");

    }

    // Tutorial Step 7
    private void onRemoteUserLeft(int uid) {
        if(isAudio){
            Ls.e("onRemoteUserLeft   list.size=" + list.size());
            if (list.size() > 1) {
//                //不能使用  uid.remove(uid),不能按次序删除，而是按对象删除
//                uids.remove(Integer.valueOf(uid));
//                smallRecyclerView.setAdapter(new SurfaceAdapter(this, uids));
                setupRemoteAudio();
            } else {
                finish();
            }
        }else{
            Ls.e("onRemoteUserLeft   uids.size=" + uids.size());
            if (uids.size() > 1) {
                //不能使用  uid.remove(uid),不能按次序删除，而是按对象删除
                uids.remove(Integer.valueOf(uid));
                smallRecyclerView.setAdapter(new SurfaceAdapter(this, uids));
            } else {
                finish();
            }
        }
    }

    // Tutorial Step 10
    private void onRemoteUserVideoMuted(int uid, boolean muted) {
//        FrameLayout container = (FrameLayout) findViewById(R.id.remote_video_view_container);
//
//        SurfaceView surfaceView = (SurfaceView) container.getChildAt(0);
//
//        Object tag = surfaceView.getTag();
//        if (tag != null && (Integer) tag == uid) {
//            surfaceView.setVisibility(muted ? View.GONE : View.VISIBLE);
//        }
    }


    public boolean checkSelfPermission(String permission, int requestCode) {
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
