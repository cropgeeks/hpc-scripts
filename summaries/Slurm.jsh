#!/mnt/cluster/apps/java/latest/bin/java --source 11

import java.io.*;
import java.time.*;
import java.text.*;
import java.util.*;

public class Slurm
{
	private DecimalFormat df0 = new DecimalFormat("###,###");
	private DecimalFormat df1 = new DecimalFormat("0.0");
	private DecimalFormat df2 = new DecimalFormat("###,##0.00");

	private ArrayList<TableEntry> table = new ArrayList<>();
	
	private long totalRunTime;
	private boolean hasCpuEff, hasMemEff;
	private float avgCpuEff, avgMemEff;
	
	public static void main(String[] args)
		throws Exception
	{
		Slurm slurm = new Slurm();
		
		slurm.parseReport(new File(args[0]));
		slurm.calcMetrics();

		BufferedWriter out = new BufferedWriter(new FileWriter(new File(args[1]), true));
		slurm.printReport(out);
		out.close();

		slurm.printGraphs(new File(args[2]));
	}

	private void parseReport(File reportFile)
		throws IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(reportFile));

		String str = null;
		while ((str = in.readLine()) != null)
		{
			if (str.isBlank() || str.startsWith("JobID"))
				continue;
			
			TableEntry te = new TableEntry();
			String[] tokens = str.trim().split("\\|");
			
			// Parse the columns
			te.jobID = tokens[0];
			te.partition = tokens[1];
			te.jobName = tokens[2];
			if (!tokens[3].equals("---"))
				te.nCPUs = Integer.parseInt(tokens[3]);
			if (!tokens[4].equals("---"))
				te.cpuTimeRAW = Long.parseLong(tokens[4]);
			te.elapsed = tokens[5];
			if (!tokens[6].equals("---"))
				te.reqMem = Float.parseFloat(tokens[6].replaceAll("G", ""));
			te.state = tokens[7];			
			if (!tokens[8].equals("---"))
				te.cpuEff = Float.parseFloat(tokens[8]);
			if (!tokens[9].equals("---"))
				te.memEff = Float.parseFloat(tokens[9]);
			if (!tokens[10].equals("---") && !tokens[10].isBlank())
				te.maxRSS = Float.parseFloat(tokens[10].replaceAll("G", ""));
			te.nodeList = tokens[11];

			table.add(te);
		}

		in.close();
	}
	
	private void calcMetrics()
	{
		// How many entries were used to calculate the averages
		int cpuCount = 0, memCount = 0;
		
		for (TableEntry te: table)
		{
			if (te.cpuTimeRAW != null)
				totalRunTime += te.cpuTimeRAW;
			
			// Do we have stats on CPU efficiency?
			if (te.cpuEff != null)
			{
				cpuCount++;
				avgCpuEff += te.cpuEff;
				hasCpuEff = true;
			}
			
			// Do we have stats on mem efficiency?
			if (te.memEff != null)
			{
				memCount++;
				avgMemEff += te.memEff;
				hasMemEff = true;
			}
		}
		
		// Final stats
		if (hasCpuEff)
			avgCpuEff = avgCpuEff / cpuCount;
		if (hasMemEff)
			avgMemEff = avgMemEff / memCount;
	}
	
	private void printReport(BufferedWriter out)
		throws Exception
	{
		out.write("<div class='container'>");
		out.write("<p><b>Your slurm jobs</b></p>");

		if (table.size() == 0)
		{
			out.write("<p>You've not submitted any jobs to the cluster during the last week.");
			out.newLine();
			return;
		}

		out.write("<div class='alert alert-primary' role='alert'>");
		out.write("You've had " + df0.format(table.size()) + (table.size() == 1 ? " job" : " jobs") + " active over the last");
		out.write(" week, with a cumulative CPU-time of <b>" + formatTime(totalRunTime) + "</b>.");
		out.write("</div>");

		if (totalRunTime > 0)
		{
			float cost = (totalRunTime / 60f / 60f) * 0.02f;

			out.write("<p>If we assume <span class='lightBlue'>£0.02</span> per CPU-hour, this would've cost");
			out.write(" <span class='lightBlue'>£" + df2.format(cost) + "</span>.");
			out.newLine();

			if (hasCpuEff)
			{
				float wasted = cost * ((100-avgCpuEff)/100f);

				out.write(" Your average CPU efficiency was " + formatCPU(avgCpuEff) + ", ");
				out.write("meaning <span class='red'>£" + df2.format(wasted) + "</span> was potentially wasted");
				out.write(" due to CPUs allocated but not utilised. You should try to target 100% CPU");
				out.write(" efficiency, however jobs that perform a lot of I/O, can't utilise multiple");
				out.write(" cores all of the time, or primarily use GPUs, will reduce this.");
				out.newLine();
			}

			out.write("</p>");

			if (hasMemEff)
			{
				out.write("Memory efficiency was " + formatMem(avgMemEff) + ". This is difficult to quantify,");
				out.write(" but every GB you allocate and don't use is a GB that no-one");
				out.write(" else can use either. Over-allocating quickly leads to additional nodes");
				out.write(" powering on to meet demand, wasting energy because nodes are now under-utilised, however");
				out.write(" this must be balanced against losing time due to jobs running out of memory. An ideal");
				out.write(" memory efficiency target is 90%.</p>");
				out.newLine();
			}

			out.write("<p align='center'>");
			if (hasCpuEff)
				out.write("<img class='img-fluid' src='data:image/png;base64,CPU.PNG'/>");
			if (hasMemEff)
				out.write("<img class='img-fluid' src='data:image/png;base64,MEM.PNG'/>");
			out.write("</p>");
			out.newLine();
		}

		out.write("<p>You can view live efficiency metrics for the cluster as a whole at ");
		out.write("<a href='https://www.cropdiversity.ac.uk/top'>https://cropdiversity.ac.uk/top</a>.</p>");
		
		out.write("<div align='center' class='table-responsive'>");
		out.write("<table class='table table-striped text-center text-nowrap'>");
		out.write("<thead><tr>");
		out.write("<th>JobID</th>");
		out.write("<th>Name</th>");
		out.write("<th>Q</th>");
		out.write("<th>NCPUS</th>");
		out.write("<th>Elapsed</th>");
		out.write("<th>ReqMem</th>");
		out.write("<th>State</th>");
		out.write("<th>CPUEff</th>");
		out.write("<th>MemEff</th>");
		out.write("<th>MaxRSS</th>");
		out.write("<th>NodeList</th>");
		out.write("</tr></thead>");
		out.write("<tbody>");
		out.newLine();

		for (TableEntry te: table)
		{
			out.write("<tr>");
			out.write("<td>" + te.jobID + "</td>");
			out.write("<td>" + truncate(te.jobName, 10) + "</td>");
			out.write("<td>" + te.partition.substring(0,2) + "</td>");
			if (te.nCPUs != null)
				out.write("<td>" + te.nCPUs + "</td>");
			else
				out.write("<td>---</td>");
			out.write("<td>" + te.elapsed + "</td>");
			if (te.reqMem != null)
				out.write("<td>" + te.reqMem + "G</td>");
			else
				out.write("<td>---</td>");
			out.write("<td>" + formatState(te.state) + "</td>");
			if (te.cpuEff != null)
				out.write("<td>" + formatCPU(te.cpuEff) + "</td>");
			else
				out.write("<td>---</td>");
			if (te.memEff != null)
				out.write("<td>" + formatMem(te.memEff) + "</td>");
			else
				out.write("<td>---</td>");
			if (te.maxRSS != null)
				out.write("<td>" + te.maxRSS + "G</td>");
			else
				out.write("<td>---</td>");
			out.write("<td>" + shortenNodeName(te.nodeList) + "</td>");
			out.write("</tr>");
			out.newLine();
		}

		out.write("</tbody>");
		out.write("</table>");
		out.write("</div>");

		out.write("</div>");
		out.newLine();
	}

	private void printGraphs(File outFile)
		throws IOException
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
//		out.write("job\tcpu\tmem");
//		out.newLine();

		int count = 1;
		for (TableEntry te: table)
		{
			out.write("" + (count++));

			out.write("\t");
			if (te.cpuEff != null)
				out.write("" + te.cpuEff);
			else
				out.write("?");
			out.write("\t");
			if (te.memEff != null)
				out.write("" + te.memEff);
			else
				out.write("?");
			
			out.newLine();
		}

		out.close();
	}

	private String formatCPU(Float cpu)
	{
		if (cpu != null)
		{
			if (cpu < 20f)
				return "<span class='red'>" + df1.format(cpu) + "%</span>";
			else if (cpu < 80f)
				return df1.format(cpu) + "%";
			else
				return "<span class='green'>" + df1.format(cpu) + "%</span>";
		}

		return "---";
	}

	private String formatMem(Float mem)
	{
		if (mem != null)
		{
			if (mem < 20f || mem >= 90f)
				return "<span class='red'>" + df1.format(mem) + "%</span>";
			else if (mem < 60f)
				return df1.format(mem) + "%";
			else
				return "<span class='green'>" + df1.format(mem) + "%</span>";
		}

		return "---";
	}

	private String formatState(String state)
	{
		switch (state)
		{
			case "TIMEOUT":
			case "FAILED":
			case "OUT_OF_MEMORY":
				return "<span class='red'>" + state + "</span>";
			case "RUNNING": return "<span class='cyan'>" + state + "</span>";
			case "CANCELLED": return "<span class='yellow'>" + state + "</span>";
			case "COMPLETED": return "<span class='green'>" + state + "</span>";
			case "PENDING": return "<span class='blue'>" + state + "</span>";

			default: return state;
		}
	}

	private String truncate(String str, int limit)
	{
		if (str.length() > limit)
			return str.substring(0, limit);
		
		return str;
	}

	private String shortenNodeName(String name)
	{
		int index = name.lastIndexOf("-");
		if (index != -1)
			return name.substring(index+1);
		
		return name;
	}

	public String formatTime(long time)
	{
		Duration dur = Duration.ofSeconds(time);
	
		long days = dur.toDays();
		dur = dur.minusDays(days);
		long hours = dur.toHours();
		dur = dur.minusHours(hours);
		long minutes = dur.toMinutes();
		dur = dur.minusMinutes(minutes);
		long seconds = dur.getSeconds();

		String str = "";
		if (days > 0)
			str += df0.format(days) + (days==1?" day":" days") + ", ";
		if (days > 0 || hours > 0)
			str += hours + (hours==1?" hour":" hours") + ", ";
		if (hours > 0 || minutes > 0)
			str += minutes + (minutes==1?" minute":" minutes") + ", ";
		str += seconds + (seconds==1?" second":" seconds");

		return str;
	}
	
	private static class TableEntry
	{
		String jobID, partition, jobName, state, nodeList, elapsed;
		Integer nCPUs;
		Long cpuTimeRAW;
		Float reqMem, maxRSS, cpuEff, memEff;
	}
}
