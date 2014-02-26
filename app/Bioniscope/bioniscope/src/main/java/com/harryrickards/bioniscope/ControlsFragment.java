package com.harryrickards.bioniscope;

import android.app.Activity;
import android.os.Bundle;
import android.os.Trace;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import com.harryrickards.bioniscope.SI;

import org.w3c.dom.Text;

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
    double timeSample;

    final static double MIN_TIME_SAMPLE = 1;
    final static double MAX_TIME_SAMPLE = 101;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout for fragment
        return inflater.inflate(R.layout.controls_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
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
                Log.w("scope", "onstoptrackingtouch");
                mCallback.onTimeSampleChanged(getTimeDiv());
            }
        });
        timeDivPeriod = (TextView) getView().findViewById(R.id.timeDivPeriod);
        timeDivFrequency = (TextView) getView().findViewById(R.id.timeDivFrequency);

        // Button to sample
        Button captureButton = (Button) view.findViewById(R.id.captureButton);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onSampleRequested();
            }
        });
        Button spectrumCaptureButton = (Button) view.findViewById(R.id.spectrumCaptureButton);
        spectrumCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onSpectrumSampleRequested();
            }
        });

        // Set default controls
        traceOne.setTraceEnabled(true, false);
        traceTwo.setTraceEnabled(true, false);
        traceOne.setVoltsDiv(traceOne.MAX_VOLTS, false);
        traceTwo.setVoltsDiv(traceTwo.MAX_VOLTS, false);
        setTimeDiv(MIN_TIME_SAMPLE, false);
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

}
