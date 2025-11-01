package com.newsproject.oneroadmap.Activities;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.newsproject.oneroadmap.Fragments.LoginPage1;
import com.newsproject.oneroadmap.Fragments.LoginPage2;
import com.newsproject.oneroadmap.Fragments.LoginPage3;
import com.newsproject.oneroadmap.R;

public class LoginActivity extends AppCompatActivity {

    private View progressStep1, progressStep2, progressStep3;
    private int currentStep = 1;
    private int previousStep = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize progress bar views
        progressStep1 = findViewById(R.id.progressStep1);
        progressStep2 = findViewById(R.id.progressStep2);
        progressStep3 = findViewById(R.id.progressStep3);

        // Set initial progress state
        updateProgressBar(1, true);

        // Load first fragment if not already loaded
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new LoginPage1())
                    .commit();
        }

        boolean isUserLoggedIn = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                .getBoolean("isLoggedIn", false);

        if (isUserLoggedIn) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    public void updateProgressBar(int step, boolean isForward) {
        previousStep = currentStep;
        currentStep = step;

        // Set pivot points for left-to-right (forward) or right-to-left (backward)
        float pivotX = isForward ? 0f : 1f;
        progressStep1.setPivotX(pivotX);
        progressStep2.setPivotX(pivotX);
        progressStep3.setPivotX(pivotX);

        AnimatorSet animatorSet = new AnimatorSet();

        // Maintain state of previously filled segments
        if (currentStep >= 1 && previousStep < 1) {
            // Animate step 1 if moving to step 1
            animatorSet.play(createFillAnimation(progressStep1, isForward));
        } else if (currentStep >= 1) {
            // Keep step 1 filled
            progressStep1.setBackgroundColor(getResources().getColor(R.color.purple));
            progressStep1.setScaleX(1f);
        }

        if (currentStep >= 2 && previousStep < 2) {
            // Animate step 2 if moving to step 2
            animatorSet.play(createFillAnimation(progressStep2, isForward));
        } else if (currentStep >= 2) {
            // Keep step 2 filled
            progressStep2.setBackgroundColor(getResources().getColor(R.color.purple));
            progressStep2.setScaleX(1f);
        } else if (currentStep < 2 && previousStep >= 2) {
            // Animate unfilling step 2 if moving back
            animatorSet.play(createUnfillAnimation(progressStep2, isForward));
        }

        if (currentStep >= 3 && previousStep < 3) {
            // Animate step 3 if moving to step 3
            animatorSet.play(createFillAnimation(progressStep3, isForward));
        } else if (currentStep >= 3) {
            // Keep step 3 filled
            progressStep3.setBackgroundColor(getResources().getColor(R.color.purple));
            progressStep3.setScaleX(1f);
        } else if (currentStep < 3 && previousStep >= 3) {
            // Animate unfilling step 3 if moving back
            animatorSet.play(createUnfillAnimation(progressStep3, isForward));
        }

        animatorSet.start();
    }

    private AnimatorSet createFillAnimation(View progressView, boolean isForward) {
        // Color animation
        int colorFrom = getResources().getColor(R.color.grey);
        int colorTo = getResources().getColor(R.color.purple);
        ValueAnimator colorAnimator = ValueAnimator.ofArgb(colorFrom, colorTo);
        colorAnimator.addUpdateListener(animator ->
                progressView.setBackgroundColor((int) animator.getAnimatedValue())
        );

        // Scale animation for filling effect
        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(isForward ? 0f : 1f, isForward ? 1f : 0f);
        scaleAnimator.addUpdateListener(animator ->
                progressView.setScaleX((float) animator.getAnimatedValue())
        );

        // Combine animations
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(colorAnimator, scaleAnimator);
        animatorSet.setDuration(500);
        animatorSet.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        return animatorSet;
    }

    private AnimatorSet createUnfillAnimation(View progressView, boolean isForward) {
        // Color animation
        int colorFrom = getResources().getColor(R.color.purple);
        int colorTo = getResources().getColor(R.color.grey);
        ValueAnimator colorAnimator = ValueAnimator.ofArgb(colorFrom, colorTo);
        colorAnimator.addUpdateListener(animator ->
                progressView.setBackgroundColor((int) animator.getAnimatedValue())
        );

        // Scale animation for unfilling effect
        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(!isForward ? 1f : 0f, !isForward ? 0f : 1f);
        scaleAnimator.addUpdateListener(animator ->
                progressView.setScaleX((float) animator.getAnimatedValue())
        );

        // Combine animations
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(colorAnimator, scaleAnimator);
        animatorSet.setDuration(500);
        animatorSet.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        return animatorSet;
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof LoginPage1) {
            updateProgressBar(1, false);
            super.onBackPressed(); // Exit activity
        } else if (currentFragment instanceof LoginPage2) {
            updateProgressBar(1, false);
            super.onBackPressed();
        } else if (currentFragment instanceof LoginPage3) {
            updateProgressBar(2, false);
            super.onBackPressed();
        }
    }
}