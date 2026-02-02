package com.newsproject.oneroadmap.Adapters;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.newsproject.oneroadmap.Activities.LoginActivity;
import com.newsproject.oneroadmap.Activities.MainActivity;
import com.newsproject.oneroadmap.Models.OnboardingModel;
import com.newsproject.oneroadmap.R;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class OnboardingAdapter
        extends RecyclerView.Adapter<OnboardingAdapter.ViewHolder> {

    private final List<OnboardingModel> list;
    private final Context context;
    private ViewHolder holderRef;

    public OnboardingAdapter(Context context, List<OnboardingModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding, parent, false);

        holderRef = new ViewHolder(view);
        return holderRef;
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder, int position) {
        holder.bindContent(0); // 🔒 single static page
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    // 🔙 BACK handling from Activity
    public boolean handleBack() {
        return holderRef != null && holderRef.onPrevious();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        CircleImageView center, icon1, icon2, icon3, icon4;
        TextView title, subtitle;
        CardView next;
        View dot1, dot2, dot3;

        int contentIndex = 0;

        // Fixed XML angles
        final float[] baseAngles = {215f, 310f, 56f, 135f};
        float orbitRotation = 0f;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            center = itemView.findViewById(R.id.img_center);
            icon1 = itemView.findViewById(R.id.icon_1);
            icon2 = itemView.findViewById(R.id.icon_2);
            icon3 = itemView.findViewById(R.id.icon_3);
            icon4 = itemView.findViewById(R.id.icon_4);

            title = itemView.findViewById(R.id.tv_title);
            subtitle = itemView.findViewById(R.id.tv_subtitle);
            next = itemView.findViewById(R.id.btn_next);

            dot1 = itemView.findViewById(R.id.dot1);
            dot2 = itemView.findViewById(R.id.dot2);
            dot3 = itemView.findViewById(R.id.dot3);

            applyOrbitAngles();
            next.setOnClickListener(v -> onNext());
        }

        void onNext() {
            rotateOrbitClockwise();
            contentIndex++;

            if (contentIndex < list.size()) {
                bindContent(contentIndex);
            } else {
                goNextScreen();
            }
        }

        boolean onPrevious() {
            if (contentIndex == 0) return false;

            rotateOrbitAntiClockwise();
            contentIndex--;
            bindContent(contentIndex);
            return true;
        }

        void bindContent(int index) {
            OnboardingModel item = list.get(index);

            center.setImageResource(item.centerImage);
            icon1.setImageResource(item.icon1);
            icon2.setImageResource(item.icon2);
            icon3.setImageResource(item.icon3);
            icon4.setImageResource(item.icon4);

            title.setText(item.title);
            subtitle.setText(item.subtitle);

            updateIndicator(index);
        }

        // 🔁 CLOCKWISE
        void rotateOrbitClockwise() {
            animateOrbit(orbitRotation, orbitRotation += 90f);
        }

        // 🔁 ANTI-CLOCKWISE
        void rotateOrbitAntiClockwise() {
            animateOrbit(orbitRotation, orbitRotation -= 90f);
        }

        void animateOrbit(float start, float end) {
            ValueAnimator animator = ValueAnimator.ofFloat(start, end);
            animator.setDuration(500);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(a -> {
                orbitRotation = (float) a.getAnimatedValue();
                applyOrbitAngles();
            });
            animator.start();
        }

        void applyOrbitAngles() {
            setAngle(icon1, baseAngles[0] + orbitRotation);
            setAngle(icon2, baseAngles[1] + orbitRotation);
            setAngle(icon3, baseAngles[2] + orbitRotation);
            setAngle(icon4, baseAngles[3] + orbitRotation);
        }

        void setAngle(View view, float angle) {
            ConstraintLayout.LayoutParams lp =
                    (ConstraintLayout.LayoutParams) view.getLayoutParams();
            lp.circleAngle = angle % 360;
            view.setLayoutParams(lp);
        }

        void updateIndicator(int pos) {
            dot1.setBackgroundColor(0x66FFFFFF);
            dot2.setBackgroundColor(0x66FFFFFF);
            dot3.setBackgroundColor(0x66FFFFFF);

            if (pos == 0) dot1.setBackgroundColor(0xFFFFFFFF);
            else if (pos == 1) dot2.setBackgroundColor(0xFFFFFFFF);
            else dot3.setBackgroundColor(0xFFFFFFFF);
        }

        void goNextScreen() {
            boolean loggedIn =
                    context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            .getBoolean("is_logged_in", false);

            context.startActivity(
                    new Intent(context,
                            loggedIn ? MainActivity.class : LoginActivity.class)
            );
        }
    }
}
