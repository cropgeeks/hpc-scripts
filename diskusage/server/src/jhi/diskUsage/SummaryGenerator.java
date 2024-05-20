package jhi.diskUsage;

import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class SummaryGenerator {
  UserManager userManager;
  Database database;
  ArrayList<String> users;
  long home;
  long projects;
  long backedup;
  long scratch;

  public SummaryGenerator(UserManager userManager, Database database) {
    this.userManager = userManager;
    this.database = database;
    users = userManager.getCurrentUsers();
  }

  public void generateSummary(String summaryLocation) {
    String output = "Username\tHome\tProjects\tBacked Up\tScratch\n";
    for (String user : users) {
      getData(user);
      output += user + "\t" + formatSize(home) + "\t" + formatSize(projects) + "\t" + formatSize(backedup) + "\t" + formatSize(scratch) + "\n";
    }

    try {
      File file = new File(summaryLocation);
      FileWriter out = new FileWriter(file);
      out.write(output);
      out.close();
    }
    catch (Exception e) {System.out.println(e.getMessage());}
    
  }

  public void getData(String user) {
    home = database.getUserSize(user, "home");
    projects = database.getUserSize(user, "projects");
    backedup = home + projects;
    scratch = database.getUserSize(user, "scratch");
  }


  public String formatSize(long size)
		{
		DecimalFormat df = new DecimalFormat("#.#");
		int unitID = 0;
		String units = "";
		double displaySize = size;

		while (displaySize > 1024) {
			displaySize = displaySize / 1024;
			unitID++;
    }

		switch (unitID) {
			case 0:
				units = "B";
				break;
			case 1:
				units = "K";
				break;
			case 2:
				units = "M";
				break;
			case 3:
				units = "G";
				break;
			case 4:
				units = "T";
				break;
			case 5:
				units = "P";
				break;
    }

		String display = df.format(displaySize) + units;
		return (display);
  }
}
