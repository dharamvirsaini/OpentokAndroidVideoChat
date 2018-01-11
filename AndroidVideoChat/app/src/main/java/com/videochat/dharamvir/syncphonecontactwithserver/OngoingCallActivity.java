package com.videochat.dharamvir.syncphonecontactwithserver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.Connection;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class OngoingCallActivity extends AppCompatActivity
        implements
        Publisher.PublisherListener,
        Session.SessionListener, View.OnClickListener, Session.SignalListener, EasyPermissions.PermissionCallbacks {

    private static final String TAG = OngoingCallActivity.class.getSimpleName();

    private final int MAX_NUM_SUBSCRIBERS = 4;

    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;

    private Session mSession;
    private Publisher mPublisher;
    private String shareID;

    private int numUserConnected;

   // private ArrayList<Subscriber> mSubscribers = new ArrayList<Subscriber>();
    private HashMap<Stream, Subscriber> mSubscriberStreams = new HashMap<Stream, Subscriber>();

    private RelativeLayout mPublisherViewContainer;
    private RelativeLayout mPublisherViewContainer_FrameLayout;

    private RelativeLayout mSubscriberViewContainer;
    private Subscriber mSubscriber;

    boolean isMultiParty = true;
    boolean isIncoming = false;
    private final int PICK_IMAGE_REQUEST = 574;

    String callerName;
    List<String> names, tokens;
    Boolean noResponse = false;
    List<SignalMessage> mMessages;
    private RecyclerView mRecList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.call_bg);

        mMessages = new ArrayList<>();

        if(getIntent().getExtras() != null && getIntent().getStringExtra("API_KEY") != null)
        {
            //incoming call request
            isIncoming = true;
            Log.d("apikey", getIntent().getStringExtra("API_KEY") + "  " + getIntent().getStringExtra("SESSION_ID") + "   " + getIntent().getStringExtra("TOKEN"));
            OpenTokConfig.API_KEY = getIntent().getStringExtra("API_KEY");
            OpenTokConfig.SESSION_ID = getIntent().getStringExtra("SESSION_ID");
            OpenTokConfig.TOKEN = getIntent().getStringExtra("TOKEN");
            callerName = getIntent().getStringExtra("From");
            isMultiParty = Boolean.parseBoolean(getIntent().getStringExtra("multi"));

        }
        else {
            names = getIntent().getStringArrayListExtra("names");
            tokens = getIntent().getStringArrayListExtra("tokens");
            isMultiParty = getIntent().getBooleanExtra("multi", false);
        }

        if (isMultiParty) {
            ((RelativeLayout) findViewById(R.id.single_party)).setVisibility(View.GONE);
          //  mPublisherViewContainer = (RelativeLayout) findViewById(R.id.publisherview);
        }
        else {
            ((RelativeLayout) findViewById(R.id.multi_party)).setVisibility(View.GONE);

            mSubscriberViewContainer = (RelativeLayout) findViewById(R.id.subscriber_container);
        }

        mPublisherViewContainer_FrameLayout = (RelativeLayout) findViewById(R.id.publisher_container_framelayout);
        findViewById(R.id.toggle_text).setOnClickListener(this);
        findViewById(R.id.swap_camera).setOnClickListener(this);
        findViewById(R.id.end_call_image).setOnClickListener(this);
        findViewById(R.id.send_text).setOnClickListener(this);
        findViewById(R.id.send_picture).setOnClickListener(this);
        findViewById(R.id.minimize_chat).setOnClickListener(this);
        findViewById(R.id.share_image).setOnClickListener(this);

        mRecList = (RecyclerView)findViewById(R.id.text_chat);
        mRecList.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mRecList.setLayoutManager(llm);

        mRecList.setAdapter(new MessageAdapter(mMessages, this));

        if (isIncoming == true) {
           // connectToSession();
            requestPermissions();
        }
        else {
            if(names.size() > 1)
            ((TextView)findViewById(R.id.textView2)).setText(names.get(0) + " and " + Integer.toString(names.size() - 1) + " others");
            else
                ((TextView)findViewById(R.id.textView2)).setText(names.get(0));

           // connectToSession();
            requestPermissions();
        }

        final ToggleButton  toggle = (ToggleButton) findViewById(R.id.toggle);
        final ToggleButton  toggle_video = (ToggleButton) findViewById(R.id.toggle_video);


        toggle_video.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.d("DisplayContactsActivity", "checked");
                    toggle_video.setBackgroundResource(R.drawable.no_video);
                    if(mPublisher != null)
                        mPublisher.setPublishVideo(false);
                    // The toggle is enabled
                } else {
                    // The toggle is disabled
                    Log.d("DisplayContactsActivity", "unchecked");
                    toggle_video.setBackgroundResource(R.drawable.video);
                    if(mPublisher != null)
                        mPublisher.setPublishVideo(true);
                }
            }
        });

        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.d("DisplayContactsActivity", "checked");
                    toggle.setBackgroundResource(R.drawable.mute);
                    if(mPublisher != null)
                        mPublisher.setPublishAudio(false);
                    // The toggle is enabled
                } else {
                    // The toggle is disabled
                    Log.d("DisplayContactsActivity", "unchecked");
                    toggle.setBackgroundResource(R.drawable.unmute);
                    if(mPublisher != null)
                        mPublisher.setPublishAudio(true);
                }
            }
        });



    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this)
                    .setTitle(getString(R.string.title_settings_dialog))
                    .setRationale(getString(R.string.rationale_ask_again))
                    .setPositiveButton(getString(R.string.setting))
                    .setNegativeButton(getString(R.string.cancel))
                    .setRequestCode(RC_SETTINGS_SCREEN_PERM)
                    .build()
                    .show();
        }
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions() {

        String[] perms = {android.Manifest.permission.INTERNET, android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO };
        if (EasyPermissions.hasPermissions(this, perms)) {
            // if there is no server URL set
            connectToSession();
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), RC_VIDEO_APP_PERM, perms);
        }
    }

    private void connectToSession() {

        final Long time = System.currentTimeMillis();
        Log.d("time is " , Long.toString(time));

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = null;

        if(!isIncoming) {
            url = Constants.SERVER_URL + Long.toString(time);
            shareID = Long.toString(time);
        }
        else {
            shareID = OpenTokConfig.TOKEN;
            url = Constants.SERVER_URL + OpenTokConfig.TOKEN;
        }


        Log.d("url is " , url);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>(){
                    @Override
                    public void onResponse(String response) {
                        Log.d("Response is ", response);

                        JSONObject obj = null;
                        try {
                            obj = new JSONObject(response);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            //return null;
                        }

                        try {
                            OpenTokConfig.API_KEY = obj.getString("apiKey");
                            OpenTokConfig.SESSION_ID = obj.getString("sessionId");
                            OpenTokConfig.TOKEN = obj.getString("token");
                            OpenTokConfig.time = Long.toString(time);

                            if(!isIncoming){

                                mSession = new Session.Builder(OngoingCallActivity.this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID).build();
                                mSession.setSessionListener(OngoingCallActivity.this);
                                mSession.connect(OpenTokConfig.TOKEN);

                                new NotifyCaller().execute();
                            }

                            else
                            {
                                mSession = new Session.Builder(OngoingCallActivity.this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID).build();
                                mSession.setSessionListener(OngoingCallActivity.this);
                                mSession.connect(OpenTokConfig.TOKEN);

                                ((TextView)findViewById(R.id.textView2)).setText(callerName);
                            }

                            mSession.setSignalListener(OngoingCallActivity.this);
                            Log.d("credentials are ", obj.getString("apiKey") + obj.getString("sessionId") + obj.getString("token"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        // Display the first 500 characters of the response string.
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(OngoingCallActivity.this, "Internal error occured! Please try again!", Toast.LENGTH_SHORT).show();
            }
        });
// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    @Override
    public void onClick(View v) {

        switch(v.getId())
        {
            case R.id.swap_camera:
                if(mPublisher != null)
                    mPublisher.cycleCamera();
                break;

            case R.id.end_call_image:
                endCall();
                break;

            case R.id.share_image:
                shareLink();
                break;

            case R.id.toggle_text:
                startChat();
                break;

            case R.id.send_text:
                sendTextMessage();
                break;

            case R.id.send_picture:
                sendPictureMessage();
                break;

            case R.id.minimize_chat:
                ((LinearLayout)findViewById(R.id.end_button_layout)).setVisibility(View.VISIBLE);
                ((LinearLayout)findViewById(R.id.text_chat_layout)).setVisibility(View.GONE);
                break;


        }

    }

    private void shareLink() {

        Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
        whatsappIntent.setType("text/plain");
        whatsappIntent.setPackage("com.whatsapp");
        whatsappIntent.putExtra(Intent.EXTRA_TEXT, "https://www.participateme.com/session/" + shareID);
        startActivity(whatsappIntent);
    }

    private void sendTextMessage() {

        EditText text = (EditText)findViewById(R.id.chat_text);

        if(!text.getText().toString().trim().isEmpty()) {

            JSONObject msg = new JSONObject();

            try {
                msg.put("name", getSharedPreferences(PhoneAuthActivity.MyPREFERENCES, MODE_PRIVATE).getString("name", null));
                msg.put("code", getSharedPreferences(PhoneAuthActivity.MyPREFERENCES, MODE_PRIVATE).getString("phone", null));
                msg.put("image", "n");
                msg.put("data", text.getText().toString().trim());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mSession.sendSignal("default", msg.toString());

            SignalMessage message = new SignalMessage();
            message.setData(text.getText().toString().trim());
            message.setType(Constants.TYPE_SELF_TEXT);
            mMessages.add(message);
            text.setText("");
            mRecList.getAdapter().notifyDataSetChanged();
        }

    }

    private void sendPictureMessage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data!=null && data.getData()!=null){

            Uri imagePath = data.getData();

            try {
                //Getting the Bitmap from Gallery
                Bitmap bitmap2 = MediaStore.Images.Media.getBitmap(getContentResolver(), imagePath);

                //Scaling the bitmap as it might cause issues OPENGL RENDERING
                //  Bitmap bitmap1= new Bitma(getResources() , bitmap2).getBitmap();
                int nh = (int) ( bitmap2.getHeight() * (128.0 / bitmap2.getWidth()) );
                String image = getStringImage(Bitmap.createScaledBitmap(bitmap2, 128, nh, true));
               // String image = getStringImage(bitmap2);
                JSONObject msg = new JSONObject();

                try {
                    msg.put("name", getSharedPreferences(PhoneAuthActivity.MyPREFERENCES, MODE_PRIVATE).getString("name", null));
                    msg.put("image", "y");
                    msg.put("data", image);
                    msg.put("code", getSharedPreferences(PhoneAuthActivity.MyPREFERENCES, MODE_PRIVATE).getString("phone", null));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mSession.sendSignal("default", msg.toString());

                SignalMessage message = new SignalMessage();
                message.setData(image);
                message.setType(Constants.TYPE_SELF_IMAGE);
                mMessages.add(message);
                mRecList.getAdapter().notifyDataSetChanged();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getStringImage(Bitmap bmp){
        if (bmp != null){

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        }

        return "noimage";
    }

    private void startChat() {

        ((LinearLayout)findViewById(R.id.end_button_layout)).setVisibility(View.GONE);

        ((LinearLayout)findViewById(R.id.text_chat_layout)).setVisibility(View.VISIBLE);

       /* if(textLayout.getVisibility() == View.VISIBLE)
        {
            textLayout.setVisibility(View.GONE);
        }
        else {
            textLayout.setVisibility(View.VISIBLE);
        }*/

    }

    @Override
    public void onSignalReceived(Session session, String type, String message, Connection connection) {
        Log.d(TAG, "message received is " + message);

        if(mMessages.size() > 2) {
            mRecList.scrollToPosition(mMessages.size() - 1);
        }

        try {
            JSONObject json = new JSONObject(message);
            String phone = json.getString("code");
            String name = json.getString("name");
            String image = json.getString("image");
            String data = json.getString("data");

            if (!phone.equals(getSharedPreferences(PhoneAuthActivity.MyPREFERENCES, MODE_PRIVATE).getString("phone", null))) {

                SignalMessage signal = new SignalMessage();
                signal.setData(data);
                signal.setName(name);
                signal.setCode(phone);

                if (image.equals("y")) {
                    signal.setType(Constants.TYPE_REMOTE_IMAGE);
                } else {
                    signal.setType(Constants.TYPE_REMOTE_TEXT);
                }

                mMessages.add(signal);


                mRecList.getAdapter().notifyDataSetChanged();
            }
            }
        catch(Exception e){
                e.printStackTrace();
            }
        }



    protected class NotifyCaller extends AsyncTask<String,Void,Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... params) {

            String requestUrl = Constants.NOTIFY_CALLER_REQUEST_URL;

            JSONObject jsonObject = new JSONObject();

            try {

                jsonObject.put("from", getSharedPreferences(PhoneAuthActivity.MyPREFERENCES, MODE_PRIVATE).getString("name", null));
                jsonObject.put("device_tokens", tokens);
                jsonObject.put("SessionID", OpenTokConfig.SESSION_ID);
                jsonObject.put("Token", OpenTokConfig.time);
                jsonObject.put("API_KEY", OpenTokConfig.API_KEY);
                jsonObject.put("multi", Boolean.toString(isMultiParty));

            } catch (JSONException j) {
                j.printStackTrace();
            }

            String response = DisplayContactsActivity.postObject(requestUrl, jsonObject);

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
            }, 40000);

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

        mPublisher.setPublishVideo(true);

    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");

        super.onPause();

        if (mSession == null) {
            return;
        }
        mSession.onPause();

        if(mPublisher != null)
        mPublisher.setPublishVideo(false);

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


    //    if(isMultiParty)
      //      mPublisherViewContainer.addView(mPublisher.getView());
        //else
            mPublisherViewContainer_FrameLayout.addView(mPublisher.getView());

        if (mPublisher.getView() instanceof GLSurfaceView) {
            ((GLSurfaceView) mPublisher.getView()).setZOrderOnTop(true);
        }

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

        if(numUserConnected == 2 && !isMultiParty)
        {
            isMultiParty = true;
            ((RelativeLayout) findViewById(R.id.single_party)).setVisibility(View.GONE);
            ((RelativeLayout) findViewById(R.id.multi_party)).setVisibility(View.VISIBLE);

            mSubscriberViewContainer.removeAllViews();

            mSubscriberStreams.put(mSubscriber.getStream(), mSubscriber);

            int position = mSubscriberStreams.size() - 1;
            int id = getResources().getIdentifier("subscriberview" + (new Integer(position)).toString(), "id", OngoingCallActivity.this.getPackageName());
            RelativeLayout subscriberViewContainer = (RelativeLayout) findViewById(id);

            mSubscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
            if (mSubscriber.getView() instanceof GLSurfaceView) {
               // ((GLSurfaceView) mSubscriber.getView()).setZOrderOnTop(false);
            }
            subscriberViewContainer.addView(mSubscriber.getView());

        }

        if( ((LinearLayout)findViewById(R.id.calling_text_layout)).getVisibility() == View.VISIBLE)
            ((LinearLayout)findViewById(R.id.calling_text_layout)).setVisibility(View.INVISIBLE);

        if(!isMultiParty)
        {
            if (mSubscriber == null) {
                mSubscriber = new Subscriber.Builder(this, stream).build();
                mSubscriber.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
                mSession.subscribe(mSubscriber);
                if (mSubscriber.getView() instanceof GLSurfaceView) {
                   // ((GLSurfaceView) mSubscriber.getView()).setZOrderMediaOverlay(false);
                }
                mSubscriberViewContainer.addView(mSubscriber.getView());
            }

            return;
        }

        if (mSubscriberStreams.size() + 1 > MAX_NUM_SUBSCRIBERS) {
            Toast.makeText(this, "New subscriber ignored. MAX_NUM_SUBSCRIBERS limit reached.", Toast.LENGTH_LONG).show();
            return;
        }

        final Subscriber subscriber = new Subscriber.Builder(OngoingCallActivity.this, stream).build();
        mSession.subscribe(subscriber);
       // mSubscribers.add(subscriber);
        mSubscriberStreams.put(stream, subscriber);

        int position = mSubscriberStreams.size() - 1;
        int id = getResources().getIdentifier("subscriberview" + (new Integer(position)).toString(), "id", OngoingCallActivity.this.getPackageName());
        RelativeLayout subscriberViewContainer = (RelativeLayout) findViewById(id);

        subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        if (subscriber.getView() instanceof GLSurfaceView) {
           // ((GLSurfaceView) subscriber.getView()).setZOrderMediaOverlay(true);
        }
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

       // int position = mSubscribers.indexOf(subscriber);
        //int id = getResources().getIdentifier("subscriberview" + (new Integer(position)).toString(), "id", OngoingCallActivity.this.getPackageName());

       // mSubscribers.remove(subscriber);
        mSubscriberStreams.remove(stream);

        RelativeLayout subscriberViewContainer = (RelativeLayout) subscriber.getView().getParent();

       //old RelativeLayout subscriberViewContainer = (RelativeLayout) findViewById(id);
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

        ((LinearLayout)findViewById(R.id.text_chat_layout)).setVisibility(View.GONE);

        ((LinearLayout)findViewById(R.id.calling_text_layout)).setVisibility(View.VISIBLE);

        ((TextView)findViewById(R.id.textView3)).postDelayed(new Runnable() {
            @Override
            public void run() {
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

        for (HashMap.Entry<Stream, Subscriber> entry : mSubscriberStreams.entrySet())
        {
            System.out.println(entry.getKey() + "/" + entry.getValue());

            Subscriber subscriber = entry.getValue();

            if (subscriber != null) {
                mSession.unsubscribe(subscriber);
                subscriber.destroy();
            }
        }

     /*   if (mSubscribers.size() > 0) {
            for (Subscriber subscriber : mSubscribers) {
                if (subscriber != null) {
                    mSession.unsubscribe(subscriber);
                    subscriber.destroy();
                }
            }
        }*/

        if (mPublisher != null) {
                mPublisherViewContainer_FrameLayout.removeView(mPublisher.getView());
            mSession.unpublish(mPublisher);
            mPublisher.destroy();
            mPublisher = null;
        }
        mSession.disconnect();
    }
}
