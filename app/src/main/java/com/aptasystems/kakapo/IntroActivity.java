package com.aptasystems.kakapo;

import android.content.Intent;
import android.os.Bundle;

import com.aptasystems.kakapo.util.PrefsUtil;
import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.github.paolorotolo.appintro.model.SliderPage;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class IntroActivity  extends AppIntro2 {

    @Inject
    PrefsUtil _prefsUtil;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Perform dependency injection.
        ((KakapoApplication) getApplication()).getKakapoComponent().inject(this);

        int[] titles = new int[]{
                R.string.intro_1_title,
                R.string.intro_2_title,
                R.string.intro_3_title,
                R.string.intro_4_title,
                R.string.intro_5_title,
                R.string.intro_6_title,
        };
        int[] drawables = new int[]{
                R.drawable.intro_drawable_1,
                R.drawable.intro_drawable_2,
                R.drawable.intro_drawable_3,
                R.drawable.intro_drawable_4,
                R.drawable.intro_drawable_5,
                R.drawable.intro_drawable_6,
        };
        int[] descriptions = new int[]{
                R.string.intro_1_description,
                R.string.intro_2_description,
                R.string.intro_3_description,
                R.string.intro_4_description,
                R.string.intro_5_description,
                R.string.intro_6_description,
        };

        for (int ii = 0; ii < titles.length; ii++) {
            SliderPage page = new SliderPage();
            page.setTitle(getResources().getString(titles[ii]));
            page.setDescription(getResources().getString(descriptions[ii]));
            page.setImageDrawable(drawables[ii]);
            page.setTitleColor(getResources().getColor(android.R.color.white));
            page.setDescColor(getResources().getColor(android.R.color.white));
            page.setBgColor(getResources().getColor(R.color.colorPrimaryDark));
//            addSlide(KakapoAppIntroFragment.newInstance(page));
            addSlide(AppIntroFragment.newInstance(page));
        }

//        showStatusBar(false);
//        showSeparator(true);

//        setColorDoneText(getResources().getColor(android.R.color.white));
//        setColorSkipButton(getResources().getColor(android.R.color.white));
//        setSeparatorColor(getResources().getColor(R.color.colorAccent));
//        setNextArrowColor(getResources().getColor(android.R.color.white));
//        setIndicatorColor(getResources().getColor(R.color.colorAccent),
//                getResources().getColor(R.color.colorPrimary));
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        _prefsUtil.setIntroShown(true);

        // Go right to the sign in/sign up activity
        Intent intent = new Intent(this, SelectUserAccountActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);

        _prefsUtil.setIntroShown(true);

        // Go right to the sign in/sign up activity
        Intent intent = new Intent(this, SelectUserAccountActivity.class);
        startActivity(intent);
        finish();
    }
}
