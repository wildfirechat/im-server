@Grab(group='org.mapdb', module='mapdb', version='1.0.8')
import org.mapdb.DB
import org.mapdb.DBMaker

class User implements Serializable {
    String name
    String surname
}

def writeSomeData(Map m) {
    m.put(1, new User(name: "mario", surname: "rossi"))
    m.put(2, new User(name: "gianni", surname: "bianchi"))
    m.put(3, new User(name: "pino", surname: "verdi"))
}

def readSomeData(Map m) {
    m.each {k, v ->
        println "id: $k, value: $v"
    }
}

if (args.size() != 1) {
    println "groovy mapdbWalTest.groovy [writer|reader]"
    exit 1
}

String mode = args[0]
File tmpFile = new File("test_store.mapdb")
tmpFile.createNewFile()
DB db = DBMaker.newFileDB(tmpFile).make()
Map mapStore = db.getHashMap("storageMap")

switch (mode) {
    case "writer":
        writeSomeData(mapStore)
        db.commit()
        return
    case "reader":
        readSomeData(mapStore)
        return
    default:
        println "Error option not recognized must be one of [writer|reader]"
}