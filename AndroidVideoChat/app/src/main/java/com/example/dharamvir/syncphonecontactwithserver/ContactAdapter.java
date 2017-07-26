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
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private List<ContactInfo> mContactList;
    private MainActivity mContext;
    private ArrayList<Integer> mCheckedPos = new ArrayList<>();

    public ContactAdapter(List<ContactInfo> contactList, final MainActivity context) {
        this.mContactList = contactList;
        this.mContext = context;

        context.findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.onContactClick(mCheckedPos);
            }
        });
    }


    @Override
    public int getItemCount() {
        return mContactList.size();
    }

    @Override
    public void onBindViewHolder(ContactViewHolder contactViewHolder, int i) {
        ContactInfo ci = mContactList.get(i);
        contactViewHolder.vName.setText(ci.name);


        if(ci.imageURL.contains("png"))
        { //contact image exists on server
            contactViewHolder.vProfileImage.setVisibility(View.INVISIBLE);

            Glide.with(mContext).load(ci.imageURL)
                    .thumbnail(0.5f)
                    .crossFade()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(contactViewHolder.vCircleProfileImage);

        }
        else {
            //Show circular image with first character of name
            contactViewHolder.vProfileImage.setVisibility(View.VISIBLE);
            contactViewHolder.vCircleProfileImage.setVisibility(View.INVISIBLE);

            ColorGenerator generator = ColorGenerator.MATERIAL; // or use DEFAULT
            int color = generator.getColor(ci.name + ci.name.charAt(0));
            Log.d("color is " , Integer.toString(color));

            TextDrawable drawable = TextDrawable.builder()
                    .buildRound(Character.toString(ci.name.charAt(0)), color);

            contactViewHolder.vProfileImage.setImageDrawable(drawable);
        }

        if(mContext.mSelectView.getText().equals("Select")) {
            contactViewHolder.vCheckBox.setChecked(false);
            contactViewHolder.vCheckBox.setVisibility(View.INVISIBLE);
            mCheckedPos.clear();
        }
        else {
            contactViewHolder.vCheckBox.setVisibility(View.VISIBLE);

            if(mCheckedPos.contains(i)){
                contactViewHolder.vCheckBox.setChecked(true);
            }
        }
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

        public ContactViewHolder(View v) {
            super(v);
            vName =  (TextView) v.findViewById(R.id.txtName);
            vProfileImage = (ImageView)  v.findViewById(R.id.profile_image);
            vCheckBox = (CheckBox) v.findViewById(R.id.checkBox);
            vCircleProfileImage = (ImageView)  v.findViewById(R.id.profile_image_circle);

            vCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            if(isChecked){
                mCheckedPos.add(getAdapterPosition());
            }
            else{
                if(mCheckedPos.contains(getAdapterPosition()))
                mCheckedPos.remove(new Integer(getAdapterPosition()));
            }

            if (mCheckedPos.size() > 0){
                mContext.findViewById(R.id.button2).setVisibility(View.VISIBLE);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mContext.findViewById(R.id.add_contact_below).getLayoutParams();
                params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                mContext.findViewById(R.id.add_contact_below).setLayoutParams(params);
            }
            else {
                mContext.findViewById(R.id.button2).setVisibility(View.GONE);
                                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mContext.findViewById(R.id.add_contact_below).getLayoutParams();
                                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                                // params.addRule(RelativeLayout.LEFT_OF, R.id.id_to_be_left_of);

                                mContext.findViewById(R.id.add_contact_below).setLayoutParams(params);}

                            }
                        });

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    int pos = getAdapterPosition();
                    mContext.onContactClick(pos);

                }
            });
        }
    }
}