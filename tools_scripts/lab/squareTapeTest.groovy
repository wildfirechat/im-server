@Grab(group='com.squareup', module='tape', version='1.2.3')
import com.squareup.tape.QueueFile

byte[] content = UUID.randomUUID().toString().bytes//[0x0C, 0x0A, 0x0F, 0x0E, 0x0B, 0x0A, 0x0B, 0x0E] as byte[]

numMessages = 10000
def file = File.createTempFile("tape", ".bin")
println "Created tmp file"
QueueFile queue = new QueueFile(file)
println "Created queue and starting"
long start = System.currentTimeMillis()
(1..numMessages).each {
    queue.add(content)
}
long stop = System.currentTimeMillis()
println "Wrote ${numMessages*content.length} bytes in ${stop-start} ms"