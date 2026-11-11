package de.meiapps.sternenreise;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import static android.opengl.GLES20.*;

/** Painted 360-degree environments with stereo foreground sprites and gaze interaction. */
final class WorldRenderer implements GLSurfaceView.Renderer {
    interface Events { void activate(String id); void menu(); }

    // Exact reading order of the four-by-four transparent atlas (see docs/ART.md).
    private static final int LUMI=0, KEY=1, CHEST=2, OPEN_CHEST=3;
    private static final int POT=4, SPROUT=5, FLOWER=6, WATERING_CAN=7;
    private static final int MACHINE=8, LIT_MACHINE=9, STAR=10, MOON=11, SUN=12, HOME=15;
    private static final int SILVER_KEY=16, MAILBAG=17, SUN_MAIL=18, MOON_MAIL=19, STAR_MAIL=20;
    private static final int SMALL_JUG=21, LARGE_JUG=22, PUMP=23, GEAR=24, OIL=25, CLOCK=26, OWL=27;
    private static final int FOX=28, BEETLE=29, SEEDS=30, FERRY=31;
    private static final float LABEL_OFFSET=.48f;
    // Visible bottom edges inside the padded atlas cells; +1 is the quad's bottom.
    private static final float[] FOOT={.894f,.849f,.733f,.823f,.830f,.836f,.875f,.740f,
        .862f,.862f,.701f,.772f,.772f,.727f,.894f,.842f};
    private record Target(String id,String label,float x,float y,float z,boolean portal) { }
    private record Sprite(int cell,float x,float y,float z,float halfWidth,float halfHeight,boolean selected) { }
    private record Caption(int texture,float aspect) { }

    private final Context context;
    private final Adventure game;
    private final HeadTracker tracker;
    private final Events events;
    private final boolean stereo;
    private final float lens;
    private float eyeSeparation,lensSpacingMm,physicalWidthMm=150;
    private final float configuredWidthMm,verticalOffsetMm;
    private boolean calibration;
    private final Gaze gaze=new Gaze();
    private final List<Target> targets=new ArrayList<>();
    private final List<Sprite> sprites=new ArrayList<>();
    private final Map<String,Caption> captions=new HashMap<>();
    private final FloatBuffer quad=buffer(-1,-1,0, 1,-1,0, -1,1,0, 1,1,0);
    private final FloatBuffer wholeTexture=buffer(0,1, 1,1, 0,0, 1,0);
    private final FloatBuffer[] atlasCoordinates=new FloatBuffer[16];
    private final float[] head=new float[16], inverseHead=new float[16], projection=new float[16];
    private final float[] view=new float[16], model=new float[16], combined=new float[16], mvp=new float[16];
    private volatile float yaw,pitch;
    private String message;
    private int width,height,eyeWidth,lastRoom=-1;
    private int foregroundProgram,panoramaProgram,distortionProgram;
    private int positionAttribute,uvAttribute,matrixUniform,colorUniform,samplerUniform;
    private int panoramaPosition,panoramaHead,panoramaTangents,panoramaSampler;
    private int distortionPosition,distortionLens,distortionFade,distortionSampler,distortionCenter;
    private int framebuffer,eyeTexture,panoramaTexture,atlasTexture,chapterTexture,whiteTexture;
    private long fadeStart;

    WorldRenderer(Context context,Adventure game,HeadTracker tracker,boolean stereo,
                  float lens,float eyeSeparation,Events events) {
        this.context=context;this.game=game;this.tracker=tracker;this.stereo=stereo;
        this.lens=lens;this.eyeSeparation=eyeSeparation;this.events=events;message=game.opening();
        android.content.SharedPreferences settings=context.getSharedPreferences("adventure",Context.MODE_PRIVATE);
        lensSpacingMm=ViewerOptics.lensDistanceMm(eyeSeparation*1000);
        configuredWidthMm=settings.getFloat("vrScreenWidthMm",0);verticalOffsetMm=settings.getFloat("vrVerticalMm",0);
        for(int cell=0;cell<16;cell++) {
            // Slight inset keeps bilinear filtering inside each transparent atlas cell.
            float left=(cell%4)/4f+.001f,right=(cell%4+1)/4f-.001f;
            float top=(cell/4)/4f+.001f,bottom=(cell/4+1)/4f-.001f;
            atlasCoordinates[cell]=buffer(left,bottom,right,bottom,left,top,right,top);
        }
    }
    void look(float dx,float dy) { yaw+=dx;pitch=Math.max(-75,Math.min(75,pitch+dy)); }
    void resetGaze() { gaze.requireNewLook(); }
    void setCalibration(boolean enabled) { calibration=enabled;gaze.reset(); }
    void setLensSpacing(float millimeters) { lensSpacingMm=ViewerOptics.lensDistanceMm(millimeters);eyeSeparation=lensSpacingMm/1000; }
    void refresh(String text) { message=text;rebuildScene();gaze.requireNewLook(); }
    private static FloatBuffer buffer(float... values) {
        FloatBuffer result=ByteBuffer.allocateDirect(values.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        result.put(values).position(0);return result;
    }

    @Override public void onSurfaceCreated(GL10 unused,EGLConfig config) {
        foregroundProgram=link(
            "attribute vec3 position;attribute vec2 texCoord;uniform mat4 matrix;varying vec2 uv;void main(){gl_Position=matrix*vec4(position,1.);uv=texCoord;}",
            "precision mediump float;varying vec2 uv;uniform sampler2D image;uniform vec4 color;void main(){vec4 pixel=texture2D(image,uv)*color;if(pixel.a<.01)discard;gl_FragColor=pixel;}");
        panoramaProgram=link(
            "attribute vec3 position;uniform mat4 cameraToWorld;uniform vec2 tangents;varying vec3 direction;void main(){gl_Position=vec4(position,1.);direction=(cameraToWorld*vec4(position.x*tangents.x,position.y*tangents.y,-1.,0.)).xyz;}",
            "precision mediump float;varying vec3 direction;uniform sampler2D image;void main(){vec3 ray=normalize(direction);float u=fract(.5+atan(ray.x,-ray.z)/6.283185307);float v=.5-asin(clamp(ray.y,-1.,1.))/3.141592654;gl_FragColor=texture2D(image,vec2(u,v));}");
        distortionProgram=link(
            "attribute vec3 position;varying vec2 point;void main(){gl_Position=vec4(position,1.);point=position.xy;}",
            "precision mediump float;varying vec2 point;uniform sampler2D image;uniform float lens;uniform float fade;uniform vec2 center;void main(){vec2 ray=point-center;vec2 source=ray*(1.+lens*dot(ray,ray));if(abs(source.x)>1.||abs(source.y)>1.)gl_FragColor=vec4(0.,0.,0.,1.);else gl_FragColor=vec4(texture2D(image,(source+1.)*.5).rgb*fade,1.);}");
        positionAttribute=glGetAttribLocation(foregroundProgram,"position");
        uvAttribute=glGetAttribLocation(foregroundProgram,"texCoord");
        matrixUniform=glGetUniformLocation(foregroundProgram,"matrix");
        colorUniform=glGetUniformLocation(foregroundProgram,"color");
        samplerUniform=glGetUniformLocation(foregroundProgram,"image");
        panoramaPosition=glGetAttribLocation(panoramaProgram,"position");
        panoramaHead=glGetUniformLocation(panoramaProgram,"cameraToWorld");
        panoramaTangents=glGetUniformLocation(panoramaProgram,"tangents");
        panoramaSampler=glGetUniformLocation(panoramaProgram,"image");
        distortionPosition=glGetAttribLocation(distortionProgram,"position");
        distortionLens=glGetUniformLocation(distortionProgram,"lens");
        distortionFade=glGetUniformLocation(distortionProgram,"fade");
        distortionSampler=glGetUniformLocation(distortionProgram,"image");
        distortionCenter=glGetUniformLocation(distortionProgram,"center");
        captions.clear();framebuffer=0;eyeTexture=0;panoramaTexture=0;lastRoom=-1;
        atlasTexture=loadAsset("sprites.webp");chapterTexture=loadAsset("chapter-sprites.webp");gaze.requireNewLook();
        Bitmap white=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);white.eraseColor(Color.WHITE);
        whiteTexture=upload(white);white.recycle();
        // Android bitmaps are premultiplied; preserve the soft painted alpha edges.
        glEnable(GL_BLEND);glBlendFunc(GL_ONE,GL_ONE_MINUS_SRC_ALPHA);glDisable(GL_DEPTH_TEST);
        rebuildScene();
    }
    @Override public void onSurfaceChanged(GL10 unused,int width,int height) {
        this.width=width;this.height=height;eyeWidth=stereo?width/2:width;
        android.util.DisplayMetrics metrics=context.getResources().getDisplayMetrics();
        physicalWidthMm=ViewerOptics.screenWidthMm(configuredWidthMm,width,metrics.xdpi,metrics.ydpi);
        allocateEye();
    }
    private void allocateEye() {
        if(eyeTexture!=0) {glDeleteTextures(1,new int[]{eyeTexture},0);glDeleteFramebuffers(1,new int[]{framebuffer},0);}
        int[] id=new int[1];glGenTextures(1,id,0);eyeTexture=id[0];glBindTexture(GL_TEXTURE_2D,eyeTexture);
        textureParameters();glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,eyeWidth,height,0,GL_RGBA,GL_UNSIGNED_BYTE,null);
        glGenFramebuffers(1,id,0);framebuffer=id[0];glBindFramebuffer(GL_FRAMEBUFFER,framebuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,eyeTexture,0);
        if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Eye framebuffer unavailable");
        glBindFramebuffer(GL_FRAMEBUFFER,0);
    }

    @Override public void onDrawFrame(GL10 unused) {
        if(width==0||height==0)return;
        long now=SystemClock.uptimeMillis();
        if(stereo)tracker.copyView(head);
        else {Matrix.setIdentityM(head,0);Matrix.rotateM(head,0,pitch,1,0,0);Matrix.rotateM(head,0,yaw,0,1,0);}
        Matrix.transposeM(inverseHead,0,head,0);
        Target focused=calibration?null:findTarget(-head[2],-head[6],-head[10]);
        if(now-fadeStart>650 && gaze.update(focused==null?null:focused.id,now))events.activate(focused.id);
        float progress=gaze.progress(now);
        float fieldOfView=stereo?82:65;
        float tangentY=(float)Math.tan(Math.toRadians(fieldOfView/2));
        for(int eye=0;eye<(stereo?2:1);eye++) {
            glBindFramebuffer(GL_FRAMEBUFFER,framebuffer);glViewport(0,0,eyeWidth,height);
            glClearColor(.03f,.06f,.1f,1);glClear(GL_COLOR_BUFFER_BIT);
            if(calibration) {
                Matrix.setIdentityM(view,0);Matrix.setIdentityM(projection,0);drawAlignment();
            } else {
            drawPanorama(tangentY);
            Matrix.perspectiveM(projection,0,fieldOfView,(float)eyeWidth/height,.08f,40);
            Matrix.setIdentityM(view,0);
            if(stereo)Matrix.translateM(view,0,eye==0?eyeSeparation/2:-eyeSeparation/2,0,0);
            Matrix.multiplyMM(combined,0,view,0,head,0);System.arraycopy(combined,0,view,0,16);
            for(Sprite sprite:sprites)drawSprite(sprite,now);
            if(game.finished() && game.room()==Adventure.Room.OBSERVATORY)drawCelebration(now);
            for(Target target:targets) {
                // Painted objects remain unobscured; names appear while examining them.
                if(target.portal || (focused!=null && focused.id.equals(target.id)))
                    drawCaption(target.label,target.x,target.y+LABEL_OFFSET,target.z,.52f,"label",false);
            }
            Matrix.setIdentityM(view,0);
            if(stereo)Matrix.translateM(view,0,eye==0?eyeSeparation/2:-eyeSeparation/2,0,0);
            drawCaption(game.finished()?(game.campaignFinished()?"Geschafft! · Alle fünf Sterne leuchten":"Kapitel geschafft!"):(game.chapter()+1)+"/5 · "+game.chapterTitle(),0,1.21f,-2.3f,.75f,"title",true);
            drawCaption(message,0,.77f,-2.3f,1.03f,"message",true);
            if(!game.puzzleStatus().isEmpty())drawCaption(game.puzzleStatus(),0,-1.05f,-2.3f,.98f,"message",true);
            if(focused!=null&&gaze.needsNewLook())drawCaption("Zum Auswählen kurz weg- und wieder hinschauen",0,-.18f,-2.3f,.8f,"label",true);
            drawReticle(focused!=null,progress);
            }
            glBindFramebuffer(GL_FRAMEBUFFER,0);glViewport(stereo?eye*eyeWidth:0,0,eyeWidth,height);
            glUseProgram(distortionProgram);position(distortionPosition);
            bindTexture(eyeTexture,distortionSampler);
            glUniform1f(distortionLens,stereo?lens:0);
            glUniform2f(distortionCenter,stereo?ViewerOptics.centerX(eye,physicalWidthMm,lensSpacingMm):0,
                stereo?ViewerOptics.centerY(verticalOffsetMm,physicalWidthMm*height/width):0);
            glUniform1f(distortionFade,Math.max(0,Math.min(1,(now-fadeStart)/450f)));
            glDrawArrays(GL_TRIANGLE_STRIP,0,4);glDisableVertexAttribArray(distortionPosition);
        }
    }
    private void drawAlignment() {
        for(int index=-4;index<=4;index++) {
            if(index==0)continue;
            flatQuad(index*.2f,0,0,.0015f,.8f,0xff344455);
            flatQuad(0,index*.2f,0,.8f,.0015f,0xff344455);
        }
        flatQuad(0,0,0,.065f,.005f,0xff40ffb0);
        flatQuad(0,0,0,.005f,.085f,0xff40ffb0);
        drawCaption("Ein gemeinsames Kreuz einstellen",0,.58f,0,.78f,"title",true);
        drawCaption(String.format(java.util.Locale.GERMAN,"Lauter / Leiser: Abstand %.1f mm",lensSpacingMm),0,-.47f,0,.78f,"label",true);
        drawCaption("Zurück: speichern und Menü",0,-.7f,0,.72f,"label",true);
    }
    private Target findTarget(float x,float y,float z) {
        Target found=null;double best=-1;float[] direction={x,y,z};
        for(Target target:targets) {
            float radius=target.portal?.18f:.145f;
            if(!Gaze.hits(direction,target.x,target.y,target.z,radius)
                && !Gaze.hits(direction,target.x,target.y+LABEL_OFFSET,target.z,.11f))continue;
            double centerScore=alignment(x,y,z,target.x,target.y,target.z);
            double labelScore=alignment(x,y,z,target.x,target.y+LABEL_OFFSET,target.z);
            double score=Math.max(centerScore,labelScore);
            if(score>best){found=target;best=score;}
        }
        return found;
    }
    private static double alignment(float x,float y,float z,float tx,float ty,float tz) {
        return (x*tx+y*ty+z*tz)/Math.sqrt(tx*tx+ty*ty+tz*tz);
    }
    private void drawPanorama(float tangentY) {
        glUseProgram(panoramaProgram);position(panoramaPosition);
        glUniformMatrix4fv(panoramaHead,1,false,inverseHead,0);
        glUniform2f(panoramaTangents,tangentY*eyeWidth/height,tangentY);
        bindTexture(panoramaTexture,panoramaSampler);glDrawArrays(GL_TRIANGLE_STRIP,0,4);
        glDisableVertexAttribArray(panoramaPosition);
    }
    private void drawSprite(Sprite sprite,long now) {
        Matrix.setIdentityM(model,0);Matrix.translateM(model,0,sprite.x,sprite.y,sprite.z);
        Matrix.rotateM(model,0,(float)-Math.toDegrees(Math.atan2(sprite.x,-sprite.z)),0,1,0);
        Matrix.scaleM(model,0,sprite.halfWidth,sprite.halfHeight,1);
        draw(sprite.cell<16?atlasTexture:chapterTexture,atlasCoordinates[sprite.cell%16],sprite.selected?0xffffeab8:Color.WHITE);
    }
    private void drawCelebration(long now) {
        for(int index=0;index<12;index++) {
            double angle=index*Math.PI*2/12;
            float x=(float)Math.sin(angle)*4,z=-(float)Math.cos(angle)*4;
            float y=1.25f+(float)Math.sin(now/1200.0+index)*.08f;
            drawSprite(new Sprite(STAR,x,y,z,.14f,.14f,false),now);
        }
    }
    private void drawReticle(boolean focused,float progress) {
        flatQuad(0,0,-2.3f,.008f,.008f,focused?0xff96f6de:Color.WHITE);
        if(!focused)return;
        for(int index=0;index<32;index++) {
            double angle=index*Math.PI*2/32;
            flatQuad((float)Math.sin(angle)*.055f,(float)Math.cos(angle)*.055f,-2.3f,.0045f,.0045f,
                index<progress*32?0xffffd886:0xff55778c);
        }
    }
    private void flatQuad(float x,float y,float z,float halfWidth,float halfHeight,int color) {
        Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);Matrix.scaleM(model,0,halfWidth,halfHeight,1);
        draw(whiteTexture,wholeTexture,color);
    }
    private void drawCaption(String text,float x,float y,float z,float halfWidth,String style,boolean hud) {
        String key=style+":"+text;Caption caption=captions.get(key);
        if(caption==null){caption=makeCaption(text,style);captions.put(key,caption);}
        Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);
        if(!hud)Matrix.rotateM(model,0,(float)-Math.toDegrees(Math.atan2(x,-z)),0,1,0);
        Matrix.scaleM(model,0,halfWidth,halfWidth/caption.aspect,1);draw(caption.texture,wholeTexture,Color.WHITE);
    }
    private void draw(int texture,FloatBuffer coordinates,int color) {
        glUseProgram(foregroundProgram);
        Matrix.multiplyMM(combined,0,view,0,model,0);Matrix.multiplyMM(mvp,0,projection,0,combined,0);
        glUniformMatrix4fv(matrixUniform,1,false,mvp,0);
        glUniform4f(colorUniform,Color.red(color)/255f,Color.green(color)/255f,Color.blue(color)/255f,1);
        position(positionAttribute);glEnableVertexAttribArray(uvAttribute);coordinates.position(0);
        glVertexAttribPointer(uvAttribute,2,GL_FLOAT,false,0,coordinates);bindTexture(texture,samplerUniform);
        glDrawArrays(GL_TRIANGLE_STRIP,0,4);glDisableVertexAttribArray(positionAttribute);glDisableVertexAttribArray(uvAttribute);
    }
    private void position(int attribute) {
        glEnableVertexAttribArray(attribute);quad.position(0);glVertexAttribPointer(attribute,3,GL_FLOAT,false,0,quad);
    }
    private static void bindTexture(int texture,int sampler) {
        glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,texture);glUniform1i(sampler,0);
    }

    private void rebuildScene() {
        for(Caption caption:captions.values())glDeleteTextures(1,new int[]{caption.texture},0);
        captions.clear();sprites.clear();targets.clear();
        int scene=game.chapter()*3+game.room().ordinal();
        if(lastRoom!=scene) {
            if(panoramaTexture!=0)glDeleteTextures(1,new int[]{panoramaTexture},0);
            panoramaTexture=loadAsset(game.chapter()==4&&game.room()==Adventure.Room.GARDEN?"ferry.webp":switch(game.room()) {
                case WORKSHOP -> "workshop.webp";case GARDEN -> "garden.webp";case OBSERVATORY -> "observatory.webp";
            });
            lastRoom=scene;fadeStart=SystemClock.uptimeMillis();yaw=0;pitch=0;tracker.center();gaze.requireNewLook();
        }
        // Each location has its own supporting surface in the painted panorama.
        float lumiFootPitch=switch(game.room()) {
            case WORKSHOP -> -26;case GARDEN -> -26;case OBSERVATORY -> -21;
        };
        float lumiYaw=game.chapter()==4&&game.room()==Room.GARDEN?(game.boatSide()==0?-145:145):-105;
        supportedItem(game.introduced()?"hint":"begin",game.introduced()?"Lumi · Hilfe":"Lumi · Kapitel beginnen",LUMI,lumiYaw,lumiFootPitch,3.5f,.46f,.60f,false);
        item("menu","Pause · Zum Menü",HOME,180,-25,3,.2f,.2f,true);
        if(game.chapter()>0)buildChapter();else switch(game.room()) {
            case WORKSHOP -> {
                if(!game.hasKey())supportedItem("key","Goldener Schlüssel",KEY,-65,-12,3.5f,.27f,.4f,false);
                supportedItem("chest",game.hasSeed()?"Geöffnete Truhe":"Kleine Truhe",game.hasSeed()?OPEN_CHEST:CHEST,64,-26,3.5f,.60f,.48f,false);
                portal("garden","In den Mondgarten",72,-3);
            }
            case GARDEN -> {
                int flowerSprite=game.watered()?FLOWER:game.planted()?SPROUT:POT;
                // Grow upwards from the same stone rim, including transparent atlas padding.
                float plantSize=game.watered()?.76f:.55f;
                supportedItem(game.watered()?"flower":"pot",
                    game.watered()?(game.hasCrystal()?"Mondblume untersuchen":"Sternenkristall nehmen")
                        :game.planted()?"Kleiner Sprössling":"Mondensamen einpflanzen",
                    flowerSprite,-70,-26,3.5f,plantSize,plantSize,false);
                supportedItem("water","Gießkanne",WATERING_CAN,85,-27,3.5f,.53f,.52f,false);
                portal("back","Zurück ins Haus",-131,-4);
                portal("observatory","Zur Sternwarte",126,-2);
            }
            case OBSERVATORY -> {
                String[] ids={"star","moon","sun"},names={"Stern","Mond","Sonne"};int[] cells={STAR,MOON,SUN};
                for(int index=0;index<3;index++)item(ids[index],names[index],cells[index],(index-1)*16,-13,3.2f,.28f,.28f,game.symbols()>index);
                supportedItem("machine",game.finished()?"Das Sternenlicht leuchtet!":"Sternenmaschine",game.finished()?LIT_MACHINE:MACHINE,
                    106,-14,4,.75f,1,false);
                portal("back","Zurück in den Garten",-156,-4);
            }
        }
        if(game.finished()&&game.chapter()<Adventure.CHAPTERS-1)item("next","Nächstes Kapitel",STAR,55,-12,3,.24f,.24f,true);
        if(game.finished())item("menu","Abenteuer geschafft · Zum Menü",HOME,34,-14,2.8f,.19f,.19f,true);
        sprites.sort(Comparator.comparingDouble((Sprite sprite)->sprite.x*sprite.x+sprite.y*sprite.y+sprite.z*sprite.z).reversed());
    }
    private void buildChapter(){
        if(!game.introduced())return;
        if(game.room()==Adventure.Room.WORKSHOP){
            if(!game.hasKey())supportedItem("key","Silberner Federschlüssel",SILVER_KEY,-66,-16,3.5f,.28f,.4f,false);
            supportedItem("chest",game.stage()>0?"Ausrüstungstruhe geöffnet":"Verschlossene Ausrüstungstruhe",game.stage()>0?OPEN_CHEST:CHEST,66,-26,3.5f,.6f,.48f,false);
            item("clue","Nachricht untersuchen",SUN_MAIL,148,-3,4,.42f,.30f,false);
            supportedItem("story","Post-Eule · Geschichte wiederholen",OWL,-146,-24,4,.35f,.45f,false);
            portal("garden",game.chapter()==4?"Zum Fähranleger":"In den Mondgarten",72,-3);return;
        }
        if(game.room()==Adventure.Room.GARDEN){
            item("clue",game.chapter()==4?"Fährregeln und Zeichenhinweis":"Gartenhinweis untersuchen",MOON_MAIL,160,-3,4,.42f,.3f,false);
            portal("back","Zurück ins Haus",-131,-4);portal("observatory","Zur Sternwarte",126,-2);
            switch(game.chapter()){
                case 1 -> {
                    supportedItem("sun","Sonnenpost einordnen",SUN_MAIL,-62,-24,3.8f,.4f,.3f,false);
                    supportedItem("moon","Mondpost einordnen",MOON_MAIL,66,-24,3.8f,.4f,.3f,false);
                    supportedItem("star","Sternenpost einordnen",STAR_MAIL,-148,-14,3.8f,.4f,.3f,false);
                    supportedItem("inspect_bag","Mondpost",MAILBAG,95,-28,4,.42f,.45f,false);
                }
                case 2 -> {
                    supportedItem("select3","Kleine Kanne · "+game.jug3()+" / 3 Liter",SMALL_JUG,-65,-26,3.5f,.45f,.5f,game.cargo()==0);
                    supportedItem("select5","Große Kanne · "+game.jug5()+" / 5 Liter",LARGE_JUG,65,-26,3.5f,.55f,.63f,game.cargo()==1);
                    supportedItem("fill","Ausgewählte Kanne füllen",PUMP,106,-26,4,.42f,.67f,false);
                    portal("empty","Ausgewählte Kanne leeren",-150,-12);
                    portal("pour","In die andere Kanne umgießen",145,-16);
                    supportedItem("flower","Vier Liter an die Mondblume geben",FLOWER,-118,-26,3.5f,.62f,.72f,false);
                    portal("reset_puzzle","Beide Kannen leeren",-175,15);
                }
                case 3 -> {
                    supportedItem("oil","Ölflasche nehmen",OIL,-70,-26,3.5f,.3f,.42f,false);
                    supportedItem("repair","Zahnradachse pflegen",GEAR,65,-26,3.5f,.5f,.5f,false);
                }
                default -> {
                    String[] names={"Lichtfuchs","Glühkäfer","Mondensamen"};int[] cells={FOX,BEETLE,SEEDS};
                    for(int i=0;i<3;i++){
                        boolean right=((game.banks()>>i)&1)==1;
                        supportedItem("cargo_"+i,names[i]+(game.cargo()==i?" · ausgewählt":" einladen"),cells[i],right?80+i*20:-120+i*20,-27,4,.48f,.58f,game.cargo()==i);
                    }
                    supportedItem("cross","Fähre ablegen · höchstens ein Gast",FERRY,game.boatSide()==0?-42:42,-29,3.8f,.7f,.48f,false);
                }
            }
            return;
        }
        item("clue","Sternenkarte untersuchen",STAR_MAIL,150,4,4,.42f,.3f,false);
        supportedItem("story","Post-Eule · Geschichte",OWL,-140,-24,4,.35f,.45f,false);
        portal("back","Zurück in den Garten",-156,-4);
        if(game.chapter()==1||game.chapter()==4){
            int[] cells={STAR_MAIL,MOON_MAIL,SUN_MAIL};String[] ids={"star","moon","sun"},names={"Stern","Mond","Sonne"};
            for(int i=0;i<3;i++)item(ids[i],names[i]+(game.chapter()==1?" · Briefkasten":" · Zeichen wählen"),cells[i],new float[]{-65,70,145}[i],-13,3.6f,.36f,.28f,false);
        }
        if(game.chapter()==3){
            for(int i=0;i<3;i++)item("lever_"+(char)('a'+i),new String[]{"Linker Hebel","Mittlerer Hebel","Rechter Hebel"}[i],GEAR,new float[]{-65,65,130}[i],-9,4,.33f,.4f,false);
            for(int i=0;i<3;i++){
                float angle=(i-1)*14,rad=(float)Math.toRadians(angle);
                sprites.add(new Sprite(new int[]{STAR,MOON,SUN}[game.wheel(i)],(float)Math.sin(rad)*3.5f,-.5f,-(float)Math.cos(rad)*3.5f,.25f,.25f,false));
            }
            portal("reset_puzzle","Alle Zeiger auf Anfang",-175,15);
        }
        supportedItem("machine",game.finished()?"Kapitel geschafft":game.chapter()==3?"Sternenuhr prüfen":"Sternenmaschine",game.chapter()==3?CLOCK:game.finished()?LIT_MACHINE:MACHINE,106,-14,4,.75f,1,false);
    }
    private void portal(String id,String label,float yaw,float pitch) {target(id,label,yaw,pitch,5,true);}
    private void item(String id,String label,int cell,float yaw,float pitch,float distance,float halfWidth,float halfHeight,boolean selected) {
        Target target=target(id,label,yaw,pitch,distance,("menu".equals(id)||"next".equals(id)));
        sprites.add(new Sprite(cell,target.x,target.y,target.z,halfWidth,halfHeight,selected));
    }
    private void supportedItem(String id,String label,int cell,float yaw,float footPitch,float distance,
                               float halfWidth,float halfHeight,boolean selected) {
        Target center=target(id,label,yaw,footPitch,distance,false,halfHeight*(cell<16?FOOT[cell]:.88f));
        sprites.add(new Sprite(cell,center.x,center.y,center.z,halfWidth,halfHeight,selected));
    }
    private Target target(String id,String label,float yaw,float pitch,float distance,boolean portal) {
        return target(id,label,yaw,pitch,distance,portal,0);
    }
    private Target target(String id,String label,float yaw,float pitch,float distance,boolean portal,float lift) {
        double horizontal=Math.toRadians(yaw),vertical=Math.toRadians(pitch);
        float x=(float)(Math.sin(horizontal)*Math.cos(vertical)*distance);
        float y=(float)(Math.sin(vertical)*distance)+lift;
        float z=(float)(-Math.cos(horizontal)*Math.cos(vertical)*distance);
        Target target=new Target(id,label,x,y,z,portal);targets.add(target);return target;
    }

    private Caption makeCaption(String text,String style) {
        boolean body="message".equals(style),title="title".equals(style);
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        paint.setTextSize(body?43:44);paint.setTextAlign(Paint.Align.CENTER);
        int bitmapWidth=body?1024:Math.max(160,Math.min(1000,(int)Math.ceil(paint.measureText(text))+70));
        List<String> lines=new ArrayList<>();String line="";
        for(String word:text.split(" ")) {
            String next=line.isEmpty()?word:line+" "+word;
            if(!line.isEmpty()&&paint.measureText(next)>bitmapWidth-70){lines.add(line);line=word;}else line=next;
        }
        lines.add(line);int bitmapHeight=lines.size()*54+44;
        Bitmap bitmap=Bitmap.createBitmap(bitmapWidth,bitmapHeight,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);
        paint.setColor(0xe6152336);canvas.drawRoundRect(1,1,bitmapWidth-1,bitmapHeight-1,24,24,paint);
        paint.setColor(title?0xffffdf9d:Color.WHITE);
        Paint.FontMetrics metrics=paint.getFontMetrics();float baseline=(bitmapHeight-(lines.size()-1)*54)/2f-(metrics.ascent+metrics.descent)/2;
        for(String value:lines){canvas.drawText(value,bitmapWidth/2f,baseline,paint);baseline+=54;}
        int texture=upload(bitmap);bitmap.recycle();return new Caption(texture,(float)bitmapWidth/bitmapHeight);
    }
    private int loadAsset(String filename) {
        try(InputStream input=context.getAssets().open("art/"+filename)) {
            BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
            Bitmap bitmap=BitmapFactory.decodeStream(input,null,options);
            if(bitmap==null)throw new IOException("Invalid image: "+filename);
            int[] limit=new int[1];glGetIntegerv(GL_MAX_TEXTURE_SIZE,limit,0);
            if(bitmap.getWidth()>limit[0]||bitmap.getHeight()>limit[0]) {
                float scale=Math.min((float)limit[0]/bitmap.getWidth(),(float)limit[0]/bitmap.getHeight());
                Bitmap reduced=Bitmap.createScaledBitmap(bitmap,Math.max(1,Math.round(bitmap.getWidth()*scale)),Math.max(1,Math.round(bitmap.getHeight()*scale)),true);
                bitmap.recycle();bitmap=reduced;
            }
            int texture=upload(bitmap);bitmap.recycle();return texture;
        }catch(IOException error){throw new IllegalStateException("Game artwork unavailable: "+filename,error);}
    }
    private static int upload(Bitmap bitmap) {
        int[] result=new int[1];glGenTextures(1,result,0);glBindTexture(GL_TEXTURE_2D,result[0]);textureParameters();
        GLUtils.texImage2D(GL_TEXTURE_2D,0,bitmap,0);return result[0];
    }
    private static void textureParameters() {
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
    }
    private static int link(String vertex,String fragment) {
        int program=glCreateProgram();
        for(int type:new int[]{GL_VERTEX_SHADER,GL_FRAGMENT_SHADER}) {
            int shader=glCreateShader(type);glShaderSource(shader,type==GL_VERTEX_SHADER?vertex:fragment);glCompileShader(shader);
            int[] success=new int[1];glGetShaderiv(shader,GL_COMPILE_STATUS,success,0);
            if(success[0]==0)throw new IllegalStateException(glGetShaderInfoLog(shader));
            glAttachShader(program,shader);glDeleteShader(shader);
        }
        glLinkProgram(program);int[] success=new int[1];glGetProgramiv(program,GL_LINK_STATUS,success,0);
        if(success[0]==0)throw new IllegalStateException(glGetProgramInfoLog(program));return program;
    }
}
