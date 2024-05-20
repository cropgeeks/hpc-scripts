#!/mnt/cluster/apps/java/latest/bin/java --source 11

import java.io.*;
import java.time.*;
import java.text.*;
import java.util.*;

public class DiskUsage
{
	private DecimalFormat df0 = new DecimalFormat("###,###");
	private DecimalFormat df1 = new DecimalFormat("#,###,##0.0");
	private DecimalFormat df2 = new DecimalFormat("#,###,##0.00");

	private float bgfsSize, bgfsUsed, bgfsFree1, bgfsFree2, bgfsRatio;
	private String bgfsCompRatio;

	private Location[] system = new Location[3];
	private Location[] user = new Location[3];

	// These are counts of how many users per location have data stored there
	// Start at -1 so when they parse "system" it's the same as skipping it
	private int homeCount=-1, projectsCount=-1, scratchCount=-1;

	// args[0]=username args[1]=reportFile
	public static void main(String[] args)
		throws Exception
	{
		DiskUsage du = new DiskUsage();		
		
		du.readBeeGFSValues();

		du.readUserValues(args[0]);

		du.createReport(args[0], new File(args[1]));
	}

	private void readBeeGFSValues()
		throws IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(new File("/mnt/cluster/diskusage/motd/values")));

		bgfsSize = Float.parseFloat(in.readLine().split("\t")[1]);
		bgfsUsed = Float.parseFloat(in.readLine().split("\t")[1]);
		bgfsFree1 = Float.parseFloat(in.readLine().split("\t")[1]);
		bgfsFree2 = Float.parseFloat(in.readLine().split("\t")[1]);
		bgfsRatio = Float.parseFloat(in.readLine().split("\t")[1]);

		in.close();

		in = new BufferedReader(new FileReader(new File("/mnt/cluster/diskusage/motd/rincewind.ratio")));
		bgfsCompRatio = in.readLine();
		in.close();
	}

	private void readUserValues(String username)
		throws IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(new File("diskusage.txt")));

		// Read/skip the header line
		String str = in.readLine();

		while ((str = in.readLine()) != null)
		{
			Location[] data = new Location[3];

			if (str.isEmpty())
				continue;
			
			String[] tokens = str.split("\t");

			data[0] = new Location("home");
			data[0].size = Float.parseFloat(tokens[1]);
			data[0].folders = Long.parseLong(tokens[2]);
			data[0].files = Long.parseLong(tokens[3]);
			if (data[0].size > 0)
				homeCount++;

			data[1] = new Location("projects");
			data[1].size = Float.parseFloat(tokens[4]);
			data[1].folders = Long.parseLong(tokens[5]);
			data[1].files = Long.parseLong(tokens[6]);
			if (data[1].size > 0)
				projectsCount++;

			data[2] = new Location("scratch");
			data[2].size = Float.parseFloat(tokens[7]);
			data[2].folders = Long.parseLong(tokens[8]);
			data[2].files = Long.parseLong(tokens[9]);
			if (data[2].size > 0)
				scratchCount++;

			String name = tokens[0];
			if (name.equals("system"))
				system = data;
			else if (name.equals(username))
				user = data;
		}

		in.close();

		// Now, for each location, work out what their percentage increase/decrease
		// is versus an equal share of the storage (usage / number of users)
		for (Location location: user)
		{
			if (location.name.equals("home"))
			{
				float equalUse = system[0].size / homeCount;
				location.difference = location.size / equalUse; //((location.size - equalUse) / equalUse) * 100;
			}

			else if (location.name.equals("projects"))
			{
				float equalUse = system[1].size / projectsCount;
				location.difference = location.size / equalUse; //((location.size - equalUse) / equalUse) * 100;
			}

			else if (location.name.equals("scratch"))
			{
				float equalUse = system[2].size / scratchCount;
				location.difference = location.size / equalUse; //((location.size - equalUse) / equalUse) * 100;
			}
		}

		System.out.println("home users: " + homeCount + " / " + df0.format(system[0].size));
		System.out.println("proj users: " + projectsCount + " / " + df0.format(system[1].size));
		System.out.println("scra users: " + scratchCount + " / " + df0.format(system[2].size));
	}

	private void createReport(String username, File outFile)
		throws IOException
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile, true));

		out.write("<div class='container'>");
		out.write("<p><b>Your disk usage</b></p>");

		out.write("<p>Your tracked usage as of the last overnight scan is as follows:</p>");

		out.write("<div align='center' class='table-responsive'>");
		out.write("<table class='table table-striped text-center text-nowrap'>");
		out.write("<thead><tr>");
		out.write("<th>Location</th>");
		out.write("<th>Files</th>");
		out.write("<th>Folders</th>");
		out.write("<th>Size</th>");
		out.write("<th>Fair Share</th>");
		out.write("<th>1-year Cost</th>");
		out.write("</tr></thead>");
		out.write("<tbody>");

		for (Location location: user)
		{
			out.write("<div class='row'>");
			out.write("<td>" + location.name + "</td>");
			out.write("<td>" + df0.format(location.files) + "</td>");
			out.write("<td>" + df0.format(location.folders) + "</td>");
			out.write("<td>" + formatSize(location.size) + "</td>");
			out.write("<td>" + formatDifference(location.difference) + "</td>");
			out.write("<td>" + formatCost(location.size) + "</td>");
			out.write("</tr>");
		}

		out.write("</tbody>");
		out.write("</table>");
		out.write("</div>");

		out.write("<p class='pt-2'>Your Fair Share value shows how much more or less");
		out.write(" your usage is compared with an equal share of all usage if every");
		out.write(" user was allocated the same amount. Costs are estimated at ");
		out.write(" <span class='lightBlue'>£10</span> per TB per month.</p>");

		if (user[0].files > 1000000 || user[1].files > 1000000 || user[2].files > 1000000)
		{
			out.write("<p>Note that storing lots of small files is often more detrimental");
			out.write(" to file system/backup performance than holding fewer, larger files.");
			out.write(" You should consider zipping/tarring infrequently accessed data to");
			out.write(" help reduce file counts.</p>");
		}

		out.write("<p align='center'>");

		if (new File("tmp/home.png").exists() && new File("tmp/home.png").length() > 0)
			out.write("<img class='img-fluid' src='data:image/png;base64,HOME.PNG'/>");
		if (new File("tmp/projects.png").exists() && new File("tmp/projects.png").length() > 0)
			out.write("<img class='img-fluid' src='data:image/png;base64,PROJECTS.PNG'/>");
		if (new File("tmp/scratch.png").exists() && new File("tmp/scratch.png").length() > 0)
			out.write("<img class='img-fluid' src='data:image/png;base64,SCRATCH.PNG'/>");
		
		out.write("</p>");

		out.write("<p>Overall BeeGFS (networked storage) usage is <span class='red'>" + df1.format(bgfsUsed) + "T</span>");
		out.write(" out of <span class='lightBlue'>" + df1.format(bgfsSize) + "T</span>, with the potential");
		out.write(" to hold a further <span class='red'>" + df1.format(bgfsFree1) + "T</span>");
		out.write(" to <span class='red'>" + df1.format(bgfsFree2) + "T</span>");
		out.write(" depending on compression. The current average compression ratio is");
		out.write(" <span class='lightBlue'>" + bgfsCompRatio + "</span>, however this varies based");
		out.write(" on the files types being stored. The system automatically");
		out.write(" and <i>transparently</i> compresses all files for you.</p>");

		out.write("<p>Additional disk usage tracking is available at");
		out.write(" <a href='https://cropdiversity.ac.uk/intranet/diskusage'>https://cropdiversity.ac.uk/intranet/diskusage</a>.</p><hr>");

		out.write("</div>");
		out.close();
	}

	private String formatSize(float size)
	{
		if (size < 1024f)
			return df0.format(size) + "B";
		else if (size < 1024f*1024f)
			return df0.format(size/1024f) + "K";
		else if (size < 1024f*1024f*1024f)
			return df0.format(size/1024f/1024f) + "M";
		else
			return df0.format(size/1024f/1024f/1024f) + "G";
	}

	private String formatDifference(float difference)
	{
		if (difference == 0f)
			return df1.format(difference) + "x";

		else if (difference < 1)
			return "<span class='green'><b>&darr;</b> " + (df2.format(difference)) + "x</span>";
		
		else
			return "<span class='red'><b>&uarr;</b> " + df2.format(difference) + "x</span>";
	}

	private String formatCost(float size)
	{
		// Convert to TB
		size = size / 1024f / 1024f / 1024f / 1024f;

		return "£" + df0.format(size * 10 * 12);
	}

	private static class Location
	{
		String name;
		long files, folders;
		float size;
		float difference;

		Location(String name) {
			this.name = name;
		}
	}
}
