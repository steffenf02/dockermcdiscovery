package de.steffenf.dockerdiscovery.web;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.iki.elonen.NanoHTTPD;
import de.steffenf.dockerdiscovery.models.QueryResponse;
import de.steffenf.dockerdiscovery.models.Server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class APIServer extends NanoHTTPD {

    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public APIServer(int port) throws IOException {
        super(port);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, true);
        System.out.println("API Running.");
    }

    @Override
    public Response serve(IHTTPSession session) {
        System.out.println(String.format("Received %s on %s by %s", session.getMethod().toString(), session.getUri(), session.getRemoteIpAddress()));
        if(session.getUri().equals("/create")){
            CreateContainerResponse resp = Main.dockerClient.createContainerCmd("minecraft/vanilla")
                    .withHostConfig(HostConfig.newHostConfig()
                            //.withPortBindings(PortBinding.parse("0.0.0.0:25566:25565"))
                            .withCpuCount(1L)
                            .withPublishAllPorts(true)
                            //.withNetworkMode("host")
                    )
                    //.withAttachStdin(true)
                    .withExposedPorts(ExposedPort.tcp(25565))
                    .withTty(true)
                    .exec();
            System.out.println(resp.getId());
            Main.dockerClient.startContainerCmd(resp.getId()).exec();
            return newFixedLengthResponse("create success");
        }else if(session.getUri().equalsIgnoreCase("/get")){
            QueryResponse queryResponse = new QueryResponse();
            ArrayList<Server> servers = new ArrayList<>();
            for(Container c : Main.dockerClient.listContainersCmd().withShowAll(true).exec()){
                if(c.ports != null && c.ports.length > 0 && c.ports[0] != null)
                servers.add(new Server(c.ports[0].getPublicPort(), c.getNames()[0], c.getState(), c.getId(), c.getImage()));
            }
            queryResponse.servers = servers;
            return newFixedLengthResponse(gson.toJson(queryResponse));
        }else if(session.getUri().equalsIgnoreCase("/nuke")){
            Main.dockerClient.listContainersCmd().withShowAll(true).exec().forEach(c -> {
                System.out.println(c.getState() + " " + c.getStatus());
                System.out.println("Deleting: " + Arrays.toString(c.getNames()));
                if(c.getState().equalsIgnoreCase("running")){
                    System.out.println("Stopping first...");
                    Main.dockerClient.stopContainerCmd(c.getId()).exec();
                }
                Main.dockerClient.removeContainerCmd(c.getId()).withRemoveVolumes(true).exec();
            });
            return newFixedLengthResponse("nuke success");
        }else if(session.getUri().equalsIgnoreCase("/delete")){
            if(session.getParameters().containsKey("id")){
                Main.dockerClient.listContainersCmd().withShowAll(true).exec().forEach(c -> {
                    if(session.getParameters().get("id").get(0).equals(c.getId())){
                        System.out.println(c.getState() + " " + c.getStatus());
                        System.out.println("Deleting: " + Arrays.toString(c.getNames()));
                        if(c.getState().equalsIgnoreCase("running")){
                            System.out.println("Stopping first...");
                            Main.dockerClient.stopContainerCmd(c.getId()).exec();
                        }
                        Main.dockerClient.removeContainerCmd(c.getId()).withRemoveVolumes(true).exec();
                    }
                });
                newFixedLengthResponse("delete success");
            }else{
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/html", "no id supplied");
            }
        }

        return newFixedLengthResponse("hello");
    }
}
