package jhi.diskUsage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Scanner;

public class UserManager {
  File userList;
  ArrayList<String> currentUsers = new ArrayList<String>();
  ArrayList<String> allUsers;
  Database database;


  public UserManager(Database database) {
    this.database = database;
    this.allUsers = database.getUsers();    
  }

  public void readUsersFile(String filepath) {
    userList = new File(filepath);
    
    try (BufferedReader read = new BufferedReader(new FileReader(userList))) {
      String username = "";
      String fullname = "";
      String uid = "";
      String institute = "";

      String str = read.readLine();

      while (str != null) {        
        if (str.trim().startsWith("User login:")) {
          username = (str.substring(str.indexOf(":") + 2));
        }

        if (str.trim().startsWith("Display name:")) {
          fullname = str.substring(str.indexOf(":") + 2);
        }

        if (str.trim().startsWith("UID:")) {
          uid = str.substring(str.indexOf(":") + 2);
        }

        if (str.trim().startsWith("Department Number:")) {
          institute = str.substring(str.indexOf(":") + 2);
        }
        
        //when user info is obtained from file, checks if in db and adds them to current user list
        if (!username.isEmpty() && !fullname.isEmpty() && !institute.isEmpty() && !uid.isEmpty()) {
          database.checkUser(username, fullname, institute, uid);
          currentUsers.add(username);

          username = "";
          fullname = "";
          uid = "";
          institute = "";
        }
        str = read.readLine();
      }
    }
    catch(Exception e) {
      System.out.println(e.getMessage());
    }
  }

  public ArrayList<String> getCurrentUsers() { return currentUsers; }
}