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
import android.widget.RadioGroup;
import android.widget.Spinner;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class LoginPage3 extends Fragment {

    private FirebaseFirestore db;
    private DatabaseHelper databaseHelper;
    private SharedPreferences sharedPreferences;
    private Button btnStudyMaterial;
    private Spinner spinnerDistrict3, spinnerTaluka3;
    private RadioGroup rgCurrentPdf, rgJobByStream;
    private Button btnPrev3, btnSubmit3;
    private String name, gender, ageGroup, avatar, education, twelfth, degree, postGrad;
    private boolean[] selectedStudyMaterials;
    private boolean upscSelected, mpscSelected;
    private ArrayAdapter<String> talukaAdapter;
    private final String[] studyMaterialOptions = {"UPSC", "MPSC"};

    public LoginPage3() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        databaseHelper = new DatabaseHelper(requireContext());
        sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

        if (getArguments() != null) {
            name = getArguments().getString("name");
            gender = getArguments().getString("gender");
            ageGroup = getArguments().getString("ageGroup");
            avatar = getArguments().getString("avatar");
            education = getArguments().getString("education");
            twelfth = getArguments().getString("twelfth");
            degree = getArguments().getString("degree");
            postGrad = getArguments().getString("postGrad");
            Log.d("LoginPage3", "Retrieved from Bundle - education: " + education +
                    ", twelfth: " + twelfth + ", degree: " + degree + ", postGrad: " + postGrad);
        }

        selectedStudyMaterials = new boolean[studyMaterialOptions.length];
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_page3, container, false);

        btnStudyMaterial = view.findViewById(R.id.btnStudyMaterial);
        spinnerDistrict3 = view.findViewById(R.id.spinnerDistrict3);
        spinnerTaluka3 = view.findViewById(R.id.spinnerTaluka3);
        rgCurrentPdf = view.findViewById(R.id.rgCurrentPdf);
        rgJobByStream = view.findViewById(R.id.rgJobByStream);
        btnPrev3 = view.findViewById(R.id.btnPrev3);
        btnSubmit3 = view.findViewById(R.id.btnSubmit3);

        // Set default radio button selections
        rgCurrentPdf.check(R.id.rbPdfNo); // Default to "नाही" (false)
        rgJobByStream.check(R.id.rbJobNo); // Default to "नाही" (false)

        setupSpinners();
        setupStudyMaterialSelection();
        restoreSavedData();

        btnPrev3.setOnClickListener(v -> {
            saveCurrentData();
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        btnSubmit3.setOnClickListener(v -> saveUserDetails());

        return view;
    }

    private void setupSpinners() {
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, DataConstants.DISTRICTS);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDistrict3.setAdapter(districtAdapter);

        List<String> defaultTalukaOptions = new ArrayList<>(Arrays.asList("Select Taluka"));
        talukaAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, defaultTalukaOptions);
        talukaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTaluka3.setAdapter(talukaAdapter);
        spinnerTaluka3.setEnabled(false);
        spinnerTaluka3.setAlpha(0.5f);

        spinnerDistrict3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDistrict = DataConstants.DISTRICTS.get(position);
                if (!selectedDistrict.equals("Select District")) {
                    List<String> talukaOptions = DataConstants.TALUKA_MAP.getOrDefault(selectedDistrict, Arrays.asList("Select Taluka"));
                    talukaAdapter.clear();
                    talukaAdapter.addAll(talukaOptions);
                    talukaAdapter.notifyDataSetChanged();
                    spinnerTaluka3.setEnabled(true);
                    spinnerTaluka3.setAlpha(1.0f);
                    spinnerTaluka3.setSelection(0);
                    String savedTaluka = sharedPreferences.getString("taluka", "");
                    if (!savedTaluka.isEmpty()) {
                        int talukaPosition = talukaAdapter.getPosition(savedTaluka);
                        if (talukaPosition != -1) {
                            spinnerTaluka3.setSelection(talukaPosition);
                        }
                    }
                } else {
                    spinnerTaluka3.setEnabled(false);
                    spinnerTaluka3.setAlpha(0.5f);
                    talukaAdapter.clear();
                    talukaAdapter.add("Select Taluka");
                    talukaAdapter.notifyDataSetChanged();
                    spinnerTaluka3.setSelection(0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupStudyMaterialSelection() {
        btnStudyMaterial.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Select Study Materials");
            builder.setMultiChoiceItems(studyMaterialOptions, selectedStudyMaterials, (dialog, which, isChecked) -> {
                selectedStudyMaterials[which] = isChecked;
            });
            builder.setPositiveButton("OK", (dialog, which) -> {
                StringBuilder selectedText = new StringBuilder();
                List<String> selectedItems = new ArrayList<>();
                for (int i = 0; i < studyMaterialOptions.length; i++) {
                    if (selectedStudyMaterials[i]) {
                        selectedItems.add(studyMaterialOptions[i]);
                    }
                }
                selectedText.append(selectedItems.isEmpty() ? "Select Study Materials" : String.join(", ", selectedItems));
                btnStudyMaterial.setText(selectedText.toString());
                upscSelected = selectedItems.contains("UPSC");
                mpscSelected = selectedItems.contains("MPSC");
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("upsc", upscSelected);
                editor.putBoolean("mpsc", mpscSelected);
                editor.putString("studyMaterials", selectedText.toString());
                editor.apply();
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        });
    }

    private void saveCurrentData() {
        boolean currentAffairs = rgCurrentPdf.getCheckedRadioButtonId() == R.id.rbPdfYes;
        boolean jobs = rgJobByStream.getCheckedRadioButtonId() == R.id.rbJobYes;
        String district = spinnerDistrict3.getSelectedItem().toString();
        String taluka = spinnerTaluka3.getSelectedItem().toString();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("currentAffairs", currentAffairs);
        editor.putBoolean("jobs", jobs);
        editor.putString("district", district);
        editor.putString("taluka", taluka);
        editor.putString("twelfth", twelfth); // Save twelfth
        editor.apply();
        Log.d("LoginPage3", "Saved to SharedPrefs: currentAffairs=" + currentAffairs + ", jobs=" + jobs);
    }

    private void restoreSavedData() {
        boolean savedCurrentAffairs = sharedPreferences.getBoolean("currentAffairs", false);
        boolean savedJobs = sharedPreferences.getBoolean("jobs", false);
        String savedDistrict = sharedPreferences.getString("district", "");
        String savedStudyMaterials = sharedPreferences.getString("studyMaterials", "");
        String savedTwelfth = sharedPreferences.getString("twelfth", "");
        upscSelected = sharedPreferences.getBoolean("upsc", false);
        mpscSelected = sharedPreferences.getBoolean("mpsc", false);

        // Restore radio buttons
        rgCurrentPdf.check(savedCurrentAffairs ? R.id.rbPdfYes : R.id.rbPdfNo);
        rgJobByStream.check(savedJobs ? R.id.rbJobYes : R.id.rbJobNo);

        // Restore district
        if (!savedDistrict.isEmpty()) {
            ArrayAdapter<String> districtAdapter = (ArrayAdapter<String>) spinnerDistrict3.getAdapter();
            int position = districtAdapter.getPosition(savedDistrict);
            if (position != -1) {
                spinnerDistrict3.setSelection(position);
            }
        }

        // Restore study materials
        if (!savedStudyMaterials.isEmpty()) {
            btnStudyMaterial.setText(savedStudyMaterials);
            selectedStudyMaterials[0] = upscSelected;
            selectedStudyMaterials[1] = mpscSelected;
        }

        // Restore twelfth
        if (!savedTwelfth.isEmpty()) {
            this.twelfth = savedTwelfth; // Update local variable
        }

        Log.d("LoginPage3", "Restored: currentAffairs=" + savedCurrentAffairs + ", jobs=" + savedJobs + ", twelfth=" + savedTwelfth);
    }

    private void saveUserDetails() {
        boolean currentAffairs = rgCurrentPdf.getCheckedRadioButtonId() == R.id.rbPdfYes;
        boolean jobs = rgJobByStream.getCheckedRadioButtonId() == R.id.rbJobYes;
        String district = spinnerDistrict3.getSelectedItem().toString();
        String taluka = spinnerTaluka3.getSelectedItem().toString();

        // Validate inputs
        if (!upscSelected && !mpscSelected || district.equals("Select District") || taluka.equals("Select Taluka")) {
            Toast.makeText(requireContext(), "Please select study materials, district, and taluka!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = sharedPreferences.getString("userId", "");
        if (userId.isEmpty()) {
            userId = UUID.randomUUID().toString();
            SharedPreferences.Editor spEditor = sharedPreferences.edit();
            spEditor.putString("userId", userId);
            spEditor.putBoolean("isLoggedIn", true);
            spEditor.apply();
            Log.d("LoginPage3", "Generated new userId: " + userId);
        }

        User user = new User(userId, name, gender, avatar, upscSelected, mpscSelected, degree, postGrad,
                district, taluka, currentAffairs, jobs, ageGroup, education, twelfth);
        Log.d("LoginPage3", "User object: currentAffairs=" + currentAffairs + ", jobs=" + jobs + ", twelfth=" + twelfth);

        String finalUserId = userId;
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "User saved successfully in Firestore: " + finalUserId);
                    try {
                        databaseHelper.insertUser(user);
                        saveToSharedPreferences(user);
                        Toast.makeText(requireContext(), "User saved successfully!", Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    } catch (Exception e) {
                        Log.e("SQLite", "Failed to save user in SQLite: " + e.getMessage());
                        handleSQLiteError(e.getMessage(), user);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to save user: " + e.getMessage());
                    Toast.makeText(requireContext(), "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveToSharedPreferences(User user) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isDataSaved", true);
        editor.putString("userName", user.getName());
        editor.putString("gender", user.getGender());
        editor.putString("avatar", user.getAvatar());
        editor.putBoolean("upsc", user.isUpsc());
        editor.putBoolean("mpsc", user.isMpsc());
        editor.putString("degree", user.getDegree());
        editor.putString("postGraduation", user.getPostGraduation());
        editor.putString("district", user.getDistrict());
        editor.putString("taluka", user.getTaluka());
        editor.putBoolean("currentAffairs", user.isCurrentAffairs());
        editor.putBoolean("jobs", user.isJobs());
        editor.putString("ageGroup", user.getAgeGroup());
        editor.putString("education", user.getEducation());
        editor.putString("twelfth", user.getTwelfth());
        editor.apply();
        Log.d("SharedPreferences", "Updated SharedPreferences: isDataSaved=true, userName=" + user.getName() +
                ", currentAffairs=" + user.isCurrentAffairs() + ", jobs=" + user.isJobs() + ", twelfth=" + user.getTwelfth());
    }

    private void handleSQLiteError(String errorMsg, User user) {
        if (errorMsg != null && errorMsg.contains("UNIQUE constraint failed")) {
            try {
                databaseHelper.updateUser(user);
                saveToSharedPreferences(user);
                Toast.makeText(requireContext(), "User updated successfully!", Toast.LENGTH_SHORT).show();
                navigateToMainActivity();
            } catch (Exception updateEx) {
                Log.e("SQLite", "Failed to update user: " + updateEx.getMessage());
                shareErrorViaWhatsApp(updateEx.getMessage(), user);
            }
        } else if (errorMsg != null && errorMsg.contains("no column named")) {
            try {
                requireContext().deleteDatabase("user_db");
                databaseHelper = new DatabaseHelper(requireContext());
                databaseHelper.insertUser(user);
                saveToSharedPreferences(user);
                Toast.makeText(requireContext(), "User saved successfully!", Toast.LENGTH_SHORT).show();
                navigateToMainActivity();
            } catch (Exception recreateEx) {
                Log.e("SQLite", "Failed to recreate database: " + recreateEx.getMessage());
                shareErrorViaWhatsApp(recreateEx.getMessage(), user);
            }
        } else {
            shareErrorViaWhatsApp(errorMsg, user);
        }

        String displayMsg = (errorMsg != null && errorMsg.contains("UNIQUE constraint failed")) ? "User ID already exists" :
                (errorMsg != null && errorMsg.contains("no column named")) ? "Database schema error" :
                        (errorMsg != null && errorMsg.length() > 50 ? errorMsg.substring(0, 50) + "..." : errorMsg);
        Toast.makeText(requireContext(), "SQLite error: " + displayMsg, Toast.LENGTH_LONG).show();
    }

    private void shareErrorViaWhatsApp(String errorMessage, User user) {
        StringBuilder message = new StringBuilder();
        message.append("SQLite Error: ").append(errorMessage).append("\n\n");
        message.append("User Data:\n");
        message.append("userId: ").append(user.getUserId()).append("\n");
        message.append("name: ").append(user.getName()).append("\n");
        message.append("gender: ").append(user.getGender()).append("\n");
        message.append("upsc: ").append(user.isUpsc()).append("\n");
        message.append("mpsc: ").append(user.isMpsc()).append("\n");
        message.append("degree: ").append(user.getDegree()).append("\n");
        message.append("postGraduation: ").append(user.getPostGraduation()).append("\n");
        message.append("district: ").append(user.getDistrict()).append("\n");
        message.append("taluka: ").append(user.getTaluka()).append("\n");
        message.append("currentAffairs: ").append(user.isCurrentAffairs()).append("\n");
        message.append("jobs: ").append(user.isJobs()).append("\n");
        message.append("ageGroup: ").append(user.getAgeGroup()).append("\n");
        message.append("education: ").append(user.getEducation()).append("\n");
        message.append("twelfth: ").append(user.getTwelfth()).append("\n");
        message.append("Database File: ").append(requireContext().getDatabasePath("user_db").getAbsolutePath()).append("\n");
        message.append("Storage Available: ").append(getAvailableStorage()).append(" bytes\n");

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, message.toString());
        sendIntent.setType("text/plain");
        sendIntent.setPackage("com.whatsapp");
        try {
            startActivity(sendIntent);
        } catch (Exception e) {
            Log.e("WhatsApp", "Failed to share error via WhatsApp: " + e.getMessage());
            Toast.makeText(requireContext(), "WhatsApp not installed or error occurred", Toast.LENGTH_SHORT).show();
        }
    }

    private long getAvailableStorage() {
        try {
            File path = requireContext().getDatabasePath("user_db").getParentFile();
            return path.getUsableSpace();
        } catch (Exception e) {
            return -1;
        }
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requireActivity() instanceof LoginActivity) {
            ((LoginActivity) requireActivity()).updateProgressBar(3, false);
        }
    }
}