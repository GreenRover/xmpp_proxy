/*
 * Handle single clients.
 */
package ch.mst.tcp;


import java.io.IOException;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import ch.mst.xmpp_reverse_proxy;
import ch.mst.config.Target;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author henning@mst.ch Heiko Henning
 */
public class ServerInstance implements Runnable {
    
    /**
     * The input listener on which we wait for incoming from client data.
     */
    private InputStream serverInStream = null;
    /**
     * The output stream to write date to client.
     */
    private OutputStream serverOutStream = null;
    
    /**
     * The input listener for data fro target system.
     */
    private InputStream targetInStream = null;
    /**
     * The output stream to write data to target system.
     */
    private OutputStream targetOutStream = null;
    
    /**
     * The buffer where the domain check bytes are written.
     * Because 130 is the solution of all problems.
     */
    private byte[] check_buffer = new byte[130];
    
    
    /**
     * Target host and port to forward to.
     */
    Target target;
    
    public ServerInstance(Socket serverInstanceSocket) throws IOException {
        this.serverInStream = serverInstanceSocket.getInputStream();
        this.serverOutStream = serverInstanceSocket.getOutputStream();
    }
    
    @Override
    public void run() {
        try {        
            if (!this.detectTargetSystem()) {
                // Failed to detect and connect to target system.

                this.serverInStream.close();
                this.serverOutStream.close();
                
                this.targetInStream.close();
                this.targetOutStream.close();

                return;
            }
            
            // Forward the traffic.
            new Thread( new CopyStream(serverInStream, targetOutStream) {
                @Override
                public void onError(IOException ex) {
                    try { serverInStream.close();      } catch (IOException e) { /* ignore */ }
                    try { serverOutStream.close();     } catch (IOException e) { /* ignore */ }
                    try { targetInStream.close();      } catch (IOException e) { /* ignore */ }
                    try { targetOutStream.close();     } catch (IOException e) { /* ignore */ }
                }
            }).start();

            new Thread( new CopyStream(targetInStream, serverOutStream) {
                @Override
                public void onError(IOException ex) {
                    try { serverInStream.close();      } catch (IOException e) { /* ignore */ }
                    try { serverOutStream.close();     } catch (IOException e) { /* ignore */ }
                    try { targetInStream.close();      } catch (IOException e) { /* ignore */ }
                    try { targetOutStream.close();     } catch (IOException e) { /* ignore */ }
                }
            }).start();
            
        } catch (IOException ex) {}

    }
    
    /**
     * Detect target system by domain.
     * <?xml version='1.0' ?><stream:stream to='portal-testing2' xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' version='1.0'>
     *   
     * @return 
     */
    protected boolean detectTargetSystem() {
        try {
            // Read some bytes.
            this.serverInStream.read(check_buffer, 0, check_buffer.length);

            // Get the string back from the read bytes.
            String str = new String(check_buffer, "UTF-8");
            
            // Patttern expect to find.
            Pattern pattern = Pattern.compile("\\<stream:stream[^>]*to=[\\\"']?([\\w\\-]+)[\\\"']?");
            
            //  Look up buffer by regex.
            Matcher match = pattern.matcher(str);
            
            if(match.matches()) {
                // Buffer match regex.
                String to = match.group(1);
                
                // Resolve domain to taget host.
                this.target = xmpp_reverse_proxy.resolveDomain(to);
                
                if (this.target == null) {
                    returnXmppError(to, "Unable to resolve domain for " + to);
                    return false;
                }
                
                try {
                    // Connect to target system.
                    this.connectoToTarget(this.target);
                    
                    // Write bufferd data to target system.
                    this.targetOutStream.write(check_buffer);
                    
                    // Free memory
                    check_buffer = null;
                } catch (Exception ex) {
                    returnXmppError(to, "Unable to connect to target server");
                    return false;
                }
                
            } else {
                returnXmppError("xmpp-proxy", "Didt not received any headers.");
                return false;
            }
        } catch (IOException ex) {
            if (!ex.toString().contains("socket close")) {
                returnXmppError("xmpp-proxy", ex.getMessage());
                return false;
            }
        }

        return true;
    }
    
    /**
     * Return an xmpp error and log issue.
     * 
     * @param from
     * @param message 
     */
    protected void returnXmppError(String from, String message) {
        
        xmpp_reverse_proxy.LOGGER.log(Level.WARNING, message);
        
        String response = "<?xml version=\"1.0\"?>" + 
                "<stream:stream xmlns=\"jabber:client\" xmlns:stream=\"http://etherx.jabber.org/streams\" id=\"1337\" from=\"" + from + "\" version=\"1.0\">" + 
                "<stream:error>" + 
                    "<policy-violation xmlns=\"urn:ietf:params:xml:ns:xmpp-streams\"/>" + 
                        "<text xml:lang=\"\" xmlns=\"urn:ietf:params:xml:ns:xmpp-streams\">" + message + "</text>" + 
                "</stream:error>";
        
        try {
            this.serverOutStream.write(response.getBytes("UTF-8"));
        } catch (Exception ex) {
            Logger.getLogger(ServerInstance.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * create connection to target host.
     * 
     * @param target
     * @throws Exception 
     */
    protected void connectoToTarget(Target target) throws Exception {
        Socket targetSocket = new Socket(target.getHost(), target.getPort());
        
        this.targetInStream = targetSocket.getInputStream();
        this.targetOutStream = targetSocket.getOutputStream();
    }
      
}
