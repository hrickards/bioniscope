package com.harryrickards.bioniscope;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

/**
 * Fragment for digital (very basic logic analyser) output of scope
 */
public class DigitalFragment extends Fragment {
    OnDigitalActionInterface mCallback;
    GraphView mGraphView;
    GraphViewSeries mSeries[];
    double mTimeSample = 2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout for fragment
        return inflater.inflate(R.layout.digital_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mGraphView = new LineGraphView(getActivity(), getString(R.string.digital_title));
        mGraphView.getGraphViewStyle().setNumHorizontalLabels(11); // 10 horizontal divisions
        mGraphView.getGraphViewStyle().setNumVerticalLabels(26); // 25 vertical divisions
        mGraphView.getGraphViewStyle().setVerticalLabelsWidth(120);
        mGraphView.setManualYAxisBounds(25, 0);
        mGraphView.setViewPort(0, 100);
        mGraphView.setScalable(true);
        mGraphView.setScrollable(true);
        mGraphView.setCustomLabelFormatter(new CustomLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                // Only format y values
                if (!isValueX) {

                }
                return null;
            }
        });

        // 8 data series (one per channel)
        mSeries = new GraphViewSeries[8];
        for (int i=0; i<8; i++) {
            mSeries[i] = new GraphViewSeries(new GraphView.GraphViewData[] {});
            mGraphView.addSeries(mSeries[i]);
        }

        // Show graph
        LinearLayout graphLayout = (LinearLayout) getView().findViewById(R.id.digitalGraph);
        graphLayout.addView(mGraphView);
    }

    public void setData(byte[] xData) {
        // To force straighter lines, we add 2 graph points for each data points
        // TODO Do this in a less hackish way
        double ghost = 0.01;

        for (int j=0; j<8; j++) {
        GraphView.GraphViewData[] data = new GraphView.GraphViewData[2*xData.length-1];

            // Add actual data points
            // mTimeSample*i gives time in us
            // Bytes are unsigned, so to convert into a positive int we have to take the absolute
            // value
            for (int i=0; i<xData.length; i++) {
                int xdi = xData[i];
                int yVal = ((xdi>>j)&0x01)+j*3;
                data[2*i] = new GraphView.GraphViewData(mTimeSample*i, yVal);
            }

            // Add data points after for all except last point
            for (int i=0; i<xData.length-1; i++) {
                int xdi = xData[i];
                int yVal = ((xdi>>j)&0x01)+j*3;
                data[2*i+1] = new GraphView.GraphViewData(mTimeSample*(i+1)-ghost, yVal);
            }
            mSeries[j].resetData(data);
        }
    }

    public interface OnDigitalActionInterface {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure activity has implemented callback interface
        try {
            mCallback = (OnDigitalActionInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnDigitalActionInterface");
        }
    }

    public void setTimeSample(double timeSample) {
        mTimeSample = timeSample;
    }
}
