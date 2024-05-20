package jhi.diskUsage;

import java.time.LocalDate;
import java.util.List;

public class ScanManager {
  DiskUsage usage;
  List<UserStat> info;
  String folderName = "";
  UserManager userManager;
  Database database;
  String filePath = "";
  
  public ScanManager(String filePath, UserManager userManager, Database database) {
    this.userManager = userManager;
    this.database = database;
    this.filePath = filePath;
  }

  public void setFolderName() {
		if(filePath.contains("\\")) {
			folderName = filePath.substring(filePath.lastIndexOf("\\")+1);
    }
		else if(filePath.contains("/")) {
			folderName = filePath.substring(filePath.lastIndexOf("/") + 1);
    }
  }

  public void scan() {
    try {
      usage = new DiskUsage(filePath);
      info = usage.run();
      for (UserStat user : info) {
        System.out.println("adding " + user.name + "'s records to db");
        database.checkUser(user.name, user.institute, user.fullname, null);
        
        String today = LocalDate.now().toString();
        if(!database.addUsage(today, user.name, folderName, user.dirs.longValue(), user.files.longValue(), user.bytes.longValue())) {
          database.updateUsage(today, user.name, folderName, user.dirs.longValue(), user.files.longValue(), user.bytes.longValue());
        }
      }
    }
    catch (Exception e) {System.out.println(e.getMessage());}
  } 
}
