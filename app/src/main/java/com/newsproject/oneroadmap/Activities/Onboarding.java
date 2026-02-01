package com.newsproject.oneroadmap.Activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.newsproject.oneroadmap.Adapters.OnboardingAdapter;
import com.newsproject.oneroadmap.Models.OnboardingModel;
import com.newsproject.oneroadmap.R;

import java.util.ArrayList;
import java.util.List;

public class Onboarding extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_onboarding);

        ViewPager2 viewPager = findViewById(R.id.onboardingViewPager);

        List<OnboardingModel> list = new ArrayList<>();

        list.add(new OnboardingModel(
                R.drawable.ga2,
                R.drawable.ma1,
                R.drawable.ma2,
                R.drawable.ma3,
                R.drawable.ma4,
                "Maharashtra Job",
                "Maharashtra state, Central and private job update available"
        ));

        list.add(new OnboardingModel(
                R.drawable.ga3,
                R.drawable.ma2,
                R.drawable.ma3,
                R.drawable.ma4,
                R.drawable.ma1,
                "Career Roadmap",
                "Information About The Right RoadMap According To Your Education"
        ));

        list.add(new OnboardingModel(
                R.drawable.ga4,
                R.drawable.ma4,
                R.drawable.ma1,
                R.drawable.ma2,
                R.drawable.ma3,
                "Customise Your Notifications",
                "Customise your notifications for perfect updates"
        ));

        viewPager.setAdapter(new OnboardingAdapter(list, viewPager));

        viewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);
                    }
                }
        );
    }
}
