import processing.core.*; 
import processing.xml.*; 

import hypermedia.net.*; 
import java.lang.reflect.Method; 

import java.applet.*; 
import java.awt.Dimension; 
import java.awt.Frame; 
import java.awt.event.MouseEvent; 
import java.awt.event.KeyEvent; 
import java.awt.event.FocusEvent; 
import java.awt.Image; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class hardsummer extends PApplet {




// Configuration
String ADDRESS = "192.168.1.130";
int PORT = 58082;
int FONT_SIZE = 48;
int SPACING = FONT_SIZE;
int MODE_TIME = 30;
String[] enabledModes = new String[] {
  "drawPurpleBlueCrawl",
  "drawColorCrawlByWord",
  "drawColorCrawlByLetter",
  "drawFastColorCrawlByLetter",
  "drawShootingColor",
  "drawRandomColors",
  "drawFadingWords"
};

int PURPLE = 0xffE741F3;
int BLUE = 0xff23D7EC;
int GREEN = 0xff00FB78;
int PINK = 0xffFB59BB;
int[] PALETTE = new int[] { PURPLE, BLUE, GREEN, PINK };

// Calculated constants
int LETTERS = "HARD SUMMER".length();
int WIDTH = (FONT_SIZE+SPACING)*(LETTERS+1);
int HEIGHT = PApplet.parseInt(WIDTH/1.777778f);
int STARTING_OFFSET = (LETTERS/2)*(FONT_SIZE+SPACING)*-1;
int Y_AXIS = 1;
int X_AXIS = 2;

// Instance variables
PFont font;
int[] values = new int[LETTERS];
int[] fixedGradient = new int[100];
String mode = null;
Method currentModeMethod = null;
long modeStartFrame = 0;
int iValue;  // Use incrementIEvery to cycle through 0-11 (number of letters)
int jValue;  // Use incrementJEvery to cycle through 0-100
float[] rValues = new float[LETTERS];
int[] rc1Values = new int[LETTERS];
int[] rc2Values = new int[LETTERS];
byte[] buffer = new byte[257]; //LETTERS*3+1];
UDP udp;

public void drawPurpleBlueCrawl() {
  incrementJEvery(1,1);
  mirroredFixedLetterGradient(0,LETTERS,jValue,100,PURPLE,BLUE);
}

public void drawColorCrawlByWord() {
  incrementJEvery(1,1);
  mirroredFixedLetterGradient(0,4,jValue,100,PURPLE,BLUE);
  mirroredFixedLetterGradient(5,6,jValue,100,PINK,GREEN);
}

public void drawColorCrawlByLetter() {
  incrementJEvery(1,1);
  
  for (int i=0; i<LETTERS; i++) {
    mirroredFixedLetterGradient(i,1,jValue,100,rc1Values[i],rc2Values[i]);
  }
}

public void drawFastColorCrawlByLetter() {
  incrementJEvery(1,5);
  
  for (int i=0; i<LETTERS; i++) {
    mirroredFixedLetterGradient(i,1,jValue,100,rc1Values[i],rc2Values[i]);
  }
}

public void drawShootingColor() {
  incrementIEvery(100);
  incrementJEvery(1,1);
  
  int j = iValue + 1;
  if (j >= rc1Values.length) { j = j - rc1Values.length; }
  
  int c1 = color(0);
  int c2a = rc1Values[iValue];
  int c2b = rc1Values[j];
  int c2 = getGradient(jValue,100,c2a,c2b);
  
  mirroredFixedLetterGradient(0,LETTERS,jValue,100,c1,c2);
}

public void drawRandomColors() {
  int j;
  
  if (frameCount % 4 == 0) {
    for (int i=0; i<values.length; i++) {
      j = PApplet.parseInt(random(PALETTE.length*8));
      
      values[i] = (j<PALETTE.length) ? PALETTE[PApplet.parseInt(random(PALETTE.length))] : color(0);
    }
  }
}

public void drawFadingWords() {
  incrementIEvery(100);
  incrementJEvery(1,1);
  
  int j = iValue + 1;
  if (j >= rc1Values.length) { j = j - rc1Values.length; }
  
  int c1a = rc2Values[iValue];
  int c1b = rc2Values[j];
  int c2a = rc1Values[iValue];
  int c2b = rc1Values[j];
  int c1 = getGradient(jValue,100,c1a,c1b);
  int c2 = getGradient(jValue,100,c2a,c2b);
  
  mirroredFixedLetterGradient(0,1,jValue,100,c1,c2);
  mirroredFixedLetterGradient(1,1,jValue,100,c1,c2);
  mirroredFixedLetterGradient(2,1,jValue,100,c1,c2);
  mirroredFixedLetterGradient(3,1,jValue,100,c1,c2);

  c1a = rc1Values[iValue];
  c1b = rc1Values[j];
  c2a = rc2Values[iValue];
  c2b = rc2Values[j];
  c1 = getGradient(jValue,100,c1a,c1b);
  c2 = getGradient(jValue,100,c2a,c2b);

  mirroredFixedLetterGradient(5,1,jValue,100,c1,c2);
  mirroredFixedLetterGradient(6,1,jValue,100,c1,c2);
  mirroredFixedLetterGradient(7,1,jValue,100,c1,c2);
  mirroredFixedLetterGradient(8,1,jValue,100,c1,c2);
  mirroredFixedLetterGradient(9,1,jValue,100,c1,c2);
  mirroredFixedLetterGradient(10,1,jValue,100,c1,c2);
}

public void setup() {
  size(WIDTH, HEIGHT);

  for(int i=0; i<values.length; i++) {
    values[i] = color(0);
  }

  font = loadFont("Disorient-" + FONT_SIZE + ".vlw");
  textFont(font, FONT_SIZE);
  textAlign(CENTER, CENTER);

  udp = new UDP(this);
  
  drawBackground();
  resetBuffer();
  newMode();
}

public void resetBuffer() {
  for (int i=0; i<buffer.length; i++) {
    buffer[i] = 0;
  }
}

public void loadBuffer() {
  int j;
  buffer[0] = 1;
  
  for (int i=0; i<values.length; i++) {
    j = i*3 + 1;
    
    buffer[j] = PApplet.parseByte(red(values[i]));
    buffer[j+1] = PApplet.parseByte(green(values[i]));
    buffer[j+2] = PApplet.parseByte(blue(values[i]));
  }
}

public void sendData() {
  loadBuffer();
  udp.send(buffer,ADDRESS,PORT);
}

public void resetFixedGradient() {
  for (int i=0; i<fixedGradient.length; i++) {
    fixedGradient[i] = 0;
  }
}

public void drawBackground() {
  int sky1, sky2, sky3;

  sky1 = color(0, 0, 0);
  sky2 = color(0, 0, 50);
  sky3 = color(0, 0, 100);
  int ply1 = color(30, 10, 10);
  int ply2 = color(130, 100, 100);
  int skyHeight = PApplet.parseInt(HEIGHT*0.66667f);
  int sky1Height = PApplet.parseInt(skyHeight*0.56f);
  int sky2Height = skyHeight-sky1Height;
  int plyHeight = HEIGHT-skyHeight;

  setGradient(0, 0, WIDTH, sky1Height, sky1, sky2, Y_AXIS);
  fill(sky2); 
  noStroke(); 
  rect(0, sky1Height, WIDTH, 80);
  setGradient(0, sky1Height, WIDTH, sky2Height, sky2, sky3, Y_AXIS);
  
  setGradient(0, skyHeight, WIDTH, plyHeight, ply1, ply2, Y_AXIS);

  for (int i=0; i<100; i++) {
    int x = (int)random(WIDTH);
    int y = (int)random(skyHeight);
    fill(random(100));
    rect(x, y, 2, 2);
  }
}

public void letter(int position, String letter) {
  int offset = STARTING_OFFSET + (position * (FONT_SIZE+SPACING));

  fill(200);
  for (int x=-5; x<=5; x+=5) {
    for (int y=-5; y<=5; y+=5) {
      text(letter, width/2+offset+x, height/2+y);
    }
  }

  fill(values[position]);
  text(letter, width/2+offset, height/2);
}

public void newMode() {
  int modeIndex = enabledModes.length > 1 ? PApplet.parseInt(random(enabledModes.length)) : 0;
  
  mode = enabledModes[modeIndex];
  
  try {
    currentModeMethod = this.getClass().getDeclaredMethod(mode, new Class[] {});
  }
  catch (Exception e) {};
  
  iValue = jValue = 0;
  modeStartFrame = frameCount;
  resetFixedGradient();
  
  for (int i=0; i<rValues.length; i++) {
    rValues[i] = random(1);
    rc1Values[i] = PALETTE[PApplet.parseInt(random(PALETTE.length))];
    rc2Values[i] = PALETTE[PApplet.parseInt(random(PALETTE.length))];
    
    while (rc1Values[i] == rc2Values[i]) {
      rc2Values[i] = PALETTE[PApplet.parseInt(random(PALETTE.length))];
    }
  } 
}

public void draw() {
  if (currentModeMethod != null) {
    try {
      currentModeMethod.invoke(this);
    }
    catch (Exception e) { e.printStackTrace(); }
  }
  else {
    println("Current method is null.");
  }
  
  letter(0, "H");
  letter(1, "A");
  letter(2, "R");
  letter(3, "D");
  
  letter(5, "S");
  letter(6, "U");
  letter(7, "M");
  letter(8, "M");
  letter(9, "E");
  letter(10, "R");
  
  sendData();
  
  if (frameCount > modeStartFrame + MODE_TIME*frameRate) {
    println("New mode");
    newMode();
  }
}

public void incrementJEvery(int mod, int by) {
  if (frameCount % mod == 0) {
    jValue+=by;
    
    if (jValue >= 100) { jValue = 0; }
  }
}

public void incrementIEvery(int mod) {
  if (frameCount % mod == 0) {
    iValue++;
    
    if (iValue >= LETTERS) { iValue = 0; }
  }
}

public int getGradient(int step, int steps, int c1, int c2) {
  float deltaR = red(c2)-red(c1);
  float deltaG = green(c2)-green(c1);
  float deltaB = blue(c2)-blue(c1);

  int c = color(
    (red(c1)+step*(deltaR/steps)), 
    (green(c1)+step*(deltaG/steps)), 
    (blue(c1)+step*(deltaB/steps)) 
  );

  return c;
}

public void fixedGradient(int step, int steps, int offset, int c1, int c2) {
  // calculate differences between color components 
  float deltaR = red(c2)-red(c1);
  float deltaG = green(c2)-green(c1);
  float deltaB = blue(c2)-blue(c1);
  int k;

  for (int i=0; i<steps; i++) {
    k = (i>=step) ? i-step : step+i;
    
    int c = color(
      (red(c1)+k*(deltaR/steps)), 
      (green(c1)+k*(deltaG/steps)), 
      (blue(c1)+k*(deltaB/steps)) 
    );
    fill(c);
    rect((i+offset)*5,0,5,5);
    fixedGradient[i+offset] = c;
  }
}

public void mirroredFixedGradient(int step, int steps, int c1, int c2) {
  // calculate differences between color components 
  float deltaR = red(c2)-red(c1);
  float deltaG = green(c2)-green(c1);
  float deltaB = blue(c2)-blue(c1);
  int s2 = steps/2;
  int j;
  
  for (int i=0; i<s2; i++) {
    j=i+step;
    if (j>=steps) { j=j-steps; }
    
    int c = color(
      (red(c1)+i*(deltaR/s2)), 
      (green(c1)+i*(deltaG/s2)), 
      (blue(c1)+i*(deltaB/s2)) 
    );
    fill(c);
    rect(j*5,0,5,5);
    fixedGradient[j] = c;
  }

  for (int i=s2; i>0; i--) {
    j=(s2-i)+s2+step;
    if (j>=steps) { j=j-steps; }
    
    int c = color(
      (red(c1)+i*(deltaR/s2)), 
      (green(c1)+i*(deltaG/s2)), 
      (blue(c1)+i*(deltaB/s2)) 
    );
    fill(c);
    rect(j*5,0,5,5);
    fixedGradient[j] = c;
  }

}

public void applyFixedGradient(int x, int w) {
  int j;
  for (int i=0; i<w; i++) {
    j = PApplet.parseInt(i*1.0f/w*100+5);
    values[i+x] = fixedGradient[j];
    fill(255,0,0);
    rect(j*5,5,1,5);
  }
}

public void mirroredFixedLetterGradient(int x, int w, int step, int steps, int c1, int c2) {
  mirroredFixedGradient(step,steps,c1,c2);
  applyFixedGradient(x, w);  
}

public void fixedLetterGradient(int x, int w, int step, int steps, int c1, int c2) {
  fixedGradient(step,steps,0,c1,c2);
  applyFixedGradient(x, w);
}

public void letterGradient(int x, int w, int c1, int c2) {
  // calculate differences between color components 
  float deltaR = red(c2)-red(c1);
  float deltaG = green(c2)-green(c1);
  float deltaB = blue(c2)-blue(c1);
  int j;

  for (int i=x; i<(x+w); i++) {
    int c = color(
      (red(c1)+(i-x)*(deltaR/w)), 
      (green(c1)+(i-x)*(deltaG/w)), 
      (blue(c1)+(i-x)*(deltaB/w)) 
    );

    j = i;
    while (j >= LETTERS) { j = j - LETTERS; }
    values[j] = c;
  }
}

public void setGradient(int x, int y, float w, float h, int c1, int c2, int axis ) {
  // calculate differences between color components 
  float deltaR = red(c2)-red(c1);
  float deltaG = green(c2)-green(c1);
  float deltaB = blue(c2)-blue(c1);

  // choose axis
  if (axis == Y_AXIS) {
    /*nested for loops set pixels
     in a basic table structure */
    // column
    for (int i=x; i<=(x+w); i++) {
      // row
      for (int j = y; j<=(y+h); j++) {
        int c = color(
        (red(c1)+(j-y)*(deltaR/h)), 
        (green(c1)+(j-y)*(deltaG/h)), 
        (blue(c1)+(j-y)*(deltaB/h)) 
          );
        set(i, j, c);
      }
    }
  }  
  else if (axis == X_AXIS) {
    // column 
    for (int i=y; i<=(y+h); i++) {
      // row
      for (int j = x; j<=(x+w); j++) {
        int c = color(
        (red(c1)+(j-x)*(deltaR/h)), 
        (green(c1)+(j-x)*(deltaG/h)), 
        (blue(c1)+(j-x)*(deltaB/h)) 
          );
        set(j, i, c);
      }
    }
  }
}


  static public void main(String args[]) {
    PApplet.main(new String[] { "--bgcolor=#c0c0c0", "hardsummer" });
  }
}
