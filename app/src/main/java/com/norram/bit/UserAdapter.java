package com.norram.bit;


import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.cardview.widget.CardView;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.AccessToken;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder>{

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        ImageView image;
        TextView text;
        LinearLayout liner;
        ImageView favorite;

        public ViewHolder(View view) {
            super(view);

            card = view.findViewById(R.id.itemUserCard);
            image = view.findViewById(R.id.itemUserImage);
            text = view.findViewById(R.id.itemUserText);
            liner = view.findViewById(R.id.itemUserLinear);
            favorite = view.findViewById(R.id.itemFavoriteImage);
        }
    }

    private final FragmentActivity activity;
    private final ArrayList<HashMap<String, String>> userList;
    private final String tableName;
    private final int split = 4;

    public UserAdapter(
            FragmentActivity activity,
            ArrayList<HashMap<String, String>> userList,
            String tableName
    ) {
        this.activity = activity;
        this.userList = userList;
        this.tableName = tableName;
    }

    @NonNull
    @Override
    public UserAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_user, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserAdapter.ViewHolder holder, int position) {
        CommonData data = CommonData.getInstance();
        HashMap<String, String> user = userList.get(position);

        holder.image.getLayoutParams().width = data.screenWidth / split;
        holder.image.getLayoutParams().height = data.screenWidth / split;
        holder.image.requestLayout();

        holder.card.setRadius((data.screenWidth / ((float) split * 2)));
        Picasso.get()
                .load(user.get("url"))
                .resize(data.screenWidth / split, data.screenWidth / split)
                .centerCrop() // trim from the center
                .into(holder.image, new Callback() {
                    @Override
                    public void onSuccess() {}

                    @Override
                    public void onError(Exception e) {
                        updateUrl(user.get("name"), holder.image);
                    }
                }
        );
        holder.text.setText(user.get("name"));
        if(Objects.equals(user.get("isFavorite"), "true")) holder.favorite.setVisibility(View.VISIBLE);

        holder.liner.setOnClickListener(v -> {
            if(user.get("name") != null) {
                ModeFragmentDirections.ActionModeFragmentToSearchFragment action = ModeFragmentDirections.actionModeFragmentToSearchFragment(user.get("name"));
                Navigation.findNavController(v).navigate(action);
            }
        });
        holder.liner.setOnLongClickListener(v -> {
            DeleteDialogFragment dialogFragment = new DeleteDialogFragment(this, userList, position);
            if(activity != null) {
                if(!tableName.equals("FAVORITE_TABLE")) dialogFragment.show(activity.getSupportFragmentManager(),  "delete_dialog");
            }
            return true; // choose whether to interfere with setOnClickListener
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    private void updateUrl(String username, ImageView imageView) {
        Context context;
        if(activity != null) {
            context = activity.getBaseContext();
        } else {
            return;
        }

        String requestUrl;
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        String requestUrlFormatter = CommonData.requestUrlFormatter();
        if(accessToken != null && !accessToken.isExpired()) {
            SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
            requestUrl = String.format(requestUrlFormatter,
                    sharedPref.getString(activity.getString(R.string.iba_id), ""),
                    username,
                    "",
                    accessToken.getToken()
            );
        } else {
            CommonData data = CommonData.getInstance();
            requestUrl = String.format(requestUrlFormatter,
                    data.BUSINESS_ACCOUNT_ID,
                    username,
                    "",
                    data.ACCESS_TOKEN
            );
        }

        try {
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);

            ConnectivityManager connectivityService = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(connectivityService.getNetworkCapabilities(connectivityService.getActiveNetwork()) != null) asyncExecute(connection, username, imageView, context);
            } else {
                if(connectivityService.getActiveNetworkInfo() == null) asyncExecute(connection, username, imageView, context);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @UiThread
    public void asyncExecute(HttpURLConnection connection, String username, ImageView imageView, Context context) {
        Looper mainLooper = Looper.getMainLooper();
        Handler handler = HandlerCompat.createAsync(mainLooper);
        UserAdapter.BackgroundTask backgroundTask = new BackgroundTask(handler, connection, username, imageView, context);
        ExecutorService executorService  = Executors.newSingleThreadExecutor();
        executorService.submit(backgroundTask);
    }

    private class BackgroundTask implements Runnable {
        private final Handler handler;
        private final HttpURLConnection connection;
        private final String username;
        private final ImageView imageView;
        private final Context context;

        public BackgroundTask(Handler handler, HttpURLConnection connection, String username, ImageView imageView, Context context) {
            this.handler = handler;
            this.connection = connection;
            this.username = username;
            this.imageView = imageView;
            this.context = context;
        }

        @WorkerThread
        @Override
        public void run() {
            if(connection.getErrorStream() != null) return;
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder jsonText = new StringBuilder();
                String readText = bufferedReader.readLine();
                while (readText != null) {
                    jsonText.append(readText);
                    readText = bufferedReader.readLine();
                }
                JSONObject jsonObj = new JSONObject(jsonText.toString());
                JSONObject bdJSON = jsonObj.getJSONObject("business_discovery");
                String iconUrl;
                if(bdJSON.has("profile_picture_url")) {
                    iconUrl = bdJSON.getString("profile_picture_url");
                } else {
                    iconUrl = "";
                }

                SQLiteDatabase db;
                switch (tableName) {
                    case "HISTORY_TABLE":
                        HistoryOpenHelper helperHis = new HistoryOpenHelper(context);
                        db = helperHis.getWritableDatabase();
                        db.execSQL("UPDATE HISTORY_TABLE SET url = ? WHERE name = ?",
                                new Object[]{iconUrl, username});
                        break;
                    case "FAVORITE_TABLE":
                        FavoriteOpenHelper helperFav = new FavoriteOpenHelper(context);
                        db = helperFav.getWritableDatabase();
                        db.execSQL("UPDATE FAVORITE_TABLE SET url = ? WHERE name = ?",
                                new Object[]{iconUrl, username});
                        break;
                }
                UserAdapter.PostExecutor postExecutor = new PostExecutor(iconUrl, imageView);
                handler.post(postExecutor);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class  PostExecutor implements Runnable {
        String iconUrl;
        ImageView imageView;

        public PostExecutor(String iconUrl, ImageView imageView) {
            this.iconUrl = iconUrl;
            this.imageView = imageView;
        }

        @Override
        public void run() {
            CommonData data = CommonData.getInstance();
            Picasso.get()
                    .load(iconUrl)
                    .resize(data.screenWidth / split, data.screenWidth / split)
                    .centerCrop() // trim from the center
                    .into(imageView);
        }
    }
}
