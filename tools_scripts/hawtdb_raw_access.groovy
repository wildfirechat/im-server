@Grab(group='org.fusesource.hawtdb', module='hawtdb', version='1.6')

import org.fusesource.hawtbuf.codec.StringCodec
import org.fusesource.hawtdb.api.BTreeIndexFactory
import org.fusesource.hawtdb.api.PageFile
import org.fusesource.hawtdb.api.PageFileFactory
import org.fusesource.hawtdb.api.SortedIndex
import org.fusesource.hawtbuf.Buffer



File tmpFile = new File(System.getProperty("user.home") + File.separator + "test_hawtdb.dat")
//tmpFile.createNewFile()

def writeSomeData(tmpFile) {
    def factory = new PageFileFactory()
    factory.file = tmpFile
    factory.open()
    def pageFile = factory.getPageFile()
    int pageIdx = pageFile.alloc()
    pageFile.write(pageIdx, new Buffer("Test".getBytes()))
    println "writed at page index ${pageIdx}"
    pageFile.flush()   
    pageFile.close()
}
 

def readTest(tmpFile) {
    def factory = new PageFileFactory()
    factory.file = tmpFile
    factory.open()
    def pageFile = factory.getPageFile()
    println "page size is: " + pageFile.pageSize
    Buffer b = new Buffer(512)
    pageFile.read(0, b)
    println "content try read " + b 
    
    pageFile.close()
}


writeSomeData(tmpFile)       
readTest(tmpFile)       


