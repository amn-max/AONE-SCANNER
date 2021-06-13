package com.aonescan.scanner.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aonescan.scanner.R;
import com.aonescan.scanner.ScreenItem;
import com.bumptech.glide.Glide;

import java.util.List;


public class IntroViewPagerAdapter extends RecyclerView.Adapter<IntroViewPagerAdapter.IntroViewHolder> {
    List<ScreenItem> mListScreen;
    private Context context;
    private LayoutInflater layoutInflater;

    public IntroViewPagerAdapter(Context context, List<ScreenItem> mListScreen) {
        this.context = context;
        this.mListScreen = mListScreen;
    }

    @NonNull
    @Override
    public IntroViewPagerAdapter.IntroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_screen, parent, false);
        return new IntroViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IntroViewPagerAdapter.IntroViewHolder holder, int position) {
        holder.title.setText(mListScreen.get(position).getTitle());
        holder.description.setText(mListScreen.get(position).getDescription());
//        holder.imgSlide.setImageResource(mListScreen.get(position).getScreenImg());
        Glide.with(context).load(mListScreen.get(position).getScreenImg()).into(holder.imgSlide);
    }

    @Override
    public int getItemCount() {
        return mListScreen.size();
    }

    public class IntroViewHolder extends RecyclerView.ViewHolder {
        ImageView imgSlide;
        TextView title;
        TextView description;

        public IntroViewHolder(@NonNull View itemView) {
            super(itemView);
            imgSlide = itemView.findViewById(R.id.intro_image);
            title = itemView.findViewById(R.id.intro_title);
            description = itemView.findViewById(R.id.intro_description);
        }
    }
//
//    public IntroViewPagerAdapter(Context context, List<ScreenItem> mListScreen) {
//        this.context = context;
//        this.mListScreen = mListScreen;
//    }
//
//    @NonNull
//    @Override
//    public Object instantiateItem(@NonNull ViewGroup container, int position) {
//        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        View layoutScreen = inflater.inflate(R.layout.layout_screen,null);
//
//        ImageView imgSlide = layoutScreen.findViewById(R.id.intro_image);
//        TextView title = layoutScreen.findViewById(R.id.intro_title);
//        TextView description = layoutScreen.findViewById(R.id.intro_description);
//
//        title.setText(mListScreen.get(position).getTitle());
//        description.setText(mListScreen.get(position).getDescription());
//        imgSlide.setImageResource(mListScreen.get(position).getScreenImg());
//
//        container.addView(layoutScreen);
//
//        return layoutScreen;
//    }
//
//    @Override
//    public int getCount() {
//        return mListScreen.size();
//    }
//
//    @Override
//    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
//        return view==object;
//    }
//
//    @Override
//    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
//        container.removeView((View)object);
//    }
}
