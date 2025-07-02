package com.lonewalker.bluetoothmessenger.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.lonewalker.bluetoothmessenger.R;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 3000; // 3 seconds
    private static final long LETTER_DELAY = 150; // 150ms between each letter

    private TextView[] letters;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        handler = new Handler(Looper.getMainLooper());
        initializeLetters();
        startLetterAnimation();
    }

    private void initializeLetters() {
        letters = new TextView[10];
        letters[0] = findViewById(R.id.letterL);
        letters[1] = findViewById(R.id.letterO);
        letters[2] = findViewById(R.id.letterN);
        letters[3] = findViewById(R.id.letterE);
        letters[4] = findViewById(R.id.letterW);
        letters[5] = findViewById(R.id.letterA);
        letters[6] = findViewById(R.id.letterL2);
        letters[7] = findViewById(R.id.letterK);
        letters[8] = findViewById(R.id.letterE2);
        letters[9] = findViewById(R.id.letterR);
    }

    private void startLetterAnimation() {
        // Start animation for each letter with delay
        for (int i = 0; i < letters.length; i++) {
            final int index = i;
            handler.postDelayed(() -> animateLetter(letters[index]), i * LETTER_DELAY);
        }

        // Navigate to MainActivity after all animations
        handler.postDelayed(this::navigateToMain, SPLASH_DELAY);
    }

    private void animateLetter(TextView letter) {
        // Create fade in animation
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(letter, "alpha", 0f, 1f);
        fadeIn.setDuration(800);
        fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());

        // Create slide up animation
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(letter, "translationY", 50f, 0f);
        slideUp.setDuration(800);
        slideUp.setInterpolator(new BounceInterpolator());

        // Create scale animation
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(letter, "scaleX", 0.5f, 1.2f, 1f);
        scaleX.setDuration(800);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(letter, "scaleY", 0.5f, 1.2f, 1f);
        scaleY.setDuration(800);
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        // Create rotation animation
        ObjectAnimator rotation = ObjectAnimator.ofFloat(letter, "rotation", -10f, 10f, 0f);
        rotation.setDuration(800);
        rotation.setInterpolator(new AccelerateDecelerateInterpolator());

        // Play all animations together
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(fadeIn, slideUp, scaleX, scaleY, rotation);
        animatorSet.start();

        // Add a subtle glow effect
        letter.setElevation(10f);
        handler.postDelayed(() -> letter.setElevation(0f), 800);
    }

    private void navigateToMain() {
        // Create fade out animation for all letters
        AnimatorSet fadeOutSet = new AnimatorSet();
        ObjectAnimator[] fadeOuts = new ObjectAnimator[letters.length];
        
        for (int i = 0; i < letters.length; i++) {
            fadeOuts[i] = ObjectAnimator.ofFloat(letters[i], "alpha", 1f, 0f);
            fadeOuts[i].setDuration(500);
        }
        
        fadeOutSet.playTogether(fadeOuts);
        fadeOutSet.start();

        // Navigate to MainActivity after fade out
        fadeOutSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
} 