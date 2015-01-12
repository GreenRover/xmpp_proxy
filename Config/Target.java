/*
 * Single target item.
 */
package Config;

/**
 *
 * @author henning@mst.ch Heiko Henning
 */
public class Target {
    protected String host;
    protected int port;
    
    public Target(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    public String getHost() {
        return host;
    }

    public Target setHost(String host) {
        this.host = host;
        
        return this;
    }

    public int getPort() {
        return port;
    }

    public Target setPort(int port) {
        this.port = port;
        
        return this;
    }
}
