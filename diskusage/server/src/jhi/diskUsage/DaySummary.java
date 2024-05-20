package jhi.diskUsage;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.util.ArrayList;

public class DaySummary {
  Database database;
  UserManager userManager;
  ArrayList<String> users;
  

  public DaySummary(UserManager userManager, Database database) {
    this.database = database;
    this.userManager = userManager;
    users = userManager.getCurrentUsers();
  }

  public void writeDaySummary(String filepath) {
    String output = "User\tHome Size\tHome Dirs\tHome Files\tProjects Size\tProjects Dirs\tProjects Files\tScratch Size\tScratch Dirs\tScratch Files";
    String today = LocalDate.now().toString();
    users.add("system");

    for (String user : users) {
      output += "\n";
      output += user + "\t" 
        + database.getRecords(today, user, "home") + "\t" 
        + database.getRecords(today, user, "projects") + "\t"
        + database.getRecords(today, user, "scratch");
    }

    try {
      File file = new File(filepath);
      FileWriter out = new FileWriter(file);
      out.write(output);
      out.close();
    }
    catch (Exception e) {System.out.println(e.getMessage());}
  }
}
