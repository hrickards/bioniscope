package com.harryrickards.bioniscope;

import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

/**
 * Fragment to show waveform of scope
 */
public class GraphFragment extends Fragment {
    GraphView mGraphView;
    GraphViewSeries mSeriesA;
    GraphViewSeries mSeriesB;
    double timeSample;
    double mVoltsRangeA;
    double mVoltsRangeB;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout for fragment
        return inflater.inflate(R.layout.graph_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setupGraph();
    }

    protected void setupGraph() {
        timeSample = 1;
        mVoltsRangeA = 5;
        mVoltsRangeB = 5;

        // Initiate GraphView
        mGraphView = new LineGraphView(getActivity(), getString(R.string.graph_title));
        mGraphView.getGraphViewStyle().setNumHorizontalLabels(11); // 11 horizontal divisions
        mGraphView.getGraphViewStyle().setNumVerticalLabels(11); // 10 vertical divisions
        mGraphView.getGraphViewStyle().setVerticalLabelsWidth(120);
        mGraphView.setManualYAxisBounds(5, -5);
        mGraphView.setViewPort(0, 100);
        mGraphView.setScalable(true);
        mGraphView.setScrollable(true);

        // Data series
        mSeriesA = new GraphViewSeries(new GraphView.GraphViewData[] {});
        mSeriesB = new GraphViewSeries(new GraphView.GraphViewData[] {});
        mSeriesA.getStyle().color = Color.BLUE;
        mSeriesB.getStyle().color = Color.GREEN;
        mGraphView.addSeries(mSeriesA);
        mGraphView.addSeries(mSeriesB);

        LinearLayout graphLayout = (LinearLayout) getView().findViewById(R.id.graph);
        graphLayout.addView(mGraphView);
    }

    // B iff channel, else A
    public void setData(byte[] xData, boolean channel) {
        // Get the voltage range
        double voltsRange;
        if (channel) {
            voltsRange = mVoltsRangeA;
        } else {
            voltsRange = mVoltsRangeB;
        }

        mGraphView.setManualYAxisBounds(voltsRange, -voltsRange);

        GraphView.GraphViewData[] data = new GraphView.GraphViewData[xData.length];
        for (int i=0; i<xData.length; i++) {
            // Bytes are unsigned, so to convert into a positive int we have to take the absolute
            // value
            // TODO Move these to be calibrated
            data[i] = new GraphView.GraphViewData(i*timeSample, (Math.abs((int) xData[i])-127.5)*2*voltsRange/255.0);
        }

        // Get the series to add the data to
        GraphViewSeries series;
        if (channel) {
            series = mSeriesB;
        } else {
            series = mSeriesA;
        }
        series.resetData(data);
    }

    public void setTimeSample(double mTimeSample) {
        timeSample = mTimeSample;
    }

    public void setVoltsRangeA(double voltsRangeA) {
        mVoltsRangeA = voltsRangeA;
    }

    public void setVoltsRangeB(double voltsRangeB) {
        mVoltsRangeB = voltsRangeB;
    }

    public void hideTraceOne() {mSeriesA.resetData(new GraphView.GraphViewData[] {});}
    public void hideTraceTwo() {mSeriesB.resetData(new GraphView.GraphViewData[] {});}
}
