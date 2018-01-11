package com.videochat.dharamvir.syncphonecontactwithserver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class DisplayContactsActivity extends AppCompatActivity {

    private static final String TAG = "DisplayContactsActivity";
    public static final String IMAGE_ACCESS_URL = "http://contactsyncer.com/uploads/";
    public static final int REQUEST_PERMISSION_CODE = 1;
    public static DisplayContactsActivity sActivityContext;

    private Cursor mCursor;
    private RecyclerView mRecList;
    private List<String> mPhoneList;
    private List<String> mNameList;
    private ArrayList<String> mDeviceTokens = new ArrayList<>();
    private MaterialDialog mProgressDialog;
    private ArrayList<ContactInfo> result;
     TextView mSelectView;
    private Boolean mFromAddContact = false;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sActivityContext = this;

        Intent in = getIntent();

        String code = in.getStringExtra("code");
        String phone = in.getStringExtra("phone");

        mProgressDialog = new MaterialDialog.Builder(DisplayContactsActivity.this)
                .cancelable(false)
                .progress(true, 100)
                .content("Please wait...")
                .build();

        mProgressDialog.show();

        Log.d("DisplayContactsActivity", "phone is " + phone + " and code is " + code);

        ((ImageView) findViewById(R.id.add_contact)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(DisplayContactsActivity.this, EditProfileActivity.class);
                startActivity(intent);

            }
        });

        ((ImageView) findViewById(R.id.add_contact_below)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mFromAddContact = true;

                Intent intent = new Intent(Intent.ACTION_INSERT,
                        ContactsContract.Contacts.CONTENT_URI);
                startActivity(intent);

            }
        });


        mRecList = (RecyclerView) findViewById(R.id.cardList);
        mRecList.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mRecList.setLayoutManager(llm);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            EnableRuntimePermission();
        } else {
            createList();
        }

        mSelectView = ((TextView) findViewById(R.id.textView4));

        mSelectView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mSelectView.getText().equals("Select")) {
                    mSelectView.setText("Cancel");
                } else {
                    DisplayContactsActivity.this.findViewById(R.id.button2).setVisibility(View.VISIBLE);
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)DisplayContactsActivity.this.findViewById(R.id.add_contact_below).getLayoutParams();
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

                    DisplayContactsActivity.this.findViewById(R.id.button2).setVisibility(View.GONE);
                    mSelectView.setText("Select");
                }

                mRecList.getAdapter().notifyDataSetChanged();


            }
        });


    }


    private List<ContactInfo> createList() {

        List<ContactInfo> result = new ArrayList<ContactInfo>();

        mPhoneList = new ArrayList<>();
        mNameList = new ArrayList<>();

        mCursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

        while (mCursor.moveToNext()) {

            // ContactInfo ci = new ContactInfo();

            String name = mCursor.getString(mCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));

            String phonenumber = mCursor.getString(mCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            if (!phonenumber.contains("+"))

                phonenumber = phonenumber.replaceAll("[\\W_]", "").replaceFirst("^0*", "");

            else
                phonenumber = "+" + phonenumber.replaceAll("[\\W_]", "").replaceFirst("^0*", "");

            Log.d("phone number is ", phonenumber);

            if (!mPhoneList.contains(phonenumber)) {
                mPhoneList.add(phonenumber);

                mNameList.add(name);
            }


        }

        mCursor.close();

        new VerifyPhoneNumbers().execute();

        return result;
    }

    public void EnableRuntimePermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(
                DisplayContactsActivity.this,
                Manifest.permission.READ_CONTACTS)) {

            Toast.makeText(DisplayContactsActivity.this, "CONTACTS permission allows us to Access CONTACTS app", Toast.LENGTH_LONG).show();

        } else {

            ActivityCompat.requestPermissions(DisplayContactsActivity.this, new String[]{
                    Manifest.permission.READ_CONTACTS}, REQUEST_PERMISSION_CODE);

        }
    }

    @Override
    public void onRequestPermissionsResult(int RC, String per[], int[] PResult) {

        switch (RC) {

            case REQUEST_PERMISSION_CODE:

                if (PResult.length > 0 && PResult[0] == PackageManager.PERMISSION_GRANTED) {

                    createList();

                    //  Toast.makeText(DisplayContactsActivity.this,"Permission Granted, Now your application can access CONTACTS.", Toast.LENGTH_LONG).show();

                } else {

                    Toast.makeText(DisplayContactsActivity.this, "Permission Canceled, Now your application cannot access CONTACTS.", Toast.LENGTH_LONG).show();

                }
                break;
        }
    }

    public static String postObject(String completeUrl, JSONObject jsonObject) {
        DataOutputStream dataOutputStream;
        InputStream is;
        String jsonstring1 = "";

        try {
            String jsonstring = jsonObject.toString();
            URL url = new URL(completeUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setConnectTimeout(15000);
            httpURLConnection.setReadTimeout(15000);

            dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());
            dataOutputStream.write(jsonstring.getBytes());
            //Log.d("url calling in post",""+dataOutputStream);


            dataOutputStream.flush();
            dataOutputStream.close();

            int httpResult = httpURLConnection.getResponseCode();
            if (httpResult == HttpURLConnection.HTTP_OK) {
                is = new BufferedInputStream(httpURLConnection.getInputStream());
                Scanner s = new Scanner(is).useDelimiter("\\A");
                if (s.hasNext()) {
                    jsonstring1 = s.next();
                }
            }

        } catch (MalformedURLException e) {
            Log.d("error", "malformedUrl in Post");
        } catch (IOException e) {

            Log.d("error", "IOException in Post");
            return null;

        } catch (Exception e) {
            Log.d("error", "Exception in Post");
        }


        return jsonstring1;
    }

    public void onContactClick(int pos) {

        Log.d("name clicked is ", result.get(pos).name + "  token is " + mDeviceTokens.get(pos));

        ArrayList<String> tokens = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();

//        for(int i = 0; i < 1; i++)
//        {
        tokens.add(mDeviceTokens.get(pos));
        names.add(result.get(pos).name);

//        }

        Intent in = new Intent(this, OngoingCallActivity.class);

        in.putStringArrayListExtra("tokens", tokens);
        in.putStringArrayListExtra("names", names);

       // mFromAddContact = true;
        startActivity(in);


    }

    public void onContactClick(ArrayList<Integer> checkedPos) {

        if (checkedPos.size() > 4) {
            new MaterialDialog.Builder(this)
                    .title("Error")
                    .content("Please select maximum 4 participants")
                    .positiveText("OK")
                    .negativeText("Retry")
                    .show();
            return;
        }

        ArrayList<String> tokens = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();

        for (int i = 0; i < checkedPos.size(); i++) {
            tokens.add(mDeviceTokens.get(checkedPos.get(i)));
            names.add(result.get(checkedPos.get(i)).name);

        }

        Intent in = new Intent(this, OngoingCallActivity.class);

        in.putStringArrayListExtra("tokens", tokens);
        in.putStringArrayListExtra("names", names);
        if (checkedPos.size() > 1) {
            in.putExtra("multi", true);
        } else {
            in.putExtra("multi", false);
        }

       // mFromAddContact = true;
        startActivity(in);

    }

    protected class VerifyPhoneNumbers extends AsyncTask<String, Void, ArrayList<ContactInfo>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected ArrayList<ContactInfo> doInBackground(String... params) {

            String requestUrl = Constants.VERIFY_PHONE_NUMBERS_REQUEST_URL;


            JSONObject jsonObject = new JSONObject();

            try {

                jsonObject.put("phonenumbers", mPhoneList);

            } catch (JSONException j) {
                j.printStackTrace();
            }

            String response = postObject(requestUrl, jsonObject);

            if (response == null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(DisplayContactsActivity.this, "Internal error occured! Please try again!", Toast.LENGTH_SHORT).show();
                    }
                });

                return null;
            }

            Log.d(TAG, response);

            JSONObject obj = null;
            try {
                obj = new JSONObject(response);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }

// Retrieve number array from JSON object.
            JSONArray array = obj.optJSONArray("isPhoneExists");
            JSONArray tokens = obj.optJSONArray("tokens");
            JSONArray imageUrls = obj.optJSONArray("imageUrl");


            result = new ArrayList<>();

            mDeviceTokens.clear();
            result.clear();

            ArrayList<String> imgurls = new ArrayList<>();

            for (int i = 0; i < array.length(); i++) {


                //check if phone number exists on server
                if (array.optString(i).equals("y")) {
                    ContactInfo ci = new ContactInfo();
                    ci.phoneNumber = mPhoneList.get(i);
                    ci.name = mNameList.get(i);
                    mDeviceTokens.add(tokens.optString(i));

                    if ((imageUrls.optString(i).equals("noimage")) || imageUrls.optString(i).equals("")) {
                        ci.imageURL = imageUrls.optString(i);
                        result.add(ci);

                    } else {
                        ci.imageURL = IMAGE_ACCESS_URL + imageUrls.optString(i) + ".png";

                        if(!imgurls.contains(ci.imageURL)) {
                            imgurls.add(ci.imageURL);
                            result.add(ci);
                        }
                    }

                }


                Log.d("array is ", array.optString(i));
                Log.d("token is ", tokens.optString(i));

            }

            return result;

        }

        @Override
        protected void onPostExecute(ArrayList<ContactInfo> s) {
            super.onPostExecute(s);

            if (s == null) {
                new MaterialDialog.Builder(DisplayContactsActivity.this)
                        .title("Network error occured. Please try again")
                        .positiveText("Quit")
                        .show();
                mProgressDialog.cancel();
                return;
            }

            if(result.size() > 0) {
                ((RelativeLayout)findViewById(R.id.no_contacts_layout)).setVisibility(View.GONE);
                ContactAdapter ca = new ContactAdapter(s, DisplayContactsActivity.this);
                mRecList.setAdapter(ca);
            } else {
                ((RelativeLayout)findViewById(R.id.no_contacts_layout)).setVisibility(View.VISIBLE);
            }



            mProgressDialog.cancel();


        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //new contact added, refresh the list
        if (mFromAddContact) {
            createList();
        }

        mFromAddContact = false;
        //getDelegate().onStart();
    }

}
