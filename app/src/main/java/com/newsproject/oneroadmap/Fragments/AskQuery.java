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
import android.widget.EditText;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.DialogFragment;

import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.ApiClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AskQuery extends DialogFragment {

    private ImageView backButton;
    private LinearLayout queryTypeContainer;
    private TextView tvSelectedType;
    private ImageView arrowDown;
    private EditText etTitle;
    private View submitButtonCard;
    private TextView submitText;

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
        etTitle = view.findViewById(R.id.query_title);
        submitButtonCard = view.findViewById(R.id.cardView6);
        submitText = view.findViewById(R.id.textView33);

        // ----- Click listeners -----------------------------------------------
        backButton.setOnClickListener(v -> dismiss());

        // ---- Dropdown -------------------------------------------------------
        queryTypeContainer.setOnClickListener(v -> showDropdownMenu());

        // ---- Submit ---------------------------------------------------------
        submitButtonCard.setClickable(true);
        submitButtonCard.setOnClickListener(v -> submitQuery());
        if (submitText != null) {
            submitText.setOnClickListener(v -> submitQuery());
        }

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

    private void submitQuery() {
        Context ctx = getContext();
        if (ctx == null) return;
        SharedPreferences sp = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = sp.getString("userId", "");
        String name = sp.getString("userName", sp.getString("name", "User"));
        String education = sp.getString("education", "");
        String avatar = sp.getString("avatar", "girl_profile"); // drawable name

        String type = tvSelectedType.getText() != null ? tvSelectedType.getText().toString() : "";
        if ("Select Query Type".equals(type)) type = "";
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        if (userId.isEmpty()) { Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show(); return; }
        if (title.isEmpty()) { Toast.makeText(requireContext(), "Please enter your question", Toast.LENGTH_SHORT).show(); return; }

        String uploadTime = nowIsoUtc();

        try {
            JSONObject body = new JSONObject();
            body.put("userId", userId);
            body.put("name", name);
            body.put("education", education);
            if (!type.isEmpty()) body.put("type", type);
            body.put("title", title);
            body.put("uploadTime", uploadTime);
            // Send both camelCase and snake_case for compatibility
            body.put("userRs", avatar);           // user avatar drawable name
            body.put("user_rs", avatar);
            body.put("replyText", "");            // initially empty
            body.put("replyTimestamp", "");       // initially empty
            body.put("replyUserRs", "");          // initially empty
            body.put("reply_user_rs", "");
            body.put("likedByUsers", new JSONArray());

            ApiClient.getInstance().saveOrUpdateQuery(body.toString(), new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    Log.e("AskQuery", "Submit failed", e);
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
                    );
                }

                @Override
                public void onResponse(Call call, Response response) throws java.io.IOException {
                    response.close();
                    if (response.isSuccessful()) {
                        // Close after successful submit
                        if (getDialog() != null) {
                            requireActivity().runOnUiThread(() -> dismiss());
                        }
                    } else {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Submit failed: " + response.code(), Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            });
        } catch (JSONException ignored) {}
    }

    private String nowIsoUtc() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
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