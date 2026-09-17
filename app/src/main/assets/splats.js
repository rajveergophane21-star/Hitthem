/* Splat engine, lifted verbatim from index.html so the screen overlay draws
   exactly the same mess as the game. Pure canvas: every function takes a 2D
   context and board-space coordinates and touches nothing else. */
(function(global){
  'use strict';
  /* blob() adds vertices as things scale up so the wobble stops reading as
     polygon corners; the game drives this from its Size control, the overlay
     from its own throw scale. */
  var SZ=1;
  function rand(a,b){return a+Math.random()*(b-a);}
  function pick(a){return a[(Math.random()*a.length)|0];}
  function clamp(v,a,b){return v<a?a:v>b?b:v;}
  function rr(g,x,y,w,h,r){
    if(g.roundRect){g.beginPath();g.roundRect(x,y,w,h,r);return;}
    g.beginPath();g.moveTo(x+r,y);g.lineTo(x+w-r,y);g.quadraticCurveTo(x+w,y,x+w,y+r);
    g.lineTo(x+w,y+h-r);g.quadraticCurveTo(x+w,y+h,x+w-r,y+h);
    g.lineTo(x+r,y+h);g.quadraticCurveTo(x,y+h,x,y+h-r);
    g.lineTo(x,y+r);g.quadraticCurveTo(x,y,x+r,y);g.closePath();
  }
  var PAINTS=[202,262,318,14,45,152,338];
  /* ---------------- paint helpers ---------------- */
  function blob(g,cx,cy,r,n,wob,fill,alpha){
    var pts=[],i,a,rv;
    if(SZ>1)n=Math.min(30,Math.round(n*Math.sqrt(SZ)));
    for(i=0;i<n;i++){a=i/n*Math.PI*2;rv=r*(1+(Math.random()*2-1)*wob);
      pts.push([cx+Math.cos(a)*rv,cy+Math.sin(a)*rv]);}
    g.globalAlpha=alpha;g.fillStyle=fill;g.beginPath();
    g.moveTo((pts[n-1][0]+pts[0][0])/2,(pts[n-1][1]+pts[0][1])/2);
    for(i=0;i<n;i++){
      var c=pts[i],x=pts[(i+1)%n];
      g.quadraticCurveTo(c[0],c[1],(c[0]+x[0])/2,(c[1]+x[1])/2);
    }
    g.closePath();g.fill();g.globalAlpha=1;
  }

  /* ---------------- tomato ---------------- */
  var TOM=['#c9301a','#e14a2b','#a81f12','#ef6b3c','#b52818'];
  function splatTomato(g,x,y,sc){
    var R=60*sc,i,a,d;
    g.save();g.translate(x,y);
    g.globalCompositeOperation='multiply';
    blob(g,0,0,R*1.14,15,.32,'#5e0f08',.3);
    g.globalCompositeOperation='source-over';
    blob(g,0,0,R,14,.4,pick(TOM),.93);
    blob(g,rand(-9,9),rand(-9,9),R*.6,12,.42,'#8e1c12',.45);
    blob(g,rand(-13,13),rand(-15,5),R*.3,10,.5,'#f58a55',.5);
    for(i=0;i<13;i++){a=rand(0,6.283);d=R*rand(.95,2.3);
      blob(g,Math.cos(a)*d,Math.sin(a)*d,R*rand(.06,.24),8,.5,pick(TOM),.88);}
    for(i=0;i<10;i++){a=rand(0,6.283);d=R*rand(.2,.75);
      g.globalAlpha=.75;g.fillStyle='#ffe7b4';
      g.beginPath();g.ellipse(Math.cos(a)*d,Math.sin(a)*d,4.4*sc,2.8*sc,a,0,Math.PI*2);g.fill();}
    g.globalAlpha=.35;g.fillStyle='#ffd9b0';
    g.beginPath();g.ellipse(-R*.3,-R*.34,R*.22,R*.13,-.6,0,Math.PI*2);g.fill();
    g.globalAlpha=1;g.restore();
  }

  /* ---------------- egg ---------------- */
  function shard(g,cx,cy,r,rot){
    g.save();g.translate(cx,cy);g.rotate(rot);
    g.globalAlpha=.3;g.fillStyle='#000';
    g.beginPath();g.moveTo(-r+2.5,-r*.6+3.5);g.lineTo(r+2.5,-r*.3+3.5);
    g.lineTo(r*.6+2.5,r*.7+3.5);g.lineTo(-r*.7+2.5,r*.5+3.5);g.closePath();g.fill();
    g.globalAlpha=1;g.fillStyle='#f8f4ec';
    g.beginPath();g.moveTo(-r,-r*.6);g.lineTo(r,-r*.3);g.lineTo(r*.6,r*.7);g.lineTo(-r*.7,r*.5);g.closePath();g.fill();
    g.strokeStyle='rgba(120,110,95,.45)';g.lineWidth=1.2;g.stroke();
    g.fillStyle='rgba(255,255,255,.5)';
    g.beginPath();g.moveTo(-r*.8,-r*.45);g.lineTo(r*.5,-r*.2);g.lineTo(r*.4,-r*.02);g.lineTo(-r*.75,-r*.25);g.closePath();g.fill();
    g.restore();
  }
  function splatEgg(g,x,y,sc){
    var R=62*sc,i,a,d;
    g.save();g.translate(x,y);
    blob(g,0,0,R*1.26,16,.3,'#f3eee1',.2);
    blob(g,0,0,R,15,.38,'#f9f5ea',.64);
    blob(g,rand(-9,9),rand(-9,9),R*.68,13,.4,'#fffdf6',.62);
    for(i=0;i<11;i++){a=rand(0,6.283);d=R*rand(.95,2.05);
      blob(g,Math.cos(a)*d,Math.sin(a)*d,R*rand(.05,.2),9,.5,'#f9f5ea',.6);}
    var yx=rand(-12,12),yy=rand(-12,12);
    blob(g,yx,yy,R*.42,12,.26,'#c8860d',.9);
    blob(g,yx,yy,R*.34,12,.22,'#f3b427',.96);
    blob(g,yx-R*.09,yy-R*.1,R*.12,9,.3,'#ffdf84',.8);
    for(i=0;i<6;i++){a=rand(0,6.283);d=R*rand(.7,1.75);
      shard(g,Math.cos(a)*d,Math.sin(a)*d,rand(6,13)*sc,rand(0,6.283));}
    g.globalAlpha=.55;g.fillStyle='#fff';
    g.beginPath();g.ellipse(-R*.34,-R*.36,R*.22,R*.11,-.6,0,Math.PI*2);g.fill();
    g.beginPath();g.ellipse(R*.3,R*.22,R*.13,R*.06,.5,0,Math.PI*2);g.fill();
    g.globalAlpha=1;g.restore();
  }

  /* ---------------- paint balloon ---------------- */
  function splatPaint(g,x,y,sc,h){
    var R=58*sc,i,a,d;
    var c1='hsl('+h+',86%,53%)',c2='hsl('+h+',88%,35%)',c3='hsl('+h+',92%,70%)',c0='hsl('+h+',78%,20%)';
    g.save();g.translate(x,y);
    g.globalCompositeOperation='multiply';
    blob(g,0,0,R*1.1,15,.3,c0,.26);
    g.globalCompositeOperation='source-over';
    blob(g,0,0,R,14,.42,c1,.95);
    blob(g,rand(-9,9),rand(-9,9),R*.62,12,.4,c2,.5);
    blob(g,rand(-13,13),rand(-15,4),R*.34,10,.45,c3,.6);
    for(i=0;i<16;i++){a=rand(0,6.283);d=R*rand(.9,2.6);
      blob(g,Math.cos(a)*d,Math.sin(a)*d,R*rand(.05,.26),8,.5,Math.random()<.35?c3:c1,.9);}
    g.globalAlpha=.45;g.fillStyle='#fff';
    g.beginPath();g.ellipse(-R*.3,-R*.34,R*.2,R*.11,-.6,0,Math.PI*2);g.fill();
    g.globalAlpha=1;g.restore();
  }
  function flyBalloon(g,x,y,sc,spin,h){
    g.save();g.translate(x,y);g.rotate(spin*.35);
    g.fillStyle='rgba(0,0,0,.3)';g.beginPath();g.ellipse(4*sc,5*sc,18*sc,22*sc,0,0,Math.PI*2);g.fill();
    var gr=g.createRadialGradient(-7*sc,-10*sc,2,0,0,25*sc);
    gr.addColorStop(0,'hsl('+h+',95%,78%)');gr.addColorStop(.5,'hsl('+h+',88%,53%)');gr.addColorStop(1,'hsl('+h+',85%,28%)');
    g.fillStyle=gr;g.beginPath();g.ellipse(0,-2*sc,16*sc,19*sc,0,0,Math.PI*2);g.fill();
    g.fillStyle='hsl('+h+',80%,30%)';
    g.beginPath();g.moveTo(-4*sc,16*sc);g.lineTo(4*sc,16*sc);g.lineTo(2.2*sc,23*sc);g.lineTo(-2.2*sc,23*sc);g.closePath();g.fill();
    g.fillStyle='rgba(255,255,255,.5)';
    g.beginPath();g.ellipse(-6*sc,-9*sc,4.5*sc,6.5*sc,-.35,0,Math.PI*2);g.fill();
    g.restore();
  }

  /* ---------------- rock ---------------- */
  function polyPath(g,poly,r,ox,oy){
    g.beginPath();
    for(var i=0;i<poly.length;i++){
      var a=i/poly.length*6.283;
      if(i)g.lineTo(ox+Math.cos(a)*r*poly[i],oy+Math.sin(a)*r*poly[i]);
      else g.moveTo(ox+Math.cos(a)*r*poly[i],oy+Math.sin(a)*r*poly[i]);
    }
    g.closePath();
  }
  function makePoly(){
    var p=[],i;for(i=0;i<8;i++)p.push(rand(.72,1.2));return p;
  }
  function flyRock(g,x,y,sc,spin,poly){
    g.save();g.translate(x,y);g.rotate(spin);
    g.fillStyle='rgba(0,0,0,.32)';polyPath(g,poly,19*sc,4*sc,5*sc);g.fill();
    var gr=g.createLinearGradient(-16*sc,-16*sc,14*sc,16*sc);
    gr.addColorStop(0,'#9ba1a5');gr.addColorStop(.45,'#6d7376');gr.addColorStop(1,'#393e40');
    g.fillStyle=gr;polyPath(g,poly,18*sc,0,0);g.fill();
    g.strokeStyle='rgba(18,20,22,.6)';g.lineWidth=1.7*sc;g.stroke();
    g.fillStyle='rgba(255,255,255,.16)';polyPath(g,poly,10*sc,-4.5*sc,-4.5*sc);g.fill();
    g.restore();
  }
  /* one fracture: wide at the hit, tapering to a hairline */
  function crackArm(g,pts,w0,fill,off){
    var i,L=pts.length,left=[],right=[],prev=[0,0],p,dx,dy,m,nx,ny,w;
    for(i=0;i<L;i++){
      p=pts[i];dx=p[0]-prev[0];dy=p[1]-prev[1];
      m=Math.sqrt(dx*dx+dy*dy)||1;nx=-dy/m;ny=dx/m;
      w=w0*(1-p[2]*.94);
      left.push([p[0]+nx*w,p[1]+ny*w]);
      right.push([p[0]-nx*w,p[1]-ny*w]);
      prev=p;
    }
    g.save();
    if(off)g.translate(-off,-off);
    g.beginPath();g.moveTo(0,0);
    for(i=0;i<L;i++)g.lineTo(left[i][0],left[i][1]);
    for(i=L-1;i>=0;i--)g.lineTo(right[i][0],right[i][1]);
    g.closePath();g.fillStyle=fill;g.fill();
    g.restore();
  }

  function smashRock(g,x,y,sc){
    var TAU=6.28318,n=5+(Math.random()*4|0),arms=[],i,j,a,aa,d,len,steps,pts,r,A,B,p1,p2;
    for(i=0;i<n;i++){
      a=i/n*TAU+rand(-.38,.38);
      len=rand(58,196)*sc;
      steps=3+(Math.random()*3|0);
      pts=[];
      for(j=1;j<=steps;j++){
        d=len*Math.pow(j/steps,.9);
        aa=a+rand(-.26,.26);
        pts.push([Math.cos(aa)*d,Math.sin(aa)*d,j/steps]);
      }
      arms.push(pts);
    }
    g.save();g.translate(x,y);

    /* the surface dips in around the hit */
    var bg=g.createRadialGradient(0,0,4*sc,0,0,48*sc);
    bg.addColorStop(0,'rgba(0,0,0,.36)');bg.addColorStop(.48,'rgba(0,0,0,.15)');
    bg.addColorStop(1,'rgba(0,0,0,0)');
    g.fillStyle=bg;g.beginPath();g.arc(0,0,48*sc,0,Math.PI*2);g.fill();

    /* each fracture: a lit edge behind, the split itself on top */
    for(i=0;i<n;i++){
      crackArm(g,arms[i],4.8*sc,'rgba(255,244,224,.22)',1.7*sc);
      crackArm(g,arms[i],4.8*sc,'rgba(7,5,4,.74)',0);
    }

    /* the odd splinter running between two fractures - never a full ring */
    g.lineCap='round';
    for(i=0;i<n;i++){
      if(Math.random()<.55)continue;
      A=arms[i];B=arms[(i+1)%n];
      p1=A[(Math.random()*A.length)|0];
      p2=B[(Math.random()*B.length)|0];
      g.strokeStyle='rgba(7,5,4,.5)';g.lineWidth=rand(.8,1.9)*sc;
      g.beginPath();g.moveTo(p1[0],p1[1]);
      g.quadraticCurveTo((p1[0]+p2[0])*.4,(p1[1]+p2[1])*.4,p2[0],p2[1]);
      g.stroke();
    }

    /* chips lifted out of the surface */
    for(i=0;i<5;i++){
      a=rand(0,TAU);d=rand(4,19)*sc;r=rand(4,10)*sc;
      g.save();g.translate(Math.cos(a)*d,Math.sin(a)*d);g.rotate(rand(0,TAU));
      g.fillStyle='rgba(255,246,228,.2)';
      g.beginPath();g.moveTo(-r,-r*.5);g.lineTo(r*.9,-r*.25);g.lineTo(r*.35,r*.7);g.closePath();g.fill();
      g.restore();
    }

    blob(g,0,0,12*sc,8,.5,'#0a0807',.84);
    blob(g,rand(-3,3),rand(-3,3),6.5*sc,7,.55,'#2b2421',.7);
    g.fillStyle='rgba(226,219,206,.45)';
    for(i=0;i<14;i++){a=rand(0,TAU);d=rand(18,122)*sc;
      g.beginPath();g.arc(Math.cos(a)*d,Math.sin(a)*d,rand(1,3.2)*sc,0,Math.PI*2);g.fill();}
    g.restore();
  }

  /* ---------------- snowball ---------------- */
  function splatSnow(g,x,y,sc){
    var R=66*sc,i,a,d;
    g.save();g.translate(x,y);
    for(i=0;i<22;i++){a=rand(0,6.283);d=R*rand(0,1.5);
      blob(g,Math.cos(a)*d,Math.sin(a)*d,R*rand(.12,.4),9,.45,'#ffffff',rand(.24,.55));}
    for(i=0;i<5;i++){a=rand(0,6.283);d=R*rand(.15,.8);
      blob(g,Math.cos(a)*d,Math.sin(a)*d,R*rand(.2,.38),10,.4,'#ffffff',.9);}
    blob(g,0,0,R*.5,12,.4,'#ffffff',.96);
    blob(g,rand(-7,7),rand(-7,7),R*.34,10,.4,'#ffffff',1);
    g.fillStyle='rgba(244,250,255,.85)';
    for(i=0;i<26;i++){a=rand(0,6.283);d=R*rand(.6,2.3);
      g.beginPath();g.arc(Math.cos(a)*d,Math.sin(a)*d,rand(1.5,5)*sc,0,Math.PI*2);g.fill();}
    g.globalAlpha=.14;g.fillStyle='#a8c4de';
    g.beginPath();g.ellipse(4*sc,8*sc,R*.42,R*.2,0,0,Math.PI*2);g.fill();
    g.globalAlpha=1;g.restore();
  }
  function flySnow(g,x,y,sc,spin){
    var i;
    g.save();g.translate(x,y);g.rotate(spin*.5);
    g.fillStyle='rgba(0,0,0,.22)';g.beginPath();g.arc(4*sc,4*sc,18*sc,0,Math.PI*2);g.fill();
    var gr=g.createRadialGradient(-6*sc,-8*sc,2,0,0,20*sc);
    gr.addColorStop(0,'#ffffff');gr.addColorStop(.6,'#eef4fa');gr.addColorStop(1,'#b5c8da');
    g.fillStyle=gr;g.beginPath();g.arc(0,0,17*sc,0,Math.PI*2);g.fill();
    g.fillStyle='rgba(255,255,255,.7)';
    for(i=0;i<5;i++){g.beginPath();g.arc(rand(-10,10)*sc,rand(-10,10)*sc,rand(2,4)*sc,0,Math.PI*2);g.fill();}
    g.restore();
  }


  global.Splats={rand:rand,pick:pick,clamp:clamp,PAINTS:PAINTS,
    setScale:function(v){SZ=v||1;},
    tomato:splatTomato,egg:splatEgg,paint:splatPaint,rock:smashRock,snow:splatSnow};
})(this);
