package de.meiapps.sternenreise;

/** Five persistent chapters. Rules and save migration are independent of Android. */
public final class Adventure {
    public enum Room { WORKSHOP, GARDEN, OBSERVATORY }
    public static final int CHAPTERS=5;
    private static final String[] TITLES={"Das verlorene Sternenlicht","Die verschwundene Mondpost","Der Garten ohne Tau","Das Räderwerk der Sternenuhr","Die Nacht der fünf Sterne"};
    private static final class State {
        Room room=Room.WORKSHOP;
        boolean key,seed,planted,water,crystal,lens,finished,introduced;
        int symbols,clues,stage,hints,jug3,jug5,banks,boatSide,cargo=-1;
        final int[] wheels=new int[3];
        State(int chapter){introduced=chapter==0;}
    }
    private final State[] states=new State[CHAPTERS];
    private int chapter,frontier;
    public Adventure(){for(int i=0;i<CHAPTERS;i++)states[i]=new State(i);}
    private State s(){return states[chapter];}
    public int chapter(){return chapter;}
    public String chapterTitle(){return TITLES[chapter];}
    public String chapterTitle(int index){return TITLES[index];}
    public int unlocked(){int n=0;while(n<CHAPTERS-1&&states[n].finished)n++;return Math.max(frontier,n);}
    public boolean canOpen(int index){return index>=0&&index<CHAPTERS&&index<=unlocked();}
    public boolean openChapter(int index){if(!canOpen(index))return false;frontier=unlocked();chapter=index;return true;}
    public void restartChapter(){frontier=unlocked();states[chapter]=new State(chapter);}
    public boolean completed(int index){return states[index].finished;}
    public boolean campaignFinished(){for(State state:states)if(!state.finished)return false;return true;}
    public Room room(){return s().room;}
    public boolean hasKey(){return s().key;}
    public boolean hasSeed(){return s().seed;}
    public boolean planted(){return s().planted;}
    public boolean watered(){return s().water;}
    public boolean hasCrystal(){return s().crystal;}
    public boolean hasLens(){return s().lens;}
    public boolean finished(){return s().finished;}
    public boolean introduced(){return s().introduced;}
    public int symbols(){return s().symbols;}
    public int stage(){return s().stage;}
    public int clues(){return s().clues;}
    public int jug3(){return s().jug3;}
    public int jug5(){return s().jug5;}
    public int wheel(int index){return s().wheels[index];}
    public int banks(){return s().banks;}
    public int boatSide(){return s().boatSide;}
    public int cargo(){return s().cargo;}
    public String title(){return switch(room()){case WORKSHOP->"Die Sternenwerkstatt";case GARDEN->chapter==4?"Die Sternenfähre":"Der Mondgarten";case OBSERVATORY->"Die Sternwarte";};}
    public String opening(){return chapter==0?hint():s().introduced?hint():introduction();}
    private String introduction(){return switch(chapter){
        case 1->"Lumi hat Besuch: Die kleine Post-Eule ist im Wind ihre Mondpost losgeworden. Drei Briefe müssen vor dem Morgengrauen in der richtigen Reihenfolge zum Sternenwächter. Ein versiegelter Beutel liegt noch im Haus. Schau dich um und sammle Hinweise, bevor du die Post sortierst.";
        case 2->"Die Mondblumen lassen die Köpfe hängen. Die Tauquelle ist verstummt! Für die neue Tauperle braucht der Garten genau vier Liter Wasser. Es gibt aber nur eine kleine Drei-Liter-Kanne und eine große Fünf-Liter-Kanne. Zuerst müssen wir die alte Pumpe wieder betriebsbereit machen.";
        case 3->"In der Sternwarte steht die Sternenuhr still. Ohne sie weiß der Morgen nicht, wann er beginnen soll. Ein Zahnrad fehlt und das Räderwerk klemmt. Später wirst du merken: Jeder Hebel bewegt gleich zwei Zeiger. Beobachte genau, statt einfach alles einzuschalten.";
        default->"Heute feiern alle das Fest der fünf Sterne. Aber ein Lichtfuchs, ein Glühkäfer und ein Beutel Mondensamen warten am falschen Ufer. Die Sternenfähre trägt Lumi und höchstens einen Gast. Ohne Lumi passen nicht alle Gäste aufeinander auf. Zum Schluss musst du aus Hinweisen an verschiedenen Orten das letzte Sternenzeichen zusammensetzen.";
    };}
    public String hint(){
        if(finished())return chapter==4?"Alle fünf Sterne leuchten! Die Mondpost ist da, die Blumen haben Tau und die Sternenuhr läuft. Danke, Sternenfreund!":chapter==0?"Du hast das Sternenlicht zurückgebracht! Danke, Sternenfreund!":"Kapitel geschafft! "+chapterTitle()+" ist gelöst. Im Menü oder am Stern kannst du zum nächsten Kapitel weitergehen.";
        if(chapter==0)return tutorialHint();
        if(!introduced())return "Finde Lumi und schau ihn an, um dieses Kapitel zu beginnen.";
        return switch(room()){
            case WORKSHOP->stage()==0?"Erkunde das Haus. Für die verschlossene Truhe brauchst du einen Schlüssel und einen Hinweis auf deinen Auftrag.":"Du hast die Ausrüstung. Der Weg führt in den Garten.";
            case GARDEN->switch(chapter){case 1->"Finde die Sortierregel und ordne die drei Briefe. Die Zeichen allein verraten die Reihenfolge nicht.";case 2->"Wie bekommst du genau vier Liter in die große Kanne? Du darfst füllen, leeren und umgießen.";case 3->"Das Zahnrad ist da, aber es braucht noch Pflege. Suche etwas, das ein klemmendes Räderwerk wieder leichtgängig macht.";default->"Bringe alle drei Gäste sicher ans andere Ufer. Überlege vor jeder Fahrt, wer ohne Lumi zusammenbleibt.";};
            case OBSERVATORY->switch(chapter){case 1->"Wer ist der Empfänger der Mondpost? Suche die Nachricht der Post-Eule.";case 2->"Die Tauperle ist bereit. Finde ihren Platz in der Sternwarte.";case 3->"Die drei Zeiger hängen zusammen. Finde den Zielzustand und beobachte, welche beiden Zeiger jeder Hebel verändert.";default->"Vier Zeichen öffnen das letzte Sternentor. Verbinde die Hinweise aus Haus, Garten und Sternwarte.";};
        };
    }
    public String puzzleStatus(){
        if(chapter==2&&room()==Room.GARDEN)return "Kleine Kanne: "+jug3()+" / 3 Liter · Große Kanne: "+jug5()+" / 5 Liter";
        if(chapter==3&&room()==Room.OBSERVATORY)return "Zeiger: "+sign(s().wheels[0])+" · "+sign(s().wheels[1])+" · "+sign(s().wheels[2]);
        if(chapter==4&&room()==Room.GARDEN)return "Lumi: "+(boatSide()==0?"linkes":"rechtes")+" Ufer · Mitfahrer: "+(cargo()<0?"niemand":new String[]{"Lichtfuchs","Glühkäfer","Mondensamen"}[cargo()]);
        if((chapter==1&&room()==Room.GARDEN)||(chapter==4&&room()==Room.OBSERVATORY))return "Belegte Plätze: "+symbols()+" / "+(chapter==1?3:4);
        return "";
    }
    private static String sign(int value){return new String[]{"Stern","Mond","Sonne"}[value];}
    public String act(String id){
        if("story".equals(id))return chapter==0?tutorialHint():introduction();
        if("next".equals(id)){if(finished()&&chapter<CHAPTERS-1){frontier=unlocked();chapter++;return opening();}return hint();}
        if(chapter==0)return actTutorial(id);
        if("begin".equals(id)){s().introduced=true;return introduction();}
        if("hint".equals(id)){if(!introduced())return introduction();s().hints=Math.min(3,s().hints+1);return gradedHint();}
        if(!introduced())return hint();
        if("back".equals(id)){if(room()==Room.OBSERVATORY)s().room=Room.GARDEN;else if(room()==Room.GARDEN)s().room=Room.WORKSHOP;s().hints=0;return hint();}
        if("reset_puzzle".equals(id)&&!finished()){
            if(chapter==2){s().jug3=s().jug5=0;return "Beide Kannen sind wieder leer.";}
            if(chapter==3){java.util.Arrays.fill(s().wheels,0);return "Alle Zeiger stehen wieder auf Stern.";}
        }
        if("clue".equals(id)){s().clues|=1<<room().ordinal();return clue();}
        if(room()==Room.WORKSHOP){
            if("key".equals(id)){s().key=true;return "Du findest den silbernen Federschlüssel. Welche Truhe lässt sich damit öffnen?";}
            if("chest".equals(id)){if(!hasKey())return "Die Truhe ist noch verschlossen.";if((clues()&1)==0)return "In der Nähe liegt eine Nachricht. Lies oder höre sie zuerst an, damit wir wissen, wozu die Ausrüstung gehört.";s().stage=Math.max(1,stage());s().seed=true;return switch(chapter){case 1->"Im Beutel liegen drei Briefe. Trage sie zum Sortierplatz im Garten.";case 2->"Ein Pumpengriff! Im Garten kannst du jetzt die Kannen benutzen.";case 3->"Das fehlende Zahnrad! Im Garten findet sich bestimmt Hilfe für die rostige Achse.";default->"Die Laterne gehört zur Sternenfähre. Jetzt können wir zum Ufer gehen.";};}
            if("garden".equals(id)){if(stage()<1)return "Wir brauchen noch die Ausrüstung aus der Truhe.";s().room=Room.GARDEN;s().hints=0;return hint();}
        }else if(room()==Room.GARDEN){
            if("observatory".equals(id)){if(stage()<2)return "Hier wartet noch eine ungelöste Aufgabe.";s().room=Room.OBSERVATORY;s().hints=0;return hint();}
            return actGarden(id);
        }else return actObservatory(id);
        return hint();
    }
    private String actGarden(String id){
        if(chapter==1&&isSign(id)){
            if((clues()&2)==0)return "Suche zuerst die Sortierregel im Garten.";
            return sequence(id,new String[]{"sun","moon","star"},true);
        }
        if(chapter==2){
            if("select3".equals(id)){s().cargo=0;return "Kleine Drei-Liter-Kanne ausgewählt. Wähle jetzt Pumpe, Leeren oder Umgießen.";}
            if("select5".equals(id)){s().cargo=1;return "Große Fünf-Liter-Kanne ausgewählt. Wähle jetzt Pumpe, Leeren oder Umgießen.";}
            if("fill".equals(id)||"empty".equals(id)||"pour".equals(id)){
                if(cargo()<0)return "Wähle zuerst eine der beiden Kannen aus.";
                id="fill".equals(id)?(cargo()==0?"fill3":"fill5"):"empty".equals(id)?(cargo()==0?"empty3":"empty5"):(cargo()==0?"pour3to5":"pour5to3");
            }
            switch(id){
                case "fill3":s().jug3=3;break;case "fill5":s().jug5=5;break;
                case "empty3":s().jug3=0;break;case "empty5":s().jug5=0;break;
                case "pour3to5":int a=Math.min(jug3(),5-jug5());s().jug3-=a;s().jug5+=a;break;
                case "pour5to3":int b=Math.min(jug5(),3-jug3());s().jug5-=b;s().jug3+=b;break;
                case "flower":if((clues()&2)==0)return "Am Beet steht, wie viel Wasser die Blume braucht.";if(jug5()!=4)return "Die große Kanne enthält "+jug5()+" Liter. Die Blume braucht genau vier. Versuche es weiter!";s().stage=2;s().crystal=true;return "Genau vier Liter! Die Mondblume schenkt dir eine neue Tauperle. Bring sie zur Sternwarte.";
                default:return hint();
            }
            return puzzleStatus()+". Die Füllstände bleiben erhalten, wenn du zurück ins Haus gehst.";
        }
        if(chapter==3){
            if("oil".equals(id)){s().planted=true;return "Eine kleine Flasche Öl. Die rostige Achse am Zahnrad könnte sie brauchen.";}
            if("repair".equals(id)){if(!s().planted)return "Das Zahnrad klemmt noch. Suche etwas für die rostige Achse.";if((clues()&2)==0)return "Die Pflegeanleitung liegt noch im Garten.";s().stage=2;return "Jetzt dreht sich das geölte Zahnrad leicht. Ab zur Sternenuhr!";}
        }
        if(chapter==4){
            if(id.startsWith("cargo_")){
                int item=switch(id){case "cargo_0"->0;case "cargo_1"->1;case "cargo_2"->2;default->-1;};
                if(item<0)return hint();if(((banks()>>item)&1)!=boatSide())return "Dieser Gast wartet am anderen Ufer.";
                s().cargo=s().cargo==item?-1:item;return puzzleStatus();
            }
            if("cross".equals(id)){
                if((clues()&2)==0)return "Lies zuerst die Fährregeln auf dem Schild am Ufer.";
                int next=banks()^(cargo()<0?0:1<<cargo()),side=1-boatSide();
                if(!safeBanks(next,side))return "So geht es noch nicht: Ohne Lumi dürfen Fuchs und Käfer oder Käfer und Samen nicht zusammenbleiben. Nimm einen anderen Gast mit oder bringe jemanden zurück.";
                s().banks=next;s().boatSide=side;s().cargo=-1;
                if(next==7&&side==1){s().stage=2;return "Alle drei sind sicher drüben! Der Weg zur Sternwarte ist frei. Denke auch an die Zeichen auf dem Fährschild.";}
                return "Die Fähre landet am "+(side==0?"linken":"rechten")+" Ufer. Auch eine Rückfahrt ohne Gast ist möglich.";
            }
        }
        return hint();
    }
    public static boolean safeBanks(int rightBank,int caretakerSide){
        int unattended=caretakerSide==0?rightBank:(7^rightBank);
        return (unattended&3)!=3&&(unattended&6)!=6;
    }
    private String actObservatory(String id){
        if(chapter==1&&isSign(id)){
            if((clues()&4)==0)return "Die Post-Eule hat eine Nachricht für den Empfänger hinterlassen.";
            if(!"star".equals(id))return "Dieser Briefkasten gehört einem anderen Empfänger. Denke an die Nachricht der Eule.";
            s().stage=3;return "Der Sternenwächter hat seine Post! Mit der Maschine kannst du die Zustellbestätigung senden.";
        }
        if(chapter==3&&id.startsWith("lever_")){
            switch(id){case "lever_a":turn(0);turn(1);break;case "lever_b":turn(1);turn(2);break;case "lever_c":turn(0);turn(2);break;default:return hint();}
            return puzzleStatus()+". Jeder Hebel bewegt zwei Zeiger einen Schritt weiter.";
        }
        if(chapter==4&&isSign(id)){
            if((clues()&4)==0)return "Die letzte Sternenkarte fehlt noch. Suche sie in der Sternwarte.";
            return sequence(id,new String[]{"moon","star","sun","moon"},false);
        }
        if("machine".equals(id)){
            if((clues()&4)==0)return "Schau dich in der Sternwarte nach dem letzten Hinweis um.";
            if(chapter==1&&stage()<3)return "Die Briefe müssen erst beim richtigen Empfänger ankommen.";
            if(chapter==3&&(s().wheels[0]!=0||s().wheels[1]!=0||s().wheels[2]!=1))return "Die Uhr ist noch nicht richtig eingestellt. Vergleiche alle drei Zeiger mit der Zielkarte.";
            if(chapter==4&&stage()<3)return "Das letzte Sternenzeichen ist noch nicht vollständig.";
            s().stage=3;s().finished=true;return hint();
        }
        return hint();
    }
    private void turn(int index){s().wheels[index]=(s().wheels[index]+1)%3;}
    private static boolean isSign(String id){return "star".equals(id)||"moon".equals(id)||"sun".equals(id);}
    private String sequence(String id,String[] order,boolean garden){
        if(s().symbols>=order.length)return "Die Zeichenfolge ist schon vollständig.";
        if(!id.equals(order[s().symbols])){s().symbols=0;return "Diese Reihenfolge passt noch nicht. Schau dir die gefundenen Hinweise noch einmal an. Du darfst neu beginnen.";}
        s().symbols++;
        if(s().symbols==order.length){s().stage=garden?2:3;return garden?"Die drei Briefe sind richtig sortiert. Trage sie zur Sternwarte!":"Die vier Zeichen passen zusammen. Jetzt kannst du das letzte Sternenlicht entzünden.";}
        return "Das Zeichen passt. "+s().symbols+" von "+order.length+" Plätzen sind belegt.";
    }
    private String clue(){return switch(chapter){
        case 1->switch(room()){case WORKSHOP->"Die Post-Eule schreibt: Der Federschlüssel öffnet die Reisetasche. Die Briefe werden im Garten sortiert und gehören einem Wächter in der Sternwarte.";case GARDEN->"Sortierregel: Die Sonnenpost muss vor der Mondpost liegen. Die Sternenpost kommt nach beiden anderen Briefen.";case OBSERVATORY->"Die Nachricht lautet: Mein Empfänger trägt das Zeichen, das nachts funkelt, aber weder rund ist noch eine Sichel hat.";};
        case 2->switch(room()){case WORKSHOP->"Der Tauhüter braucht vier Liter Wasser. In der Truhe liegt der Griff der Gartenpumpe. Die kleine Kanne fasst drei, die große fünf Liter.";case GARDEN->"Am Beet steht: Genau vier Liter müssen in der großen Kanne sein. Umgießen endet, wenn die erste Kanne leer oder die zweite voll ist. Wasser darf zurück in den Brunnen.";case OBSERVATORY->"Die Tauperle gehört in die runde Schale der Sternenmaschine. Die Blume hat die Menge bereits geprüft.";};
        case 3->switch(room()){case WORKSHOP->"Ein Brief des Uhrmachers: Das fehlende Zahnrad liegt in der verschlossenen Truhe. Die Zielkarte für die Zeiger wartet in der Sternwarte.";case GARDEN->"Pflegeanleitung: Ein Tropfen Öl auf die rostige Zahnradachse macht sie wieder leichtgängig. Das Öl ist in einer kleinen Glasflasche.";case OBSERVATORY->"Zielkarte: Links ein Stern, in der Mitte ein Stern, rechts ein Mond. Die Zeiger laufen im Kreis: Stern, Mond, Sonne, wieder Stern. Jeder Hebel bewegt zwei Zeiger.";};
        default->switch(room()){case WORKSHOP->"Erster Hinweis: Das Nachtlicht steht am Anfang und am Ende der vier Zeichen. Mit Nachtlicht ist die Mondsichel gemeint. In der Truhe liegt die Fährlaterne.";case GARDEN->"Fährregeln: Lumi darf höchstens einen Gast mitnehmen. Ohne Lumi dürfen Fuchs und Käfer oder Käfer und Samen nicht zusammenbleiben. Zweiter Zeichenhinweis: Der Stern steht direkt vor der Sonne.";case OBSERVATORY->"Letzte Sternenkarte: Vier Plätze bilden das Zeichen. Verbinde den Hinweis aus dem Haus mit dem vom Fährschild. Manche Zeichen werden mehrfach gebraucht.";};
    };}
    private String gradedHint(){
        if(s().hints==1)return hint()+" Hinweise findest du auch abseits deiner ersten Blickrichtung.";
        if(s().hints==2)return room()==Room.WORKSHOP?"Untersuche die Nachricht und suche dann den silbernen Schlüssel. Die Truhe verrät, welche Ausrüstung du brauchst.":clue();
        if(room()==Room.WORKSHOP)return "Nachricht, Schlüssel, Truhe: Erst zusammen führen sie dich weiter. Schau auch seitlich und hinter dich.";
        if(chapter==1)return room()==Room.GARDEN?"Beginne mit der Sonnenpost. Zwischen ihr und der Sternenpost bleibt ein Platz für den Mond.":"Der Empfänger ist der Sternenwächter. Wähle den Sternenbriefkasten.";
        if(chapter==2)return room()==Room.GARDEN?"Fülle die große Kanne und gieße in die kleine. In der großen bleiben zwei Liter. Leere die kleine. Jetzt kannst du die zwei Liter umfüllen und mit einer weiteren Füllung weiterrechnen.":"Schau die Schale an der Sternenmaschine an.";
        if(chapter==3)return "Links bewegt die ersten beiden Zeiger, Mitte die letzten beiden, rechts die äußeren. Notiere im Kopf, wie oft jeder Zeiger weiterwandern muss. Du kannst jederzeit auf Anfang stellen.";
        return room()==Room.GARDEN?"Nimm zuerst den Glühkäfer mit und fahre allein zurück. Später musst du den Käfer einmal wieder zurückbringen. Achte immer auf das Ufer ohne Lumi.":"Die Mondsichel gehört auf den ersten und letzten Platz. Stern und Sonne stehen in dieser Reihenfolge dazwischen.";
    }
    private String tutorialHint() {
        if (s().finished) return "Du hast das Sternenlicht zurückgebracht! Danke, Sternenfreund!";
        return switch(s().room) {
            case WORKSHOP -> !s().key ? "Ich bin Lumi! Suche den goldenen Schlüssel im Regal." : !s().seed ? "In der kleinen Truhe wartet ein Mondensamen." : "Schau die Tür zum Garten an. Dort braucht dein Samen Wasser.";
            case GARDEN -> !s().water ? "Pflanze den Samen in den Topf. Dann schau die Gießkanne an." : !s().crystal ? "Die Mondblume leuchtet! Nimm ihren blauen Kristall." : "Der Kristall öffnet die Tür zur Sternwarte.";
            case OBSERVATORY -> !s().lens ? "Am Pult gilt die Reihenfolge: Stern, Mond, Sonne. Schau die passenden Symbole an." : "Setze die Linse und den Kristall in die Sternenmaschine!";
        };
    }
    private String actTutorial(String id) {
        if ("hint".equals(id)) return hint();
        if (s().room == Room.WORKSHOP) {
            switch(id) {
                case "key": s().key = true; return "Ein goldener Schlüssel! Damit kannst du die kleine Truhe öffnen.";
                case "chest": if (!s().key) return "Die Truhe ist zu. Lumi meint, im Regal liegt ein Schlüssel.";
                    s().seed = true; return "Du hast einen Mondensamen gefunden! Lass ihn im Mondgarten wachsen.";
                case "garden": if (!s().seed) return "Wir brauchen erst den Mondensamen aus der Truhe.";
                    s().room = Room.GARDEN; return hint();
            }
        } else if (s().room == Room.GARDEN) {
            switch(id) {
                case "back": s().room = Room.WORKSHOP; return hint();
                case "pot": s().planted = true; return s().water ? "Deine Mondblume ist wunderschön gewachsen." : "Du legst den Samen in die weiche Erde. Jetzt fehlt Wasser!";
                case "water": if (!s().planted) return "Lege zuerst den Mondensamen in den Topf. Schau dafür den Topf an.";
                    s().water = true; return "Plitsch, platsch! Der Samen wächst zu einer leuchtenden Mondblume. Schau sie an!";
                case "flower": if (!s().water) return "Hier kann eine Mondblume wachsen. Schau erst die Gießkanne an.";
                    s().crystal = true; return "Die Blume schenkt dir einen Sternenkristall. Die Tür zur Sternwarte leuchtet!";
                case "observatory": if (!s().crystal) return "Die Tür braucht das Licht eines Sternenkristalls.";
                    s().room = Room.OBSERVATORY; return hint();
            }
        } else {
            switch(id) {
                case "back": s().room = Room.GARDEN; return hint();
                case "star": case "moon": case "sun":
                    if (s().lens) return "Du hast die Sternenlinse schon gefunden. Schau die große Maschine an.";
                    String expected = new String[]{"star", "moon", "sun"}[s().symbols];
                    if (!id.equals(expected)) { s().symbols = "star".equals(id) ? 1 : 0; return "Versuch es noch einmal: Stern, Mond, Sonne."; }
                    s().symbols++;
                    if (s().symbols == 3) { s().lens = true; return "Richtig! Die Sternenlinse gleitet in deine Tasche. Jetzt zur Maschine!"; }
                    return s().symbols == 1 ? "Der Stern leuchtet. Jetzt der Mond!" : "Der Mond leuchtet. Jetzt die Sonne!";
                case "machine": if (!s().lens) return "Der Kristall ist da. Löse noch das Rätsel am Symbolpult, um die Linse zu bekommen.";
                    s().finished = true; return hint();
            }
        }
        return hint();
    }
    public String save(){
        StringBuilder out=new StringBuilder("SR3|").append(chapter).append('|').append(unlocked());
        for(State state:states)out.append('|').append(encode(state));return out.toString();
    }
    private static String encode(State s){return s.room+","+s.key+","+s.seed+","+s.planted+","+s.water+","+s.crystal+","+s.lens+","+s.finished+","+s.symbols+","+s.introduced+","+s.clues+","+s.stage+","+s.hints+","+s.jug3+","+s.jug5+","+s.wheels[0]+","+s.wheels[1]+","+s.wheels[2]+","+s.banks+","+s.boatSide+","+s.cargo;}
    public static Adventure restore(String data){
        Adventure result=new Adventure();
        try{
            if(data.startsWith("SR3|")){
                String[] pieces=data.split("\\|");if(pieces.length!=CHAPTERS+3)return result;
                int active=Integer.parseInt(pieces[1]);if(active<0||active>=CHAPTERS)return result;
                for(int i=0;i<CHAPTERS;i++)result.states[i]=decode(pieces[i+3],i);
                result.frontier=Integer.parseInt(pieces[2]);if(result.frontier<0||result.frontier>=CHAPTERS)return new Adventure();
                result.chapter=result.canOpen(active)?active:result.unlocked();
            }else result.states[0]=decode(data,0);
        }catch(IllegalArgumentException|NullPointerException ignored){return new Adventure();}
        return result;
    }
    private static State decode(String data,int chapter){
        State state=new State(chapter);
        try{
            String[] p=data.split(",");if(p.length!=21&&(chapter!=0||p.length!=9))return state;
            state.room=Room.valueOf(p[0]);state.key=bool(p[1]);state.seed=bool(p[2]);state.planted=bool(p[3]);state.water=bool(p[4]);state.crystal=bool(p[5]);state.lens=bool(p[6]);state.finished=bool(p[7]);state.symbols=Integer.parseInt(p[8]);
            if(p.length==21){state.introduced=bool(p[9]);state.clues=Integer.parseInt(p[10]);state.stage=Integer.parseInt(p[11]);state.hints=Integer.parseInt(p[12]);state.jug3=Integer.parseInt(p[13]);state.jug5=Integer.parseInt(p[14]);for(int i=0;i<3;i++)state.wheels[i]=Integer.parseInt(p[15+i]);state.banks=Integer.parseInt(p[18]);state.boatSide=Integer.parseInt(p[19]);state.cargo=Integer.parseInt(p[20]);}
            if(state.symbols<0||state.symbols>(chapter==4?4:3)||state.clues<0||state.clues>7||state.stage<0||state.stage>3||state.hints<0||state.hints>3||state.jug3<0||state.jug3>3||state.jug5<0||state.jug5>5||state.banks<0||state.banks>7||state.boatSide<0||state.boatSide>1||state.cargo< -1||state.cargo>2)return new State(chapter);
            for(int wheel:state.wheels)if(wheel<0||wheel>2)return new State(chapter);
            if(chapter==0){if((state.seed&&!state.key)||(state.planted&&!state.seed)||(state.water&&!state.planted)||(state.crystal&&!state.water)||(state.lens&&(!state.crystal||state.symbols!=3))||(!state.lens&&state.symbols==3)||(state.finished&&!state.lens)||(state.room!=Room.WORKSHOP&&!state.seed)||(state.room==Room.OBSERVATORY&&!state.crystal))return new State(0);}
            else if((state.finished&&state.stage!=3)||(state.room==Room.GARDEN&&state.stage<1)||(state.room==Room.OBSERVATORY&&state.stage<2)||(chapter==4&&!safeBanks(state.banks,state.boatSide)))return new State(chapter);
            return state;
        }catch(IllegalArgumentException ignored){return new State(chapter);}
    }
    private static boolean bool(String value){if(!"true".equals(value)&&!"false".equals(value))throw new IllegalArgumentException("Boolean expected");return Boolean.parseBoolean(value);}
}
