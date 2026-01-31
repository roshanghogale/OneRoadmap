package com.newsproject.oneroadmap.Fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
    private TextView tvAge;                  // 👈 replaces Spinner
    private Button btnNext;
    private RecyclerView avatarRecycler;
    private TextView avatarTextView;
    private CircleImageView userProfileImage;

    private SharedPreferences sharedPreferences;
    private AvatarAdapter avatarAdapter;
    private List<String> avatarList = new ArrayList<>();

    private String selectedAvatar = "";
    private String selectedAgeGroup = "";

    private final List<String> ageGroups = Arrays.asList(
            "14 ते 18",
            "19 ते 25",
            "26 ते 31",
            "32 पेक्षा जास्त"
    );

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = requireContext()
                .getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        View view = inflater.inflate(R.layout.fragment_login_page1, container, false);

        // ---------- Views ----------
        etName = view.findViewById(R.id.etName);
        radioGroupGender = view.findViewById(R.id.radioGroupGender);
        tvAge = view.findViewById(R.id.tvAge);               // 👈 NEW
        btnNext = view.findViewById(R.id.btnNext);
        avatarRecycler = view.findViewById(R.id.avatar_recycler);
        avatarTextView = view.findViewById(R.id.textView17);
        userProfileImage = view.findViewById(R.id.user_profile);

        avatarRecycler.setVisibility(View.GONE);
        avatarTextView.setVisibility(View.GONE);

        // ---------- Avatar Recycler ----------
        avatarAdapter = new AvatarAdapter(requireContext(), avatarList, this);
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        avatarRecycler.setLayoutManager(layoutManager);
        avatarRecycler.setAdapter(avatarAdapter);

        // ---------- Age dialog trigger ----------
        tvAge.setOnClickListener(v -> showAgeDialog());

        // ---------- Gender selection ----------
        radioGroupGender.setOnCheckedChangeListener((group, checkedId) -> {
            avatarList.clear();
            selectedAvatar = "";
            avatarAdapter.updateAvatars(avatarList);

            avatarRecycler.setVisibility(View.VISIBLE);
            avatarTextView.setVisibility(View.VISIBLE);
            userProfileImage.setImageResource(R.drawable.profile);

            if (checkedId == R.id.radio_men) {
                for (int i = 1; i <= 15; i++) avatarList.add("ma" + i);
            } else if (checkedId == R.id.radio_women) {
                for (int i = 1; i <= 15; i++) avatarList.add("ga" + i);
            }

            avatarAdapter.updateAvatars(avatarList);
        });

        restoreSavedData();

        // ---------- NEXT ----------
        btnNext.setOnClickListener(v -> validateAndProceed(view));

        return view;
    }

    // =========================================================
    // AGE DIALOG
    // =========================================================
    private void showAgeDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_age_picker);
        dialog.setCancelable(true);

        ListView listView = dialog.findViewById(R.id.listAge);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_age_dialog,
                ageGroups
        );

        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            selectedAgeGroup = ageGroups.get(position);
            tvAge.setText(selectedAgeGroup);
            tvAge.setTextColor(Color.BLACK);
            dialog.dismiss();
        });

        dialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(Color.TRANSPARENT)
        );
        dialog.show();
    }

    // =========================================================
    // VALIDATION + NAVIGATION
    // =========================================================
    private void validateAndProceed(View rootView) {

        String name = etName.getText().toString().trim();
        int genderId = radioGroupGender.getCheckedRadioButtonId();

        if (name.isEmpty()) {
            toast("Please enter your name");
            return;
        }
        if (genderId == -1) {
            toast("Please select gender");
            return;
        }
        if (selectedAvatar.isEmpty()) {
            toast("Please select an avatar");
            return;
        }
        if (selectedAgeGroup.isEmpty()) {
            toast("Please select age group");
            return;
        }

        RadioButton rb = rootView.findViewById(genderId);
        String gender = rb.getText().toString();

        // Save
        sharedPreferences.edit()
                .putString("name", name)
                .putString("gender", gender)
                .putString("avatar", selectedAvatar)
                .putString("ageGroup", selectedAgeGroup)
                .apply();

        Bundle bundle = new Bundle();
        bundle.putString("name", name);
        bundle.putString("gender", gender);
        bundle.putString("avatar", selectedAvatar);
        bundle.putString("ageGroup", selectedAgeGroup);

        LoginPage2 page2 = new LoginPage2();
        page2.setArguments(bundle);

        FragmentTransaction ft = requireActivity()
                .getSupportFragmentManager()
                .beginTransaction();

        ft.setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
        );

        ft.replace(R.id.fragment_container, page2);
        ft.addToBackStack(null);
        ft.commit();

        if (requireActivity() instanceof LoginActivity) {
            ((LoginActivity) requireActivity()).updateProgressBar(2, true);
        }
    }

    // =========================================================
    // RESTORE STATE
    // =========================================================
    private void restoreSavedData() {

        String name = sharedPreferences.getString("name", "");
        String gender = sharedPreferences.getString("gender", "");
        String avatar = sharedPreferences.getString("avatar", "");
        String age = sharedPreferences.getString("ageGroup", "");

        if (!name.isEmpty()) etName.setText(name);

        if (!gender.isEmpty()) {
            RadioButton men = radioGroupGender.findViewById(R.id.radio_men);
            RadioButton women = radioGroupGender.findViewById(R.id.radio_women);

            if (gender.equals(men.getText().toString())) men.setChecked(true);
            else if (gender.equals(women.getText().toString())) women.setChecked(true);
        }

        if (!avatar.isEmpty()) {
            selectedAvatar = avatar;
            avatarAdapter.setSelectedAvatar(avatar);

            int resId = getResources().getIdentifier(
                    avatar, "drawable", requireContext().getPackageName()
            );
            if (resId != 0) userProfileImage.setImageResource(resId);
        }

        if (!age.isEmpty()) {
            selectedAgeGroup = age;
            tvAge.setText(age);
            tvAge.setTextColor(Color.BLACK);
        }
    }

    // =========================================================
    // AVATAR CALLBACK
    // =========================================================
    @Override
    public void onAvatarClick(String drawableName) {
        selectedAvatar = drawableName;
        avatarAdapter.setSelectedAvatar(drawableName);

        int resId = getResources().getIdentifier(
                drawableName, "drawable", requireContext().getPackageName()
        );
        if (resId != 0) userProfileImage.setImageResource(resId);
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requireActivity() instanceof LoginActivity) {
            ((LoginActivity) requireActivity()).updateProgressBar(1, false);
        }
    }
}
