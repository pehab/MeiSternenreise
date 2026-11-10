package de.meiapps.sternenreise;

/** Deterministic story state. Android and renderer only present these actions. */
public final class Adventure {
    public enum Room { WORKSHOP, GARDEN, OBSERVATORY }
    private Room room = Room.WORKSHOP;
    private boolean key, seed, planted, water, crystal, lens, finished;
    private int symbols;
    public Room room() { return room; }
    public boolean hasKey() { return key; }
    public boolean hasSeed() { return seed; }
    public boolean planted() { return planted; }
    public boolean watered() { return water; }
    public boolean hasCrystal() { return crystal; }
    public boolean hasLens() { return lens; }
    public boolean finished() { return finished; }
    public int symbols() { return symbols; }
    public String title() { return switch(room) {
        case WORKSHOP -> "Die Sternenwerkstatt";
        case GARDEN -> "Der Mondgarten";
        case OBSERVATORY -> "Die Sternwarte";
    }; }
    public String hint() {
        if (finished) return "Du hast das Sternenlicht zurückgebracht! Danke, Sternenfreund!";
        return switch(room) {
            case WORKSHOP -> !key ? "Ich bin Lumi! Suche den goldenen Schlüssel im Regal." : !seed ? "In der kleinen Truhe wartet ein Mondensamen." : "Schau die Tür zum Garten an. Dort braucht dein Samen Wasser.";
            case GARDEN -> !water ? "Pflanze den Samen in den Topf. Dann schau die Gießkanne an." : !crystal ? "Die Mondblume leuchtet! Nimm ihren blauen Kristall." : "Der Kristall öffnet die Tür zur Sternwarte.";
            case OBSERVATORY -> !lens ? "Am Pult gilt die Reihenfolge: Stern, Mond, Sonne. Schau die passenden Symbole an." : "Setze die Linse und den Kristall in die Sternenmaschine!";
        };
    }
    public String act(String id) {
        if ("hint".equals(id)) return hint();
        if (room == Room.WORKSHOP) {
            switch(id) {
                case "key": key = true; return "Ein goldener Schlüssel! Damit kannst du die kleine Truhe öffnen.";
                case "chest": if (!key) return "Die Truhe ist zu. Lumi meint, im Regal liegt ein Schlüssel.";
                    seed = true; return "Du hast einen Mondensamen gefunden! Lass ihn im Mondgarten wachsen.";
                case "garden": if (!seed) return "Wir brauchen erst den Mondensamen aus der Truhe.";
                    room = Room.GARDEN; return hint();
            }
        } else if (room == Room.GARDEN) {
            switch(id) {
                case "back": room = Room.WORKSHOP; return hint();
                case "pot": planted = true; return water ? "Deine Mondblume ist wunderschön gewachsen." : "Du legst den Samen in die weiche Erde. Jetzt fehlt Wasser!";
                case "water": if (!planted) return "Lege zuerst den Mondensamen in den Topf. Schau dafür den Topf an.";
                    water = true; return "Plitsch, platsch! Der Samen wächst zu einer leuchtenden Mondblume. Schau sie an!";
                case "flower": if (!water) return "Hier kann eine Mondblume wachsen. Schau erst die Gießkanne an.";
                    crystal = true; return "Die Blume schenkt dir einen Sternenkristall. Die Tür zur Sternwarte leuchtet!";
                case "observatory": if (!crystal) return "Die Tür braucht das Licht eines Sternenkristalls.";
                    room = Room.OBSERVATORY; return hint();
            }
        } else {
            switch(id) {
                case "back": room = Room.GARDEN; return hint();
                case "star": case "moon": case "sun":
                    if (lens) return "Du hast die Sternenlinse schon gefunden. Schau die große Maschine an.";
                    String expected = new String[]{"star", "moon", "sun"}[symbols];
                    if (!id.equals(expected)) { symbols = "star".equals(id) ? 1 : 0; return "Versuch es noch einmal: Stern, Mond, Sonne."; }
                    symbols++;
                    if (symbols == 3) { lens = true; return "Richtig! Die Sternenlinse gleitet in deine Tasche. Jetzt zur Maschine!"; }
                    return symbols == 1 ? "Der Stern leuchtet. Jetzt der Mond!" : "Der Mond leuchtet. Jetzt die Sonne!";
                case "machine": if (!lens) return "Der Kristall ist da. Löse noch das Rätsel am Symbolpult, um die Linse zu bekommen.";
                    finished = true; return hint();
            }
        }
        return hint();
    }
    public String save() { return room.name()+","+key+","+seed+","+planted+","+water+","+crystal+","+lens+","+finished+","+symbols; }
    public static Adventure restore(String data) {
        Adventure a = new Adventure();
        try {
            String[] p = data.split(",");
            if (p.length != 9) return a;
            Room r = Room.valueOf(p[0]); int n = Integer.parseInt(p[8]);
            for (int i=1;i<8;i++) if (!p[i].equals("true") && !p[i].equals("false")) return a;
            boolean k=Boolean.parseBoolean(p[1]), s=Boolean.parseBoolean(p[2]), pl=Boolean.parseBoolean(p[3]), w=Boolean.parseBoolean(p[4]), c=Boolean.parseBoolean(p[5]), l=Boolean.parseBoolean(p[6]), f=Boolean.parseBoolean(p[7]);
            if (n<0 || n>3 || (s&&!k) || (pl&&!s) || (w&&!pl) || (c&&!w) || (l&&(!c||n!=3)) || (!l&&n==3) || (f&&!l) || (r!=Room.WORKSHOP&&!s) || (r==Room.OBSERVATORY&&!c)) return a;
            a.room=r; a.key=k; a.seed=s; a.planted=pl; a.water=w; a.crystal=c; a.lens=l; a.finished=f; a.symbols=n;
        } catch (IllegalArgumentException | NullPointerException ignored) { return new Adventure(); }
        return a;
    }
}
