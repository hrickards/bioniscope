package com.harryrickards.bioniscope;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Fragment for controlling scope settings
 */
public class SpectrumControlsFragment extends Fragment {
    OnSpectrumControlChangedListener mCallback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout for fragment
        return inflater.inflate(R.layout.spectrum_controls_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // TODO
    }

    public interface OnSpectrumControlChangedListener {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure activity has implemented callback interface
        try {
            mCallback = (OnSpectrumControlChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSpectrumControlChangedListener");
        }
    }
}
