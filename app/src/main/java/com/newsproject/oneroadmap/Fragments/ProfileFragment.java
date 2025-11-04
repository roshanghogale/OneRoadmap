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
import android.util.Log;
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

    private TextView profileName, currentAffairsSelection, jobsSelection, coinTextView,
            studyMaterialSelection, profileTxtJobByStream;          // <-- NEW
    private ImageView profileNameEdit, currentAffairsEdit, jobsEdit, profileImage, studyMaterialEdit;
    private LinearLayout shareButtonContainer;
    private RadioGroup radioGroupGender;
    private Spinner spinnerEducation, spinnerTwelfth, spinnerDegree, spinnerPostGrad,
            spinnerDistrict, spinnerTaluka, spinnerAgeGroup;
    private ArrayAdapter<String> talukaAdapter, degreeAdapter, postGradAdapter;
    private boolean isReverting = false;
    private Handler handler = new Handler();
    private int displayedCoins = 0;

    private final String[] studyMaterialOptions = {
            "Government", "Police & Defence", "Banking", "Self Improvement"
    };
    private final List<String> ageGroupOptions = Arrays.asList(
            "Select Age Group", "18-25", "26-35", "36-45", "Above 45");

    /* --------------------------------------------------------------------- */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        userId = sharedPreferences.getString("userId", null);
        db = FirebaseFirestore.getInstance();
        dbHelper = new DatabaseHelper(requireContext());

        // One-time migration from old mpsc/upsc keys
        migrateOldStudyPreferences();
    }

    private void migrateOldStudyPreferences() {
        if (sharedPreferences.contains("mpsc") || sharedPreferences.contains("upsc")) {
            boolean mpsc = sharedPreferences.getBoolean("mpsc", false);
            boolean upsc = sharedPreferences.getBoolean("upsc", false);
            editor.putBoolean("study_Government", mpsc);
            editor.putBoolean("study_Police___Defence", upsc);
            editor.remove("mpsc").remove("upsc").apply();
        }
    }

    /* --------------------------------------------------------------------- */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // ----- UI INITIALISATION -------------------------------------------------
        profileName               = view.findViewById(R.id.profile_name);
        profileNameEdit           = view.findViewById(R.id.imageView22);
        profileImage              = view.findViewById(R.id.imageView19);
        radioGroupGender          = view.findViewById(R.id.radioGroupGender);
        spinnerEducation          = view.findViewById(R.id.spinnerEducation);
        spinnerTwelfth            = view.findViewById(R.id.spinnerTwelfth);
        spinnerDegree             = view.findViewById(R.id.spinnerDegree);
        spinnerPostGrad           = view.findViewById(R.id.spinnerPostGraduation);
        spinnerDistrict           = view.findViewById(R.id.spinnerDistrict);
        spinnerTaluka             = view.findViewById(R.id.spinnerTaluka);
        spinnerAgeGroup           = view.findViewById(R.id.spinnerAgeGroup);
        currentAffairsSelection   = view.findViewById(R.id.current_affairs_selection);
        currentAffairsEdit        = view.findViewById(R.id.profile_current_affairs_edit);
        jobsSelection             = view.findViewById(R.id.profile_10th_and_12th_jobs_selection);
        jobsEdit                  = view.findViewById(R.id.profile_10th_and_12th_jobs_edit);
        studyMaterialSelection    = view.findViewById(R.id.study_material_selection);
        studyMaterialEdit         = view.findViewById(R.id.profile_study_material_edit);
        coinTextView              = view.findViewById(R.id.textView25);
        shareButtonContainer      = view.findViewById(R.id.linearLayout_share_earn);
        profileTxtJobByStream     = view.findViewById(R.id.profileTxtJobByStream);   // NEW
        TextView deleteAccountBtn = view.findViewById(R.id.textView43);

        // ----- SETUP ------------------------------------------------------------
        setupSpinners();
        setupEditListeners();
        setupGenderListener();
        setupShareButton();
        deleteAccountBtn.setOnClickListener(v -> showDeleteAccountDialog());

        // ----- LOAD DATA --------------------------------------------------------
        loadProfileData();

        return view;
    }

    /* --------------------------------------------------------------------- */
    private void setupSpinners() {
        // Age Group
        setupSpinner(spinnerAgeGroup, ageGroupOptions, "ageGroup");

        // Twelfth Stream
        setupSpinner(spinnerTwelfth, DataConstants.TWELFTH_OPTIONS, "twelfth");

        // ---- Twelfth listener – controls advanced education & job text ----------
        spinnerTwelfth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean enableAdvanced = (position == 3); // “नाही या पुढील शिक्षण आहे”
                setAdvancedEducationEnabled(enableAdvanced);

                // ---- UPDATE JOB TEXT (same logic as LoginPage3) -----------------
                updateJobTextByTwelfth(DataConstants.TWELFTH_OPTIONS.get(position));

                if (!enableAdvanced) {
                    spinnerEducation.setSelection(0);
                    spinnerDegree.setSelection(0);
                    spinnerPostGrad.setSelection(0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                setAdvancedEducationEnabled(false);
                updateJobTextByTwelfth(DataConstants.TWELFTH_OPTIONS.get(0));
            }
        });

        // Education Category
        ArrayAdapter<String> educationAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item, DataConstants.EDUCATION_OPTIONS);
        educationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEducation.setAdapter(educationAdapter);

        // Degree & PostGrad default adapters
        degreeAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item,
                new ArrayList<>(Arrays.asList("Select Degree")));
        degreeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDegree.setAdapter(degreeAdapter);

        postGradAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item,
                new ArrayList<>(Arrays.asList("Select Post Graduation")));
        postGradAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPostGrad.setAdapter(postGradAdapter);

        // District
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item, DataConstants.DISTRICTS);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDistrict.setAdapter(districtAdapter);

        // Taluka
        talukaAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item,
                new ArrayList<>(Arrays.asList("Select Taluka")));
        talukaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTaluka.setAdapter(talukaAdapter);
        spinnerTaluka.setEnabled(false);
        spinnerTaluka.setAlpha(0.5f);

        // Education listener
        spinnerEducation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isReverting) return;
                String selected = DataConstants.EDUCATION_OPTIONS.get(position);
                String current  = sharedPreferences.getString("education",
                        DataConstants.EDUCATION_OPTIONS.get(0));

                if (!selected.equals(current) && !selected.equals("Select Education Category")) {
                    showConfirmationDialog("education", selected, () -> {
                        spinnerEducation.setSelection(position);
                        saveToSharedPreferences("education", selected);
                        saveToFirestore("education", selected);
                        updateDegreeAndPostGradSpinners(selected);
                        saveToSharedPreferences("degree", "Select Degree");
                        saveToFirestore("degree", "Select Degree");
                        saveToSharedPreferences("postGraduation", "Select Post Graduation");
                        saveToFirestore("postGraduation", "Select Post Graduation");
                        updateSQLiteUser();
                    }, () -> {
                        isReverting = true;
                        spinnerEducation.setSelection(
                                DataConstants.EDUCATION_OPTIONS.indexOf(current));
                        isReverting = false;
                    });
                } else {
                    updateDegreeAndPostGradSpinners(selected);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateDegreeAndPostGradSpinners(DataConstants.EDUCATION_OPTIONS.get(0));
            }
        });

        // Other spinners
        spinnerDegree.setOnItemSelectedListener(createDegreeListener());
        spinnerPostGrad.setOnItemSelectedListener(createPostGradListener());
        spinnerDistrict.setOnItemSelectedListener(createDistrictListener());
        spinnerTaluka.setOnItemSelectedListener(createTalukaListener());
    }

    /* --------------------------------------------------------------------- */
    /** Same logic as LoginPage3 – updates the job label text */
    private void updateJobTextByTwelfth(String twelfth) {
        String savedJobText = sharedPreferences.getString("jobTextByTwelfth", "");
        String jobText = "";

        if (!savedJobText.isEmpty()) {
            jobText = savedJobText;
        } else {
            int idx = DataConstants.TWELFTH_OPTIONS.indexOf(twelfth);
            if (idx >= 0 && idx < DataConstants.JOB_TEXT_BY_TWELFTH.size()) {
                jobText = DataConstants.JOB_TEXT_BY_TWELFTH.get(idx);
            }
        }

        if (!jobText.isEmpty()) {
            profileTxtJobByStream.setText("तुम्हाला " + jobText);
        } else {
            profileTxtJobByStream.setText("दहावी,बारावी आधारित जॉब पाहिजेत ?");
        }
    }

    /* --------------------------------------------------------------------- */
    private void setAdvancedEducationEnabled(boolean enabled) {
        spinnerEducation.setEnabled(enabled);
        spinnerDegree.setEnabled(enabled);
        spinnerPostGrad.setEnabled(enabled);
        float alpha = enabled ? 1.0f : 0.5f;
        spinnerEducation.setAlpha(alpha);
        spinnerDegree.setAlpha(alpha);
        spinnerPostGrad.setAlpha(alpha);
    }

    private void updateDegreeAndPostGradSpinners(String education) {
        List<String> degrees = new ArrayList<>(
                DataConstants.DEGREE_MAP.getOrDefault(education,
                        Arrays.asList("Select Degree")));
        List<String> postGrads = new ArrayList<>(
                DataConstants.POST_GRAD_MAP.getOrDefault(education,
                        Arrays.asList("Select Post Graduation")));

        degreeAdapter.clear(); degreeAdapter.addAll(degrees); degreeAdapter.notifyDataSetChanged();
        postGradAdapter.clear(); postGradAdapter.addAll(postGrads); postGradAdapter.notifyDataSetChanged();

        String savedDegree = sharedPreferences.getString("degree", degrees.get(0));
        String savedPostGrad = sharedPreferences.getString("postGraduation", postGrads.get(0));

        isReverting = true;
        spinnerDegree.setSelection(degrees.indexOf(savedDegree) >= 0 ? degrees.indexOf(savedDegree) : 0);
        spinnerPostGrad.setSelection(postGrads.indexOf(savedPostGrad) >= 0 ? postGrads.indexOf(savedPostGrad) : 0);
        isReverting = false;
    }

    private void updateTalukaSpinner(String district) {
        List<String> talukas = new ArrayList<>(
                DataConstants.TALUKA_MAP.getOrDefault(district,
                        Arrays.asList("Select Taluka")));
        talukaAdapter.clear(); talukaAdapter.addAll(talukas); talukaAdapter.notifyDataSetChanged();

        if (district.equals("Select District")) {
            spinnerTaluka.setEnabled(false); spinnerTaluka.setAlpha(0.5f);
            isReverting = true; spinnerTaluka.setSelection(0); isReverting = false;
        } else {
            spinnerTaluka.setEnabled(true); spinnerTaluka.setAlpha(1.0f);
            String saved = sharedPreferences.getString("taluka", talukas.get(0));
            isReverting = true;
            spinnerTaluka.setSelection(talukas.indexOf(saved) >= 0 ? talukas.indexOf(saved) : 0);
            isReverting = false;
        }
    }

    /* --------------------------------------------------------------------- */
    private void setupSpinner(Spinner spinner, List<String> options, String prefKey) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item, new ArrayList<>(options));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        String saved = sharedPreferences.getString(prefKey, options.get(0));
        int pos = options.indexOf(saved);
        if (pos >= 0) { isReverting = true; spinner.setSelection(pos); isReverting = false; }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isReverting) return;
                String selected = options.get(position);
                String current  = sharedPreferences.getString(prefKey, options.get(0));
                if (!selected.equals(current) && !selected.equals(options.get(0))) {
                    showConfirmationDialog(prefKey, selected, () -> {
                        spinner.setSelection(position);
                        saveToSharedPreferences(prefKey, selected);
                        saveToFirestore(prefKey, selected);
                        updateSQLiteUser();
                    }, () -> {
                        isReverting = true;
                        spinner.setSelection(options.indexOf(current));
                        isReverting = false;
                    });
                }
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /* --------------------------------------------------------------------- */
    private AdapterView.OnItemSelectedListener createDegreeListener() {
        return new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (isReverting) return;
                String selected = degreeAdapter.getItem(pos);
                String current  = sharedPreferences.getString("degree", "Select Degree");
                if (!selected.equals(current) && !selected.equals("Select Degree")) {
                    showConfirmationDialog("degree", selected, () -> {
                        spinnerDegree.setSelection(pos);
                        saveToSharedPreferences("degree", selected);
                        saveToFirestore("degree", selected);
                        updateSQLiteUser();
                    }, () -> {
                        isReverting = true;
                        List<String> degrees = new ArrayList<>(
                                DataConstants.DEGREE_MAP.getOrDefault(
                                        sharedPreferences.getString("education",
                                                "Select Education Category"),
                                        Arrays.asList("Select Degree")));
                        spinnerDegree.setSelection(degrees.indexOf(current));
                        isReverting = false;
                    });
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
    }

    private AdapterView.OnItemSelectedListener createPostGradListener() {
        return new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (isReverting) return;
                String selected = postGradAdapter.getItem(pos);
                String current  = sharedPreferences.getString("postGraduation", "Select Post Graduation");
                if (!selected.equals(current) && !selected.equals("Select Post Graduation")) {
                    showConfirmationDialog("postGraduation", selected, () -> {
                        spinnerPostGrad.setSelection(pos);
                        saveToSharedPreferences("postGraduation", selected);
                        saveToFirestore("postGraduation", selected);
                        updateSQLiteUser();
                    }, () -> {
                        isReverting = true;
                        List<String> postGrads = new ArrayList<>(
                                DataConstants.POST_GRAD_MAP.getOrDefault(
                                        sharedPreferences.getString("education",
                                                "Select Education Category"),
                                        Arrays.asList("Select Post Graduation")));
                        spinnerPostGrad.setSelection(postGrads.indexOf(current));
                        isReverting = false;
                    });
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
    }

    private AdapterView.OnItemSelectedListener createDistrictListener() {
        return new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (isReverting) return;
                String selected = DataConstants.DISTRICTS.get(pos);
                String current  = sharedPreferences.getString("district",
                        DataConstants.DISTRICTS.get(0));
                if (!selected.equals(current) && !selected.equals("Select District")) {
                    showConfirmationDialog("district", selected, () -> {
                        spinnerDistrict.setSelection(pos);
                        saveToSharedPreferences("district", selected);
                        saveToFirestore("district", selected);
                        updateTalukaSpinner(selected);
                        saveToSharedPreferences("taluka", "Select Taluka");
                        saveToFirestore("taluka", "Select Taluka");
                        updateSQLiteUser();
                    }, () -> {
                        isReverting = true;
                        spinnerDistrict.setSelection(DataConstants.DISTRICTS.indexOf(current));
                        isReverting = false;
                    });
                } else {
                    updateTalukaSpinner(selected);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                updateTalukaSpinner(DataConstants.DISTRICTS.get(0));
            }
        };
    }

    private AdapterView.OnItemSelectedListener createTalukaListener() {
        return new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (isReverting) return;
                String selected = talukaAdapter.getItem(pos);
                String current  = sharedPreferences.getString("taluka", "Select Taluka");
                if (!selected.equals(current) && !selected.equals("Select Taluka")) {
                    showConfirmationDialog("taluka", selected, () -> {
                        spinnerTaluka.setSelection(pos);
                        saveToSharedPreferences("taluka", selected);
                        saveToFirestore("taluka", selected);
                        updateSQLiteUser();
                    }, () -> {
                        isReverting = true;
                        List<String> talukas = new ArrayList<>(
                                DataConstants.TALUKA_MAP.getOrDefault(
                                        sharedPreferences.getString("district",
                                                "Select District"),
                                        Arrays.asList("Select Taluka")));
                        spinnerTaluka.setSelection(talukas.indexOf(current));
                        isReverting = false;
                    });
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
    }

    /* --------------------------------------------------------------------- */
    private void setupEditListeners() {
        profileNameEdit.setOnClickListener(v -> showNameEditDialog());
        currentAffairsEdit.setOnClickListener(v ->
                showRadioDialog("currentAffairs", "करंट अफेअर्स नोटिफिकेशन",
                        sharedPreferences.getBoolean("currentAffairs", false)));
        jobsEdit.setOnClickListener(v ->
                showRadioDialog("jobs", "दहावी,बारावी आधारित जॉब",
                        sharedPreferences.getBoolean("jobs", false)));
        studyMaterialEdit.setOnClickListener(v -> showStudyMaterialDialog());
    }

    /* --------------------------------------------------------------------- */
    private void showNameEditDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(requireContext())
                .setTitle("Edit Name");
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(sharedPreferences.getString("userName", "User"));
        b.setView(input);
        b.setPositiveButton("OK", (d, w) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(sharedPreferences.getString("userName", "User"))) {
                showConfirmationDialog("userName", newName, () -> {
                    profileName.setText(newName);
                    saveToSharedPreferences("userName", newName);
                    saveToFirestore("name", newName);
                    updateSQLiteUser();
                }, () -> {});
            }
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private void showStudyMaterialDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(requireContext())
                .setTitle("FREE स्टडी मटेरियल कोणत्या भरतीची पाहिजे ?");

        boolean[] checked = new boolean[studyMaterialOptions.length];
        for (int i = 0; i < studyMaterialOptions.length; i++) {
            String key = "study_" + studyMaterialOptions[i]
                    .replace(" & ", "_").replace(" ", "_");
            checked[i] = sharedPreferences.getBoolean(key, false);
        }

        b.setMultiChoiceItems(studyMaterialOptions, checked,
                (dialog, which, isChecked) -> checked[which] = isChecked);

        b.setPositiveButton("OK", (dialog, which) -> {
            List<String> selected = new ArrayList<>();
            Map<String, Object> updates = new HashMap<>();

            for (int i = 0; i < studyMaterialOptions.length; i++) {
                String option = studyMaterialOptions[i];
                String key = "study_" + option.replace(" & ", "_")
                        .replace(" ", "_");
                boolean value = checked[i];
                editor.putBoolean(key, value);
                if (value) selected.add(option);
                updates.put(key, value);
            }
            editor.apply();

            studyMaterialSelection.setText(selected.isEmpty() ?
                    "कोणतेही नाही" : String.join(", ", selected));
            saveToFirestore(updates);
            updateSQLiteUser();
            Toast.makeText(requireContext(),
                    "Study material updated!", Toast.LENGTH_SHORT).show();
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private void saveToFirestore(Map<String, Object> updates) {
        if (userId != null) {
            db.collection("users").document(userId)
                    .update(updates)
                    .addOnSuccessListener(a -> Log.d("Profile", "Updated in Firestore"))
                    .addOnFailureListener(e -> Toast.makeText(requireContext(),
                            "Update failed", Toast.LENGTH_SHORT).show());
        }
    }

    private String getStudyMaterialDisplayText() {
        List<String> selected = new ArrayList<>();
        for (String option : studyMaterialOptions) {
            String key = "study_" + option.replace(" & ", "_")
                    .replace(" ", "_");
            if (sharedPreferences.getBoolean(key, false)) selected.add(option);
        }
        return selected.isEmpty() ? "कोणतेही नाही" : String.join(", ", selected);
    }

    /* --------------------------------------------------------------------- */
    private void setupGenderListener() {
        radioGroupGender.setOnCheckedChangeListener((group, checkedId) -> {
            if (isReverting) return;
            String selected = checkedId == R.id.radio_men ? "पुरुष" : "स्त्री";
            String current  = sharedPreferences.getString("gender", "");
            if (!selected.equals(current)) {
                showConfirmationDialog("gender", selected, () -> {
                    saveToSharedPreferences("gender", selected);
                    saveToFirestore("gender", selected);
                    updateSQLiteUser();
                }, () -> {
                    isReverting = true;
                    radioGroupGender.check(current.equals("पुरुष") ?
                            R.id.radio_men : R.id.radio_women);
                    isReverting = false;
                });
            }
        });
    }

    private void showRadioDialog(String prefKey, String title, boolean current) {
        AlertDialog.Builder b = new AlertDialog.Builder(requireContext())
                .setTitle(title);
        String[] options = {"हो", "नाही"};
        int checked = current ? 0 : 1;
        b.setSingleChoiceItems(options, checked, (d, w) -> {});
        b.setPositiveButton("OK", (d, w) -> {
            boolean selected = ((AlertDialog) d).getListView()
                    .getCheckedItemPosition() == 0;
            if (selected != current) {
                showConfirmationDialog(prefKey, selected ? "हो" : "नाही", () -> {
                    (prefKey.equals("currentAffairs") ?
                            currentAffairsSelection : jobsSelection)
                            .setText(selected ? "हो" : "नाही");
                    editor.putBoolean(prefKey, selected).apply();
                    saveToFirestore(prefKey, selected);
                    updateSQLiteUser();
                }, () -> {});
            }
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private void showConfirmationDialog(String key, String value,
                                        Runnable onConfirm, Runnable onCancel) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Change")
                .setMessage("Are you sure you want to change " +
                        getFieldName(key) + " to " + value + "?")
                .setPositiveButton("Yes", (d, w) -> onConfirm.run())
                .setNegativeButton("No", (d, w) -> onCancel.run())
                .show();
    }

    private String getFieldName(String key) {
        return switch (key) {
            case "userName"        -> "Name";
            case "gender"          -> "Gender";
            case "degree"          -> "Bachelor's Degree";
            case "twelfth"         -> "12th Stream";
            case "postGraduation"  -> "Master's Degree";
            case "district"        -> "District";
            case "taluka"          -> "Taluka";
            case "ageGroup"        -> "Age Group";
            case "education"       -> "Education Category";
            case "currentAffairs"  -> "Current Affairs Notifications";
            case "jobs"            -> "10th/12th Based Jobs";
            default                -> key;
        };
    }

    /* --------------------------------------------------------------------- */
    private void saveToSharedPreferences(String key, String value) {
        editor.putString(key, value).apply();
    }

    private void saveToFirestore(String key, String value) {
        if (userId == null) return;
        String firestoreKey = key.equals("userName") ? "name" : key;
        Map<String, Object> map = new HashMap<>(); map.put(firestoreKey, value);
        db.collection("users").document(userId).update(map)
                .addOnSuccessListener(a -> Toast.makeText(requireContext(),
                        "Updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "Failed", Toast.LENGTH_SHORT).show());
    }

    private void saveToFirestore(String key, boolean value) {
        if (userId == null) return;
        Map<String, Object> map = new HashMap<>(); map.put(key, value);
        db.collection("users").document(userId).update(map);
    }

    /* --------------------------------------------------------------------- */
    private void updateSQLiteUser() {
        if (userId == null || userId.isEmpty()) return;

        User user = new User(
                userId,
                sharedPreferences.getString("userName", "User"),
                sharedPreferences.getString("gender", ""),
                sharedPreferences.getString("avatar", "girl_profile"),
                sharedPreferences.getBoolean("study_Government", false),
                sharedPreferences.getBoolean("study_Police___Defence", false),
                sharedPreferences.getBoolean("study_Banking", false),
                sharedPreferences.getBoolean("study_Self_Improvement", false),
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

        try { dbHelper.updateUser(user); }
        catch (Exception e) {
            Toast.makeText(requireContext(),
                    "DB Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /* --------------------------------------------------------------------- */
    private void loadProfileData() {
        if (userId == null || userId.isEmpty()) {
            startActivity(new Intent(requireContext(), LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK));
            requireActivity().finish();
            return;
        }

        User user = dbHelper.getUser(userId);
        if (user != null) loadUserData(user);
        else loadFromSharedPrefs();
    }

    private void loadUserData(User user) {
        profileName.setText(user.getName() != null ? user.getName() : "User");
        loadAvatar(user.getAvatar());
        setGender(user.getGender());
        setSpinnersFromUser(user);
        currentAffairsSelection.setText(user.isCurrentAffairs() ? "हो" : "नाही");
        jobsSelection.setText(user.isJobs() ? "हो" : "नाही");
        studyMaterialSelection.setText(getStudyMaterialDisplayText());
        coinTextView.setText(String.valueOf(dbHelper.getUserCoins(userId)));

        // ---- NEW: set job text on first load ---------------------------------
        updateJobTextByTwelfth(user.getTwelfth());
    }

    private void loadFromSharedPrefs() {
        profileName.setText(sharedPreferences.getString("userName", "User"));
        loadAvatar(sharedPreferences.getString("avatar", "girl_profile"));
        setGender(sharedPreferences.getString("gender", ""));
        setSpinnersFromPrefs();
        currentAffairsSelection.setText(
                sharedPreferences.getBoolean("currentAffairs", false) ? "हो" : "नाही");
        jobsSelection.setText(
                sharedPreferences.getBoolean("jobs", false) ? "हो" : "नाही");
        studyMaterialSelection.setText(getStudyMaterialDisplayText());
        coinTextView.setText(String.valueOf(dbHelper.getUserCoins(userId)));

        // ---- NEW: set job text from prefs ------------------------------------
        updateJobTextByTwelfth(sharedPreferences.getString("twelfth",
                DataConstants.TWELFTH_OPTIONS.get(0)));
    }

    private void loadAvatar(String avatar) {
        try {
            int resId = getResources().getIdentifier(avatar, "drawable",
                    requireContext().getPackageName());
            profileImage.setImageResource(resId != 0 ? resId : R.drawable.girl_profile);
        } catch (Exception e) {
            profileImage.setImageResource(R.drawable.girl_profile);
        }
    }

    private void setGender(String gender) {
        isReverting = true;
        radioGroupGender.check(gender != null && gender.equals("पुरुष") ?
                R.id.radio_men : R.id.radio_women);
        isReverting = false;
    }

    private void setSpinnersFromUser(User user) {
        setSpinnerSelection(spinnerAgeGroup, ageGroupOptions, user.getAgeGroup());
        setSpinnerSelection(spinnerTwelfth, DataConstants.TWELFTH_OPTIONS, user.getTwelfth());

        setSpinnerSelection(spinnerEducation, DataConstants.EDUCATION_OPTIONS,
                user.getEducation());
        updateDegreeAndPostGradSpinners(user.getEducation());
        setSpinnerSelection(spinnerDegree,
                new ArrayList<>(DataConstants.DEGREE_MAP.getOrDefault(
                        user.getEducation(), Arrays.asList("Select Degree"))),
                user.getDegree());
        setSpinnerSelection(spinnerPostGrad,
                new ArrayList<>(DataConstants.POST_GRAD_MAP.getOrDefault(
                        user.getEducation(), Arrays.asList("Select Post Graduation"))),
                user.getPostGraduation());

        setSpinnerSelection(spinnerDistrict, DataConstants.DISTRICTS, user.getDistrict());
        updateTalukaSpinner(user.getDistrict());
        setSpinnerSelection(spinnerTaluka,
                new ArrayList<>(DataConstants.TALUKA_MAP.getOrDefault(
                        user.getDistrict(), Arrays.asList("Select Taluka"))),
                user.getTaluka());
    }

    private void setSpinnersFromPrefs() {
        String education = sharedPreferences.getString("education",
                "Select Education Category");

        setSpinnerSelection(spinnerAgeGroup, ageGroupOptions,
                sharedPreferences.getString("ageGroup", "Select Age Group"));
        setSpinnerSelection(spinnerTwelfth, DataConstants.TWELFTH_OPTIONS,
                sharedPreferences.getString("twelfth", "Select 12th Stream"));

        setSpinnerSelection(spinnerEducation, DataConstants.EDUCATION_OPTIONS,
                education);
        updateDegreeAndPostGradSpinners(education);
        setSpinnerSelection(spinnerDegree,
                new ArrayList<>(DataConstants.DEGREE_MAP.getOrDefault(
                        education, Arrays.asList("Select Degree"))),
                sharedPreferences.getString("degree", "Select Degree"));
        setSpinnerSelection(spinnerPostGrad,
                new ArrayList<>(DataConstants.POST_GRAD_MAP.getOrDefault(
                        education, Arrays.asList("Select Post Graduation"))),
                sharedPreferences.getString("postGraduation", "Select Post Graduation"));

        String district = sharedPreferences.getString("district", "Select District");
        setSpinnerSelection(spinnerDistrict, DataConstants.DISTRICTS, district);
        updateTalukaSpinner(district);
        setSpinnerSelection(spinnerTaluka,
                new ArrayList<>(DataConstants.TALUKA_MAP.getOrDefault(
                        district, Arrays.asList("Select Taluka"))),
                sharedPreferences.getString("taluka", "Select Taluka"));
    }

    private void setSpinnerSelection(Spinner spinner, List<String> options, String value) {
        if (value != null) {
            int pos = options.indexOf(value);
            isReverting = true;
            spinner.setSelection(pos >= 0 ? pos : 0);
            isReverting = false;
        }
    }

    /* --------------------------------------------------------------------- */
    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure? This cannot be undone.")
                .setPositiveButton("Yes", (d, w) -> deleteAccount())
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteAccount() {
        if (userId == null) return;
        db.collection("users").document(userId).delete()
                .addOnSuccessListener(a -> {
                    dbHelper.deleteUser(userId);
                    editor.clear().putBoolean("isLoggedIn", false).apply();
                    startActivity(new Intent(requireContext(), LoginActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    requireActivity().finish();
                });
    }

    /* --------------------------------------------------------------------- */
    private void setupShareButton() {
        shareButtonContainer.setOnClickListener(v -> {
            try {
                Uri uri = generateBannerWithText();
                Intent intent = new Intent(Intent.ACTION_SEND)
                        .setType("image/*")
                        .putExtra(Intent.EXTRA_TEXT,
                                "Join our app: https://bit.ly/yourapp-profile")
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .setPackage("com.whatsapp");
                startActivityForResult(intent, 100);
            } catch (Exception e) {
                Toast.makeText(requireContext(),
                        "Share error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Uri generateBannerWithText() {
        Bitmap bmp = BitmapFactory.decodeResource(getResources(),
                R.drawable.student_update_1).copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE); paint.setTextSize(50);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Download this app",
                bmp.getWidth() / 2f, bmp.getHeight() - 100, paint);

        File file = new File(requireContext().getCacheDir(), "banner.png");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) { throw new RuntimeException(e); }
        return FileProvider.getUriForFile(requireContext(),
                "com.newsproject.oneroadmap.fileprovider", file);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            int current = dbHelper.getUserCoins(userId);
            int newCoins = current + 100;
            dbHelper.updateUserCoins(userId, newCoins);
            saveCoinsToFirestore(newCoins);
            showCoinAnimationDialog(current, newCoins);
        }
    }

    private void showCoinAnimationDialog(int start, int end) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.coin_dialog_layout, null);
        TextView count = view.findViewById(R.id.coin_count);
        Button ok = view.findViewById(R.id.ok_button);
        count.setText("Coins: " + start);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        ok.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        displayedCoins = start;
        handler.post(new Runnable() {
            @Override public void run() {
                if (displayedCoins < end) {
                    displayedCoins++;
                    count.setText("Coins: " + displayedCoins);
                    handler.postDelayed(this, 20);
                } else {
                    coinTextView.setText(String.valueOf(end));
                }
            }
        });
    }

    private void saveCoinsToFirestore(int coins) {
        if (userId != null) {
            db.collection("users").document(userId)
                    .update("coins", coins)
                    .addOnSuccessListener(a -> Toast.makeText(requireContext(),
                            "Coins updated", Toast.LENGTH_SHORT).show());
        }
    }
}