package com.example.dharamvir.syncphonecontactwithserver;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OngoingCallActivity extends AppCompatActivity
        implements
        Publisher.PublisherListener,
        Session.SessionListener {

    private static final String TAG = "simple-multiparty " + MainActivity.class.getSimpleName();

    private final int MAX_NUM_SUBSCRIBERS = 4;

    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;

    private Session mSession;
    private Publisher mPublisher;

    private int numUserConnected;

    private ArrayList<Subscriber> mSubscribers = new ArrayList<Subscriber>();
    private HashMap<Stream, Subscriber> mSubscriberStreams = new HashMap<Stream, Subscriber>();

    private RelativeLayout mPublisherViewContainer;
    private FrameLayout mPublisherViewContainer_FrameLayout;

    private FrameLayout mSubscriberViewContainer;
    private Subscriber mSubscriber;

    boolean isMultiParty = true;
    boolean isIncoming = false;

    String callerName;
    List<String> names, tokens;
    Boolean noResponse = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.call_bg);


        if(getIntent().getExtras() != null && getIntent().getStringExtra("API_KEY") != null)
        {
            isIncoming = true;
            Log.d("apikey", getIntent().getStringExtra("API_KEY") + "  " + getIntent().getStringExtra("SESSION_ID") + "   " + getIntent().getStringExtra("TOKEN"));
            OpenTokConfig.API_KEY = getIntent().getStringExtra("API_KEY");
            OpenTokConfig.SESSION_ID = getIntent().getStringExtra("SESSION_ID");
            OpenTokConfig.TOKEN = getIntent().getStringExtra("TOKEN");
            callerName = getIntent().getStringExtra("From");
            isMultiParty = getIntent().getBooleanExtra("multi", false);

        }

        else
        {
            names = getIntent().getStringArrayListExtra("names");
            tokens = getIntent().getStringArrayListExtra("tokens");
            isMultiParty = getIntent().getBooleanExtra("multi", false);
        }

        if(isMultiParty) {

            ((FrameLayout) findViewById(R.id.single_party)).setVisibility(View.GONE);
            mPublisherViewContainer = (RelativeLayout) findViewById(R.id.publisherview);
        }
        else
        {
            ((RelativeLayout) findViewById(R.id.multi_party)).setVisibility(View.GONE);
            mPublisherViewContainer_FrameLayout = (FrameLayout)findViewById(R.id.publisher_container_framelayout);
            mSubscriberViewContainer = (FrameLayout)findViewById(R.id.subscriber_container);
        }


        if(isIncoming == true) {
            mSession = new Session.Builder(OngoingCallActivity.this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID).build();
            mSession.setSessionListener(this);
            mSession.connect(OpenTokConfig.TOKEN);

            ((TextView)findViewById(R.id.textView2)).setText(callerName);
        }
        else{
            if(names.size() > 1)
            ((TextView)findViewById(R.id.textView2)).setText(names.get(0) + " and " + Integer.toString(names.size() - 1) + " others");

            else
                ((TextView)findViewById(R.id.textView2)).setText(names.get(0));

            connectToSession();
        }

        final ToggleButton  toggle = (ToggleButton) findViewById(R.id.toggle);

     //  Log.d("toggle default", Boolean.toString(toggle.isChecked()));

        ((ImageView)findViewById(R.id.swap_camera)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(mPublisher != null)
                    mPublisher.cycleCamera();
            }
        });

        ((ImageView)findViewById(R.id.end_call_image)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                disconnectSession();
                endCall();
            }
        });

        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.d("MainActivity", "checked");
                    toggle.setBackgroundResource(R.drawable.mute);
                    if(mPublisher != null)
                        mPublisher.setPublishAudio(false);
                    // The toggle is enabled
                } else {
                    // The toggle is disabled
                    Log.d("MainActivity", "unchecked");
                    toggle.setBackgroundResource(R.drawable.unmute);
                    if(mPublisher != null)
                        mPublisher.setPublishAudio(true);
                }
            }
        });


   /*     final Button swapCamera = (Button) findViewById(R.id.swapCamera);
        swapCamera.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mPublisher == null) {
                    return;
                }
                mPublisher.cycleCamera();
            }
        });

       final ToggleButton toggleAudio = (ToggleButton) findViewById(R.id.toggleAudio);
        toggleAudio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mPublisher == null) {
                    return;
                }
                if (isChecked) {
                    mPublisher.setPublishAudio(true);
                } else {
                    mPublisher.setPublishAudio(false);
                }
            }
        });

        final ToggleButton toggleVideo = (ToggleButton) findViewById(R.id.toggleVideo);
        toggleVideo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mPublisher == null) {
                    return;
                }
                if (isChecked) {
                    mPublisher.setPublishVideo(true);
                } else {
                    mPublisher.setPublishVideo(false);
                }
            }
        });
*/
        // requestPermissions();
    }

    private void connectToSession() {

        mSession = new Session.Builder(OngoingCallActivity.this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID).build();
        mSession.setSessionListener(OngoingCallActivity.this);
        mSession.connect(OpenTokConfig.TOKEN);

new GetLogoDetails().execute();
    }

    protected class GetLogoDetails extends AsyncTask<String,Void,Void> {




        public GetLogoDetails() {


        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... params) {

            String requestUrl = "http://www.contactsyncer.com/notifycaller.php";



            JSONObject jsonObject = new JSONObject();

            try {

                jsonObject.put("from", getSharedPreferences(PhoneAuthActivity.MyPREFERENCES, MODE_PRIVATE).getString("name", null));
                jsonObject.put("device_tokens", tokens);

                jsonObject.put("SessionID", OpenTokConfig.SESSION_ID);
                jsonObject.put("Token", OpenTokConfig.TOKEN);
                jsonObject.put("API_KEY", OpenTokConfig.API_KEY);
                jsonObject.put("API_KEY", OpenTokConfig.API_KEY);
                jsonObject.put("multi", Boolean.toString(isMultiParty));


               // jsonObject.put("device_tokens", tokens);


            } catch (JSONException j) {
                j.printStackTrace();
            }

            String response = MainActivity.postObject(requestUrl, jsonObject);

            if (response == null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(OngoingCallActivity.this, "Internal error occured! Please try again!", Toast.LENGTH_SHORT).show();
                    }
                });

                return null;
            }

            Log.e("response is", "" + response);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            ((RelativeLayout) findViewById(R.id.multi_party)).postDelayed(new Runnable() {
                @Override
                public void run() {

                    if(numUserConnected == 0)
                    {
                        noResponse = true;
                        endCall();
                    }

                }
            }, 20000);

            super.onPostExecute(aVoid);
        }
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");

        super.onStart();
    }

    @Override
    protected void onRestart() {
        Log.d(TAG, "onRestart");

        super.onRestart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");

        super.onResume();

        if (mSession == null) {
            return;
        }
        mSession.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");

        super.onPause();

        if (mSession == null) {
            return;
        }
        mSession.onPause();

        if (isFinishing()) {
            disconnectSession();
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onPause");

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");

        disconnectSession();

        super.onDestroy();
    }


    @Override
    public void onConnected(Session session) {
        Log.d(TAG, "onConnected: Connected to session " + session.getSessionId());

        mPublisher = new Publisher.Builder(OngoingCallActivity.this).name("publisher").build();

        mPublisher.setPublisherListener(this);
        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);


        if(isMultiParty)
            mPublisherViewContainer.addView(mPublisher.getView());
        else
            mPublisherViewContainer_FrameLayout.addView(mPublisher.getView());

        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session) {
        Log.d(TAG, "onDisconnected: disconnected from session " + session.getSessionId());

        mSession = null;
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.d(TAG, "onError: Error (" + opentokError.getMessage() + ") in session " + session.getSessionId());

        Toast.makeText(this, "Session error. See the logcat please.", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.d(TAG, "onStreamReceived: New stream " + stream.getStreamId() + " in session " + session.getSessionId());

        numUserConnected++;

        if( ((LinearLayout)findViewById(R.id.calling_text_layout)).getVisibility() == View.VISIBLE)
            ((LinearLayout)findViewById(R.id.calling_text_layout)).setVisibility(View.INVISIBLE);

        if(!isMultiParty)
        {
            if (mSubscriber == null) {
                mSubscriber = new Subscriber.Builder(this, stream).build();
                mSubscriber.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
                mSession.subscribe(mSubscriber);
                mSubscriberViewContainer.addView(mSubscriber.getView());
            }

            return;
        }

        if (mSubscribers.size() + 1 > MAX_NUM_SUBSCRIBERS) {
            Toast.makeText(this, "New subscriber ignored. MAX_NUM_SUBSCRIBERS limit reached.", Toast.LENGTH_LONG).show();
            return;
        }






        final Subscriber subscriber = new Subscriber.Builder(OngoingCallActivity.this, stream).build();
        mSession.subscribe(subscriber);
        mSubscribers.add(subscriber);
        mSubscriberStreams.put(stream, subscriber);

        int position = mSubscribers.size() - 1;
        int id = getResources().getIdentifier("subscriberview" + (new Integer(position)).toString(), "id", OngoingCallActivity.this.getPackageName());
        RelativeLayout subscriberViewContainer = (RelativeLayout) findViewById(id);

        subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        subscriberViewContainer.addView(subscriber.getView());

    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.d(TAG, "onStreamDropped: Stream " + stream.getStreamId() + " dropped from session " + session.getSessionId());

        numUserConnected--;


        if(!isMultiParty)
        {
            if (mSubscriber != null) {
                mSubscriber = null;
                mSubscriberViewContainer.removeAllViews();
            }

            endCall();
            return;
        }




        Subscriber subscriber = mSubscriberStreams.get(stream);
        if (subscriber == null) {
            return;
        }

        int position = mSubscribers.indexOf(subscriber);
        int id = getResources().getIdentifier("subscriberview" + (new Integer(position)).toString(), "id", OngoingCallActivity.this.getPackageName());

        mSubscribers.remove(subscriber);
        mSubscriberStreams.remove(stream);

        RelativeLayout subscriberViewContainer = (RelativeLayout) findViewById(id);
        subscriberViewContainer.removeView(subscriber.getView());

        if(numUserConnected == 0)
            endCall();


    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        Log.d(TAG, "onStreamCreated: Own stream " + stream.getStreamId() + " created");
    }

    private void endCall()
    {

        if(!noResponse)
        ((TextView)findViewById(R.id.textView3)).setText("Call Ended...");
        else
            ((TextView)findViewById(R.id.textView3)).setText("No Response.. Call Ended...");
        ((LinearLayout)findViewById(R.id.calling_text_layout)).setVisibility(View.VISIBLE);

        ((TextView)findViewById(R.id.textView3)).postDelayed(new Runnable() {
            @Override
            public void run() {
               /* Intent in = new Intent(OngoingCallActivity.this, MainActivity.class);
                in.putExtra("code", getSharedPreferences(PhoneAuthActivity.MyPREFERENCES, Context.MODE_PRIVATE).getString("code", null));
                in.putExtra("phone", getSharedPreferences(PhoneAuthActivity.MyPREFERENCES, Context.MODE_PRIVATE).getString("phone", null));

                startActivity(in);*/

               disconnectSession();
                OngoingCallActivity.this.finish();

            }
        }, 3000);
    }


    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
        Log.d(TAG, "onStreamDestroyed: Own stream " + stream.getStreamId() + " destroyed");
    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {
        Log.d(TAG, "onError: Error (" + opentokError.getMessage() + ") in publisher");

        Toast.makeText(this, "Session error. See the logcat please.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void disconnectSession() {
        if (mSession == null) {
            return;
        }

        if (mSubscribers.size() > 0) {
            for (Subscriber subscriber : mSubscribers) {
                if (subscriber != null) {
                    mSession.unsubscribe(subscriber);
                    subscriber.destroy();
                }
            }
        }

        if (mPublisher != null) {
            if(isMultiParty)
            mPublisherViewContainer.removeView(mPublisher.getView());
            else
                mPublisherViewContainer_FrameLayout.removeView(mPublisher.getView());
            mSession.unpublish(mPublisher);
            mPublisher.destroy();
            mPublisher = null;
        }
        mSession.disconnect();
    }
}
