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

        // Format labels with SI prefixes
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

        LinearLayout graphLayout = (LinearLayout) getView().findViewById(R.id.graph);
        graphLayout.addView(mGraphView);
    }

    // Set the vertical bounds of the graph
    protected void setYBounds(double lowerBound, double upperBound) {
        mGraphView.setManualYAxisBounds(upperBound, lowerBound);
        mGraphView.redrawAll();
    }
}
