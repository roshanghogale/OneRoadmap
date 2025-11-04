package com.newsproject.oneroadmap.Fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.DialogFragment;

import com.newsproject.oneroadmap.R;

public class AskQuery extends DialogFragment {

    private ImageView backButton;
    private LinearLayout queryTypeContainer;
    private TextView tvSelectedType;
    private ImageView arrowDown;

    // -----------------------------------------------------------------
    //  Options (exactly what you asked for)
    // -----------------------------------------------------------------

    private static final String DEFAULT_TEXT = "Select Query Type";
    private final String[] queryTypes = {
            "Maharashtra Government",
            "Central Government",
            "Government Banking",
            "Private Banking",
            "Private"
    };

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.fragment_ask_query, null);

        // ----- UI references -------------------------------------------------
        backButton = view.findViewById(R.id.back_button7);
        queryTypeContainer = view.findViewById(R.id.query_type_container);
        tvSelectedType = view.findViewById(R.id.tv_selected_type);
        arrowDown = view.findViewById(R.id.imageView27);

        // ----- Click listeners -----------------------------------------------
        backButton.setOnClickListener(v -> dismiss());

        // ---- Dropdown -------------------------------------------------------
        queryTypeContainer.setOnClickListener(v -> showDropdownMenu());

        // ----- Build dialog --------------------------------------------------
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(view);
        AlertDialog dialog = builder.create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        return dialog;
    }

    // -----------------------------------------------------------------
    //  Show Material PopupMenu anchored to the whole row
    // -----------------------------------------------------------------
    private void showDropdownMenu() {
        PopupMenu popup = new PopupMenu(requireContext(), queryTypeContainer);

        // 1. add a “clear” entry at the top
        popup.getMenu().add(0, -1, 0, DEFAULT_TEXT);   // id = -1 → clear
        for (int i = 0; i < queryTypes.length; i++) {
            popup.getMenu().add(0, i, i + 1, queryTypes[i]);
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == -1) {                         // user tapped the default entry
                tvSelectedType.setText(DEFAULT_TEXT);
                arrowDown.setRotation(0);           // back to down-arrow
            } else {                                // a real type was chosen
                tvSelectedType.setText(queryTypes[id]);
                arrowDown.setRotation(180);         // keep it up-arrow
            }
            return true;
        });

        // 2. rotate arrow **only when the menu is shown**
        arrowDown.setRotation(180);                 // up-arrow while menu is open
        popup.setOnDismissListener(menu -> arrowDown.setRotation(
                DEFAULT_TEXT.equals(tvSelectedType.getText().toString()) ? 0 : 180
        ));

        popup.show();
    }

    @Override
    public void onStart() {
        super.onStart();
        // keep WRAP_CONTENT height, limit to 85% of screen if needed
        Dialog dlg = getDialog();
        if (dlg != null && dlg.getWindow() != null) {
            dlg.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}