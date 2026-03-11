package com.newsproject.oneroadmap.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.pm.PackageManager;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.ads.MobileAds;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.newsproject.oneroadmap.Adapters.StoriesAdapter;
import com.newsproject.oneroadmap.Fragments.*;
import com.newsproject.oneroadmap.Models.JobUpdate;
import com.newsproject.oneroadmap.Models.News;
import com.newsproject.oneroadmap.Models.User;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.DataConstants;
import com.newsproject.oneroadmap.Utils.DatabaseHelper;
import com.newsproject.oneroadmap.Utils.NewsUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    public BottomNavigationView bottomNavigationView;
    private final Map<Integer, Class<? extends Fragment>> navItemToFragmentClassMap = new HashMap<>();
    private boolean isProgrammaticNavigation = false;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private DatabaseHelper databaseHelper;
    private boolean hasSubscribedToTopics = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, initializationStatus -> {
            Log.d(TAG, "Mobile Ads SDK initialized");
        });

        databaseHelper = new DatabaseHelper(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        navItemToFragmentClassMap.put(R.id.nav_home, HomeFragment.class);
        navItemToFragmentClassMap.put(R.id.nav_saved, SavedJobsFragment.class);
        navItemToFragmentClassMap.put(R.id.nav_main, MainFragment.class);
        navItemToFragmentClassMap.put(R.id.nav_profile, ProfileFragment.class);
        navItemToFragmentClassMap.put(R.id.nav_all_jobs, AllCategory.class);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (isProgrammaticNavigation) return true;
            Class<? extends Fragment> fragmentClass = navItemToFragmentClassMap.get(item.getItemId());
            if (fragmentClass != null) {
                Fragment currentFragment = getCurrentFragment();
                if (currentFragment != null && currentFragment.getClass() == fragmentClass) return true;
                try {
                    Fragment fragment = fragmentClass.newInstance();
                    if (item.getItemId() == R.id.nav_home) {
                        clearBackStack();
                        replaceFragment(fragment, false);
                    } else {
                        replaceFragment(fragment, true);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error creating fragment", e);
                }
                return true;
            }
            return false;
        });

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment currentFragment = getCurrentFragment();
            updateBottomNavigationView(currentFragment);
            handleBottomNavigationVisibility(currentFragment);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Fragment currentFragment = getCurrentFragment();
                FragmentManager fm = getSupportFragmentManager();
                if (currentFragment instanceof HomeFragment) {
                    if (HomeFragment.isStoriesPlayerVisible()) HomeFragment.stopStory(MainActivity.this);
                    else finishAffinity();
                } else if (fm.getBackStackEntryCount() > 0) fm.popBackStack();
                else finishAffinity();
            }
        });

        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment(), false);
            handleIntent(getIntent());
        }

        subscribeToUserSpecificTopics();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;

        // Handle Deep Link
        Uri data = intent.getData();
        if (data != null && "myapp".equals(data.getScheme()) && "profile".equals(data.getHost())) {
            replaceFragment(new ProfileFragment(), false);
            return;
        }

        // Handle Notification Navigation
        String navigateTo = intent.getStringExtra("navigate_to");
        if ("job_details".equals(navigateTo)) {
            String jobJson = intent.getStringExtra("job_data");
            if (jobJson != null) {
                JobUpdate job = new Gson().fromJson(jobJson, JobUpdate.class);
                clearBackStack();
                replaceFragment(new HomeFragment(), false);
                replaceFragment(JobUpdateDetails.newInstance(job), true);
            }
        } else if ("news_details".equals(navigateTo)) {
            String newsId = intent.getStringExtra("news_id");
            clearBackStack();
            HomeFragment homeFragment = new HomeFragment();
            Bundle args = new Bundle();
            args.putString("target_news_id", newsId);
            homeFragment.setArguments(args);
            replaceFragment(homeFragment, false);
        } else if ("pdf_navigation".equals(navigateTo)) {
            String pdfUrl = intent.getStringExtra("pdf_url");
            clearBackStack();
            HomeFragment homeFragment = new HomeFragment();
            Bundle args = new Bundle();
            args.putString("target_pdf_url", pdfUrl);
            homeFragment.setArguments(args);
            replaceFragment(homeFragment, false);
        } else if ("student_update_details".equals(navigateTo)) {
            String studentData = intent.getStringExtra("student_data");
            clearBackStack();
            HomeFragment homeFragment = new HomeFragment();
            Bundle args = new Bundle();
            args.putString("target_student_data", studentData);
            homeFragment.setArguments(args);
            replaceFragment(homeFragment, false);
        } else if ("home".equals(navigateTo)) {
            clearBackStack();
            replaceFragment(new HomeFragment(), false);
        }
    }

    private Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }

    private void replaceFragment(Fragment fragment, boolean addToBackStack) {
        if (fragment == null) return;
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_container, fragment, fragment.getClass().getSimpleName());
        if (addToBackStack) ft.addToBackStack(fragment.getClass().getSimpleName());
        ft.commitAllowingStateLoss();
        updateBottomNavigationView(fragment);
        handleBottomNavigationVisibility(fragment);
    }

    private void clearBackStack() {
        FragmentManager fm = getSupportFragmentManager();
        int backStackCount = fm.getBackStackEntryCount();
        for (int i = 0; i < backStackCount; i++) fm.popBackStack();
    }

    private void updateBottomNavigationView(Fragment fragment) {
        if (fragment == null) return;
        Integer navItemId = null;
        for (Map.Entry<Integer, Class<? extends Fragment>> entry : navItemToFragmentClassMap.entrySet()) {
            if (entry.getValue() == fragment.getClass()) {
                navItemId = entry.getKey();
                break;
            }
        }
        if (navItemId != null) {
            final Integer finalNavItemId = navItemId;
            uiHandler.post(() -> {
                isProgrammaticNavigation = true;
                bottomNavigationView.setSelectedItemId(finalNavItemId);
                isProgrammaticNavigation = false;
            });
        }
    }

    private void handleBottomNavigationVisibility(Fragment fragment) {
        boolean hide = fragment instanceof AllCategory || fragment instanceof BankingJobs || fragment instanceof PrivateJobs
                || fragment instanceof GovernmentJobs || fragment instanceof Result_HallTitcket || fragment instanceof JobUpdateDetails
                || fragment instanceof AllBannersList || fragment instanceof VideoFragment || fragment instanceof WebViewFragment
                || fragment instanceof NewsFragment;

        if (hide) hideBottomNavigation();
        else showBottomNavigation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) Log.d("Permission", "POST_NOTIFICATIONS granted");
        }
    }

    public void hideBottomNavigation() {
        if (bottomNavigationView != null) bottomNavigationView.animate().translationY(bottomNavigationView.getHeight()).setDuration(500).start();
    }

    public void showBottomNavigation() {
        if (bottomNavigationView != null) bottomNavigationView.animate().translationY(0).setDuration(500).start();
    }

    private void subscribeToUserSpecificTopics() {
        if (hasSubscribedToTopics) return;
        SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sp.getString("userId", null);
        if (userId == null) return;
        User user = databaseHelper.getUser(userId);
        if (user == null) return;

        Set<String> topics = new HashSet<>();
        FirebaseMessaging fm = FirebaseMessaging.getInstance();

        String ageGroup = user.getAgeGroup();
        if (ageGroup != null) {
            if (ageGroup.equals("14 ते 18")) topics.add("14to18");
            else if (ageGroup.equals("19 ते 25")) topics.add("19to25");
            else if (ageGroup.equals("26 ते 31")) topics.add("26to31");
            else if (ageGroup.equals("32 पेक्षा जास्त")) topics.add("32above");
        }

        String twelfth = user.getTwelfth();
        if (twelfth != null) {
            if (twelfth.equals("सध्या दहावीला आहे")) topics.add("10th");
            else if (twelfth.equals("सध्या बारावीला आहे")) topics.add("12th");
            else if (twelfth.equals("माझ या पुढील शिक्षण आहे")) {
                String degree = user.getDegree();
                if (degree != null && !degree.equals("Select Degree") && !degree.isEmpty()) topics.add(sanitizeTopic(degree));
            }
        }

        if (user.isCurrentAffairs()) topics.add("currentaffairs");
        if (user.isJobs()) { topics.add("10th"); topics.add("12th"); }
        if (user.isStudyGovernment()) topics.add("governmentfree");
        if (user.isStudyPoliceDefence()) topics.add("policefree");
        if (user.isStudyBanking()) topics.add("bankingfree");

        String taluka = user.getTaluka();
        if (taluka != null && !taluka.equals("Select Taluka") && !taluka.isEmpty()) topics.add(sanitizeTopic(taluka));

        topics.add("all"); topics.add("news"); topics.add("dpaper");

        for (String topic : topics) fm.subscribeToTopic(topic);
        hasSubscribedToTopics = true;
    }

    private String sanitizeTopic(String input) {
        if (input == null) return "";
        return input.replace("&", "and").replaceAll("[^A-Za-z0-9]", "");
    }
}
