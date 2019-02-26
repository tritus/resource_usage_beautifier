#!/usr/bin/env kscript
//DEPS com.github.holgerbrandl:kravis:0.5

import java.awt.Dimension
import java.io.File
import java.io.FileWriter
import kotlin.math.roundToInt
import kravis.*
import krangl.*

data class ProcessResourceUsage(val virtualMemory: Int, val residentMemory: Int, val sharedMemory: Int, val processorUsagePercent: Int)
data class ProcessInformation(val pid: Int, val name: String, val resourceUsage: ProcessResourceUsage)
data class MemoryState(val date: String, val processes: List<ProcessInformation>)
data class Filepath(val rawPath: String)

val newMemoryStateLineRegex = Regex("^top -.+$")
val newProcessInfoLineRegex = Regex("^\\s*\\d+\\s.+$")
val memoryStateDateRegex = Regex("^top - (\\S+:\\S+:\\S+)\\s.*$")
val processInfoRegex = Regex("^\\s*(\\d+)\\s+\\S+\\s+\\S+\\s+\\S+\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+\\S+\\s+(\\S+)\\s+\\S+\\s+\\S+\\s+(\\S+)\\s*$")

fun extractNewMemoryStateFromLine(line: String): MemoryState? {
    return memoryStateDateRegex
        .find(line)
        .also { it ?: println("did not find anything in $line using regex $memoryStateDateRegex") }
        ?.destructured
        ?.let { MemoryState(it.component1(), listOf()) }
}

fun extractProcessInfoFromLine(line: String): ProcessInformation? {
    return processInfoRegex
        .find(line)
        ?.destructured
        ?.let {
            ProcessInformation(
                it.component1().toInt(),
                it.component6(),
                ProcessResourceUsage(
                    extractMemoryQuantity(it.component2()),
                    extractMemoryQuantity(it.component3()),
                    extractMemoryQuantity(it.component4()),
                    it.component5().toFloat().roundToInt()
                )
            )
        }
}

fun extractMemoryQuantity(quantity: String): Int {
    return if (quantity.takeLast(1) == "g") {
        (quantity.dropLast(1).toFloat() * 1024 * 1024 * 1024).toInt()
    } else if (quantity.takeLast(1) == "m") {
        (quantity.dropLast(1).toFloat() * 1024 * 1024).toInt()
    } else if (quantity.takeLast(1) == "k") {
        (quantity.dropLast(1).toFloat() * 1024).toInt()
    } else {
        quantity.toInt()
    }
}

fun addProcessInfoToLastMemoryRecord(data: MutableList<MemoryState>, newProcessInfo: ProcessInformation) {
    val lastDataIndex = data
        .lastIndex
        .takeIf { it > -1 }
        ?: throw IllegalStateException("Cannot find last memory index in list : $data")
    data[lastDataIndex] = data[lastDataIndex].let { MemoryState(it.date, it.processes + newProcessInfo) }
}

fun exportProcessesAsCsv(data: List<MemoryState>, outputFolderPath: Filepath): Filepath {
    return Filepath("${outputFolderPath.rawPath}/allProcessData.csv")
        .also { path ->
            writeToFile(path) { fileWriter ->
                fileWriter.append("date,virtualMemory,residentMemory,sharedMemory,processorUsagePercent,processId")
                fileWriter.append('\n')
                data.forEach { currentState ->
                    currentState
                        .processes
                        .forEach {
                            fileWriter.append("${currentState.date},${it.resourceUsage.virtualMemory},${it.resourceUsage.residentMemory},${it.resourceUsage.sharedMemory},${it.resourceUsage.processorUsagePercent},${it.pid}${it.name}")
                            fileWriter.append('\n')
                        }
                }
            }
        }
}

fun writeToFile(path: Filepath, writingAction: (FileWriter) -> Unit) {
    var fileWriter: FileWriter? = null
    try {
        fileWriter = FileWriter(path.rawPath)
        writingAction(fileWriter)
    } finally {
        try {
            fileWriter!!.flush()
            fileWriter.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun exportProcessesPlots(dataCsvPath: Filepath, outputPath: Filepath) {
    DataFrame.readCSV(dataCsvPath.rawPath)
        .also { exportColumnToGraph(it, "processorUsagePercent", outputPath) }
        .also { exportColumnToGraph(it, "virtualMemory", outputPath) }
        .also { exportColumnToGraph(it, "residentMemory", outputPath) }
        .also { exportColumnToGraph(it, "sharedMemory", outputPath) }
}

fun exportColumnToGraph(dataFrame: DataFrame, columnName: String, outputPath: Filepath) {
    dataFrame
        .plot(x = "date", y = columnName, color = "processId")
        .geomPath()
        .save(File("${outputPath.rawPath}/${columnName}.png"), Dimension(1900, 1080))
}



println("Extracting data from the log file")
var memoryData = mutableListOf<MemoryState>()
File(args[0])
    .forEachLine { line ->
        print(".")
        if (newMemoryStateLineRegex.matches(line)) {
            extractNewMemoryStateFromLine(line)
                ?.also { memoryData.add(it) }
                ?: throw IllegalStateException("Could not create new memory data from line '$line'")
        } else if (newProcessInfoLineRegex.matches(line)) {
            extractProcessInfoFromLine(line)
                ?.also { addProcessInfoToLastMemoryRecord(memoryData, it) }
                ?: throw IllegalStateException("Could not extract process data for line '$line'")
        }
    }.also {
        println("")
        println("Exporting data to separated csv files")
        exportProcessesAsCsv(memoryData, Filepath(args[1]))
            .also { exportProcessesPlots(it, Filepath("${args[1]}")) }
    }
