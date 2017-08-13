package com.example.dharamvir.syncphonecontactwithserver;

import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private OngoingCallActivity context;
    LayoutInflater inflater;

    List<SignalMessage> data;

    public MessageAdapter(List<SignalMessage> data, OngoingCallActivity context) {

        this.context = context;
        inflater = LayoutInflater.from(context);
        this.data = data;
    }


    @Override
    public int getItemViewType(int position) {

       return data.get(position).getType();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        RecyclerView.ViewHolder viewHolder;

        switch (viewType) {

            case Constants.TYPE_REMOTE_TEXT:
                View v1 = inflater.inflate(R.layout.remote_text_item, viewGroup, false);
                viewHolder = new TextViewHolder(v1);
                break;

            case Constants.TYPE_REMOTE_IMAGE:
                View v2 = inflater.inflate(R.layout.remote_image_item, viewGroup, false);
                viewHolder = new ImageViewHolder(v2);
                break;

            case Constants.TYPE_SELF_TEXT:
                View v3 = inflater.inflate(R.layout.self_text_item, viewGroup, false);
                viewHolder = new TextViewHolder(v3);
                break;

            case Constants.TYPE_SELF_IMAGE:
                View v4 = inflater.inflate(R.layout.self_image_item, viewGroup, false);
                viewHolder = new ImageViewHolder(v4);
                break;

            default:
                viewHolder = null;
                break;

        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {



        if(viewHolder.getItemViewType() == Constants.TYPE_REMOTE_IMAGE || viewHolder.getItemViewType() == Constants.TYPE_SELF_IMAGE)
        {
            ImageViewHolder vh1 = (ImageViewHolder) viewHolder;
            configureImageViewHolder(vh1, position);
        }

        else if(viewHolder.getItemViewType() == Constants.TYPE_SELF_TEXT || viewHolder.getItemViewType() == Constants.TYPE_REMOTE_TEXT)
        {
            TextViewHolder vh1 = (TextViewHolder) viewHolder;
            configureTextViewHolder(vh1, position);
            //configureTextViewHolder(vh1, position);
        }

    }


    public class ImageViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;

        public ImageViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView)itemView.findViewById(R.id.imageView);
        }
    }

    public class TextViewHolder extends RecyclerView.ViewHolder {

        TextView messageText;
        TextView name;

        public TextViewHolder(View itemView) {
            super(itemView);
            messageText = (TextView)itemView.findViewById(R.id.textView);
            name = (TextView)itemView.findViewById(R.id.remote_name);
        }
    }

    private void configureImageViewHolder(ImageViewHolder vh1, int position) {
        byte[] b = Base64.decode(data.get(position).getData() , Base64.DEFAULT);
        vh1.imageView.setImageBitmap(BitmapFactory.decodeByteArray(b, 0, b.length));

       /* Glide.with(context).load(BitmapFactory.decodeByteArray(b, 0, b.length))
                .thumbnail(0.5f)
                .crossFade()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(vh1.imageView)
        ;*/

    }

    private void configureTextViewHolder(TextViewHolder vh2, int position) {

        Log.d("MessageAdapter", "message is " + data.get(position).getData());

       if(data.get(position).getType() == Constants.TYPE_REMOTE_TEXT) {
           ColorGenerator generator = ColorGenerator.MATERIAL; // or use DEFAULT
           int color = generator.getColor(data.get(position).getCode());

           vh2.name.setText(data.get(position).getName());
           vh2.name.setTextColor(color);
       }
        vh2.messageText.setText(data.get(position).getData());

    }

    @Override
    public int getItemCount() {

        return data.size();
    }
}