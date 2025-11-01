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
    private ArrayAdapter<String> degreeAdapter, postGradAdapter;
    private String name, gender, ageGroup, avatar;

    public LoginPage2() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
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
        spinnerTwelfth = view.findViewById(R.id.spinnerTwelfth);
        spinnerDegree = view.findViewById(R.id.spinnerDegree);
        spinnerPostGrad = view.findViewById(R.id.spinnerPostGrad);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        btnNext = view.findViewById(R.id.btnNext);

        setupSpinners();
        restoreSavedData();

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

    private void setupSpinners() {
        // Twelfth Stream Spinner
        ArrayAdapter<String> twelfthAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, new ArrayList<>(DataConstants.TWELFTH_OPTIONS));
        twelfthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTwelfth.setAdapter(twelfthAdapter);

        // Education Category Spinner
        ArrayAdapter<String> educationAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, new ArrayList<>(DataConstants.EDUCATION_OPTIONS));
        educationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEducation.setAdapter(educationAdapter);

        // Set default adapters for Degree and Post Grad with mutable lists
        degreeAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, new ArrayList<>(Arrays.asList("Select Degree")));
        degreeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDegree.setAdapter(degreeAdapter);

        postGradAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, new ArrayList<>(Arrays.asList("Select Post Graduation")));
        postGradAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPostGrad.setAdapter(postGradAdapter);

        // Listener for Education Category to update Degree and Post Grad
        spinnerEducation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = DataConstants.EDUCATION_OPTIONS.get(position);
                if (!selectedCategory.equals("Select Education Category")) {
                    // Use mutable lists to avoid UnsupportedOperationException
                    List<String> degrees = new ArrayList<>(DataConstants.DEGREE_MAP.getOrDefault(selectedCategory, Arrays.asList("Select Degree", "Other")));
                    List<String> postGrads = new ArrayList<>(DataConstants.POST_GRAD_MAP.getOrDefault(selectedCategory, Arrays.asList("Select Post Graduation", "None")));

                    degreeAdapter.clear();
                    degreeAdapter.addAll(degrees);
                    degreeAdapter.notifyDataSetChanged();

                    postGradAdapter.clear();
                    postGradAdapter.addAll(postGrads);
                    postGradAdapter.notifyDataSetChanged();

                    restoreDegreeAndPostGrad();
                } else {
                    degreeAdapter.clear();
                    degreeAdapter.addAll(new ArrayList<>(Arrays.asList("Select Degree")));
                    degreeAdapter.notifyDataSetChanged();

                    postGradAdapter.clear();
                    postGradAdapter.addAll(new ArrayList<>(Arrays.asList("Select Post Graduation")));
                    postGradAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void saveCurrentData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("education", spinnerEducation.getSelectedItem().toString());
        editor.putString("twelfth", spinnerTwelfth.getSelectedItem().toString());
        editor.putString("degree", spinnerDegree.getSelectedItem().toString());
        editor.putString("postGrad", spinnerPostGrad.getSelectedItem().toString());
        editor.apply();
    }

    private void restoreSavedData() {
        String savedEducation = sharedPreferences.getString("education", "");
        String savedTwelfth = sharedPreferences.getString("twelfth", "");

        if (!savedTwelfth.isEmpty()) {
            ArrayAdapter<String> twelfthAdapter = (ArrayAdapter<String>) spinnerTwelfth.getAdapter();
            int position = twelfthAdapter.getPosition(savedTwelfth);
            if (position != -1) {
                spinnerTwelfth.setSelection(position);
            }
        }

        if (!savedEducation.isEmpty()) {
            ArrayAdapter<String> educationAdapter = (ArrayAdapter<String>) spinnerEducation.getAdapter();
            int position = educationAdapter.getPosition(savedEducation);
            if (position != -1) {
                spinnerEducation.setSelection(position);
            }
        }
    }

    private void restoreDegreeAndPostGrad() {
        String savedDegree = sharedPreferences.getString("degree", "");
        String savedPostGrad = sharedPreferences.getString("postGrad", "");

        if (!savedDegree.isEmpty()) {
            ArrayAdapter<String> degreeAdapter = (ArrayAdapter<String>) spinnerDegree.getAdapter();
            int position = degreeAdapter.getPosition(savedDegree);
            if (position != -1) {
                spinnerDegree.setSelection(position);
            }
        }

        if (!savedPostGrad.isEmpty()) {
            ArrayAdapter<String> postGradAdapter = (ArrayAdapter<String>) spinnerPostGrad.getAdapter();
            int position = postGradAdapter.getPosition(savedPostGrad);
            if (position != -1) {
                spinnerPostGrad.setSelection(position);
            }
        }
    }

    private void transferDataToNextFragment() {
        String education = spinnerEducation.getSelectedItem().toString();
        String twelfth = spinnerTwelfth.getSelectedItem().toString();
        String degree = spinnerDegree.getSelectedItem().toString();
        String postGrad = spinnerPostGrad.getSelectedItem().toString();

        if (education.equals("Select Education Category") || twelfth.equals("Select 12th Stream") ||
                degree.equals("Select Degree")) {
            Toast.makeText(requireContext(), "Please fill all required fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        saveCurrentData();

        Log.d("LoginPage2", "Next button clicked - education: " + education +
                ", twelfth: " + twelfth + ", degree: " + degree + ", postGrad: " + postGrad);

        Bundle bundle = new Bundle();
        bundle.putString("name", name);
        bundle.putString("gender", gender);
        bundle.putString("ageGroup", ageGroup);
        bundle.putString("avatar", avatar);
        bundle.putString("education", education);
        bundle.putString("twelfth", twelfth);
        bundle.putString("degree", degree);
        bundle.putString("postGrad", postGrad);

        Fragment nextFragment = new LoginPage3();
        nextFragment.setArguments(bundle);

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                R.anim.slide_in_right,  // enter
                R.anim.slide_out_left,  // exit
                R.anim.slide_in_left,   // popEnter
                R.anim.slide_out_right  // popExit
        );
        transaction.replace(R.id.fragment_container, nextFragment);
        transaction.addToBackStack(null);
        transaction.commit();

        if (requireActivity() instanceof LoginActivity) {
            ((LoginActivity) requireActivity()).updateProgressBar(3, true);
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