#!/bin/bash -l

USERNAME=$1
ADDRESS=$2

if [ ! -f "/mnt/cluster/last/$USERNAME" ]; then
    echo "$1 does not exist"
    exit 1
fi

# Clean up any left over files from previous runs
rm -rf tmp/
mkdir tmp/

mkdir -p reports/$(date +'%Y-%m-%d')
UUID=$(uuidgen)
HTML=reports/$(date +'%Y-%m-%d')/$USERNAME.$UUID.html


# Introduction
cat header.html > $HTML
./Intro.jsh $USERNAME $HTML

# Disk usage
echo diskusage/$USERNAME.txt
./graph-home.sh diskusage/$USERNAME.txt tmp/home.png
./graph-projects.sh diskusage/$USERNAME.txt tmp/projects.png
./graph-scratch.sh diskusage/$USERNAME.txt tmp/scratch.png
./DiskUsage.jsh $USERNAME $HTML

# Slurm (reportseff)
echo "querying..."
/usr/local/bin/reportseff -u $USERNAME -p --format 'jobid,partition,jobname,ncpus,cputimeraw,elapsed,reqmem,state,cpueff,memeff,MaxRSS,NodeList' --extra-args '--units G' > tmp/seff.txt

# Slurm (generate report)
echo "processing..."
./Slurm.jsh tmp/seff.txt $HTML tmp/graph.txt
./graph-cpu.sh tmp/graph.txt tmp/cpu.png
./graph-mem.sh tmp/graph.txt tmp/mem.png

# BASE64 encode the PNGs and insert into the HTML
PNG=$(base64 -w 0 tmp/home.png)
replace HOME.PNG $PNG -- $HTML
PNG=$(base64 -w 0 tmp/projects.png)
replace PROJECTS.PNG $PNG -- $HTML
PNG=$(base64 -w 0 tmp/scratch.png)
replace SCRATCH.PNG $PNG -- $HTML
PNG=$(base64 -w 0 tmp/cpu.png)
replace CPU.PNG $PNG -- $HTML
PNG=$(base64 -w 0 tmp/mem.png)
replace MEM.PNG $PNG -- $HTML


# And finally email...
EMAIL=$(cat $HTML)
(
echo "To: $ADDRESS"
echo "Subject: Crop Diversity HPC - Your Weekly Summary"
echo "Mime-Version: 1.0"
echo "Content-Type: text/html; charset=utf-8"
echo "Content-Transfer-Encoding: 8bit"
echo
echo "<p>Hi $USERNAME,</p>"
echo "<p>Your weekly usage report for Crop Diversity HPC is now ready, showing a "
echo "summary of your current disk usage and Slurm job statistics over the last "
echo "seven days.</p>"
echo "<p>It is important that you review this information.</p>"
echo "<p>You can view your report here:<p>&nbsp;&nbsp;&nbsp;&nbsp;"
echo "<a href='https://cropdiversity.ac.uk/$HTML'>https://cropdiversity.ac.uk/$HTML</a>.</p>"
) | /usr/sbin/sendmail -t -f noreply@cropdiversity.ac.uk -F "Crop Diversity Help"

echo ""
