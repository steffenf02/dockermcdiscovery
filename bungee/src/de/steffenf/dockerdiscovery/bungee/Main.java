package de.steffenf.dockerdiscovery.bungee;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.steffenf.dockerdiscovery.models.QueryResponse;
import de.steffenf.dockerdiscovery.models.Server;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Map;

public class Main extends Plugin implements Listener {

    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerCommand(this, new RefreshCommand());
        getProxy().getPluginManager().registerCommand(this, new CreateCommand());
        getProxy().getPluginManager().registerCommand(this, new DeleteCommand());

        getProxy().getPluginManager().registerListener(this, this);
        refreshServers();
        getLogger().info("ready");
    }

    @EventHandler
    public void onJoin(PostLoginEvent e){
        e.getPlayer().connect((ServerInfo) getProxy().getServers().values().toArray()[0]);
    }

    public void refreshServers(){
        // add new
        QueryResponse resp = getContainers();
        for (Server server : resp.servers) {
            if(!getProxy().getServers().containsKey(server.name)){
                getProxy().getServers().put(server.name, getProxy().constructServerInfo(server.name, InetSocketAddress.createUnresolved("127.0.0.1", server.port), "", false));
            }
        }
        // remove old

        ArrayList<String> serverStrings = new ArrayList<>();
        for (Server server : resp.servers) {
            serverStrings.add(server.name);
        }
        ArrayList<String> serversToDelete = new ArrayList<>();
        for (Map.Entry<String, ServerInfo> server : getProxy().getServers().entrySet()) {
            if(!serverStrings.contains(server.getKey())){
                serversToDelete.add(server.getKey());
            }
        }
        serversToDelete.forEach(s ->{
            getProxy().getServers().remove(s);
        });
    }

    public class DeleteCommand extends Command{

        public DeleteCommand() {
            super("delete");
        }

        @Override
        public void execute(CommandSender commandSender, String[] args) {
            if(args.length > 0){
                ArrayList<Server> servers = getContainers().servers;
                if(args[0].equalsIgnoreCase("current")){
                    if(!(commandSender instanceof ProxiedPlayer)){
                        commandSender.sendMessage("duh");
                    }else{
                        commandSender.sendMessage("aight");
                        for(Server s : servers){
                            if(((ProxiedPlayer) commandSender).getServer().getInfo().getName().equalsIgnoreCase(s.name)){
                                deleteServer(s.id);
                                refreshServers();
                                commandSender.sendMessage(String.format("command sent to %s/%s",s.name , s.id));
                                return;
                            }
                        }
                    }
                }else{
                    for(Server s : servers){
                        if(s.name.equalsIgnoreCase(args[0])){
                            commandSender.sendMessage("deleting " + args[0]);
                            deleteServer(s.id);
                            refreshServers();
                            commandSender.sendMessage(String.format("command sent to %s/%s",s.name , s.id));
                            return;
                        }
                    }
                    commandSender.sendMessage("not found");
                }
            }else{
                commandSender.sendMessage("usage: /delete current|name");
            }
        }
    }

    public class CreateCommand extends Command{

        public CreateCommand() {
            super("create");
        }

        @Override
        public void execute(CommandSender commandSender, String[] strings) {
            createServer();
            commandSender.sendMessage("created server. refreshing...");
            refreshServers();
            commandSender.sendMessage("should be done i guess");
        }
    }

    public class RefreshCommand extends Command{

        public RefreshCommand() {
            super("refresh");
        }

        @Override
        public void execute(CommandSender commandSender, String[] strings) {
            refreshServers();
            commandSender.sendMessage(String.format("refreshed %s servers", getProxy().getServers().size()));
        }

    }

    public static QueryResponse getContainers() {
        HttpClient client = HttpClient.newBuilder().build();
        try {
            HttpResponse<String> resp = client.send(HttpRequest.newBuilder().GET().uri(new URI("http://localhost/get")).build(), HttpResponse.BodyHandlers.ofString());
            String body = resp.body();
            return gson.fromJson(body, QueryResponse.class);
        } catch (IOException | URISyntaxException | InterruptedException e) {
            e.printStackTrace();
        }
        return new QueryResponse();
    }

    public static void createServer() {
        HttpClient client = HttpClient.newBuilder().build();
        try {
            HttpResponse<String> resp = client.send(HttpRequest.newBuilder().GET().uri(new URI("http://localhost/create")).build(), HttpResponse.BodyHandlers.ofString());
            return;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void deleteServer(String id){
        HttpClient client = HttpClient.newBuilder().build();
        try {
            HttpResponse<String> resp = client.send(HttpRequest.newBuilder().GET().uri(new URI("http://localhost/delete?id=" + id)).build(), HttpResponse.BodyHandlers.ofString());
            return;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
