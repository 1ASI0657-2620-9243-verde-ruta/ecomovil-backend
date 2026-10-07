@echo off
cd /d "%~dp0"

echo [1/3] Levantando PostgreSQL y Mosquitto...
pushd ecomovil-infra
docker compose up -d
popd
timeout /t 8 /nobreak >nul

echo [2/3] Levantando microservicios...
for %%S in (iam-service vehicle-service telemetry-service safety-service access-service reservation-service) do (
  start "%%S" cmd /k "cd /d %~dp0%%S && mvnw.cmd -DskipTests spring-boot:run"
  timeout /t 20 /nobreak >nul
)

echo [3/3] Levantando API Gateway...
start "api-gateway" cmd /k "cd /d %~dp0api-gateway && mvnw.cmd -DskipTests spring-boot:run"

echo Listo. Espera ~30 s y abre la pagina.
pause