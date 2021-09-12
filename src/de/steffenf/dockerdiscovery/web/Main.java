package de.steffenf.dockerdiscovery.web;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.io.IOException;

public class Main {

    public static DockerClient dockerClient = null;

    public static void main(String[] args) throws IOException {
        new APIServer(80);
        DockerClientConfig cfg = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("npipe:////./pipe/docker_engine") // windows only
                .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(cfg.getDockerHost())
                .build();
        dockerClient = DockerClientImpl.getInstance(cfg, httpClient);
        dockerClient.pingCmd().exec();




        System.out.println("ready and running!");
        while(true){ // horrible way to do this but makes the program not exit
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
