/**
 * Script used to reconstruct statistics
 */
 
Map populateWithTimings(reportFile, timingsMap) {
    reportFile.eachLine {String line ->
       if (line.startsWith("msg ID")) {
          return
       }
       def tab = line.split(",")
       int msgId = tab[0].trim() as int
       long time = tab[1].trim() as long
       
       //TODO notify an alert in case of collisions
       timingsMap[msgId] = time
    }
    timingsMap
}

Map calculateTransportTime(prodMap, consMap) {
    Map transportMap = [:]
    consMap.each {msgId, arrivalTime ->
        long time = arrivalTime - prodMap[msgId]
        transportMap[msgId] = time
    }
    transportMap
}
 
Map consumerTimings = [:]
println "Load consumer receive timings"
def consumerReport = new File("/home/andrea/workspace/moquette/consumer_bechmark.txt");
populateWithTimings(consumerReport, consumerTimings)

println "consumer timings loaded ${consumerTimings.size()} records"

Map producerTimings = [:]
def producerReport1 = new File("/home/andrea/workspace/moquette/producer_bechmark_Prod1.txt");
def producerReport2 = new File("/home/andrea/workspace/moquette/producer_bechmark_Prod2.txt");
populateWithTimings(producerReport1, producerTimings)
println "producer 1 timings loaded ${producerTimings.size()} records"

populateWithTimings(producerReport2, producerTimings)
println "producer 2 timings loaded ${producerTimings.size()} records"

Map report = calculateTransportTime(producerTimings, consumerTimings)
File reportFile = new File("/home/andrea/workspace/moquette/report.txt")

def minTime = report.min {it.value}.value / (10 ** 6)
def maxTime = report.max {it.value}.value / (10 ** 6)
def totalTime = 0

report = report.sort()

report.each {msgID, time ->
   totalTime += time
   reportFile <<  "$msgID, $time\n" 
}

def meanTime = (totalTime / report.size() )  / (10 ** 6)
reportFile << "min $minTime \n max $maxTime \n mean $meanTime \n"

println "created report file $reportFile"
//reportFile.close()


