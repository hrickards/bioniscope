package com.harryrickards.bioniscope;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jjoe64.graphview.CustomLabelFormatter;
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
        // Initiate GraphView
        mGraphView = new LineGraphView(getActivity(), "Bioniscope");
        mGraphView.getGraphViewStyle().setNumHorizontalLabels(21); // 20 horizontal divisions
        mGraphView.getGraphViewStyle().setNumVerticalLabels(21); // 20 vertical divisions
        mGraphView.getGraphViewStyle().setVerticalLabelsWidth(120);
        mGraphView.setManualYAxisBounds(256, 0);
        mGraphView.setViewPort(0, 100);
        mGraphView.setScalable(true);
        mGraphView.setScrollable(true);

        // Data series
        mSeriesA = new GraphViewSeries(new GraphView.GraphViewData[] {});
        mSeriesB = new GraphViewSeries(new GraphView.GraphViewData[] {});
        mGraphView.addSeries(mSeriesA);
        mGraphView.addSeries(mSeriesB);

        // Format labels with SI prefixes
        /*
        mGraphView.setCustomLabelFormatter(new CustomLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                String units = isValueX ? "s" : "V";
                String prefix;
                if (value > 1e-4) { prefix = "+"; }
                else if (value > -1e-4) { prefix = " "; }
                else { prefix = ""; }

                return prefix + SI.formatSI(value) + units;
            }
        });
        */

        LinearLayout graphLayout = (LinearLayout) getView().findViewById(R.id.graph);
        graphLayout.addView(mGraphView);
    }

    // Set the vertical bounds of the graph
    protected void setYBounds(double lowerBound, double upperBound) {
        /*
        mGraphView.setManualYAxisBounds(upperBound, lowerBound);
        mGraphView.redrawAll();
        */
    }

    // B iff channel, else A
    public void setData(byte[] xData, boolean channel) {
        GraphView.GraphViewData[] data = new GraphView.GraphViewData[xData.length];
        for (int i=0; i<xData.length; i++) {
            // Bytes are unsigned, so to convert into a positive int we have to take the absolute
            // value
            data[i] = new GraphView.GraphViewData(i, Math.abs((int) xData[i]));
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
}
