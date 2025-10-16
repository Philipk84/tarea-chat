## Integrantes
- Alejandro Vargas
- Sebastián Romero
- Felipe Calderon
  
## Ejecución del proyecto

1. Clonar el repositorio
2. Moverse en consola dentro de la carpeta del proyecto
    ```bash
   cd Proyecto

3. Compilar el proyecto
    ```bash
    gradle build

4. Modificar el archivo config.json: En este archivo, cambia el valor del campo host por la dirección IP del equipo donde se está ejecutando el servidor, por ejemplo:
    ```bash
    json
    {
      "port": 5000,
      "host": "192.168.1.10"
    }
    
  Todos deben tener la misma dirección de host
  
5. Correr el main del Servidor: Ejecutar la clase Main que se encuentra en Ui dentro del Server

6. Correr el main del Cliente por cada usuario: Ejecutar el Main que esta en Ui dentro de Client

7. Usar la aplicación
