#!/bin/bash

# Random sleep to try and stop every node hammering the head node's NFS at the
# same time and effecting its CPU usage readings etc
sleep $(($RANDOM % 10))

cd /mnt/cluster/www/top/stats

# Short form of the server name
TMPFILE=$(mktemp)
NAME=$(hostname -s)

# CPU usage (percentage) on a 0-100 scale
USAGE=$(top -bn1 | grep "Cpu(s)" | sed "s/.*, *\([0-9.]*\)%* id.*/\1/" | awk '{print 100 - $1}')
echo "USAGE=$USAGE" > $TMPFILE

# System uptime
UPTIME=$(uptime -p | awk '{$1=""; print $0}')
echo "UPTIME=$UPTIME" >> $TMPFILE

# Number of (unique) users logged on
USERS=$(w | awk 'FNR>2{print $1}' | sort -u | wc -l)
echo "USERS=$USERS" >> $TMPFILE

# Total memory
#MEMTOTAL=$(free | awk 'NR==2{print $2}')
#echo "MEMTOTAL=$MEMTOTAL" >> $TMPFILE

# Used memory
MEMUSED=$(free | awk 'NR==2{print $3}')
echo "MEMUSED=$MEMUSED" >> $TMPFILE

# System power (in watts)
RACADM=/opt/dell/srvadmin/sbin/racadm
if [ -x "$(command -v $RACADM)" ]; then
#  if [ "$NAME" == "n17-28-1536-apollo" ]; then
    WATTS=$(ipmitool sdr | grep "Pwr Consumption" | awk '{print $4}')
#  else
#    WATTS=$($RACADM getsensorinfo | grep Watts | awk '{print $6}')
#    WATTS=$(echo $WATTS|tr -d '\r')
#  fi  
  echo "WATTS=$WATTS" >> $TMPFILE
fi

# Slurm stats
if [ -f "/etc/profile.d/slurm.sh" ]; then
  . /etc/profile.d/slurm.sh
  
  # SLURM CPU/slot allocation for this node
  ALLOC=$(sinfo -o "%30N %14C" -N | grep -m 1 ${NAME} | awk '{print $2}')
  echo "ALLOC=$ALLOC" >> $TMPFILE

  # Count of active Slurm jobs
  JOBCOUNT=$(squeue -h -o "%t" | wc -l)
  echo "JOBCOUNT=$JOBCOUNT" >> $TMPFILE

  # Number of users with jobs (active or waiting)
  JOBUSERS=$(squeue -h -o "%u" | sort -u | wc -l)
  echo "JOBUSERS=$JOBUSERS" >> $TMPFILE

  # Allocated memory
  MEMALLOC=$(scontrol show node $NAME | grep AllocMem | awk -v FS="(AllocMem=| FreeMem)" '{print $2}')
  echo "MEMALLOC=$MEMALLOC" >> $TMPFILE
fi

# Disk space - we read what path to check from the nodes.txt file (col 5)
DISKPATH=$(grep $NAME ../nodes.txt | awk '{print $5}')
DISKTOTAL=$(df -B 1 $DISKPATH | awk 'NR==2{print $2}')
DISKUSED=$(df -B 1 $DISKPATH | awk 'NR==2{print $3}')
echo "DISKTOTAL=$DISKTOTAL" >> $TMPFILE
echo "DISKUSED=$DISKUSED" >> $TMPFILE

# Copy the tempfile back into this folder
mv $TMPFILE $NAME
chmod 644 $NAME
