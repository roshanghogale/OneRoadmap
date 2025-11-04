package com.newsproject.oneroadmap.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.newsproject.oneroadmap.Activities.LoginActivity;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.DataConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LoginPage2 extends Fragment {

    private Spinner spinnerEducation, spinnerTwelfth, spinnerDegree, spinnerPostGrad;
    private Button btnPrevious, btnNext;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private ArrayAdapter<String> degreeAdapter, postGradAdapter;
    private String name, gender, ageGroup, avatar;

    public LoginPage2() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        if (getArguments() != null) {
            name = getArguments().getString("name");
            gender = getArguments().getString("gender");
            ageGroup = getArguments().getString("ageGroup");
            avatar = getArguments().getString("avatar");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_page2, container, false);

        spinnerEducation = view.findViewById(R.id.spinnerEducation);
        spinnerTwelfth   = view.findViewById(R.id.spinnerTwelfth);
        spinnerDegree    = view.findViewById(R.id.spinnerDegree);
        spinnerPostGrad  = view.findViewById(R.id.spinnerPostGrad);
        btnPrevious      = view.findViewById(R.id.btnPrevious);
        btnNext          = view.findViewById(R.id.btnNext);

        setupSpinners();
        restoreSavedData();

        // Twelfth Spinner Listener – enables/disables + saves job text
        spinnerTwelfth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean enable = (position == 3); // "नाही या पुढील शिक्षण आहे"
                setAdvancedSpinnersEnabled(enable);

                if (!enable) {
                    spinnerEducation.setSelection(0);
                    spinnerDegree.setSelection(0);
                    spinnerPostGrad.setSelection(0);
                }

                // Save selected twelfth option
                String selectedTwelfth = DataConstants.TWELFTH_OPTIONS.get(position);
                editor.putString("twelfth", selectedTwelfth);

                // Save corresponding job text
                String jobText = DataConstants.JOB_TEXT_BY_TWELFTH.get(position);
                editor.putString("jobTextByTwelfth", jobText);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                editor.putString("twelfth", "");
                editor.putString("jobTextByTwelfth", DataConstants.JOB_TEXT_BY_TWELFTH.get(0));
                editor.apply();
            }
        });

        btnPrevious.setOnClickListener(v -> {
            saveCurrentData();
            String education = spinnerEducation.getSelectedItem().toString();
            String twelfth = spinnerTwelfth.getSelectedItem().toString();
            String degree = spinnerDegree.getSelectedItem().toString();
            String postGrad = spinnerPostGrad.getSelectedItem().toString();
            Log.d("LoginPage2", "Previous button clicked - education: " + education +
                    ", twelfth: " + twelfth + ", degree: " + degree + ", postGrad: " + postGrad);
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        btnNext.setOnClickListener(v -> transferDataToNextFragment());

        return view;
    }

    private void setAdvancedSpinnersEnabled(boolean enabled) {
        spinnerEducation.setEnabled(enabled);
        spinnerDegree.setEnabled(enabled);
        spinnerPostGrad.setEnabled(enabled);

        float alpha = enabled ? 1f : 0.5f;
        spinnerEducation.setAlpha(alpha);
        spinnerDegree.setAlpha(alpha);
        spinnerPostGrad.setAlpha(alpha);
    }

    private void transferDataToNextFragment() {
        String twelfth = spinnerTwelfth.getSelectedItem().toString();

        if (twelfth.equals(DataConstants.TWELFTH_OPTIONS.get(0))) {
            Toast.makeText(requireContext(), "कृपया तुमचे शिक्षण निवडा!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean needAdvanced = twelfth.equals(DataConstants.TWELFTH_OPTIONS.get(3));
        if (needAdvanced) {
            String education = spinnerEducation.getSelectedItem().toString();
            String degree    = spinnerDegree.getSelectedItem().toString();

            if (education.equals("Select Education Category") ||
                    degree.equals("Select Degree")) {
                Toast.makeText(requireContext(), "सर्व आवश्यक फील्ड्स भरा!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        saveCurrentData();

        Bundle bundle = new Bundle();
        bundle.putString("name", name);
        bundle.putString("gender", gender);
        bundle.putString("ageGroup", ageGroup);
        bundle.putString("avatar", avatar);
        bundle.putString("twelfth", twelfth);

        if (needAdvanced) {
            bundle.putString("education", spinnerEducation.getSelectedItem().toString());
            bundle.putString("degree",    spinnerDegree.getSelectedItem().toString());
            bundle.putString("postGrad",  spinnerPostGrad.getSelectedItem().toString());
        } else {
            bundle.putString("education", "");
            bundle.putString("degree",    "");
            bundle.putString("postGrad",  "");
        }

        Fragment nextFragment = new LoginPage3();
        nextFragment.setArguments(bundle);

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right);
        transaction.replace(R.id.fragment_container, nextFragment)
                .addToBackStack(null)
                .commit();

        if (requireActivity() instanceof LoginActivity) {
            ((LoginActivity) requireActivity()).updateProgressBar(3, true);
        }
    }

    private void restoreSavedData() {
        String savedTwelfth = sharedPreferences.getString("twelfth", "");
        if (!savedTwelfth.isEmpty()) {
            ArrayAdapter<String> twelfthAdapter = (ArrayAdapter<String>) spinnerTwelfth.getAdapter();
            int pos = twelfthAdapter.getPosition(savedTwelfth);
            if (pos != -1) {
                spinnerTwelfth.setSelection(pos); // This triggers the listener → saves job text
            }
        }

        if (spinnerEducation.isEnabled()) {
            String savedEducation = sharedPreferences.getString("education", "");
            if (!savedEducation.isEmpty()) {
                ArrayAdapter<String> eduAdapter = (ArrayAdapter<String>) spinnerEducation.getAdapter();
                int pos = eduAdapter.getPosition(savedEducation);
                if (pos != -1) spinnerEducation.setSelection(pos);
            }
        }
    }

    private void setupSpinners() {
        // Twelfth Spinner
        ArrayAdapter<String> twelfthAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, new ArrayList<>(DataConstants.TWELFTH_OPTIONS));
        twelfthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTwelfth.setAdapter(twelfthAdapter);

        // Education Category Spinner
        ArrayAdapter<String> educationAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, new ArrayList<>(DataConstants.EDUCATION_OPTIONS));
        educationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEducation.setAdapter(educationAdapter);

        // Degree & PostGrad default adapters
        degreeAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, new ArrayList<>(Arrays.asList("Select Degree")));
        degreeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDegree.setAdapter(degreeAdapter);

        postGradAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, new ArrayList<>(Arrays.asList("Select Post Graduation")));
        postGradAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPostGrad.setAdapter(postGradAdapter);

        // Education → Degree & PostGrad mapping
        spinnerEducation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = DataConstants.EDUCATION_OPTIONS.get(position);
                if (!selectedCategory.equals("Select Education Category")) {
                    List<String> degrees = new ArrayList<>(DataConstants.DEGREE_MAP.getOrDefault(selectedCategory, Arrays.asList("Select Degree", "Other")));
                    List<String> postGrads = new ArrayList<>(DataConstants.POST_GRAD_MAP.getOrDefault(selectedCategory, Arrays.asList("Select Post Graduation", "None")));

                    degreeAdapter.clear(); degreeAdapter.addAll(degrees); degreeAdapter.notifyDataSetChanged();
                    postGradAdapter.clear(); postGradAdapter.addAll(postGrads); postGradAdapter.notifyDataSetChanged();

                    restoreDegreeAndPostGrad();
                } else {
                    degreeAdapter.clear(); degreeAdapter.addAll(Arrays.asList("Select Degree")); degreeAdapter.notifyDataSetChanged();
                    postGradAdapter.clear(); postGradAdapter.addAll(Arrays.asList("Select Post Graduation")); postGradAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void saveCurrentData() {
        editor.putString("education", spinnerEducation.getSelectedItem().toString());
        editor.putString("twelfth", spinnerTwelfth.getSelectedItem().toString());
        editor.putString("degree", spinnerDegree.getSelectedItem().toString());
        editor.putString("postGrad", spinnerPostGrad.getSelectedItem().toString());

        // Also save job text again (in case user navigated back)
        int pos = spinnerTwelfth.getSelectedItemPosition();
        if (pos >= 0 && pos < DataConstants.JOB_TEXT_BY_TWELFTH.size()) {
            editor.putString("jobTextByTwelfth", DataConstants.JOB_TEXT_BY_TWELFTH.get(pos));
        }
        editor.apply();
    }

    private void restoreDegreeAndPostGrad() {
        String savedDegree = sharedPreferences.getString("degree", "");
        String savedPostGrad = sharedPreferences.getString("postGrad", "");

        if (!savedDegree.isEmpty()) {
            ArrayAdapter<String> degAdapter = (ArrayAdapter<String>) spinnerDegree.getAdapter();
            int pos = degAdapter.getPosition(savedDegree);
            if (pos != -1) spinnerDegree.setSelection(pos);
        }

        if (!savedPostGrad.isEmpty()) {
            ArrayAdapter<String> pgAdapter = (ArrayAdapter<String>) spinnerPostGrad.getAdapter();
            int pos = pgAdapter.getPosition(savedPostGrad);
            if (pos != -1) spinnerPostGrad.setSelection(pos);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requireActivity() instanceof LoginActivity) {
            ((LoginActivity) requireActivity()).updateProgressBar(2, false);
        }
    }
}