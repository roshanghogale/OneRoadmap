package com.newsproject.oneroadmap.Activities;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.pm.PackageManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;
import com.newsproject.oneroadmap.Fragments.*;
import com.newsproject.oneroadmap.Models.User;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.DataConstants;
import com.newsproject.oneroadmap.Utils.DatabaseHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    public BottomNavigationView bottomNavigationView;
    private final Map<Class<? extends Fragment>, Integer> fragmentToNavItemMap = new HashMap<>();
    private final Map<Integer, Fragment> navItemToFragmentMap = new HashMap<>();
    private boolean isProgrammaticNavigation = false;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private DatabaseHelper databaseHelper;
    private boolean hasSubscribedToTopics = false; // Prevent duplicate subscriptions

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(this);

        // Request notification permission (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }

        // Handle deep link to profile
        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null && "myapp".equals(data.getScheme()) && "profile".equals(data.getHost())) {
            showFragment(new ProfileFragment(), true);
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Map fragments to navigation items
        fragmentToNavItemMap.put(HomeFragment.class, R.id.nav_home);
        fragmentToNavItemMap.put(ChatFragment.class, R.id.nav_chat);
        fragmentToNavItemMap.put(MainFragment.class, R.id.nav_main);
        fragmentToNavItemMap.put(ProfileFragment.class, R.id.nav_profile);
        fragmentToNavItemMap.put(AllCategory.class, R.id.nav_all_jobs);

        navItemToFragmentMap.put(R.id.nav_home, new HomeFragment());
        navItemToFragmentMap.put(R.id.nav_chat, new ChatFragment());
        navItemToFragmentMap.put(R.id.nav_main, new MainFragment());
        navItemToFragmentMap.put(R.id.nav_profile, new ProfileFragment());
        navItemToFragmentMap.put(R.id.nav_all_jobs, new AllCategory());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (isProgrammaticNavigation) return true;

            Fragment selectedFragment = navItemToFragmentMap.get(item.getItemId());
            if (selectedFragment != null) {
                Fragment topFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

                if (item.getItemId() == R.id.nav_home) {
                    clearBackStack();
                    if (topFragment == null || topFragment.getClass() != selectedFragment.getClass()) {
                        showFragment(selectedFragment, false);
                    }
                } else {
                    if (topFragment == null || topFragment.getClass() != selectedFragment.getClass()) {
                        showFragment(selectedFragment, true);
                    }
                }
                return true;
            }
            return false;
        });

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment topFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            updateBottomNavigationView(topFragment);
            handleBottomNavigationVisibility(topFragment);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fm = getSupportFragmentManager();
                Fragment topFragment = fm.findFragmentById(R.id.fragment_container);

                if (topFragment instanceof HomeFragment) {
                    if (HomeFragment.isStoriesPlayerVisible()) {
                        HomeFragment.stopStory(MainActivity.this);
                    } else {
                        finishAffinity();
                    }
                } else if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                } else {
                    finishAffinity();
                }
            }
        });

        if (savedInstanceState == null) {
            showFragment(navItemToFragmentMap.get(R.id.nav_home), false);
        }

        // Subscribe to user-specific topics after login
        subscribeToUserSpecificTopics();
    }

    private void subscribeToUserSpecificTopics() {
        if (hasSubscribedToTopics) return;

        // Get userId from SharedPreferences
        SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sp.getString("userId", null);

        if (userId == null) {
            Log.d("FCM", "No userId in SharedPreferences. Skipping topic subscription.");
            return;
        }

        User user = databaseHelper.getUser(userId);
        if (user == null) {
            Log.d("FCM", "No user in SQLite yet. Skipping topic subscription.");
            return;
        }

        List<String> topics = new ArrayList<>();
        FirebaseMessaging fm = FirebaseMessaging.getInstance();

        // 1. Twelfth-based topics
        String twelfth = user.getTwelfth();
        int twelfthIndex = DataConstants.TWELFTH_OPTIONS.indexOf(twelfth);

        if (twelfthIndex == 1) {
            topics.add("10th");
        } else if (twelfthIndex == 2) {
            topics.add("12th");
        } else if (twelfthIndex == 3) {
            String degree = user.getDegree();
            if (degree != null && !degree.equals("Select Degree") && !degree.isEmpty()) {
                String clean = degree.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
                if (!clean.isEmpty()) topics.add(clean);
            }
        } else if (twelfthIndex == 4) {
            topics.add("10th");
            topics.add("12th");
        }

        // 2. Taluka
        String taluka = user.getTaluka();
        if (taluka != null && !taluka.equals("Select Taluka") && !taluka.isEmpty()) {
            String clean = taluka.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
            if (!clean.isEmpty()) topics.add(clean);
        }

        // 3. Free Study Materials
        if (user.isStudyGovernment()) topics.add("governmentfree");
        if (user.isStudyPoliceDefence()) topics.add("policefree");
        if (user.isStudyBanking()) topics.add("bankingfree");
        if (user.isStudySelfImprovement()) topics.add("selfimprovementfree");

        // 4. Job by Stream
        if (user.isJobs()) {
            topics.add("jobsteam");
        }

        // 5. NEW: Current Affairs
        if (user.isCurrentAffairs()) {
            topics.add("currentaffairs");
        }

        topics.add("all");
        topics.add("d_paper");
        topics.add("news");


        // Subscribe to all
        for (String topic : topics) {
            fm.subscribeToTopic(topic).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("FCM", "Subscribed to: " + topic);
                } else {
                    Log.e("FCM", "Failed to subscribe: " + topic);
                }
            });
        }

        hasSubscribedToTopics = true;
        Log.d("FCM", "User-specific topics subscribed: " + topics);
    }

    private void showFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        for (Fragment frag : fm.getFragments()) {
            if (frag != null && frag.isVisible()) ft.hide(frag);
        }

        if (fragment.isAdded()) {
            ft.show(fragment);
        } else {
            ft.add(R.id.fragment_container, fragment, fragment.getClass().getSimpleName());
        }

        if (addToBackStack) {
            ft.addToBackStack(fragment.getClass().getSimpleName());
        }
        ft.commit();

        updateBottomNavigationView(fragment);
        handleBottomNavigationVisibility(fragment);
    }

    private void clearBackStack() {
        FragmentManager fm = getSupportFragmentManager();
        for (int i = 0; i < fm.getBackStackEntryCount(); i++) {
            fm.popBackStack();
        }
    }

    private void updateBottomNavigationView(Fragment fragment) {
        if (fragment == null) return;

        Integer navItemId = fragmentToNavItemMap.get(fragment.getClass());
        if (navItemId != null) {
            uiHandler.post(() -> {
                isProgrammaticNavigation = true;
                bottomNavigationView.setSelectedItemId(navItemId);
                isProgrammaticNavigation = false;
            });
        }
    }

    private void handleBottomNavigationVisibility(Fragment fragment) {
        boolean hide = fragment instanceof AllCategory
                || fragment instanceof BankingJobs
                || fragment instanceof PrivateJobs
                || fragment instanceof GovernmentJobs
                || fragment instanceof Result_HallTitcket
                || fragment instanceof JobUpdateDetails
                || fragment instanceof AllBannersList
                || fragment instanceof VideoFragment
                || fragment instanceof PDFViewerFragment;

        if (hide) hideBottomNavigation();
        else showBottomNavigation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission", "POST_NOTIFICATIONS granted");
            } else {
                Log.w("Permission", "POST_NOTIFICATIONS denied");
            }
        }
    }

    public void hideBottomNavigation() {
        if (bottomNavigationView != null) {
            bottomNavigationView.animate()
                    .translationY(bottomNavigationView.getHeight())
                    .setDuration(500)
                    .start();
        }
    }

    public void showBottomNavigation() {
        if (bottomNavigationView != null) {
            bottomNavigationView.animate()
                    .translationY(0)
                    .setDuration(500)
                    .start();
        }
    }
}