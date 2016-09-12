@Grab(group='org.mapdb', module='mapdb', version='1.0.8')
import org.mapdb.DB
import org.mapdb.DBMaker

def fileName = "mapdbtest.mapdb"
if (args.size() == 1) {
    fileName = args[1]
}
File tmpFile = new File(fileName)
// create/get the map named "data"
tmpFile.createNewFile()
DB db = DBMaker.newFileDB(tmpFile).make()
Map map = db.getHashMap("data")

numMessages = 1000000

println "Created store and starting"
long start = System.currentTimeMillis()
(1..numMessages).each {
    String randomContent = UUID.randomUUID().toString()
    // add and read some data
    map.put(it, randomContent)
}
// close the store (this will persist changes)
db.commit()
db.close()
long stop = System.currentTimeMillis()
long contentSize = UUID.randomUUID().toString().bytes.length
println "Wrote $numMessages keys, ${numMessages * contentSize} bytes in ${stop - start} ms"
def writeSpeed = (numMessages * contentSize)/((stop - start)/ 1000)
println "Speed is: $writeSpeed byte/sec"



//The read part
db = DBMaker.newFileDB(tmpFile).make()
map = db.getHashMap("data")
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
