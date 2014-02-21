package com.harryrickards.bioniscope;

import com.harryrickards.bioniscope.R;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        // Show up button
        // TODO Use getSupportActionBar
        // To to this, need a PreferenceActivity that extends from ActionBarActivity
        // getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // When up button pressed, navigate back
            case android.R.id.home:
                Intent i = new Intent(this, MainActivity.class);
                // Use the old MainActivity instance, rather than starting a new one
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                NavUtils.navigateUpTo(this, i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}