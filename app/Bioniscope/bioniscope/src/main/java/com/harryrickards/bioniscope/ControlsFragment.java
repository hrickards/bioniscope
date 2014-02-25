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
    double frequency;
    TextView timeDivFrequency;
    TextView timeDivPeriod;
    SeekBar timeDivSlider;

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
                mCallback.onTimeDivChanged(getTimeDiv());
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
    }

    protected void setTimeDivFromProgress(int progress) {
        // We actually store frequency due to rounding errors
        // Reduce from 0 to 100 to 1 to 5 (1+progress/25
        // Raise 10 to above to get 10 to 100k
        setTimeDiv(Math.pow(10, 1 + ((double) progress) / 25), true);
    }

    // Note: the double argument is frequency, not time/div to reduce rounding errors
    protected void setTimeDiv(double mFrequency, boolean fromUser) {
        frequency = mFrequency;
        timeDivPeriod.setText(SI.formatSI(1 / frequency)+"s");
        timeDivFrequency.setText(SI.formatSI(frequency)+"Hz");

        // Move seekbar to right position if updated programmatically
        if (!fromUser) {
            // Inverse of f=10^(1+p/25) is 25*(log10(f)-1)
            int progress = (int) (25*(Math.log10(frequency)-1));
            timeDivSlider.setProgress(progress);
        }
    }
    // Really returns frequency
    protected double getTimeDiv() {
        return frequency;
    }

    public interface OnControlChangedListener {
        public void onTraceOneToggled(boolean enabled);
        public void onTraceTwoToggled(boolean enabled);
        public void onTraceOneVoltsDivChanged(double value);
        public void onTraceTwoVoltsDivChanged(double value);
        public void onTimeDivChanged(double value);
        public void onSampleRequested();
    }

    // Methods to set controls based on activity values
    public void setTraceOneEnabled(boolean enabled) {
        traceOne.setTraceEnabled(enabled, false);
    }
    public void setTraceTwoEnabled(boolean enabled) {
        traceTwo.setTraceEnabled(enabled, false);
    }
    public void setTraceOneVoltsDiv(double value) {
        traceOne.setVoltsDiv(value, false);
    }
    public void setTraceTwoVoltsDiv(double value) {
        traceTwo.setVoltsDiv(value, false);
    }
    public void setTimeDiv(double value) {
        setTimeDiv(value, false);
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
}
