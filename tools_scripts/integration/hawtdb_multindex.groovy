@Grab(group='org.fusesource.hawtdb', module='hawtdb', version='1.6')

import org.fusesource.hawtbuf.codec.StringCodec
import org.fusesource.hawtdb.api.IndexFactory
import org.fusesource.hawtdb.api.HashIndexFactory
import org.fusesource.hawtdb.api.BTreeIndexFactory
import org.fusesource.hawtdb.api.PageFile
import org.fusesource.hawtdb.api.PageFileFactory
import org.fusesource.hawtdb.api.MultiIndexFactory
import org.fusesource.hawtdb.api.SortedIndex



File tmpFile = new File(System.getProperty("user.home") + File.separator + "test_hawtdb.dat")
//tmpFile.createNewFile()

def writeSomeData(tmpFile) {
    def factory = new PageFileFactory()
    factory.file = tmpFile
    factory.open()
    def pageFile = factory.getPageFile()
    MultiIndexFactory multiIndexFactory = new MultiIndexFactory(pageFile);
    
    BTreeIndexFactory<String, String> indexFactory = new BTreeIndexFactory<String, String>()
    indexFactory.setKeyCodec(StringCodec.INSTANCE)
    
    BTreeIndexFactory<String, String> indexFactory2 = new BTreeIndexFactory<String, String>()
    

    SortedIndex<String, String> writeIndex = multiIndexFactory.openOrCreate("subscriptions", indexFactory)
    SortedIndex<String, String> retainedIndex = multiIndexFactory.openOrCreate("retained", indexFactory2)
    println "Hawt opened, try to read"
    println "Size: ${writeIndex.size()}"
    
    println "write something"
    writeIndex.put("titolo", "capucetto rosso")
    retainedIndex.put("autore", "leggenda popolare")
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
    
    MultiIndexFactory multiIndexFactory = new MultiIndexFactory(pageFile);
    
    IndexFactory<String, String> indexFactory = new BTreeIndexFactory<String, String>()
    BTreeIndexFactory<String, String> indexFactory2 = new BTreeIndexFactory<String, String>()
    indexFactory.setKeyCodec(StringCodec.INSTANCE)
    
    SortedIndex<String, String> readingIndex = multiIndexFactory.openOrCreate("subscriptions", indexFactory)
    SortedIndex<String, String> retainedIndex = multiIndexFactory.openOrCreate("retained", indexFactory2)
    
    println "read something, size ${readingIndex.size()}"
    for (def entry : readingIndex) { 
        println "read k, v -> ${entry.key}, ${entry.value} "
    }
    
    for (def entry : retainedIndex) { 
        println "read k, v -> ${entry.key}, ${entry.value} "
    }
    
    factory.close()
}


writeSomeData(tmpFile)       
readAllFileContents(tmpFile)       





