package me.sihang.backend.domain;

public class ConfigPath {

    private String server_url;
    private String datapath;

    public ConfigPath(){}
    public ConfigPath(String server_url ,String datapath){
        this.server_url = server_url;
        this.datapath = datapath;
    }

    public String getServer_url(){
        return server_url;
    }


    public String getDatapath(){
        return datapath;
    }
}
