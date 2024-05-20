package jhi.diskUsage;

import java.text.*;
import java.util.concurrent.atomic.*;

public class UserStat implements Comparable<UserStat> {
	private static DecimalFormat df = new DecimalFormat("0.###");

	/**
	 * Disk usage in bytes of all files owned by this user.
	 */
	public AtomicLong bytes = new AtomicLong();
	/**
	 * Count of all files owned by this user.
	 */
	public AtomicLong files = new AtomicLong();
	/**
	 * Count of all directories owned by this user.
	 */
	public AtomicLong dirs = new AtomicLong();

	/**
	 * The filesystem name of this user.
	 */
	public String name;
	public String fullname;
	public String institute;

	//private boolean queryRun;

	UserStat(String name) {
		this.name = name;
		}

	public String toString() {
		return name
			   + "\t" + fullname + " (" + institute + ")"
			   + "\t" + df.format(dirs.longValue())
			   + "\t" + df.format(files.longValue())
			   + "\t" + df.format(bytes.longValue())
			   + "\t" + df.format(bytes.longValue() / 1024f / 1024f / 1024f);
		}

	public String getName() {
		return name;
		}

	public String getFullname() {
		return fullname;
		}

	public String getInstitute() {
		return institute;
		}

	public long getDirs() {
		return (dirs.longValue());
		}

	public long getFiles() {
		return (files.longValue());
		}

	public long getBytes() {
		return (bytes.longValue());
		}

	public int compareTo(UserStat other) {
		// We want largest first, so return MINUS-the-comparison
		return -Long.compare(bytes.longValue(), other.bytes.longValue());
		}
	}