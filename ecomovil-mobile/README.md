# EcoMovil Mobile (Android)

App Android de EcoMovil. Empaqueta el frontend de `../ecomovil-web` dentro de un
WebView, así web y móvil comparten el mismo código.

## Probar en el emulador de Android Studio

1. Levantar el backend en la PC (ver el README principal): Docker, los
   microservicios y el `api-gateway` en el puerto 8080.
2. Abrir la carpeta `ecomovil-mobile` en Android Studio
   (File > Open). La primera vez descarga Gradle y las dependencias.
3. Crear o elegir un emulador (Device Manager), por ejemplo un Pixel con API 34.
4. Pulsar **Run**. La app se instala y abre en el emulador.

Dentro del emulador, `10.0.2.2` es el `localhost` de la PC, por eso la app usa
`http://10.0.2.2:8080` como servidor. No hay que configurar nada.

## Sin compilar: instalar el APK

Cada push que toca `ecomovil-mobile/` o `ecomovil-web/` compila el APK en
GitHub Actions (pestaña **Actions** > *Android APK* > artefacto
`ecomovil-debug-apk`). Para instalarlo, arrastrar `app-debug.apk` a la ventana
del emulador.

## Celular físico

En la pantalla de inicio de sesión, cambiar el servidor por la IP de la PC en la
red WiFi, por ejemplo `http://192.168.1.20:8080`. El celular y la PC deben estar
en la misma red y el firewall de Windows debe permitir el puerto 8080.

## Depurar

Con la app abierta en el emulador, entrar a `chrome://inspect` en Chrome de la PC
para ver la consola y las peticiones del WebView.
