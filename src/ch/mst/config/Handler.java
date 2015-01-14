/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package src.ch.mst.config;

import ch.mst.xmpp_reverse_proxy;
import ch.mst.config.Target;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.ini4j.Ini;


/**
 *
 * @author Heiko Henning <hennings@mst.ch>
 */
public class Handler {
    
    protected File config_file;
    
    protected static HashMap<String, Target> domains = new HashMap<String, Target>();
    
    protected int default_port;
    
    /**
     * last modify timestap of config file to monitor for changes.
     */
    protected long last_modified = 0;
    
    public Handler(String config_file ,int default_port) {
        this.default_port = default_port;
        this.config_file = new File(config_file);
    }
    
    /**
     * Parse the config file and update domain mapping array.
     * @throws java.io.IOException
     */
    public void parseConfig() throws IOException {
        Ini ini = new Ini(this.config_file);
        Ini.Section domains_section = ini.get("domains");
        
        Handler.domains.clear();
        
        for (Map.Entry<String, String> entry : domains_section.entrySet()) {
            String domain_name= entry.getKey().toLowerCase();
            String target_string = entry.getValue();
            
            Target target_object = new Target(
                    target_string.toLowerCase(),
                    this.default_port
                );
            
            // Test if port was given.
            String[] target_parts = target_string.split(":");
            
            if (target_parts.length == 2) {
                target_object = new Target(
                        target_parts[0].toLowerCase(),
                        Integer.parseInt(target_parts[1]) 
                    );
            }
            
            // Add traget to hashmap.
            Handler.domains.put(
                domain_name,
                target_object    
            );
            
            xmpp_reverse_proxy.log(Level.INFO, "Add target " + domain_name + " = " + target_object);
        }
        
        this.last_modified = this.config_file.lastModified();
    }
    
    /**
     * Monitor config file for changes.
     */
    public void monitorConfig() {
        
        final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

        final Handler handler = this;
        
        Runnable processDataCmd = new Runnable() {
            @Override
            public void run() {
                long new_last_modified = handler.config_file.lastModified();

                if (handler.last_modified != new_last_modified) {

                    // Parse config if ini file has changed.
                    try {
                        handler.parseConfig();
                    } catch (Exception exp) {
                        // oops, something went wrong
                        xmpp_reverse_proxy.log(Level.SEVERE, "Unable  to parse config " + exp);
                    }
                }

                handler.last_modified = new_last_modified;
            }
        };

        // Check config all 3 sec
        service.scheduleAtFixedRate(processDataCmd, 0, 3, TimeUnit.SECONDS);
    }
    
    /**
     * Get target by domain.
     * 
     * @param domain
     * @return 
     */
    public static Target resolveDomain(String domain) {
        return Handler.domains.get(domain);
    }
}
