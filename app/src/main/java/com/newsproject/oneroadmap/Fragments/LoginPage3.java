package com.newsproject.oneroadmap.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.newsproject.oneroadmap.Activities.LoginActivity;
import com.newsproject.oneroadmap.Activities.MainActivity;
import com.newsproject.oneroadmap.Models.User;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.DataConstants;
import com.newsproject.oneroadmap.Utils.DatabaseHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class LoginPage3 extends Fragment {

    private FirebaseFirestore db;
    private DatabaseHelper databaseHelper;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

    private Button btnStudyMaterial;
    private Spinner spinnerDistrict3, spinnerTaluka3;
    private RadioGroup rgCurrentPdf, rgJobByStream;
    private Button btnPrev3, btnSubmit3;

    // UI elements for job text
    private TextView txtJobByStreamLabel;
    private RadioButton rbJobYes, rbJobNo;

    // Data from previous pages
    private String name, gender, ageGroup, avatar, education, twelfth, degree, postGrad;
    private String jobTextByTwelfth = ""; // Will be loaded from SP or DataConstants

    // Study-material selection
    private final String[] studyMaterialOptions = {
            "Government", "Police & Defence", "Banking", "Self Improvement"
    };
    private boolean[] selectedStudyMaterials = new boolean[studyMaterialOptions.length];

    private ArrayAdapter<String> talukaAdapter;

    public LoginPage3() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        databaseHelper = new DatabaseHelper(requireContext());
        sp = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        editor = sp.edit();

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

        // Load saved study material preferences
        for (int i = 0; i < studyMaterialOptions.length; i++) {
            String key = "study_" + studyMaterialOptions[i].replace(" & ", "_").replace(" ", "_");
            selectedStudyMaterials[i] = sp.getBoolean(key, false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_page3, container, false);

        // Initialize views
        btnStudyMaterial = view.findViewById(R.id.btnStudyMaterial);
        spinnerDistrict3 = view.findViewById(R.id.spinnerDistrict3);
        spinnerTaluka3 = view.findViewById(R.id.spinnerTaluka3);
        rgCurrentPdf = view.findViewById(R.id.rgCurrentPdf);
        rgJobByStream = view.findViewById(R.id.rgJobByStream);
        btnPrev3 = view.findViewById(R.id.btnPrev3);
        btnSubmit3 = view.findViewById(R.id.btnSubmit3);

        // Job stream UI
        txtJobByStreamLabel = view.findViewById(R.id.txtJobByStream);
        rbJobYes = view.findViewById(R.id.rbJobYes);
        rbJobNo = view.findViewById(R.id.rbJobNo);

        // Default radio selections
        rgCurrentPdf.check(R.id.rbPdfNo);
        rgJobByStream.check(R.id.rbJobNo);

        setupSpinners();
        setupStudyMaterialSelection();
        loadAndDisplayJobText(); // NEW: Load job text from SP or fallback
        restoreSavedData();

        btnPrev3.setOnClickListener(v -> {
            saveCurrentData();
            requireActivity().onBackPressed();
        });

        btnSubmit3.setOnClickListener(v -> saveUserDetails());

        return view;
    }

    // Load job text from SharedPreferences or fallback to DataConstants
    private void loadAndDisplayJobText() {
        String savedJobText = sp.getString("jobTextByTwelfth", "");
        if (!savedJobText.isEmpty()) {
            jobTextByTwelfth = savedJobText;
        } else if (twelfth != null && !twelfth.isEmpty()) {
            int index = DataConstants.TWELFTH_OPTIONS.indexOf(twelfth);
            if (index >= 0 && index < DataConstants.JOB_TEXT_BY_TWELFTH.size()) {
                jobTextByTwelfth = DataConstants.JOB_TEXT_BY_TWELFTH.get(index);
            }
        }

        if (!jobTextByTwelfth.isEmpty()) {
            txtJobByStreamLabel.setText("तुम्हाला " + jobTextByTwelfth);
            rbJobYes.setText("होय");
            rbJobNo.setText("नाही");
        } else {
            txtJobByStreamLabel.setText("दहावी, बारावी आधारित जॉब पाहिजेत ?");
        }
    }

    private void setupSpinners() {
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item, DataConstants.DISTRICTS);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDistrict3.setAdapter(districtAdapter);

        talukaAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item,
                new ArrayList<>(Arrays.asList("Select Taluka")));
        talukaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTaluka3.setAdapter(talukaAdapter);
        spinnerTaluka3.setEnabled(false);
        spinnerTaluka3.setAlpha(0.5f);

        spinnerDistrict3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String district = DataConstants.DISTRICTS.get(pos);
                if (!district.equals("Select District")) {
                    List<String> talukas = DataConstants.TALUKA_MAP.getOrDefault(district,
                            Arrays.asList("Select Taluka"));
                    talukaAdapter.clear();
                    talukaAdapter.addAll(talukas);
                    talukaAdapter.notifyDataSetChanged();
                    spinnerTaluka3.setEnabled(true);
                    spinnerTaluka3.setAlpha(1f);
                    spinnerTaluka3.setSelection(0);

                    String saved = sp.getString("taluka", "");
                    if (!saved.isEmpty()) {
                        int idx = talukas.indexOf(saved);
                        if (idx >= 0) spinnerTaluka3.setSelection(idx);
                    }
                } else {
                    spinnerTaluka3.setEnabled(false);
                    spinnerTaluka3.setAlpha(0.5f);
                    talukaAdapter.clear();
                    talukaAdapter.add("Select Taluka");
                    talukaAdapter.notifyDataSetChanged();
                }
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupStudyMaterialSelection() {
        btnStudyMaterial.setOnClickListener(v -> {
            AlertDialog.Builder b = new AlertDialog.Builder(requireContext())
                    .setTitle("FREE स्टडी मटेरियल कोणत्या भरतीची पाहिजे ?")
                    .setMultiChoiceItems(studyMaterialOptions, selectedStudyMaterials,
                            (dialog, which, isChecked) -> selectedStudyMaterials[which] = isChecked)
                    .setPositiveButton("OK", (dialog, which) -> {
                        List<String> chosen = new ArrayList<>();
                        for (int i = 0; i < studyMaterialOptions.length; i++) {
                            if (selectedStudyMaterials[i]) chosen.add(studyMaterialOptions[i]);
                        }
                        String txt = chosen.isEmpty() ? "कोणतेही नाही" : String.join(", ", chosen);
                        btnStudyMaterial.setText(txt);

                        // Save to SharedPreferences
                        for (int i = 0; i < studyMaterialOptions.length; i++) {
                            String key = "study_" + studyMaterialOptions[i]
                                    .replace(" & ", "_").replace(" ", "_");
                            editor.putBoolean(key, selectedStudyMaterials[i]);
                        }
                        editor.apply();
                    })
                    .setNegativeButton("Cancel", null);
            b.show();
        });
    }

    private void restoreSavedData() {
        // Current Affairs
        boolean ca = sp.getBoolean("currentAffairs", false);
        rgCurrentPdf.check(ca ? R.id.rbPdfYes : R.id.rbPdfNo);

        // Jobs by Stream
        boolean job = sp.getBoolean("jobs", false);
        rgJobByStream.check(job ? R.id.rbJobYes : R.id.rbJobNo);

        // District
        String savedDist = sp.getString("district", "");
        if (!savedDist.isEmpty()) {
            int pos = DataConstants.DISTRICTS.indexOf(savedDist);
            if (pos >= 0) spinnerDistrict3.setSelection(pos);
        }

        // Study Material Button Text
        List<String> saved = new ArrayList<>();
        for (String opt : studyMaterialOptions) {
            String key = "study_" + opt.replace(" & ", "_").replace(" ", "_");
            if (sp.getBoolean(key, false)) saved.add(opt);
        }
        btnStudyMaterial.setText(saved.isEmpty() ? "कोणतेही नाही" : String.join(", ", saved));
    }

    private void saveCurrentData() {
        boolean ca = rgCurrentPdf.getCheckedRadioButtonId() == R.id.rbPdfYes;
        boolean job = rgJobByStream.getCheckedRadioButtonId() == R.id.rbJobYes;
        String district = spinnerDistrict3.getSelectedItem().toString();
        String taluka = spinnerTaluka3.getSelectedItem().toString();

        editor.putBoolean("currentAffairs", ca);
        editor.putBoolean("jobs", job);
        editor.putString("district", district);
        editor.putString("taluka", taluka);
        editor.apply();
    }

    private void saveUserDetails() {
        boolean ca = rgCurrentPdf.getCheckedRadioButtonId() == R.id.rbPdfYes;
        boolean job = rgJobByStream.getCheckedRadioButtonId() == R.id.rbJobYes;
        String district = spinnerDistrict3.getSelectedItem().toString();
        String taluka = spinnerTaluka3.getSelectedItem().toString();

        // ---- Validation ----
        boolean anyStudy = false;
        for (boolean b : selectedStudyMaterials) if (b) { anyStudy = true; break; }

        if (!anyStudy) {
            Toast.makeText(requireContext(), "कृपया किमान एक स्टडी मटेरियल निवडा!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (district.equals("Select District")) {
            Toast.makeText(requireContext(), "कृपया जिल्हा निवडा!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (taluka.equals("Select Taluka")) {
            Toast.makeText(requireContext(), "कृपया तालुका निवडा!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rgJobByStream.getCheckedRadioButtonId() == -1) {
            Toast.makeText(requireContext(), "कृपया नोकरी पर्याय निवडा!", Toast.LENGTH_SHORT).show();
            return;
        }

        // ---- User ID ----
        String userId = sp.getString("userId", "");
        if (userId.isEmpty()) {
            userId = UUID.randomUUID().toString();
            editor.putString("userId", userId).putBoolean("isLoggedIn", true).apply();
        }

        // ---- Build User object ----
        User user = new User(
                userId, name, gender, avatar,
                selectedStudyMaterials[0],   // Government
                selectedStudyMaterials[1],   // Police & Defence
                selectedStudyMaterials[2],   // Banking
                selectedStudyMaterials[3],   // Self Improvement
                degree, postGrad,
                district, taluka,
                ca, job,
                ageGroup, education, twelfth
        );

        // ---- Save to Firestore + SQLite + SharedPreferences ----
        db.collection("users").document(userId).set(user)
                .addOnSuccessListener(aVoid -> {
                    try {
                        databaseHelper.insertUser(user);
                        saveAllToSharedPreferences(user);
                        Toast.makeText(requireContext(), "सर्व माहिती यशस्वी साठवली!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(requireContext(), MainActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        requireActivity().finish();
                    } catch (Exception e) {
                        handleSQLiteError(e.getMessage(), user);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "Firestore त्रुटी: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveAllToSharedPreferences(User u) {
        editor.putBoolean("isDataSaved", true);
        editor.putString("userName", u.getName());
        editor.putString("gender", u.getGender());
        editor.putString("avatar", u.getAvatar());

        editor.putBoolean("study_Government", u.isStudyGovernment());
        editor.putBoolean("study_Police___Defence", u.isStudyPoliceDefence());
        editor.putBoolean("study_Banking", u.isStudyBanking());
        editor.putBoolean("study_Self_Improvement", u.isStudySelfImprovement());

        editor.putString("degree", u.getDegree());
        editor.putString("postGraduation", u.getPostGraduation());
        editor.putString("district", u.getDistrict());
        editor.putString("taluka", u.getTaluka());
        editor.putBoolean("currentAffairs", u.isCurrentAffairs());
        editor.putBoolean("jobs", u.isJobs());
        editor.putString("ageGroup", u.getAgeGroup());
        editor.putString("education", u.getEducation());
        editor.putString("twelfth", u.getTwelfth());
        editor.putString("jobTextByTwelfth", jobTextByTwelfth); // Save for ProfileFragment
        editor.apply();
    }

    private void handleSQLiteError(String msg, User user) {
        // Keep your existing error handling logic
        Log.e("LoginPage3", "SQLite Error: " + msg);
        Toast.makeText(requireContext(), "डेटा सेव्ह करताना त्रुटी: " + msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requireActivity() instanceof LoginActivity) {
            ((LoginActivity) requireActivity()).updateProgressBar(3, false);
        }
    }
}