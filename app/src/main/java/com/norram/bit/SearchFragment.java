package com.norram.bit;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.core.os.HandlerCompat;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.norram.bit.databinding.FragmentSearchBinding;
import com.squareup.picasso.Picasso;
import com.stfalcon.imageviewer.StfalconImageViewer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SearchFragment extends Fragment {
    private FragmentSearchBinding binding;
    private final int spanCount = 3;
    private final String requestUrlFormatter = CommonData.requestUrlFormatter();
    private String username = "";
    private String usernameTmp = "";
    private String iconUrl = "";
    private String name = "";
    private String afterToken = "";
    private ArrayList<InstaMedia> instaMediaList = new ArrayList<>();
    private boolean favoriteFlag = false;

    private View.OnClickListener listener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_search, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // prevent UI error
        binding.profileConstraint.setVisibility(View.GONE);
        binding.recyclerView.setVisibility(View.GONE);

        username = SearchFragmentArgs.fromBundle(getArguments()).getUsername();

        binding.searchView.setQuery(username, false);
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if(query != null) {
                    username = query;
                    resetData();
                    getMediaInfo();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(newText != null) usernameTmp = newText;
                return false;
            }
        });

        binding.searchButton.setOnClickListener(v -> {
            //clear focus
            binding.searchView.clearFocus();
            InputMethodManager inputMethodManager = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);

            if(!usernameTmp.equals("")) {
                username = usernameTmp;
                resetData();
                getMediaInfo();
            }
        });

        CommonData data = CommonData.getInstance();
        binding.searchCard.setRadius(data.screenWidth / ((float) spanCount * 2));
        binding.iconImageView.getLayoutParams().width = data.screenWidth / spanCount;
        binding.iconImageView.getLayoutParams().height = data.screenWidth / spanCount;
        binding.iconImageView.requestLayout();

        binding.favoriteImageView.setOnClickListener(v -> {
            FavoriteOpenHelper helper = new FavoriteOpenHelper(requireContext());
            SQLiteDatabase db = helper.getWritableDatabase();
            if(favoriteFlag) {
                db.execSQL("DELETE FROM FAVORITE_TABLE WHERE name = ?",
                        new String[]{username});
                binding.favoriteImageView.setImageResource(R.drawable.ic_baseline_favorite_border_24);
                favoriteFlag = false;
            } else {
                db.execSQL("INSERT INTO FAVORITE_TABLE(url, name) VALUES(?, ?)",
                        new Object[]{iconUrl, username});
                binding.favoriteImageView.setImageResource(R.drawable.ic_baseline_favorite_pink_24);

                Animation animation = AnimationUtils.loadAnimation(requireContext(), R.anim.touch_favorite);
                v.startAnimation(animation);
                favoriteFlag = true;
            }
        });

        binding.recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), spanCount, RecyclerView.VERTICAL, false));

        listener = v -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.addButton.setVisibility(View.INVISIBLE);
        };
        binding.addButton.setOnClickListener(listener);

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.options_menu, menu);
            }

            @SuppressWarnings("unchecked")
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if(menuItem.getItemId() == R.id.expandAll) {
                    RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = binding.recyclerView.getAdapter();
                    if(adapter != null) {
                        for(int i = adapter.getItemCount(); i >= 0; i--) {
                            SearchAdapter.ViewHolder holder = (SearchAdapter.ViewHolder) binding.recyclerView.findViewHolderForAdapterPosition(i);
                            if(holder != null && holder.isExpand) holder.expand.performClick();
                        }
                    }
                }
                return true;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        getMediaInfo();
    }

    private void resetData() {
        afterToken = "";
        instaMediaList = new ArrayList<>();
    }

    private void getMediaInfo() {
        // cut URL
        String prefix = "instagram.com/";
        if(username.contains(prefix)) {
            int preIdx = username.indexOf(prefix) + prefix.length();
            username = username.substring(preIdx, username.length()-1);
            int sufIdx = username.indexOf("?");
            if(sufIdx == -1) sufIdx = username.indexOf("/");
            if(sufIdx != -1) username = username.substring(0, sufIdx);
        }

        String requestUrl;
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if(accessToken != null && !accessToken.isExpired()) {
            SharedPreferences sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE);
            requestUrl = String.format(requestUrlFormatter,
                    sharedPref.getString(getString(R.string.iba_id), ""),
                    username,
                    afterToken,
                    accessToken.getToken()
            );
        } else {
            CommonData data = CommonData.getInstance();
            requestUrl = String.format(requestUrlFormatter,
                    data.BUSINESS_ACCOUNT_ID,
                    username,
                    afterToken,
                    data.ACCESS_TOKEN
            );
        }

        try {
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);

            ConnectivityManager connectivityService = (ConnectivityManager) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(connectivityService.getNetworkCapabilities(connectivityService.getActiveNetwork()) == null) {
                    Toast.makeText(requireContext(), getResources().getString(R.string.error0), Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(View.INVISIBLE);
                } else {
                    asyncExecute(connection);
                }
            } else {
                if(connectivityService.getActiveNetworkInfo() == null) {
                    Toast.makeText(requireContext(), getResources().getString(R.string.error0), Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(View.INVISIBLE);
                } else {
                    asyncExecute(connection);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @UiThread
    public void asyncExecute(HttpURLConnection connection) {
        Looper mainLooper = Looper.getMainLooper();
        Handler handler = HandlerCompat.createAsync(mainLooper);
        BackgroundTask backgroundTask = new BackgroundTask(handler, connection);
        ExecutorService executorService  = Executors.newSingleThreadExecutor();
        executorService.submit(backgroundTask);
    }

    private class BackgroundTask implements Runnable {
        private final Handler handler;
        private final HttpURLConnection connection;

        public BackgroundTask(Handler handler, HttpURLConnection connection) {
            this.handler = handler;
            this.connection = connection;
        }

        @WorkerThread
        @Override
        public void run() {
            Context context = requireContext();
            try {
                if(connection.getErrorStream() != null) {
                    PostExecutor postExecutor = new PostExecutor(context, false);
                    handler.post(postExecutor);
                    return;
                }

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder jsonText = new StringBuilder();
                String readText = bufferedReader.readLine();
                while (readText != null) {
                    jsonText.append(readText);
                    readText = bufferedReader.readLine();
                }
                JSONObject jsonObj = new JSONObject(jsonText.toString());
                JSONObject bdJSON = jsonObj.getJSONObject("business_discovery");
                if(bdJSON.has("profile_picture_url")) {
                    iconUrl = bdJSON.getString("profile_picture_url");
                } else {
                    iconUrl = "";
                }
                if(bdJSON.has("name")) {
                    name = bdJSON.getString("name");
                } else {
                    name = "";
                }
                JSONObject mediaJSON = bdJSON.getJSONObject("media");
                JSONArray mediaArray = mediaJSON.getJSONArray("data");

                for(int i = 0; i < mediaArray.length(); i++) {
                    String permalink = "";
                    JSONObject mediaData = mediaArray.getJSONObject(i);
                    ArrayList<String> childrenUrls = new ArrayList<>();
                    if(mediaData.has("permalink")) permalink = mediaData.getString("permalink");
                    if (mediaData.getString("media_type").equals("CAROUSEL_ALBUM") && mediaData.has("children")) {
                        JSONArray childrenDataArray = mediaData.getJSONObject("children").getJSONArray("data");
                        for(int j = 1; j < childrenDataArray.length(); j++) {
                            JSONObject childrenData = childrenDataArray.getJSONObject(j);
                            if (childrenData.getString("media_type").equals("IMAGE"))
                                childrenUrls.add(childrenData.getString("media_url"));
                        }
                    }
                    if (!mediaData.getString("media_type").equals("VIDEO")
                            && mediaData.has("media_url")
                            && mediaData.has("media_type")) {
                        instaMediaList.add(
                                new InstaMedia(
                                        mediaData.getString("media_url"),
                                        permalink,
                                        mediaData.getString("media_type"),
                                        childrenUrls,
                                        true
                                )
                        );
                    }
                }

                HistoryOpenHelper helper = new HistoryOpenHelper(context);
                SQLiteDatabase db = helper.getWritableDatabase();
                db.execSQL("INSERT INTO HISTORY_TABLE(url, name) VALUES(?, ?)",
                        new Object[]{iconUrl, username});

                binding.addButton.setOnClickListener(v -> {
                    listener.onClick(v);
                    try {
                        if(mediaJSON.has("paging")) {
                            JSONObject cursorsJSON = mediaJSON.getJSONObject("paging").getJSONObject("cursors");
                            if(cursorsJSON.has("after")) {
                                afterToken = ".after(" + cursorsJSON.getString("after") + ")";
                                getMediaInfo();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });

                PostExecutor postExecutor = new PostExecutor(context, true);
                handler.post(postExecutor);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class PostExecutor implements Runnable {
        Context context;
        boolean isNormal;

        public PostExecutor(Context context, boolean isNormal) {
            this.context = context;
            this.isNormal = isNormal;
        }

        @UiThread
        @Override
        // method in UI thread when finishing background thread
        public void run() {
            binding.progressBar.setVisibility(View.INVISIBLE);

            if(!isNormal) {
                Toast.makeText(context, getResources().getString(R.string.error1), Toast.LENGTH_SHORT).show();
                return;
            }

            CommonData data = CommonData.getInstance();
            if (afterToken.equals("")) {
                binding.nestedScrollView.fullScroll(ScrollView.FOCUS_UP); // return to the top
                binding.usernameText.setText(name);
                if(!iconUrl.equals("")) {
                    Picasso.get()
                            .load(iconUrl)
                            .resize(data.screenWidth / spanCount, data.screenWidth / spanCount)
                            .centerCrop() // trim from the center
                            .into(binding.iconImageView);

                    binding.iconImageView.setOnClickListener(v ->
                            new StfalconImageViewer.Builder<>(context, new ArrayList<>(Collections.singletonList(iconUrl)), (imageView, image) ->
                                    Picasso.get().load(image).into(imageView))
                                    .withTransitionFrom(binding.iconImageView)
                                    .show());
                }
            }

            binding.recyclerView.setAdapter(new SearchAdapter(context, instaMediaList, binding.searchView));
            checkFavorite(context);

            // prevent UI error
            binding.profileConstraint.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.VISIBLE);

            binding.addButton.setVisibility(View.VISIBLE);
        }

        private void checkFavorite(Context context) {
            FavoriteOpenHelper helper = new FavoriteOpenHelper(context);
            SQLiteDatabase db = helper.getWritableDatabase();
            Cursor c = db.rawQuery("SELECT name FROM FAVORITE_TABLE ORDER BY id DESC", null);
            boolean next = c.moveToFirst();
            while (next) {
                String name = c.getString(0);
                if(name.equals(username)) {
                    binding.favoriteImageView.setImageResource(R.drawable.ic_baseline_favorite_pink_24);
                    favoriteFlag = true;
                    return;
                }
                next = c.moveToNext();
            }
            c.close();

            binding.favoriteImageView.setImageResource(R.drawable.ic_baseline_favorite_border_24);
            favoriteFlag = false;
        }
    }
}