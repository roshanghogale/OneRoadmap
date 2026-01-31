package com.newsproject.oneroadmap.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.newsproject.oneroadmap.Models.News;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.BuildConfig;
import com.newsproject.oneroadmap.Utils.TimeAgoUtil;
import com.newsproject.oneroadmap.Utils.ShareHelper;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.Button;
import android.widget.TextView;
import android.view.LayoutInflater;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NewsFragment extends Fragment {
    private static final String ARG_NEWS_LIST = "news_list";
    private static final String ARG_INITIAL_POSITION = "initial_position";

    private ViewPager2 viewPager;
    private List<News> newsList;
    private int initialPosition;
    private ShareHelper shareHelper;
    private ActivityResultLauncher<Intent> shareLauncher;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int displayedCoins = 0;

    public NewsFragment() {
        // Required empty public constructor
    }

    public static NewsFragment newInstance(String newsListJson, int initialPosition) {
        NewsFragment fragment = new NewsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NEWS_LIST, newsListJson);
        args.putInt(ARG_INITIAL_POSITION, initialPosition);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String newsListJson = getArguments().getString(ARG_NEWS_LIST);
            initialPosition = getArguments().getInt(ARG_INITIAL_POSITION, 0);
            
            // Deserialize news list from JSON
            Gson gson = new Gson();
            Type listType = new TypeToken<List<News>>(){}.getType();
            newsList = gson.fromJson(newsListJson, listType);
            if (newsList == null) {
                newsList = new ArrayList<>();
            }
        }
        
        // Initialize ShareHelper
        shareHelper = new ShareHelper(requireContext());
        
        // Initialize share launcher
        shareLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // When user returns from sharing, add coins and show dialog
                    if (shareHelper != null) {
                        // Get current coins to show in dialog
                        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                        String userId = prefs.getString("userId", "");
                        if (userId != null && !userId.isEmpty()) {
                            com.newsproject.oneroadmap.Utils.DatabaseHelper dbHelper = new com.newsproject.oneroadmap.Utils.DatabaseHelper(requireContext());
                            int current = dbHelper.getUserCoins(userId);
                            com.newsproject.oneroadmap.Utils.CoinManager coinManager = new com.newsproject.oneroadmap.Utils.CoinManager(requireContext(), userId);
                            coinManager.addCoinsForShare(newCoins -> {
                                // Show coin animation dialog
                                showCoinAnimationDialog(current, newCoins);
                            });
                        }
                    }
                });
        
        shareHelper.setShareLauncher(shareLauncher);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_news, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewPager = view.findViewById(R.id.news_viewpager);
        
        if (newsList != null && !newsList.isEmpty()) {
            NewsPagerAdapter adapter = new NewsPagerAdapter(newsList, shareHelper, shareLauncher);
            viewPager.setAdapter(adapter);
            
            // Improve animation smoothness
            viewPager.setOffscreenPageLimit(2); // Preload adjacent pages for smoother transitions
            
            // Set initial position
            if (initialPosition >= 0 && initialPosition < newsList.size()) {
                viewPager.setCurrentItem(initialPosition, false);
            }
            
            // Set orientation to horizontal for left/right swiping
            // Standard behavior: swipe left = next (older), swipe right = previous (newer)
            viewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
            
            // Add smooth page transformer for better animation
            viewPager.setPageTransformer((page, position) -> {
                // Smooth fade and scale animation
                float absPosition = Math.abs(position);
                if (absPosition >= 1) {
                    page.setAlpha(0f);
                } else {
                    page.setAlpha(1 - absPosition);
                    float scale = 1 - absPosition * 0.1f;
                    page.setScaleX(scale);
                    page.setScaleY(scale);
                }
            });
        }
    }

    private class NewsPagerAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<NewsPagerAdapter.NewsViewHolder> {
        private final List<News> newsList;
        private final ShareHelper shareHelper;
        private final ActivityResultLauncher<Intent> shareLauncher;

        public NewsPagerAdapter(List<News> newsList, ShareHelper shareHelper, ActivityResultLauncher<Intent> shareLauncher) {
            this.newsList = newsList;
            this.shareHelper = shareHelper;
            this.shareLauncher = shareLauncher;
        }

        @NonNull
        @Override
        public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_news_item, parent, false);
            return new NewsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
            News news = newsList.get(position);
            holder.bind(news, shareHelper);
        }

        @Override
        public int getItemCount() {
            return newsList.size();
        }

        class NewsViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            private ImageView bannerImageView;
            private TextView titleTextView;
            private TextView titleDescriptionTextView;
            private TextView subTitleTextView;
            private TextView typeTextView;
            private TextView timeAgoTextView;
            private TextView paragraph1TextView;
            private TextView paragraph2TextView;
            private TextView dateTextView;
            private View shareButtonContainer;

            public NewsViewHolder(@NonNull View itemView) {
                super(itemView);
                bannerImageView = itemView.findViewById(R.id.news_banner);
                titleTextView = itemView.findViewById(R.id.news_title);
                titleDescriptionTextView = itemView.findViewById(R.id.news_title_description);
                subTitleTextView = itemView.findViewById(R.id.news_sub_title);
                typeTextView = itemView.findViewById(R.id.news_type);
                timeAgoTextView = itemView.findViewById(R.id.news_time_ago);
                paragraph1TextView = itemView.findViewById(R.id.news_description_paragraph1);
                paragraph2TextView = itemView.findViewById(R.id.news_description_paragraph2);
                dateTextView = itemView.findViewById(R.id.news_date);
                shareButtonContainer = itemView.findViewById(R.id.share_button_container);
            }

            public void bind(News news, ShareHelper shareHelper) {
                // Set title
                setTextIfNotNull(titleTextView, news.getTitle());

                // Set title_description (from root level)
                setTextIfNotNull(titleDescriptionTextView, news.getTitleDescription());

                // Set sub_title (from root level)
                setTextIfNotNull(subTitleTextView, news.getSubTitle());

                // Set type
                if (news.getType() != null && !news.getType().isEmpty()) {
                    typeTextView.setText(news.getType());
                    typeTextView.setVisibility(View.VISIBLE);
                } else {
                    typeTextView.setVisibility(View.GONE);
                }

                // Set time ago
                String timeAgo = getTimeAgo(news);
                if (timeAgo != null && !timeAgo.isEmpty() && !timeAgo.equals("Unknown")) {
                timeAgoTextView.setText(timeAgo);
                    timeAgoTextView.setVisibility(View.VISIBLE);
                } else {
                    timeAgoTextView.setVisibility(View.GONE);
                }

                // Set date in Marathi format
                String formattedDate = formatDateToMarathi(news);
                if (formattedDate != null && !formattedDate.isEmpty()) {
                    dateTextView.setText("Date : " + formattedDate);
                    dateTextView.setVisibility(View.VISIBLE);
                } else {
                    dateTextView.setVisibility(View.GONE);
                }

                // Set description fields from description object
                News.Description description = news.getDescription();
                if (description != null) {
                    // Set paragraph1: prefer titleDescription, fallback to paragraph1
                    String paragraph1Text = null;
                    paragraph1Text = description.getParagraph1();
                    setTextIfNotNull(paragraph1TextView, paragraph1Text);
                    
                    // Set paragraph2: prefer subTitle, fallback to paragraph2
                    String paragraph2Text = null;
                    paragraph2Text = description.getParagraph2();
                    
                    setTextIfNotNull(paragraph2TextView, paragraph2Text);
                } else {
                    paragraph1TextView.setVisibility(View.GONE);
                    paragraph2TextView.setVisibility(View.GONE);
                }

                // Load image
                String imageUrl = news.getImageUrl();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    String fullUrl = buildFullUrl(imageUrl);
                    if (fullUrl != null) {
                        Glide.with(itemView.getContext())
                                .load(fullUrl)
                                .placeholder(R.drawable.student_update_1)
                                .error(R.drawable.student_update_1)
                                .into(bannerImageView);
                    } else {
                        bannerImageView.setImageResource(R.drawable.student_update_1);
                    }
                } else {
                    bannerImageView.setImageResource(R.drawable.student_update_1);
                }
                
                // Setup share button
                if (shareButtonContainer != null && shareHelper != null) {
                    shareButtonContainer.setOnClickListener(v -> {
                        // Get news title - use getTitle() method
                        String title = news.getTitle() != null ? news.getTitle() : "Latest News";
                        String bannerUrl = news.getImageUrl();
                        
                        // Build full URL for banner if it's relative
                        if (bannerUrl != null && !bannerUrl.isEmpty()) {
                            bannerUrl = buildFullUrl(bannerUrl);
                        }
                        
                        // Share only the title with banner image (same format as student update)
                        shareHelper.shareJobWithImage(title, null, bannerUrl);
                    });
                }
            }

            private void setTextIfNotNull(TextView textView, String text) {
                if (text != null && !text.isEmpty()) {
                    textView.setText(text);
                    textView.setVisibility(View.VISIBLE);
                } else {
                    textView.setVisibility(View.GONE);
                }
            }

            private String getTimeAgo(News news) {
                if (news.getCreatedAt() != null) {
                    return TimeAgoUtil.getTimeAgo(news.getCreatedAt());
                }
                if (news.getDate() != null && !news.getDate().isEmpty()) {
                    return TimeAgoUtil.getTimeAgo(news.getDate());
                }
                return "Unknown";
            }

            private String buildFullUrl(String filePath) {
                if (filePath == null || filePath.isEmpty()) return null;
                String url = filePath.startsWith("http") ? filePath : BuildConfig.BASE_URL + filePath;
                if (url.startsWith("http://")) {
                    url = url.replace("http://", "https://");
                }
                return url;
            }

            private String formatDateToMarathi(News news) {
                Date date = null;

                // Try to get date from createdAt first (preferred)
                if (news.getCreatedAt() != null) {
                    date = news.getCreatedAt();
                } else if (news.getDate() != null && !news.getDate().isEmpty()) {
                    // Try to parse date string (format: yyyy-MM-dd)
                    try {
                        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        inputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
                        date = inputFormat.parse(news.getDate());
                    } catch (Exception e1) {
                        // Try ISO format as fallback
                        try {
                            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                            date = isoFormat.parse(news.getDate());
                        } catch (Exception e2) {
                            return null;
                        }
                    }
                }

                if (date == null) return null;

                // Format in Marathi locale with Marathi digits
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", new Locale("mr", "IN"));
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
                String formatted = sdf.format(date);

                // Convert to Marathi digits
                return toMarathiDigits(formatted);
            }

            private String toMarathiDigits(String input) {
                if (input == null) return null;
                return input
                        .replace('0', '०')
                        .replace('1', '१')
                        .replace('2', '२')
                        .replace('3', '३')
                        .replace('4', '४')
                        .replace('5', '५')
                        .replace('6', '६')
                        .replace('7', '७')
                        .replace('8', '८')
                        .replace('9', '९');
            }
        }
    }
    
    private void showCoinAnimationDialog(int start, int end) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.coin_dialog_layout, null);
        TextView count = view.findViewById(R.id.coin_count);
        Button ok = view.findViewById(R.id.ok_button);
        count.setText("Coins: " + start);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        ok.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        displayedCoins = start;
        handler.post(new Runnable() {
            @Override public void run() {
                if (!isAdded() || getActivity() == null) return;
                if (displayedCoins < end) {
                    displayedCoins++;
                    count.setText("Coins: " + displayedCoins);
                    handler.postDelayed(this, 20);
                }
            }
        });
    }
}
