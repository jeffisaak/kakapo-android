package com.aptasystems.kakapo.util;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import static android.content.Context.MODE_PRIVATE;

@Singleton
public class PrefsUtil {

    private Context _context;

    private static final String SHARED_PREFERENCES = "kakapo";

    private static final String PREF_KEY_CURRENT_USER_ACCOUNT_ID = "currentUserAccountId";
    private static final String PREF_KEY_CURRENT_PASSWORD = "currentPassword";
    private static final String PREF_KEY_CURRENT_TAB_INDEX = "currentTabIndex";
    private static final String PREF_KEY_INTRO_SHOWN = "introShown";
    private static final String PREF_KEY_FILTER_TYPE_PREFIX = "filterType.";
    private static final String PREF_KEY_CONFIRMATION_PREFIX = "confirmationDialog.";

    @Inject
    public PrefsUtil(Context context) {
        _context = context.getApplicationContext();
    }

    public void clearCredentials() {
        _context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE)
                .edit()
                .remove(PREF_KEY_CURRENT_USER_ACCOUNT_ID)
                .remove(PREF_KEY_CURRENT_PASSWORD)
                .apply();
    }

    public Long getCurrentUserAccountId() {
        long currentUserAccountId = _context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE)
                .getLong(PREF_KEY_CURRENT_USER_ACCOUNT_ID, -1L);
        return currentUserAccountId == -1L ? null : currentUserAccountId;
    }

    public void setCurrentUserAccountId(Long currentUserAccountId) {
        if (currentUserAccountId == null) {
            _context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE)
                    .edit()
                    .remove(PREF_KEY_CURRENT_USER_ACCOUNT_ID)
                    .apply();
        } else {
            _context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE)
                    .edit()
                    .putLong(PREF_KEY_CURRENT_USER_ACCOUNT_ID, currentUserAccountId)
                    .apply();
        }
    }

    public String getCurrentPassword() {
        return _context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE)
                .getString(PREF_KEY_CURRENT_PASSWORD, null);
    }

    public void setCurrentPassword(String password) {
        if (password == null) {
            _context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE)
                    .edit()
                    .remove(PREF_KEY_CURRENT_PASSWORD)
                    .apply();
        } else {
            _context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE)
                    .edit()
                    .putString(PREF_KEY_CURRENT_PASSWORD, password)
                    .apply();
        }
    }

    public int getCurrentTabIndex(int defaultValue) {
        return _context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE)
                .getInt(PREF_KEY_CURRENT_TAB_INDEX, defaultValue);
    }

    public void setCurrentTabIndex(int currentTabIndex) {
        _context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE)
                .edit()
                .putInt(PREF_KEY_CURRENT_TAB_INDEX, currentTabIndex)
                .apply();
    }

    public boolean isIntroShown() {
        return _context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE)
                .getBoolean(PREF_KEY_INTRO_SHOWN, false);
    }

    public void setIntroShown(boolean introShown) {
        _context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_KEY_INTRO_SHOWN, introShown)
                .apply();
    }

    public EnumSet<FilterType> getFilterTypes() {

        String prefKey = buildUserSpecificKey(PREF_KEY_FILTER_TYPE_PREFIX);

        // Set up the default set.
        Set<String> defaultSet = new HashSet<>();
        defaultSet.add(FilterType.Mine.name());
        defaultSet.add(FilterType.Friends.name());
        defaultSet.add(FilterType.Strangers.name());

        // Get the string set from the preferences.
        Set<String> stringSet = _context
                .getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE)
                .getStringSet(prefKey, defaultSet);

        // Turn the string set into a list of enums.
        Collection<FilterType> filterTypes = new ArrayList<>();
        for (String string : stringSet) {
            filterTypes.add(FilterType.valueOf(string));
        }

        // Build an enumset and return it.
        if (!filterTypes.isEmpty()) {
            return EnumSet.copyOf(filterTypes);
        } else {
            return EnumSet.noneOf(FilterType.class);
        }
    }

    public void setFilterTypes(EnumSet<FilterType> filterTypes) {

        String prefKey = buildUserSpecificKey(PREF_KEY_FILTER_TYPE_PREFIX);

        // Turn the enumset into a set of strings.
        Set<String> stringSet = new HashSet<>();

        for (FilterType filterType : filterTypes) {
            stringSet.add(filterType.name());
        }

        _context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE)
                .edit()
                .putStringSet(prefKey, stringSet)
                .apply();
    }

    public void addFilter(FilterType filterType) {
        EnumSet<FilterType> filters = getFilterTypes();
        filters.add(filterType);
        setFilterTypes(filters);
    }

    public void removeFilter(FilterType filterType) {
        EnumSet<FilterType> filters = getFilterTypes();
        filters.remove(filterType);
        setFilterTypes(filters);
    }

    public boolean isDontAskAgain(String dialogId) {
        return _context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE)
                .getBoolean(PREF_KEY_CONFIRMATION_PREFIX + dialogId, false);
    }

    public void setDontAskAgain(String dialogId, boolean value) {
        _context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_KEY_CONFIRMATION_PREFIX + dialogId, value)
                .apply();
    }

    private String buildUserSpecificKey(String prefix) {
        return prefix + getCurrentUserAccountId();
    }
}
