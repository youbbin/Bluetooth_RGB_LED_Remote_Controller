#include <SoftwareSerial.h>

#define BT_RXD 8
#define BT_TXD 7
SoftwareSerial bluetooth(BT_RXD, BT_TXD);
byte data;
int pinR=9;
int pinG=10;
int pinB=11;
int pinPIR=2;

double R=128;
double G=255;
double B=0;
double brt;
int ledOn=0; // 1: on, 0: off
byte colors[6];
int mode=0; // 1: pir mode on, 0: pir mode off
int pir;

void setup() {
  Serial.begin(9600);
  bluetooth.begin(9600);
  pinMode(pinPIR,INPUT);
  pinMode(pinR,OUTPUT);
  pinMode(pinG,OUTPUT);
  pinMode(pinB,OUTPUT);
}

void loop() {
  if(bluetooth.available()){
    bluetooth.readBytes(colors,6);
    if(colors[0]==(byte)0) ledOn=0; // led Off
    if(colors[0]==(byte)1) ledOn=1; // led On
    if(colors[5]==(byte)0) mode=0; // 인체감지 모드 Off
    if(colors[5]==(byte)1) mode=1; // 인체감지 모드 On
    
    for(byte i=0;i<6;i++){
      Serial.print((String)colors[i]+" ");
    }
    Serial.println();

    brt=colors[4];
    R=colors[1]*(brt/100);
    G=colors[2]*(brt/100);
    B=colors[3]*(brt/100);
  }
  
  if(ledOn==1 || ((mode==1)&& (pir=digitalRead(pinPIR))==HIGH)){
     analogWrite(pinR, R),analogWrite(pinG, G),analogWrite(pinB, B);
     if(mode==1&&pir==HIGH){
       delay(5000); // 인체 감지 시 5초간 On
     }
  }
  else if(ledOn==0 || ((mode==1) && (pir=digitalRead(pinPIR))==LOW)){
     analogWrite(pinR, 0),analogWrite(pinG, 0),analogWrite(pinB, 0);
     if(mode==1&&pir==LOW){
      delay(1000);
     }
  }


  if(Serial.available()){
    bluetooth.write(Serial.read());
  }
}
