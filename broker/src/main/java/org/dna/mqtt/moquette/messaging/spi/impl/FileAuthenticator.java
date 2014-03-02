package org.dna.mqtt.moquette.messaging.spi.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import org.dna.mqtt.moquette.server.IAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
public class FileAuthenticator implements IAuthenticator {

    private static final Logger LOG = LoggerFactory.getLogger(FileAuthenticator.class);
    
    private Map<String, String> m_identities = new HashMap<String, String>();
    
    FileAuthenticator(String filePath) {
        LOG.info("Loading password file: " + filePath);
        File file = new File(filePath);
        try {
            FileReader reader = new FileReader(file);
            parse(reader);
        } catch (FileNotFoundException fex) {
            LOG.warn(String.format("Parsing not existing file %s", filePath), fex);
        } catch (ParseException pex) {
            LOG.warn(String.format("Fromat ero in parsing password file %s", filePath), pex);
        }
    }
    
    private void parse(Reader reader) throws ParseException {
        if (reader == null) {
            return;
        }
        
        BufferedReader br = new BufferedReader(reader);
        String line;
        try {
            while ((line = br.readLine()) != null) {
                int commentMarker = line.indexOf('#');
                if (commentMarker != -1) {
                    if (commentMarker == 0) {
                        //skip its a comment
                        continue;
                    } else {
                        //it's a malformed comment
                        throw new ParseException(line, commentMarker);
                    }
                } else {
                    if (line.isEmpty() || line.matches("^\\s*$")) {
                        //skip it's a black line
                        continue;
                    }
                    
                    //split till the first space
                    int deilimiterIdx = line.indexOf(':');
                    String username = line.substring(0, deilimiterIdx).trim();
                    String password = line.substring(deilimiterIdx + 1).trim();
                    
                    m_identities.put(username, password);
                }
            }
        } catch (IOException ex) {
            throw new ParseException("Failed to read", 1);
        }
    }
    
    public boolean checkValid(String username, String password) {
        String foundPwq = m_identities.get(username);
        if (foundPwq == null) {
            return false;
        }
        
        return foundPwq.equals(password);
    }
    
}
