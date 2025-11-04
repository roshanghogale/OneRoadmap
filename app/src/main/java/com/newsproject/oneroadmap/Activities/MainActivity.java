package com.newsproject.oneroadmap.Activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;

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
import com.newsproject.oneroadmap.R;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    public BottomNavigationView bottomNavigationView;
    private final Map<Class<? extends Fragment>, Integer> fragmentToNavItemMap = new HashMap<>();
    private final Map<Integer, Fragment> navItemToFragmentMap = new HashMap<>();
    private boolean isProgrammaticNavigation = false;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request notification permission (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }

        // Subscribe to Firebase topics
        String[] topics = {"science", "arts", "commerce", "business"};
        for (String topic : topics) {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("notification testing", "Subscribed to topic: " + topic);
                        } else {
                            Log.e("notification testing", "Failed to subscribe topic: " + topic);
                        }
                    });
        }

        // Handle deep link to profile
        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null && "myapp".equals(data.getScheme()) && "profile".equals(data.getHost())) {
            showFragment(new ProfileFragment(), true);
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation);

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
                    // Clear back stack when Home is clicked
                    clearBackStack();
                    if (topFragment == null || topFragment.getClass() != selectedFragment.getClass()) {
                        showFragment(selectedFragment, false);
                    }
                } else {
                    if (topFragment == null || topFragment.getClass() != selectedFragment.getClass()) {
                        showFragment(selectedFragment, true); // add to back stack
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
                        finishAffinity(); // exit app
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
    }

    private void showFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        // Hide all fragments
        for (Fragment frag : fm.getFragments()) {
            if (frag != null && frag.isVisible()) ft.hide(frag);
        }

        // Show or add the fragment
        if (fragment.isAdded()) ft.show(fragment);
        else ft.add(R.id.fragment_container, fragment, fragment.getClass().getSimpleName());

        if (addToBackStack) ft.addToBackStack(fragment.getClass().getSimpleName());
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
        if (fragment instanceof AllCategory
                || fragment instanceof BankingJobs
                || fragment instanceof PrivateJobs
                || fragment instanceof GovernmentJobs
                || fragment instanceof Result_HallTitcket
                || fragment instanceof JobUpdateDetails
                || fragment instanceof AllBannersList
                || fragment instanceof VideoFragment
                || fragment instanceof PDFViewerFragment) {
            hideBottomNavigation();
        } else {
            showBottomNavigation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission", "POST_NOTIFICATIONS permission granted");
            } else {
                Log.w("Permission", "POST_NOTIFICATIONS permission denied");
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
