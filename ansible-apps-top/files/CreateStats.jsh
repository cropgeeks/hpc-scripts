#!/mnt/cluster/apps/java/latest/bin/java --source 11

import java.io.*;
import java.text.*;
import java.util.*;

public class CreateStats
{
	// 2 minutes 'ago'
	private static long MINS2 = System.currentTimeMillis() - 120000;
	// When are we tracking green on/off times from?
	private long greenStart = System.currentTimeMillis();

	// Holds summary stats for the entire cluster
	private Node summary = new Node("gruffalo", 8, 16, 0);

	// HPC nodes information
	private LinkedHashMap<String,Node> nodes = new LinkedHashMap<>();
	private Node nodesSummary = new Node();

	// Storage nodes information
	private LinkedHashMap<String,Node> beegfs = new LinkedHashMap<>();
	private Node beegfsSummary = new Node();

	public static void main(String[] args)
		throws Exception
	{
		new CreateStats().run();
	}

	private void run()
		throws Exception
	{
		// Load the basic node information into memory
		loadNodes();
		loadTimes();

		// Then for each node, get its individual stats
		for (Node node: nodes.values())
			getStats(node);
		for (Node node: beegfs.values())
			getStats(node);

		// And calculate summary info for all nodes
		getStats(summary);
		calculateSummary(nodesSummary, nodes.values());
		calculateSummary(beegfsSummary, beegfs.values());

		// Update the times.txt on/off tracking file
		printTimes();

		// Write everything out into a JSON file
		printJSON();

		// Finally print values to be captured for RRD Storage
		printRRD();
	}

	private void loadNodes()
		throws IOException
	{
		BufferedReader in = new BufferedReader(new FileReader("nodes.txt"));

		String str = null;
		while ((str = in.readLine()) != null)
		{
			if (str.isBlank() || str.startsWith("NODENAME"))
				continue;

			String[] tokens = str.split("\t");
			String name = tokens[0];
			int cpus = Integer.parseInt(tokens[1]);
			int memTotal = Integer.parseInt(tokens[2]);
			float idleWatts = Float.parseFloat(tokens[5]);

			Node n = new Node(name, cpus, memTotal, idleWatts);

//			System.out.println("Added " + name);
			if (tokens[3].equals("hpc"))
				nodes.put(name, n);
			else if (tokens[3].equals("beegfs"))
				beegfs.put(name, n);
		}

		in.close();
	}

	private void loadTimes()
	{
		try
		{
			BufferedReader in = new BufferedReader(new FileReader("times.txt"));

			// First line will be green start time
			String str = in.readLine();
			greenStart = Long.parseLong(str);

			while ((str = in.readLine()) != null)
			{
				if (str.isBlank())
					continue;
				
				String[] tokens = str.split("\t");
				String name = tokens[0];

				// Search the hashs to find this node
				Node node = nodes.get(name);
				if (node == null)
					node = beegfs.get(name);
				if (node == null)
					continue;
				
				// Then set its on/off time values so far
				node.minsOn += Integer.parseInt(tokens[1]);
				node.minsOff += Integer.parseInt(tokens[2]);
			}

			in.close();
		}
		catch (Exception e) { e.printStackTrace(); }
	}

	private void getStats(Node node)
	{
		try
		{
			File file = new File("stats", node.name);
			if (file.exists() == false || file.lastModified() < MINS2)
			{
				node.minsOff++;
				System.out.println(node.name + "\tstats not found or too old");
				return;
			}

			BufferedReader in = new BufferedReader(new FileReader(file));
			String str = null;

			while ((str = in.readLine()) != null && !str.isBlank())
			{
				String value = str.substring(str.indexOf("=")+1);

				if (str.startsWith("USAGE="))
					node.usage = Float.parseFloat(value);
				else if (str.startsWith("UPTIME="))
					node.uptime = value;
				else if (str.startsWith("USERS="))
					node.users = Integer.parseInt(value);
//				else if (str.startsWith("MEMTOTAL"))
//					node.memTotal = Long.parseLong(value);
				else if (str.startsWith("MEMUSED"))
					node.memUsed = Long.parseLong(value);
				else if (str.startsWith("MEMALLOC") && !value.isBlank())
					node.memAlloc = 1024L * Long.parseLong(value);
				else if (str.startsWith("WATTS"))
				{
					// Most nodes will have this, but apollo doesn't for some reason
					if (value.contains("Watts"))
						value = value.substring(0, value.indexOf("W"));
					try { node.watts = Integer.parseInt(value); }
                    catch (Exception we) {}
				}
				else if (str.startsWith("ALLOC") && !value.isBlank())
				{
					String alloc = value.substring(0, value.indexOf("/"));
					node.alloc = Integer.parseInt(alloc);
				}
				else if (str.startsWith("JOBCOUNT="))
					node.jobCount = Integer.parseInt(value);
				else if (str.startsWith("JOBUSERS="))
					node.jobUsers = Integer.parseInt(value);
				else if (str.startsWith("DISKTOTAL"))
					node.diskTotal = Long.parseLong(value);
				else if (str.startsWith("DISKUSED"))
					node.diskUsed = Long.parseLong(value);	
			}

			node.online = true;
			node.minsOn++;
			System.out.println(node.name + "\t" + node.usage + "\t" + node.alloc);

			in.close();
		}
		catch (Exception e)
		{
			System.out.println(node.name + " exception:");
			e.printStackTrace();
		}
	}

	private void calculateSummary(Node summary, Collection<Node> nodes)
	{
		for (Node node: nodes)
		{
			summary.cpus += node.cpus;
			summary.alloc += node.alloc;
			summary.usage += node.usage;
			summary.memTotal += node.memTotal;
			summary.memUsed += node.memUsed;
			summary.memAlloc += node.memAlloc;
			summary.watts += node.watts;
			summary.diskTotal += node.diskTotal;
			summary.diskUsed += node.diskUsed;
			
			summary.idleWatts += (node.minsOff * node.idleWatts);
			summary.minsOff += node.minsOff;
		}

		summary.name = "";
		summary.online = true;
		summary.usage = summary.usage / nodes.size();
	}

	private void printJSON()
		throws IOException
	{
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("stats.json")));

		out.println("{");

		// Headnode info
		out.println("  \"system\": {");
		out.println("    \"usage\": " + summary.usage + ",");
		out.println("    \"jobCount\": " + summary.jobCount + ",");
		out.println("    \"uptime\": \"" + summary.uptime + "\",");
		out.println("    \"users\": " + summary.users + ",");
		out.println("    \"jobUsers\": " + summary.jobUsers + ",");
		out.println("    \"greenStart\": " + greenStart + ",");
		out.println("    \"watts\": " + (nodesSummary.watts + beegfsSummary.watts));
		out.println("  },");

		out.println("  \"nodesSummary\": {");
		printNode(out, nodesSummary, "  ", true);

		// Loop over the nodes
		int count = 0;
		out.println("  \"nodes\":[");
		for (Node node: nodes.values())
		{
			count++;

			boolean comma = (count < nodes.size());
			out.println("    {");
			printNode(out, node, "    ", comma);
		}
		out.println("  ],");

		out.println("  \"beegfsSummary\": {");
		printNode(out, beegfsSummary, "  ", true);

		// Loop over the beegfs nodes
		count = 0;
		out.println("  \"beegfs\":[");
		for (Node node: beegfs.values())
		{
			count++;

			boolean comma = (count < beegfs.size());
			out.println("    {");
			printNode(out, node, "    ", comma);
		}
		out.println("  ]");
		out.println("}");

		out.close();
	}

	private void printNode(PrintWriter out, Node node, String space, boolean comma)
		throws IOException
	{
		out.println(space + "  \"name\": \"" + node.name + "\",");
		out.println(space + "  \"online\": " + node.online + ",");
		out.println(space + "  \"cpus\": " + node.cpus + ",");
		out.println(space + "  \"alloc\": " + node.alloc + ",");
		out.println(space + "  \"usage\": " + node.usage + ",");
		out.println(space + "  \"memTotal\": " + node.memTotal + ",");
		out.println(space + "  \"memUsed\": " + node.memUsed + ",");
		out.println(space + "  \"memAlloc\": " + node.memAlloc + ",");
		out.println(space + "  \"watts\": " + node.watts + ",");
		out.println(space + "  \"diskTotal\": " + node.diskTotal + ",");
		out.println(space + "  \"diskUsed\": " + node.diskUsed + ",");
		out.println(space + "  \"minsOff\": " + node.minsOff + ",");
		out.println(space + "  \"idleWatts\": " + node.idleWatts);

		// Last element needs a comma
		if (comma)
			out.println(space + "},");
		else
			out.println(space + "}");
	}

	// Produces a simple txt file that can be grepped by bash and its values
	// fed into the rrdupdate program
	private void printRRD()
		throws IOException
	{
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("rrds/stats.txt")));

		NumberFormat nf = NumberFormat.getInstance();
		out.println("rrd_watts " + (nodesSummary.watts + beegfsSummary.watts));
		out.println("rrd_hpc_watts " + nodesSummary.watts);
		out.println("rrd_beegfs_watts " + beegfsSummary.watts);
		out.println("rrd_usage " + nf.format(nodesSummary.usage));
		out.println("rrd_alloc " + nf.format(nodesSummary.alloc / (float) nodesSummary.cpus * 100));
		if (nodesSummary.memTotal == 0)
			out.println("rrd_mem U");
		else
			out.println("rrd_mem " + nf.format(nodesSummary.memUsed / (double) nodesSummary.memTotal * 100));
		if (beegfsSummary.diskTotal == 0)
			out.println("rrd_disk U");
		else
			out.println("rrd_disk " + nf.format(beegfsSummary.diskUsed / (double) beegfsSummary.diskTotal * 100));

		out.close();
	}

	// Writes a tab-delimited file: NODENAME MINS_ON MINS_OFF
	private void printTimes()
		throws IOException
	{
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("times.txt")));

		out.println(greenStart);

		for (Node node: nodes.values())
			out.println(node.name + "\t" + node.minsOn + "\t" + node.minsOff);
		for (Node node: beegfs.values())
			out.println(node.name + "\t" + node.minsOn + "\t" + node.minsOff);

		out.close();
	}

	private static class Node
	{
		String name, uptime;
		boolean online;
		int cpus, alloc, watts, users, jobCount, jobUsers, minsOn, minsOff;
		float usage, idleWatts;
		// mem values in KB; disk values in bytes
		long memTotal, memUsed, memAlloc, diskTotal, diskUsed;

		Node() {}

		Node(String name, int cpus, int memTotalGB, float idleWatts)
		{
			this.name = name;
			this.cpus = cpus;
			this.memTotal = memTotalGB * 1048576L;
			this.idleWatts = idleWatts;
		}
	}
}