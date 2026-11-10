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
    private HeadTracker tracker;
    private GLSurfaceView surface;
    private WorldRenderer renderer;
    private TextToSpeech speech;
    private GitHubUpdates updates;
    private boolean speakingReady,foreground,inGame,vr,returnToMenu;
    private android.content.SharedPreferences prefs;
    private float previousX,previousY;
    private final Handler handler=new Handler(Looper.getMainLooper());
    @Override public void onCreate(Bundle state){
        super.onCreate(state);prefs=getSharedPreferences("adventure",MODE_PRIVATE);game=Adventure.restore(prefs.getString("state",""));tracker=new HeadTracker(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        speech=new TextToSpeech(this,status->{if(status==TextToSpeech.SUCCESS){
            int result=speech.setLanguage(Locale.GERMAN);speakingReady=result!=TextToSpeech.LANG_MISSING_DATA&&result!=TextToSpeech.LANG_NOT_SUPPORTED;
            if(speakingReady&&speech.getVoices()!=null)for(Voice voice:speech.getVoices())if(voice.getLocale().getLanguage().equals("de")&&!voice.isNetworkConnectionRequired()){speech.setVoice(voice);break;}
            speech.setSpeechRate(.88f);
        }});
        updates=GitHubUpdates.attach(this,REPOSITORY,this::showMenu);showMenu();
    }
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);}
    private void speak(String text){if(speakingReady&&foreground&&prefs.getBoolean("voice",true))speech.speak(text,TextToSpeech.QUEUE_FLUSH,null,"lumi");}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);return v;}
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextSize(15);b.setAllCaps(false);b.setTextColor(0xff142238);
        GradientDrawable bg=new GradientDrawable();bg.setColor(0xfff5d48b);bg.setCornerRadius(dp(14));b.setBackground(bg);b.setPadding(dp(18),dp(5),dp(18),dp(5));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.bottomMargin=dp(10);b.setLayoutParams(p);b.setOnClickListener(v->action.run());return b;}
    private void stopGame(){
        tracker.stop();handler.removeCallbacksAndMessages(null);if(speech!=null)speech.stop();
        if(surface!=null){surface.onPause();surface=null;renderer=null;}
        inGame=false;
    }
    private void showMenu(){
        stopGame();immersive();
        FrameLayout root=new FrameLayout(this);root.addView(new SkyBackdrop());
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(28),dp(18),dp(28),dp(18));root.addView(row,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout story=new LinearLayout(this);story.setOrientation(LinearLayout.VERTICAL);story.setPadding(0,0,dp(32),0);row.addView(story,new LinearLayout.LayoutParams(0,-2,1));
        story.addView(text("MEIAPPS  /  EIN KLEINES VR-ABENTEUER",11,0xff8edbd6));
        TextView title=text("MeiSternenreise",34,0xffffdf9d);title.setTypeface(null,Typeface.BOLD);story.addView(title);
        TextView subtitle=text("Das Geheimnis der\nSternenwerkstatt",22,0xfff1f0ec);subtitle.setPadding(0,dp(10),0,dp(10));story.addView(subtitle);
        story.addView(text("Hilf Lumi, das Sternenlicht zurückzubringen.\nUmschauen. Entdecken. Kleine Rätsel lösen.",15,0xffb8c7d8));
        TextView status=text(game.finished()?"★ Das Sternenlicht leuchtet wieder!":"Dein Spielstand: "+game.title(),13,0xff8edbd6);status.setPadding(0,dp(16),0,0);story.addView(status);
        ScrollView scroll=new ScrollView(this);row.addView(scroll,new LinearLayout.LayoutParams(dp(280),-1));LinearLayout controls=new LinearLayout(this);controls.setPadding(0,dp(12),0,0);controls.setOrientation(LinearLayout.VERTICAL);scroll.addView(controls);
        controls.addView(button("Am Handy spielen",()->startGame(false)));
        Button vrButton=button("Mit VR-Brille spielen",()->instructions());vrButton.setEnabled(tracker.available());controls.addView(vrButton);
        if(!tracker.available())controls.addView(text("Kein geeigneter Bewegungssensor erkannt. Der Handy-Modus funktioniert trotzdem.",12,0xffb8c7d8));
        controls.addView(button("Einstellungen & Ton",this::settings));
        controls.addView(button("Updates",this::checkUpdates));
        controls.addView(button("Abenteuer neu starten",()->new AlertDialog.Builder(this).setTitle("Neu anfangen?").setMessage("Dein bisheriger Spielstand wird zurückgesetzt.").setNegativeButton("Zurück",null).setPositiveButton("Neu starten",(d,n)->{game=new Adventure();save();showMenu();}).show()));
        controls.addView(text("Version "+BuildConfig.VERSION_NAME+" · Drei Räume · Ohne Zeitdruck",11,0xff9cadc4));setContentView(root);
    }
    private void instructions(){new AlertDialog.Builder(this).setTitle("Bereit für die Sternenreise?")
        .setMessage("Setz dich bequem hin. Halte das Handy quer und gerade vor dich. Nach dem Start hast du 5 Sekunden, um es in die Brille zu legen.\n\nSchau einen Gegenstand an, bis sich der Kreis füllt. Lumi gibt dir Hinweise. Hinter dir findest du die Pause.\n\nLautstärketaste: Blick neu ausrichten. Android-Zurück: Pause. Fang mit einer kurzen Runde an; bei Unwohlsein die Brille abnehmen.")
        .setNegativeButton("Zurück",null).setPositiveButton("Los geht’s",(d,n)->countdown()).show();}
    private void countdown(){stopGame();TextView counter=text("",44,0xffffdf9d);counter.setGravity(Gravity.CENTER);counter.setBackgroundColor(0xff101a32);setContentView(counter);immersive();
        for(int i=0;i<5;i++){final int remaining=5-i;handler.postDelayed(()->counter.setText("Handy in die Brille legen\n"+remaining),i*1000L);}handler.postDelayed(()->startGame(true),5000);}
    private void startGame(boolean stereo){
        stopGame();vr=stereo;inGame=true;tracker.center();
        surface=new GLSurfaceView(this);surface.setEGLContextClientVersion(2);surface.setPreserveEGLContextOnPause(true);
        renderer=new WorldRenderer(game,tracker,stereo,prefs.getFloat("lens",.16f),prefs.getFloat("ipd",.064f),this);surface.setRenderer(renderer);
        surface.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN){previousX=e.getX();previousY=e.getY();return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){if(!vr&&renderer!=null)renderer.look((previousX-e.getX())*.12f,(previousY-e.getY())*.12f);previousX=e.getX();previousY=e.getY();return true;}if(e.getAction()==MotionEvent.ACTION_UP){v.performClick();return true;}return true;});
        setContentView(surface);immersive();if(stereo&&foreground)tracker.start();speak(game.hint());
    }
    @Override public void activate(String id){
        runOnUiThread(()->{
            if(!inGame||!foreground||surface==null)return;
            if("menu".equals(id)){showMenu();return;}
            GLSurfaceView current=surface;WorldRenderer currentRenderer=renderer;
            current.queueEvent(()->{String result=game.act(id);String state=game.save();currentRenderer.refresh(result);
                runOnUiThread(()->{prefs.edit().putString("state",state).apply();if(inGame)speak(result);});});
        });
    }
    @Override public void menu(){runOnUiThread(this::showMenu);}
    private void save(){prefs.edit().putString("state",game.save()).apply();}
    private void settings(){
        LinearLayout body=new LinearLayout(this);body.setPadding(dp(22),dp(8),dp(22),dp(8));body.setOrientation(LinearLayout.VERTICAL);
        Switch voice=new Switch(this);voice.setText("Lumi spricht Hinweise vor");voice.setChecked(prefs.getBoolean("voice",true));voice.setOnCheckedChangeListener((b,on)->prefs.edit().putBoolean("voice",on).apply());body.addView(voice);
        TextView note=text(speakingReady?"Deutsche Sprachausgabe ist verfügbar.":"Für gesprochene Hinweise bitte eine deutsche Stimme in Androids Sprachausgabe installieren. Alle Hinweise erscheinen auch als Text.",13,0xff414b5b);body.addView(note);
        body.addView(text("Linsenkorrektur (0 = aus)",15,0xff293c52));SeekBar distortion=new SeekBar(this);distortion.setMax(35);distortion.setProgress(Math.round(prefs.getFloat("lens",.16f)*100));body.addView(distortion);
        body.addView(text("Augenabstand: 54–74 mm",15,0xff293c52));SeekBar ipd=new SeekBar(this);ipd.setMax(20);ipd.setProgress(Math.round(prefs.getFloat("ipd",.064f)*1000)-54);body.addView(ipd);
        body.addView(text("Die erste Version nutzt ein manuelles Brillenprofil. Stelle die Brille am Handy scharf; QR-Brillenprofile werden noch nicht eingelesen.",13,0xff414b5b));
        new AlertDialog.Builder(this).setTitle("Einstellungen").setView(body).setPositiveButton("Speichern",(d,n)->prefs.edit().putFloat("lens",distortion.getProgress()/100f).putFloat("ipd",(54+ipd.getProgress())/1000f).apply()).setNegativeButton("Zurück",null).show();
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
    @Override public boolean onKeyDown(int code,KeyEvent event){if(inGame&&vr&&(code==KeyEvent.KEYCODE_VOLUME_UP||code==KeyEvent.KEYCODE_VOLUME_DOWN)){tracker.center();if(surface!=null)surface.queueEvent(renderer::resetGaze);return true;}return super.onKeyDown(code,event);}
    final class SkyBackdrop extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        SkyBackdrop(){super(MainActivity.this);setContentDescription("Sternenhimmel");}
        @Override protected void onDraw(Canvas c){int w=getWidth(),h=getHeight();paint.setShader(new LinearGradient(0,0,w,h,0xff20394e,0xff10162b,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,paint);paint.setShader(null);
            java.util.Random random=new java.util.Random(31);for(int i=0;i<75;i++){paint.setColor(i%4==0?0xfff7d598:0xff718fa1);c.drawCircle(random.nextFloat()*w,random.nextFloat()*h,dp(i%4==0?1.5f:.8f),paint);}
            paint.setColor(0x1548bcb4);c.drawCircle(w*.43f,h*.75f,h*.65f,paint);
        }
    }
}
