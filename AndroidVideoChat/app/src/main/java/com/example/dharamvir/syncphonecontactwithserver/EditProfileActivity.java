package com.example.dharamvir.syncphonecontactwithserver;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class EditProfileActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageView mProfileImage, mOkImage;
    private final int PICK_IMAGE_REQUEST = 574;
    private EditText mNameText;
    private String name;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_info);

        mProfileImage = (ImageView)findViewById(R.id.imageView);
        mOkImage = (ImageView) findViewById(R.id.imageView2);

        mNameText = (EditText)findViewById(R.id.editText2);

        if(getSharedPreferences(PhoneAuthActivity.MyPREFERENCES, MODE_PRIVATE).getString("image_data", "noimage") != "noimage")
        {
            byte[] b = Base64.decode(getSharedPreferences(PhoneAuthActivity.MyPREFERENCES, MODE_PRIVATE).getString("image_data", "noimage"), Base64.DEFAULT);
            bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
            mProfileImage.setImageBitmap(bitmap);

            mNameText.setText(getSharedPreferences(PhoneAuthActivity.MyPREFERENCES, MODE_PRIVATE).getString("name", "Unknown"));
        }
        
        mProfileImage.setOnClickListener(this);
        mOkImage.setOnClickListener(this);

    }

    private void updateProfileInfo() {

    if(mNameText.getText().toString().trim().equals("")){

        final Snackbar snackbar = Snackbar.make(findViewById(R.id.profile_info_container),"Please enter your name...",Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("OK", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackbar.dismiss();
            }
        });
        snackbar.show();
        return;
    }
    else{

        name = mNameText.getText().toString().trim();
        Log.d("name is " , name);
        new UpdateInfoOnServer().execute();
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

    private void showFileChooser() {
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
                int nh = (int) ( bitmap2.getHeight() * (96.0 / bitmap2.getWidth()) );
                bitmap = Bitmap.createScaledBitmap(bitmap2, 96, nh, true);

                mProfileImage.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClick(View v) {

        switch(v.getId())
        {
            case R.id.imageView:
            {
                showFileChooser();
                break;
            }

            case R.id.imageView2:
            {
                updateProfileInfo();
            }
        }

    }

    protected class UpdateInfoOnServer extends AsyncTask<String,Void,String> {

    MaterialDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new MaterialDialog.Builder(EditProfileActivity.this)
                    .content("Please wait...")
                    .progress(true,100)
                    .build();

            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {

            String requestUrl = Constants.UPDATE_PROFILE_REQUEST_URL;

            JSONObject jsonObject = new JSONObject();

            try {

                jsonObject.put("name", getSharedPreferences(PhoneAuthActivity.MyPREFERENCES, MODE_PRIVATE).getString("phone", null));
                jsonObject.put("profileName", name);
                jsonObject.put("image", getStringImage(bitmap));

            } catch (JSONException j) {
                j.printStackTrace();
            }

            String response = MainActivity.postObject(requestUrl, jsonObject);

            if (response == null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(EditProfileActivity.this, "Internal error occured! Please try again!", Toast.LENGTH_SHORT).show();
                    }
                });

                return null;
            }

            Log.e("response is", "" + response);
            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);

            progressDialog.cancel();

            if(response != null && response.equals("success"))
            {
                SharedPreferences.Editor editor = getSharedPreferences(PhoneAuthActivity.MyPREFERENCES, MODE_PRIVATE).edit();
                editor.putString("name", name);

                if(getStringImage(bitmap) != "noimage") {

                    editor.putString("image_data", getStringImage(bitmap));
                }

                editor.commit();

                MainActivity.sActivityContext.finish();

                Intent in = new Intent(EditProfileActivity.this, MainActivity.class);
                in.putExtra("code", getSharedPreferences(PhoneAuthActivity.MyPREFERENCES, MODE_PRIVATE).getString("code", null));
                in.putExtra("phone", getSharedPreferences(PhoneAuthActivity.MyPREFERENCES, MODE_PRIVATE).getString("phone", null));

                startActivity(in);

                EditProfileActivity.this.finish();
            }
        }
    }

}