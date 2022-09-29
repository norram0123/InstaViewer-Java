package com.norram.bit;

import android.app.AlertDialog;
import android.app.Dialog;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.HashMap;

public class DeleteDialogFragment extends DialogFragment {
    private final UserAdapter userAdapter;
    private final ArrayList<HashMap<String, String>> historyList;
    private final int position;

    public DeleteDialogFragment(
            UserAdapter userAdapter,
            ArrayList<HashMap<String, String>> historyList,
            int position
    ) {
        this.userAdapter = userAdapter;
        this.historyList = historyList;
        this.position = position;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dialog_delete_history)
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    HistoryOpenHelper helper = new HistoryOpenHelper(requireContext());
                    SQLiteDatabase db = helper.getWritableDatabase();
                    db.execSQL("DELETE FROM HISTORY_TABLE WHERE id = (SELECT id FROM HISTORY_TABLE ORDER BY id DESC LIMIT 1 OFFSET ?)",
                            new String[]{position + ""});
                    historyList.remove(position);
                    userAdapter.notifyItemRemoved(position);
                    userAdapter.notifyItemRangeChanged(position, historyList.size() - position);
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> {
                    // User cancelled the dialog
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}