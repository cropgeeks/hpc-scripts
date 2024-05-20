#!/bin/bash

# Sleep for long enough to give the nodes a chance to report
sleep 15

# Collate all the stats into stats.json
cd /mnt/cluster/www/top
./CreateStats.jsh

# Update the RRD databases and produce graphs
sh db-graphs.sh
