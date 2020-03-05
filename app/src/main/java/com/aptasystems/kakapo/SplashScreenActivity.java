package com.aptasystems.kakapo;

import android.content.Intent;
import android.os.Bundle;

import com.aptasystems.kakapo.util.PrefsUtil;

import javax.inject.Inject;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreenActivity extends AppCompatActivity {

    @Inject
    PrefsUtil _prefsUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Perform dependency injection.
        ((KakapoApplication) getApplication()).getKakapoComponent().inject(this);

        if (!_prefsUtil.isIntroShown()) {
            // If the intro has not yet been shown, show it.
            Intent intent = new Intent(this, IntroActivity.class);
            startActivity(intent);
            finish();
        } else if (_prefsUtil.getCurrentUserAccountId() == null ||
                _prefsUtil.getCurrentHashedPassword() == null) {
            // If we aren't signed in, redirect to the select user account activity.
            Intent intent = new Intent(this, SelectUserAccountActivity.class);
            startActivity(intent);
            finish();
        } else {
            // Otherwise, proceed to the main activity.
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
