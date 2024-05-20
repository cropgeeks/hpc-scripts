#!/usr/bin/gnuplot -c

set term png medium transparent size 450,275 

#set xlabel "job" tc "#bfbfbf"
#set ylabel "%" tc "#bfbfbf"
set tics #tc "#bfbfbf"
set grid xtics
set grid ytics
set grid mxtics 
set grid mytics
unset xtics

#set xrange [1:215]
# y-axis range
set yrange [-5:105]
# colorbox range
set cbrange [0:100]
# don't display the heatmap
unset colorbox

# colorbox palette thresholds
set palette defined (0 "red", 20 "#bfbfbf", 80 "green")
set title "CPU Efficiency" #tc "#bfbfbf"
set output ARG2
# plot col1:col2:col2(reused for color)
plot ARG1 using 1:2:2 with points title "" linecolor palette
