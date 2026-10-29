package com.salmanmalvasi.studentcc.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import com.salmanmalvasi.studentcc.R;
import com.salmanmalvasi.studentcc.animations.AlphaAnimation;
import com.salmanmalvasi.studentcc.animations.ResizeAnimation;

public class PerformanceCard extends LinearLayout {
    AppCompatTextView title, scoreText, subtitleText;
    Context context;
    ProgressBar scoreProgress;

    JSONObject defaultValues;

    public PerformanceCard(Context context) {
        super(context);
        this.context = context;
        this.title = new AppCompatTextView(context);
        this.scoreText = new AppCompatTextView(context);
        this.subtitleText = new AppCompatTextView(context);
        this.scoreProgress = new ProgressBar(context, null, 0, R.style.Widget_VTOP_CircularProgressBar);
        this.initialize();
    }

    public PerformanceCard(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        this.title = new AppCompatTextView(context);
        this.scoreText = new AppCompatTextView(context);
        this.subtitleText = new AppCompatTextView(context);
        this.scoreProgress = new ProgressBar(context, null, 0, R.style.Widget_VTOP_CircularProgressBar);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PerformanceCard, 0, 0);

        try {
            this.setTitle(a.getString(R.styleable.PerformanceCard_title));
            this.setScore(
                    (double) a.getFloat(R.styleable.PerformanceCard_score, 0),
                    (double) a.getFloat(R.styleable.PerformanceCard_total, 100)
            );
        } finally {
            a.recycle();
        }

        this.initialize();
    }

    private void initialize() {
        float pixelDensity = this.context.getResources().getDisplayMetrics().density;
        this.defaultValues = new JSONObject();

        TypedValue colorOnSurface = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.colorOnSurface, colorOnSurface, true);

        TypedValue colorOnSurfaceVariant = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.colorOnSurfaceVariant, colorOnSurfaceVariant, true);

        this.setOrientation(VERTICAL);
        this.setGravity(Gravity.CENTER);
        // Build a solid card background using theme surface + outline colors
        android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
        cardBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(16 * pixelDensity);
        TypedValue surfaceColor = new TypedValue();
        getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, surfaceColor, true);
        cardBg.setColor(surfaceColor.data);
        TypedValue outlineColor = new TypedValue();
        getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOutline, outlineColor, true);
        cardBg.setStroke((int)(1 * pixelDensity), outlineColor.data);
        this.setBackground(cardBg);
        this.setPadding(
                (int) (12 * pixelDensity),
                (int) (12 * pixelDensity),
                (int) (12 * pixelDensity),
                (int) (10 * pixelDensity)
        );

        // Centered Title
        LayoutParams titleParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        titleParams.gravity = Gravity.CENTER_HORIZONTAL;
        titleParams.setMargins(0, 0, 0, (int) (6 * pixelDensity));
        this.title.setLayoutParams(titleParams);
        this.title.setGravity(Gravity.CENTER);
        this.title.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        this.title.setTextColor(colorOnSurface.data);
        this.title.setTextSize(13);
        this.title.setTypeface(this.title.getTypeface(), Typeface.BOLD);
        this.title.setSingleLine(true);
        this.title.setMaxLines(1);
        this.title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        this.addView(this.title);

        // Circular Progress + Score Container
        RelativeLayout container = new RelativeLayout(this.context);
        LayoutParams containerParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );
        containerParams.gravity = Gravity.CENTER_HORIZONTAL;
        container.setLayoutParams(containerParams);

        RelativeLayout.LayoutParams scoreProgressParams = new RelativeLayout.LayoutParams(
                (int) (84 * pixelDensity),
                (int) (84 * pixelDensity)
        );
        scoreProgressParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        this.scoreProgress.setIndeterminateDrawable(ContextCompat.getDrawable(this.getContext(), R.drawable.background_circular_progress_indeterminate));
        this.scoreProgress.setLayoutParams(scoreProgressParams);

        RelativeLayout.LayoutParams scoreTextParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        scoreTextParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        this.scoreText.setLayoutParams(scoreTextParams);
        this.scoreText.setGravity(Gravity.CENTER);
        this.scoreText.setTextColor(colorOnSurface.data);
        this.scoreText.setTextSize(16);
        this.scoreText.setTypeface(this.scoreText.getTypeface(), Typeface.BOLD);

        container.addView(this.scoreProgress);
        container.addView(this.scoreText);
        this.addView(container);

        // Subtitle Text below circular progress
        LayoutParams subtitleParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        subtitleParams.gravity = Gravity.CENTER_HORIZONTAL;
        subtitleParams.setMargins(0, (int) (8 * pixelDensity), 0, 0);
        this.subtitleText.setLayoutParams(subtitleParams);
        this.subtitleText.setGravity(Gravity.CENTER);
        this.subtitleText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        this.subtitleText.setTextColor(colorOnSurfaceVariant.data);
        this.subtitleText.setTextSize(11);
        this.subtitleText.setVisibility(GONE);
        this.addView(this.subtitleText);
    }

    public void setTitle(String title) {
        this.title.setText(title);
    }

    public void setSubtitle(String subtitle) {
        if (subtitle != null && !subtitle.trim().isEmpty()) {
            this.subtitleText.setText(subtitle);
            this.subtitleText.setVisibility(VISIBLE);
        } else {
            this.subtitleText.setVisibility(GONE);
        }
    }

    public void hideScoreDisplay() {
        this.scoreProgress.setVisibility(GONE);
        this.scoreText.setVisibility(GONE);
    }

    public void showScoreDisplay() {
        this.scoreProgress.setVisibility(VISIBLE);
        this.scoreText.setVisibility(VISIBLE);
    }

    public void setRelativePerformance(String relativeValue) {
        if (relativeValue != null && !relativeValue.trim().isEmpty()) {
            this.hideScoreDisplay();
            this.scoreText.setText(relativeValue);
            this.scoreText.setVisibility(VISIBLE);
            this.subtitleText.setText("weightage from average");
            this.subtitleText.setVisibility(VISIBLE);
        } else {
            this.hide();
        }
    }

    public void setScore(Double score, Double max) {
        score = Math.ceil(score);
        max = Math.ceil(max);

        this.scoreProgress.setProgress(score.intValue(), true);
        this.scoreProgress.setSecondaryProgress(max.intValue());

        String scoreText = (max >= 100) ? score.intValue() + "%" : score.intValue() + "/" + max.intValue();
        this.scoreText.setText(scoreText);
    }

    public void setIndeterminate(boolean indeterminate) {
        this.scoreProgress.setIndeterminate(indeterminate);

        if (indeterminate) {
            this.scoreText.setVisibility(INVISIBLE);
            this.subtitleText.setVisibility(GONE);
        } else {
            this.scoreText.setVisibility(VISIBLE);
        }
    }

    public void show() {
        this.clearAnimation();
        this.post(() -> {
            try {
                LinearLayout card = this;
                int cardWidth = this.defaultValues.getInt("cardWidth");

                ResizeAnimation expandAnimation = new ResizeAnimation(card, ResizeAnimation.DIRECTION_X, cardWidth, this.defaultValues);
                AlphaAnimation alphaAnimation = new AlphaAnimation(card, 1);

                expandAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        card.startAnimation(alphaAnimation);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });

                card.setVisibility(VISIBLE);
                card.startAnimation(expandAnimation);
            } catch (Exception ignored) {
            }
        });
    }

    public void hide() {
        this.clearAnimation();
        this.post(() -> {
            try {
                LinearLayout card = this;

                if (!this.defaultValues.has("cardWidth")) {
                    LayoutParams layoutParams = (LayoutParams) card.getLayoutParams();

                    this.defaultValues.put("cardWidth", card.getMeasuredWidth());
                    this.defaultValues.put("marginStart", layoutParams.getMarginStart());
                    this.defaultValues.put("marginEnd", layoutParams.getMarginEnd());
                }

                ResizeAnimation compressAnimation = new ResizeAnimation(card, ResizeAnimation.DIRECTION_X, 0, this.defaultValues);
                AlphaAnimation alphaAnimation = new AlphaAnimation(card, 0);

                alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        card.setVisibility(INVISIBLE);
                        card.startAnimation(compressAnimation);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });

                card.startAnimation(alphaAnimation);
            } catch (Exception ignored) {
            }
        });
    }
}
