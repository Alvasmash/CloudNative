@echo off
cd /d "%~dp0"
docker compose up --build -d --wait --wait-timeout 180
if errorlevel 1 (
  echo No se pudo iniciar. Comprueba que Docker Desktop este abierto y los puertos 4200 y 8080 esten libres.
  pause
  exit /b 1
)
echo Pedidos360 disponible en http://localhost:4200/login
start "" "http://localhost:4200/login"
