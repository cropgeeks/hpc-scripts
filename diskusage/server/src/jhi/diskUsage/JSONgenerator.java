package jhi.diskUsage;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class JSONgenerator {
  ArrayList<String> users;
  Database database;
  String folder;
  LocalDate today;
  int minusDays;
  HashMap<String, Long> ranges;
  String dirCol;
  String fileCol;
  String sizeCol;
  String jsonLocation;
  long dirDiff;
  long fileDiff;
  long sizeDiff;
  
  public JSONgenerator(String folder, UserManager userManager, Database database, String jsonLocation) {
    this.users = userManager.getCurrentUsers();
    this.database = database;
    this.folder = folder;
    this.jsonLocation = jsonLocation;
    today = LocalDate.now();
    minusDays = 7;
    database.setRanges();
    ranges = database.getRanges();
  }

  public void writeJSON(){
    String json = "[\n";
    ArrayList<String> sortedUsers = sortBySize();
    int userSize = sortedUsers.size();
    int userCounter = 0;

    for (String user : sortedUsers) {
      userCounter++;
      HashMap<String, UserStat> records = database.getUserRecords(today.plusDays(1), minusDays, user, folder);
      ArrayList<String> sortedDates = new ArrayList<String>(records.keySet());
 
      Collections.sort(sortedDates);
            
      System.out.println("Writing user " + user);

      int recordSize = records.size();
      int recordCounter = 0;

      json += "\t{\n";
      json += "\t\t\"folder\": \"" + folder + "\",\n";
      json += "\t\t\"username\": \"" + user + "\",\n";
      json += "\t\t\"jsonUsage\": [\n";


      for (String date : sortedDates) {
        recordCounter++;
        calcDiffs(sortedDates, records);

        UserStat record = records.get(date);
        setColours(folder, record);

        json += "\t\t\t{\n";
        json += "\t\t\t\t\"recorded\": \"" + date + "\",\n";
        json += "\t\t\t\t\"directories\": " + record.dirs + ",\n";
        json += "\t\t\t\t\"dirCol\": \"" + dirCol + "\",\n";
        json += "\t\t\t\t\"files\": " + record.files + ",\n";
        json += "\t\t\t\t\"fileCol\": \"" + fileCol + "\",\n";
        json += "\t\t\t\t\"size\": " + record.bytes + ",\n";
        json += "\t\t\t\t\"sizeCol\": \"" + sizeCol + "\"\n";
        
        if(recordCounter < recordSize) {
          json += "\t\t\t},\n";
        }
        else {
          json += "\t\t\t}\n";
        }
      }
      json += "\t\t],\n";

      json += "\t\t\"sizeDifference\": " + sizeDiff + ",\n";
      json += "\t\t\"dirsDifference\": " + dirDiff + ",\n";
      json += "\t\t\"filesDifference\": " + fileDiff +"\n";

      if(userCounter < userSize) {
        json += "\t},\n";
      }
      else {
        json += "\t}\n";
      }    
    }
    json += "]";
    //System.out.println(json);
    
    try {
      File file = new File(jsonLocation);
      FileWriter out = new FileWriter(file);
      out.write(json);
      out.close();
    }
    catch (Exception e) {System.out.println(e.getMessage());}
    
  }

  public void calcDiffs(ArrayList<String> dates, HashMap<String, UserStat> records) {
    int end = dates.size()-1;
    
    if(dates.size()<=1) {
      dirDiff = records.get(dates.get(end)).dirs.longValue();
      fileDiff = records.get(dates.get(end)).files.longValue();
      sizeDiff = records.get(dates.get(end)).bytes.longValue();
    }
    else {

      dirDiff = records.get(dates.get(end)).dirs.longValue() - records.get(dates.get(end-1)).dirs.longValue();
      fileDiff = records.get(dates.get(end)).files.longValue() - records.get(dates.get(end-1)).files.longValue();
      sizeDiff = records.get(dates.get(end)).bytes.longValue() - records.get(dates.get(end-1)).bytes.longValue();
    }
  }

  public ArrayList<String> sortBySize() {

    ArrayList<String> sortedUsers = new ArrayList<String>();
    HashMap<String, Long> sizes = database.getSizes(LocalDate.now().toString(), folder);
    ArrayList<Long> list = new ArrayList<Long>();

    for (Map.Entry<String, Long> entry : sizes.entrySet()) {
      list.add(entry.getValue());
    }
    //removes duplicate values and stops users appearing multiple times
    list = new ArrayList<>(new HashSet<>(list));

    Collections.sort(list);
    Collections.reverse(list);

    sortedUsers.add("system");

    for (Long num : list) {
      for (Map.Entry<String, Long> entry : sizes.entrySet()) {
        if (entry.getValue().equals(num)) {
          //checks user is in the list of current users rather than being historic user
          if(users.contains(entry.getKey())) {
            sortedUsers.add(entry.getKey());
          }
        }
      }
    }
    return sortedUsers;
  }

  public void setColours(String folder, UserStat record) {

    //System should display as having the same background colour as the table in vue
    if (record.name == "system") {
      dirCol = "background-color: rgb(252, 247, 245);";
      fileCol = "background-color: rgb(252, 247, 245);";
      sizeCol = "background-color: rgb(252, 247, 245);";
    }

    else {
      switch(folder){
        case "home": {
          dirCol = calcColour(ranges.get("minHomeDirs"), ranges.get("maxHomeDirs"), record.dirs.longValue());
          fileCol = calcColour(ranges.get("minHomeFiles"), ranges.get("maxHomeFiles"), record.files.longValue());
          sizeCol = calcColour(ranges.get("minHomeSize"), ranges.get("maxHomeSize"), record.bytes.longValue());
          break;
        }
        case "projects": {
          dirCol = calcColour(ranges.get("minProjectsDirs"), ranges.get("maxProjectsDirs"), record.dirs.longValue());
          fileCol = calcColour(ranges.get("minProjectsFiles"), ranges.get("maxProjectsFiles"), record.files.longValue());
          sizeCol = calcColour(ranges.get("minProjectsSize"), ranges.get("maxProjectsSize"), record.bytes.longValue());
          break;
        }
        case "scratch": {
          dirCol = calcColour(ranges.get("minScratchDirs"), ranges.get("maxScratchDirs"), record.dirs.longValue());
          fileCol = calcColour(ranges.get("minScratchFiles"), ranges.get("maxScratchFiles"), record.files.longValue());
          sizeCol = calcColour(ranges.get("minScratchSize"), ranges.get("maxScratchSize"), record.bytes.longValue());
          break;
        }
      }
    }
  }

  public String calcColour(Long min, Long max, Long value) {
    double normal = ((double)value - (double)min) / ((double)max - (double)min);
    long[] col1 = new long[] {250, 238, 232};
    long[] col2 = new long[] {229, 80, 57};

    double f1 = 1-normal;
    double f2 = normal;

    long red = (long) (f1 * col1[0] + f2 * col2[0]);
    long green = (long) (f1 * col1[1] + f2 * col2[1]);
    long blue = (long) (f1 * col1[2] + f2 * col2[2]);
    return ("background-color: rgb(" + red + "," + green + "," + blue + ");");
  }
}