#!/usr/bin/gnuplot -c

set term png medium transparent size 450,200 

#set xlabel "date" tc "#bfbfbf"
set decimal locale
set format y "%'g"
set ylabel "usage (GB)" #tc "#bfbfbf"
#set tics tc "#bfbfbf"
set grid xtics
set grid ytics
unset border

#stats ARG1 using 2
#set yrange [-0.05:STATS_max]

set xdata time
set timefmt "%Y-%m-%d"
set format x "%m/%y"

set autoscale xfix
set autoscale y


#set xrange [1:215]
# y-axis range
#set yrange [-5:105]
# colorbox range
set cbrange [0:10]
# don't display the heatmap
unset colorbox

# colorbox palette thresholds
set palette defined (0 "green", 10 "red")
set title "home" #tc "#bfbfbf"
set output ARG2
# plot col1:col2:col2(reused for color)
plot ARG1 using 1:2:2 with lines title "" linecolor palette
