package jhi.diskUsage;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.text.DecimalFormat;

public class UserHistory {

  Database database;
  UserManager userManager;
  ArrayList<String> users;
  LocalDate tomorrow;
  LocalDate start;
  

  public UserHistory(UserManager userManager, Database database) {
    this.database = database;
    this.userManager = userManager;
    users = userManager.getCurrentUsers();
    tomorrow = LocalDate.now().plusDays(1);
    start = tomorrow.minusMonths(6);
  }

  public void write(String filepath) {
    
    

    for (String user : users) {
      String filename = user + ".txt";
      System.out.println("Writing " + filename);

      String output = "Date\tHome Size\tProjects Size\tScratch Size";      
      output += getHistory(user);

      try {
        File parentFile = new File(filepath);
        parentFile.mkdir();
        File file = new File(parentFile, filename);
        
        FileWriter out = new FileWriter(file);
        out.write(output);
        out.close();
      }
      catch (Exception e) {System.out.println(e.getMessage());}
    }
  }


  public String getHistory(String username) {
    String output = "";
    for(LocalDate date=start; date.isBefore(tomorrow); date=date.plusDays(1)) {
      String home = database.getHistory(date.toString(), username, "home");
      String projects = database.getHistory(date.toString(), username, "projects");
      String scratch = database.getHistory(date.toString(), username, "scratch");

      output += "\n";
      output += date.toString() + "\t";
      output += formatSize(home) + "\t";
      output += formatSize(projects) + "\t";
      output += formatSize(scratch);
    }
    return output;
  }

  public String formatSize(String value) {
    
    if(value.equals("?")) {
      return value;
    }

    long size = Long.parseLong(value);
    DecimalFormat df = new DecimalFormat("#.#");
    
    double displaySize = size;
    displaySize = displaySize/1024/1024/1024;

    String display = df.format(displaySize);
    return (display);
  }

  
}
