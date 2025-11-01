package com.newsproject.oneroadmap.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.DataConstants;
import com.newsproject.oneroadmap.Utils.DatabaseHelper;
import com.newsproject.oneroadmap.Activities.LoginActivity;
import com.newsproject.oneroadmap.Models.User;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private FirebaseFirestore db;
    private DatabaseHelper dbHelper;
    private String userId;
    private TextView profileName, currentAffairsSelection, jobsSelection, coinTextView, studyMaterialSelection;
    private ImageView profileNameEdit, currentAffairsEdit, jobsEdit, profileImage, studyMaterialEdit;
    private LinearLayout shareButtonContainer;
    private RadioGroup radioGroupGender;
    private Spinner spinnerEducation, spinnerTwelfth, spinnerDegree, spinnerPostGrad, spinnerDistrict, spinnerTaluka, spinnerAgeGroup;
    private ArrayAdapter<String> talukaAdapter, degreeAdapter, postGradAdapter;
    private boolean isReverting = false;
    private Handler handler = new Handler();
    private int displayedCoins = 0;

    private final List<String> ageGroupOptions = Arrays.asList(
            "Select Age Group", "18-25", "26-35", "36-45", "Above 45");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        userId = sharedPreferences.getString("userId", null);
        db = FirebaseFirestore.getInstance();
        dbHelper = new DatabaseHelper(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize UI elements
        profileName = view.findViewById(R.id.profile_name);
        profileNameEdit = view.findViewById(R.id.imageView22);
        profileImage = view.findViewById(R.id.imageView19);
        radioGroupGender = view.findViewById(R.id.radioGroupGender);
        spinnerEducation = view.findViewById(R.id.spinnerEducation);
        spinnerTwelfth = view.findViewById(R.id.spinnerTwelfth);
        spinnerDegree = view.findViewById(R.id.spinnerDegree);
        spinnerPostGrad = view.findViewById(R.id.spinnerPostGraduation);
        spinnerDistrict = view.findViewById(R.id.spinnerDistrict);
        spinnerTaluka = view.findViewById(R.id.spinnerTaluka);
        spinnerAgeGroup = view.findViewById(R.id.spinnerAgeGroup);
        currentAffairsSelection = view.findViewById(R.id.current_affairs_selection);
        currentAffairsEdit = view.findViewById(R.id.profile_current_affairs_edit);
        jobsSelection = view.findViewById(R.id.profile_10th_and_12th_jobs_selection);
        jobsEdit = view.findViewById(R.id.profile_10th_and_12th_jobs_edit);
        studyMaterialSelection = view.findViewById(R.id.study_material_selection);
        studyMaterialEdit = view.findViewById(R.id.profile_study_material_edit);
        coinTextView = view.findViewById(R.id.textView25);
        shareButtonContainer = view.findViewById(R.id.linearLayout_share_earn);
        TextView deleteAccountButton = view.findViewById(R.id.textView43);

        // Set up spinners
        setupSpinners();
        setupEditListeners();
        setupGenderListener();
        setupShareButton();
        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());

        // Load profile data
        loadProfileData();

        return view;
    }

    private void setupSpinners() {
        // Age Group Spinner
        setupSpinner(spinnerAgeGroup, ageGroupOptions, "ageGroup");

        // Twelfth Stream Spinner
        setupSpinner(spinnerTwelfth, DataConstants.TWELFTH_OPTIONS, "twelfth");

        // Education Category Spinner
        ArrayAdapter<String> educationAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, DataConstants.EDUCATION_OPTIONS);
        educationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEducation.setAdapter(educationAdapter);

        // Initialize default adapters for Degree and Post Grad
        degreeAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, new ArrayList<>(Arrays.asList("Select Degree")));
        degreeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDegree.setAdapter(degreeAdapter);

        postGradAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, new ArrayList<>(Arrays.asList("Select Post Graduation")));
        postGradAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPostGrad.setAdapter(postGradAdapter);

        // District Spinner
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, DataConstants.DISTRICTS);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDistrict.setAdapter(districtAdapter);

        // Taluka Spinner
        talukaAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, new ArrayList<>(Arrays.asList("Select Taluka")));
        talukaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTaluka.setAdapter(talukaAdapter);
        spinnerTaluka.setEnabled(false);
        spinnerTaluka.setAlpha(0.5f);

        // Education Spinner Listener (to update Degree and Post Grad)
        spinnerEducation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isReverting) return;
                String selectedEducation = DataConstants.EDUCATION_OPTIONS.get(position);
                String currentEducation = sharedPreferences.getString("education", DataConstants.EDUCATION_OPTIONS.get(0));
                if (!selectedEducation.equals(currentEducation) && !selectedEducation.equals("Select Education Category")) {
                    showConfirmationDialog("education", selectedEducation, () -> {
                        spinnerEducation.setSelection(position);
                        saveToSharedPreferences("education", selectedEducation);
                        saveToFirestore("education", selectedEducation);
                        updateDegreeAndPostGradSpinners(selectedEducation);
                        saveToSharedPreferences("degree", "Select Degree");
                        saveToFirestore("degree", "Select Degree");
                        saveToSharedPreferences("postGraduation", "Select Post Graduation");
                        saveToFirestore("postGraduation", "Select Post Graduation");
                        updateSQLiteUser();
                    }, () -> {
                        isReverting = true;
                        spinnerEducation.setSelection(DataConstants.EDUCATION_OPTIONS.indexOf(currentEducation));
                        isReverting = false;
                    });
                } else {
                    updateDegreeAndPostGradSpinners(selectedEducation);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateDegreeAndPostGradSpinners(DataConstants.EDUCATION_OPTIONS.get(0));
            }
        });

        // Degree Spinner Listener
        spinnerDegree.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isReverting) return;
                String selectedDegree = degreeAdapter.getItem(position);
                String currentDegree = sharedPreferences.getString("degree", "Select Degree");
                if (!selectedDegree.equals(currentDegree) && !selectedDegree.equals("Select Degree")) {
                    showConfirmationDialog("degree", selectedDegree, () -> {
                        spinnerDegree.setSelection(position);
                        saveToSharedPreferences("degree", selectedDegree);
                        saveToFirestore("degree", selectedDegree);
                        updateSQLiteUser();
                    }, () -> {
                        isReverting = true;
                        List<String> degrees = new ArrayList<>(DataConstants.DEGREE_MAP.getOrDefault(
                                sharedPreferences.getString("education", "Select Education Category"),
                                Arrays.asList("Select Degree")));
                        spinnerDegree.setSelection(degrees.indexOf(currentDegree));
                        isReverting = false;
                    });
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Post Graduation Spinner Listener
        spinnerPostGrad.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isReverting) return;
                String selectedPostGrad = postGradAdapter.getItem(position);
                String currentPostGrad = sharedPreferences.getString("postGraduation", "Select Post Graduation");
                if (!selectedPostGrad.equals(currentPostGrad) && !selectedPostGrad.equals("Select Post Graduation")) {
                    showConfirmationDialog("postGraduation", selectedPostGrad, () -> {
                        spinnerPostGrad.setSelection(position);
                        saveToSharedPreferences("postGraduation", selectedPostGrad);
                        saveToFirestore("postGraduation", selectedPostGrad);
                        updateSQLiteUser();
                    }, () -> {
                        isReverting = true;
                        List<String> postGrads = new ArrayList<>(DataConstants.POST_GRAD_MAP.getOrDefault(
                                sharedPreferences.getString("education", "Select Education Category"),
                                Arrays.asList("Select Post Graduation")));
                        spinnerPostGrad.setSelection(postGrads.indexOf(currentPostGrad));
                        isReverting = false;
                    });
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // District Spinner Listener
        spinnerDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isReverting) return;
                String selectedDistrict = DataConstants.DISTRICTS.get(position);
                String currentDistrict = sharedPreferences.getString("district", DataConstants.DISTRICTS.get(0));
                if (!selectedDistrict.equals(currentDistrict) && !selectedDistrict.equals("Select District")) {
                    showConfirmationDialog("district", selectedDistrict, () -> {
                        spinnerDistrict.setSelection(position);
                        saveToSharedPreferences("district", selectedDistrict);
                        saveToFirestore("district", selectedDistrict);
                        updateTalukaSpinner(selectedDistrict);
                        saveToSharedPreferences("taluka", "Select Taluka");
                        saveToFirestore("taluka", "Select Taluka");
                        updateSQLiteUser();
                    }, () -> {
                        isReverting = true;
                        spinnerDistrict.setSelection(DataConstants.DISTRICTS.indexOf(currentDistrict));
                        isReverting = false;
                    });
                } else {
                    updateTalukaSpinner(selectedDistrict);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateTalukaSpinner(DataConstants.DISTRICTS.get(0));
            }
        });

        // Taluka Spinner Listener
        spinnerTaluka.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isReverting) return;
                String selectedTaluka = talukaAdapter.getItem(position);
                String currentTaluka = sharedPreferences.getString("taluka", "Select Taluka");
                if (!selectedTaluka.equals(currentTaluka) && !selectedTaluka.equals("Select Taluka")) {
                    showConfirmationDialog("taluka", selectedTaluka, () -> {
                        spinnerTaluka.setSelection(position);
                        saveToSharedPreferences("taluka", selectedTaluka);
                        saveToFirestore("taluka", selectedTaluka);
                        updateSQLiteUser();
                    }, () -> {
                        isReverting = true;
                        List<String> talukas = new ArrayList<>(DataConstants.TALUKA_MAP.getOrDefault(
                                sharedPreferences.getString("district", "Select District"),
                                Arrays.asList("Select Taluka")));
                        spinnerTaluka.setSelection(talukas.indexOf(currentTaluka));
                        isReverting = false;
                    });
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void updateDegreeAndPostGradSpinners(String education) {
        // Create mutable lists to avoid UnsupportedOperationException
        List<String> degrees = new ArrayList<>(DataConstants.DEGREE_MAP.getOrDefault(education, Arrays.asList("Select Degree")));
        List<String> postGrads = new ArrayList<>(DataConstants.POST_GRAD_MAP.getOrDefault(education, Arrays.asList("Select Post Graduation")));

        degreeAdapter.clear();
        degreeAdapter.addAll(degrees);
        degreeAdapter.notifyDataSetChanged();

        postGradAdapter.clear();
        postGradAdapter.addAll(postGrads);
        postGradAdapter.notifyDataSetChanged();

        String savedDegree = sharedPreferences.getString("degree", degrees.get(0));
        String savedPostGrad = sharedPreferences.getString("postGraduation", postGrads.get(0));

        int degreePosition = degrees.indexOf(savedDegree);
        isReverting = true;
        spinnerDegree.setSelection(degreePosition >= 0 ? degreePosition : 0);
        isReverting = false;

        int postGradPosition = postGrads.indexOf(savedPostGrad);
        isReverting = true;
        spinnerPostGrad.setSelection(postGradPosition >= 0 ? postGradPosition : 0);
        isReverting = false;
    }

    private void updateTalukaSpinner(String district) {
        List<String> talukas = new ArrayList<>(DataConstants.TALUKA_MAP.getOrDefault(district, Arrays.asList("Select Taluka")));
        talukaAdapter.clear();
        talukaAdapter.addAll(talukas);
        talukaAdapter.notifyDataSetChanged();

        if (district.equals("Select District")) {
            spinnerTaluka.setEnabled(false);
            spinnerTaluka.setAlpha(0.5f);
            isReverting = true;
            spinnerTaluka.setSelection(0);
            isReverting = false;
        } else {
            spinnerTaluka.setEnabled(true);
            spinnerTaluka.setAlpha(1.0f);
            String savedTaluka = sharedPreferences.getString("taluka", talukas.get(0));
            int talukaPosition = talukas.indexOf(savedTaluka);
            isReverting = true;
            spinnerTaluka.setSelection(talukaPosition >= 0 ? talukaPosition : 0);
            isReverting = false;
        }
    }

    private void setupSpinner(Spinner spinner, List<String> options, String prefKey) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item, new ArrayList<>(options));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        String savedValue = sharedPreferences.getString(prefKey, options.get(0));
        int position = options.indexOf(savedValue);
        if (position >= 0) {
            isReverting = true;
            spinner.setSelection(position);
            isReverting = false;
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isReverting) return;
                String selectedValue = options.get(position);
                String currentValue = sharedPreferences.getString(prefKey, options.get(0));
                if (!selectedValue.equals(currentValue) && !selectedValue.equals(options.get(0))) {
                    showConfirmationDialog(prefKey, selectedValue, () -> {
                        spinner.setSelection(position);
                        saveToSharedPreferences(prefKey, selectedValue);
                        saveToFirestore(prefKey, selectedValue);
                        updateSQLiteUser();
                    }, () -> {
                        isReverting = true;
                        spinner.setSelection(options.indexOf(currentValue));
                        isReverting = false;
                    });
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupEditListeners() {
        profileNameEdit.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Edit Name");
            final EditText input = new EditText(requireContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setText(sharedPreferences.getString("userName", "User"));
            builder.setView(input);
            builder.setPositiveButton("OK", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty() && !newName.equals(sharedPreferences.getString("userName", "User"))) {
                    showConfirmationDialog("userName", newName, () -> {
                        profileName.setText(newName);
                        saveToSharedPreferences("userName", newName);
                        saveToFirestore("name", newName); // Firestore uses "name" instead of "userName"
                        updateSQLiteUser();
                    }, () -> {});
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        });

        currentAffairsEdit.setOnClickListener(v -> showRadioDialog("currentAffairs", "करंट अफेअर्स नोटिफिकेशन",
                sharedPreferences.getBoolean("currentAffairs", false)));

        jobsEdit.setOnClickListener(v -> showRadioDialog("jobs", "दहावी,बारावी आधारित जॉब",
                sharedPreferences.getBoolean("jobs", false)));

        studyMaterialEdit.setOnClickListener(v -> showStudyMaterialDialog());
    }

    private void showStudyMaterialDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("MPSC/UPSC स्टडी मटेरियल");
        String[] options = {"MPSC", "UPSC"};
        boolean[] checkedItems = new boolean[]{
                sharedPreferences.getBoolean("mpsc", false),
                sharedPreferences.getBoolean("upsc", false)
        };
        builder.setMultiChoiceItems(options, checkedItems, (dialog, which, isChecked) -> checkedItems[which] = isChecked);
        builder.setPositiveButton("OK", (dialog, which) -> {
            boolean mpscSelected = checkedItems[0];
            boolean upscSelected = checkedItems[1];
            if (mpscSelected != sharedPreferences.getBoolean("mpsc", false) ||
                    upscSelected != sharedPreferences.getBoolean("upsc", false)) {
                showConfirmationDialog("studyMaterial", getStudyMaterialDisplayText(mpscSelected, upscSelected), () -> {
                    editor.putBoolean("mpsc", mpscSelected);
                    editor.putBoolean("upsc", upscSelected);
                    editor.apply();
                    studyMaterialSelection.setText(getStudyMaterialDisplayText(mpscSelected, upscSelected));
                    saveToFirestore("mpsc", mpscSelected);
                    saveToFirestore("upsc", upscSelected);
                    updateSQLiteUser();
                }, () -> {});
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private String getStudyMaterialDisplayText(boolean mpsc, boolean upsc) {
        if (mpsc && upsc) return "MPSC, UPSC";
        if (mpsc) return "MPSC";
        if (upsc) return "UPSC";
        return "None";
    }

    private void setupGenderListener() {
        radioGroupGender.setOnCheckedChangeListener((group, checkedId) -> {
            if (isReverting) return;
            String selectedGender = checkedId == R.id.radio_men ? "पुरुष" : "स्त्री";
            String currentGender = sharedPreferences.getString("gender", "");
            if (!selectedGender.equals(currentGender)) {
                showConfirmationDialog("gender", selectedGender, () -> {
                    saveToSharedPreferences("gender", selectedGender);
                    saveToFirestore("gender", selectedGender);
                    updateSQLiteUser();
                }, () -> {
                    isReverting = true;
                    radioGroupGender.check(currentGender.equals("पुरुष") ? R.id.radio_men : R.id.radio_women);
                    isReverting = false;
                });
            }
        });
    }

    private void showRadioDialog(String prefKey, String title, boolean currentValue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(title);
        String[] options = {"हो", "नाही"};
        int checkedItem = currentValue ? 0 : 1;
        builder.setSingleChoiceItems(options, checkedItem, (dialog, which) -> {});
        builder.setPositiveButton("OK", (dialog, which) -> {
            int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
            boolean selectedValue = selectedPosition == 0;
            if (selectedValue != currentValue) {
                showConfirmationDialog(prefKey, selectedValue ? "हो" : "नाही", () -> {
                    if (prefKey.equals("currentAffairs")) {
                        currentAffairsSelection.setText(selectedValue ? "हो" : "नाही");
                    } else {
                        jobsSelection.setText(selectedValue ? "हो" : "नाही");
                    }
                    editor.putBoolean(prefKey, selectedValue);
                    editor.apply();
                    saveToFirestore(prefKey, selectedValue);
                    updateSQLiteUser();
                }, () -> {});
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showConfirmationDialog(String key, String value, Runnable onConfirm, Runnable onCancel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Confirm Change");
        builder.setMessage("Are you sure you want to change " + getFieldName(key) + " to " + value + "?");
        builder.setPositiveButton("Yes", (dialog, which) -> onConfirm.run());
        builder.setNegativeButton("No", (dialog, which) -> onCancel.run());
        builder.show();
    }

    private String getFieldName(String key) {
        switch (key) {
            case "userName": return "Name";
            case "gender": return "Gender";
            case "studyMaterial": return "MPSC/UPSC Study Material";
            case "degree": return "Bachelor's Degree";
            case "twelfth": return "12th Stream";
            case "postGraduation": return "Master's Degree";
            case "district": return "District";
            case "taluka": return "Taluka";
            case "ageGroup": return "Age Group";
            case "education": return "Education Category";
            case "currentAffairs": return "Current Affairs Notifications";
            case "jobs": return "10th/12th Based Jobs";
            default: return key;
        }
    }

    private void saveToSharedPreferences(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    private void saveToFirestore(String key, String value) {
        if (userId != null) {
            Map<String, Object> updates = new HashMap<>();
            String firestoreKey = key.equals("userName") ? "name" : key;
            updates.put(firestoreKey, value);
            db.collection("users").document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(requireContext(), "User ID not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToFirestore(String key, boolean value) {
        if (userId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put(key, value);
            db.collection("users").document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(requireContext(), "User ID not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSQLiteUser() {
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(requireContext(), "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = new User(
                userId,
                sharedPreferences.getString("userName", "User"),
                sharedPreferences.getString("gender", ""),
                sharedPreferences.getString("avatar", "girl_profile"),
                sharedPreferences.getBoolean("upsc", false),
                sharedPreferences.getBoolean("mpsc", false),
                sharedPreferences.getString("degree", "Select Degree"),
                sharedPreferences.getString("postGraduation", "Select Post Graduation"),
                sharedPreferences.getString("district", "Select District"),
                sharedPreferences.getString("taluka", "Select Taluka"),
                sharedPreferences.getBoolean("currentAffairs", false),
                sharedPreferences.getBoolean("jobs", false),
                sharedPreferences.getString("ageGroup", "Select Age Group"),
                sharedPreferences.getString("education", "Select Education Category"),
                sharedPreferences.getString("twelfth", "Select 12th Stream")
        );

        try {
            dbHelper.updateUser(user);
            Toast.makeText(requireContext(), "Local database updated", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to update local database: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadProfileData() {
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(requireContext(), "User ID not found. Please log in.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
            return;
        }

        User user = dbHelper.getUser(userId);
        if (user != null) {
            profileName.setText(user.getName() != null ? user.getName() : "User");

            // Load avatar
            String avatar = user.getAvatar() != null ? user.getAvatar() : "girl_profile";
            try {
                int resId = getResources().getIdentifier(avatar, "drawable", requireContext().getPackageName());
                profileImage.setImageResource(resId != 0 ? resId : R.drawable.girl_profile);
            } catch (Exception e) {
                profileImage.setImageResource(R.drawable.girl_profile);
                Toast.makeText(requireContext(), "Error loading avatar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            // Gender
            if (user.getGender() != null) {
                isReverting = true;
                radioGroupGender.check(user.getGender().equals("पुरुष") ? R.id.radio_men : R.id.radio_women);
                isReverting = false;
            }

            // Spinners
            setSpinnerSelection(spinnerAgeGroup, ageGroupOptions, user.getAgeGroup());
            setSpinnerSelection(spinnerTwelfth, DataConstants.TWELFTH_OPTIONS, user.getTwelfth());
            setSpinnerSelection(spinnerEducation, DataConstants.EDUCATION_OPTIONS, user.getEducation());
            updateDegreeAndPostGradSpinners(user.getEducation() != null ? user.getEducation() : "Select Education Category");
            setSpinnerSelection(spinnerDegree, new ArrayList<>(DataConstants.DEGREE_MAP.getOrDefault(user.getEducation(), Arrays.asList("Select Degree"))), user.getDegree());
            setSpinnerSelection(spinnerPostGrad, new ArrayList<>(DataConstants.POST_GRAD_MAP.getOrDefault(user.getEducation(), Arrays.asList("Select Post Graduation"))), user.getPostGraduation());
            setSpinnerSelection(spinnerDistrict, DataConstants.DISTRICTS, user.getDistrict());
            updateTalukaSpinner(user.getDistrict() != null ? user.getDistrict() : "Select District");
            setSpinnerSelection(spinnerTaluka, new ArrayList<>(DataConstants.TALUKA_MAP.getOrDefault(user.getDistrict(), Arrays.asList("Select Taluka"))), user.getTaluka());

            // Other fields
            currentAffairsSelection.setText(user.isCurrentAffairs() ? "हो" : "नाही");
            jobsSelection.setText(user.isJobs() ? "हो" : "नाही");
            studyMaterialSelection.setText(getStudyMaterialDisplayText(user.isMpsc(), user.isUpsc()));
            coinTextView.setText(String.valueOf(dbHelper.getUserCoins(userId)));
        } else {
            Toast.makeText(requireContext(), "User data not found in database. Using saved preferences.", Toast.LENGTH_SHORT).show();
            String userName = sharedPreferences.getString("userName", "User");
            profileName.setText(userName);

            // Load avatar
            String avatar = sharedPreferences.getString("avatar", "girl_profile");
            try {
                int resId = getResources().getIdentifier(avatar, "drawable", requireContext().getPackageName());
                profileImage.setImageResource(resId != 0 ? resId : R.drawable.girl_profile);
            } catch (Exception e) {
                profileImage.setImageResource(R.drawable.girl_profile);
                Toast.makeText(requireContext(), "Error loading avatar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            // Gender
            String gender = sharedPreferences.getString("gender", "");
            isReverting = true;
            radioGroupGender.check(gender.equals("पुरुष") ? R.id.radio_men : gender.equals("स्त्री") ? R.id.radio_women : -1);
            isReverting = false;

            // Spinners
            String education = sharedPreferences.getString("education", "Select Education Category");
            setSpinnerSelection(spinnerAgeGroup, ageGroupOptions, sharedPreferences.getString("ageGroup", "Select Age Group"));
            setSpinnerSelection(spinnerTwelfth, DataConstants.TWELFTH_OPTIONS, sharedPreferences.getString("twelfth", "Select 12th Stream"));
            setSpinnerSelection(spinnerEducation, DataConstants.EDUCATION_OPTIONS, education);
            updateDegreeAndPostGradSpinners(education);
            setSpinnerSelection(spinnerDegree, new ArrayList<>(DataConstants.DEGREE_MAP.getOrDefault(education, Arrays.asList("Select Degree"))), sharedPreferences.getString("degree", "Select Degree"));
            setSpinnerSelection(spinnerPostGrad, new ArrayList<>(DataConstants.POST_GRAD_MAP.getOrDefault(education, Arrays.asList("Select Post Graduation"))), sharedPreferences.getString("postGraduation", "Select Post Graduation"));
            String district = sharedPreferences.getString("district", "Select District");
            setSpinnerSelection(spinnerDistrict, DataConstants.DISTRICTS, district);
            updateTalukaSpinner(district);
            setSpinnerSelection(spinnerTaluka, new ArrayList<>(DataConstants.TALUKA_MAP.getOrDefault(district, Arrays.asList("Select Taluka"))), sharedPreferences.getString("taluka", "Select Taluka"));

            // Other fields
            currentAffairsSelection.setText(sharedPreferences.getBoolean("currentAffairs", false) ? "हो" : "नाही");
            jobsSelection.setText(sharedPreferences.getBoolean("jobs", false) ? "हो" : "नाही");
            studyMaterialSelection.setText(getStudyMaterialDisplayText(
                    sharedPreferences.getBoolean("mpsc", false),
                    sharedPreferences.getBoolean("upsc", false)));
            coinTextView.setText(String.valueOf(dbHelper.getUserCoins(userId)));
        }
    }

    private void setSpinnerSelection(Spinner spinner, List<String> options, String value) {
        if (value != null) {
            int position = options.indexOf(value);
            if (position >= 0) {
                isReverting = true;
                spinner.setSelection(position);
                isReverting = false;
            } else {
                isReverting = true;
                spinner.setSelection(0);
                isReverting = false;
            }
        }
    }

    private void showDeleteAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Delete Account");
        builder.setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.");
        builder.setPositiveButton("Yes", (dialog, which) -> deleteAccount());
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void deleteAccount() {
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(requireContext(), "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    boolean deletedFromSQLite = dbHelper.deleteUser(userId);
                    if (deletedFromSQLite) {
                        editor.clear();
                        editor.putBoolean("isLoggedIn", false);
                        editor.apply();
                        Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(requireContext(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    } else {
                        Toast.makeText(requireContext(), "Failed to delete account from local database", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to delete account from Firestore", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupShareButton() {
        shareButtonContainer.setOnClickListener(v -> {
            try {
                Uri imageUri = generateBannerWithText();
                String shortUrl = "https://bit.ly/yourapp-profile";
                String shareMessage = "Join our app: " + shortUrl;
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/*");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setPackage("com.whatsapp");
                startActivityForResult(shareIntent, 100);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Error sharing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Uri generateBannerWithText() {
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.student_update_1);
            Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setTextSize(50);
            paint.setAntiAlias(true);
            paint.setTextAlign(Paint.Align.CENTER);
            String text = "Download this app";
            float x = mutableBitmap.getWidth() / 2f;
            float y = mutableBitmap.getHeight() - 100;
            canvas.drawText(text, x, y, paint);
            File cacheDir = requireContext().getCacheDir();
            File imageFile = new File(cacheDir, "banner_with_text.png");
            try (FileOutputStream out = new FileOutputStream(imageFile)) {
                mutableBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            return FileProvider.getUriForFile(
                    requireContext(),
                    "com.newsproject.oneroadmap.fileprovider",
                    imageFile
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate banner: " + e.getMessage());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            int currentCoins = dbHelper.getUserCoins(userId);
            int newCoins = currentCoins + 100;
            saveCoinsToFirestore(newCoins);
            dbHelper.updateUserCoins(userId, newCoins);
            showCoinAnimationDialog(currentCoins, newCoins);
        }
    }

    private void showCoinAnimationDialog(int startCoins, int endCoins) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.coin_dialog_layout, null);
        TextView coinCountText = dialogView.findViewById(R.id.coin_count);
        Button okButton = dialogView.findViewById(R.id.ok_button);
        coinCountText.setText("Coins: " + startCoins);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        okButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        displayedCoins = startCoins;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (displayedCoins < endCoins) {
                    displayedCoins++;
                    coinCountText.setText("Coins: " + displayedCoins);
                    handler.postDelayed(this, 20);
                } else {
                    coinTextView.setText(String.valueOf(endCoins));
                }
            }
        });
    }

    private void saveCoinsToFirestore(int coins) {
        if (userId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("coins", coins);
            db.collection("users").document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Coins updated", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to update coins", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(requireContext(), "User ID not found", Toast.LENGTH_SHORT).show();
        }
    }
}