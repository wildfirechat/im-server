@Grab(group='org.eclipse.jetty.aggregate', module='jetty-server', version='8.1.16.v20140903')
@Grab(group='javax.servlet', module='javax.servlet-api', version='3.0.1')

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.server.handler.DefaultHandler
import org.eclipse.jetty.server.Handler

Server server = new Server();
SelectChannelConnector connector = new SelectChannelConnector();
connector.setPort(8080);
server.addConnector(connector);

ResourceHandler resource_handler = new ResourceHandler();
resource_handler.setDirectoriesListed(true);
//resource_handler.setWelcomeFiles(["index.html"] as new String[0]);

resource_handler.setResourceBase(".");

HandlerList handlers = new HandlerList();
handlers.addHandler(resource_handler)
handlers.addHandler(new DefaultHandler());
server.setHandler(handlers);

server.start();
println "Local server started"

server.join();