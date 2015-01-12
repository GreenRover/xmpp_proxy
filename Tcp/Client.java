/*
 * Handle single clients.
 */
package Tcp;

import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author henning@mst.ch Heiko Henning
 */
public class Client implements Runnable {
    
    protected Socket clientSocket;
    
    public Client(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }
    
    @Override
    public void run() {
                
        InputStream  input  = this.clientSocket.getInputStream();
        OutputStream output = this.clientSocket.getOutputStream();
        
        // <?xml version='1.0' ?><stream:stream to='portal-testing2' xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' version='1.0'>
        
        
        xmpp_reverse_proxy.resolveDomain(to);
    }
    
}
