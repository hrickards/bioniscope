package com.harryrickards.bioniscope;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Fragment for controlling scope settings
 */
public class DigitalControlsFragment extends Fragment {
    OnDigitalControlChangedListener mCallback;
    SeekBar timeSampleSlider;
    TextView timeSampleLabel;
    double mTimeSample;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout for fragment
        return inflater.inflate(R.layout.digital_controls_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Button captureButton = (Button) view.findViewById(R.id.digitalCaptureButton);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onDigitalSampleRequested();
            }
        });

        timeSampleSlider = (SeekBar) view.findViewById(R.id.timeSampleSlider);
        timeSampleLabel = (TextView) view.findViewById(R.id.timeSampleLabel);
        setTimeSample(2);

        timeSampleSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setTimeSample(sliderToTimeSample(progress), true);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mCallback.onDigitalTimeSampleChanged(mTimeSample);
            }
        });
    }

    public interface OnDigitalControlChangedListener {
        public void onDigitalSampleRequested();
        public void onDigitalTimeSampleChanged(double timeSample);
    }

    // timeSapmle in us
    protected void setTimeSample(double timeSample, boolean fromUser) {
        mTimeSample = timeSample;
        if (!fromUser) {
            mCallback.onDigitalTimeSampleChanged(timeSample);
            timeSampleSlider.setProgress(timeSampleToSlider(timeSample));
        }
        timeSampleLabel.setText(Double.toString(timeSample) + "us");
    }
    protected void setTimeSample(double timeSample) {setTimeSample(timeSample, false); };

    protected int timeSampleToSlider(double timeSample) {
        // timeSample is in us
        // double ranges from 0 to 100
        return (int) timeSample-2;
    }

    protected double sliderToTimeSample(int sliderValue) {
        // Reverses timeSampleToSlider
        return (double) sliderValue+2;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure activity has implemented callback interface
        try {
            mCallback = (OnDigitalControlChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnDigitalControlChangedListener");
        }
    }
}
