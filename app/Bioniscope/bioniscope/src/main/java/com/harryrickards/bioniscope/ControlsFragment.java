package com.harryrickards.bioniscope;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Fragment for controlling scope settings
 */
public class ControlsFragment extends Fragment {
    OnControlChangedListener mCallback;
    TraceControlView traceOne;
    TraceControlView traceTwo;
    TextView timeDivFrequency;
    TextView timeDivPeriod;
    SeekBar timeDivSlider;
    Button captureButton;
    Button spectrumCaptureButton;
    double timeSample;

    boolean sampleButtonOneEnabled = true;
    boolean sampleButtonTwoEnabled = true;
    boolean spectrumSampleButtonOneEnabled = true;
    boolean spectrumSampleButtonTwoEnabled = true;

    final static double MIN_TIME_SAMPLE = 1;
    final static double MAX_TIME_SAMPLE = 101;
    final static double DEFAULT_TIME_SAMPLE = MIN_TIME_SAMPLE;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout for fragment
        return inflater.inflate(R.layout.controls_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        captureButton = (Button) view.findViewById(R.id.captureButton);
        spectrumCaptureButton = (Button) view.findViewById(R.id.spectrumCaptureButton);

        // Route interface methods for the two traces through to the main activity
        traceOne = (TraceControlView) getView().findViewById(R.id.traceOne);
        traceTwo = (TraceControlView) getView().findViewById(R.id.traceTwo);
        traceOne.setOnTraceControlChangedListener(new TraceControlView.OnTraceControlChangedListener() {
            @Override
            public void onTraceToggled(boolean enabled) {
                mCallback.onTraceOneToggled(enabled);
            }
            @Override
            public void onVoltsDivChanged(double value) {
                mCallback.onTraceOneVoltsDivChanged(value);
            }
        });
        traceTwo.setOnTraceControlChangedListener(new TraceControlView.OnTraceControlChangedListener() {
            @Override
            public void onTraceToggled(boolean enabled) {

                mCallback.onTraceTwoToggled(enabled);
            }
            @Override
            public void onVoltsDivChanged(double value) {
                mCallback.onTraceTwoVoltsDivChanged(value);
            }
        });

        // Call onTimeDivChanged and update labels whenever timeDivSlider changed
        timeDivSlider = (SeekBar) getView().findViewById(R.id.timeDivSlider);
        timeDivSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            // Whenever seekbar moved (even if user has not finished moving it yet)
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setTimeDivFromProgress(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            // When user has finished sliding the seekbar, call interface
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.v("scope", "onstoptrackingtouch");
                mCallback.onTimeSampleChanged(getTimeDiv());
            }
        });
        timeDivPeriod = (TextView) getView().findViewById(R.id.timeDivPeriod);
        timeDivFrequency = (TextView) getView().findViewById(R.id.timeDivFrequency);

        // Button to sample
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onSampleRequested();
            }
        });
        spectrumCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onSpectrumSampleRequested();
            }
        });

        // Set default controls
        traceOne.setTraceEnabled(true, false);
        traceTwo.setTraceEnabled(true, false);
        traceOne.setVoltsDiv(traceOne.DEFAULT_VOLTS, false);
        traceTwo.setVoltsDiv(traceTwo.DEFAULT_VOLTS, false);
        setTimeDiv(DEFAULT_TIME_SAMPLE, false);
    }

    protected void setTimeDivFromProgress(int progress) {
        // We actually store frequency due to rounding errors
        // Reduce from 0 to 100 to 1 to 5 (1+progress/25
        // Raise 10 to above to get 10 to 100k
        //setTimeDiv(Math.pow(10, 1 + ((double) progress) / 25), true);
        setTimeDiv(MIN_TIME_SAMPLE+progress*(MAX_TIME_SAMPLE-MIN_TIME_SAMPLE)/100, true);
    }

    protected void setTimeDiv(double mTimeSample, boolean fromUser) {
        //frequency = mFrequency;
        timeSample = mTimeSample;
        timeDivPeriod.setText(Double.toString(timeSample) + "us");
        timeDivFrequency.setText(SI.formatSI(1E6/timeSample)+"Hz");

        // Move seekbar to right position if updated programmatically
        if (!fromUser) {
            // Inverse of f=10^(1+p/25) is 25*(log10(f)-1)
            //int progress = (int) (25*(Math.log10(frequency)-1));
            //timeDivSlider.setProgress(progress);
            timeDivSlider.setProgress((int) (100*((mTimeSample-MIN_TIME_SAMPLE)/(MAX_TIME_SAMPLE-MIN_TIME_SAMPLE))));
        }
    }

    protected double getTimeDiv() {
        return timeSample;
    }

    public interface OnControlChangedListener {
        public void onTraceOneToggled(boolean enabled);
        public void onTraceTwoToggled(boolean enabled);
        public void onTraceOneVoltsDivChanged(double value);
        public void onTraceTwoVoltsDivChanged(double value);
        public void onTimeSampleChanged(double value);
        public void onSampleRequested();
        public void onSpectrumSampleRequested();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure activity has implemented callback interface
        try {
            mCallback = (OnControlChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnControlChangedListener");
        }
    }

    public boolean traceOneEnabled() {return (traceOne == null) || traceOne.traceEnabled();}
    public boolean traceTwoEnabled() {return (traceTwo == null) || traceTwo.traceEnabled();}

    // Grey out sample button
    public void setSampleButtonOneEnabled(boolean enabled) {
        sampleButtonOneEnabled = enabled;
        updateSampleButtonEnabled();
    }
    public void setSampleButtonTwoEnabled(boolean enabled) {
        sampleButtonTwoEnabled = enabled;
        updateSampleButtonEnabled();
    }
    protected void updateSampleButtonEnabled() {
        if (captureButton != null) {
            captureButton.setEnabled(sampleButtonOneEnabled && sampleButtonTwoEnabled);
        }
    }
    public void setSpectrumSampleButtonOneEnabled(boolean enabled) {
        spectrumSampleButtonOneEnabled = enabled;
        updateSpectrumSampleButtonEnabled();
    }
    public void setSpectrumSampleButtonTwoEnabled(boolean enabled) {
        spectrumSampleButtonTwoEnabled = enabled;
        updateSpectrumSampleButtonEnabled();
    }
    protected void updateSpectrumSampleButtonEnabled() {
        if (spectrumCaptureButton != null) {
            spectrumCaptureButton.setEnabled(spectrumSampleButtonOneEnabled && spectrumSampleButtonTwoEnabled);
        }
    }
}
