// URL del API Gateway por defecto. Se puede cambiar desde la pantalla de login
// (queda guardada en el dispositivo). Al desplegar el backend en la nube,
// reemplazar la detección por su URL pública.
(function () {
  var host = window.location.hostname;
  if (host === "appassets.androidplatform.net") {
    // App Android (ecomovil-mobile) en el emulador: 10.0.2.2 es el localhost de la PC
    window.ECOMOVIL_API_URL = "http://10.0.2.2:8080";
  } else if (host && host !== "localhost" && host !== "127.0.0.1") {
    // Abierto desde otra máquina o desde el Chrome del emulador (http://10.0.2.2:8795):
    // el gateway corre en la misma máquina que sirve la página
    window.ECOMOVIL_API_URL = window.location.protocol + "//" + host + ":8080";
  } else {
    window.ECOMOVIL_API_URL = "http://localhost:8080";
  }
})();
