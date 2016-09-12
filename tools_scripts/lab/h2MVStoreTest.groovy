@Grab(group='com.h2database', module='h2-mvstore', version='1.4.192')
import org.h2.mvstore.*

def fileName = "mvtest.h2"
if (args.size() == 1) {
    fileName = args[1]
}
MVStore s = MVStore.open(fileName)
// create/get the map named "data"
MVMap<Integer, String> map = s.openMap("data")

numMessages = 10000000

println "Created store and starting"
long start = System.currentTimeMillis()
(1..numMessages).each {
    String randomContent = UUID.randomUUID().toString()
    // add and read some data
    map.put(it, randomContent)
}
long stop = System.currentTimeMillis()
long contentSize = UUID.randomUUID().toString().bytes.length
println "Wrote $numMessages keys, ${numMessages * contentSize} bytes in ${stop - start} ms"
def writeSpeed = (numMessages * contentSize)/((stop - start)/ 1000)
println "Speed is: $writeSpeed byte/sec"

// close the store (this will persist changes)
s.close()

//The read part

map = MVStore.open(fileName).openMap("data")
//read now 10M keys
start = System.currentTimeMillis()
(1..numMessages).each {
    int key = (int) Math.floor(Math.random() * numMessages) + 1
    // add and read some data
    String randomContent = map.get(key)
}

stop = System.currentTimeMillis()
println "Read $numMessages keys, ${numMessages * contentSize} bytes in ${stop - start} ms"
def readSpeed = (numMessages * contentSize)/((stop - start)/ 1000)
println "Read speed is: $readSpeed byte/sec"
