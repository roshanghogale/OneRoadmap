package com.newsproject.oneroadmap.Activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.newsproject.oneroadmap.Adapters.OnboardingAdapter;
import com.newsproject.oneroadmap.Models.OnboardingModel;
import com.newsproject.oneroadmap.R;

import java.util.ArrayList;
import java.util.List;

public class Onboarding extends AppCompatActivity {

    private OnboardingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        ViewPager2 viewPager = findViewById(R.id.onboardingViewPager);
        viewPager.setUserInputEnabled(false); // 🔒 disable swipe

        List<OnboardingModel> list = new ArrayList<>();

        list.add(new OnboardingModel(
                R.drawable.center_onboarding_logo,
                R.drawable.onboarding_logo2,
                R.drawable.onboarding_logo1,
                R.drawable.onboarding_logo4,
                R.drawable.onboarding_logo3,
                "Maharashtra Job",
                "Maharashtra state, Central and private job update available"
        ));

        list.add(new OnboardingModel(
                R.drawable.center_onboarding_logo,
                R.drawable.onboarding_logo2,
                R.drawable.onboarding_logo1,
                R.drawable.onboarding_logo4,
                R.drawable.onboarding_logo3,
                "Career Roadmap",
                "Information About The Right RoadMap According To Your Education"
        ));

        list.add(new OnboardingModel(
                R.drawable.center_onboarding_logo,
                R.drawable.onboarding_logo2,
                R.drawable.onboarding_logo1,
                R.drawable.onboarding_logo4,
                R.drawable.onboarding_logo3,
                "Customise Your Notifications",
                "Customise your notifications for perfect updates"
        ));

        adapter = new OnboardingAdapter(this, list);
        viewPager.setAdapter(adapter);
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (adapter != null && adapter.handleBack()) {
            return; // onboarding handled back
        }
        super.onBackPressed(); // exit app
    }
}
