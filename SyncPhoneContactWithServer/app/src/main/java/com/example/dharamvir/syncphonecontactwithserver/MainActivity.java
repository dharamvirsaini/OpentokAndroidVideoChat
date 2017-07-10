package com.example.dharamvir.syncphonecontactwithserver;

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
import android.view.Menu;
import android.view.MenuItem;
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
import java.util.Random;
import java.util.Scanner;


public class MainActivity extends AppCompatActivity {

    Cursor cursor;
    RecyclerView recList;

    public  static final int RequestPermissionCode  = 1 ;
    List<String> phoneList;
    List<String> nameList;
    ArrayList<String> deviceTokens = new ArrayList<>();
    MaterialDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent in = getIntent();

        String code = in.getStringExtra("code");
        String phone = in.getStringExtra("phone");

        progressDialog = new MaterialDialog.Builder(MainActivity.this)
                .cancelable(false)
                .progress(true,100)
                .content("Please wait Sonal...")
                .build();

        progressDialog.show();

        Log.d("MainActivity", "phone is " + phone + " and code is " + code);

        recList = (RecyclerView) findViewById(R.id.cardList);
        recList.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recList.setLayoutManager(llm);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            EnableRuntimePermission();
        }
        else
        {
            createList();
        }






    }


    private List<ContactInfo> createList() {

        List<ContactInfo> result = new ArrayList<ContactInfo>();

        phoneList = new ArrayList<>();
        nameList = new ArrayList<>();

        cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null, null, null);

        while (cursor.moveToNext()) {

           // ContactInfo ci = new ContactInfo();

           String  name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));

            String phonenumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            phonenumber = phonenumber.replaceAll("\\s+","").replaceFirst("^0*", "");

            phoneList.add(phonenumber);
            nameList.add(name);






        }

        cursor.close();

        new GetLogoDetails().execute();

        return result;
    }

    public void EnableRuntimePermission(){

        if (ActivityCompat.shouldShowRequestPermissionRationale(
                MainActivity.this,
                Manifest.permission.READ_CONTACTS))
        {

            Toast.makeText(MainActivity.this,"CONTACTS permission allows us to Access CONTACTS app", Toast.LENGTH_LONG).show();

        } else {

            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    Manifest.permission.READ_CONTACTS}, RequestPermissionCode);

        }
    }

    @Override
    public void onRequestPermissionsResult(int RC, String per[], int[] PResult) {

        switch (RC) {

            case RequestPermissionCode:

                if (PResult.length > 0 && PResult[0] == PackageManager.PERMISSION_GRANTED) {

                    createList();

                    Toast.makeText(MainActivity.this,"Permission Granted, Now your application can access CONTACTS.", Toast.LENGTH_LONG).show();

                } else {

                    Toast.makeText(MainActivity.this,"Permission Canceled, Now your application cannot access CONTACTS.", Toast.LENGTH_LONG).show();

                }
                break;
        }
    }

    private boolean isExist(String phonenumber) {
        return true;
    }

    public static String postObject(String completeUrl,JSONObject jsonObject)
    {
        DataOutputStream dataOutputStream;
        InputStream is;
        String jsonstring1 ="";

        try{
            String jsonstring = jsonObject.toString();
            URL url = new URL(completeUrl);
            HttpURLConnection httpURLConnection=(HttpURLConnection)url.openConnection();
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
            if(httpResult==HttpURLConnection.HTTP_OK) {
                is = new BufferedInputStream(httpURLConnection.getInputStream());
                Scanner s = new Scanner(is).useDelimiter("\\A");
                if (s.hasNext()) {
                    jsonstring1 = s.next();
                }
            }

        }catch(MalformedURLException e){
            Log.d("error","malformedUrl in Post");
        }catch (IOException e){
            Log.d("error","IOException in Post");
        }catch(Exception e){
            Log.d("error", "Exception in Post");
        }


        return jsonstring1;
    }

    protected class GetLogoDetails extends AsyncTask<String,Void,ArrayList<ContactInfo>> {




        public GetLogoDetails() {


        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected ArrayList<ContactInfo> doInBackground(String... params) {

            String requestUrl = "http://www.contactsyncer.com/verifyphonenumbers.php";



            JSONObject jsonObject = new JSONObject();

            try {

                jsonObject.put("phonenumbers", phoneList);

            } catch (JSONException j) {
                j.printStackTrace();
            }

            String response = postObject(requestUrl, jsonObject);

            if (response == null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Internal error occured! Please try again!", Toast.LENGTH_SHORT).show();
                    }
                });

                return null;
            }

            Log.e("response is", "" + response);

            JSONObject obj = null;
            try {
                obj = new JSONObject(response);
            } catch (JSONException e) {
                e.printStackTrace();
            }

// Retrieve number array from JSON object.
            JSONArray array = obj.optJSONArray("isPhoneExists");
            JSONArray tokens = obj.optJSONArray("tokens");

            ArrayList<ContactInfo> result = new ArrayList<>();

            for(int i = 0; i < array.length(); i++)
            {


                //check if phone number exists on server
                if(array.optString(i).equals("y"))
                {
                    ContactInfo ci = new ContactInfo();
                    ci.phoneNumber = phoneList.get(i);
                    ci.name = nameList.get(i);

                    deviceTokens.add(tokens.optString(i));

                    result.add(ci);

                }


                Log.d("array is " , array.optString(i));
                Log.d("token is ", tokens.optString(i));

            }

            return result;

        }

        @Override
        protected void onPostExecute(ArrayList<ContactInfo> s) {
            super.onPostExecute(s);

            ContactAdapter ca = new ContactAdapter(s);
            recList.setAdapter(ca);

            progressDialog.cancel();


        }
    }

}
