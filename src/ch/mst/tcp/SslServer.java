/*
 * Tcp server
 */
package ch.mst.tcp;

import ch.mst.xmpp_reverse_proxy;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.logging.Level;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

/**
 *
 * @author henning@mst.ch Heiko Henning
 */


public final class SslServer extends Server {

    public SslServer(int port, String pem_file) throws IOException {
        super(port);
        
        this.initJavaKeyStore(pem_file);
    }
    
    private void openServerSocket() {
        try {
            SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            sslserversocketfactory.getSupportedCipherSuites();
            
            this.serverSocket = (SSLServerSocket) sslserversocketfactory.createServerSocket(this.serverPort);

            
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port " + this.serverPort, e);
        }
        
        xmpp_reverse_proxy.log(Level.INFO, "Start listening on port: " + Integer.toString(this.serverPort)); 
    }
    
    /**
     * Init java keystore for sll server.
     * 
     * @param pem_file
     *    Path to apache *.pem file.
     * 
     * @throws IOException 
     */
    protected void initJavaKeyStore(String pem_file) throws IOException {
        // Throw exception if pem file dosent exist.
        File pem = new File(pem_file);
        
        // Generate random keystore password.
        String key_store = "/tmp/xmpp_proxy.keystore";
        String key_stor_pw = new BigInteger(130, new SecureRandom()).toString(32);
        
        String pks12_file = "/tmp/xmpp_proxy.p12";
        
        // Create p12 from pem.
        Runtime.getRuntime().exec(
                "openssl pkcs12 -export " + 
                "-in " + pem.getAbsolutePath() + " " + 
                "-inkey " + pem.getAbsolutePath() + " " + 
                "-out " + pks12_file + " " +
                "-passsout pas:" + key_stor_pw + " " + 
                "-name xmpp_server"
            );

        // Create java key store.
        Runtime.getRuntime().exec(
                "keytool -importkeystore " + 
                "-deststorepass " + key_stor_pw + "  " + 
                "-destkeystore " + key_store + " " + 
                "-srckeystore " + pks12_file + " " + 
                "-srcstoretype PKCS12 " + 
                "-srcstorepass " + key_stor_pw + " " +
                "-alias xmpp_server"
            );
        
        // Clean up
        File file = new File(pks12_file);
        file.delete();
        
        // Tel system to use the new keystore.
        System.setProperty("javax.net.ssl.keyStore", key_store);
        System.setProperty("javax.net.ssl.keyStorePassword", key_stor_pw);
    }
    
}