package com.harryrickards.bioniscope;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/*
    Compound view for controlling a specific trace
    Based on http://www.vogella.com/tutorials/AndroidCustomViews/article.html#compoundcontrols
 */
public class TraceControlView extends LinearLayout {
    OnTraceControlChangedListener mCallback;
    double voltsDiv;
    TextView voltsDivLabel;
    CheckBox toggle;
    SeekBar voltsDivBar;

    final static double MAX_VOLTS = 5.0;
    final static double MIN_VOLTS = 50e-3;
    final static double DEFAULT_VOLTS = MAX_VOLTS;

    public TraceControlView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TraceControlView, 0, 0);

        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_trace_control, this, true);

        // UI title (e.g., Trace 1)
        TextView title = (TextView) findViewById(R.id.traceTitle);
        title.setText("Trace " + a.getString(R.styleable.TraceControlView_traceId));

        // Label with current value of volts/div
        voltsDivLabel = (TextView) findViewById(R.id.voltsDivValue);

        // Call interface methods when checkbox toggled or slider moved
        toggle = (CheckBox) findViewById(R.id.traceToggle);
        toggle.setOnClickListener(new OnClickListener() {
            // Only called when toggled by user
            @Override
            public void onClick(View view) {
                if (mCallback != null) {
                    mCallback.onTraceToggled(((CheckBox) view).isChecked());
                }
            }
        });
        voltsDivBar = (SeekBar) findViewById(R.id.traceVoltsDiv);
        voltsDivBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            // Called when the value changes, even when user is still sliding the seekbar
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mCallback != null && fromUser) {
                    // Calculate volts/div from seekbar
                    setVoltsDivFromProgress(progress);
                }
                //if (!fromUser) {
                //    Log.w("t", Integer.toString(progress));
                //}
            }

            // When user has finished sliding the seekbar, call interface
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mCallback.onVoltsDivChanged(getVoltsDiv());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
        });
    }
    public TraceControlView(Context context) {
        this(context, null);
    }

    // Getter (from int progress) & setter for volts/div
    protected void setVoltsDivFromProgress(int progress) {
        // Calculate new voltsDiv value
        // 5mV to 500mV
        setVoltsDiv(MIN_VOLTS+((double) progress)*(MAX_VOLTS-MIN_VOLTS)/100, true);
    }
    protected void setVoltsDiv(double mVoltsDiv, boolean fromUser) {
        voltsDiv = mVoltsDiv;
        voltsDivLabel.setText("\u00B1" + SI.formatSI(voltsDiv)+"V");

        // Move seekbar to right position if updated programmatically
        if (!fromUser) {
            int progress = (int) (100*((voltsDiv-MIN_VOLTS)/(MAX_VOLTS-MIN_VOLTS)));
            voltsDivBar.setProgress(progress);
        }
    }
    protected double getVoltsDiv() {
        return voltsDiv;
    }

    // Setter for trace enabled
    protected void setTraceEnabled(boolean enabled, boolean fromUser) {
        // Toggle checkbox if updated programmatically
        if (!fromUser) {
            toggle.setChecked(enabled);
        }
    }

    // Check if enabled
    protected boolean traceEnabled() {
        return toggle.isChecked();
    }

    // Interface for when trace controls (e.g. volts/div) change
    public interface OnTraceControlChangedListener {
        public void onTraceToggled(boolean enabled);
        public void onVoltsDivChanged(double value);
    }
    public void setOnTraceControlChangedListener(OnTraceControlChangedListener listener) {
        mCallback = listener;
    }
}
