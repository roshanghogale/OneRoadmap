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
import android.view.View;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;
import com.newsproject.oneroadmap.Adapters.StoriesAdapter;
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
            replaceFragment(new ProfileFragment(), false);
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Map navigation items to fragment classes
        navItemToFragmentClassMap.put(R.id.nav_home, HomeFragment.class);
        navItemToFragmentClassMap.put(R.id.nav_chat, ChatFragment.class);
        navItemToFragmentClassMap.put(R.id.nav_main, MainFragment.class);
        navItemToFragmentClassMap.put(R.id.nav_profile, ProfileFragment.class);
        navItemToFragmentClassMap.put(R.id.nav_all_jobs, AllCategory.class);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (isProgrammaticNavigation) {
                return true;
            }

            Class<? extends Fragment> fragmentClass = navItemToFragmentClassMap.get(item.getItemId());
            if (fragmentClass != null) {
                Fragment currentFragment = getCurrentFragment();
                
                // Check if we're already showing this fragment
                if (currentFragment != null && currentFragment.getClass() == fragmentClass) {
                    return true;
                }

                try {
                    Fragment fragment = fragmentClass.newInstance();
                    
                    if (item.getItemId() == R.id.nav_home) {
                        // Home fragment - clear back stack and replace
                        clearBackStack();
                        replaceFragment(fragment, false);
                    } else {
                        // Other fragments - add to back stack
                        replaceFragment(fragment, true);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error creating fragment: " + fragmentClass.getSimpleName(), e);
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
                    if (HomeFragment.isStoriesPlayerVisible()) {
                        StoriesAdapter.StoryViewHolder.cancelViewTask();
                        StoriesAdapter.StoryViewHolder.releaseVideo();
                        if (StoriesAdapter.storiesPlayer != null) {
                            StoriesAdapter.storiesPlayer.setVisibility(View.GONE);
                        }
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
            replaceFragment(new HomeFragment(), false);
        }

        // Subscribe to user-specific topics after login
        subscribeToUserSpecificTopics();
    }

    private Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }

    private void replaceFragment(Fragment fragment, boolean addToBackStack) {
        if (fragment == null) {
            return;
        }

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ft.replace(R.id.fragment_container, fragment, fragment.getClass().getSimpleName());

        if (addToBackStack) {
            ft.addToBackStack(fragment.getClass().getSimpleName());
        }

        ft.commitAllowingStateLoss();

        updateBottomNavigationView(fragment);
        handleBottomNavigationVisibility(fragment);
    }

    private void clearBackStack() {
        FragmentManager fm = getSupportFragmentManager();
        int backStackCount = fm.getBackStackEntryCount();
        for (int i = 0; i < backStackCount; i++) {
            fm.popBackStack();
        }
    }

    private void updateBottomNavigationView(Fragment fragment) {
        if (fragment == null) {
            return;
        }

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
        boolean hide = fragment instanceof AllCategory
                || fragment instanceof BankingJobs
                || fragment instanceof PrivateJobs
                || fragment instanceof GovernmentJobs
                || fragment instanceof Result_HallTitcket
                || fragment instanceof JobUpdateDetails
                || fragment instanceof AllBannersList
                || fragment instanceof VideoFragment
                || fragment instanceof PDFViewerFragment
                || fragment instanceof WebViewFragment
                || fragment instanceof NewsFragment;

        if (hide) {
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

    private void subscribeToUserSpecificTopics() {
        if (hasSubscribedToTopics) return;

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

        // 5. Current Affairs
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
}
