package com.harryrickards.bioniscope;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

/**
 * Fragment for digital (very basic logic analyser) output of scope
 */
public class DigitalFragment extends Fragment {
    OnDigitalActionInterface mCallback;
    GraphView mGraphView;
    GraphViewSeries mSeries;

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
        mGraphView.getGraphViewStyle().setNumVerticalLabels(11); // 10 vertical divisions
        mGraphView.getGraphViewStyle().setVerticalLabelsWidth(120);
        mGraphView.setManualYAxisBounds(256, 0);
        mGraphView.setViewPort(0, 100);
        mGraphView.setScalable(true);
        mGraphView.setScrollable(true);

        // Data series
        mSeries = new GraphViewSeries(new GraphView.GraphViewData[] {});
        mGraphView.addSeries(mSeries);

        // Show graph
        LinearLayout graphLayout = (LinearLayout) getView().findViewById(R.id.digitalGraph);
        graphLayout.addView(mGraphView);
    }

    public void setData(byte[] xData) {
        // To force straighter lines, we add 2 graph points for each data points
        // TODO Do this in a less hackish way
        double ghost = 0.01;

        GraphView.GraphViewData[] data = new GraphView.GraphViewData[2*xData.length-1];

        // Add actual data points
        // 0.5*i gives time in us
        // Bytes are unsigned, so to convert into a positive int we have to take the absolute
        // value
        for (int i=0; i<xData.length; i++) {
            data[2*i] = new GraphView.GraphViewData(0.5*i, Math.abs((int) xData[i]));
        }

        // Add data points after for all except last point
        for (int i=0; i<xData.length-1; i++) {
            data[2*i+1] = new GraphView.GraphViewData(0.5*(i+1)-ghost, Math.abs((int) xData[i]));
        }
        mSeries.resetData(data);
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
}
