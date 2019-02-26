__Dependencies :__

- kotlin compiler (`brew install kotlin` on mac)
- kscript interpreter (`brew install kscript` on mac)
- R (`brew install R` on mac)
- R packages : forcats, readr, ggplot2, dplyr (in terminal, launch R console by entering `R` and in the console, then `install.packages("forcats", "readr", "ggplot2", "dplyr")`)

__Usage :__

The script takes two arguments :
  1. the log file path
  2. the output folder path where all processes info will be saved

The format for the memory log is the output of 'top -b'. Such log can be obtained by running commands like 'top -b >> memory_usage.log'
The output will be a bunch of csv files. One csv for each process observed during the logging session. On each csv, you'll have the memory and processor consumption of the process for each log time.


__Example :__

`memory_log_reader.kts memory_usage.log export`