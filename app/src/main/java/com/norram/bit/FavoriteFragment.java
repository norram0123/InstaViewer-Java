package com.norram.bit;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.norram.bit.databinding.FragmentFavoriteBinding;

import java.util.ArrayList;
import java.util.HashMap;

public class FavoriteFragment extends Fragment {
    private FragmentFavoriteBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_favorite, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final int spanCount = 3;
        binding.recyclerView.setLayoutManager(
                new GridLayoutManager(requireContext(), spanCount, RecyclerView.VERTICAL, false)
        );

        ArrayList<HashMap<String, String>> favoriteList = new ArrayList<>();
        FavoriteOpenHelper helper = new FavoriteOpenHelper(requireContext());
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL("CREATE TEMPORARY TABLE FAVORITE_TMP " +
                "AS SELECT MAX(id), url, name FROM FAVORITE_TABLE GROUP BY name");
        db.execSQL("DELETE FROM FAVORITE_TABLE");
        db.execSQL("INSERT INTO FAVORITE_TABLE SELECT * FROM FAVORITE_TMP");
        Cursor c = db.rawQuery("SELECT url, name FROM FAVORITE_TABLE ORDER BY id DESC", null);
        boolean next = c.moveToFirst(); // check cursor has first row or not
        // get all rows
        while (next) {
            HashMap<String, String> data = new HashMap<>();
            String url = c.getString(0);
            String name = c.getString(1);
            data.put("url", url);
            data.put("name", name);
            data.put("isFavorite", "true");
            favoriteList.add(data);
            next = c.moveToNext();
        }
        c.close();

        binding.recyclerView.setAdapter(new UserAdapter(getActivity(), favoriteList, "FAVORITE_TABLE"));
    }
}