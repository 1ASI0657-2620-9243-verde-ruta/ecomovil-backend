// URL del API Gateway. Se puede cambiar desde la pantalla de login
// (queda guardada en el dispositivo).
(function () {
  var loc = window.location;
  var host = loc.hostname;

  // URL del servidor en la nube para la app Android. Al desplegar, poner aquí
  // la IP o dominio público, por ejemplo "http://20.120.30.40".
  var CLOUD_API_URL = "";

  if (host === "appassets.androidplatform.net") {
    // App Android: servidor en la nube si está definido; si no, la PC vista
    // desde el emulador (10.0.2.2 es el localhost de la PC).
    window.ECOMOVIL_API_URL = CLOUD_API_URL || "http://10.0.2.2:8080";
  } else if (!loc.port || loc.port === "80" || loc.port === "443") {
    // Servido por nginx (docker-compose.prod.yml): el API está en /api del mismo dominio
    window.ECOMOVIL_API_URL = loc.origin;
  } else if (host === "localhost" || host === "127.0.0.1") {
    // Desarrollo: python -m http.server 8795 y gateway en 8080
    window.ECOMOVIL_API_URL = "http://localhost:8080";
  } else {
    // Abierto desde otro equipo o desde el Chrome del emulador
    window.ECOMOVIL_API_URL = loc.protocol + "//" + host + ":8080";
  }
})();
