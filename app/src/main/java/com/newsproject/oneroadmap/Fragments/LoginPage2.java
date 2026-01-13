package com.newsproject.oneroadmap.Fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.newsproject.oneroadmap.Activities.LoginActivity;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.DataConstants;

import java.util.ArrayList;
import java.util.List;

public class LoginPage2 extends Fragment {

    private TextView tvTwelfth, tvEducation, tvDegree, tvPostGrad;
    private Button btnPrevious, btnNext;

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    private String name, gender, ageGroup, avatar;
    private String selectedTwelfth = "";
    private String selectedEducation = "";
    private String selectedDegree = "";
    private String selectedPostGrad = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        editor = prefs.edit();

        if (getArguments() != null) {
            name = getArguments().getString("name");
            gender = getArguments().getString("gender");
            ageGroup = getArguments().getString("ageGroup");
            avatar = getArguments().getString("avatar");
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_login_page2, container, false);

        tvTwelfth   = view.findViewById(R.id.tvTwelfth);
        tvEducation = view.findViewById(R.id.tvEducation);
        tvDegree    = view.findViewById(R.id.tvDegree);
        tvPostGrad  = view.findViewById(R.id.tvPostGrad);

        btnPrevious = view.findViewById(R.id.btnPrevious);
        btnNext     = view.findViewById(R.id.btnNext);

        setAdvancedEnabled(false); // 🔒 disabled initially

        // -------- Twelfth --------
        tvTwelfth.setOnClickListener(v ->
                showDialog("तुमचं शिक्षण निवाडा",DataConstants.TWELFTH_OPTIONS, value -> {
                    selectedTwelfth = value;
                    tvTwelfth.setText(value);
                    tvTwelfth.setTextColor(Color.BLACK);

                    boolean needAdvanced =
                            value.equals(DataConstants.TWELFTH_OPTIONS.get(3));
                    setAdvancedEnabled(needAdvanced);
                })
        );

        // -------- Education --------
        tvEducation.setOnClickListener(v ->
                showDialog("Select Education Category",DataConstants.EDUCATION_OPTIONS, value -> {
                    selectedEducation = value;
                    tvEducation.setText(value);
                    tvEducation.setTextColor(Color.BLACK);

                    selectedDegree = "";
                    selectedPostGrad = "";

                    tvDegree.setText("Select Degree");
                    tvPostGrad.setText("Select Post Graduation");
                    tvDegree.setTextColor(Color.GRAY);
                    tvPostGrad.setTextColor(Color.GRAY);
                })
        );

        // -------- Degree --------
        tvDegree.setOnClickListener(v -> {
            List<String> degrees = DataConstants.DEGREE_MAP.get(selectedEducation);
            if (degrees != null) {
                showDialog("Select Degree",degrees, value -> {
                    selectedDegree = value;
                    tvDegree.setText(value);
                    tvDegree.setTextColor(Color.BLACK);
                });
            }
        });

        // -------- Post Graduation --------
        tvPostGrad.setOnClickListener(v -> {
            List<String> post = DataConstants.POST_GRAD_MAP.get(selectedEducation);
            if (post != null) {
                showDialog("Select Post Graduation",post, value -> {
                    selectedPostGrad = value;
                    tvPostGrad.setText(value);
                    tvPostGrad.setTextColor(Color.BLACK);
                });
            }
        });

        btnPrevious.setOnClickListener(v -> requireActivity().onBackPressed());
        btnNext.setOnClickListener(v -> validateAndProceed());

        restoreSavedData();
        return view;
    }

    // =====================================================
    // COMMON DIALOG (REUSED FROM PAGE 1)
    // =====================================================
    private void showDialog(String titletext, List<String> items, OnSelect listener) {
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

    private void setAdvancedEnabled(boolean enabled) {
        tvEducation.setEnabled(enabled);
        tvDegree.setEnabled(enabled);
        tvPostGrad.setEnabled(enabled);

        float alpha = enabled ? 1f : 0.4f;
        tvEducation.setAlpha(alpha);
        tvDegree.setAlpha(alpha);
        tvPostGrad.setAlpha(alpha);
    }

    private void validateAndProceed() {

        if (selectedTwelfth.isEmpty()) {
            toast("कृपया शिक्षण निवडा");
            return;
        }

        boolean needAdvanced =
                selectedTwelfth.equals(DataConstants.TWELFTH_OPTIONS.get(3));

        if (needAdvanced && (selectedEducation.isEmpty() || selectedDegree.isEmpty())) {
            toast("सर्व आवश्यक माहिती भरा");
            return;
        }

        editor.putString("twelfth", selectedTwelfth);
        editor.putString("education", selectedEducation);
        editor.putString("degree", selectedDegree);
        editor.putString("postGrad", selectedPostGrad);
        editor.apply();

        Bundle bundle = new Bundle();
        bundle.putString("name", name);
        bundle.putString("gender", gender);
        bundle.putString("ageGroup", ageGroup);
        bundle.putString("avatar", avatar);
        bundle.putString("twelfth", selectedTwelfth);
        bundle.putString("education", selectedEducation);
        bundle.putString("degree", selectedDegree);
        bundle.putString("postGrad", selectedPostGrad);

        Fragment next = new LoginPage3();
        next.setArguments(bundle);

        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        ft.setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
        );
        ft.replace(R.id.fragment_container, next)
                .addToBackStack(null)
                .commit();

        if (requireActivity() instanceof LoginActivity) {
            ((LoginActivity) requireActivity()).updateProgressBar(3, true);
        }
    }

    private void restoreSavedData() {
        selectedTwelfth = prefs.getString("twelfth", "");
        selectedEducation = prefs.getString("education", "");
        selectedDegree = prefs.getString("degree", "");
        selectedPostGrad = prefs.getString("postGrad", "");

        if (!selectedTwelfth.isEmpty()) {
            tvTwelfth.setText(selectedTwelfth);
            tvTwelfth.setTextColor(Color.BLACK);
        }
        if (!selectedEducation.isEmpty()) {
            tvEducation.setText(selectedEducation);
            tvEducation.setTextColor(Color.BLACK);
            setAdvancedEnabled(true);
        }
        if (!selectedDegree.isEmpty()) {
            tvDegree.setText(selectedDegree);
            tvDegree.setTextColor(Color.BLACK);
        }
        if (!selectedPostGrad.isEmpty()) {
            tvPostGrad.setText(selectedPostGrad);
            tvPostGrad.setTextColor(Color.BLACK);
        }
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    interface OnSelect {
        void onSelect(String value);
    }
}
