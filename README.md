__Dependencies :__

- kotlin compiler (`brew install kotlin` on mac)
- kscript interpreter (`brew install kscript` on mac)
- R (`brew install R` on mac)
- R packages : forcats, readr, ggplot2, dplyr (in terminal, launch R console by entering `R` and in the console, then `install.packages("forcats", "readr", "ggplot2", "dplyr")`)

__Usage :__

The script takes two arguments :
  1. the log file path
  2. the output folder path where all processes info will be saved

The format for the memory log is the output of `top -b`. Such log can be obtained by running commands like `top -b >> memory_usage.log`
The output will be a CSV files with all the logging data well organized and a bunch of graphs. One graph for each resource observed during the logging session.


__Example :__

`memory_log_reader.kts memory_usage.log export`