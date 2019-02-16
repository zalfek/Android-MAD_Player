package marmiss.aleksejs.mad_player;

public class Song {
    private String name;
    private String artist;
    private String url;


    Song(String name, String artist, String url){
        this.name = name;
        this.artist = artist;
        this.url = url;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl(){
        return url;
    }
    public String getArtist(){
        return artist;
    }
    public String getName(){
        return name;
    }
}
