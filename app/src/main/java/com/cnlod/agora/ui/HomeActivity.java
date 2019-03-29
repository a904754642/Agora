package com.cnlod.agora.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.cnlod.agora.AGApplication;
import com.cnlod.agora.Constant;
import com.cnlod.agora.R;
import com.cnlod.agora.util.Ls;

import org.json.JSONException;
import org.json.JSONObject;

import io.agora.AgoraAPI;
import io.agora.AgoraAPIOnlySignal;
import io.agora.IAgoraAPI;
import io.agora.rtc.RtcEngine;

public class HomeActivity extends AppCompatActivity {
    private Context mContext;
    private boolean isAudio = false;
    private boolean loginFlag = false;
    private String appId;
    private AgoraAPIOnlySignal mAgoraAPI;
    private final int REQUEST_CODE = 0x01;

    private Button conn2KFBtn;
    private Button conn2DOCBtn;
    private RadioGroup rg;
    private RadioButton rb1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_home);

        initAgoraEngineAndJoinChannel();
        appId = getString(R.string.agora_app_id);
        mContext = this;

        initView();
    }

    private void initView() {
        conn2KFBtn = findViewById(R.id.btn_p4);
        conn2DOCBtn = findViewById(R.id.btn_p5);
        rg = findViewById(R.id.rg);
        rb1 = findViewById(R.id.rb1);

        setBtnEnable(conn2KFBtn, false);
        setBtnEnable(conn2DOCBtn, false);

        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                isAudio = checkedId == rb1.getId();
            }
        });
    }

    private String usrId;

    public void onPatient(View view) {//我是患者
        setBtnEnable(conn2KFBtn, true);
        setBtnEnable(conn2DOCBtn, false);
        usrId=Constant.userId1;

        mAgoraAPI.login2(appId, Constant.userId1, "_no_need_token", 0, "", 5, 1);
    }

    public void onKF(View view) {//我是客服
        setBtnEnable(conn2KFBtn, false);
        setBtnEnable(conn2DOCBtn, true);
        usrId=Constant.userId2;

        mAgoraAPI.login2(appId, Constant.userId2, "_no_need_token", 0, "", 5, 1);
    }

    public void onDoctor(View view) {//我是医生
        setBtnEnable(conn2KFBtn, false);
        setBtnEnable(conn2DOCBtn, false);
        usrId=Constant.userId3;

        mAgoraAPI.login2(appId, Constant.userId3, "_no_need_token", 0, "", 5, 1);
    }

    public void onInviteKF(View view) {//邀请客服语音通话
        Ls.w("queryUserStatus--" + Constant.userId2);
        mAgoraAPI.queryUserStatus(Constant.userId2);
    }

    public void onInviteDoctor(View view) {//邀请医生语音通话
        Ls.w("queryUserStatus--" + Constant.userId2);
        mAgoraAPI.queryUserStatus(Constant.userId3);
    }

    private void setBtnEnable(Button button, boolean b) {
        button.setClickable(b);
        button.setEnabled(b);
    }

    private void initAgoraEngineAndJoinChannel() {
        mAgoraAPI = AGApplication.the().getmAgoraAPI();
    }

    private void addCallback() {
        mAgoraAPI.callbackSet(new AgoraAPI.CallBack() {

            @Override
            public void onLoginSuccess(int i, int i1) {
                Ls.w("onLoginSuccess " + i + "  " + i1);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loginFlag = true;
                        Ls.ts("注册成功");
                    }
                });
            }

            @Override
            public void onLogout(final int i) {
                Ls.w("onLogout  i = " + i);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (i == IAgoraAPI.ECODE_LOGOUT_E_KICKED) { // other login the account
                            Ls.ts("Other login account, you are logout.");
                        } else if (i == IAgoraAPI.ECODE_LOGOUT_E_NET) { // net
                            Ls.ts("Logout for Network can not be.");
                        }
                    }
                });

            }

            @Override
            public void onLoginFailed(final int i) {
                Ls.w("onLoginFailed " + i);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (i == IAgoraAPI.ECODE_LOGIN_E_NET) {
                            Ls.ts("Login Failed for the network is not available");
                        }
                        Ls.ts("注册失败");
                    }
                });
            }

            @Override
            public void onInviteReceived(final String channelID, final String account, final int uid, final String s2) { //call out other remote receiver
                Ls.e("111-----onInviteReceived  channelID = " + channelID + " account = " + account + "   uid=" + uid + "  s2 = " + s2);
                Ls.e("222-----onInviteReceived  account = " + Constant.userId2);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject json = new JSONObject(s2);
                            isAudio = json.optInt("isAudio") == 1;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Intent intent = new Intent();
                        if (isAudio) {//音频
                            intent.setClass(mContext, CallForAudioActivity.class);
                        } else {//视频
                            intent.setClass(mContext, CallForVideoActivity.class);
                        }
                        intent.putExtra("account", usrId);//自己
                        intent.putExtra("channelName", channelID);
                        intent.putExtra("subscriber", account);//对方
                        intent.putExtra("type", Constant.CALL_IN);
                        intent.putExtra("isAudio", isAudio);
                        startActivityForResult(intent, REQUEST_CODE);
                    }
                });
            }

            @Override
            public void onInviteReceivedByPeer(final String channelID, final String account, int uid) {//call out other local receiver
                Ls.e("111-----onInviteReceivedByPeer  channelID = " + channelID + "  account = " + account);
                Ls.e("222-----onInviteReceivedByPeer  account  = " + Constant.userId1);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Intent intent = new Intent();
                        if (isAudio) {//音频
                            intent.setClass(mContext, CallForAudioActivity.class);
                        } else {//视频
                            intent.setClass(mContext, CallForVideoActivity.class);
                        }
                        intent.putExtra("account", usrId);//自己
                        intent.putExtra("channelName", channelID);
                        intent.putExtra("subscriber", account);//对方
                        intent.putExtra("type", Constant.CALL_OUT);
                        intent.putExtra("isAudio", isAudio);
                        startActivityForResult(intent, REQUEST_CODE);

                    }
                });

            }

            @Override
            public void onInviteFailed(String channelID, String account, int uid, int i1, String s2) {
                Ls.w("111-----onInviteFailed  channelID = " + channelID + " account = " + account + "   uid=" + uid + " s2: " + s2 + " i1: " + i1);
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
                Ls.w("111-----onQueryUserStatusResult name = " + name + " status = " + status);

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

                            mAgoraAPI.channelInviteUser2("channel", name, json.toString());
                        } else if (status.equals("0")) {
                            Ls.ts(name + " is offline ，不在线");
                        }
                    }
                });
            }

        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Ls.w("onActivityResult requestCode: " + requestCode + " resultCode: " + resultCode);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getStringExtra("result").equals("finish")) {
                finish();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        addCallback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Ls.w("onDestroy");
        if (loginFlag) {
            mAgoraAPI.logout();
        }
        RtcEngine.destroy();
    }


}