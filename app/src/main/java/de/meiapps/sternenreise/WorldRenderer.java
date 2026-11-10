package de.meiapps.sternenreise;

import android.graphics.*;
import android.opengl.*;
import android.opengl.Matrix;
import android.os.SystemClock;
import java.nio.*;
import java.util.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import static android.opengl.GLES20.*;

/** Native 3D, parallel stereo cameras and per-eye radial lens correction. */
final class WorldRenderer implements GLSurfaceView.Renderer {
    interface Events { void activate(String id); void menu(); }
    private record Target(String id,String label,float x,float y,float z,int color) { }
    private record Solid(float x,float y,float z,float sx,float sy,float sz,int color,int shape) {
        Solid(float x,float y,float z,float sx,float sy,float sz,int color,boolean gem) { this(x,y,z,sx,sy,sz,color,gem?1:0); }
    }
    private final HeadTracker tracker;
    private final Events events;
    private final Gaze gaze = new Gaze();
    private final List<Solid> solids=new ArrayList<>();
    private final List<Target> targets=new ArrayList<>();
    private final Map<String,Integer> textTextures=new HashMap<>();
    private final Map<FloatBuffer,FloatBuffer> meshNormals=new IdentityHashMap<>();
    private Adventure game;
    private String message;
    private boolean stereo;
    private volatile float yaw,pitch;
    private float lens,eyeSeparation;
    private int width,height,eyeWidth,program,distortionProgram,fbo,eyeTexture,depth,whiteTexture;
    private int lastRoom=-1;
    private long fadeStart;
    private final float[] head=new float[16],projection=new float[16],view=new float[16],model=new float[16],mvp=new float[16],tmp=new float[16];
    private final FloatBuffer cube=buffer(new float[]{
        -1,-1,1, 1,-1,1, 1,1,1, -1,-1,1, 1,1,1, -1,1,1,
        1,-1,-1, -1,-1,-1, -1,1,-1, 1,-1,-1, -1,1,-1, 1,1,-1,
        -1,-1,-1, -1,-1,1, -1,1,1, -1,-1,-1, -1,1,1, -1,1,-1,
        1,-1,1, 1,-1,-1, 1,1,-1, 1,-1,1, 1,1,-1, 1,1,1,
        -1,1,1, 1,1,1, 1,1,-1, -1,1,1, 1,1,-1, -1,1,-1,
        -1,-1,-1, 1,-1,-1, 1,-1,1, -1,-1,-1, 1,-1,1, -1,-1,1});
    private final FloatBuffer gem=buffer(new float[]{0,1,0,-1,0,0,0,0,1, 0,1,0,0,0,1,1,0,0, 0,1,0,1,0,0,0,0,-1, 0,1,0,0,0,-1,-1,0,0, 0,-1,0,0,0,1,-1,0,0, 0,-1,0,1,0,0,0,0,1, 0,-1,0,0,0,-1,1,0,0, 0,-1,0,-1,0,0,0,0,-1});
    private final FloatBuffer star=symbolMesh(0),moon=symbolMesh(1),sun=symbolMesh(2);
    private final FloatBuffer quad=buffer(new float[]{-1,-1,0,1,-1,0,-1,1,0,1,1,0});
    private final FloatBuffer uv=buffer(new float[]{0,1,1,1,0,0,1,0});
    WorldRenderer(Adventure game,HeadTracker tracker,boolean stereo,float lens,float eyeSeparation,Events events) {
        this.game=game;this.tracker=tracker;this.stereo=stereo;this.lens=lens;this.eyeSeparation=eyeSeparation;this.events=events;message=game.hint();
    }
    void look(float dx,float dy) { yaw+=dx; pitch=Math.max(-75,Math.min(75,pitch+dy)); }
    void resetGaze() { gaze.reset(); }
    void refresh(String text) { message=text; rebuild(); }
    private static FloatBuffer buffer(float[] values) { FloatBuffer b=ByteBuffer.allocateDirect(values.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();b.put(values).position(0);return b; }
    @Override public void onSurfaceCreated(GL10 unused,EGLConfig config) {
        program=link("attribute vec3 a; attribute vec3 n; attribute vec2 t; uniform mat4 m; uniform float lit; varying vec2 uv; varying float light; void main(){gl_Position=m*vec4(a,1.);uv=t;light=mix(1.,.48+.52*max(0.,dot(n,normalize(vec3(-.3,.8,.5)))),lit);}",
            "precision mediump float; uniform vec4 c; uniform sampler2D s; uniform float textured; varying vec2 uv;varying float light;void main(){vec4 base=mix(c,texture2D(s,uv)*c,textured);gl_FragColor=vec4(base.rgb*light,base.a);}");
        distortionProgram=link("attribute vec3 a; varying vec2 p;void main(){gl_Position=vec4(a,1.);p=a.xy;}",
            "precision mediump float;varying vec2 p;uniform sampler2D s;uniform float k;uniform float fade;void main(){float r=dot(p,p);vec2 q=p*(1.+k*r);vec2 uv=(q+1.)*.5;if(abs(q.x)>1.||abs(q.y)>1.)gl_FragColor=vec4(0.,0.,0.,1.);else gl_FragColor=vec4(texture2D(s,uv).rgb*fade,1.);}");
        textTextures.clear();eyeTexture=0;fbo=0;depth=0;
        int[] white=new int[1];glGenTextures(1,white,0);whiteTexture=white[0];glBindTexture(GL_TEXTURE_2D,whiteTexture);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
        ByteBuffer pixel=ByteBuffer.allocateDirect(4);pixel.put(new byte[]{-1,-1,-1,-1}).position(0);
        glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,1,1,0,GL_RGBA,GL_UNSIGNED_BYTE,pixel);
        glEnable(GL_DEPTH_TEST); glEnable(GL_BLEND);glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
        rebuild();
    }
    @Override public void onSurfaceChanged(GL10 unused,int w,int h) {width=w;height=h;eyeWidth=stereo?w/2:w;allocateEye();}
    private void allocateEye() {
        if (eyeTexture!=0) {glDeleteTextures(1,new int[]{eyeTexture},0);glDeleteFramebuffers(1,new int[]{fbo},0);glDeleteRenderbuffers(1,new int[]{depth},0);}
        int[] id=new int[1];glGenTextures(1,id,0);eyeTexture=id[0];glBindTexture(GL_TEXTURE_2D,eyeTexture);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,eyeWidth,height,0,GL_RGBA,GL_UNSIGNED_BYTE,null);
        glGenRenderbuffers(1,id,0);depth=id[0];glBindRenderbuffer(GL_RENDERBUFFER,depth);glRenderbufferStorage(GL_RENDERBUFFER,GL_DEPTH_COMPONENT16,eyeWidth,height);
        glGenFramebuffers(1,id,0);fbo=id[0];glBindFramebuffer(GL_FRAMEBUFFER,fbo);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,eyeTexture,0);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_RENDERBUFFER,depth);
        if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE) throw new IllegalStateException("Eye framebuffer unavailable");
        glBindFramebuffer(GL_FRAMEBUFFER,0);
    }
    @Override public void onDrawFrame(GL10 unused) {
        if(width==0||height==0)return;
        long now=SystemClock.uptimeMillis();
        if(stereo) tracker.copyView(head); else {Matrix.setIdentityM(head,0);Matrix.rotateM(head,0,pitch,1,0,0);Matrix.rotateM(head,0,yaw,0,1,0);}
        float[] forward={-head[2],-head[6],-head[10]};
        Target focused=null;double best=-1;
        for(Target t:targets) if(Gaze.hits(forward,t.x,t.y,t.z,.145f)) {
            double score=(forward[0]*t.x+forward[1]*t.y+forward[2]*t.z)/Math.sqrt(t.x*t.x+t.y*t.y+t.z*t.z);
            if(score>best){best=score;focused=t;}
        }
        if(now-fadeStart>650 && gaze.update(focused==null?null:focused.id,now)) {
            String id=focused.id;
            // Callback runs on UI; state mutations return through queueEvent.
            events.activate(id);
        }
        float progress=gaze.progress(now);
        for(int eye=0;eye<(stereo?2:1);eye++) {
            glBindFramebuffer(GL_FRAMEBUFFER,fbo);glViewport(0,0,eyeWidth,height);glEnable(GL_DEPTH_TEST);
            glClearColor(.035f,.06f,.12f,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
            Matrix.perspectiveM(projection,0,stereo?82:65,(float)eyeWidth/height,.08f,40);
            Matrix.setIdentityM(view,0);if(stereo)Matrix.translateM(view,0,eye==0?eyeSeparation/2:-eyeSeparation/2,0,0);
            Matrix.multiplyMM(tmp,0,view,0,head,0);System.arraycopy(tmp,0,view,0,16);
            for(Solid s:solids) drawSolid(s);
            for(Target t:targets) drawLabel(t.label,t.x,t.y+.48f,t.z,.55f,.13f,Color.WHITE,false);
            // Head-locked panels sit at a comfortable finite distance in both eyes.
            Matrix.setIdentityM(view,0);if(stereo)Matrix.translateM(view,0,eye==0?eyeSeparation/2:-eyeSeparation/2,0,0);
            drawLabel(game.title(),0,.98f,-2.3f,1,.105f,0xfff9d78a,true);
            drawLabel(message,0,-.83f,-2.3f,1.05f,.26f,Color.WHITE,true);
            glDisable(GL_DEPTH_TEST);
            drawLabel("+",0,0,-2.3f,.035f,.035f,focused==null?Color.WHITE:0xff7ce9d9,true);
            if(focused!=null) {
                for(int i=0;i<32;i++) {
                    double angle=i*Math.PI*2/32;
                    drawBox((float)Math.sin(angle)*.055f,(float)Math.cos(angle)*.055f,-2.3f,.005f,.005f,.001f,i<progress*32?0xfff9d78a:0xff52657e,false);
                }
            }
            glBindFramebuffer(GL_FRAMEBUFFER,0);glViewport(stereo?eye*eyeWidth:0,0,eyeWidth,height);glDisable(GL_DEPTH_TEST);
            glUseProgram(distortionProgram);int a=glGetAttribLocation(distortionProgram,"a");glEnableVertexAttribArray(a);quad.position(0);glVertexAttribPointer(a,3,GL_FLOAT,false,0,quad);
            glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,eyeTexture);glUniform1i(glGetUniformLocation(distortionProgram,"s"),0);
            glUniform1f(glGetUniformLocation(distortionProgram,"k"),stereo?lens:0);
            float fade=Math.min(1,(now-fadeStart)/450f);glUniform1f(glGetUniformLocation(distortionProgram,"fade"),Math.max(0,fade));
            glDrawArrays(GL_TRIANGLE_STRIP,0,4);glDisableVertexAttribArray(a);
        }
    }
    private void rebuild() {
        for(int texture:textTextures.values())glDeleteTextures(1,new int[]{texture},0);textTextures.clear();
        solids.clear();targets.clear();
        if(lastRoom!=game.room().ordinal()) {fadeStart=SystemClock.uptimeMillis();lastRoom=game.room().ordinal();yaw=0;pitch=0;tracker.center();gaze.reset();}
        int wall=game.room()==Adventure.Room.GARDEN?0xff284d58:0xff253953;
        add(0,-1.75f,0,6,.1f,6,0xff665340);add(0,3.4f,0,6,.1f,6,0xff15233c);
        add(0,.8f,-5,6,2.5f,.12f,wall);add(-5,.8f,0,.12f,2.5f,5,wall);add(5,.8f,0,.12f,2.5f,5,wall);add(0,.8f,5,6,2.5f,.12f,wall);
        for(int i=-4;i<=4;i++){add(i,-1.63f,0,.018f,.012f,5,0xffa7895e);add(i,.8f,-4.8f,.055f,2.4f,.05f,0xff977351);}
        // Tiny stars and windows around the whole room reward looking around.
        Random random=new Random(77+game.room().ordinal());
        for(int i=0;i<45;i++){float x=random.nextFloat()*8-4,z=random.nextFloat()*8-4;add(x,2.7f+random.nextFloat()*.4f,z,.025f,.025f,.025f,0xffffdc94);}
        for(int side:new int[]{-1,1}) {add(side*4.82f,.7f,1,.04f,1.2f,1,0xff0e203b);add(side*4.75f,.7f,1,.06f,.045f,1,0xffc69b61);add(side*4.75f,.7f,1,.06f,1.2f,.045f,0xffc69b61);}
        robot(-2.3f,-.4f,-2.5f);
        target("hint","Lumi · Hilfe",-2.3f,.15f,-2.5f,0xff7ce9d9);
        target("menu","Pause",0,-1.0f,2.5f,0xffd3c3ab);add(0,-1.15f,2.5f,.45f,.13f,.13f,0xffd3c3ab);
        switch(game.room()) {
            case WORKSHOP -> {
                add(-1.5f,-.8f,-3.7f,1.2f,.12f,.55f,0xffac8054);add(-1.5f,.25f,-3.85f,1.2f,.08f,.4f,0xffac8054);
                for(int i=0;i<6;i++)add(-2.3f+i*.29f,-.35f,-3.8f,.1f,.4f,.18f,new int[]{0xff68c8bc,0xffeeae73,0xffbc9cd7}[i%3]);
                if(!game.hasKey()){target("key","Schlüssel",-1.2f,.6f,-3.4f,0xffffd477);add(-1.2f,.6f,-3.4f,.25f,.06f,.06f,0xffffd477);add(-1.4f,.6f,-3.4f,.08f,.16f,.06f,0xffffd477);}
                target("chest",game.hasSeed()?"Leere Truhe":"Kleine Truhe",.8f,-.45f,-2.6f,0xffdda65d);add(.8f,-.8f,-2.6f,.55f,.4f,.4f,0xffa26c46);add(.8f,-.4f,-2.6f,.57f,.08f,.42f,0xffe8b36b);
                door("garden","Mondgarten",3,.1f,-3.9f,0xff60c7ad);
            }
            case GARDEN -> {
                add(0,-1.5f,-3,2.1f,.1f,1.2f,0xff486b53);
                target("pot","Mondensamen",-.8f,-.5f,-2.7f,0xffe9a878);add(-.8f,-1.1f,-2.7f,.4f,.45f,.4f,0xffc4815c);
                target("water","Gießkanne",1.5f,-.5f,-2.6f,0xff7ccadd);add(1.5f,-.85f,-2.6f,.3f,.3f,.25f,0xff7ccadd);add(1.1f,-.65f,-2.6f,.3f,.06f,.06f,0xff7ccadd);
                if(game.watered()){add(-.8f,-.35f,-2.7f,.055f,.65f,.055f,0xff8dd8a0);target("flower",game.hasCrystal()?"Mondblume":"Sternenkristall",-.8f,.5f,-2.7f,0xff8ee5ff);solids.add(new Solid(-.8f,.5f,-2.7f,.34f,.45f,.34f,0xff8ee5ff,true));}
                door("back","Werkstatt",-3,.1f,-3.9f,0xffd6a46c);door("observatory","Sternwarte",3,.1f,-3.9f,0xff8baff3);
            }
            case OBSERVATORY -> {
                add(0,-1.35f,-3.3f,2,.2f,.7f,0xff8f7198);
                String[] ids={"star","moon","sun"},labels={"Stern","Mond","Sonne"};int[] colors={0xffffdc81,0xffb8c9ff,0xffffac76};
                for(int i=0;i<3;i++){float x=(i-1)*1.25f;target(ids[i],labels[i],x,-.35f,-3,colors[i]);solids.add(new Solid(x,-.35f,-3,.3f,.35f,.15f,game.symbols()>i?0xff85f3ca:colors[i],i+2));}
                target("machine",game.finished()?"Sternenlicht!":"Sternenmaschine",2.6f,.4f,-2.2f,0xffffd477);
                add(2.6f,-.9f,-2.2f,.55f,.65f,.45f,0xff667ba8);solids.add(new Solid(2.6f,.4f,-2.2f,.5f,.65f,.5f,game.finished()?0xffffedaa:0xff7999c5,true));
                if(game.finished())for(int i=0;i<20;i++){double a=i*Math.PI*2/20;solids.add(new Solid((float)Math.sin(a)*3.8f,1.7f,(float)Math.cos(a)*3.8f,.15f,.25f,.15f,0xffffdf85,true));}
                door("back","Mondgarten",-3,.1f,-3.9f,0xff60c7ad);
        }
        }
    }
    private void target(String id,String label,float x,float y,float z,int color){targets.add(new Target(id,label,x,y,z,color));}
    private void add(float x,float y,float z,float sx,float sy,float sz,int c){solids.add(new Solid(x,y,z,sx,sy,sz,c,false));}
    private void robot(float x,float y,float z){add(x,y-.4f,z,.3f,.35f,.22f,0xff62b9be);add(x,y+.15f,z,.4f,.28f,.25f,0xffe1d0a6);add(x-.16f,y+.17f,z+.26f,.065f,.065f,.02f,0xff234352);add(x+.16f,y+.17f,z+.26f,.065f,.065f,.02f,0xff234352);add(x,y+.55f,z,.025f,.12f,.025f,0xffffdc81);}
    private void door(String id,String label,float x,float y,float z,int color){target(id,label,x,y,z,color);add(x,y-.3f,z,.66f,1.2f,.12f,color);add(x+.42f,y-.3f,z+.15f,.07f,.07f,.05f,0xffffda86);}
    private void drawSolid(Solid s){
        if(s.shape<2){drawBox(s.x,s.y,s.z,s.sx,s.sy,s.sz,s.color,s.shape==1);return;}
        Matrix.setIdentityM(model,0);Matrix.translateM(model,0,s.x,s.y,s.z);Matrix.scaleM(model,0,s.sx,s.sy,s.sz);
        FloatBuffer mesh=s.shape==2?star:s.shape==3?moon:sun;draw(mesh,mesh.capacity()/3,s.color,0,false);
    }
    private void drawBox(float x,float y,float z,float sx,float sy,float sz,int c,boolean isGem){
        Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);Matrix.scaleM(model,0,sx,sy,sz);
        if(isGem)Matrix.rotateM(model,0,(SystemClock.uptimeMillis()%18000)/50f,0,1,0);
        draw(isGem?gem:cube,isGem?24:36,c,0,false);
    }
    private void drawLabel(String text,float x,float y,float z,float sx,float sy,int color,boolean hud) {
        Integer texture=textTextures.get(text);if(texture==null){texture=makeText(text);textTextures.put(text,texture);}
        Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);
        if(!hud){double angle=Math.atan2(x,z)+Math.PI;Matrix.rotateM(model,0,(float)Math.toDegrees(angle),0,1,0);}
        Matrix.scaleM(model,0,sx,sy,1);draw(quad,4,color,texture,true);
    }
    private void draw(FloatBuffer vertices,int count,int color,int texture,boolean strip){
        glUseProgram(program);Matrix.multiplyMM(tmp,0,view,0,model,0);Matrix.multiplyMM(mvp,0,projection,0,tmp,0);
        glUniformMatrix4fv(glGetUniformLocation(program,"m"),1,false,mvp,0);
        glUniform4f(glGetUniformLocation(program,"c"),Color.red(color)/255f,Color.green(color)/255f,Color.blue(color)/255f,1);
        int a=glGetAttribLocation(program,"a"),t=glGetAttribLocation(program,"t"),normal=glGetAttribLocation(program,"n");
        glUniform1f(glGetUniformLocation(program,"lit"),texture==0&&!strip?1:0);
        if(!strip){FloatBuffer normals=meshNormals.get(vertices);if(normals==null){normals=normals(vertices);meshNormals.put(vertices,normals);}glEnableVertexAttribArray(normal);normals.position(0);glVertexAttribPointer(normal,3,GL_FLOAT,false,0,normals);}else{glDisableVertexAttribArray(normal);glVertexAttrib3f(normal,0,0,1);}glEnableVertexAttribArray(a);vertices.position(0);glVertexAttribPointer(a,3,GL_FLOAT,false,0,vertices);
        glUniform1f(glGetUniformLocation(program,"textured"),texture==0?0:1);
        glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,texture==0?whiteTexture:texture);glUniform1i(glGetUniformLocation(program,"s"),0);
        if(texture!=0){glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,texture);glUniform1i(glGetUniformLocation(program,"s"),0);glEnableVertexAttribArray(t);uv.position(0);glVertexAttribPointer(t,2,GL_FLOAT,false,0,uv);}
        glDrawArrays(strip?GL_TRIANGLE_STRIP:GL_TRIANGLES,0,count);glDisableVertexAttribArray(a);glDisableVertexAttribArray(normal);if(texture!=0)glDisableVertexAttribArray(t);
    }
    private int makeText(String text){
        Bitmap b=Bitmap.createBitmap(1024,256,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xe619263d);c.drawRoundRect(2,2,1022,254,28,28,p);p.setColor(Color.WHITE);p.setTextSize(text.length()>55?34:44);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextAlign(Paint.Align.CENTER);
        List<String> lines=new ArrayList<>();String line="";for(String word:text.split(" ")){String next=line.isEmpty()?word:line+" "+word;if(p.measureText(next)>940){lines.add(line);line=word;}else line=next;}lines.add(line);
        float y=128-(lines.size()-1)*24;for(String l:lines){c.drawText(l,512,y+14,p);y+=48;}
        int[] id=new int[1];glGenTextures(1,id,0);glBindTexture(GL_TEXTURE_2D,id[0]);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);GLUtils.texImage2D(GL_TEXTURE_2D,0,b,0);b.recycle();return id[0];
    }
    private static FloatBuffer normals(FloatBuffer vertices){
        float[] data=new float[vertices.capacity()];
        for(int i=0;i+8<data.length;i+=9){
            float ax=vertices.get(i+3)-vertices.get(i),ay=vertices.get(i+4)-vertices.get(i+1),az=vertices.get(i+5)-vertices.get(i+2);
            float bx=vertices.get(i+6)-vertices.get(i),by=vertices.get(i+7)-vertices.get(i+1),bz=vertices.get(i+8)-vertices.get(i+2);
            float nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx,length=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
            if(length==0)length=1;for(int v=0;v<3;v++){data[i+v*3]=nx/length;data[i+v*3+1]=ny/length;data[i+v*3+2]=nz/length;}
        }
        return buffer(data);
    }
    private static FloatBuffer symbolMesh(int kind){
        java.util.ArrayList<Float> vertices=new java.util.ArrayList<>();
        int count=kind==0?10:kind==1?48:48;
        for(int i=0;i<count;i++){
            double a=Math.PI/2+i*2*Math.PI/count,b=Math.PI/2+(i+1)*2*Math.PI/count;
            if(kind==1){
                // Outer left arc and a shifted, inset arc meet at both crescent tips.
                a=Math.PI/3+i*(4*Math.PI/3)/count;b=Math.PI/3+(i+1)*(4*Math.PI/3)/count;
                float ox=(float)Math.cos(a),oy=(float)Math.sin(a),px=(float)Math.cos(b),py=(float)Math.sin(b);
                float ix=.5f+.72f*(ox-.5f),iy=oy,jx=.5f+.72f*(px-.5f),jy=py;
                addTriangle(vertices,ox,oy,px,py,ix,iy);addTriangle(vertices,px,py,jx,jy,ix,iy);
            }else{
                float ra=kind==0?(i%2==0?1:.43f):(i%4==0?1:.73f),rb=kind==0?((i+1)%2==0?1:.43f):((i+1)%4==0?1:.73f);
                addTriangle(vertices,0,0,(float)Math.cos(a)*ra,(float)Math.sin(a)*ra,(float)Math.cos(b)*rb,(float)Math.sin(b)*rb);
            }
        }
        float[] data=new float[vertices.size()];for(int i=0;i<data.length;i++)data[i]=vertices.get(i);return buffer(data);
    }
    private static void addTriangle(java.util.List<Float> data,float ax,float ay,float bx,float by,float cx,float cy){for(float v:new float[]{ax,ay,0,bx,by,0,cx,cy,0})data.add(v);}
    private static int link(String v,String f){int p=glCreateProgram();for(int type:new int[]{GL_VERTEX_SHADER,GL_FRAGMENT_SHADER}){int s=glCreateShader(type);glShaderSource(s,type==GL_VERTEX_SHADER?v:f);glCompileShader(s);int[] ok=new int[1];glGetShaderiv(s,GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(glGetShaderInfoLog(s));glAttachShader(p,s);glDeleteShader(s);}glLinkProgram(p);int[] ok=new int[1];glGetProgramiv(p,GL_LINK_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(glGetProgramInfoLog(p));return p;}
}
