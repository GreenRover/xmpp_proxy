package ch.mst;

/*
 * xmpp reverse proxy class.
 */

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.ini4j.Ini;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.agafua.syslog.SyslogHandler;

import ch.mst.config.Target;
import ch.mst.tcp.Server;
import java.util.Map;


/**
 *
 * @author henning@mst.ch Heiko Henning
 */
public class xmpp_reverse_proxy {
    
    // The list of domains parsed form config.
    protected static HashMap<String, Target> domains = new HashMap<String, Target>();
    protected static String config_file;
    protected static int port = 5222;
    protected static int ssl_port;
    
    public final static Logger LOGGER = Logger.getLogger("mst.xmpp");

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {
            // parse the command line arguments
            CommandLine cli = getCliArgs(args);
            
            xmpp_reverse_proxy.port = Integer.parseInt(cli.getOptionValue("port"));
            if (cli.hasOption("ssl_port")) {
                xmpp_reverse_proxy.ssl_port = Integer.parseInt(cli.getOptionValue("ssl_port"));
            }
            xmpp_reverse_proxy.config_file = cli.getOptionValue("config");
        }
        catch(ParseException exp) {
            // oops, something went wrong
            System.err.println("Missing argument.  Reason: " + exp.getMessage());
            System.exit(0);
        }
        
        try {
            parseConfig();
        } catch (Exception exp) {
            // oops, something went wrong
            System.err.println("Unable to parse config: " + exp);
            System.exit(0);
        }
        
        // Add syslog support to logger instance.
        xmpp_reverse_proxy.LOGGER.addHandler(new SyslogHandler());
        
        Server server = new Server(port);
        new Thread(server).start();
    }
    
    /**
     * Parse configuration file to domains hashmap.
     * 
     * @throws IOException 
     */
    protected static void parseConfig() throws IOException {
        Ini ini = new Ini(new File(xmpp_reverse_proxy.config_file));
        Ini.Section domains_section = ini.get("domains");
        
        

        xmpp_reverse_proxy.domains.clear();
        
        for (Map.Entry<String, String> entry : domains_section.entrySet()) {
            String domain_name= entry.getKey().toLowerCase();
            String target_string = entry.getValue();
            
            Target target_object = new Target(
                    target_string.toLowerCase(),
                    xmpp_reverse_proxy.port
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
            xmpp_reverse_proxy.domains.put(
                domain_name,
                target_object    
            );
            
            xmpp_reverse_proxy.LOGGER.log(Level.INFO, "Add target: " + domain_name + " = " + target_object);
        }

    }
    
    /**
     * Parse cli options.
     * 
     * @param args
     * @return
     */
    protected static CommandLine getCliArgs(String[] args) throws ParseException {
        
        Option port_option = OptionBuilder.withArgName("port")
                                .hasArg()
                                .withDescription("The port to listen on")
                                .isRequired()
                                .create("port");
        
        Option ssl_port_option = OptionBuilder.withArgName("ssl_port")
                                .hasArg()
                                .withDescription("The ssl port to listen on")
                                .create("ssl_port");
        
        Option config_file_option = OptionBuilder.withArgName("config")
                                .hasArg()
                                .withDescription("Configuration file")
                                .isRequired()
                                .create("config");
        
        // create Options object
        Options options = new Options();

        options.addOption(port_option);
        options.addOption(ssl_port_option);
        options.addOption(config_file_option);
        
        
        // create the parser
        CommandLineParser parser = new PosixParser();

        // parse the command line arguments
        CommandLine cli = parser.parse(options, args);
        
        return cli;
    }
    
    /**
     * Get target by domain.
     * 
     * @param domain
     * @return 
     */
    public static Target resolveDomain(String domain) {
        return xmpp_reverse_proxy.domains.get(domain);
    }
    
}
