package se.rosenbaum.bitcoiniblt.chart;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;

import java.awt.image.BufferedImage;

public class BarChart {
    private int[] xValues;
    private int[] yValues;

    private String xCaption;
    private String yCaption;

    public BarChart(int[] xValues, int[] yValues, String xCaption, String yCaption) {
        this.xValues = xValues;
        this.yValues = yValues;
        this.xCaption = xCaption;
        this.yCaption = yCaption;
    }

    public BufferedImage getImage() {
        DefaultCategoryDataset dataSet = new DefaultCategoryDataset();
        for (int i = 0; i < xValues.length; i++) {
            dataSet.addValue(Integer.valueOf(yValues[i]), "apa", Integer.valueOf(xValues[i]));
        }
        JFreeChart chart = ChartFactory.createBarChart("", xCaption, yCaption, dataSet, PlotOrientation.HORIZONTAL,
                false, false, false);

        return chart.createBufferedImage(600,400);

    }
}