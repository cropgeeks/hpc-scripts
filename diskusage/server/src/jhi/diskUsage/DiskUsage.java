package jhi.diskUsage;

import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

public class DiskUsage {
	// Executor used for doing the multicore processing
	private ThreadPoolExecutor executor;

	// Hashtable of results: key=name, value=UserStat
	private ConcurrentHashMap<String, UserStat> map;

	// The top-level directories being scanned
	private String[] topLevelDirectories;

	private boolean okToRun = true;
	private javax.swing.Timer timer;
	private long lastBytes = 0;

	public static void main(String[] args)
    throws Exception {
      //String[] args1 = {"1", "C:\\Becky\\Documents\\Projects\\diskUsage-v4"};

    if (args.length == 0) {
      System.out.println("Usage: [cores] <folder1> [<folder2] ... <folderN>]");
      System.exit(1);
      }

    new DiskUsage(args).run();
    System.exit(0);
    }


	/**
	 * Constructs a new DiskUsage instance. The number of cores to use for
	 * scanning can optionally be provided, followed by one or more folders.
	 *
	 * @param args of the form: [cores] <folder> [<folder...]
	 */
	public DiskUsage(String... args) {
		int cores = Runtime.getRuntime().availableProcessors();

		// Attempt to read the numCores as the first argument.
		if (args.length > 1) {
			try {
				cores = Integer.parseInt(args[0]);
				// If ok, strip it off the list so all we have left are folders
				args = Arrays.copyOfRange(args, 1, args.length);
				}
			catch (Exception ne) {}
			}

		topLevelDirectories = args;

		executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(cores);
//		map = new IPAManager().query();
		map = new ConcurrentHashMap<>();
		}

	private void addToQueue(Path path) {
		PathRunnable pr = new PathRunnable(path);
		executor.submit(pr);

		if (timer != null) {
			timer.restart();
			}
		}

	/**
	 * Runs the scanner and returns a list of user statistics.
	 *
	 * @return a list of UserStat objects, one per user for all users found
	 * owning files within the scanned directory structure
	 */
	public List<UserStat> run()
    throws Exception {

    DateFormat df = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    df.setTimeZone(TimeZone.getTimeZone("UTC"));

    long t1 = System.currentTimeMillis();

    // Submit the top-level directories to the thread queue
    for (String topLevelDirectory : topLevelDirectories) {
      addToQueue(Paths.get(topLevelDirectory));
      }

    // Horrible hack (tm). If the number of tracked bytes hasn't changed for
    // 30 seconds, then terminate the scan. Any new additions to the queue
    // will reset the timer. Ideally, we'd just track the number of active
    // jobs, but rare cases have seen the scan not complete (with a queue=0)
    // with some unknown job probably still active. This hack hopefully
    // avoids that by giving queue=0 threads time to complete (or add to the
    // queue again).
    timer = new javax.swing.Timer(10000,
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          System.out.println("Timer: 10s since last check");
          if (getSystemUser().bytes.longValue() == lastBytes && executor.getQueue().size() == 0) {
            System.out.println("Timer: No change - terminating");
            okToRun = false;
            }
          lastBytes = getSystemUser().bytes.longValue();
          }
        });
    timer.start();

    // And another failsafe - quit after 12 hours regardless of state
/*	javax.swing.Timer timer2 = new javax.swing.Timer(43200000, 
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          System.out.println("Timer: 12 hours elasped - terminating");
          okToRun = false;
          }
        });
      timer2.start(); */

    try {
      while (okToRun) {
        long t2 = System.currentTimeMillis();
        System.out.print(df.format(new Date(t2 - t1)) + "\t");

        System.out.print(getSystemUser());
        System.out.println("\tQ" + executor.getQueue().size());

        Thread.sleep(5000);
        }
      timer.stop();
      }
    catch (InterruptedException e) {}

    // Strip the collection of users down to just those found on this run,
    // which will be any with a byte count > 0
    List<UserStat> users = map.values().stream()
      .filter(user -> user.bytes.longValue() > 0)
      .collect(Collectors.toList());
    users.add(0, getSystemUser());

    // Sort (by total byte count)
    Collections.sort(users);

    // The print the results
    System.out.println("\nUser\tDirs\tFiles\tBytes\tGB");
    for (UserStat user : users) {
      System.out.println(user);
      }

    return users;
    }

  // Returns a "system" user containing total files, directories, and bytes
  private UserStat getSystemUser() {
    UserStat system = new UserStat("system");

    for (UserStat user : map.values()) {
      system.bytes.addAndGet(user.bytes.longValue());
      system.files.addAndGet(user.files.longValue());
      system.dirs.addAndGet(user.dirs.longValue());
      }

    return system;
    }

	private class PathRunnable implements Runnable {
		// The directory being processed by this thread
		Path directory;

		// Tracks the last UserStat referenced by this thread. Chances are
		// each file in a directory is owned by the same user, so the same
		// object will be reusabable in the vast majority of cases
		UserStat lastUser = null;

		PathRunnable(Path directory) {
			this.directory = directory;
			}

		public void run() {
			// We need to get the owner/size info for the directory itself. This
			// is not the size of its contents but just its file handle.
			try {
				processPath(directory, true);
				}
			catch (Exception e) {}

			// Now get a list of Path objects in this directory
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(directory)) {
				// Then process each Path
				for (Path path : ds) {
					try {
						// For a (non symlink) directory, add it to the queue
						if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
							addToQueue(path);
							}
						else {
							processPath(path, false);
							}
						}
					catch (Exception e) {}
					}
				}
			catch (IOException e) {
				System.out.println(directory + " - " + e);
				}
			}

		private void processPath(Path path, boolean isDirectory)
      throws Exception {
			String name = null;
			long size = 0;

			// Is this path a symlink?
			if (Files.isSymbolicLink(path)) {
				BasicFileAttributes bfa = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

				name = Files.getOwner(path, LinkOption.NOFOLLOW_LINKS).getName();
				size = bfa.size();
				}

			// Or a normal file or directory?
			else {
				name = Files.getOwner(path).getName();
				size = Files.size(path);
				}


			// Update the user information
			UserStat user = null;

			// Can we simply reuse the same user as last time?
			if (lastUser != null && name.equals(lastUser.name)) {
				user = lastUser;
				}

			// If not, make a new one, and add or retrieve it
			// from the hashmap of users
			else {
				user = new UserStat(name);
				UserStat existing = map.putIfAbsent(name, user);
				if (existing != null) {
					user = existing;
					}
				}

			// Increment the totals
			user.bytes.addAndGet(size);
			if (isDirectory) {
				user.dirs.incrementAndGet();
				}
			else {
				user.files.incrementAndGet();
				}

			lastUser = user;
			//			System.out.println(path + " - " + name + " - " + size);
			}
		}
	}