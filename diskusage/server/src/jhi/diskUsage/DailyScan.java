package jhi.diskUsage;

import java.time.LocalDateTime;

public class DailyScan {

  //Runs with parameters {database location, ipa file, motd summary filename, folder to scan, json filename, all user size file location, user history folder}
  public static void main(String[] args) {
    System.out.println(LocalDateTime.now());
    String databasePath = args[0];
    String userFile = args[1];
    String summary = args[2];
    String fileToScan = args[3];
    String jsonLocation = args[4];
    String daySummaryLocation = args[5];
    String historyFolder = args[6];

    //Initialise database
    System.out.println("");
    System.out.println(LocalDateTime.now());
    System.out.println("Initialising db");
    Database database = new Database(databasePath);

    //Read ipa users file to create list of current users and add new ones to database
    System.out.println("");
    System.out.println(LocalDateTime.now());
    System.out.println("Reading users");
    UserManager userManager = new UserManager(database);
    userManager.readUsersFile(userFile);

    //Scan the desired folder, store scan results in database
    System.out.println("");
    System.out.println(LocalDateTime.now());
    System.out.println("Scanning");
    ScanManager scanManager = new ScanManager(fileToScan, userManager, database);
    scanManager.setFolderName();
    scanManager.scan();

    //Generate JSON files for website
    System.out.println("");
    System.out.println(LocalDateTime.now());
    JSONgenerator jsonGenerator = new JSONgenerator(scanManager.folderName, userManager, database, jsonLocation);
    jsonGenerator.writeJSON();


    //Code for producing jsons from correct database during testing phase
    /*System.out.println("");
    System.out.println(LocalDateTime.now());
    JSONgenerator jsonGenerator = new JSONgenerator("home", userManager, database, "home.json");
    jsonGenerator.writeJSON();

    System.out.println("");
    System.out.println(LocalDateTime.now());
    jsonGenerator = new JSONgenerator("projects", userManager, database, "projects.json");
    jsonGenerator.writeJSON();

    System.out.println("");
    System.out.println(LocalDateTime.now());
    jsonGenerator = new JSONgenerator("scratch", userManager, database, "scratch.json");
    jsonGenerator.writeJSON();*/

    //Create summary file of all users' file sizes today
    System.out.println("");
    System.out.println(LocalDateTime.now());
    System.out.println("Writing summary file");
    SummaryGenerator summaryGenerator = new SummaryGenerator(userManager, database);
    summaryGenerator.generateSummary(summary);

    //
    System.out.println("");
    System.out.println(LocalDateTime.now());
    System.out.println("Writing day summary");
    DaySummary daySummary = new DaySummary(userManager, database);
    daySummary.writeDaySummary(daySummaryLocation);

    System.out.println("");
    System.out.println(LocalDateTime.now());
    System.out.println("Writing user histories");
    UserHistory userHistory = new UserHistory(userManager, database);
    userHistory.write(historyFolder); 

    System.out.println("");
    System.out.println(LocalDateTime.now());
    System.out.println("Complete");
    System.exit(0);

  }
}
