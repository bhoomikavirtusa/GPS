package com.wiley.sf.common.benchmark;

import java.awt.BasicStroke;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.wiley.sf.common.config.PropertiesUtil;
import com.wiley.sf.common.sql.SimpleDBConnect;


/**
 * Retrieve benchmark results from database and create graphs.
 * TODO: Take params for start/end date/time.
 *
 * @since   JDK 1.5
 * @version $Id: BenchGraph.java,v 1.7 2010-09-01 21:36:00 smarkoff Exp $
 * @author  Steve Markoff
 */
public class BenchGraph {

    public static void main(String [] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: <properties file>");
            System.exit(1);
        }

        BenchGraph graph = new BenchGraph(args[0]);
        graph.go();
    }


    private final SimpleDBConnect dbConnect;
    private final String environmentName;


    public BenchGraph(String propertiesFileName) throws Exception {
        dbConnect = SimpleDBConnect.create(propertiesFileName);  // throws 4 exceptions
        // get a test connection
        Connection con = dbConnect.getConnection();
        con.close();

        PropertiesUtil props = new PropertiesUtil(propertiesFileName);
        environmentName = props.getString("environment.name");
    }

    private void go() throws Exception {
        Connection con = dbConnect.getConnection();
        con.setAutoCommit(true);

        String sql = "select started, completed, time_ms, environment, type, score "
            + "from benchmark where environment = ? order by started";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, environmentName);
        ResultSet rs = ps.executeQuery();
        ArrayList<Benchmark> list = new ArrayList<Benchmark>();
        while (rs.next()) {
            Timestamp started = rs.getTimestamp(1);
            Timestamp completed = rs.getTimestamp(2);
            long timeMS = rs.getLong(3);
            String environment = rs.getString(4);
            String type = rs.getString(5);
            int score = rs.getInt(6);

            Date startedDate = new Date(started.getTime());
            Date completedDate = new Date(completed.getTime());

            list.add(new Benchmark(startedDate, completedDate, timeMS, environment, type, score));
        }
        ps.close();
        con.close();

        graph(list);
    }

    private void graph(ArrayList<Benchmark> list) throws IOException {
        HashSet<String> typeSet = new HashSet<String>();
        HashMap<String, TimeSeries> typeMap = new HashMap<String, TimeSeries>();

        for (Benchmark benchmark : list) {
            String type = benchmark.getType();
            if (!typeSet.contains(type)) {
                typeSet.add(type);
                typeMap.put(type, new TimeSeries(type));
            }

            TimeSeries ts = typeMap.get(type);
            Date completed = benchmark.getCompleted();
            Minute min = new Minute(completed);
            ts.add(min, benchmark.getScore());
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();

        for (String type : typeSet) {
            dataset.addSeries(typeMap.get(type));
        }

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Benchmarks over time - " + environmentName,    // chart title
            "Date/Time",                     // domain axis label
            "score",                        // range axis label
            dataset,                    // data
            true,                       // include legend
            true,                       // tooltips
            false);                     // urls

        /*
        chart.addSubtitle(new TextTitle("Number of Classes By Release"));
        TextTitle source = new TextTitle(
            "Source: Java In A Nutshell (5th Edition) "
            + "by David Flanagan (O'Reilly)");
        source.setFont(new Font("SansSerif", Font.PLAIN, 10));
        source.setPosition(RectangleEdge.BOTTOM);
        source.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        chart.addSubtitle(source);
        */

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(false);

        XYItemRenderer renderer = plot.getRenderer();
        if (renderer instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer rr = (XYLineAndShapeRenderer) renderer;
            rr.setBaseShapesVisible(true);
            rr.setBaseShapesFilled(true);
            renderer.setSeriesStroke(0, new BasicStroke(2.0f));
            renderer.setSeriesStroke(1, new BasicStroke(2.0f));
        }

        DateAxis axis = (DateAxis) plot.getDomainAxis();
        //axis.setDateFormatOverride(new SimpleDateFormat("hh:mm a"));
        axis.setDateFormatOverride(new SimpleDateFormat("MM.dd HH:mm"));

        File graphFile = new File("C:/temp/Permissions/benchmarks" + environmentName + ".png");
        ChartUtilities.saveChartAsPNG(graphFile, chart, 900, 600);  // throws IOException
        System.out.println("Saved graph to " + graphFile.getPath());
    }
}

class Benchmark {
    private final Date started;
    private final Date completed;
    private final long timeMS;
    private final String environment;
    private final String type;
    private final int score;

    public Benchmark(Date started, Date completed, long timeMS, String environment, String type, int score) {
        this.started = started;
        this.completed = completed;
        this.timeMS = timeMS;
        this.environment = environment;
        this.type = type;
        this.score =score;
    }

    public Date getStarted() { return started; }
    public Date getCompleted() { return completed; }
    public long getTimeMS() { return timeMS; }
    public String getEnvironment() { return environment; }
    public String getType() { return type; }
    public int getScore() { return score; }
}
