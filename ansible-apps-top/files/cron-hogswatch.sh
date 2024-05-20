#!/bin/bash

cd /mnt/cluster/www/hogwatch

. /etc/profile.d/slurm.sh
squeue -o "%8u %10P %.2t %.4C %.10R %m" > cores.txt
squeue -o "%.20i %2P %.8j %.8u %.2t %.10M %.4C %.10R %m" > squeue.txt
./CoreUsage.jsh cores.txt
