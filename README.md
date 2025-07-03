


//code is here

#include <WiFi.h>
#include <WebServer.h>
#include <DHT.h>

// ==== Wi-Fi Credentials ====
const char* ssid = "MiFi_2B1266";     // Change to your actual WiFi SSID
const char* password = "12345678";   // Change to your actual WiFi password

// ==== Pin Configuration ====
#define DHTPIN 4
#define DHTTYPE DHT11
#define MOISTURE_PIN 34
#define LED_PIN 32

DHT dht(DHTPIN, DHTTYPE);

// ==== Web Server ====
WebServer server(80);

// ==== Variables ====
float ET = 0;
bool freshET = false;
bool autoMode = true;
bool lastValveState = false;

unsigned long lastETUpdate = 0;
unsigned long lastSensorSend = 0;
unsigned long lastStatusSend = 0;

const unsigned long sensorInterval = 5000;
const unsigned long statusInterval = 3000;
const int moistureThreshold = 2230;       // Tune as per your sensor
const float etThreshold = 4.0;

// ==== Handle Serial Input from MATLAB ====
void processSerialInput() {
  while (Serial.available()) {
    String input = Serial.readStringUntil('\n');

    if (input == "1") {
      autoMode = false;
      digitalWrite(LED_PIN, HIGH);
      Serial.println("# Manual Mode: Valve ON");
    } else if (input == "0") {
      autoMode = false;
      digitalWrite(LED_PIN, LOW);
      Serial.println("# Manual Mode: Valve OFF");
    } else if (input == "A" || input == "a") {
      autoMode = true;
      Serial.println("# Auto Mode Enabled");
    } else {
      ET = input.toFloat();
      lastETUpdate = millis();
      freshET = true;
    }
  }
}

// ==== Web Request Handlers ====
void handleOn() {
  autoMode = false;
  digitalWrite(LED_PIN, HIGH);
  server.send(200, "text/plain", "Valve ON (Manual Mode)");
}

void handleOff() {
  autoMode = false;
  digitalWrite(LED_PIN, LOW);
  server.send(200, "text/plain", "Valve OFF (Manual Mode)");
}

void handleAuto() {
  autoMode = true;
  server.send(200, "text/plain", "Auto Mode Enabled");
}

void setup() {
  Serial.begin(115200);
  delay(1000);  // Wait for Serial Monitor
  Serial.println("Starting ESP32...");

  dht.begin();
  pinMode(MOISTURE_PIN, INPUT);
  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, LOW);

  // ==== Connect to WiFi ====
  WiFi.begin(ssid, password);
  Serial.print("Connecting to WiFi");

  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 40) {
    delay(500);
    Serial.print(".");
    attempts++;
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\n✅ WiFi connected.");
    Serial.print("🌐 IP Address: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println("\n❌ Failed to connect to WiFi.");
  }

  // ==== Setup Web Server Routes ====
  server.on("/on", handleOn);
  server.on("/off", handleOff);
  server.on("/auto", handleAuto);
  server.begin();
  Serial.println("🌐 Web server started");

  Serial.println("📡 ESP32_READY");
}

void loop() {
  server.handleClient();
  processSerialInput();

  int moistureValue = analogRead(MOISTURE_PIN);

  if (autoMode && ET > 0 && freshET) {
    bool shouldTurnOn = (moistureValue > moistureThreshold && ET > etThreshold);
    digitalWrite(LED_PIN, shouldTurnOn);

    if (shouldTurnOn != lastValveState || millis() - lastStatusSend > statusInterval) {
      if (shouldTurnOn) {
        Serial.println("# Auto Mode: High ET & Dry Soil - Valve ON");
      } else {
        Serial.println("# Auto Mode: Conditions Not Met - Valve OFF");
      }
      Serial.print("Soil Moisture: "); Serial.println(moistureValue);
      lastValveState = shouldTurnOn;
      lastStatusSend = millis();
    }

    freshET = false;
  }

  // ==== Send Sensor Data Every Interval ====
  if (millis() - lastSensorSend >= sensorInterval) {
    float temp = dht.readTemperature();
    float hum = dht.readHumidity();

    if (!isnan(temp) && !isnan(hum)) {
      float temp2 = temp * temp;
      float hum2 = hum * hum;
      float interaction = temp * hum;

      Serial.print(temp); Serial.print(",");
      Serial.print(temp2); Serial.print(",");
      Serial.print(hum); Serial.print(",");
      Serial.print(hum2); Serial.print(",");
      Serial.println(interaction);
    } else {
      Serial.println("# DHT read error");
    }

    lastSensorSend = millis();
  }

  delay(100);  // Avoid overloading loop
}

