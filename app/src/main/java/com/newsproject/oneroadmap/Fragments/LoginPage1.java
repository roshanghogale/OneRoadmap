package com.newsproject.oneroadmap.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.newsproject.oneroadmap.Activities.LoginActivity;
import com.newsproject.oneroadmap.Adapters.AvatarAdapter;
import com.newsproject.oneroadmap.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class LoginPage1 extends Fragment implements AvatarAdapter.OnAvatarClickListener {

    private EditText etName;
    private RadioGroup radioGroupGender;
    private Spinner spinnerAge;
    private Button btnNext;
    private RecyclerView avatarRecycler;
    private TextView avatarTextView;
    private CircleImageView userProfileImage;
    private SharedPreferences sharedPreferences;
    private AvatarAdapter avatarAdapter;
    private String selectedAvatar = "";
    private List<String> avatarList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_page1, container, false);

        etName = view.findViewById(R.id.etName);
        radioGroupGender = view.findViewById(R.id.radioGroupGender);
        spinnerAge = view.findViewById(R.id.spinnerAge);
        btnNext = view.findViewById(R.id.btnNext);
        avatarRecycler = view.findViewById(R.id.avatar_recycler);
        avatarTextView = view.findViewById(R.id.textView17);
        userProfileImage = view.findViewById(R.id.user_profile);

        // Ensure RecyclerView and TextView are initially GONE
        avatarRecycler.setVisibility(View.GONE);
        avatarTextView.setVisibility(View.GONE);

        // Setup RecyclerView with horizontal GridLayoutManager (2 rows)
        avatarList = new ArrayList<>();
        avatarAdapter = new AvatarAdapter(requireContext(), avatarList, this);
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        avatarRecycler.setLayoutManager(layoutManager);
        avatarRecycler.setAdapter(avatarAdapter);

        // Setup Spinner
        List<String> ageGroupOptions = new ArrayList<>(Arrays.asList(
                "Select Age Group", "१४ ते १८ ", "१९ ते २५", "२६ ते ३१", "३२ पेक्षा जास्त "
        ));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                ageGroupOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAge.setAdapter(adapter);

        // Handle gender selection
        radioGroupGender.setOnCheckedChangeListener((group, checkedId) -> {
            avatarList.clear();
            selectedAvatar = "";
            avatarAdapter.updateAvatars(avatarList);
            // Show RecyclerView and TextView when a radio button is selected
            avatarRecycler.setVisibility(View.VISIBLE);
            avatarTextView.setVisibility(View.VISIBLE);
            // Reset user profile image to default when gender changes
            userProfileImage.setImageResource(R.drawable.profile);
            if (checkedId == R.id.radio_men) {
                for (int i = 1; i <= 15; i++) {
                    avatarList.add("ma" + i);
                }
            } else if (checkedId == R.id.radio_women) {
                for (int i = 1; i <= 15; i++) {
                    avatarList.add("ga" + i);
                }
            }
            avatarAdapter.updateAvatars(avatarList);
        });

        // Restore saved data
        restoreSavedData();

        // Handle NEXT button click
        btnNext.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            int selectedGenderId = radioGroupGender.getCheckedRadioButtonId();
            String gender = "";
            if (selectedGenderId != -1) {
                RadioButton selectedRadio = view.findViewById(selectedGenderId);
                gender = selectedRadio.getText().toString();
            }
            String ageGroup = spinnerAge.getSelectedItem().toString();

            // Validate inputs
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (gender.isEmpty()) {
                Toast.makeText(requireContext(), "Please select gender", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedAvatar.isEmpty()) {
                Toast.makeText(requireContext(), "Please select an avatar", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ageGroup.equals("Select Age Group")) {
                Toast.makeText(requireContext(), "Please select age group", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save data to SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("name", name);
            editor.putString("gender", gender);
            editor.putString("avatar", selectedAvatar);
            editor.putString("ageGroup", ageGroup);
            editor.apply();

            // Pass data to LoginPage2
            Bundle bundle = new Bundle();
            bundle.putString("name", name);
            bundle.putString("gender", gender);
            bundle.putString("avatar", selectedAvatar);
            bundle.putString("ageGroup", ageGroup);

            LoginPage2 loginPage2 = new LoginPage2();
            loginPage2.setArguments(bundle);

            FragmentTransaction transaction = requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction();
            transaction.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
            );
            transaction.replace(R.id.fragment_container, loginPage2);
            transaction.addToBackStack(null);
            transaction.commit();

            // Update progress bar
            if (requireActivity() instanceof LoginActivity) {
                ((LoginActivity) requireActivity()).updateProgressBar(2, true);
            }
        });

        return view;
    }

    private void restoreSavedData() {
        String savedName = sharedPreferences.getString("name", "");
        String savedGender = sharedPreferences.getString("gender", "");
        String savedAvatar = sharedPreferences.getString("avatar", "");
        String savedAgeGroup = sharedPreferences.getString("ageGroup", "");

        if (!savedName.isEmpty()) {
            etName.setText(savedName);
        }
        if (!savedGender.isEmpty()) {
            RadioButton radioMen = radioGroupGender.findViewById(R.id.radio_men);
            RadioButton radioWomen = radioGroupGender.findViewById(R.id.radio_women);
            if (savedGender.equals(radioMen.getText().toString())) {
                radioMen.setChecked(true);
                avatarList.clear();
                for (int i = 1; i <= 15; i++) {
                    avatarList.add("ma" + i);
                }
                // Show RecyclerView and TextView when restoring gender
                avatarRecycler.setVisibility(View.VISIBLE);
                avatarTextView.setVisibility(View.VISIBLE);
            } else if (savedGender.equals(radioWomen.getText().toString())) {
                radioWomen.setChecked(true);
                avatarList.clear();
                for (int i = 1; i <= 15; i++) {
                    avatarList.add("ga" + i);
                }
                // Show RecyclerView and TextView when restoring gender
                avatarRecycler.setVisibility(View.VISIBLE);
                avatarTextView.setVisibility(View.VISIBLE);
            }
            if (!avatarList.isEmpty()) {
                avatarAdapter.updateAvatars(avatarList);
            }
        }
        if (!savedAvatar.isEmpty()) {
            selectedAvatar = savedAvatar;
            avatarAdapter.setSelectedAvatar(savedAvatar);
            // Update user profile image with saved avatar
            int resId = getResources().getIdentifier(savedAvatar, "drawable", requireContext().getPackageName());
            if (resId != 0) {
                userProfileImage.setImageResource(resId);
            }
        }
        if (!savedAgeGroup.isEmpty()) {
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerAge.getAdapter();
            int position = adapter.getPosition(savedAgeGroup);
            if (position != -1) {
                spinnerAge.setSelection(position);
            }
        }
    }

    @Override
    public void onAvatarClick(String drawableName) {
        selectedAvatar = drawableName;
        avatarAdapter.setSelectedAvatar(drawableName);
        // Update user profile image with selected avatar
        int resId = getResources().getIdentifier(drawableName, "drawable", requireContext().getPackageName());
        if (resId != 0) {
            userProfileImage.setImageResource(resId);
        }
        Toast.makeText(requireContext(), "Selected: " + drawableName, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requireActivity() instanceof LoginActivity) {
            ((LoginActivity) requireActivity()).updateProgressBar(1, false);
        }
    }
}