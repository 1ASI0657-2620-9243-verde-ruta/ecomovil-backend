# Despliegue de EcoMovil en la nube

EcoMovil se despliega completo en una máquina virtual con Docker: PostgreSQL con
PostGIS, Mosquitto, los seis microservicios, el API Gateway, el frontend web
(nginx) y el simulador IoT. Todo se define en
`ecomovil-infra/docker-compose.prod.yml`.

| Puerto | Uso |
|---|---|
| 80 | Frontend web y API en `/api` (nginx) |
| 8080 | API Gateway directo (Postman, app Android) |
| 22 | SSH para administrar la VM |

La base de datos y el broker MQTT no se exponen a Internet.

## Requisitos de la VM

- Ubuntu Server 22.04 o 24.04.
- 2 vCPU y **4 GB de RAM** como mínimo. El sistema completo usa unos 1,7 GB y
  la compilación de las imágenes necesita más.
- 30 GB de disco.

## Opción recomendada: Azure for Students

Con el correo `@upc.edu.pe` se activa Azure for Students, que da crédito sin
tarjeta: https://azure.microsoft.com/free/students

1. En el portal de Azure, **Crear un recurso > Máquina virtual**.
2. Imagen: **Ubuntu Server 24.04 LTS**. Tamaño: **Standard_B2s** (2 vCPU, 4 GB)
   o cualquiera con 4 GB. El portal muestra el precio por hora antes de crear.
3. Autenticación: clave SSH o contraseña.
4. Puertos de entrada: **HTTP (80)** y **SSH (22)**.
5. Pestaña **Opciones avanzadas > Datos personalizados (Custom data)**: pegar el
   contenido de `ecomovil-infra/cloud-init.yaml`.
6. **Revisar y crear**.
7. Cuando termine, en **Redes** de la VM agregar una regla de entrada para el
   puerto **8080** (TCP).
8. Esperar unos 15 minutos: la VM instala Docker, clona este repositorio y
   compila las imágenes sola.
9. Abrir `http://<IP-pública>` en el navegador.

Para ver el avance de la instalación desde SSH:

```bash
sudo tail -f /var/log/cloud-init-output.log
sudo docker compose -f /opt/ecomovil/ecomovil-infra/docker-compose.prod.yml ps
```

**Para no gastar crédito**, detener la VM desde el portal cuando no se use. Al
volver a encenderla, los contenedores arrancan solos y los datos se conservan.
La IP pública puede cambiar al reiniciar, salvo que se configure como estática.

## AWS o Google Cloud

El mismo `cloud-init.yaml` sirve en ambos:

- **AWS EC2**: instancia Ubuntu `t3.medium` (4 GB). Pegar el archivo en
  *Advanced details > User data*. En el *Security Group* abrir 80, 8080 y 22.
- **Google Compute Engine**: VM Ubuntu `e2-medium` (4 GB). En
  *Management > Automation > Startup script* usar los comandos de la sección
  `runcmd`. En el firewall abrir 80 y 8080.

## Conectar la app Android al servidor

En `ecomovil-web/config.js` poner la IP pública en `CLOUD_API_URL`, por ejemplo
`var CLOUD_API_URL = "http://20.120.30.40";`, y hacer push. GitHub Actions
compila un APK nuevo que ya apunta al servidor. Sin recompilar, también se
puede cambiar el servidor desde la pantalla de inicio de sesión de la app.

## Actualizar el servidor

```bash
ssh usuario@<IP-pública>
cd /opt/ecomovil
sudo git pull
cd ecomovil-infra
sudo docker compose -f docker-compose.prod.yml up -d --build
```

## Probar en local igual que en la nube

```bash
cd ecomovil-infra
docker compose -f docker-compose.prod.yml up -d --build
```

Luego abrir http://localhost. Para detenerlo:
`docker compose -f docker-compose.prod.yml down`.

## Variables de entorno

Los servicios leen su configuración de variables de entorno y, si no existen,
usan los valores locales (`localhost`), así siguen funcionando desde IntelliJ.

| Variable | Servicios | Ejemplo en Docker |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | iam, vehicle, reservation, telemetry, safety | `jdbc:postgresql://postgres:5432/ecomovil` |
| `MQTT_BROKER_URL` | telemetry, safety, access | `tcp://mosquitto:1883` |
| `VEHICLE_SERVICE_URL` | reservation, api-gateway | `http://vehicle-service:8081` |
| `*_SERVICE_URL` | api-gateway | `http://iam-service:8086`, etc. |
| `SHOW_SQL` | servicios con base de datos | `false` |

Para cambiar la contraseña de la base de datos, crear un archivo
`ecomovil-infra/.env` con `DB_PASSWORD=...` antes del primer arranque.
