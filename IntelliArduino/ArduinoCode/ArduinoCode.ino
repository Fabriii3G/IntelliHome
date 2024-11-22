// Biblioteca servo
#include <Servo.h>

Servo MyServo;

// Pin del LED
const int ledPin = 3;
// Pin del buzzer
const int buzzerPin = 2;

void setup() {
  Serial.begin(9600);  // Inicia la comunicación serial a 9600 bps

  // Configuración del pin del servomotor
  MyServo.attach(4);

  // Configuración de los pines
  pinMode(ledPin, OUTPUT);
  pinMode(buzzerPin, OUTPUT);
}

void loop() {
  // Verifica si hay datos disponibles en el puerto serial
  if (Serial.available() > 0) {
    char command = Serial.read(); // Lee el comando enviado

    // Procesa el comando recibido
    if (command == '1') {
      PrendeLED();  // Prende el LED
      Serial.println("LED encendido");
    } else if (command == '0') {
      ApagaLED();   // Apaga el LED
      Serial.println("LED apagado");
    }else if (command == '2'){
      Sonido();    // Activa el buzzer
      Serial.println("Buzzer activado");
    }else if (command == '3'){
      mueveServo();    // Activa el servo
      Serial.println("Servo activado");
    }
  }
}

void ApagaLED(){
  digitalWrite(ledPin, LOW); // Apaga el LED
}

void PrendeLED() {
  digitalWrite(ledPin, HIGH);  // Enciende el LED
}

void Sonido() {
  digitalWrite(buzzerPin, HIGH);  // Activa el buzzer
  delay(500);                     // Emite sonido por 500 ms
  digitalWrite(buzzerPin, LOW);   // Apaga el buzzer
} 

void mueveServo() {
  //Serial.println("Moviendo Servo");
  MyServo.write(50);              // Mueve el servo a 50 grados 
  delay(1000);                    // Mantiene la posición por 1 segundo
  MyServo.write(0);               // Mueve el servo de regreso a 0 grados
  delay(1000);
  MyServo.write(50);              // Regresa a la posición inicial
}
