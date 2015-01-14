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

import org.productivity.java.syslog4j.Syslog;
import org.productivity.java.syslog4j.SyslogIF;

import ch.mst.config.Target;
import ch.mst.tcp.Server;
import ch.mst.tcp.SslServer;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import src.ch.mst.config.Handler;

;


/**
 *
 * @author henning@mst.ch Heiko Henning
 */
public class xmpp_reverse_proxy {
    
    // The list of domains parsed form config.
    protected static String config_file;
    protected static int port = 5222;
    protected static int ssl_port = 0;
    protected static String ssl_cert = "";
    
    public final static Logger CONSOLE_LOGGER = Logger.getLogger("mst.xmpp");
    public final static SyslogIF SYSLOG_LOGGER = Syslog.getInstance("udp");

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
            if (cli.hasOption("ssl_cert")) {
                xmpp_reverse_proxy.ssl_cert = cli.getOptionValue("ssl_cert");
            }
            
            xmpp_reverse_proxy.config_file = cli.getOptionValue("config");
        }
        catch(ParseException exp) {
            // oops, something went wrong
            System.err.println("Missing argument.  Reason: " + exp.getMessage());
            System.exit(0);
        }
        
        try {
            Handler config_handler = new Handler(
                xmpp_reverse_proxy.config_file,
                xmpp_reverse_proxy.port
            );
            config_handler.parseConfig();
            config_handler.monitorConfig();

        } catch (Exception exp) {
            // oops, something went wrong
            System.err.println("Unable to parse config: " + exp);
            System.exit(0);
        }
        
        SYSLOG_LOGGER.getConfig().setIdent("xmpp proxy");
        SYSLOG_LOGGER.getConfig().setFacility("LOCAL6");
        
        // Start tcp server.
        Server server = new Server(xmpp_reverse_proxy.port);
        new Thread(server).start();
        
        // Start ssl server if configured.
        if (!xmpp_reverse_proxy.ssl_cert.isEmpty() && xmpp_reverse_proxy.ssl_port > 0) {
            try {
                Server ssl_server = new SslServer(xmpp_reverse_proxy.ssl_port, xmpp_reverse_proxy.ssl_cert);
                new Thread(ssl_server).start();
            } catch (IOException ex) {
                System.err.println("Unable to start ssl server.  Reason: " + ex.getMessage());
            } 
        }
    }
    
    /**
     * Parse configuration file to domains hashmap.
     * 
     * @throws IOException 
     */
    protected static void parseConfig() throws IOException {


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
        
        Option ssl_cert_option = OptionBuilder.withArgName("ssl_cert")
                                .hasArg()
                                .withDescription("Path to pem file")
                                .create("ssl_cert");
        
        Option config_file_option = OptionBuilder.withArgName("config")
                                .hasArg()
                                .withDescription("Configuration file")
                                .isRequired()
                                .create("config");
        
        // create Options object
        Options options = new Options();

        options.addOption(port_option);
        options.addOption(ssl_port_option);
        options.addOption(ssl_cert_option);
        options.addOption(config_file_option);
        
        
        // create the parser
        CommandLineParser parser = new PosixParser();

        // parse the command line arguments
        CommandLine cli = parser.parse(options, args);
        
        return cli;
    }
    

    
    /**
     * Log message to console and syslog.
     * 
     * @param level
     * @param message 
     */
    public static void log(Level level, String message) {
        xmpp_reverse_proxy.CONSOLE_LOGGER.log(level, message);
        
        if (level == Level.SEVERE) {
            xmpp_reverse_proxy.SYSLOG_LOGGER.critical(message);
        } else if (level == Level.WARNING) {
            xmpp_reverse_proxy.SYSLOG_LOGGER.warn(message);
        } else {
            xmpp_reverse_proxy.SYSLOG_LOGGER.info(message);
        }
    }
    
}
