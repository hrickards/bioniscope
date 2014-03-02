package com.harryrickards.bioniscope;

import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import java.util.Arrays;

/**
 * Fragment for spectrum (FFT) output of scope
 */
public class SpectrumFragment extends Fragment {
    GraphView mGraphView;
    GraphViewSeries mSeriesA, mSeriesB;
    double timeSample = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.v("scope", "spectrum onCreateView");
        // Inflate layout for fragment
        return inflater.inflate(R.layout.spectrum_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Log.v("spectrum", "spectrum onViewCreated");
        mGraphView = new LineGraphView(getActivity(), getString(R.string.spectrum_title));
        mGraphView.getGraphViewStyle().setNumHorizontalLabels(11); // 10 horizontal divisions
        mGraphView.getGraphViewStyle().setNumVerticalLabels(11); // 10 vertical divisions
        mGraphView.getGraphViewStyle().setVerticalLabelsWidth(120);
        //mGraphView.setViewPort(0, 100);
        //mGraphView.setScalable(true);
        //mGraphView.setScrollable(true);

        // Data series
        mSeriesA = new GraphViewSeries(new GraphView.GraphViewData[] {});
        mSeriesB = new GraphViewSeries(new GraphView.GraphViewData[] {});
        mSeriesA.getStyle().color = Color.BLUE;
        mSeriesB.getStyle().color = Color.GREEN;
        mGraphView.addSeries(mSeriesA);
        mGraphView.addSeries(mSeriesB);

        // Show graph
        LinearLayout graphLayout = (LinearLayout) getView().findViewById(R.id.spectrumGraph);
        graphLayout.addView(mGraphView);
    }

    // Channel A <=> channel
    public void setData(byte[] timeData, boolean channel) {
        // Run the FFT
        FFT fft = new FFT(timeData.length);

        // Input data which will contain magnitudes
        double[] magnitudes = new double[timeData.length];
        for (int i=0; i<timeData.length; i++) { magnitudes[i] = Math.abs((int) timeData[i]); }

        // Temporary array used to store imaginary parts. Prefilled with zeroes.
        double[] imag = new double[timeData.length];
        Arrays.fill(imag, 0.0);

        // Run the FFT
        fft.fft(magnitudes, imag);

        // We only care about the first N/2 entries as we're only dealing with real frequencies
        double values[] = new double[512];
        System.arraycopy(magnitudes, 0, values, 0, values.length);

        // Add results to a GraphViewData array
        GraphView.GraphViewData[] data = new GraphView.GraphViewData[values.length];
        for (int i=0; i<data.length; i++) {
            // f = i * Fs / N
            // fs = 1/T = (1000/T in us)
            double frequency = i*1e6/(1024.0*timeSample);
            // TODO y scale
            data[i] = new GraphView.GraphViewData(frequency, values[i]);
        }

        // Add data to the series
        GraphViewSeries series;
        if (channel) {
            series = mSeriesB;
        } else {
            series = mSeriesA;
        }
        series.resetData(data);
    }

    public void hideTraceOne() {mSeriesA.resetData(new GraphView.GraphViewData[] {});}
    public void hideTraceTwo() {mSeriesB.resetData(new GraphView.GraphViewData[] {});}

    public void setTimeSample(double mTimeSample) {
        timeSample = mTimeSample;
    }
}
