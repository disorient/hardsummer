import hypermedia.net.*;
import java.lang.reflect.Method;

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

color PURPLE = #E741F3;
color BLUE = #23D7EC;
color GREEN = #00FB78;
color PINK = #FB59BB;
color[] PALETTE = new color[] { PURPLE, BLUE, GREEN, PINK };

// Calculated constants
int LETTERS = "HARD SUMMER".length();
int WIDTH = (FONT_SIZE+SPACING)*(LETTERS+1);
int HEIGHT = int(WIDTH/1.777778);
int STARTING_OFFSET = (LETTERS/2)*(FONT_SIZE+SPACING)*-1;
int Y_AXIS = 1;
int X_AXIS = 2;

// Instance variables
PFont font;
color[] values = new color[LETTERS];
color[] fixedGradient = new color[100];
String mode = null;
Method currentModeMethod = null;
long modeStartFrame = 0;
int iValue;  // Use incrementIEvery to cycle through 0-11 (number of letters)
int jValue;  // Use incrementJEvery to cycle through 0-100
float[] rValues = new float[LETTERS];
color[] rc1Values = new color[LETTERS];
color[] rc2Values = new color[LETTERS];
byte[] buffer = new byte[257]; //LETTERS*3+1];
UDP udp;

void drawPurpleBlueCrawl() {
  incrementJEvery(1,1);
  mirroredFixedLetterGradient(0,LETTERS,jValue,100,PURPLE,BLUE);
}

void drawColorCrawlByWord() {
  incrementJEvery(1,1);
  mirroredFixedLetterGradient(0,4,jValue,100,PURPLE,BLUE);
  mirroredFixedLetterGradient(5,6,jValue,100,PINK,GREEN);
}

void drawColorCrawlByLetter() {
  incrementJEvery(1,1);
  
  for (int i=0; i<LETTERS; i++) {
    mirroredFixedLetterGradient(i,1,jValue,100,rc1Values[i],rc2Values[i]);
  }
}

void drawFastColorCrawlByLetter() {
  incrementJEvery(1,5);
  
  for (int i=0; i<LETTERS; i++) {
    mirroredFixedLetterGradient(i,1,jValue,100,rc1Values[i],rc2Values[i]);
  }
}

void drawShootingColor() {
  incrementIEvery(100);
  incrementJEvery(1,1);
  
  int j = iValue + 1;
  if (j >= rc1Values.length) { j = j - rc1Values.length; }
  
  color c1 = color(0);
  color c2a = rc1Values[iValue];
  color c2b = rc1Values[j];
  color c2 = getGradient(jValue,100,c2a,c2b);
  
  mirroredFixedLetterGradient(0,LETTERS,jValue,100,c1,c2);
}

void drawRandomColors() {
  int j;
  
  if (frameCount % 4 == 0) {
    for (int i=0; i<values.length; i++) {
      j = int(random(PALETTE.length*8));
      
      values[i] = (j<PALETTE.length) ? PALETTE[int(random(PALETTE.length))] : color(0);
    }
  }
}

void drawFadingWords() {
  incrementIEvery(100);
  incrementJEvery(1,1);
  
  int j = iValue + 1;
  if (j >= rc1Values.length) { j = j - rc1Values.length; }
  
  color c1a = rc2Values[iValue];
  color c1b = rc2Values[j];
  color c2a = rc1Values[iValue];
  color c2b = rc1Values[j];
  color c1 = getGradient(jValue,100,c1a,c1b);
  color c2 = getGradient(jValue,100,c2a,c2b);
  
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

void setup() {
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

void resetBuffer() {
  for (int i=0; i<buffer.length; i++) {
    buffer[i] = 0;
  }
}

void loadBuffer() {
  int j;
  buffer[0] = 1;
  
  for (int i=0; i<values.length; i++) {
    j = i*3 + 1;
    
    buffer[j] = byte(red(values[i]));
    buffer[j+1] = byte(green(values[i]));
    buffer[j+2] = byte(blue(values[i]));
  }
}

void sendData() {
  loadBuffer();
  udp.send(buffer,ADDRESS,PORT);
}

void resetFixedGradient() {
  for (int i=0; i<fixedGradient.length; i++) {
    fixedGradient[i] = 0;
  }
}

void drawBackground() {
  color sky1, sky2, sky3;

  sky1 = color(0, 0, 0);
  sky2 = color(0, 0, 50);
  sky3 = color(0, 0, 100);
  color ply1 = color(30, 10, 10);
  color ply2 = color(130, 100, 100);
  int skyHeight = int(HEIGHT*0.66667);
  int sky1Height = int(skyHeight*0.56);
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

void letter(int position, String letter) {
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

void newMode() {
  int modeIndex = enabledModes.length > 1 ? int(random(enabledModes.length)) : 0;
  
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
    rc1Values[i] = PALETTE[int(random(PALETTE.length))];
    rc2Values[i] = PALETTE[int(random(PALETTE.length))];
    
    while (rc1Values[i] == rc2Values[i]) {
      rc2Values[i] = PALETTE[int(random(PALETTE.length))];
    }
  } 
}

void draw() {
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

void incrementJEvery(int mod, int by) {
  if (frameCount % mod == 0) {
    jValue+=by;
    
    if (jValue >= 100) { jValue = 0; }
  }
}

void incrementIEvery(int mod) {
  if (frameCount % mod == 0) {
    iValue++;
    
    if (iValue >= LETTERS) { iValue = 0; }
  }
}

color getGradient(int step, int steps, color c1, color c2) {
  float deltaR = red(c2)-red(c1);
  float deltaG = green(c2)-green(c1);
  float deltaB = blue(c2)-blue(c1);

  color c = color(
    (red(c1)+step*(deltaR/steps)), 
    (green(c1)+step*(deltaG/steps)), 
    (blue(c1)+step*(deltaB/steps)) 
  );

  return c;
}

void fixedGradient(int step, int steps, int offset, color c1, color c2) {
  // calculate differences between color components 
  float deltaR = red(c2)-red(c1);
  float deltaG = green(c2)-green(c1);
  float deltaB = blue(c2)-blue(c1);
  int k;

  for (int i=0; i<steps; i++) {
    k = (i>=step) ? i-step : step+i;
    
    color c = color(
      (red(c1)+k*(deltaR/steps)), 
      (green(c1)+k*(deltaG/steps)), 
      (blue(c1)+k*(deltaB/steps)) 
    );
    fill(c);
    rect((i+offset)*5,0,5,5);
    fixedGradient[i+offset] = c;
  }
}

void mirroredFixedGradient(int step, int steps, color c1, color c2) {
  // calculate differences between color components 
  float deltaR = red(c2)-red(c1);
  float deltaG = green(c2)-green(c1);
  float deltaB = blue(c2)-blue(c1);
  int s2 = steps/2;
  int j;
  
  for (int i=0; i<s2; i++) {
    j=i+step;
    if (j>=steps) { j=j-steps; }
    
    color c = color(
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
    
    color c = color(
      (red(c1)+i*(deltaR/s2)), 
      (green(c1)+i*(deltaG/s2)), 
      (blue(c1)+i*(deltaB/s2)) 
    );
    fill(c);
    rect(j*5,0,5,5);
    fixedGradient[j] = c;
  }

}

void applyFixedGradient(int x, int w) {
  int j;
  for (int i=0; i<w; i++) {
    j = int(i*1.0/w*100+5);
    values[i+x] = fixedGradient[j];
    fill(255,0,0);
    rect(j*5,5,1,5);
  }
}

void mirroredFixedLetterGradient(int x, int w, int step, int steps, color c1, color c2) {
  mirroredFixedGradient(step,steps,c1,c2);
  applyFixedGradient(x, w);  
}

void fixedLetterGradient(int x, int w, int step, int steps, color c1, color c2) {
  fixedGradient(step,steps,0,c1,c2);
  applyFixedGradient(x, w);
}

void letterGradient(int x, int w, color c1, color c2) {
  // calculate differences between color components 
  float deltaR = red(c2)-red(c1);
  float deltaG = green(c2)-green(c1);
  float deltaB = blue(c2)-blue(c1);
  int j;

  for (int i=x; i<(x+w); i++) {
    color c = color(
      (red(c1)+(i-x)*(deltaR/w)), 
      (green(c1)+(i-x)*(deltaG/w)), 
      (blue(c1)+(i-x)*(deltaB/w)) 
    );

    j = i;
    while (j >= LETTERS) { j = j - LETTERS; }
    values[j] = c;
  }
}

void setGradient(int x, int y, float w, float h, color c1, color c2, int axis ) {
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
        color c = color(
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
        color c = color(
        (red(c1)+(j-x)*(deltaR/h)), 
        (green(c1)+(j-x)*(deltaG/h)), 
        (blue(c1)+(j-x)*(deltaB/h)) 
          );
        set(j, i, c);
      }
    }
  }
}


