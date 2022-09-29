package com.norram.bit;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.norram.bit.databinding.FragmentHistoryBinding;

import java.util.ArrayList;
import java.util.HashMap;

public class HistoryFragment extends Fragment {
    private FragmentHistoryBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_history, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final int spanCount = 3;
        binding.searchView.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        binding.recyclerView.setLayoutManager(
                new GridLayoutManager(requireContext(), spanCount, RecyclerView.VERTICAL, false)
        );

        // make favorite list
        FavoriteOpenHelper helperFav = new FavoriteOpenHelper(requireContext());
        ArrayList<String> favListTmp = new ArrayList<>();
        SQLiteDatabase dbFav = helperFav.getWritableDatabase();
        Cursor c = dbFav.rawQuery("SELECT name FROM FAVORITE_TABLE ORDER BY id DESC", null);
        boolean next = c.moveToFirst(); // check cursor has first row or not
        // get all rows
        while (next) {
            favListTmp.add(c.getString(0));
            next = c.moveToNext();
        }
        c.close();

        // make history list
        HistoryOpenHelper helperHis = new HistoryOpenHelper(requireContext());
        ArrayList<HashMap<String, String>> historyList = new ArrayList<>();
        SQLiteDatabase dbHis = helperHis.getWritableDatabase();
        dbHis.execSQL("CREATE TEMPORARY TABLE HISTORY_TMP AS SELECT MAX(id), url, name FROM HISTORY_TABLE GROUP BY name");
        dbHis.execSQL("DELETE FROM HISTORY_TABLE");
        dbHis.execSQL("INSERT INTO HISTORY_TABLE SELECT * FROM HISTORY_TMP");

        c = dbHis.rawQuery("SELECT url, name FROM HISTORY_TABLE ORDER BY id DESC", null);
        next = c.moveToFirst(); // check cursor has first row or not
        // get all rows
        while (next) {
            HashMap<String, String> data = new HashMap<>();
            String url = c.getString(0);
            String name = c.getString(1);
            data.put("url", url);
            data.put("name", name);
            // compare fav list with his list
            if(favListTmp.contains(name)) {
                data.put("isFavorite", "true");
            } else {
                data.put("isFavorite", "false");
            }
            historyList.add(data);
            next = c.moveToNext();
        }
        c.close();

        CommonData data = CommonData.getInstance();
        if(!data.isMeasured) {
            data.isMeasured = true;
            // use ViewTreeObserver to get accurate width
            binding.historyLinear.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            data.screenWidth = binding.historyLinear.getWidth();
                            binding.recyclerView.setAdapter(new UserAdapter(getActivity(), historyList, "HISTORY_TABLE"));
                            binding.historyLinear.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
            );
        } else {
            binding.recyclerView.setAdapter(new UserAdapter(getActivity(), historyList, "HISTORY_TABLE"));
        }

        binding.searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        if(query != null) {
                            ModeFragmentDirections.ActionModeFragmentToSearchFragment action = ModeFragmentDirections.actionModeFragmentToSearchFragment(query);
                            Navigation.findNavController(view).navigate(action);
                        }
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        return false;
                    }
                }
        );

        binding.searchButton.setOnClickListener(v -> {
            String username = binding.searchView.getQuery().toString();
            if (!username.equals("")) {
                ModeFragmentDirections.ActionModeFragmentToSearchFragment action = ModeFragmentDirections.actionModeFragmentToSearchFragment(username);
                Navigation.findNavController(view).navigate(action);
            }
        });
    }
}
