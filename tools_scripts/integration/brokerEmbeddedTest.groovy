@GrabResolver(name='moquette-bintray', root='http://dl.bintray.com/andsel/maven/')
@Grab(group='io.moquette', module='moquette-broker', version='0.12.1-SNAPSHOT')

import io.moquette.server.Server

println "Starting broker in embedded mode"
Server server = new Server()
//Properties props = [port: 1883, host: "0.0.0.0", 'password_file': '../broker/config/password_file.conf'] as Properties
Properties props = new Properties()
props.setProperty('port', '1883')
props.setProperty('host', "0.0.0.0")
props.setProperty('password_file', '../broker/config/password_file.conf')
println "starting proprs $props"
server.startServer(props)
println "Stopping broker.."
server.stopServer()
