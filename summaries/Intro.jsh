#!/mnt/cluster/apps/java/latest/bin/java --source 11

import java.io.*;
import java.time.*;
import java.text.*;
import java.util.*;

public class Intro
{
	public static void main(String[] args)
		throws Exception
	{
		Intro intro = new Intro();
		
		// args[0]=username args[1]=reportFile
		intro.createReport(args[0], new File(args[1]));
	}

	private void createReport(String username, File outFile)
		throws IOException
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile, true));

		out.write("<div class='container'>");
		out.write("<p><b>Crop Diversity HPC Usage Report</b></p>");

		out.write("<p>Hi <b>" + username + "</b>,</p>");
		out.write("<p>This report (generated " + new java.util.Date() + ") ");
		out.write("provides a summary of your cluster usage over the last seven days. ");
		out.write("Note, this reporting is still a work in progress and there may be errors listed - if you spot anything unusual, ");
		out.write("please report it using <a href='https://cropdiversity-hpc.slack.com'>Slack</a>.</p>");

		// Last login time?
		long time = new File("/mnt/cluster/last/" + username).lastModified();
		out.write("<p>You last logged in on " + new java.util.Date(time) + ".</p>");
		out.write("<hr>");
		out.write("</div>");

		out.close();
	}
}
