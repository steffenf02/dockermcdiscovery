package de.steffenf.dockerdiscovery.models;

public class Server {

    public int port;
    public String name;
    public String status;
    public String id;
    public String image;


    public Server(int port, String name, String status, String id, String image) {
        this.port = port;
        this.name = name;
        this.status = status;
        this.id = id;
        this.image = image;
    }
}
