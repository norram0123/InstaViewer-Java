package com.norram.bit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.json.JSONException;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.norram.bit.databinding.ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        setSupportActionBar(binding.toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.navHostFragment);
        assert navHostFragment != null;
        NavController navController = navHostFragment.getNavController();
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration);

        callbackManager = CallbackManager.Factory.create();
        binding.fbButton.setPermissions(Arrays.asList(
                "pages_show_list",
                "instagram_basic",
                "instagram_manage_insights"
        ));

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        AccessToken accessToken = loginResult.getAccessToken();
                        GraphRequest requestFirst = GraphRequest.newGraphPathRequest(
                                accessToken,
                                "me/accounts",
                                responseFirst -> {
                                    try {
                                        assert responseFirst.getJSONObject() != null;
                                        String pageId = responseFirst.getJSONObject().getJSONArray("data").getJSONObject(0).getString("id");
                                        GraphRequest requestSecond = GraphRequest.newGraphPathRequest(
                                                accessToken,
                                                "/" + pageId,
                                                responseSecond -> {
                                                    try {
                                                        assert responseSecond.getJSONObject() != null;
                                                        String instagramBusinessAccount = responseSecond.getJSONObject().getJSONObject("instagram_business_account").getString("id");
                                                        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                                                        SharedPreferences.Editor editor = sharedPref.edit();
                                                        editor.putString(getResources().getString(R.string.iba_id), instagramBusinessAccount);
                                                        editor.apply();
                                                    } catch (JSONException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                });
                                        Bundle parameters = new Bundle();
                                        parameters.putString("fields", "instagram_business_account");
                                        requestSecond.setParameters(parameters);
                                        requestSecond.executeAsync();
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                        Bundle parameters = new Bundle();
                        parameters.putString("fields", "instagram_business_account");
                        requestFirst.setParameters(parameters);
                        requestFirst.executeAsync();
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(MainActivity.this, R.string.error2, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull FacebookException e) {
                        Toast.makeText(MainActivity.this, R.string.error3, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}
