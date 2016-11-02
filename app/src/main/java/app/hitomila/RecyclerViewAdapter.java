package app.hitomila;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.bumptech.glide.Glide;

import app.hitomila.common.hitomi.IndexData;

/**
 * Created by admin on 2016-11-01.
 */

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    Context appContext;

    //Inflate 받은 부분에서 위젯별 할당
    public class ViewHolder extends RecyclerView.ViewHolder{
        public ImageView thumbNailView;
        public TextView titleTextView;
        public TextView typeTextView;
        public TextView languageTextView;
        public CardView  cardView;

        public ViewHolder(View itemView) {
            super(itemView);

            thumbNailView = (ImageView)itemView.findViewById(R.id.viewHolderThumbnailImageView);
            titleTextView = (TextView)itemView.findViewById(R.id.viewholderTitleTextView);
            typeTextView = (TextView)itemView.findViewById(R.id.viewholderTypeTextView);
            languageTextView = (TextView)itemView.findViewById(R.id.viewholderLanguageTextView);
            cardView = (CardView)itemView.findViewById(R.id.cardview);
        }
    }

    IndexData.node[] dataSet;

    public RecyclerViewAdapter(Context mContext){
        appContext = mContext;
    }

    public void setData(IndexData dataSet){
        this.dataSet = dataSet.getDatas();
    }


    //ViewHolder의 Infalte 담당
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_main_index_viewholderitem, parent, false);
        return new ViewHolder(item);
    }

    //Listener등 기능삽입
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.titleTextView.setText(dataSet[position].title);
        holder.languageTextView.setText(dataSet[position].mangaLangugae);
        holder.typeTextView.setText(dataSet[position].type);
        Glide.with(appContext).load(dataSet[position].thumbnailUrl).override(150, 200)
                .into(holder.thumbNailView);
    }

    @Override
    public int getItemCount() {
        if(dataSet != null)
            return dataSet.length;
        else return 0;
    }
}
