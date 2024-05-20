package jhi.diskUsage;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

public class Database {
  private String path;
  private ArrayList<String> folderNames = new ArrayList<String>();
  private ArrayList<String> institutes = new ArrayList<String>();
  //private Hashtable<String, Folder> folders = new Hashtable<String, Folder>();
  private ArrayList<String> users = new ArrayList<String>();
  private HashMap<String, Long> ranges = new HashMap<String, Long>();

  //Initialises database
  public Database(String location) {
    path = "jdbc:sqlite:" + location;
    initDB();
    setUsers();
  }

  private Connection connect() {
    Connection conn = null;
    try {
      Class.forName("org.sqlite.JDBC");
      conn = DriverManager.getConnection(path);
    }

    catch (Exception e) {
      System.out.println(e.getMessage());
    }

    return conn;
  }

  private void closeConn(Connection conn) {
		try {
			if (conn != null) {
        conn.close();
      }
    }

		catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }

  private void initDB() {
    initUsers();
    initUsage();
  }

  private void initUsers() {
    String sql = "CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, fullname TEXT, institute TEXT, uid INTEGER);";
    Connection conn = connect();
    
    try(PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.execute();
    }

    catch(Exception e) {
      System.out.println(e.getMessage());
    }

    finally {
      closeConn(conn);
    }
  }

  private void initUsage() {
    String sql = "CREATE TABLE IF NOT EXISTS usage (recordedDate TEXT, username TEXT, folder TEXT, directories INTEGER, files INTEGER, size INTEGER, PRIMARY KEY (recordedDate, username, folder), FOREIGN KEY (username) REFERENCES users(username));";
    Connection conn = connect();
        
    try(PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.execute();
    }

    catch(Exception e) {
      System.out.println(e.getMessage());
    }

    finally {
      closeConn(conn);
    }
  }

  public void addUser(String username, String fullname, String institute, String uid) {
    String sql = "INSERT OR IGNORE INTO users VALUES (?, ?, ?, ?)";
    Connection conn = connect();

    try(PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, username);
      stmt.setString(2, fullname);
      stmt.setString(3, institute);
      stmt.setString(4, uid);

      stmt.execute();
    }

    catch(Exception e) {
      System.out.println(e.getMessage());
    }

    finally{
      closeConn(conn);
    }

    if(!users.contains(username)) {
      users.add(username);
    }
  }
  
  public String checkUser(String username, String institute, String fullname, String uid) {
    Connection conn = connect();
    String sql = "";
    ResultSet rs;

    //if username is numeric (only uid was reported)
    try {
      Integer.parseInt(username);
      uid = username;

      //check if user exists in db already
      sql = "SELECT username FROM users WHERE uid=?";

      try(PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setInt(1, Integer.parseInt(uid));
        rs = stmt.executeQuery();

        //if found, get their username
        if(rs.next()) {
          username = rs.getString("username");
        }
        //if not found, add to db with uid as username
        else {
          addUserbyUID(uid);
        }
      }
      catch (Exception e) {System.out.println(e.getMessage());}
      finally { closeConn(conn);}
    }
    //if username isn't numeric
    catch (Exception intExeption) {
      sql = "SELECT * FROM users WHERE username=?";

      try(PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, username);
        rs = stmt.executeQuery();

        //if not found, add user to db
        if(!rs.next()) {
          addUser(username, fullname, institute, uid);
        }
      }
      catch (Exception e) {System.out.println(e.getMessage());}
      finally { closeConn(conn);}
    }
    
    return username;
  }

  //if a userid isn't already in the db and we have no other info, add them as a user using thier uid as username
  public void addUserbyUID(String uid) {
    String sql = "INSERT OR IGNORE INTO users(username,uid) VALUES (?,?)";
    Connection conn = connect();

    try(PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, uid);
      stmt.setString(2, uid);
      stmt.execute();
    }
        
    catch(Exception e) {
      System.out.println(e.getMessage());
    }
        
    finally {
      closeConn(conn);
    }
      
    if(!users.contains(uid)) {
      users.add(uid);
    }
  }
    
  public boolean addUsage(String recorded, String username, String folder, long directories, long files, long size) {
    boolean success = true;
    String sql = "INSERT INTO usage VALUES (?,?,?,?,?,?);";
    Connection conn = connect();

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, recorded);
      stmt.setString(2, username);
      stmt.setString(3, folder);
      stmt.setLong(4, directories);
      stmt.setLong(5, files);
      stmt.setLong(6, size);
      stmt.executeUpdate();
    }

    catch (Exception e) {
      success = false;
    }

    finally {
      closeConn(conn);
    }

    return success;
  }

  public void updateUsage(String recorded, String username, String folder, long directories, long files, long size) {
    String sql = "UPDATE usage SET directories = ?, files = ?, size  =? WHERE recordedDate = ? AND username = ? AND folder = ?;";
    Connection conn = connect();

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, directories);
      stmt.setLong(2, files);
      stmt.setLong(3, size);
      stmt.setString(4, recorded);
      stmt.setString(5, username);
      stmt.setString(6, folder);
      stmt.executeUpdate();
    }

    catch (Exception e) {
      System.out.println(e.getMessage());
    }

    finally {
      closeConn(conn);
    }
  }

  public void setUsers() {
    ResultSet rs;
    String sql = "SELECT * FROM users;";
    Connection conn = connect();

    try(PreparedStatement stmt = conn.prepareStatement(sql)) {
      rs = stmt.executeQuery();

      while(rs.next()) {
        String username = rs.getString("username");

        if(!users.contains(username)) {
          users.add(username);
        }
      }

      rs.close();      
    }

    catch(Exception e) {
      System.out.println(e.getMessage());
    }

    finally {
      closeConn(conn);
    }
  }

  public ArrayList<String> getUsers() {
    return users;
  }

  /*public ArrayList<String> getRecordedDates(String username, String folder) {
        ArrayList<String> recordedDates = new ArrayList<String>();
        String today = LocalDate.now().toString();
        String tenDays = LocalDate.now().minusDays(6).toString();
        String sql = "SELECT recordedDate FROM usage WHERE username = ? AND folder = ? AND recordedDate >= ? AND recordedDate <= ? ORDER BY recordedDate;";
        Connection conn = connect();
        ResultSet rs;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) 
            {
            stmt.setString(1, username);
            stmt.setString(2, folder);
            stmt.setString(3, tenDays);
            stmt.setString(4, today);
            rs = stmt.executeQuery();

            while(rs.next())
                {
                String recorded = (rs.getString("recordedDate"));
                if(!recordedDates.contains(recorded))
                    {
                    recordedDates.add(recorded);
                    }
                }
            rs.close();            
//            System.out.println("getRecordedDates");
            }
        catch (Exception e) 
            {
            System.out.println(e.getMessage());
//            System.out.println("getRecordedDates Error");
            }
        finally
            {
            closeConn(conn);
            }
        return recordedDates;
        }*/


  /*public long getDirectories(String recorded, String username, String folder) {
    long dirs = 0;
    String sql = "SELECT directories FROM usage WHERE recordedDate = ? AND username = ? AND folder = ?;";
    Connection conn = connect();
    ResultSet rs;
      
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, recorded);
      stmt.setString(2, username);
      stmt.setString(3, folder);
      rs = stmt.executeQuery();
      dirs = rs.getLong("directories");
      rs.close();
    }

    catch (Exception e) {
      System.out.println(e.getMessage());
    }
        
    finally {
      closeConn(conn);
    }

    return dirs;
	}/* */

	/*public long getFiles(String recorded, String username, String folder) {
		long files = 0;
    String sql = "SELECT files FROM usage WHERE recordedDate = ? AND username = ? AND folder = ?;";
    Connection conn = connect();
    ResultSet rs;
        
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, recorded);
      stmt.setString(2, username);
      stmt.setString(3, folder);
      rs = stmt.executeQuery();
      files = rs.getLong("files");
      rs.close();
    }

    catch (Exception e) {
      System.out.println(e.getMessage());
    }
        
    finally {
      closeConn(conn);
    }

    return files;
	} */

  public HashMap<String,Long> getSizes(String recorded, String folder) {
		long size = 0;
    String username = "";
    HashMap<String, Long> sizes = new HashMap<String, Long>();

    String sql = "SELECT username, size FROM usage WHERE recordedDate = ? AND folder = ?;";
    Connection conn = connect();
    ResultSet rs;
        
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, recorded);
      stmt.setString(2, folder);
      rs = stmt.executeQuery();
      while(rs.next()) {
        username = rs.getString("username");
        size = rs.getLong("size");
        sizes.put(username, size);
      }
      rs.close();
    }
        
    catch (Exception e) {
      System.out.println(e.getMessage());
    }
    
    finally {
      closeConn(conn);
    }
    return sizes;
	}

  public String getRecords(String recorded, String username, String folder) {
		String record = "";
    String sql = "SELECT * FROM usage WHERE recordedDate = ? AND username = ? and folder = ?;";
    Connection conn = connect();
    ResultSet rs;
    long dirs = 0;
    long files = 0;
    long size = 0;
    
    
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, recorded);
      stmt.setString(2, username);
      stmt.setString(3, folder);
      rs = stmt.executeQuery();

      dirs = rs.getLong("directories");
      files = rs.getLong("files");
      size = rs.getLong("size");
        
      record = size + "\t" + dirs + "\t" + files;        
      
      rs.close();
    }
    catch (Exception e) { System.out.println(e.getMessage()); }
    finally { closeConn(conn); }

  return record;
	}

  public String getHistory(String recorded, String username, String folder) {
		String size = "";
    String sql = "SELECT size FROM usage WHERE recordedDate = ? AND username = ? and folder = ?;";
    Connection conn = connect();
    ResultSet rs;
        
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, recorded);
      stmt.setString(2, username);
      stmt.setString(3, folder);
      rs = stmt.executeQuery();

      if(rs.next()) {

        size += rs.getLong("size");
      }
      //if size isn't known, file requires a ?
      else {
        size = "?";
      }
            
      rs.close();
    }
    catch (Exception e) { System.out.println(e.getMessage()); }
    finally { closeConn(conn); }

  return size;
	}
    
  public void setRanges() {
		long value = 0;
        
    HashMap<String, String> sql = new HashMap<String, String>();
    //SQL to get home folder ranges
    sql.put("minHomeSize", "SELECT username, size AS 'value' FROM usage WHERE recordedDate >= ? AND recordedDate <= ? AND folder = 'home' AND username != 'system'  ORDER BY size ASC LIMIT 1;");
    sql.put("maxHomeSize", "SELECT username, size AS 'value' FROM usage WHERE recordedDate >= ? AND recordedDate <= ? AND folder = 'home' AND username != 'system'  ORDER BY size DESC LIMIT 1");
    sql.put("minHomeDirs", "SELECT username, directories AS 'value' FROM usage WHERE recordedDate >= ? AND recordedDate <= ? AND folder = 'home' AND username != 'system'  ORDER BY directories ASC LIMIT 1;");
    sql.put("maxHomeDirs", "SELECT username, directories AS 'value' FROM usage WHERE recordedDate >= ? AND recordedDate <= ? AND folder = 'home' AND username != 'system'  ORDER BY directories DESC LIMIT 1");
    sql.put("minHomeFiles", "SELECT username, files AS 'value' FROM usage WHERE recordedDate >= ? AND recordedDate <= ? AND folder = 'home' AND username != 'system'  ORDER BY files ASC LIMIT 1;");
    sql.put("maxHomeFiles", "SELECT username, files AS 'value' FROM usage WHERE recordedDate >= ? AND recordedDate <= ? AND folder = 'home' AND username != 'system'  ORDER BY files DESC LIMIT 1");
    //SQL to get projects folder ranges
    sql.put("minProjectsSize", "SELECT username, size AS 'value' FROM usage WHERE recordedDate >= ? AND recordedDate <= ? AND folder = 'projects' AND username != 'system'  ORDER BY size ASC LIMIT 1;");
    sql.put("maxProjectsSize", "SELECT username, size AS 'value' FROM usage WHERE recordedDate >= ? AND recordedDate <= ? AND folder = 'projects' AND username != 'system'  ORDER BY size DESC LIMIT 1");
    sql.put("minProjectsDirs", "SELECT username, directories AS 'value' FROM usage WHERE recordedDate >= ? AND recordedDate <= ? AND folder = 'projects' AND username != 'system'  ORDER BY directories ASC LIMIT 1;");
    sql.put("maxProjectsDirs", "SELECT username, directories AS 'value' FROM usage WHERE recordedDate >= ? AND recordedDate <= ? AND folder = 'projects' AND username != 'system'  ORDER BY directories DESC LIMIT 1");
    sql.put("minProjectsFiles", "SELECT username, files AS 'value' FROM usage WHERE recordedDate >= ? AND recordedDate <= ? AND folder = 'projects' AND username != 'system'  ORDER BY files ASC LIMIT 1;");
    sql.put("maxProjectsFiles", "SELECT username, files AS 'value' FROM usage WHERE recordedDate >= ? AND recordedDate <= ? AND folder = 'projects' AND username != 'system'  ORDER BY files DESC LIMIT 1");
    //SQL to get scratch folder ranges
    sql.put("minScratchSize", "SELECT username, size AS 'value' FROM usage WHERE recordedDate >= ? AND recordedDate <= ? AND folder = 'scratch' AND username != 'system'  ORDER BY size ASC LIMIT 1;");
    sql.put("maxScratchSize", "SELECT username, size AS 'value' FROM usage WHERE recordedDate >= ? AND recordedDate <= ? AND folder = 'scratch' AND username != 'system'  ORDER BY size DESC LIMIT 1");
    sql.put("minScratchDirs", "SELECT username, directories AS 'value' FROM usage WHERE recordedDate >= ? AND recordedDate <= ? AND folder = 'scratch' AND username != 'system'  ORDER BY directories ASC LIMIT 1;");
    sql.put("maxScratchDirs", "SELECT username, directories AS 'value' FROM usage WHERE recordedDate >= ? AND recordedDate <= ? AND folder = 'scratch' AND username != 'system'  ORDER BY directories DESC LIMIT 1");
    sql.put("minScratchFiles", "SELECT username, files AS 'value' FROM usage WHERE recordedDate >= ? AND recordedDate <= ? AND folder = 'scratch' AND username != 'system'  ORDER BY files ASC LIMIT 1;");
    sql.put("maxScratchFiles", "SELECT username, files AS 'value' FROM usage WHERE recordedDate >= ? AND recordedDate <= ? AND folder = 'scratch' AND username != 'system' ORDER BY files DESC LIMIT 1");

    Connection conn = connect();
    
    LocalDate today = LocalDate.now();
    LocalDate weekAgo = today.minusDays(6);
        
    for(String item : sql.keySet()){
      try (PreparedStatement stmt = conn.prepareStatement(sql.get(item))) {
        stmt.setString(1, weekAgo.toString());
        stmt.setString(2, today.toString());
        
        ResultSet rs = stmt.executeQuery();
        ranges.put(item, rs.getLong("value"));
        
        rs.close();
      }
      
      catch (Exception e) {
        System.out.println(e.getMessage());
      }
    }

    closeConn(conn);
	}
    
  public HashMap<String, Long> getRanges() {
    return ranges;
  }

  public HashMap<String, UserStat> getUserRecords(LocalDate end, int minusDays, String username, String folder) {
		HashMap<String, UserStat> records = new HashMap<String, UserStat>();


    String sql = "SELECT * FROM usage WHERE recordedDate = ? AND username = ? AND folder = ?;";
    Connection conn = connect();

    for(LocalDate date = end.minusDays(minusDays); date.isBefore(end); date = date.plusDays(1)) {
      ResultSet rs; 
    
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, date.toString());
        stmt.setString(2, username);
        stmt.setString(3, folder);
        
        rs = stmt.executeQuery();

        UserStat record = new UserStat(username);
        String recordedDate = (date.toString());
          
        record.dirs.set(rs.getLong("directories"));
        record.files.set(rs.getLong("files"));
        record.bytes.set(rs.getLong("size"));
          
        records.put(recordedDate, record);
        rs.close();
        }

      catch (Exception e) { System.out.println(e.getMessage()); }
    }
    
    closeConn(conn);

    return records;
	}

  public long getUserSize(String username, String folder) {
    long size = 0l;
    String today = LocalDate.now().toString();

    String sql = "SELECT size FROM usage WHERE recordedDate = ? AND username = ? AND folder = ?;";
    Connection conn = connect();
    ResultSet rs;    
    
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, today);
      stmt.setString(2, username);
      stmt.setString(3, folder);
      
      rs = stmt.executeQuery();

      size = rs.getLong("size");
    }
    
    catch (Exception e) {System.out.println(e.getMessage());}
    finally { closeConn(conn); }

    return size;
  }

  public String getUserByUID(String uid) {
    String username = "";

    String sql = "SELECT * FROM users WHERE uid = ?;";
    Connection conn = connect();
    ResultSet rs;    
    
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, uid);
      
      rs = stmt.executeQuery();

      username = rs.getString("username");
      
    }
    
    catch (Exception e) {System.out.println(e.getMessage());}
    finally { closeConn(conn); }

    return username;
  }

}
