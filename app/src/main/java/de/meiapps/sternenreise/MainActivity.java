package de.meiapps.sternenreise;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.opengl.GLSurfaceView;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.*;
import android.widget.*;
import com.google.android.play.core.appupdate.*;
import com.google.android.play.core.install.model.*;
import de.meiapps.updates.GitHubUpdates;
import java.util.Locale;

public final class MainActivity extends Activity implements WorldRenderer.Events {
    private static final String REPOSITORY="pehab/MeiSternenreise";
    private Adventure game;
    private PlayerProfiles profiles;
    private HeadTracker tracker;
    private GLSurfaceView surface;
    private WorldRenderer renderer;
    private TextToSpeech speech;
    private GitHubUpdates updates;
    private boolean speakingReady,foreground,inGame,vr,returnToMenu,calibration;
    private android.content.SharedPreferences prefs;
    private float previousX,previousY;
    private long storyEpoch;
    private final Handler handler=new Handler(Looper.getMainLooper());
    @Override public void onCreate(Bundle state){
        super.onCreate(state);prefs=getSharedPreferences("adventure",MODE_PRIVATE);profiles=PlayerProfiles.restore(prefs.getString("profiles",null),prefs.getString("state",""));game=Adventure.restore(profiles.active().state());persistProfiles();tracker=new HeadTracker(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(Build.VERSION.SDK_INT>=28){WindowManager.LayoutParams attributes=getWindow().getAttributes();attributes.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;getWindow().setAttributes(attributes);}
        speech=new TextToSpeech(this,status->{if(status==TextToSpeech.SUCCESS){
            int result=speech.setLanguage(Locale.GERMAN);speakingReady=result!=TextToSpeech.LANG_MISSING_DATA&&result!=TextToSpeech.LANG_NOT_SUPPORTED;
            if(speakingReady&&speech.getVoices()!=null)for(Voice voice:speech.getVoices())if(voice.getLocale().getLanguage().equals("de")&&!voice.isNetworkConnectionRequired()){speech.setVoice(voice);break;}
            speech.setSpeechRate(.88f);
        }});
        updates=GitHubUpdates.attach(this,REPOSITORY,this::showMenu);
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::showMenu);
        showMenu();
    }
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);}
    private void speak(String text){if(speakingReady&&foreground&&prefs.getBoolean("voice",true))speech.speak(text,TextToSpeech.QUEUE_FLUSH,null,"lumi");}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);return v;}
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextSize(15);b.setAllCaps(false);b.setTextColor(0xff142238);
        GradientDrawable bg=new GradientDrawable();bg.setColor(0xfff5d48b);bg.setCornerRadius(dp(14));b.setBackground(bg);b.setPadding(dp(18),dp(5),dp(18),dp(5));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.bottomMargin=dp(10);b.setLayoutParams(p);b.setOnClickListener(v->action.run());return b;}
    private void stopGame(){
        storyEpoch++;
        tracker.stop();handler.removeCallbacksAndMessages(null);if(speech!=null)speech.stop();
        if(surface!=null){surface.onPause();surface=null;renderer=null;}
        inGame=false;calibration=false;
        if(profiles!=null&&game!=null)save();
    }
    private void showMenu(){
        stopGame();immersive();
        FrameLayout root=new FrameLayout(this);root.addView(new SkyBackdrop());
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(28),dp(18),dp(28),dp(18));root.addView(row,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout story=new LinearLayout(this);story.setOrientation(LinearLayout.VERTICAL);story.setPadding(0,0,dp(32),0);row.addView(story,new LinearLayout.LayoutParams(0,-2,1));
        story.addView(text("MEIAPPS  /  EIN KLEINES VR-ABENTEUER",11,0xff8edbd6));
        TextView title=text("MeiSternenreise",34,0xffffdf9d);title.setTypeface(null,Typeface.BOLD);story.addView(title);
        TextView subtitle=text(game.chapterTitle(),22,0xfff1f0ec);subtitle.setPadding(0,dp(10),0,dp(10));story.addView(subtitle);
        story.addView(text("Hilf Lumi, das Sternenlicht zurückzubringen.\nUmschauen. Entdecken. Kleine Rätsel lösen.",15,0xffb8c7d8));
        TextView status=text(profiles.active().name()+" · Kapitel "+(game.chapter()+1)+" / "+Adventure.CHAPTERS+"\n"+(game.campaignFinished()?"★ Alle fünf Sterne leuchten!":game.finished()?"★ Kapitel geschafft":game.title()),13,0xff8edbd6);status.setPadding(0,dp(16),0,0);story.addView(status);
        ScrollView scroll=new ScrollView(this);row.addView(scroll,new LinearLayout.LayoutParams(dp(280),-1));LinearLayout controls=new LinearLayout(this);controls.setPadding(0,dp(12),0,0);controls.setOrientation(LinearLayout.VERTICAL);scroll.addView(controls);
        controls.addView(button("Profil wechseln · "+profiles.active().name(),this::chooseProfile));
        controls.addView(button("Kapitel auswählen",this::chooseChapter));
        if(game.finished()&&game.chapter()<Adventure.CHAPTERS-1)controls.addView(button("Nächstes Kapitel",()->{game.act("next");save();showMenu();}));
        controls.addView(button("Am Handy spielen",()->startGame(false)));
        Button vrButton=button("Mit VR-Brille spielen",()->instructions());vrButton.setEnabled(tracker.available());controls.addView(vrButton);
        Button alignment=button("VR-Bild ausrichten",this::alignmentInstructions);alignment.setEnabled(tracker.available());controls.addView(alignment);
        if(!tracker.available())controls.addView(text("Kein geeigneter Bewegungssensor erkannt. Der Handy-Modus funktioniert trotzdem.",12,0xffb8c7d8));
        controls.addView(button("Einstellungen & Ton",this::settings));
        controls.addView(button("Updates",this::checkUpdates));
        controls.addView(button("Kapitel neu starten",()->new AlertDialog.Builder(this).setTitle("Kapitel neu starten?").setMessage("Das aktuelle Kapitel von "+profiles.active().name()+" beginnt neu. Andere Kapitel und Profile bleiben erhalten.").setNegativeButton("Zurück",null).setPositiveButton("Neu starten",(d,n)->{game.restartChapter();save();showMenu();}).show()));
        controls.addView(text("Version "+BuildConfig.VERSION_NAME+" · Fünf Kapitel · Ohne Zeitdruck",11,0xff9cadc4));setContentView(root);
    }
    private void instructions(){new AlertDialog.Builder(this).setTitle("Bereit für die Sternenreise?")
        .setMessage("Setz dich bequem hin. Halte das Handy quer und gerade vor dich. Nach dem Start hast du 5 Sekunden, um es in die Brille zu legen.\n\nSchau einen Gegenstand an, bis sich der Kreis füllt. Lumi gibt dir Hinweise. Hinter dir findest du die Pause.\n\nLautstärketaste: Blick neu ausrichten. Android-Zurück: Pause. Fang mit einer kurzen Runde an; bei Unwohlsein die Brille abnehmen.")
        .setNegativeButton("Zurück",null).setPositiveButton("Los geht’s",(d,n)->countdown()).show();}
    private void alignmentInstructions(){new AlertDialog.Builder(this).setTitle("VR-Bild ausrichten")
        .setMessage("Lege das Handy nach dem Countdown in die Brille. In beiden Augen erscheint ein grünes Kreuz.\n\nStelle mit Lauter und Leiser den Bildabstand so ein, dass du ein gemeinsames Kreuz siehst. Android-Zurück beendet die Ausrichtung und behält den eingestellten Abstand.\n\nUnter Einstellungen kannst du zusätzlich die Displaybreite und die Bildhöhe korrigieren.")
        .setNegativeButton("Zurück",null).setPositiveButton("Ausrichten",(d,n)->countdown(true)).show();}
    private void countdown(){countdown(false);}
    private void countdown(boolean align){stopGame();TextView counter=text("",44,0xffffdf9d);counter.setGravity(Gravity.CENTER);counter.setBackgroundColor(0xff101a32);setContentView(counter);immersive();
        for(int i=0;i<5;i++){final int remaining=5-i;handler.postDelayed(()->counter.setText("Handy in die Brille legen\n"+remaining),i*1000L);}
        handler.postDelayed(()->{if(align)startCalibration();else startGame(true);},5000);}
    void startCalibration(){
        startGame(true);calibration=true;if(speech!=null)speech.stop();
        WorldRenderer current=renderer;surface.queueEvent(()->current.setCalibration(true));
    }
    void startGame(boolean stereo){
        stopGame();vr=stereo;inGame=true;tracker.center();
        surface=new GLSurfaceView(this);surface.setEGLContextClientVersion(2);surface.setPreserveEGLContextOnPause(true);
        renderer=new WorldRenderer(this,game,tracker,stereo,prefs.getFloat("lens",.16f),prefs.getFloat("ipd",.064f),this);surface.setRenderer(renderer);
        surface.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN){previousX=e.getX();previousY=e.getY();return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){if(!vr&&renderer!=null)renderer.look((previousX-e.getX())*.12f,(previousY-e.getY())*.12f);previousX=e.getX();previousY=e.getY();return true;}if(e.getAction()==MotionEvent.ACTION_UP){v.performClick();return true;}return true;});
        setContentView(surface);immersive();if(stereo&&foreground)tracker.start();speak(game.opening());
    }
    @Override public void activate(String id){
        runOnUiThread(()->{
            if(!inGame||!foreground||surface==null)return;
            if("menu".equals(id)){showMenu();return;}
            GLSurfaceView current=surface;WorldRenderer currentRenderer=renderer;String profileId=profiles.active().id();long epoch=storyEpoch;Adventure sourceGame=game;
            current.queueEvent(()->{String result=sourceGame.act(id);String state=sourceGame.save();currentRenderer.refresh(result);
                runOnUiThread(()->{if(epoch!=storyEpoch)return;if(profiles.store(profileId,state))persistProfiles();if(inGame&&profileId.equals(profiles.active().id()))speak(result);});});
        });
    }
    @Override public void menu(){runOnUiThread(this::showMenu);}
    private void persistProfiles(){prefs.edit().putString("profiles",profiles.save()).apply();}
    private void save(){profiles.store(profiles.active().id(),game.save());persistProfiles();}
    private void chooseProfile(){
        stopGame();java.util.List<PlayerProfiles.Profile> list=profiles.all();String[] labels=new String[list.size()];
        for(int i=0;i<labels.length;i++){PlayerProfiles.Profile player=list.get(i);labels[i]=(player.id().equals(profiles.active().id())?"★ ":"☆ ")+player.name()+" · Kapitel "+(Adventure.restore(player.state()).chapter()+1);}
        new AlertDialog.Builder(this).setTitle("Wer spielt?").setItems(labels,(d,n)->{profiles.select(list.get(n).id());game=Adventure.restore(profiles.active().state());persistProfiles();showMenu();})
            .setPositiveButton("Neues Profil",(d,n)->profileName(null)).setNeutralButton("Verwalten",(d,n)->manageProfiles()).setNegativeButton("Zurück",null).show();
    }
    private void profileName(String id){
        EditText input=new EditText(this);input.setSingleLine(true);input.setHint("Name");input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(24)});
        if(id!=null)for(PlayerProfiles.Profile p:profiles.all())if(p.id().equals(id))input.setText(p.name());
        new AlertDialog.Builder(this).setTitle(id==null?"Neues Spielerprofil":"Profil umbenennen").setView(input).setNegativeButton("Zurück",null)
            .setPositiveButton("Speichern",(d,n)->{try{stopGame();if(id==null){profiles.create(input.getText().toString());game=Adventure.restore(profiles.active().state());}else profiles.rename(id,input.getText().toString());persistProfiles();showMenu();}
                catch(IllegalArgumentException invalid){Toast.makeText(this,invalid.getMessage(),Toast.LENGTH_LONG).show();chooseProfile();}}).show();
    }
    private void manageProfiles(){
        java.util.List<PlayerProfiles.Profile> list=profiles.all();String[] names=new String[list.size()];for(int i=0;i<names.length;i++)names[i]=list.get(i).name();
        new AlertDialog.Builder(this).setTitle("Profil verwalten").setItems(names,(d,n)->{PlayerProfiles.Profile selected=list.get(n);
            new AlertDialog.Builder(this).setTitle(selected.name()).setItems(new String[]{"Umbenennen","Profil löschen"},(dialog,action)->{
                if(action==0)profileName(selected.id());else new AlertDialog.Builder(this).setTitle("Profil löschen?").setMessage("Die gesamte Geschichte von "+selected.name()+" wird gelöscht.").setNegativeButton("Zurück",null)
                    .setPositiveButton("Löschen",(confirm,button)->{stopGame();if(!profiles.delete(selected.id()))Toast.makeText(this,"Mindestens ein Profil muss erhalten bleiben.",Toast.LENGTH_LONG).show();game=Adventure.restore(profiles.active().state());persistProfiles();showMenu();}).show();
            }).setNegativeButton("Zurück",null).show();
        }).setNegativeButton("Zurück",null).show();
    }
    private void chooseChapter(){
        stopGame();String[] chapters=new String[Adventure.CHAPTERS];for(int i=0;i<chapters.length;i++)chapters[i]=(game.completed(i)?"★ ":game.canOpen(i)?"☆ ":"🔒 ")+(i+1)+". "+game.chapterTitle(i);
        new AlertDialog.Builder(this).setTitle("Kapitel · "+profiles.active().name()).setItems(chapters,(d,index)->{
            if(game.openChapter(index)){save();showMenu();}else Toast.makeText(this,"Löse zuerst das vorherige Kapitel.",Toast.LENGTH_LONG).show();
        }).setNegativeButton("Zurück",null).show();
    }
    private SeekBar settingSlider(LinearLayout body,String title,int maximum,int progress,float minimum,float step){
        TextView label=text("",15,0xff293c52);body.addView(label);SeekBar slider=new SeekBar(this);slider.setMax(maximum);slider.setProgress(Math.max(0,Math.min(maximum,progress)));body.addView(slider);
        String format=step<.1f?"%s: %.2f":"%s: %.1f";
        label.setText(String.format(Locale.GERMAN,format,title,minimum+slider.getProgress()*step));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int value,boolean user){label.setText(String.format(Locale.GERMAN,format,title,minimum+value*step));}
            public void onStartTrackingTouch(SeekBar bar){} public void onStopTrackingTouch(SeekBar bar){}
        });return slider;
    }
    private void settings(){
        LinearLayout body=new LinearLayout(this);body.setPadding(dp(22),dp(8),dp(22),dp(8));body.setOrientation(LinearLayout.VERTICAL);
        Switch voice=new Switch(this);voice.setText("Lumi spricht Hinweise vor");voice.setChecked(prefs.getBoolean("voice",true));voice.setOnCheckedChangeListener((b,on)->prefs.edit().putBoolean("voice",on).apply());body.addView(voice);
        body.addView(text(speakingReady?"Deutsche Sprachausgabe ist verfügbar.":"Bitte eine deutsche Android-TTS-Stimme installieren. Hinweise erscheinen auch als Text.",13,0xff414b5b));
        SeekBar distortion=settingSlider(body,"Linsenkorrektur (0 = aus)",35,Math.round(prefs.getFloat("lens",.16f)*100),0,.01f);
        float spacing=ViewerOptics.lensDistanceMm(prefs.getFloat("ipd",.064f)*1000);
        SeekBar distance=settingSlider(body,"Abstand der Bildmitten / Linsen (mm)",100,Math.round((spacing-40)*2),40,.5f);
        android.util.DisplayMetrics metrics=getResources().getDisplayMetrics();
        float width=ViewerOptics.screenWidthMm(prefs.getFloat("vrScreenWidthMm",0),metrics.widthPixels,metrics.xdpi,metrics.ydpi);
        SeekBar screen=settingSlider(body,"Displaybreite im Querformat (mm)",140,Math.round(width-100),100,1);
        SeekBar vertical=settingSlider(body,"Bildmitten nach oben (mm)",40,Math.round((prefs.getFloat("vrVerticalMm",0)+10)*2),-10,.5f);
        body.addView(text("Bei Doppelbildern zuerst „VR-Bild ausrichten“ im Menü verwenden. Lauter/Leiser verändern dort den Bildabstand. Die Displaybreite ist die sichtbare Leuchtfläche, ohne Gehäuserand. Das Brillenprofil bleibt manuell; ein QR-Profil wird noch nicht eingelesen.",13,0xff414b5b));
        ScrollView scroll=new ScrollView(this);scroll.addView(body);
        new AlertDialog.Builder(this).setTitle("Einstellungen").setView(scroll).setPositiveButton("Speichern",(d,n)->prefs.edit()
            .putFloat("lens",distortion.getProgress()/100f).putFloat("ipd",(40+distance.getProgress()*.5f)/1000)
            .putFloat("vrScreenWidthMm",100+screen.getProgress()).putFloat("vrVerticalMm",-10+vertical.getProgress()*.5f).apply())
            .setNegativeButton("Zurück",null).show();
    }
    private void checkUpdates(){
        if(!updates.fromPlay()){updates.settings();return;}
        AppUpdateManager manager=AppUpdateManagerFactory.create(this);
        manager.getAppUpdateInfo().addOnSuccessListener(info->{
            if(!foreground)return;
            if((info.updateAvailability()==UpdateAvailability.UPDATE_AVAILABLE||info.updateAvailability()==UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)&&info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)){
                try{manager.startUpdateFlowForResult(info,AppUpdateType.IMMEDIATE,this,42);}catch(IntentSender.SendIntentException e){GitHubUpdates.openPlay(this);}
            }else GitHubUpdates.openPlay(this);
        }).addOnFailureListener(error->{if(foreground)GitHubUpdates.openPlay(this);});
    }
    @Override protected void onResume(){super.onResume();foreground=true;if(returnToMenu){returnToMenu=false;showMenu();}immersive();}
    @Override protected void onPause(){foreground=false;returnToMenu=true;stopGame();super.onPause();}
    @Override protected void onStop(){super.onStop();}
    @Override protected void onDestroy(){stopGame();if(speech!=null)speech.shutdown();super.onDestroy();}
    @Override public void onBackPressed(){showMenu();}
    @Override public boolean onKeyDown(int code,KeyEvent event){if(inGame&&vr&&(code==KeyEvent.KEYCODE_VOLUME_UP||code==KeyEvent.KEYCODE_VOLUME_DOWN)){
        if(calibration){float spacing=ViewerOptics.lensDistanceMm(prefs.getFloat("ipd",.064f)*1000+(code==KeyEvent.KEYCODE_VOLUME_UP?.5f:-.5f));prefs.edit().putFloat("ipd",spacing/1000).apply();WorldRenderer current=renderer;if(surface!=null)surface.queueEvent(()->current.setLensSpacing(spacing));}
        else{tracker.center();WorldRenderer current=renderer;if(surface!=null)surface.queueEvent(current::resetGaze);}return true;}return super.onKeyDown(code,event);}
    final class SkyBackdrop extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        SkyBackdrop(){super(MainActivity.this);setContentDescription("Sternenhimmel");}
        @Override protected void onDraw(Canvas c){int w=getWidth(),h=getHeight();paint.setShader(new LinearGradient(0,0,w,h,0xff20394e,0xff10162b,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,paint);paint.setShader(null);
            java.util.Random random=new java.util.Random(31);for(int i=0;i<75;i++){paint.setColor(i%4==0?0xfff7d598:0xff718fa1);c.drawCircle(random.nextFloat()*w,random.nextFloat()*h,dp(i%4==0?1.5f:.8f),paint);}
            paint.setColor(0x1548bcb4);c.drawCircle(w*.43f,h*.75f,h*.65f,paint);
        }
    }
}
