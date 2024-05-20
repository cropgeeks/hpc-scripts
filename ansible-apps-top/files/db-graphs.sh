#!/bin/bash

cd /mnt/cluster/www/top/rrds

DBUSAGE=hpc_usage.rrd
DBWATTS=hpc_watts.rrd

# Create the USAGE RRD if it doesn't exist
if [ ! -f $DBUSAGE ]; then
  rrdtool create $DBUSAGE  \
    --start now --step 1m   \
    DS:usage:GAUGE:2m:0:100 \
    DS:alloc:GAUGE:2m:0:100 \
    DS:mem:GAUGE:2m:0:100   \
    DS:disk:GAUGE:2m:0:100   \
    RRA:AVERAGE:0.5:1m:1d   \
    RRA:AVERAGE:0.5:5m:7d   \
    RRA:AVERAGE:0.5:15m:1M  \
    RRA:AVERAGE:0.5:1h:10y
fi

# Create the WATTS RRD if it doesn't exist
if [ ! -f $DBWATTS ]; then
  rrdtool create $DBWATTS  \
    --start now --step 1m   \
    DS:watts:GAUGE:2m:0:U \
    DS:hpc:GAUGE:2m:0:U \
    DS:storage:GAUGE:2m:0:U \
    DS:other:GAUGE:2m:0:U \
    RRA:AVERAGE:0.5:1m:1d   \
    RRA:AVERAGE:0.5:5m:7d   \
    RRA:AVERAGE:0.5:15m:1M  \
    RRA:AVERAGE:0.5:1h:10y
fi

# stats-rrd.txt contains a simple summary of what we need from stats.json
USAGE=$(grep "rrd_usage" stats.txt | awk '{print $2}')
ALLOC=$(grep "rrd_alloc" stats.txt | awk '{print $2}')
MEM=$(grep "rrd_mem" stats.txt | awk '{print $2}')
DISK=$(grep "rrd_disk" stats.txt | awk '{print $2}')
rrdtool update $DBUSAGE N:$USAGE:$ALLOC:$MEM:$DISK

WATTS=$(grep "rrd_watts" stats.txt | awk '{print $2}')
HPC=$(grep "rrd_hpc_watts" stats.txt | awk '{print $2}')
STORAGE=$(grep "rrd_beegfs_watts" stats.txt | awk '{print $2}')
OTHER=U
rrdtool update $DBWATTS N:$WATTS:$HPC:$STORAGE:$OTHER


# Produce the graphs

# Outer loop
TIMES=("1h" "1d" "1w" "1M" "1y")
TITLES=("Hour" "Day" "Week" "Month" "Year")
# Inner loop
NAMES=("small" "large")
WIDTHS=("397" "897")
HEIGHTS=("197" "674")

for i in {0..4}
do

  for j in {0..1}
  do

    DB=hpc_usage.rrd

    rrdtool graph usage${NAMES[$j]}_${TIMES[$i]}.png \
      --start now-${TIMES[$i]} --end now \
      --width ${WIDTHS[$j]} --height ${HEIGHTS[$j]} --full-size-mode \
      --lower-limit '0' \
      --title "Cluster Usage Last ${TITLES[$i]}" \
      --vertical-label 'Percent' \
      --color SHADEA#e5e5e5 --color SHADEB#e5e5e5 \
      --font LEGEND:7.5 \
      DEF:usage=$DB:usage:AVERAGE \
      DEF:alloc=$DB:alloc:AVERAGE \
      DEF:mem=$DB:mem:AVERAGE \
      DEF:disk=$DB:disk:AVERAGE \
      AREA:usage#dea36a:"CPU Usage" \
      LINE2:alloc#6ab0de:"Allocated CPUs" \
      LINE2:mem#1abc9c:"Mem Usage" \
      LINE2:disk#de6a6c:"Disk Usage"

    DB=hpc_watts.rrd

    rrdtool graph watts${NAMES[$j]}_${TIMES[$i]}.png \
      --start now-${TIMES[$i]} --end now \
      --width ${WIDTHS[$j]} --height ${HEIGHTS[$j]} --full-size-mode \
      --lower-limit '0' \
      --title "Power Draw Last ${TITLES[$i]}" \
      --vertical-label 'Watts' \
      --color SHADEA#e5e5e5 --color SHADEB#e5e5e5 \
      --font LEGEND:7.5 \
      DEF:watts=$DB:watts:AVERAGE \
      DEF:hpc=$DB:hpc:AVERAGE \
      DEF:storage=$DB:storage:AVERAGE \
      AREA:storage#555cc5:STACK:"Storage Nodes" \
      AREA:hpc#6a92de:STACK:"HPC Nodes" \
    
  done

done
