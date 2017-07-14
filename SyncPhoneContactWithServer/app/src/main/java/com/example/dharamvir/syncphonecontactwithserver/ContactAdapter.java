/*
 * Copyright (C) 2014 Francesco Azzola
 *  Surviving with Android (http://www.survivingwithandroid.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.example.dharamvir.syncphonecontactwithserver;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private List<ContactInfo> contactList;
    MainActivity context;
    ArrayList<Integer> checkedPos = new ArrayList<>();

    public ContactAdapter(List<ContactInfo> contactList, final MainActivity context) {
        this.contactList = contactList;
        this.context = context;

        context.findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.onContactClick(checkedPos);
            }
        });
    }


    @Override
    public int getItemCount() {
        return contactList.size();
    }

    @Override
    public void onBindViewHolder(ContactViewHolder contactViewHolder, int i) {
        ContactInfo ci = contactList.get(i);
        contactViewHolder.vName.setText(ci.name);

        if(ci.imageURL.contains("png"))
        {
            contactViewHolder.vProfileImage.setVisibility(View.INVISIBLE);
            contactViewHolder.vCircleProfileImage.setVisibility(View.INVISIBLE);

            new BitmapWorkerTask(contactViewHolder.vCircleProfileImage).execute(ci.imageURL);
        }
        else {
            contactViewHolder.vProfileImage.setVisibility(View.VISIBLE);
            contactViewHolder.vCircleProfileImage.setVisibility(View.INVISIBLE);

            ColorGenerator generator = ColorGenerator.MATERIAL; // or use DEFAULT

            int color = generator.getColor(ci.name);

            TextDrawable drawable = TextDrawable.builder()
                    .buildRound(Character.toString(ci.name.charAt(0)), color);

            contactViewHolder.vProfileImage.setImageDrawable(drawable);
        }

        if(context.selectView.getText().equals("Select")) {
            contactViewHolder.vCheckBox.setChecked(false);
            contactViewHolder.vCheckBox.setVisibility(View.INVISIBLE);
            checkedPos.clear();
        }
        else {
            contactViewHolder.vCheckBox.setVisibility(View.VISIBLE);
            if(checkedPos.contains(i))
            {
                contactViewHolder.vCheckBox.setChecked(true);
            }
            //checkedPos = new ArrayList<>();
        }


        //contactViewHolder.vSurname.setText(ci.surname);
        //contactViewHolder.vEmail.setText(ci.email);
        //contactViewHolder.vTitle.setText(ci.name + " " + ci.surname);
    }

    @Override
    public ContactViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.
                    from(viewGroup.getContext()).
                    inflate(R.layout.card_layout, viewGroup, false);



            return new ContactViewHolder(itemView);
    }

    public class ContactViewHolder extends RecyclerView.ViewHolder {

        protected TextView vName;
        protected ImageView vProfileImage;
        protected CheckBox vCheckBox;
        protected ImageView vCircleProfileImage;
       // protected TextView vEmail;
       // protected TextView vTitle;

        public ContactViewHolder(View v) {
            super(v);
            vName =  (TextView) v.findViewById(R.id.txtName);
            vProfileImage = (ImageView)  v.findViewById(R.id.profile_image);
            vCheckBox = (CheckBox) v.findViewById(R.id.checkBox);
            vCircleProfileImage = (ImageView)  v.findViewById(R.id.profile_image_circle);

            vCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    Log.d("Contact Adapter", Boolean.toString(isChecked) + "-----" + Integer.toString(getAdapterPosition()));
if(isChecked)
{
    checkedPos.add(getAdapterPosition());
}
else
{
    if(checkedPos.contains(getAdapterPosition()))
    checkedPos.remove(new Integer(getAdapterPosition()));
}

if(checkedPos.size() > 0)
{
    context.findViewById(R.id.button2).setVisibility(View.VISIBLE);
}
else
    context.findViewById(R.id.button2).setVisibility(View.GONE);

                }
            });

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    int pos = getAdapterPosition();

                    context.onContactClick(pos);

                }
            });
//            vEmail = (TextView)  v.findViewById(R.id.txtEmail);
//            vTitle = (TextView) v.findViewById(R.id.title);
        }
    }
}

class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
    private final WeakReference<ImageView> imageViewReference;
    private String imageUrl;

    public BitmapWorkerTask(ImageView imageView) {
        // Use a WeakReference to ensure the ImageView can be garbage
        // collected
        imageViewReference = new WeakReference<ImageView>(imageView);
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(String... params) {
        imageUrl = params[0];
        return LoadImage(imageUrl);
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (imageViewReference != null && bitmap != null) {
            final ImageView imageView = imageViewReference.get();
            if (imageView != null) {
                imageView.setImageBitmap(bitmap);
                imageView.setVisibility(View.VISIBLE);
            }
        }
    }

    private Bitmap LoadImage(String URL) {
        Bitmap bitmap = null;
        InputStream in = null;
        try {
            in = OpenHttpConnection(URL);
            bitmap = BitmapFactory.decodeStream(in);
            in.close();
        } catch (IOException e1) {
        }
        return bitmap;
    }

    private InputStream OpenHttpConnection(String strURL)
            throws IOException {
        InputStream inputStream = null;
        URL url = new URL(strURL);
        URLConnection conn = url.openConnection();

        try {
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            httpConn.setRequestMethod("GET");
            httpConn.connect();

            if (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                inputStream = httpConn.getInputStream();
            }
        } catch (Exception ex) {
        }
        return inputStream;
    }
}
