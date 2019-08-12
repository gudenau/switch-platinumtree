package net.gudenau.nx.ptree;

public class Favorite{
    private final String name;
    private final String drive;
    private final String path;

    public Favorite(String name, String drive, String path){
        this.name = name;
        this.drive = drive;
        this.path = path;
    }

    public String getName(){
        return name;
    }

    public String getDrive(){
        return drive;
    }

    public String getPath(){
        return path;
    }
}
