package com.norram.bit;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.SearchView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.stfalcon.imageviewer.StfalconImageViewer;

import java.util.ArrayList;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        ImageView expand;
        boolean isExpand;

        public ViewHolder(View view) {
            super(view);
            image = view.findViewById(R.id.itemSearchImage);
            expand = view.findViewById(R.id.itemSearchButton);
            isExpand = false;
        }
    }

    private final Context context;
    private final ArrayList<InstaMedia> instaMediaList;
    private final SearchView searchView;

    public SearchAdapter(
            Context context,
            ArrayList<InstaMedia> instaMediaList,
            SearchView searchView
    ) {
        this.context = context;
        this.instaMediaList = instaMediaList;
        this.searchView = searchView;
    }

    @NonNull
    @Override
    public SearchAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_search, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchAdapter.ViewHolder holder, int position) {
        CommonData data = CommonData.getInstance();
        InstaMedia instaMedia = instaMediaList.get(position);
        holder.isExpand = instaMedia.getFlag();
        final int spanCount = 3;

        holder.image.getLayoutParams().width = data.screenWidth / spanCount;
        holder.image.getLayoutParams().height = data.screenWidth / spanCount;
        holder.image.requestLayout();

        Picasso.get()
                .load(instaMedia.getUrl())
                .resize(data.screenWidth / spanCount, data.screenWidth / spanCount)
                .centerCrop() // trim from the center
                .into(holder.image);

        if(instaMedia.getFlag()) {
            holder.expand.setImageResource(R.drawable.ic_baseline_drag_indicator_36_white);
        } else {
            holder.expand.setImageResource(R.drawable.ic_baseline_drag_indicator_36_orange);
        }

        if(instaMedia.getType().equals("CAROUSEL_ALBUM")) {
            holder.expand.setVisibility(View.VISIBLE);
            holder.expand.setOnClickListener(v -> {
                if(instaMedia.getFlag()) {
                    instaMedia.setFlag(false);
                    expandAlbum(position, instaMedia);
                } else {
                    instaMedia.setFlag(true);
                    closeAlbum(position, instaMedia);
                }
            });
        } else {
            holder.expand.setVisibility(View.INVISIBLE);
        }

        holder.image.setOnClickListener(v -> {
            //clear focus
            searchView.clearFocus();
            InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);

            ArrayList<String> list = new ArrayList<>();
            for(int i = 0; i < instaMediaList.size(); i++) {
                list.add(instaMediaList.get(i).getUrl());
            }
            // get default background color
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = context.getTheme();
            theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true);
            @ColorInt int backgroundColor = typedValue.data;

            data.chosenPermalink = instaMedia.getPermalink();
            data.chosenUrl = instaMedia.getUrl();
            new StfalconImageViewer.Builder<>(context, list, (imageView, image) ->
                    Picasso.get().load(image).into(imageView))
                    .withStartPosition(position).withBackgroundColor(backgroundColor)
                    .withOverlayView(new ImageOverlayView(context))
                    .withHiddenStatusBar(false)
                    .withImageChangeListener(newPosition -> {
                        data.chosenPermalink = instaMediaList.get(newPosition).getPermalink();
                        data.chosenUrl = instaMediaList.get(newPosition).getUrl();
                    })
                    .withTransitionFrom(holder.image)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return instaMediaList.size();
    }

    private void expandAlbum(int position, InstaMedia instaMedia) {
        for(int i = 0; i < instaMedia.getChildrenUrls().size(); i++) {
            instaMediaList.add(position + 1 + i,
                    new InstaMedia(instaMedia.getChildrenUrls().get(i),
                            instaMedia.getPermalink(),
                            "IMAGE",
                            new ArrayList<>(),
                            false)
            );
        }
        notifyItemRangeInserted(position + 1, instaMedia.getChildrenUrls().size());
        notifyItemRangeChanged(position, instaMediaList.size() - position);
    }

    private void closeAlbum(int position, InstaMedia instaMedia) {
        for(int i = 0; i < instaMedia.getChildrenUrls().size(); i++) {
            instaMediaList.remove(position + 1);
        }
        notifyItemRangeRemoved(position + 1, instaMedia.getChildrenUrls().size());
        notifyItemRangeChanged(position, instaMediaList.size() - position);
    }
}
