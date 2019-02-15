package place.visual;

import org.jfree.chart.ChartPanel;

import java.util.HashMap;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.ui.ApplicationFrame;

import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class LineChart extends ApplicationFrame {
	private static final long serialVersionUID = 6921947481829900506L;
	
	private XYSeriesCollection dataset;
	private Map<String, XYSeries> lines;

	public LineChart(String title, String xaxis, String yaxis) {
		super(title);
		
		this.dataset = new XYSeriesCollection();
		this.lines = new HashMap<String, XYSeries>();
		
		JFreeChart lineChart = ChartFactory.createXYLineChart(
				title,
				xaxis,
				yaxis,
				this.dataset,
				PlotOrientation.VERTICAL,
				true,
				true,
				false);
		
		ChartPanel chartPanel = new ChartPanel(lineChart);
		chartPanel.setPreferredSize(new java.awt.Dimension(560 , 367));
		setContentPane(chartPanel);
	}
	public void addData(String line, int x, double value){
		if(!this.lines.containsKey(line)){
			XYSeries xy = new XYSeries(line);
			this.lines.put(line, xy);
			this.dataset.addSeries(xy);
		}
		this.lines.get(line).add(x, value);
	}
}