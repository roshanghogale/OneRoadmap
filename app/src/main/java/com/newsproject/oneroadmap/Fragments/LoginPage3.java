// File: com/newsproject/oneroadmap/Fragments/LoginPage3.java
package com.newsproject.oneroadmap.Fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.newsproject.oneroadmap.Activities.LoginActivity;
import com.newsproject.oneroadmap.Activities.MainActivity;
import com.newsproject.oneroadmap.Models.User;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.ApiClient;
import com.newsproject.oneroadmap.Utils.DataConstants;
import com.newsproject.oneroadmap.Utils.DatabaseHelper;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LoginPage3 extends Fragment {

    private DatabaseHelper databaseHelper;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private ApiClient apiClient;

    private Button btnStudyMaterial, btnPrev3, btnSubmit3;
    private RadioGroup rgCurrentPdf, rgJobByStream;
    private RadioButton rbJobYes, rbJobNo;
    private TextView txtJobByStreamLabel;

    // 👇 Dialog based views
    private TextView tvDistrict, tvTaluka;

    // User data
    private String name, gender, ageGroup, avatar, education, twelfth, degree, postGrad;
    private String selectedDistrict = "";
    private String selectedTaluka = "";
    private String jobTextByTwelfth = "";

    private final String[] studyMaterialOptions = {
            "Government", "Police & Defence", "Banking"
    };
    private final boolean[] selectedStudyMaterials = new boolean[studyMaterialOptions.length];

    public LoginPage3() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        databaseHelper = new DatabaseHelper(requireContext());
        sp = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        editor = sp.edit();
        apiClient = ApiClient.getInstance();

        if (getArguments() != null) {
            name = getArguments().getString("name");
            gender = getArguments().getString("gender");
            ageGroup = getArguments().getString("ageGroup");
            avatar = getArguments().getString("avatar");
            education = getArguments().getString("education");
            twelfth = getArguments().getString("twelfth");
            degree = getArguments().getString("degree");
            postGrad = getArguments().getString("postGrad");
        }

        for (int i = 0; i < studyMaterialOptions.length; i++) {
            String key = "study_" + studyMaterialOptions[i]
                    .replace(" & ", "_")
                    .replace(" ", "_");
            selectedStudyMaterials[i] = sp.getBoolean(key, false);
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_login_page3, container, false);

        btnStudyMaterial = view.findViewById(R.id.btnStudyMaterial);
        btnPrev3 = view.findViewById(R.id.btnPrev3);
        btnSubmit3 = view.findViewById(R.id.btnSubmit3);

        rgCurrentPdf = view.findViewById(R.id.rgCurrentPdf);
        rgJobByStream = view.findViewById(R.id.rgJobByStream);
        rbJobYes = view.findViewById(R.id.rbJobYes);
        rbJobNo = view.findViewById(R.id.rbJobNo);
        txtJobByStreamLabel = view.findViewById(R.id.txtJobByStream);

        tvDistrict = view.findViewById(R.id.tvDistrict);
        tvTaluka = view.findViewById(R.id.tvTaluka);

        rgCurrentPdf.check(R.id.rbPdfNo);
        rgJobByStream.check(R.id.rbJobNo);

        setupDistrictTalukaDialogs();
        setupStudyMaterialDialog();
        loadAndDisplayJobText();
        restoreSavedData();

        btnPrev3.setOnClickListener(v -> {
            saveCurrentData();
            requireActivity().onBackPressed();
        });

        btnSubmit3.setOnClickListener(v -> saveUserDetails());

        return view;
    }

    // =====================================================
    // DISTRICT / TALUKA DIALOGS
    // =====================================================
    private void setupDistrictTalukaDialogs() {

        setTalukaEnabled(false);

        tvDistrict.setOnClickListener(v ->
                showListDialog(
                        "Select District",
                        DataConstants.DISTRICTS,
                        district -> {
                            selectedDistrict = district;
                            tvDistrict.setText(district);
                            tvDistrict.setTextColor(Color.BLACK);

                            selectedTaluka = "";
                            tvTaluka.setText("Select Taluka");
                            tvTaluka.setTextColor(Color.GRAY);

                            setTalukaEnabled(true);
                        }
                )
        );

        tvTaluka.setOnClickListener(v -> {
            if (selectedDistrict.isEmpty()) {
                toast("पहिले जिल्हा निवडा");
                return;
            }

            List<String> talukas = DataConstants.TALUKA_MAP
                    .getOrDefault(selectedDistrict, new ArrayList<>());

            showListDialog(
                    "Select Taluka",
                    talukas,
                    taluka -> {
                        selectedTaluka = taluka;
                        tvTaluka.setText(taluka);
                        tvTaluka.setTextColor(Color.BLACK);
                    }
            );
        });
    }

    private void setTalukaEnabled(boolean enabled) {
        tvTaluka.setEnabled(enabled);
        tvTaluka.setAlpha(enabled ? 1f : 0.4f);
    }

    private void showListDialog(String titletext, List<String> items, OnSelect listener) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_age_picker);
        dialog.setCancelable(true);

        TextView title = dialog.findViewById(R.id.dialog_title);
        title.setText(titletext);

        ListView listView = dialog.findViewById(R.id.listAge);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_age_dialog,
                new ArrayList<>(items)
        );

        listView.setAdapter(adapter);
        listView.setOnItemClickListener((p, v, pos, id) -> {
            listener.onSelect(items.get(pos));
            dialog.dismiss();
        });

        dialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(Color.TRANSPARENT)
        );
        dialog.show();
    }

    // =====================================================
    // STUDY MATERIAL MULTI-SELECT
    // =====================================================
    private void setupStudyMaterialDialog() {
        btnStudyMaterial.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("तुम्ही कोणत्या भरतीची तयारी करत आहे ?")
                    .setMultiChoiceItems(
                            studyMaterialOptions,
                            selectedStudyMaterials,
                            (dialog, which, isChecked) ->
                                    selectedStudyMaterials[which] = isChecked
                    )
                    .setPositiveButton("OK", (dialog, which) -> {
                        List<String> chosen = new ArrayList<>();
                        for (int i = 0; i < studyMaterialOptions.length; i++) {
                            if (selectedStudyMaterials[i]) chosen.add(studyMaterialOptions[i]);
                        }

                        btnStudyMaterial.setText(
                                chosen.isEmpty() ? "कोणतेही नाही" : String.join(", ", chosen)
                        );

                        for (int i = 0; i < studyMaterialOptions.length; i++) {
                            String key = "study_" + studyMaterialOptions[i]
                                    .replace(" & ", "_")
                                    .replace(" ", "_");
                            editor.putBoolean(key, selectedStudyMaterials[i]);
                        }
                        editor.apply();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    // =====================================================
    // SAVE + RESTORE
    // =====================================================
    private void saveCurrentData() {
        editor.putBoolean(
                "currentAffairs",
                rgCurrentPdf.getCheckedRadioButtonId() == R.id.rbPdfYes
        );
        editor.putBoolean(
                "jobs",
                rgJobByStream.getCheckedRadioButtonId() == R.id.rbJobYes
        );
        editor.putString("district", selectedDistrict);
        editor.putString("taluka", selectedTaluka);
        editor.apply();
    }

    private void restoreSavedData() {
        selectedDistrict = sp.getString("district", "");
        selectedTaluka = sp.getString("taluka", "");

        if (!selectedDistrict.isEmpty()) {
            tvDistrict.setText(selectedDistrict);
            tvDistrict.setTextColor(Color.BLACK);
            setTalukaEnabled(true);
        }

        if (!selectedTaluka.isEmpty()) {
            tvTaluka.setText(selectedTaluka);
            tvTaluka.setTextColor(Color.BLACK);
        }

        List<String> saved = new ArrayList<>();
        for (String opt : studyMaterialOptions) {
            String key = "study_" + opt.replace(" & ", "_").replace(" ", "_");
            if (sp.getBoolean(key, false)) saved.add(opt);
        }
        btnStudyMaterial.setText(
                saved.isEmpty() ? "कोणतेही नाही" : String.join(", ", saved)
        );
    }

    // =====================================================
    // FINAL SAVE
    // =====================================================
    private void saveUserDetails() {

        if (selectedDistrict.isEmpty() || selectedTaluka.isEmpty()) {
            toast("जिल्हा आणि तालुका निवडा!");
            return;
        }

        boolean anyStudy = false;
        for (boolean b : selectedStudyMaterials) {
            if (b) {
                anyStudy = true;
                break;
            }
        }

        if (!anyStudy) {
            toast("कृपया किमान एक भरती निवडा!");
            return;
        }

        String userId = sp.getString("userId", "");
        if (userId.isEmpty()) {
            userId = UUID.randomUUID().toString();
            editor.putString("userId", userId);
        }

        User user = new User(
                userId, name, gender, avatar,
                selectedStudyMaterials[0],
                selectedStudyMaterials[1],
                selectedStudyMaterials[2],
                degree, postGrad,
                selectedDistrict, selectedTaluka,
                rgCurrentPdf.getCheckedRadioButtonId() == R.id.rbPdfYes,
                rgJobByStream.getCheckedRadioButtonId() == R.id.rbJobYes,
                ageGroup, education, twelfth
        );

        try {
            User existing = databaseHelper.getUser(userId);
            if (existing != null) databaseHelper.updateUser(user);
            else databaseHelper.insertUser(user);

            editor.putBoolean("isLoggedIn", true).apply();
        } catch (Exception e) {
            toast("Local save failed");
            Log.e("LoginPage3", "DB error", e);
            return;
        }

        apiClient.saveUser(new Gson().toJson(user), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                toast("Network error");
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    startActivity(new Intent(requireContext(), MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    requireActivity().finish();
                } else {
                    toast("Server error");
                }
            }
        });
    }

    private void loadAndDisplayJobText() {
        String saved = sp.getString("jobTextByTwelfth", "");
        if (!saved.isEmpty()) jobTextByTwelfth = saved;

        if (!jobTextByTwelfth.isEmpty()) {
            txtJobByStreamLabel.setText("तुम्हाला " + jobTextByTwelfth);
        }
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    interface OnSelect {
        void onSelect(String value);
    }
}
