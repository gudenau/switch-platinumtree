package net.gudenau.nx.ptree;

class Favorite{
    private String name;
    private String drive;
    private String path;

    Favorite(String name, String drive, String path){
        this.name = name;
        this.drive = drive;
        this.path = path;
    }

    String getName(){
        return name;
    }

    String getDrive(){
        return drive;
    }

    String getPath(){
        return path;
    }

    void set(String name, String drive, String path){
        this.name = name;
        this.drive = drive;
        this.path = path;
    }
}
