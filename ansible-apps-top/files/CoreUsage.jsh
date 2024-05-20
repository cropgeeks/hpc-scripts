#!/mnt/cluster/apps/java/latest/bin/java --source 11

import java.io.*;
import java.util.*;

import java.io.*;
import java.util.*;


public class CoreUsage
{
    private static File input;
    private HashMap<String, User> users = new HashMap<String, User>();
    private Scanner reader;
    private String[] currentLine;
    private Integer totalCores = 0;
    private HashMap<String, Integer> queueTotals = new HashMap<String, Integer>();
	
    public static void main(String[] args)
        throws IOException
    {
        input = new File(args[0]);
		CoreUsage wibble = new CoreUsage();
        wibble.readFile();
        
        wibble.printJSON();        
    }

    public void readFile()
    {
        try
        {
            reader = new Scanner(input);
        }
        catch (Exception e) { System.out.println(e.getStackTrace());}

        while(reader.hasNextLine())
        {
            currentLine = reader.nextLine().strip().split("\\s+");
                    
            if(isRunning(currentLine[2]))
            {
                if (!users.containsKey(currentLine[0]))
                {
                    users.put(currentLine[0], new User(currentLine[0]));
                }
                users.get(currentLine[0]).addJob(currentLine[1], getCores(currentLine[3], currentLine[4]));

                if (!queueTotals.containsKey(currentLine[1]))
                {
                    queueTotals.put(currentLine[1], 0);
                }

                Integer queueCores = queueTotals.get(currentLine[1]) + getCores(currentLine[3], currentLine[4]);
                queueTotals.replace(currentLine[1], queueCores);
                

                totalCores += getCores(currentLine[3], currentLine[4]);
            }
        }
    }

    public boolean isRunning(String status)
    {
        if (status.equalsIgnoreCase("R")) { return true; }
        return false;
    }

    public Integer getCores(String cpus, String nodes)
    {
        Integer cores = Integer.parseInt(cpus);
        String[] nodeList = nodes.split(",");

        if (cores%2 != 0) { cores ++; }

        if (nodeList.length > 1) { cores *= nodeList.length; }

        return cores;
    }

    public void printJSON() throws IOException
    {
        int counter = 0;
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("cores.json")));

        User[] sortedUsers = users.values().toArray(new User[0]);
        Arrays.sort(sortedUsers, Collections.reverseOrder());

        if(totalCores == 0)
        {
            out.println();
        }
        else
        {

            out.println("{");
            out.println(" \"totals\": [");		
            out.println("  {\"totalCores\": " + totalCores + ",");
            for (String queue :  queueTotals.keySet())
            {
                counter++;
                out.print("  \""+ queue + "\": " + queueTotals.get(queue));
                if (counter < queueTotals.size())
                {
                    out.println(",");
                }
                else
                {
                    out.println("}");
                }
            }
            out.println(" ],");	
            out.println("  \"users\": [");

            counter = 0;

            
            for (User user: sortedUsers)
            {
                out.println("   {");
                counter ++;
                user.printUser(out);

                if(counter < users.size())
                {
                    out.println("  },");
                }
                else
                {
                    out.println("  }");
                }
            }

            out.println(" ]");
            out.println("}");
        }

        out.close();

    }

    public class User implements Comparable<User>
    {
        private String name;
        private HashMap<String, Queue> queues = new HashMap<String, Queue>();
        private Integer totalCores = 0;

        public User(String name)
        {
            setName(name);
        }

        public void setName(String name) { this.name = name; }
        public void addJob(String queueName, Integer cores) 
        {
            totalCores += cores;
            if(queues.containsKey(queueName))
            {
                queues.get(queueName).setCores(cores);
            }
            else
            {
                queues.put(queueName, new Queue(queueName, cores));
            }

        }

        public void printUser(PrintWriter out)
        {
            int counter = 0;
            out.println("   \"name\": \"" + name + "\",");
            out.println("   \"totalCores\" : " + totalCores + ",");
            out.println("   \"queues\": [");

            for(Queue queue : queues.values())
            {
                out.println("    {");

                counter ++;
                queue.printQueue(out);

                if(counter < queues.size())
                {
                    out.println("    },");
                }
                else
                {
                    out.println("    }");
                }
            }
            out.println("   ]");
        }

        public int compareTo(User other) { return totalCores.compareTo(other.totalCores); }

        public class Queue
        {
            private String name;
            private Integer cores = 0;
            
            public Queue(String name, Integer cores)
            {
                setName(name);
                setCores(cores);
            }

            public void setName(String name) { this.name = name; }
            public void setCores(Integer cores)
            {
                this.cores = this.cores + cores;
            }

            public void printQueue(PrintWriter out)
            {
                out.println("     \"name\": \"" + name + "\",");
                out.println("     \"cores\": " + cores);  
            }
        }
    }
}