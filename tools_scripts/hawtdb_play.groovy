@Grab(group='org.fusesource.hawtdb', module='hawtdb', version='1.6')

import org.fusesource.hawtbuf.codec.StringCodec
import org.fusesource.hawtdb.api.BTreeIndexFactory
import org.fusesource.hawtdb.api.PageFile
import org.fusesource.hawtdb.api.PageFileFactory
import org.fusesource.hawtdb.api.SortedIndex



File tmpFile = new File(System.getProperty("user.home") + File.separator + "test_hawtdb.dat")
//tmpFile.createNewFile()

def writeSomeData(tmpFile) {
    def factory = new PageFileFactory()
    factory.file = tmpFile
    factory.open()
    def pageFile = factory.getPageFile()
    
    BTreeIndexFactory<String, String> indexFactory = new BTreeIndexFactory<String, String>()
    indexFactory.setKeyCodec(StringCodec.INSTANCE)
    
    SortedIndex<String, String> writeIndex = indexFactory.openOrCreate(pageFile)
    println "Hawt opened, try to read"
    println "Size: ${writeIndex.size()}"
    
    println "write something"
    writeIndex.put("titolo", "capucetto rosso")
    pageFile.flush()
    
    println "check after write content: ${writeIndex.get("titolo")}, size ${writeIndex.size()}"
    
    //pageFile.close()
    factory.close()
}
 

def readAllFileContents(tmpFile) {
    def factory = new PageFileFactory()
    factory.file = tmpFile
    factory.open()
    def pageFile = factory.getPageFile()
    
    BTreeIndexFactory<String, String> indexFactory = new BTreeIndexFactory<String, String>()
    indexFactory.setKeyCodec(StringCodec.INSTANCE)
    
    SortedIndex<String, String> readingIndex = indexFactory.openOrCreate(pageFile)
    
    println "read something, size ${readingIndex.size()}"
    for (def entry : readingIndex) { 
        println "read k, v -> ${entry.key}, ${entry.value} "
    }
    
    factory.close()
}


writeSomeData(tmpFile)       
readAllFileContents(tmpFile)       






