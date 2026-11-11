package de.meiapps.sternenreise;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/** Stable identities prevent delayed saves from crossing into another player's story. */
public final class PlayerProfiles {
    public static final int MAX_PROFILES=8;
    public record Profile(String id,String name,String state) { }
    private final LinkedHashMap<String,Profile> profiles=new LinkedHashMap<>();
    private String active;
    private PlayerProfiles(){ }
    public Profile active(){return profiles.get(active);}
    public List<Profile> all(){return new ArrayList<>(profiles.values());}
    public boolean select(String id){if(!profiles.containsKey(id))return false;active=id;return true;}
    public String create(String name){if(profiles.size()>=MAX_PROFILES)throw new IllegalArgumentException("Es sind bereits acht Profile vorhanden.");String clean=checkedName(name,null);String id=UUID.randomUUID().toString();profiles.put(id,new Profile(id,clean,new Adventure().save()));active=id;return id;}
    public void rename(String id,String name){Profile current=profiles.get(id);if(current==null)throw new IllegalArgumentException("Profil nicht gefunden.");profiles.put(id,new Profile(id,checkedName(name,id),current.state));}
    public boolean store(String id,String state){Profile current=profiles.get(id);if(current==null)return false;profiles.put(id,new Profile(id,current.name,Adventure.restore(state).save()));return true;}
    public boolean delete(String id){if(profiles.size()<=1||!profiles.containsKey(id))return false;profiles.remove(id);if(id.equals(active))active=profiles.keySet().iterator().next();return true;}
    private String checkedName(String name,String ownId){
        if(name==null)throw new IllegalArgumentException("Bitte einen Namen eingeben.");String clean=name.trim().replaceAll("\\s+"," ");
        if(clean.isEmpty()||clean.codePointCount(0,clean.length())>24)throw new IllegalArgumentException("Bitte einen Namen mit 1 bis 24 Zeichen eingeben.");
        for(Profile profile:profiles.values())if(!profile.id.equals(ownId)&&profile.name.equalsIgnoreCase(clean))throw new IllegalArgumentException("Dieser Name wird schon verwendet.");return clean;
    }
    public String save(){StringBuilder out=new StringBuilder("PROFILES1|").append(active);for(Profile p:profiles.values())out.append('\n').append(p.id).append('|').append(encode(p.name)).append('|').append(encode(p.state));return out.toString();}
    public static PlayerProfiles restore(String data,String legacyState){
        PlayerProfiles result=new PlayerProfiles();
        try{
            if(data!=null&&data.length()<100000&&data.startsWith("PROFILES1|")){
                String[] lines=data.split("\n");String requested=lines[0].substring(10);
                for(int i=1;i<lines.length&&result.profiles.size()<MAX_PROFILES;i++){
                    try{String[] fields=lines[i].split("\\|");if(fields.length!=3||!fields[0].matches("[a-zA-Z0-9-]{1,64}")||result.profiles.containsKey(fields[0]))continue;
                        String name=result.checkedName(decode(fields[1]),null);String state=Adventure.restore(decode(fields[2])).save();result.profiles.put(fields[0],new Profile(fields[0],name,state));
                    }catch(IllegalArgumentException ignored){ }
                }
                if(!result.profiles.isEmpty()){result.active=result.profiles.containsKey(requested)?requested:result.profiles.keySet().iterator().next();return result;}
            }
        }catch(IllegalArgumentException ignored){ }
        result.active="player-1";result.profiles.put(result.active,new Profile(result.active,"Spieler 1",Adventure.restore(legacyState).save()));return result;
    }
    private static String encode(String value){return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));}
    private static String decode(String value){return new String(Base64.getUrlDecoder().decode(value),StandardCharsets.UTF_8);}
}
